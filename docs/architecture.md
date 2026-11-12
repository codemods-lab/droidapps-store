# Architecture

## Release pipeline

```
app repo (private)                         droid-apps (public)          droidapps-store (public)
──────────────────                         ───────────────────────          ────────────────────────
git tag v1.2.3 ─▶ .github/workflows/release.yml
                  1. version from tag (1.2.3 → code 10203)
                  2. decode KEYSTORE_BASE64, assembleRelease (release key only)
                  3. notes = commit subjects since previous tag
                  4. publish ─────────────────────▶ Release "<id>-v1.2.3"
                                                    asset <id>-1.2.3.apk
                  5. repository_dispatch ────────────────────────────────▶ catalog.yml
                     (event app-released, UTILDROID_TOKEN)                  build_catalog.py
                                                                            commits catalog/catalog.json
                                                                                     │
phone: DroidApps Store ◀── raw.githubusercontent.com/.../main/catalog/catalog.json ──┘
       downloads APK from the droid-apps Release, verifies, installs
```

The store releases itself the same way, except its release lives in its own (public) repo and its workflow is `.github/workflows/release.yml` in this repo.

`catalog.yml` also runs on every push to `catalog/apps.yaml`, `catalog/apps/**` or `scripts/build_catalog.py`, every 6 hours (`17 */6 * * *`), and on manual dispatch. So a missed ping only delays an update; it never loses one.

## The catalog

- **`catalog/apps.yaml`** is hand-edited: the store name, the signing-cert fingerprint and one entry per app (`id`, `repo`, `packageName`, `name`, `summary`, `description`, `category`, `platforms`, and `releases` for private apps).
- **`scripts/build_catalog.py`** turns it into `catalog/catalog.json`:
  - finds the newest release: `/repos/{repo}/releases/latest` for public apps, or the newest non-draft, non-prerelease release tagged `<id>-v*` in the `releases:` repo for private apps;
  - downloads the `.apk` asset and records `sha256`, `sizeBytes`, then reads `versionCode`, `versionName` and `minSdk` with `aapt2 dump badging`;
  - **skips** the release if the APK's package name doesn't match `packageName` in `apps.yaml`;
  - if anything fails (API error, network, unreadable APK), keeps the app's previous entry, so a bad run never removes an app;
  - re-downloads only when the APK URL changes, and doesn't rewrite the file when only the timestamp would change;
  - leaves `repo` empty for private apps, so the store hides the "Source" link (which would 404).
- Icons and screenshots come from `catalog/apps/<id>/icon.png` and `catalog/apps/<id>/screenshots/*`, served from `raw.githubusercontent.com`.

## The store app (`app/`)

Kotlin, Jetpack Compose, phone only. Min SDK 26, target 35. A manual DI container is in `AppContainer.kt`.

| Area | Files | Notes |
| --- | --- | --- |
| Catalog | `data/CatalogRepository.kt`, `data/CatalogModels.kt` | Fetches `catalog.json` (default URL baked in from `utildroid.githubOwner` in `gradle.properties`, overridable in Settings) and caches it on disk for offline start |
| Installed state | `data/InstalledApps.kt` | Tracks installed versions of catalog packages (`QUERY_ALL_PACKAGES`) |
| Settings | `data/SettingsStore.kt` | DataStore: catalog URL, check interval (default 12 h, 0 = off), Wi‑Fi only (on), silent updates (on), notifications (on) |
| Install | `installer/InstallManager.kt` | Download → verify → `PackageInstaller` session. Phases: Downloading, Verifying, Installing, WaitingForUser, Failed |
| Verify | `installer/ApkVerifier.kt` | SHA-256 of the file must match the catalog, and the APK's signing cert must match `store.signingCertSha256` (colons and case ignored). An empty fingerprint skips the cert check |
| Results | `installer/InstallResultReceiver.kt` | Receives session results; asks the user to confirm when Android requires it |
| Background | `work/UpdateCheckWorker.kt`, `work/Notifier.kt` | WorkManager job: refresh catalog, find installed apps with a higher `versionCode`, install silently if possible, otherwise notify. The store updates itself **last**, because that kills the process |
| UI | `ui/screens/{Browse,Detail,Updates,Settings}Screen.kt`, `ui/StoreViewModel.kt` | Browse lists apps whose `platforms` include `phone` |

**Silent updates** use `UPDATE_PACKAGES_WITHOUT_USER_ACTION`. They only work on Android 12+ (API 31), and only for apps this store installed. Otherwise Android shows its normal confirm screen.

## Trust model

- **One signing key** (`~/keys/release.jks`, alias `utildroid`, RSA 4096, valid ~100 years) signs every app. Android refuses an update signed with a different key, so this key is the root of trust. Back it up.
- The store checks every download against both the catalog's SHA-256 and the cert fingerprint. If someone uploads a forged APK to `droid-apps`, the store rejects it.
- `UTILDROID_TOKEN` can only write `droid-apps` and `droidapps-store` contents. If it leaks, the worst case is a forged release, which the cert check still blocks.
- Release builds are **never** debug-signed. Without `KEYSTORE_FILE` the Gradle release build fails with `SigningConfig "release" is missing required property "storeFile"`.

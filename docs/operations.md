# Operations runbook

## Release a new version of an app

```bash
cd ~/dev/codemods/<app>
git tag v1.2.0 && git push origin v1.2.0
```

1. Watch it: `gh run list -R codemods-lab/<app> --limit 3`. Builds take 4–8 minutes.
2. Check it landed: `gh release list -R codemods-lab/utildroid-apps`
3. The catalog rebuilds within about a minute of the release (`gh run list -R codemods-lab/utildroid-store --limit 3`).
4. On the phone, pull to refresh in the store, or wait for the background check (every 12 h by default).

Rules:
- Tags must be `vMAJOR.MINOR.PATCH`. Version code = `MAJOR*10000 + MINOR*100 + PATCH`, so keep MINOR and PATCH below 100.
- **Never reuse or move a tag** after a successful release: the catalog keys on the APK URL, and phones that already installed it won't see a "new" build. If a release fails, fix it and tag the next patch (that's what happened with `v1.0.0` → `v1.0.1`).
- The "What's new" text is the commit subjects since the previous tag, so write readable commit messages.

To release the **store itself**, do the same in `utildroid-store`. Its APK goes to its own Releases page.

## Add a new app

1. Create a **private** repo `codemods-lab/<id>`. The repo name should equal the catalog `id`, or set an Actions variable `APP_ID`.
2. In the app:
   - set `applicationId = "com.innovcrest.<name>"`;
   - merge `templates/signing.gradle.kts` into `app/build.gradle.kts` (version from env, release-key-only signing);
   - copy `templates/release.yml` to `.github/workflows/release.yml`;
   - keep personal data out of `app/src/main`, because the APK is public.
3. Add the 5 secrets (see below).
4. Add an entry to `catalog/apps.yaml` with `releases: codemods-lab/utildroid-apps`. Optionally add `catalog/apps/<id>/icon.png`. Push.
5. Tag `v1.0.0` in the app repo.

## Secrets

Each app repo needs these 5 secrets. The store repo needs the first 4.

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -i ~/keys/release.jks` |
| `KEYSTORE_PASSWORD` | keystore password (in your password manager) |
| `KEY_ALIAS` | `utildroid` |
| `KEY_PASSWORD` | same as the keystore password (PKCS12 keystores use one password) |
| `UTILDROID_TOKEN` | fine-grained PAT named `utildroid`, owner `codemods-lab`, repos `utildroid-apps` + `utildroid-store`, **Contents: Read and write** |

```bash
R=codemods-lab/<app>
base64 -i ~/keys/release.jks | gh secret set KEYSTORE_BASE64 -R $R
gh secret set KEY_ALIAS -R $R -b utildroid
gh secret set KEYSTORE_PASSWORD -R $R   # paste
gh secret set KEY_PASSWORD -R $R        # paste
gh secret set UTILDROID_TOKEN -R $R     # paste
```

### Rotate `UTILDROID_TOKEN` (when it expires or leaks)

1. Create a new token with the same settings at https://github.com/settings/personal-access-tokens/new
2. Run `gh secret set UTILDROID_TOKEN -R codemods-lab/<app>` for each app repo.
3. Delete the old token.

**Symptom of an expired token:** an app build succeeds up to "Publish to the public APK repo", then fails with 401/403.

## The signing key

- File: `~/keys/release.jks` (mode 600), alias `utildroid`.
- Fingerprint (SHA-256): `933cb230c5355557e2d00d833f1cd2d9e669eae855a82f391a14f1bb0599d0d5`. It's in `catalog/apps.yaml`.
- **Keep two offline backups** of the file and its password. If they're lost, no installed app can ever be updated again; every app would need a new package name and a reinstall.
- Check an APK's signer:
  ```bash
  ~/Library/Android/sdk/build-tools/36.0.0/apksigner verify --print-certs some.apk | grep SHA-256
  ```

## Build a release APK locally

```bash
export KEYSTORE_FILE=~/keys/release.jks KEY_ALIAS=utildroid
read -s KEYSTORE_PASSWORD && export KEYSTORE_PASSWORD KEY_PASSWORD=$KEYSTORE_PASSWORD
./gradlew assembleRelease
```

Debug builds (`./gradlew assembleDebug`) need nothing. Note that a debug build can't be installed over a store-installed app, because the keys differ.

## Run the catalog builder locally

```bash
python3 -m venv /tmp/uv && /tmp/uv/bin/pip install pyyaml
ANDROID_HOME=~/Library/Android/sdk /tmp/uv/bin/python scripts/build_catalog.py
```

This rewrites `catalog/catalog.json`. Normally CI owns that file, so discard the local change (`git checkout catalog/catalog.json`) instead of committing it.

## Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Push rejected: "refusing to allow an OAuth App to create or update workflow … without `workflow` scope" | The `gh` HTTPS token lacks `workflow` scope | Use SSH remotes (`git@github.com:codemods-lab/<repo>.git`); all clones are already set up that way |
| CI fails within ~25 s in `android-actions/setup-android@v3` ("Failed to find package 'tools'") | That action is broken on current runners | Don't use it. The runner image already has the Android SDK, and AGP downloads missing platforms itself |
| Release build fails: `SigningConfig "release" is missing required property "storeFile"` | No keystore env vars | Expected locally. In CI, check that the 4 signing secrets exist |
| App missing from the store after a release | Catalog skipped it | Check the `Build catalog` run log: "APK package is X, apps.yaml says Y" means a package-name mismatch; "no release yet" means the tag prefix doesn't match the `id` |
| Store says signature mismatch | APK not signed with the release key | Check it with `apksigner`. Never publish debug-signed APKs |
| "App not installed" on the phone | An older copy with the same package but a different key is installed | Uninstall it once, then install from the store |
| Background updates never happen | Battery optimisation, Wi‑Fi-only setting, or Android < 12 for silent installs | Check the store's Settings; on Android < 12 you'll get a notification instead |

The local `gh` CLI is an old version: it has no `gh auth token`, no `gh repo create --add-readme` and no `gh release download --clobber`. Use `gh api …` for those, or upgrade with `brew upgrade gh`.

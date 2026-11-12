# Decisions

A short log of decisions that shape the system. Add new entries at the top: date, decision, why, and what it costs.

## 2026-11-10: Private source repos, public APK repo

- **Decision:** App source lives in private repos. CI publishes signed APKs to the public `codemods-lab/droid-apps` repo as releases tagged `<id>-vX.Y.Z`. `apps.yaml` gets an optional `releases:` field.
- **Why:** The store downloads APKs and `catalog.json` without logging in. Release assets in a private repo need a token, and a token shipped inside an APK can be extracted. APKs can always be decompiled, so making them public reveals little that isn't already exposed.
- **Cost:** One extra repo and a cross-repo token (`UTILDROID_TOKEN`). Private repos have no GitHub wiki on the free plan, so docs live in `docs/` folders instead.

## 2026-11-10: All packages under `com.innovcrest.*`

- **Decision:** `applicationId` changed to `com.innovcrest.{aryoutube,spendlens,chatlens}`. Kotlin namespaces and source folders were left unchanged.
- **Why:** One namespace to register when Android developer verification (Google's 2027 sideloading rules) opens. Old installs were debug-signed and needed a reinstall anyway.
- **Cost:** Data in old installs (`com.ar.youtube`, `com.chatlens`, `com.aishwary.spendlens`) doesn't carry over.

## 2026-11-10: Release builds are only ever signed with the release key

- **Decision:** No debug-key fallback for `release` builds, in the store and all apps. A release build without `KEYSTORE_FILE` fails.
- **Why:** A debug-signed "release" APK can't update a store-installed app, and it fails the store's cert check. Failing loudly is better than publishing one.

## 2026-11-10: Version code comes from the git tag

- **Decision:** `vA.B.C` → `versionName A.B.C`, `versionCode A*10000 + B*100 + C`. It's set by CI through `VERSION_NAME` / `VERSION_CODE` env vars.
- **Why:** Nothing to bump by hand, and codes are always increasing.
- **Cost:** MINOR and PATCH must stay below 100. Tags are never moved or reused.

## 2026-11-10: First app releases are `v1.0.1`, not `v1.0.0`

- **What happened:** The `v1.0.0` app builds failed in `android-actions/setup-android@v3`, which is broken on current runners. The step was removed. Moving the tags would have needed a force-push, so `v1.0.1` was tagged instead.

## 2026-11-10: Personal data stays out of shipped code

- **Decision:** The SpendLens default "Your names" value (the owner's full name) was removed; it now starts empty.
- **Why:** The APKs are public. Test fixtures under `app/src/test` aren't shipped, so they may stay.

## 2026-11-10: One shared signing key

- **Decision:** One RSA-4096 key (`utildroid` alias) signs every app. Its fingerprint is pinned in `apps.yaml`.
- **Why:** Simple. The store only needs to trust one cert.
- **Cost:** It's a single point of failure. Losing it means every app needs a new package name. Keep it backed up.

## 2026-11-12: Renamed from UtilDroid to DroidApps Store

- **Decision:** The store is called DroidApps Store (launcher label "DroidApps"). Repos were renamed: `utildroid-store` → `droidapps-store`, `utildroid-apps` → `droid-apps`.
- **Kept on purpose:** package `com.innovcrest.utildroid` (changing it would make Android treat the store as a new app and break in-place and silent updates), the `utildroid` key alias, the `UTILDROID_TOKEN` secret name, and internal code names.
- **What still works:** GitHub redirects the old repo URLs, including the raw catalog URL baked into store versions up to 1.1.0 and old APK download links. Don't create new repos with the old names, or those redirects stop.
- **What broke:** the old website address `codemods-lab.github.io/utildroid-store/` returns 404. The site is now at `codemods-lab.github.io/droidapps-store/`.

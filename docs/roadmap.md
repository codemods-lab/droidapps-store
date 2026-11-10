# Roadmap

Candidate work, roughly ordered by value. Per-app details live in each app's `docs/roadmap.md`.

## Store and pipeline

| Idea | Why | Notes |
| --- | --- | --- |
| App icons and screenshots | Store cards are text-only today | Drop `icon.png` and `screenshots/*.png` into `catalog/apps/<id>/` and push. No code changes needed |
| TV build of the store | AR YouTube on TV is installed and updated by hand | The catalog already carries `platforms: [phone, tv]`. Needs a leanback launcher entry plus a D-pad-friendly browse screen |
| Beta channel | Test a build on one phone before everyone gets it | Publish GitHub pre-releases (`prerelease: true`). The catalog script currently ignores them; add an opt-in `betaApkUrl` and a store setting |
| Update action versions | CI warns that `setup-java@v4` and Node 20 actions are deprecated | Bump to `actions/setup-java@v5`, `actions/checkout@v5` etc. across the store and all app workflows |
| Shared reusable workflow | `release.yml` is copied into every app repo | Move it to a reusable workflow (`workflow_call`) in a public repo so a fix lands once |
| Rollback | Bad release on the phone | The catalog only knows "latest". Option: a `pin:` field per app in `apps.yaml` to hold a version |

## Platform risks to watch

- **Android developer verification (2027):** Google will require sideloaded apps to come from verified developers. When registration opens in India, register the `com.innovcrest.*` package names on a free limited-distribution account.
- **`UTILDROID_TOKEN` expiry:** App releases stop publishing when it expires. Put the expiry date in a calendar reminder.
- **YouTube changes:** AR YouTube depends on NewPipeExtractor; see that repo's docs.

## Shared gaps across the apps

- **No database migrations** in SpendLens, ChatLens or AR YouTube (all at schema version 1). Before any schema change, add a migration and a test for it.
- **No crash reporting.** All apps are offline-first by design. Option: a local crash log that each app's Settings screen can share.

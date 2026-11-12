# DroidApps Store docs

This is the wiki for the DroidApps ecosystem. It lives in the repo (not a GitHub wiki) so it's versioned with the code and works the same way in the private app repos. Update these pages in the same commit as the change they describe.

| Page | What's in it |
| --- | --- |
| [Architecture](architecture.md) | How the repos, CI, catalog and store app fit together; what the store app does on the phone |
| [Operations runbook](operations.md) | Releasing, adding an app, secrets and token rotation, key backup, troubleshooting |
| [Decisions](decisions.md) | Why things are the way they are (dated decision log) |
| [Roadmap](roadmap.md) | Known gaps and candidate next features across the whole ecosystem |

## The ecosystem at a glance

| Repo | Visibility | What it is | Docs |
| --- | --- | --- | --- |
| `codemods-lab/droidapps-store` | public | Store app + catalog + CI templates | this folder |
| `codemods-lab/droid-apps` | public | Signed APKs only (GitHub Releases, tags `<id>-vX.Y.Z`) | its README |
| `codemods-lab/ar-youtube` | private | Ad-free YouTube for phone + TV | `docs/` in that repo |
| `codemods-lab/spendlens` | private | Spend tracker from bank SMS | `docs/` in that repo |
| `codemods-lab/chatlens` | private | WhatsApp chat insights | `docs/` in that repo |

Local clones live side by side in `~/dev/codemods/`.

## Current versions (Nov 2026)

| App | Package | Latest release |
| --- | --- | --- |
| DroidApps Store | `com.innovcrest.utildroid` | `v1.0.0` |
| AR YouTube | `com.innovcrest.aryoutube` | `ar-youtube-v1.0.1` |
| SpendLens | `com.innovcrest.spendlens` | `spendlens-v1.0.1` |
| ChatLens | `com.innovcrest.chatlens` | `chatlens-v1.0.1` |

The `v1.0.0` tags in the three app repos are failed builds (see [Decisions](decisions.md), 2026-11-10). `v1.0.1` is each app's first real release.

#!/usr/bin/env python3
"""Build catalog/catalog.json from catalog/apps.yaml plus each app's latest GitHub Release.

For every app it finds the newest release, downloads the .apk asset once, records
its SHA-256 and size, and reads versionCode / versionName / minSdk with aapt2.
An app whose release can't be read keeps its previous catalog entry.

Env:
  GITHUB_TOKEN       optional, raises the GitHub API rate limit
  GITHUB_REPOSITORY  owner/repo of utildroid-store (set automatically in Actions)
  CATALOG_BRANCH     branch serving icons/screenshots (default: main)
  ANDROID_HOME       used to find aapt2 (preinstalled on GitHub's ubuntu runners)
"""
import datetime
import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request

import yaml

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG_DIR = os.path.join(ROOT, "catalog")
APPS_YAML = os.path.join(CATALOG_DIR, "apps.yaml")
CATALOG_JSON = os.path.join(CATALOG_DIR, "catalog.json")

TOKEN = os.environ.get("GITHUB_TOKEN", "")
STORE_REPO = os.environ.get("GITHUB_REPOSITORY", "codemods-lab/utildroid-store")
BRANCH = os.environ.get("CATALOG_BRANCH", "main")
IMAGE_EXT = (".png", ".jpg", ".jpeg", ".webp")


def log(msg):
    print(msg, file=sys.stderr)


def api(path):
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "utildroid-catalog"}
    if TOKEN:
        headers["Authorization"] = f"Bearer {TOKEN}"
    req = urllib.request.Request(f"https://api.github.com{path}", headers=headers)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def download(url, dest):
    headers = {"User-Agent": "utildroid-catalog", "Accept": "application/octet-stream"}
    req = urllib.request.Request(url, headers=headers)
    sha = hashlib.sha256()
    size = 0
    with urllib.request.urlopen(req, timeout=300) as r, open(dest, "wb") as f:
        while True:
            chunk = r.read(1 << 16)
            if not chunk:
                break
            f.write(chunk)
            sha.update(chunk)
            size += len(chunk)
    return sha.hexdigest(), size


def find_aapt2():
    found = shutil.which("aapt2")
    if found:
        return found
    for home in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")):
        if home:
            hits = sorted(glob.glob(os.path.join(home, "build-tools", "*", "aapt2")))
            if hits:
                return hits[-1]
    sys.exit("aapt2 not found: set ANDROID_HOME or put aapt2 on PATH")


def badging(apk):
    out = subprocess.run([find_aapt2(), "dump", "badging", apk], capture_output=True, text=True, check=True).stdout
    pkg = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", out)
    sdk = re.search(r"(?:minSdkVersion|sdkVersion):'(\d+)'", out)
    if not pkg:
        raise ValueError("could not read package info from APK")
    return pkg.group(1), int(pkg.group(2)), pkg.group(3), int(sdk.group(1)) if sdk else 0


def raw_url(rel_path):
    return f"https://raw.githubusercontent.com/{STORE_REPO}/{BRANCH}/{rel_path}"


def media(app_id):
    folder = os.path.join(CATALOG_DIR, "apps", app_id)
    icon = ""
    for ext in IMAGE_EXT:
        if os.path.isfile(os.path.join(folder, "icon" + ext)):
            icon = raw_url(f"catalog/apps/{app_id}/icon{ext}")
            break
    shots_dir = os.path.join(folder, "screenshots")
    shots = []
    if os.path.isdir(shots_dir):
        for name in sorted(os.listdir(shots_dir)):
            if name.lower().endswith(IMAGE_EXT):
                shots.append(raw_url(f"catalog/apps/{app_id}/screenshots/{name}"))
    return icon, shots


def latest_release(entry):
    """Newest published release for an app, or None.

    Public apps release in their own repo. Private apps set `releases:` to a shared public
    repo, where each release is tagged `<id>-v1.2.3`.
    """
    shared = entry.get("releases")
    if not shared:
        return api(f"/repos/{entry['repo']}/releases/latest")
    prefix = f"{entry['id']}-v"
    for rel in api(f"/repos/{shared}/releases?per_page=100"):
        if rel.get("tag_name", "").startswith(prefix) and not rel.get("draft") and not rel.get("prerelease"):
            return rel
    return None


def release_fields(entry, previous, workdir):
    """Latest-release fields for one app, or the previous ones if that fails."""
    keep = {k: previous[k] for k in (
        "versionName", "versionCode", "apkUrl", "sizeBytes", "sha256", "minSdk", "changelog", "releasedAt",
    ) if previous and k in previous}
    try:
        rel = latest_release(entry)
        if rel is None:
            log(f"  {entry['id']}: no release yet")
            return {}
    except urllib.error.HTTPError as e:
        if e.code == 404:
            log(f"  {entry['id']}: no release yet")
            return {}
        log(f"  {entry['id']}: GitHub API error {e.code}, keeping previous entry")
        return keep
    except Exception as e:  # network trouble: keep what we had
        log(f"  {entry['id']}: {e}, keeping previous entry")
        return keep

    asset = next((a for a in rel.get("assets", []) if a["name"].lower().endswith(".apk")), None)
    if not asset:
        log(f"  {entry['id']}: latest release has no .apk asset")
        return keep

    url = asset["browser_download_url"]
    if previous and previous.get("apkUrl") == url and previous.get("sha256"):
        log(f"  {entry['id']}: unchanged ({previous.get('versionName')})")
        return keep

    apk = os.path.join(workdir, f"{entry['id']}.apk")
    try:
        sha, size = download(url, apk)
        pkg, code, name, min_sdk = badging(apk)
    except Exception as e:
        log(f"  {entry['id']}: could not read APK ({e}), keeping previous entry")
        return keep
    if pkg != entry["packageName"]:
        log(f"  {entry['id']}: APK package is {pkg}, apps.yaml says {entry['packageName']}; skipped")
        return keep

    log(f"  {entry['id']}: {name} ({code})")
    return {
        "versionName": name,
        "versionCode": code,
        "apkUrl": url,
        "sizeBytes": size,
        "sha256": sha,
        "minSdk": min_sdk,
        "changelog": (rel.get("body") or "").strip(),
        "releasedAt": rel.get("published_at") or "",
    }


def main():
    with open(APPS_YAML) as f:
        cfg = yaml.safe_load(f) or {}

    previous = {}
    if os.path.isfile(CATALOG_JSON):
        with open(CATALOG_JSON) as f:
            old = json.load(f)
        previous = {a["id"]: a for a in old.get("apps", [])}
    else:
        old = None

    apps = []
    with tempfile.TemporaryDirectory() as work:
        for entry in cfg.get("apps", []):
            for key in ("id", "repo", "packageName", "name"):
                if not entry.get(key):
                    sys.exit(f"apps.yaml: every app needs '{key}' ({entry})")
            log(f"{entry['id']} <- {entry.get('releases') or entry['repo']}")
            icon, shots = media(entry["id"])
            app = {
                "id": entry["id"],
                "name": entry["name"],
                "packageName": entry["packageName"],
                "summary": entry.get("summary", ""),
                "description": entry.get("description", ""),
                "category": entry.get("category", "Tools"),
                "platforms": entry.get("platforms", ["phone"]),
                # Private source repos aren't linked from the store.
                "repo": "" if entry.get("releases") else entry["repo"],
                "iconUrl": icon,
                "screenshots": shots,
            }
            app.update(release_fields(entry, previous.get(entry["id"]), work))
            apps.append(app)

    store = cfg.get("store", {}) or {}
    catalog = {
        "schemaVersion": 1,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "store": {
            "name": store.get("name", "UtilDroid Store"),
            "signingCertSha256": store.get("signingCertSha256", "") or "",
        },
        "apps": apps,
    }

    # Skip rewriting when only the timestamp would change, so the repo isn't spammed.
    if old and {**old, "generatedAt": ""} == {**catalog, "generatedAt": ""}:
        log("No changes.")
        return
    with open(CATALOG_JSON, "w") as f:
        json.dump(catalog, f, indent=2, ensure_ascii=False)
        f.write("\n")
    log(f"Wrote {CATALOG_JSON} with {len(apps)} apps.")


if __name__ == "__main__":
    main()

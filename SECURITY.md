# FastDroid — Security Notes

The threat model and the findings of the review done before the repository went
public (October 2026).

## Threat model

**What it is.** A single-user, offline Android app. It has no accounts and no
server, and it doesn't request the `INTERNET` permission, so it cannot send
anything off the phone itself.

**Assets.** Your fasting history: start and end times, which amount to health data.
It lives in the app's private storage (a Room/SQLite database).

**Trust boundaries.**
1. **Other apps on the same phone.** They can only reach FastDroid through
   components it exports.
2. **The build pipeline.** Building runs Gradle, its plugins and around 90
   third-party libraries inside a Docker container on the developer's machine.

## Findings & status

| # | Finding | Severity | Status |
|---|---------|----------|--------|
| 1 | **ADB key exposed to the build** — `scripts/gradle.sh` mounted all of `~/.android` into the build container, including `adbkey`, the private key that authorises USB debugging on the developer's phone. A compromised build dependency could have copied it. | Medium | **Fixed** — only the debug signing key is kept, in `~/.cache/fastdroid-build` |
| 2 | **Unverified tool downloads** — the Dockerfile fetched the Android command-line tools and Gradle over HTTPS without checking hashes | Low | **Fixed** — pinned SHA-256, checked during `docker build` |
| 3 | **Exported components** — only the launcher activity is exported, and it reads no intent data. The broadcast receiver isn't exported; the system still delivers boot broadcasts to it. | — | **Reviewed — safe** |
| 4 | **PendingIntents** — every one is `FLAG_IMMUTABLE` and targets one of the app's own components | — | **Reviewed — safe** |
| 5 | **Cloud backup** — `allowBackup="true"`, so Android's backup includes the fasting history. On Android 9+ that backup is end-to-end encrypted with the device's screen lock. Since API 31, `adb backup` no longer exports the data. | Info | **Accepted** — the backup is the only off-phone copy of the history |
| 6 | **Lock screen** — the running-fast notification (and the Android 16+ Live Update chip) shows start time and elapsed time. It uses the default `VISIBILITY_PRIVATE`, so the system's "hide sensitive content" setting applies. | Info | **Accepted** |
| 7 | **Dependencies** — all 91 resolved release-runtime artifacts were checked against [OSV](https://osv.dev): no known vulnerabilities. Android lint's security checks report nothing. | — | **Clean** (as of 2026-10-19) |

## Reporting a problem

Open an issue on GitHub. If it's sensitive, use GitHub's private vulnerability
reporting under the repository's **Security** tab.

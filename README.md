# Redmi System Inspector

An Android no-root diagnostic app with a multi-source Xiaomi ROM research catalogue.

## Version 0.4.0

- Read-only device diagnostics using public Android build fields and, when the OS permits it, selected read-only `getprop` values.
- Compares the public build fingerprint with system/vendor fingerprints and displays Verified Boot / vbmeta / flash-lock properties when available.
- Checks whether `com.android.updater` is installed and enabled.
- Loads device codenames from the public RSS index in `XiaomiFirmwareUpdater/miui-updates-tracker`.
- Searches three sources for a device codename: its community RSS feed, the HyperOS archive page, and the MIUI archive page on XM Firmware Updater.
- Merges duplicate version/type/region entries, supports an optional version/build filter, and reports source errors instead of silently implying that no ROM exists.
- Ranks results using the region suffix inferred from the installed build fingerprint, then gives Recovery packages a modest review priority and compares numeric version components. The ranking is advisory and does not establish compatibility or installability.
- Infers region from recognizable ROM suffixes (EEA, Global, China, India, Russia, Turkey, Taiwan, Indonesia, Japan, Korea) and classifies Recovery/Fastboot/OTA when evidence is present.
- Displays source, region, package type, version code and publication date where available, plus a visible explanation of whether the candidate region matches the installed fingerprint.
- Shows likely same-region candidates first; a different region is ranked lower. This is based on available build properties and does not prove the phone's original sales region.
- Links to Xiaomi's official ROM portal.
- Does not flash, install, unlock the bootloader, or alter system files.

## Data sources and limitations

- Xiaomi official ROM portal: https://new.c.mi.com/global/miuidownload/
- Community ROM RSS index: https://github.com/XiaomiFirmwareUpdater/miui-updates-tracker/tree/master/rss
- XM Firmware Updater HyperOS archive: https://xmfirmwareupdater.com/archive/hyperos/
- XM Firmware Updater MIUI archive: https://xmfirmwareupdater.com/archive/miui/
- Tracker project description: https://xmfirmwareupdater.com/projects/miui-updates-tracker/

The RSS and archive sources are community-maintained and are **not Xiaomi's official update service**. The app does not independently verify each download's cryptographic signature, checksum, anti-rollback index, compatibility, region authorization, or installability. Source coverage can be incomplete, pages can change, and some fields may be unavailable to an ordinary Android app. A result count is the number of records retrieved from the sources that responded, not proof that every historical or official release has been found.

A region inferred from a build suffix is a clue, not proof of the phone's original sales region. A mismatch between build properties is not sufficient by itself to prove that a ROM was modified. Verified Boot properties are shown only if the device exposes them; their presence does not establish that every system component is original.

Never install a ROM solely because it appears in the search results. Confirm the exact model/codename, region, package type, version, rollback constraints, and source before taking any action. The app does not bypass OTA signature checks or privileged update-engine access.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Wait for the workflow to finish successfully.
4. Open the completed run and download the `Redmi-System-Inspector-debug` artifact.
5. Extract the ZIP to get `app-debug.apk`.

The debug APK is for testing.

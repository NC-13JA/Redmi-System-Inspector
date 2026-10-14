# Redmi System Inspector

An Android no-root diagnostic app with a searchable Xiaomi ROM catalogue.

## Features

- Reads public Android build/device fields without requesting IMEI, serial number, or root.
- Checks whether `com.android.updater` is installed and enabled.
- Loads device codenames from the public RSS index in `XiaomiFirmwareUpdater/miui-updates-tracker`.
- Searches release records for any listed Xiaomi device, not just the current phone.
- Optional case-insensitive filter for a build/version string such as `OS1.0.1.0.UNOMIXM`.
- Shows release titles and dates, opens the community tracker record, and extracts direct ZIP links only when the host is under `mi.com` or `miui.com`.
- Links to Xiaomi's official ROM portal.
- Does not flash, install, or alter system files.

## ROM source and safety

The RSS catalogue is community-maintained and is **not affiliated with Xiaomi**. It tracks packages described as extracted from official ROM releases, but the app does not independently verify each file's signature, checksum, device compatibility, region, rollback index, or installability. A ROM listed for a codename may still be unsuitable for a particular regional variant. Prefer the official Xiaomi ROM portal and never install a package solely because it appears in search results.

The app cannot bypass OTA signature checks or gain privileged update-engine access. Local update support and package acceptance depend on Xiaomi's device/region rules and the package itself.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Wait for the workflow to finish successfully.
4. Open the completed run and download the `Redmi-System-Inspector-debug` artifact.
5. Extract the ZIP to get `app-debug.apk`.

The debug APK is for testing.

## Data sources

- Xiaomi official ROM portal: https://new.c.mi.com/global/miuidownload/
- Community ROM RSS index: https://github.com/XiaomiFirmwareUpdater/miui-updates-tracker/tree/master/rss
- Community project disclosure: https://github.com/XiaomiFirmwareUpdater/mi-firmware-updater

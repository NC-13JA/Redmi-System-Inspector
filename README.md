# Redmi System Inspector

An initial no-root diagnostic app for Android devices, starting with Xiaomi Redmi / HyperOS devices.

## Current features

- Reads public Android build/device fields.
- Checks whether `com.android.updater` is installed and enabled.
- Compares the public build incremental identifier with the public build fingerprint suffix and labels differences as review items, not proof of tampering.
- Creates a shareable text report.
- Does not request IMEI, serial number, root, or system modification permissions.

## Build an APK

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Press **Run workflow** (or wait for the first push build).
4. Open the completed run and download the `Redmi-System-Inspector-debug` artifact.
5. Extract the ZIP to get `app-debug.apk`, then install it on your Android phone.

The debug APK is for testing. This app diagnoses; it does not install OS updates. Normal Android apps cannot bypass Xiaomi's OTA signature checks or gain privileged update-engine access.

## Build locally

Requires JDK 17, Android SDK platform 35, and Gradle 8.9:

```sh
gradle assembleDebug
```

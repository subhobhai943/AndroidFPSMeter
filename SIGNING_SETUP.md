# APK Signing and Release Setup Guide

This guide will help you set up automated signed APK builds and releases using GitHub Actions.

## Prerequisites

- Android Studio or JDK installed locally
- Git installed
- GitHub account with access to this repository

---

## Step 1: Add Gradle Wrapper JAR

The Gradle wrapper JAR file is currently missing from the repository. Add it using one of these methods:

### Method A: Using Command Line (Recommended)

1. Clone the repository if you haven't already:
   ```bash
   git clone https://github.com/subhobhai943/AndroidFPSMeter.git
   cd AndroidFPSMeter
   ```

2. Run the Gradle wrapper task:
   ```bash
   ./gradlew wrapper
   ```

3. Commit and push the wrapper jar:
   ```bash
   git add gradle/wrapper/gradle-wrapper.jar
   git commit -m "Add Gradle wrapper JAR"
   git push
   ```

### Method B: Download Manually

1. Download the Gradle 8.5 wrapper jar from:
   ```
   https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
   ```

2. Place it in: `gradle/wrapper/gradle-wrapper.jar`

3. Commit and push:
   ```bash
   git add gradle/wrapper/gradle-wrapper.jar
   git commit -m "Add Gradle wrapper JAR"
   git push
   ```

---

## Step 2: Create a Signing Keystore

If you don't already have a keystore, create one:

```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

**Important:** Remember the following details:
- Keystore password
- Key alias (e.g., "my-key-alias")
- Key password
- Keystore file location

---

## Step 3: Set Up GitHub Secrets

You need to add the following secrets to your GitHub repository:

### 3.1: Convert Keystore to Base64

Run this command to convert your keystore to Base64:

```bash
base64 my-release-key.jks > keystore-base64.txt
```

Or on macOS:
```bash
base64 -i my-release-key.jks -o keystore-base64.txt
```

### 3.2: Add Secrets to GitHub

1. Go to your repository: [https://github.com/subhobhai943/AndroidFPSMeter](https://github.com/subhobhai943/AndroidFPSMeter)
2. Click on **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add these four secrets:

| Secret Name | Value | Description |
|------------|-------|-------------|
| `KEYSTORE_BASE64` | (Content of keystore-base64.txt) | Base64 encoded keystore file |
| `KEYSTORE_PASSWORD` | (Your keystore password) | Password for the keystore |
| `KEY_ALIAS` | (Your key alias) | Alias of the key in keystore |
| `KEY_PASSWORD` | (Your key password) | Password for the key |

**Security Note:** Never commit your keystore or passwords to the repository!

---

## Step 4: Trigger a Release Build

You have two options to trigger the build workflow:

### Option A: Create a Git Tag (Recommended)

1. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. The workflow will automatically:
   - Build the release APK
   - Sign it with your keystore
   - Create a GitHub release
   - Upload the signed APK

### Option B: Manual Workflow Trigger

1. Go to **Actions** tab in your repository
2. Select **Build and Release Signed APK** workflow
3. Click **Run workflow**
4. Enter the version name (e.g., "1.0.0")
5. Click **Run workflow**

---

## Step 5: Download Your Signed APK

After the workflow completes successfully:

1. Go to the **Releases** page: [https://github.com/subhobhai943/AndroidFPSMeter/releases](https://github.com/subhobhai943/AndroidFPSMeter/releases)
2. Find your release (e.g., "AndroidFPSMeter v1.0.0")
3. Download the signed APK file

Alternatively, check the **Actions** tab to download the APK artifact from the completed workflow run.

---

## Local Signing (Optional)

For local development, you can set up signing without using GitHub Actions:

1. Create a `keystore.properties` file in the root directory:
   ```properties
   storeFile=path/to/your/keystore.jks
   storePassword=your_keystore_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```

2. Add `keystore.properties` to `.gitignore` (already included if you follow security practices)

3. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```

4. Find your signed APK at:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

---

## Troubleshooting

### Workflow fails with "Keystore not found"
- Verify that `KEYSTORE_BASE64` secret is correctly set
- Make sure the Base64 encoding was done correctly

### Signing fails with "incorrect password"
- Double-check your `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` secrets
- Ensure there are no extra spaces in the secret values

### Gradle wrapper not found
- Make sure you've completed Step 1 and pushed the wrapper jar

### Build fails with Gradle errors
- Ensure you're using the correct Gradle version (8.5)
- Try running `./gradlew clean build` locally first

---

## Next Steps

- Update `versionCode` and `versionName` in `app/build.gradle` for each new release
- Consider setting up automated version bumping
- Add more build variants (debug, staging, release) as needed
- Configure ProGuard/R8 for code obfuscation in production builds

---

## Security Best Practices

✅ **DO:**
- Keep your keystore file safe and backed up
- Use GitHub Secrets for sensitive information
- Use strong passwords for your keystore
- Regularly update your signing certificates before they expire

❌ **DON'T:**
- Never commit keystore files to version control
- Never share your keystore passwords publicly
- Don't use the same keystore for multiple apps
- Don't lose your keystore (you won't be able to update your app!)

---

## Support

If you encounter issues, check:
- GitHub Actions logs in the Actions tab
- Android build logs for detailed error messages
- This repository's Issues page

For more information, see:
- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

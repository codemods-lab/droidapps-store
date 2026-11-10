// Paste into an app's app/build.gradle.kts so templates/release.yml can sign it
// and set its version from the git tag. Same keystore for every app.

android {
    defaultConfig {
        // v1.2.3 tag -> versionName "1.2.3", versionCode 10203 (set by release.yml)
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            System.getenv("KEYSTORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Always the release key, never debug. Without KEYSTORE_FILE a release build fails.
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

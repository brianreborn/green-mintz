pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "green-mintz"
include(":domain")
include(":broker")

val prop = providers.gradleProperty("includeApp").orNull
val env = providers.environmentVariable("INCLUDE_ANDROID_APP").orNull
val sdkEnv = !providers.environmentVariable("ANDROID_SDK_ROOT").orNull.isNullOrBlank() ||
    !providers.environmentVariable("ANDROID_HOME").orNull.isNullOrBlank()
val localProps = file("local.properties").isFile
val includeApp = when {
    prop != null -> prop.equals("true", ignoreCase = true) || prop == "1"
    env != null -> !(env == "0" || env.equals("false", ignoreCase = true))
    else -> localProps || sdkEnv
}

if (includeApp) {
    include(":app")
} else {
    println("NOTE: Skipping :app (no Android SDK). Domain-only. scripts/bootstrap-android-sdk.sh then ./gradlew :app:assembleDebug")
}

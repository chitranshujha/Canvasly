// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Configure common settings for all subprojects
subprojects {
    // Apply Compose configuration to Android application modules
    pluginManager.withPlugin("com.android.application") {
        // Configure Android build features
        configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension> {
            buildFeatures {
                compose = true
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.8"
            }
        }
        
        // Add Compose BOM
        dependencies {
            add("implementation", platform("androidx.compose:compose-bom:2024.01.00"))
        }
    }
}
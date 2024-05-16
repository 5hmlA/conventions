
plugins {
    alias(libs.plugins.android.library) apply true
    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.kotlin.jvm) apply false
//    alias(libs.plugins.kotlin.serialization) apply false
//    alias(libs.plugins.hilt) apply false
//    alias(libs.plugins.ksp) apply false
//    alias(libs.plugins.protobuf) apply false
    id("io.github.5hmlA.android")
    id("io.github.5hmlA.knife")
}

knife {
    println("--knife ->------- build config")
    onVariants {
        if (it.name.contains("debug")) {
            onArtifactBuilt {
                copy {
                    //copy apk to rootDir
                    from(it)
                    //into a directory
                    into(rootDir.absolutePath)
                }
            }
        }
    }
}

android {
    namespace = "com.osp.lib"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
    }
}
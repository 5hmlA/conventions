plugins {
    //之所以要[apply true]是因为没在顶层build.gradle中apply所以这里需要
    alias(libs.plugins.android.application) apply true
    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.hilt) apply false
//    alias(libs.plugins.ksp) apply false
    id("io.github.5hmlA.android")
    id("io.github.5hmlA.protobuf")
    id("io.github.5hmlA.knife")
}

knife {
    onVariants { variants ->
        if (variants.name.contains("debug")) {
            knifeActions {
                asmTransform {
                    configs(
                        "com.osp.app.MainActivity#testChange#?=>java/io/PrintStream#println#*->hello/change",
                        "com.osp.app.MainActivity#testChange#?=>?#testRemove#*",
                        "com.osp.app.RemoveAllMethod#*#*",
                        "com.osp.app.MainActivity#testEmpty#*",
                        "com.osp.app.MainActivity#onCreate#*=>*#testRemove#*",
                    )
                }
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
}

android {
    namespace = "com.osp.app"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }

    androidComponents {
        onVariants { variants ->
            println("--androidComponents ->------- build config $variants")
        }
    }
}

//dependencies {
//    implementation(project(":lib-test"))
//}
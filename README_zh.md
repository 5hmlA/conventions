# conventions plugin for gradle

![](https://img.shields.io/badge/Android-Plugins-brightgreen.svg)

[![License](https://img.shields.io/badge/LICENSE-Apache%202-green.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)

![](https://img.shields.io/badge/Android%20Gradle%20Plugin-8.3+-lightgreen.svg)

# 介绍
一些简化配置gradle项目的插件，可以提高gradle项目配置的效率，特别是在多模块项目中，能够统一各个模块的一致性，且支持配置具体依赖的版本，一处修改全局生效

# 开始
## 首先你必须启用*version catalog*
- 你可以从[android/nowinandroid](https://github.com/android/nowinandroid)获取(当然也可以从本项目获取) ```libs.versions.toml```文件配置到你项目的gradle目录下
- 你可以自定义修改 ```libs.versions.toml```中的版本号，注意只能修改版本号
## 插件使用
### 1，为Android项目配置compose能力，添加此插件后即可在项目中使用compose
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
}

android {
    namespace = "yor applicationId"
}
```
在使用此插件之前你必须配置如下, 多模块的项目中每个模块都要这么配置
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    namespace = "yor applicationId"
}

dependencies {
    //一些android项目必要的依赖
    ...
    //compose 必要的依赖
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 2， Android项目的基础配置，添加如下插件即可，此插件会自动添加Android项目必要的依赖
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

### 3， 为gradle项目配置protobuf, 配置此插件之后即可在项目中正常使用protobuf不需要额外配置
```kotlin
plugins {
    id("io.github.5hmlA.protobuf")
}
```

### 4，组合使用插件，android项目同时使用compose和protobuf
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
    id("io.github.5hmlA.protobuf")
}
```

#### 5 简化AGP api的使用，隔离AGP不同版本api的差异
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.knife")
}
```
如果你要监听Android项目中apk的生成，然后做一些操作比如备份apk，重签名等
使用 knife 插件简化如下, 如果使用agp你必须通过自定义Task完成
```kotlin
knife {
    onVariants {
        //配置只在release上备份apk
        if (it.name.contains("release")) {
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
```
>todo更多简化api正在持续开发中..

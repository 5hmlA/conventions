# conventions plugin for gradle

# Summary
一些简化配置gradle项目的插件，可以提高gradle项目配置的效率，特别是在多模块项目中，能够统一各个模块的一致性，且支持配置具体依赖的版本，一处修改全局生效

# Getting Start
## 首先你必须启用*version catalog*
- 你可以从[android/nowinandroid](https://github.com/android/nowinandroid)获取 ```libs.versions.toml```文件配置到你项目的gradle目录下
- 你可以自定义修改 ```libs.versions.toml```中的版本号，注意只能修改版本号
## 插件使用
##### 1，为Android项目配置compose能力，添加此插件后即可在项目中使用compose
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

##### 2， Android项目的基础配置，添加如下插件即可，此插件会自动添加Android项目必要的依赖
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

##### 3， 为gradle项目配置protobuf, 配置此插件之后即可在项目中正常使用protobuf不需要额外配置
```kotlin
plugins {
    id("io.github.5hmlA.protobuf")
}
```

##### 4，组合使用插件，android项目同时使用compose和protobuf
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
    id("io.github.5hmlA.protobuf")
}
```

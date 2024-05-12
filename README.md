# conventions
### *一些gradle项目的公共配置逻辑封装的插件*

##### 1， Android项目的基础配置，添加如下插件即可，此插件会自动添加Android项目必要的依赖
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android")
}
```

##### 2， 为Android项目配置compose能力，添加次插件后即可在项目中使用compose
```kotlin
plugins {
    id("com.android.application")
    id("io.github.5hmlA.android.compose")
}
```

##### 3， 为gradle项目配置protobuf, 配置此插件之后即可在项目中正常使用protobuf不需要额外配置了
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
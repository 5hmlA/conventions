/*
 * Copyright 2023 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

@file:Suppress("UNCHECKED_CAST")

package wing

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.VariantDimension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.getByType
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.jvm.optionals.getOrNull
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


fun Project.log(msg: String) {
    //🎉 📣 🎗️ 🔥 📜 💯 📸 🎲 🚀 💡  🔔

    //    println("🎗️ $name >>> $msg".yellow)
    println("📣 $name -> $msg".yellow)
}

internal val Project.vlibs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Project.vWings
    get(): VersionCatalog? = extensions.getByType<VersionCatalogsExtension>().find("wings").getOrNull()

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidCommonExtension = CommonExtension<*, *, *, *, *, *>

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidComponentsExtensions = AndroidComponentsExtension<CommonExtension<*, *, *, *, *, *>, *, *>

/**
 * @deprecated Use {@code androidComponents} instead
 */
val Project.androidExtension
    get(): AndroidCommonExtension? = extensions.findByName("android") as? AndroidCommonExtension

val Project.androidExtensionComponent
    get(): AndroidComponentsExtensions? = extensions.findByName("androidComponents") as? AndroidComponentsExtensions

val Project.isAndroidApplication
    get(): Boolean = androidExtension is ApplicationExtension

val Project.isAndroidApp
    get(): Boolean = androidExtensionComponent is ApplicationAndroidComponentsExtension

val Project.isAndroidLibrary
    get(): Boolean = androidExtension is LibraryExtension

val Project.isAndroidLib
    get(): Boolean = androidExtensionComponent is LibraryAndroidComponentsExtension

fun VariantDimension.defineStr(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}

fun VariantDimension.defineBool(name: String, value: Boolean) {
    buildConfigField("boolean", name, value.toString())
}

fun VariantDimension.defineInt(name: String, value: Int) {
    buildConfigField("int", name, value.toString())
}

fun VariantDimension.defineFloat(name: String, value: Int) {
    buildConfigField("float", name, value.toString())
}

fun VariantDimension.defineResStr(name: String, value: String) {
    //使用方式 getResources().getString(R.string.name) 值为value
    resValue("string", name, value)
}

fun VariantDimension.defineResInt(name: String, value: String) {
    //使用方式 getResources().getInteger(R.string.name) 值为value
    resValue("integer", name, value)
}

fun VariantDimension.defineResBool(name: String, value: String) {
    //使用方式 getResources().getBoolean(R.string.name) 值为value
    resValue("bool", name, value)
}

fun Project.changeAPkName(name: String) {
    setProperty("archivesBaseName", name)
}

fun Collection<*>.toStr(): String {
    return toTypedArray().contentToString()
}

fun RepositoryHandler.chinaRepos() {
    maven {
        name = "tencent"
        isAllowInsecureProtocol = true
        setUrl("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
    google()
    mavenCentral()
    maven {
        name = "5hmlA"
        isAllowInsecureProtocol = true
        setUrl("https://maven.pkg.github.com/5hmlA/sparkj")
        credentials {
            // https://www.sojson.com/ascii.html
            username = "5hmlA"
            password = "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"
        }
    }
}

fun java.nio.file.Path.isGradleProject(): Boolean = if (!isDirectory()) false else listDirectoryEntries().any {
    it.toString() == "build.gradle.kts"
}

class ProjectRead(project: Project) : ReadOnlyProperty<Project, String> {
    override fun getValue(thisRef: Project, property: KProperty<*>): String {
        return thisRef.properties[property.name]?.toString() ?: System.getenv(property.name)
    }
}

fun Project.prop(name: String) {}

val a: Boolean by lazy { true }
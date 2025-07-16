import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}
val propsFile = file("public.properties")
val props = Properties()
if (propsFile.exists()) {
    propsFile.inputStream().use { props.load(it) }
}

props.forEach {
    extra[it.key.toString()] = it.value
}
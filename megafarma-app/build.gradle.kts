plugins {
    // Se declaran aqui una sola vez para que todos los modulos usen la misma version.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    id("org.sonarqube") version "7.6.0.18749"
}

sonar {
    properties {
        property("sonar.projectKey", "megafarma-app")
        property("sonar.projectName", "Megafarma App")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.exclusions", "**/build/**,**/generated/**,**/iosApp/**")
    }
}

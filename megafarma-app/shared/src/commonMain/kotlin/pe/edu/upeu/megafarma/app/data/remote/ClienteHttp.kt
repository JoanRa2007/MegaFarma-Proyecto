package pe.edu.upeu.megafarma.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.megafarma.app.data.local.SesionManager

/* Cada plataforma usa su motor HTTP: OkHttp (Android), Darwin (iOS), fetch del navegador (Web). */
expect fun crearHttpClientDePlataforma(configurar: HttpClientConfig<*>.() -> Unit): HttpClient

/*
 * Cliente Ktor con sus plugins:
 *  - ContentNegotiation: convierte JSON <-> objetos Kotlin.
 *  - Logging: muestra en consola cada peticion (metodo, URL y codigo).
 *  - HttpTimeout: si el servidor no responde en 15 s, se corta.
 *  - defaultRequest: agrega "Authorization: Bearer <token>" a cada peticion si hay sesion.
 */
fun crearHttpClient(sesion: SesionManager, json: Json): HttpClient = crearHttpClientDePlataforma {
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
    defaultRequest {
        sesion.token?.let { bearerAuth(it) }
    }
}

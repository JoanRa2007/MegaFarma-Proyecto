package pe.edu.upeu.megafarma.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun crearHttpClientDePlataforma(configurar: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) { configurar() }

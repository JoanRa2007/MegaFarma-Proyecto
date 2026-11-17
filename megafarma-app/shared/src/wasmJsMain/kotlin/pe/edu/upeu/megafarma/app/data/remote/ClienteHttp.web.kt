package pe.edu.upeu.megafarma.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

actual fun crearHttpClientDePlataforma(configurar: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js) { configurar() }

package pe.edu.upeu.megafarma.app

import androidx.compose.ui.window.ComposeUIViewController
import pe.edu.upeu.megafarma.app.di.initKoin

private var koinIniciado = false

/* Punto de entrada en iOS: lo llama ContentView.swift. */
fun MainViewController() = ComposeUIViewController(
    configure = {
        if (!koinIniciado) {
            initKoin()
            koinIniciado = true
        }
    }
) { App() }

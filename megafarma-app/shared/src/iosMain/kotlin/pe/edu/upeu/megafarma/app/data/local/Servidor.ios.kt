package pe.edu.upeu.megafarma.app.data.local

/* El simulador de iOS comparte la red de la Mac: el backend esta en localhost. */
actual fun servidorPorDefecto(): String = "http://localhost:8080"

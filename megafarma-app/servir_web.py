"""Sirve la pagina web ya compilada en http://localhost:5173.

Uso (desde la carpeta megafarma-app, despues de compilar la web):
    .\\gradlew.bat :shared:wasmJsBrowserDistribution
    python servir_web.py
"""
import functools
import http.server
import mimetypes
import os

CARPETA = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "shared", "build", "dist", "wasmJs", "productionExecutable")
PUERTO = 5173  # puerto permitido por CORS en el backend

# El navegador solo ejecuta WebAssembly si llega con este tipo.
mimetypes.add_type("application/wasm", ".wasm")

if not os.path.isdir(CARPETA):
    raise SystemExit("Primero compile la web: .\\gradlew.bat :shared:wasmJsBrowserDistribution")

manejador = functools.partial(http.server.SimpleHTTPRequestHandler, directory=CARPETA)
manejador.func.extensions_map[".wasm"] = "application/wasm"

print(f"Megafarma web en http://localhost:{PUERTO}  (Ctrl+C para detener)")
http.server.ThreadingHTTPServer(("0.0.0.0", PUERTO), manejador).serve_forever()

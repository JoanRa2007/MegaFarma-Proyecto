package pe.edu.upeu.megafarma.app.domain.usecase

import pe.edu.upeu.megafarma.app.core.Resultado
import pe.edu.upeu.megafarma.app.core.esFechaValida
import pe.edu.upeu.megafarma.app.domain.model.LineaVenta
import pe.edu.upeu.megafarma.app.domain.model.Lote
import pe.edu.upeu.megafarma.app.domain.model.MedioPago
import pe.edu.upeu.megafarma.app.domain.model.NuevaEntrada
import pe.edu.upeu.megafarma.app.domain.model.Sesion
import pe.edu.upeu.megafarma.app.domain.model.Venta
import pe.edu.upeu.megafarma.app.domain.repository.AuthRepository
import pe.edu.upeu.megafarma.app.domain.repository.InventarioRepository
import pe.edu.upeu.megafarma.app.domain.repository.VentaRepository

/*
 * Casos de uso: una accion del negocio con sus validaciones previas.
 * Validan en la app para avisar rapido; el backend vuelve a validar todo.
 */

class IniciarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke(usuario: String, clave: String, servidor: String): Resultado<Sesion> {
        if (usuario.isBlank() || clave.isBlank()) {
            return Resultado.Error("Ingrese usuario y contraseña")
        }
        if (!servidor.startsWith("http://") && !servidor.startsWith("https://")) {
            return Resultado.Error("La dirección del servidor debe empezar con http:// o https://")
        }
        return repositorio.iniciarSesion(usuario.trim(), clave, servidor.trim().trimEnd('/'))
    }
}

class RegistrarVentaUseCase(private val repositorio: VentaRepository) {
    suspend operator fun invoke(lineas: List<LineaVenta>, medioPago: MedioPago, clave: String): Resultado<Venta> {
        if (lineas.isEmpty()) return Resultado.Error("Agregue al menos un producto")
        if (lineas.any { it.cantidad <= 0 }) return Resultado.Error("Las cantidades deben ser mayores que cero")
        return repositorio.registrar(lineas, medioPago, clave)
    }
}

class RegistrarEntradaUseCase(private val repositorio: InventarioRepository) {
    suspend operator fun invoke(entrada: NuevaEntrada, clave: String): Resultado<Lote> {
        if (entrada.numeroLote.isBlank()) return Resultado.Error("Ingrese el número de lote")
        if (!esFechaValida(entrada.fechaVencimiento)) return Resultado.Error("La fecha debe tener el formato AAAA-MM-DD")
        if (entrada.cantidad <= 0) return Resultado.Error("La cantidad debe ser mayor que cero")
        return repositorio.registrarEntrada(entrada, clave)
    }
}

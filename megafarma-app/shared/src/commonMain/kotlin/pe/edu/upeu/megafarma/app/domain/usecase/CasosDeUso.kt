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

/*
 * Registrar una venta del punto de venta. Valida en la app las mismas reglas que
 * el backend para avisar antes de enviar: carrito, pagos (mixto, vuelto,
 * numero de operacion, 4 digitos de tarjeta) y cliente segun el comprobante.
 */
class RegistrarVentaUseCase(private val repositorio: VentaRepository) {
    suspend operator fun invoke(
        venta: pe.edu.upeu.megafarma.app.domain.model.NuevaVenta,
        total: Double,
        clave: String
    ): Resultado<Venta> {
        val error = validar(venta, total)
        if (error != null) return Resultado.Error(error)
        return repositorio.registrar(venta, clave)
    }

    fun validar(venta: pe.edu.upeu.megafarma.app.domain.model.NuevaVenta, total: Double): String? {
        if (venta.lineas.isEmpty()) return "Agregue al menos un producto"
        if (venta.lineas.any { it.cantidad <= 0 }) return "Las cantidades deben ser mayores que cero"
        if (venta.pagos.isEmpty()) return "Agregue una forma de pago"
        for (p in venta.pagos) {
            if (p.monto <= 0.0) return "Cada pago debe tener un monto mayor que cero"
            val ref = p.referencia?.trim().orEmpty()
            if (p.requiereOperacion && ref.length < 4) return "Ingrese el número de operación de ${p.medio.etiqueta}"
            if (p.medio == MedioPago.TARJETA && !(ref.length == 4 && ref.all { it.isDigit() })) {
                return "Para el pago con tarjeta ingrese los últimos 4 dígitos"
            }
        }
        val otros = venta.pagos.filter { it.medio != MedioPago.EFECTIVO }.sumOf { it.monto }
        if (otros > total + 0.001) return "Los pagos con tarjeta, billetera o transferencia superan el total"
        val pagado = venta.pagos.sumOf { it.monto }
        if (pagado + 0.001 < total) return "El pago no cubre el total de la venta"
        if (venta.tipoComprobante == pe.edu.upeu.megafarma.app.domain.model.TipoComprobante.FACTURA &&
            venta.cliente?.tipoDocumento != "RUC") {
            return "La factura requiere un cliente con RUC"
        }
        if (venta.tipoComprobante == pe.edu.upeu.megafarma.app.domain.model.TipoComprobante.BOLETA &&
            venta.cliente == null && total > 700.0) {
            return "Las boletas mayores a S/ 700.00 deben identificar al cliente (DNI)"
        }
        return null
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

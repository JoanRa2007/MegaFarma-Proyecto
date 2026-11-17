package pe.edu.upeu.megafarma.app.core

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

/* 1234.5 -> "S/ 1,234.50". Se escribe a mano porque String.format no existe en Kotlin comun. */
fun Double.soles(): String {
    val centimos = (this * 100).roundToLong()
    val signo = if (centimos < 0) "-" else ""
    val absoluto = abs(centimos)
    val entero = (absoluto / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val decimales = (absoluto % 100).toString().padStart(2, '0')
    return "S/ $signo$entero.$decimales"
}

/* "2026-11-17T09:14:31.64" -> "2026-11-17 09:14" */
fun String.fechaHora(): String = replace('T', ' ').take(16)

/* Identificador aleatorio para la idempotencia: un reintento con la misma clave no duplica nada. */
fun nuevaClave(): String = List(32) { Random.nextInt(16).toString(16) }.joinToString("")

/* Valida "AAAA-MM-DD" (formato que espera el backend). */
fun esFechaValida(texto: String): Boolean {
    val partes = texto.split("-")
    if (partes.size != 3 || partes[0].length != 4 || partes[1].length != 2 || partes[2].length != 2) return false
    val mes = partes[1].toIntOrNull() ?: return false
    val dia = partes[2].toIntOrNull() ?: return false
    return partes[0].toIntOrNull() != null && mes in 1..12 && dia in 1..31
}

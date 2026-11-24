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

/* 1234.5 -> "1234.50": monto para un campo de texto (sin "S/" ni comas). */
fun Double.montoTexto(): String = soles().removePrefix("S/ ").replace(",", "")

/* Tamaño único de todas las tablas paginadas de la app (10 filas por página). */
const val TAMANIO_TABLA = 10

/* Texto "Mostrando 11-20 de 57" para el pie de una tabla paginada. */
fun rangoMostrado(pagina: Int, tamanio: Int, enPagina: Int, total: Long): String =
    if (total == 0L) "Sin registros"
    else "Mostrando ${pagina * tamanio + 1}–${pagina * tamanio + enPagina} de $total"

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

/*
 * Conversiones de fecha para el calendario (DatePicker). El calendario trabaja con
 * milisegundos en UTC y el backend con "AAAA-MM-DD"; se calcula a mano con el
 * algoritmo de dias civiles para no agregar una libreria solo para esto.
 */
private const val MS_POR_DIA = 86_400_000L

fun fechaDesdeMillis(ms: Long): String {
    val z = ms.floorDiv(MS_POR_DIA) + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val dia = doy - (153 * mp + 2) / 5 + 1
    val mes = if (mp < 10) mp + 3 else mp - 9
    val anio = yoe + era * 400 + (if (mes <= 2) 1 else 0)
    return "$anio-${mes.toString().padStart(2, '0')}-${dia.toString().padStart(2, '0')}"
}

fun millisDesdeFecha(texto: String): Long? {
    if (!esFechaValida(texto)) return null
    val (a, m, d) = texto.split("-").map { it.toLong() }
    val y = if (m <= 2) a - 1 else a
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return (era * 146_097 + doe - 719_468) * MS_POR_DIA
}

/* "2026-11-23" -> "23/11/2026" (como se lee en Peru). */
fun String.fechaCorta(): String {
    val p = split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else this
}

package pe.edu.upeu.megafarma.app.core

/*
 * Resultado de una operacion que puede fallar (sealed = lista cerrada de casos).
 * Toda llamada a la API devuelve Exito con los datos o Error con un mensaje
 * listo para mostrar. Asi la interfaz nunca recibe excepciones sueltas.
 */
sealed interface Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>
    data class Error(val mensaje: String, val codigo: Int? = null) : Resultado<Nothing>
}

/* Transforma los datos de un Exito y deja pasar el Error sin tocarlo. */
inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(transformar(datos))
    is Resultado.Error -> this
}

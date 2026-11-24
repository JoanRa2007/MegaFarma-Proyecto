package pe.edu.upeu.megafarma.app

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.upeu.megafarma.app.data.local.AlmacenLocal

/*
 * Android guarda la sesion en SharedPreferences privadas de la app, pero cifrada:
 * cada valor se cifra con AES-GCM usando una clave que vive en el Android Keystore
 * (no se puede extraer del telefono). Asi el token no queda en texto plano en el archivo XML.
 * Si encuentra un valor antiguo sin cifrar, lo lee igual y se cifra la proxima vez que se guarde.
 */
class AlmacenAndroid(contexto: Context) : AlmacenLocal {
    private val prefs = contexto.getSharedPreferences("megafarma", Context.MODE_PRIVATE)

    override fun leer(clave: String): String? {
        val guardado = prefs.getString(clave, null) ?: return null
        if (!guardado.startsWith(PREFIJO)) return guardado
        return try {
            CifradoKeystore.descifrar(guardado.removePrefix(PREFIJO))
        } catch (e: Exception) {
            null // clave del Keystore perdida (por ejemplo, datos restaurados): se pide iniciar sesion
        }
    }

    override fun guardar(clave: String, valor: String) {
        prefs.edit().putString(clave, PREFIJO + CifradoKeystore.cifrar(valor)).apply()
    }

    override fun borrar(clave: String) = prefs.edit().remove(clave).apply()

    private companion object {
        const val PREFIJO = "v1:"
    }
}

/* AES-256-GCM con una clave del Android Keystore. Resultado en Base64: IV (12 bytes) + texto cifrado. */
private object CifradoKeystore {
    private const val ALIAS = "megafarma_sesion"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"

    private fun clave(): javax.crypto.SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? java.security.KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = javax.crypto.KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        return generador.generateKey()
    }

    fun cifrar(texto: String): String {
        val cipher = javax.crypto.Cipher.getInstance(TRANSFORMACION)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, clave())
        val datos = cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(datos, android.util.Base64.NO_WRAP)
    }

    fun descifrar(base64: String): String {
        val datos = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
        val cipher = javax.crypto.Cipher.getInstance(TRANSFORMACION)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, clave(),
            javax.crypto.spec.GCMParameterSpec(128, datos, 0, 12))
        return String(cipher.doFinal(datos, 12, datos.size - 12), Charsets.UTF_8)
    }
}

val moduloAndroid = module {
    single<AlmacenLocal> { AlmacenAndroid(androidContext()) }
}

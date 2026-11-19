package pe.edu.upeu.megafarma.util;

/*
 * PARA ESTUDIAR · UTILIDADES (util)
 * Convierte un importe a letras para el comprobante, como exige SUNAT:
 * 37.50 -> "TREINTA Y SIETE CON 50/100 SOLES".
 */

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetras() {
    }

    public static String soles(BigDecimal importe) {
        BigDecimal redondeado = importe.setScale(2, RoundingMode.HALF_UP);
        long entero = redondeado.longValue();
        int centimos = redondeado.subtract(BigDecimal.valueOf(entero)).movePointRight(2).intValue();
        String letras = entero == 0 ? "CERO" : convertir(entero);
        return letras + " CON " + String.format("%02d", centimos) + "/100 SOLES";
    }

    private static String convertir(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            String prefijo = millones == 1 ? "UN MILLON" : convertir(millones) + " MILLONES";
            long resto = n % 1_000_000;
            return resto == 0 ? prefijo : prefijo + " " + convertir(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            String prefijo = miles == 1 ? "MIL" : convertir(miles) + " MIL";
            long resto = n % 1000;
            return resto == 0 ? prefijo : prefijo + " " + convertir(resto);
        }
        if (n >= 100) {
            if (n == 100) {
                return "CIEN";
            }
            String prefijo = CENTENAS[(int) (n / 100)];
            long resto = n % 100;
            return resto == 0 ? prefijo : prefijo + " " + convertir(resto);
        }
        if (n < 30) {
            return UNIDADES[(int) n];
        }
        String decena = DECENAS[(int) (n / 10)];
        long unidad = n % 10;
        return unidad == 0 ? decena : decena + " Y " + UNIDADES[(int) unidad];
    }
}

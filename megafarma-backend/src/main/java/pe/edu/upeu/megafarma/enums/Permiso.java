package pe.edu.upeu.megafarma.enums;

/*
 * PARA ESTUDIAR · ENUM
 * Lista cerrada de valores posibles. En la base se guarda el nombre como texto.
 */

/* Permisos finos. Cada rol tiene un conjunto (ver PermisosPorRol). */
// enum: tipo con una lista fija de valores posibles
public enum Permiso {
    CATALOGO_LEER,
    CATALOGO_ESCRIBIR,
    INVENTARIO_LEER,
    INVENTARIO_ESCRIBIR,
    CLIENTES_LEER,
    CLIENTES_ESCRIBIR,
    VENTAS_LEER,
    VENTAS_REGISTRAR,
    VENTAS_ANULAR,
    REPORTES_VER,
    USUARIOS_ADMINISTRAR
}

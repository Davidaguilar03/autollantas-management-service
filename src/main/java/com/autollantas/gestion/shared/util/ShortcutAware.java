package com.autollantas.gestion.shared.util;

/**
 * Implementado por los controladores de vistas tipo "listado" para que los
 * atajos de teclado globales (Ctrl+N, Ctrl+E, F5, Supr, etc.) sepan a qué
 * acción mapear en la vista actualmente cargada.
 *
 * Todos los métodos tienen implementación por defecto vacía: cada controlador
 * sólo sobreescribe los que tengan sentido para su vista.
 */
public interface ShortcutAware {

    /** Ctrl+N — Crear un registro nuevo. */
    default void shortcutNuevo() {}

    /** Ctrl+E — Editar el registro seleccionado. */
    default void shortcutEditar() {}

    /** Supr / Delete — Eliminar o anular el registro seleccionado. */
    default void shortcutEliminar() {}

    /** Enter — Ver el detalle del registro seleccionado. */
    default void shortcutVerDetalle() {}

    /** Ctrl+F — Poner el foco en el primer campo de búsqueda/filtro. */
    default void shortcutBuscar() {}

    /** Ctrl+L / Esc — Limpiar los filtros de búsqueda. */
    default void shortcutLimpiarFiltros() {}

    /** F5 — Recargar los datos desde la base de datos. */
    default void shortcutRefrescar() {}

    /** Ctrl+S — Guardar el formulario (sólo vistas tipo formulario). */
    default void shortcutGuardar() {}

    /** Esc — Cancelar/cerrar el formulario (sólo vistas tipo formulario). */
    default void shortcutCancelar() {}
}

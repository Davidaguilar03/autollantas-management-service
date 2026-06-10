package com.autollantas.gestion.shared.util;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/**
 * Tabla base de la aplicacion.
 *
 * <p>Todas las tablas de datos (listados, historiales, alertas, etc.) deben
 * heredar de esta clase para garantizar un estilo uniforme. Aplica
 * automaticamente la clase CSS {@code data-table}, definida una sola vez en
 * {@code styles.css}, de modo que el encabezado y las filas comparten la misma
 * apariencia en todo el programa.</p>
 *
 * <p>Las tablas con edicion en linea (formularios de venta/compra) usan su
 * propio estilo especifico y no necesitan heredar de aqui.</p>
 *
 * @param <S> tipo de los elementos que muestra la tabla
 */
public class StyledTableView<S> extends TableView<S> {

    /** Clase CSS compartida por todas las tablas de datos. */
    public static final String STYLE_CLASS = "data-table";

    public StyledTableView() {
        super();
        getStyleClass().add(STYLE_CLASS);
    }

    public StyledTableView(ObservableList<S> items) {
        super(items);
        getStyleClass().add(STYLE_CLASS);
    }
}

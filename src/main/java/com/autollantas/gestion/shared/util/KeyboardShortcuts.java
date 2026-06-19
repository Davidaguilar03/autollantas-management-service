package com.autollantas.gestion.shared.util;

import com.autollantas.gestion.shared.controller.MainLayoutController;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * Instala los atajos de teclado globales de la aplicación sobre la escena
 * principal. Se llama una sola vez (desde la pantalla principal) y los atajos
 * funcionan en cualquier vista cargada en el área de contenido.
 *
 * Los atajos de "acción" (Nuevo, Editar, Eliminar, Buscar, Refrescar, Detalle)
 * se delegan al controlador de la vista activa siempre que implemente
 * {@link ShortcutAware}. Los de navegación llaman directamente al
 * {@link MainLayoutController}.
 *
 * Convención usada (estilo Windows / ofimática):
 *   Ctrl+N  Nuevo registro
 *   Ctrl+E  Editar seleccionado
 *   Supr    Eliminar / anular seleccionado
 *   Ctrl+F  Ir a la búsqueda
 *   Ctrl+L  Limpiar filtros
 *   F5      Refrescar datos
 *   Enter   Ver detalle (cuando hay una fila seleccionada)
 *
 *   Alt+Inicio  Panel de control (Dashboard)
 *   Ctrl+1      Ventas        Ctrl+2  Recaudos      Ctrl+3  Ingreso ocasional
 *   Ctrl+4      Compras       Ctrl+5  Pagos         Ctrl+6  Costos operativos
 *   Ctrl+7      Productos     Ctrl+8  Alertas       Ctrl+9  Impuestos
 *   Ctrl+0      Cuentas
 *
 *   F1          Ayuda de atajos
 *   Ctrl+Q      Cerrar sesión
 */
public final class KeyboardShortcuts {

    private KeyboardShortcuts() {}

    public static void install(Scene scene) {
        if (scene == null) return;

        // ---- Atajos de navegación / globales (accelerators) ----------------
        var acc = scene.getAccelerators();
        MainLayoutController main = MainLayoutController.getInstance();

        acc.put(KeyCombination.keyCombination("Alt+Home"),      () -> main.fireDashboard());
        acc.put(KeyCombination.keyCombination("Shortcut+1"),    () -> main.fireVentas());
        acc.put(KeyCombination.keyCombination("Shortcut+2"),    () -> main.fireRecaudos());
        acc.put(KeyCombination.keyCombination("Shortcut+3"),    () -> main.fireIngresoOcasional());
        acc.put(KeyCombination.keyCombination("Shortcut+4"),    () -> main.fireCompras());
        acc.put(KeyCombination.keyCombination("Shortcut+5"),    () -> main.firePagos());
        acc.put(KeyCombination.keyCombination("Shortcut+6"),    () -> main.fireCostosOperativos());
        acc.put(KeyCombination.keyCombination("Shortcut+7"),    () -> main.fireProductos());
        acc.put(KeyCombination.keyCombination("Shortcut+8"),    () -> main.fireAlertas());
        acc.put(KeyCombination.keyCombination("Shortcut+9"),    () -> main.fireImpuestos());
        acc.put(KeyCombination.keyCombination("Shortcut+0"),    () -> main.fireCuentas());

        acc.put(KeyCombination.keyCombination("F1"),            KeyboardShortcuts::showHelp);
        acc.put(KeyCombination.keyCombination("Shortcut+Q"),    () -> main.fireCerrarSesion());

        // ---- Atajos de acción sobre la vista activa (key filter) -----------
        // Se usa un filtro para poder respetar el foco: si el usuario está
        // escribiendo en un campo de texto, no disparamos acciones con teclas
        // "sueltas" como Supr o Enter.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> handleActionKeys(scene, ev));
    }

    private static void handleActionKeys(Scene scene, KeyEvent ev) {
        Object ctrlObj = MainLayoutController.getInstance().getActiveViewController();
        if (!(ctrlObj instanceof ShortcutAware view)) return;

        boolean shortcut = ev.isShortcutDown();   // Ctrl en Windows
        KeyCode code = ev.getCode();
        boolean typing = isTypingTarget(ev.getTarget());

        // Ctrl + tecla: siempre activos (incluso escribiendo)
        if (shortcut) {
            switch (code) {
                case S -> { view.shortcutGuardar();         ev.consume(); }
                case N -> { view.shortcutNuevo();          ev.consume(); }
                case E -> { view.shortcutEditar();          ev.consume(); }
                case F -> { view.shortcutBuscar();          ev.consume(); }
                case L -> { view.shortcutLimpiarFiltros();  ev.consume(); }
                default -> {}
            }
            return;
        }

        // Teclas sueltas: sólo si NO estamos escribiendo en un campo
        switch (code) {
            case F5     -> { view.shortcutRefrescar(); ev.consume(); }
            case ESCAPE -> { view.shortcutCancelar(); /* no consumimos: dejar cerrar combos/popups */ }
            case DELETE -> { if (!typing) { view.shortcutEliminar();  ev.consume(); } }
            case ENTER  -> { if (!typing) { view.shortcutVerDetalle(); ev.consume(); } }
            default     -> {}
        }
    }

    private static boolean isTypingTarget(Object target) {
        if (target instanceof TextInputControl) return true;
        if (target instanceof ComboBox<?> cb && cb.isEditable()) return true;
        if (target instanceof Node n && n.getParent() instanceof ComboBox<?> cb && cb.isEditable()) return true;
        return false;
    }

    // ------------------------------------------------------------------------
    private static void showHelp() {
        Node anchor = MainLayoutController.getInstance().getContentArea();
        if (anchor == null) return;
        CustomDialog.info(anchor, "Atajos de teclado",
            """
            NAVEGACIÓN
            Alt+Inicio  Panel de control      Ctrl+1  Ventas
            Ctrl+2  Recaudos                   Ctrl+3  Ingreso ocasional
            Ctrl+4  Compras                    Ctrl+5  Pagos
            Ctrl+6  Costos operativos          Ctrl+7  Productos
            Ctrl+8  Alertas de stock           Ctrl+9  Impuestos
            Ctrl+0  Cuentas

            ACCIONES (sobre la vista actual)
            Ctrl+N  Nuevo        Ctrl+E  Editar        Supr  Eliminar/Anular
            Ctrl+F  Buscar       Ctrl+L  Limpiar        F5    Refrescar
            Enter   Ver detalle (con una fila seleccionada)

            FORMULARIOS Y VENTANAS
            Enter   Guardar / Confirmar        Esc   Cancelar / Cerrar

            GENERAL
            F1  Esta ayuda       Ctrl+Q  Cerrar sesión
            """,
            null);
    }
}

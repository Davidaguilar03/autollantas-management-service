package com.autollantas.gestion.shared.util;

import com.autollantas.gestion.shared.controller.MainLayoutController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

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
    // Panel de ayuda de atajos
    // ------------------------------------------------------------------------

    private static final Color COLOR_ACENTO = Color.web("#13522d");

    /** Una fila de atajo: combinación de teclas + descripción. */
    private record Shortcut(String[] keys, String desc) {}

    /** Una categoría con su icono, título y lista de atajos. */
    private record Section(String icon, String title, Color accent, Shortcut[] items) {}

    private static Section[] buildSections() {
        return new Section[] {
            new Section("🧭", "Navegación", Color.web("#1d6fb8"), new Shortcut[] {
                new Shortcut(new String[]{"Alt", "Inicio"}, "Panel de control"),
                new Shortcut(new String[]{"Ctrl", "1"}, "Ventas"),
                new Shortcut(new String[]{"Ctrl", "2"}, "Recaudos"),
                new Shortcut(new String[]{"Ctrl", "3"}, "Ingreso ocasional"),
                new Shortcut(new String[]{"Ctrl", "4"}, "Compras"),
                new Shortcut(new String[]{"Ctrl", "5"}, "Pagos"),
                new Shortcut(new String[]{"Ctrl", "6"}, "Costos operativos"),
                new Shortcut(new String[]{"Ctrl", "7"}, "Productos"),
                new Shortcut(new String[]{"Ctrl", "8"}, "Alertas de stock"),
                new Shortcut(new String[]{"Ctrl", "9"}, "Impuestos"),
                new Shortcut(new String[]{"Ctrl", "0"}, "Cuentas"),
            }),
            new Section("⚡", "Acciones de la vista", COLOR_ACENTO, new Shortcut[] {
                new Shortcut(new String[]{"Ctrl", "N"}, "Crear nuevo registro"),
                new Shortcut(new String[]{"Ctrl", "E"}, "Editar el seleccionado"),
                new Shortcut(new String[]{"Supr"}, "Eliminar o anular"),
                new Shortcut(new String[]{"Ctrl", "F"}, "Ir a la búsqueda"),
                new Shortcut(new String[]{"Ctrl", "L"}, "Limpiar filtros"),
                new Shortcut(new String[]{"F5"}, "Refrescar datos"),
                new Shortcut(new String[]{"Enter"}, "Ver detalle del seleccionado"),
            }),
            new Section("📝", "Formularios y ventanas", Color.web("#b8791d"), new Shortcut[] {
                new Shortcut(new String[]{"Ctrl", "S"}, "Guardar (sólo formularios de venta y compra)"),
                new Shortcut(new String[]{"Enter"}, "Confirmar en un diálogo"),
                new Shortcut(new String[]{"Esc"}, "Cancelar o cerrar"),
            }),
            new Section("⌨", "General", Color.web("#7a3ea8"), new Shortcut[] {
                new Shortcut(new String[]{"F1"}, "Mostrar esta ayuda"),
                new Shortcut(new String[]{"Ctrl", "Q"}, "Cerrar sesión"),
            }),
        };
    }

    private static void showHelp() {
        Window owner = ownerWindow();
        if (owner == null) return;

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);
        coverOwner(dialog, owner);

        // Fondo oscuro
        Rectangle dimmer = new Rectangle();
        dimmer.setFill(Color.rgb(10, 18, 35, 0.62));

        VBox card = buildHelpCard(() -> dismiss(dialog));

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setFillWidth(false);
        wrapper.setPadding(new Insets(32, 24, 32, 24));   // margen arriba/abajo y a los lados

        StackPane root = new StackPane(dimmer, wrapper);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");
        root.getStylesheets().add(
                KeyboardShortcuts.class.getResource(
                        "/com/autollantas/gestion/styles/styles.css").toExternalForm());

        Scene scene = new Scene(root, Color.TRANSPARENT);
        dimmer.widthProperty().bind(scene.widthProperty());
        dimmer.heightProperty().bind(scene.heightProperty());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.F1) dismiss(dialog);
        });

        dialog.setScene(scene);
        dialog.show();

        animateIn(root, card);
    }

    private static VBox buildHelpCard(Runnable onClose) {
        // Encabezado
        Label icon = new Label("⌨");
        icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #2b2b2b;");

        Label title = new Label("Atajos de teclado");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");

        Label subtitle = new Label("Trabaja más rápido sin soltar el teclado");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6a5c47;");

        VBox titleBox = new VBox(2, title, subtitle);
        HBox header = new HBox(14, icon, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);

        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setStyle("-fx-background-color: #ece3d6;");

        // Columnas de categorías (se acomodan horizontalmente y bajan si no caben)
        FlowPane columns = new FlowPane();
        columns.setHgap(18);
        columns.setVgap(18);
        columns.setPrefWrapLength(720);
        columns.setAlignment(Pos.TOP_LEFT);
        for (Section s : buildSections()) {
            columns.getChildren().add(buildSectionCard(s));
        }

        ScrollPane scroll = new ScrollPane(columns);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setMaxHeight(560);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Pie con botón
        Button btnClose = new Button("Entendido");
        btnClose.getStyleClass().addAll("dialog-btn-confirm", "dialog-btn-primary");
        btnClose.setDefaultButton(true);
        btnClose.setOnAction(e -> onClose.run());

        Label hint = new Label("Pulsa Esc o F1 para cerrar");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6a5c47;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(12, hint, spacer, btnClose);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(18, header, sep, scroll, footer);
        card.setPadding(new Insets(26, 28, 22, 28));
        card.setMaxWidth(820);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 28, 0, 0, 12);");
        return card;
    }

    private static VBox buildSectionCard(Section s) {
        Label icon = new Label(s.icon());
        icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #2b2b2b;");
        Label title = new Label(s.title());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
        HBox head = new HBox(8, icon, title);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox(8);
        for (Shortcut sc : s.items()) {
            rows.getChildren().add(buildShortcutRow(sc, s.accent()));
        }

        VBox box = new VBox(12, head, rows);
        box.setPadding(new Insets(16, 18, 16, 18));
        box.setPrefWidth(340);
        box.setStyle(
            "-fx-background-color: #faf7f2;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #efe7da; -fx-border-radius: 12; -fx-border-width: 1;");
        return box;
    }

    private static HBox buildShortcutRow(Shortcut sc, Color accent) {
        HBox keys = new HBox(5);
        keys.setAlignment(Pos.CENTER_LEFT);
        keys.setMinWidth(118);
        keys.setPrefWidth(118);
        for (int i = 0; i < sc.keys().length; i++) {
            if (i > 0) {
                Label plus = new Label("+");
                plus.setStyle("-fx-font-size: 11px; -fx-text-fill: #2b2b2b; -fx-font-weight: bold;");
                keys.getChildren().add(plus);
            }
            keys.getChildren().add(buildKeyChip(sc.keys()[i]));
        }

        Label desc = new Label(sc.desc());
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #2b2b2b;");
        desc.setTextFill(Color.web("#2b2b2b"));
        desc.setWrapText(true);
        HBox.setHgrow(desc, Priority.ALWAYS);

        HBox row = new HBox(10, keys, desc);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label buildKeyChip(String key) {
        Label chip = new Label(key);
        chip.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #d9cdb8; -fx-border-width: 1 1 2 1; -fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 3 9 3 9;" +
            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Consolas','Courier New',monospace;" +
            "-fx-text-fill: #2e1a00;");
        return chip;
    }

    // ---- infraestructura del overlay (mismo patrón que CustomDialog) --------

    private static Window ownerWindow() {
        Node anchor = MainLayoutController.getInstance().getContentArea();
        if (anchor == null || anchor.getScene() == null) return null;
        return anchor.getScene().getWindow();
    }

    private static void coverOwner(Stage dialog, Window owner) {
        dialog.setX(owner.getX());
        dialog.setY(owner.getY());
        dialog.setWidth(owner.getWidth());
        dialog.setHeight(owner.getHeight());
        owner.xProperty().addListener((o, a, b) -> dialog.setX(b.doubleValue()));
        owner.yProperty().addListener((o, a, b) -> dialog.setY(b.doubleValue()));
        owner.widthProperty().addListener((o, a, b) -> dialog.setWidth(b.doubleValue()));
        owner.heightProperty().addListener((o, a, b) -> dialog.setHeight(b.doubleValue()));
    }

    private static void animateIn(StackPane root, VBox card) {
        root.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);

        FadeTransition fade = new FadeTransition(Duration.millis(200), root);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
        scale.setFromX(0.9); scale.setFromY(0.9);
        scale.setToX(1); scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, scale).play();
    }

    private static void dismiss(Stage dialog) {
        FadeTransition fade = new FadeTransition(Duration.millis(140), dialog.getScene().getRoot());
        fade.setFromValue(1); fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> dialog.close());
        fade.play();
    }
}

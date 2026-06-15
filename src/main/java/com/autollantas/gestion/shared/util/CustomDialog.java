package com.autollantas.gestion.shared.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Dialogs modernos sin barra de título con overlay oscuro.
 *
 * Uso:
 *   CustomDialog.info(anyNode, "Título", "Mensaje", onClose);
 *   CustomDialog.success(anyNode, "Título", "Mensaje", onClose);
 *   CustomDialog.warning(anyNode, "Título", "Mensaje", onClose);
 *   CustomDialog.confirm(anyNode, "Título", "Mensaje", onConfirm, onCancel);
 *   CustomDialog.danger(anyNode, "Título", "Mensaje", onConfirm, onCancel);
 */
public class CustomDialog {

    public enum Type { INFO, WARNING, DANGER, SUCCESS, CONFIRM }

    private static final Duration ANIM_IN  = Duration.millis(220);
    private static final Duration ANIM_OUT = Duration.millis(160);

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    public static void info(Node anyNode, String title, String message, Runnable onClose) {
        show(anyNode, Type.INFO, title, message, "Entendido", null, onClose, null);
    }

    public static void success(Node anyNode, String title, String message, Runnable onClose) {
        show(anyNode, Type.SUCCESS, title, message, "Perfecto", null, onClose, null);
    }

    public static void warning(Node anyNode, String title, String message, Runnable onClose) {
        show(anyNode, Type.WARNING, title, message, "Entendido", null, onClose, null);
    }

    public static void confirm(Node anyNode, String title, String message,
                               Runnable onConfirm, Runnable onCancel) {
        show(anyNode, Type.CONFIRM, title, message, "Confirmar", "Cancelar", onConfirm, onCancel);
    }

    public static void danger(Node anyNode, String title, String message,
                              Runnable onConfirm, Runnable onCancel) {
        show(anyNode, Type.DANGER, title, message, "Eliminar", "Cancelar", onConfirm, onCancel);
    }

    public static void confirm(Window ownerWindow, String title, String message,
                               Runnable onConfirm, Runnable onCancel) {
        showFromWindow(ownerWindow, Type.CONFIRM, title, message, "Confirmar", "Cancelar", onConfirm, onCancel);
    }

    public static void danger(Window ownerWindow, String title, String message,
                              Runnable onConfirm, Runnable onCancel) {
        showFromWindow(ownerWindow, Type.DANGER, title, message, "Salir", "Cancelar", onConfirm, onCancel);
    }

    public static void custom(Node anyNode, Type type, String title, String message,
                              String confirmLabel, String cancelLabel,
                              Runnable onConfirm, Runnable onCancel) {
        show(anyNode, type, title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    // -------------------------------------------------------------------------
    // Lógica interna
    // -------------------------------------------------------------------------

    private static void showFromWindow(Window owner, Type type, String title, String message,
                                       String confirmLabel, String cancelLabel,
                                       Runnable onConfirm, Runnable onCancel) {
        if (owner == null) return;
        buildAndShow(owner, type, title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    private static void show(Node anyNode, Type type, String title, String message,
                             String confirmLabel, String cancelLabel,
                             Runnable onConfirm, Runnable onCancel) {
        Scene ownerScene = anyNode.getScene();
        if (ownerScene == null) return;
        buildAndShow(ownerScene.getWindow(), type, title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    private static void buildAndShow(Window owner, Type type, String title, String message,
                                     String confirmLabel, String cancelLabel,
                                     Runnable onConfirm, Runnable onCancel) {
        // Stage transparente sin decoración — cubre exactamente la ventana owner
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY());
        dialog.setWidth(owner.getWidth());
        dialog.setHeight(owner.getHeight());

        // Seguir a la ventana si el usuario la mueve
        owner.xProperty().addListener((obs, o, n) -> dialog.setX(n.doubleValue()));
        owner.yProperty().addListener((obs, o, n) -> dialog.setY(n.doubleValue()));
        owner.widthProperty().addListener((obs, o, n)  -> dialog.setWidth(n.doubleValue()));
        owner.heightProperty().addListener((obs, o, n) -> dialog.setHeight(n.doubleValue()));

        Button[] confirmRef = new Button[1];
        VBox card = buildCard(type, title, message, confirmLabel, cancelLabel,
                () -> dismiss(dialog, onConfirm),
                () -> dismiss(dialog, onCancel),
                confirmRef);

        // Fondo oscuro
        Rectangle dimmer = new Rectangle();
        dimmer.setFill(Color.rgb(10, 18, 35, 0.62));

        // VBox con fillWidth=false: la tarjeta mantiene su ancho natural sin estirarse
        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setFillWidth(false);

        // StackPane raíz: dimmer abajo, wrapper centrado arriba
        StackPane root = new StackPane(dimmer, wrapper);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");
        root.getStylesheets().add(
                CustomDialog.class.getResource(
                        "/com/autollantas/gestion/styles/styles.css").toExternalForm());

        Scene scene = new Scene(root, Color.TRANSPARENT);
        dimmer.widthProperty().bind(scene.widthProperty());
        dimmer.heightProperty().bind(scene.heightProperty());

        dialog.setScene(scene);
        dialog.show();
        Platform.runLater(() -> confirmRef[0].requestFocus());

        animateIn(root, card);
    }

    private static VBox buildCard(Type type, String title, String message,
                                  String confirmLabel, String cancelLabel,
                                  Runnable onConfirm, Runnable onCancel,
                                  Button[] confirmRef) {
        Label icon = new Label(iconFor(type));
        icon.getStyleClass().addAll("dialog-icon", iconStyleClass(type));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(300);

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("dialog-message");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);

        VBox header = new VBox(12, icon, titleLabel);
        header.setAlignment(Pos.CENTER);

        HBox buttons = buildButtons(type, confirmLabel, cancelLabel, onConfirm, onCancel, confirmRef);

        VBox card = new VBox(20, header, msgLabel, buttons);
        card.getStyleClass().addAll("dialog-card", cardAccentClass(type));
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(360);
        card.setMaxWidth(500);
        VBox.setMargin(card, new Insets(0));

        return card;
    }

    private static HBox buildButtons(Type type, String confirmLabel, String cancelLabel,
                                     Runnable onConfirm, Runnable onCancel,
                                     Button[] confirmRef) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER);

        if (cancelLabel != null) {
            Button cancel = new Button(cancelLabel);
            cancel.getStyleClass().add("dialog-btn-cancel");
            cancel.setCancelButton(true);
            cancel.setOnAction(e -> onCancel.run());
            box.getChildren().add(cancel);
        }

        Button confirm = new Button(confirmLabel);
        confirm.getStyleClass().addAll("dialog-btn-confirm", confirmBtnClass(type));
        confirm.setDefaultButton(true);
        confirm.setOnAction(e -> onConfirm.run());
        box.getChildren().add(confirm);
        confirmRef[0] = confirm;

        return box;
    }

    private static void animateIn(StackPane root, VBox card) {
        root.setOpacity(0);
        card.setScaleX(0.88);
        card.setScaleY(0.88);

        FadeTransition fadeBg = new FadeTransition(ANIM_IN, root);
        fadeBg.setFromValue(0);
        fadeBg.setToValue(1);
        fadeBg.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleCard = new ScaleTransition(ANIM_IN, card);
        scaleCard.setFromX(0.88);
        scaleCard.setFromY(0.88);
        scaleCard.setToX(1);
        scaleCard.setToY(1);
        scaleCard.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeCard = new FadeTransition(ANIM_IN, card);
        fadeCard.setFromValue(0);
        fadeCard.setToValue(1);

        new ParallelTransition(fadeBg, scaleCard, fadeCard).play();
    }

    private static void dismiss(Stage dialog, Runnable callback) {
        FadeTransition fade = new FadeTransition(ANIM_OUT, dialog.getScene().getRoot());
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> {
            dialog.close();
            if (callback != null) callback.run();
        });
        fade.play();
    }

    // -------------------------------------------------------------------------
    // Helpers de estilo
    // -------------------------------------------------------------------------

    private static String iconFor(Type type) {
        return switch (type) {
            case INFO    -> "ℹ";
            case SUCCESS -> "✓";
            case WARNING -> "!";
            case DANGER  -> "✕";
            case CONFIRM -> "?";
        };
    }

    private static String iconStyleClass(Type type) {
        return switch (type) {
            case INFO    -> "dialog-icon-info";
            case SUCCESS -> "dialog-icon-success";
            case WARNING -> "dialog-icon-warning";
            case DANGER  -> "dialog-icon-danger";
            case CONFIRM -> "dialog-icon-confirm";
        };
    }

    private static String cardAccentClass(Type type) {
        return switch (type) {
            case INFO    -> "dialog-card-info";
            case SUCCESS -> "dialog-card-success";
            case WARNING -> "dialog-card-warning";
            case DANGER  -> "dialog-card-danger";
            case CONFIRM -> "dialog-card-confirm";
        };
    }

    private static String confirmBtnClass(Type type) {
        return switch (type) {
            case INFO, CONFIRM -> "dialog-btn-primary";
            case SUCCESS       -> "dialog-btn-success";
            case WARNING       -> "dialog-btn-warning";
            case DANGER        -> "dialog-btn-danger";
        };
    }
}

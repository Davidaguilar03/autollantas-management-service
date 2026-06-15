package com.autollantas.gestion.shared.util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Queue;

public class ToastNotification {

    public enum Type { SUCCESS, ERROR, WARNING }

    private static final double TOAST_WIDTH   = 360;
    private static final double MARGIN_RIGHT  = 20;
    private static final double MARGIN_TOP    = 16;
    private static final double SLOT_HEIGHT   = 72;
    private static final double GAP           = 10;
    private static final int    MAX_VISIBLE   = 3;

    private static final double BORDER_RADIUS = 10;
    private static final double BORDER_WIDTH  = 3;

    private static final Duration SLIDE_IN = Duration.millis(320);
    private static final Duration VISIBLE  = Duration.millis(4700);
    private static final Duration FADE_OUT = Duration.millis(350);

    // Slots fijos 0-1-2: null = libre, Group = ocupado
    private static final Group[]           slots        = new Group[MAX_VISIBLE];
    private static final Queue<PendingToast> pendingQueue = new ArrayDeque<>();

    private record PendingToast(Node node, Type type, String message) {}

    public static void success(Node anyNode, String message) { show(anyNode, Type.SUCCESS, message); }
    public static void error  (Node anyNode, String message) { show(anyNode, Type.ERROR,   message); }
    public static void warning(Node anyNode, String message) { show(anyNode, Type.WARNING,  message); }

    public static void success(StackPane root, String message) { show(root, Type.SUCCESS, message); }
    public static void error  (StackPane root, String message) { show(root, Type.ERROR,   message); }
    public static void warning(StackPane root, String message) { show(root, Type.WARNING,  message); }

    public static void show(Node anyNode, Type type, String message) {
        Platform.runLater(() -> {
            int slot = firstFreeSlot();
            if (slot == -1) {
                pendingQueue.add(new PendingToast(anyNode, type, message));
                return;
            }
            display(anyNode, type, message, slot);
        });
    }

    private static int firstFreeSlot() {
        for (int i = 0; i < MAX_VISIBLE; i++) {
            if (slots[i] == null) return i;
        }
        return -1;
    }

    private static double yForSlot(int slot) {
        return MARGIN_TOP + slot * (SLOT_HEIGHT + GAP);
    }

    private static void display(Node anyNode, Type type, String message, int slot) {
        Scene scene = anyNode.getScene();
        if (scene == null) return;

        Pane overlay = getOrCreateOverlay(scene);

        HBox content = buildContent(type, message);
        content.setPrefWidth(TOAST_WIDTH);
        content.setMaxWidth(TOAST_WIDTH);
        content.applyCss();
        content.layout();

        Rectangle border = new Rectangle();
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web(colorFor(type)));
        border.setStrokeWidth(BORDER_WIDTH);
        border.setStrokeType(StrokeType.INSIDE);
        border.setStrokeLineCap(StrokeLineCap.ROUND);
        border.setArcWidth(BORDER_RADIUS * 2);
        border.setArcHeight(BORDER_RADIUS * 2);
        border.setMouseTransparent(true);

        Group group = new Group(content, border);
        group.setOpacity(0);

        double posY   = yForSlot(slot);
        double startX = scene.getWidth();
        double endX   = scene.getWidth() - TOAST_WIDTH - MARGIN_RIGHT;

        group.setLayoutX(startX);
        group.setLayoutY(posY);

        slots[slot] = group;
        overlay.getChildren().add(group);

        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(group.layoutXProperty(), startX),
                new KeyValue(group.opacityProperty(), 0)
            ),
            new KeyFrame(SLIDE_IN,
                new KeyValue(group.layoutXProperty(), endX),
                new KeyValue(group.opacityProperty(), 1)
            )
        );

        slideIn.setOnFinished(e -> {
            double w = content.getWidth();
            double h = content.getHeight();
            if (w < 1 || h < 1) return;

            border.setWidth(w);
            border.setHeight(h);

            double perim = 2 * (w - BORDER_RADIUS * 2)
                         + 2 * (h - BORDER_RADIUS * 2)
                         + 2 * Math.PI * BORDER_RADIUS;

            border.getStrokeDashArray().setAll(perim);
            border.setStrokeDashOffset(perim);

            Timeline borderFill = new Timeline(
                new KeyFrame(Duration.ZERO,  new KeyValue(border.strokeDashOffsetProperty(), perim)),
                new KeyFrame(VISIBLE,        new KeyValue(border.strokeDashOffsetProperty(), 0))
            );

            FadeTransition fadeOut = new FadeTransition(FADE_OUT, group);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            SequentialTransition seq = new SequentialTransition(borderFill, fadeOut);
            seq.setOnFinished(ev -> {
                overlay.getChildren().remove(group);
                slots[slot] = null;
                PendingToast next = pendingQueue.poll();
                if (next != null) display(next.node(), next.type(), next.message(), slot);
            });
            seq.play();
        });

        slideIn.play();
    }

    private static String colorFor(Type type) {
        return switch (type) {
            case SUCCESS -> "#5cb98a";
            case ERROR   -> "#e07b7b";
            case WARNING -> "#f1b82c";
        };
    }

    private static Pane getOrCreateOverlay(Scene scene) {
        final String OVERLAY_ID = "toast-overlay-pane";
        if (scene.getRoot() instanceof Pane rootPane) {
            for (Node child : rootPane.getChildrenUnmodifiable()) {
                if (OVERLAY_ID.equals(child.getId())) return (Pane) child;
            }
            Pane overlay = new Pane();
            overlay.setId(OVERLAY_ID);
            overlay.setMouseTransparent(true);
            overlay.setPickOnBounds(false);
            overlay.prefWidthProperty().bind(scene.widthProperty());
            overlay.prefHeightProperty().bind(scene.heightProperty());
            rootPane.getChildren().add(overlay);
            return overlay;
        }
        Pane overlay = new Pane();
        overlay.setId(OVERLAY_ID);
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);
        return overlay;
    }

    private static HBox buildContent(Type type, String message) {
        String icon;
        String styleClass;
        switch (type) {
            case SUCCESS -> { icon = "✓"; styleClass = "toast-success"; }
            case ERROR   -> { icon = "✕"; styleClass = "toast-error";   }
            case WARNING -> { icon = "!"; styleClass = "toast-warning";  }
            default      -> { icon = "i"; styleClass = "toast-success";  }
        }

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("toast-icon");

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("toast-message");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(TOAST_WIDTH - 70);

        HBox content = new HBox(10, iconLabel, msgLabel);
        content.getStyleClass().addAll("toast", styleClass);
        content.setAlignment(Pos.CENTER_LEFT);

        return content;
    }
}

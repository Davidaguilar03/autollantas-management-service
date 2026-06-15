package com.autollantas.gestion.shared.controller;

import com.autollantas.gestion.shared.util.CustomDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class DialogTestController {

    @FXML private Label lblResultado;
    @FXML private StackPane root;

    @FXML
    private void showInfo() {
        CustomDialog.info(lblResultado,
                "Información del sistema",
                "Esta es una notificación informativa. El cierre de caja está programado para las 11:59 p.m. de hoy.",
                () -> lblResultado.setText("Dialog Info → cerrado"));
    }

    @FXML
    private void showSuccess() {
        CustomDialog.success(lblResultado,
                "Operación exitosa",
                "El producto fue registrado correctamente en el inventario.",
                () -> lblResultado.setText("Dialog Éxito → cerrado"));
    }

    @FXML
    private void showWarning() {
        CustomDialog.warning(lblResultado,
                "Stock bajo",
                "Llanta Radial 195/65 R15 tiene solo 2 unidades disponibles. Considera hacer un pedido pronto.",
                () -> lblResultado.setText("Dialog Advertencia → cerrado"));
    }

    @FXML
    private void showConfirm() {
        CustomDialog.confirm(lblResultado,
                "Confirmar acción",
                "¿Deseas anular la factura de venta #FV-2024-0312? Esta acción revertirá el inventario asociado.",
                () -> lblResultado.setText("Dialog Confirmar → CONFIRMADO"),
                () -> lblResultado.setText("Dialog Confirmar → cancelado"));
    }

    @FXML
    private void showDanger() {
        CustomDialog.danger(lblResultado,
                "Eliminar producto",
                "Estás a punto de eliminar \"Llanta Michelin 205/55 R16\". Esta acción es permanente y no se puede deshacer.",
                () -> lblResultado.setText("Dialog Danger → CONFIRMADO (eliminado)"),
                () -> lblResultado.setText("Dialog Danger → cancelado"));
    }
}

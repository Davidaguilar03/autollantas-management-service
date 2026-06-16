package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
public class ProductFormController {

    @Autowired
    private InventoryService inventoryService;

    @FXML private Label lblTitulo;
    @FXML private TextField txtCodigo;
    @FXML private ComboBox<ProductCategory> comboCategoria;
    @FXML private TextField txtDescripcion;

    @FXML private TextField txtPurchaseCost;
    @FXML private TextField txtTaxAmount;

    @FXML private Spinner<Integer> spinnerStock;

    private boolean guardado = false;
    private Product currentProduct;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        guardado = false;
        currencyFormat.setMaximumFractionDigits(0);
        spinnerStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));

        txtTaxAmount.setEditable(false);

        configurarComboCategorias();
        configurarLogicaPrecios();

        this.currentProduct = new Product();

        Platform.runLater(() -> txtCodigo.requestFocus());
        aplicarEstilosFocus();
    }

    private void configurarLogicaPrecios() {
        txtPurchaseCost.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                recalcularTodo();
                return;
            }
            String valorLimpio = newVal.replaceAll("[^0-9]", "");
            if (valorLimpio.isEmpty()) return;
            try {
                long numero = Long.parseLong(valorLimpio);
                String formateado = currencyFormat.format(numero);
                if (!newVal.equals(formateado)) {
                    txtPurchaseCost.setText(formateado);
                    Platform.runLater(() -> txtPurchaseCost.positionCaret(txtPurchaseCost.getText().length()));
                }
                recalcularTodo();
            } catch (NumberFormatException e) {
            }
        });

        comboCategoria.valueProperty().addListener((obs, oldVal, newVal) -> recalcularTodo());
    }

    private void recalcularTodo() {
        String costStr = txtPurchaseCost.getText() != null
                ? txtPurchaseCost.getText().replaceAll("[^0-9]", "") : "";

        double precioCompra = 0.0;
        try { if (!costStr.isEmpty()) precioCompra = Double.parseDouble(costStr); } catch (NumberFormatException ignored) {}

        double ivaFavor = precioCompra * 0.19;
        txtTaxAmount.setText(precioCompra > 0 ? currencyFormat.format(ivaFavor) : "$ 0");
    }

    public void setProduct(Product product) {
        this.currentProduct = product;

        if (product.getId() != null) {
            lblTitulo.setText("Editar Producto");
            txtCodigo.setEditable(false);
        } else {
            lblTitulo.setText("Nuevo Producto");
        }

        txtCodigo.setText(product.getCode() != null ? product.getCode() : "");
        txtDescripcion.setText(product.getDescription() != null ? product.getDescription() : "");

        if (product.getPurchaseCost() != null && product.getPurchaseCost() > 0) {
            txtPurchaseCost.setText(currencyFormat.format(product.getPurchaseCost()));
        }

        spinnerStock.getValueFactory().setValue(product.getQuantity() != null ? product.getQuantity() : 0);

        if (product.getCategory() != null) {
            comboCategoria.setValue(product.getCategory());
        }

        recalcularTodo();
    }

    @FXML
    public void guardarProducto() {
        if (!validarCampos()) return;

        if (currentProduct.getId() != null) {
            CustomDialog.confirm(txtCodigo,
                "Guardar cambios",
                "Vas a modificar el producto \"" + currentProduct.getDescription() + "\". "
                    + "Los datos anteriores serán reemplazados y los precios mínimo y sugerido se recalcularán. ¿Confirmas?",
                this::doSave,
                null);
        } else {
            doSave();
        }
    }

    private void doSave() {
        try {
            currentProduct.setCode(txtCodigo.getText());
            currentProduct.setDescription(txtDescripcion.getText());
            currentProduct.setCategory(comboCategoria.getValue());
            currentProduct.setQuantity(spinnerStock.getValue());

            String costStr = txtPurchaseCost.getText().replaceAll("[^0-9]", "");
            double purchaseCost = Double.parseDouble(costStr);
            currentProduct.setPurchaseCost(purchaseCost);

            inventoryService.recalculateMinSalePrice(currentProduct);

            guardado = true;
            cerrarVentana();

        } catch (NumberFormatException e) {
            ToastNotification.warning(txtPurchaseCost, "Revisa que los valores numéricos sean válidos");
        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(txtCodigo, "No se pudo guardar el producto: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        boolean valido = true;
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";

        txtCodigo.setStyle(normalStyle);
        txtDescripcion.setStyle(normalStyle);
        txtPurchaseCost.setStyle(normalStyle);
        comboCategoria.setStyle(normalStyle);

        if (txtCodigo.getText() == null || txtCodigo.getText().trim().isEmpty()) {
            txtCodigo.setStyle(errorStyle); valido = false;
        }
        if (comboCategoria.getValue() == null) {
            comboCategoria.setStyle(errorStyle); valido = false;
        }
        if (txtDescripcion.getText() == null || txtDescripcion.getText().trim().isEmpty()) {
            txtDescripcion.setStyle(errorStyle); valido = false;
        }
        if (txtPurchaseCost.getText() == null || txtPurchaseCost.getText().trim().isEmpty()) {
            txtPurchaseCost.setStyle(errorStyle); valido = false;
        }

        if (!valido) ToastNotification.warning(txtCodigo, "Completa los campos resaltados en rojo antes de continuar");
        return valido;
    }

    private void aplicarEstilosFocus() {
        String estiloNormal = "-fx-background-radius: 4; -fx-border-color: #cccccc; -fx-border-radius: 4;";
        String estiloFocus = "-fx-background-radius: 4; -fx-border-color: #13522d; -fx-border-radius: 4; -fx-border-width: 1.5;";

        configurarEstiloCampo(txtCodigo, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtDescripcion, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtPurchaseCost, estiloNormal, estiloFocus);
        comboCategoria.setStyle(estiloNormal);
    }

    private void configurarEstiloCampo(TextField campo, String normal, String focus) {
        campo.setStyle(normal);
        campo.focusedProperty().addListener((obs, oldVal, newVal) -> campo.setStyle(newVal ? focus : normal));
    }

    private void configurarComboCategorias() {
        List<ProductCategory> categories = inventoryService.findAllCategories();
        comboCategoria.setItems(FXCollections.observableArrayList(categories));
        comboCategoria.setConverter(new StringConverter<ProductCategory>() {
            @Override public String toString(ProductCategory cat) { return cat != null ? cat.getName() : ""; }
            @Override public ProductCategory fromString(String string) { return null; }
        });

        Callback<ListView<ProductCategory>, ListCell<ProductCategory>> cellFactory = lv -> new ListCell<>() {
            private final Label chip = new Label();
            {
                chip.setStyle("-fx-background-radius: 6; -fx-padding: 2 8 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            @Override
            protected void updateItem(ProductCategory cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String color = (cat.getColor() != null && !cat.getColor().isEmpty())
                        ? cat.getColor() : "#94a3b8";
                    chip.setText(cat.getName());
                    chip.setStyle(
                        "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;"
                    );
                    setGraphic(chip);
                    setText(null);
                }
            }
        };
        comboCategoria.setCellFactory(cellFactory);
        comboCategoria.setButtonCell(cellFactory.call(null));
    }

    @FXML
    public void cerrarVentana() {
        if (txtCodigo.getScene() != null) {
            ((Stage) txtCodigo.getScene().getWindow()).close();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

}

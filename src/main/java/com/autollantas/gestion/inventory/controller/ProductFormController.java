package com.autollantas.gestion.inventory.controller;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.service.InventoryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
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
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPorcentajeIva;
    @FXML private TextField txtMontoIva;
    @FXML private TextField txtTotal;
    @FXML private Spinner<Integer> spinnerStock;

    private boolean guardado = false;
    private Product currentProduct;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        spinnerStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));

        configurarComboCategorias();
        configurarLogicaPrecios();

        this.currentProduct = new Product();

        Platform.runLater(() -> txtCodigo.requestFocus());
        aplicarEstilosFocus();
    }

    private void configurarLogicaPrecios() {
        txtPrecioBruto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                calcularMontos();
                return;
            }

            String valorLimpio = newVal.replaceAll("[^0-9]", "");

            if (valorLimpio.isEmpty()) return;

            try {
                long numero = Long.parseLong(valorLimpio);
                String formateado = currencyFormat.format(numero);

                if (!newVal.equals(formateado)) {
                    txtPrecioBruto.setText(formateado);
                    Platform.runLater(() -> txtPrecioBruto.positionCaret(txtPrecioBruto.getText().length()));
                }

                calcularMontos();

            } catch (NumberFormatException e) {
            }
        });

        txtPorcentajeIva.textProperty().addListener((obs, oldVal, newVal) -> calcularMontos());
    }

    private void calcularMontos() {
        try {
            String brutoStr = txtPrecioBruto.getText() != null ? txtPrecioBruto.getText().replaceAll("[^0-9]", "") : "";
            String ivaPorcStr = txtPorcentajeIva.getText() != null ? txtPorcentajeIva.getText().replaceAll(",", ".") : "";

            if (brutoStr.isEmpty()) {
                txtMontoIva.setText("$ 0");
                txtTotal.setText("$ 0");
                return;
            }

            double bruto = Double.parseDouble(brutoStr);
            double porcentaje = ivaPorcStr.isEmpty() ? 0 : Double.parseDouble(ivaPorcStr.replaceAll("[^0-9.]", ""));

            double montoIva = bruto * (porcentaje / 100);
            double total = bruto + montoIva;

            txtMontoIva.setText(currencyFormat.format(montoIva));
            txtTotal.setText(currencyFormat.format(total));

        } catch (NumberFormatException e) {
            txtMontoIva.setText("$ 0");
            txtTotal.setText("$ 0");
        }
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

        if (product.getBasePrice() > 0) {
            txtPrecioBruto.setText(currencyFormat.format(product.getBasePrice()));

            if (product.getTaxAmount() > 0) {
                double porc = (product.getTaxAmount() / product.getBasePrice()) * 100;
                txtPorcentajeIva.setText(String.format("%.0f", porc));
            } else {
                txtPorcentajeIva.setText("0");
            }
        } else {
            txtPrecioBruto.setText("");
            txtPorcentajeIva.setText("19");
        }

        spinnerStock.getValueFactory().setValue(product.getQuantity());

        if (product.getCategory() != null) {
            comboCategoria.setValue(product.getCategory());
        }

        calcularMontos();
    }

    @FXML
    public void guardarProducto() {
        if (!validarCampos()) return;

        try {
            currentProduct.setCode(txtCodigo.getText());
            currentProduct.setDescription(txtDescripcion.getText());
            currentProduct.setCategory(comboCategoria.getValue());

            String brutoLimpio = txtPrecioBruto.getText().replaceAll("[^0-9]", "");
            double bruto = Double.parseDouble(brutoLimpio);

            String porcentajeStr = txtPorcentajeIva.getText().replaceAll("[^0-9.]", "");
            double porcentaje = porcentajeStr.isEmpty() ? 0 : Double.parseDouble(porcentajeStr);

            double montoIva = bruto * (porcentaje / 100);
            double total = bruto + montoIva;

            currentProduct.setBasePrice(bruto);
            currentProduct.setTaxAmount(montoIva);
            currentProduct.setPriceWithTax(total);
            currentProduct.setQuantity(spinnerStock.getValue());

            inventoryService.saveProduct(currentProduct);

            guardado = true;
            cerrarVentana();

        } catch (NumberFormatException e) {
            mostrarAlerta("Error numérico", "Revisa que los precios no sean demasiado grandes.");
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error técnico", "No se pudo guardar: " + e.getMessage());
        }
    }

    private boolean validarCampos() {
        boolean valido = true;
        String errorStyle = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-background-radius: 4;";
        String normalStyle = "-fx-border-color: #cccccc; -fx-border-radius: 4;";

        txtCodigo.setStyle(normalStyle);
        txtDescripcion.setStyle(normalStyle);
        txtPrecioBruto.setStyle(normalStyle);
        comboCategoria.setStyle(normalStyle);

        if (txtCodigo.getText() == null || txtCodigo.getText().trim().isEmpty()) {
            txtCodigo.setStyle(errorStyle);
            valido = false;
        }
        if (comboCategoria.getValue() == null) {
            comboCategoria.setStyle(errorStyle);
            valido = false;
        }
        if (txtDescripcion.getText() == null || txtDescripcion.getText().trim().isEmpty()) {
            txtDescripcion.setStyle(errorStyle);
            valido = false;
        }
        if (txtPrecioBruto.getText() == null || txtPrecioBruto.getText().trim().isEmpty()) {
            txtPrecioBruto.setStyle(errorStyle);
            valido = false;
        }

        if (!valido) {
            mostrarAlerta("Datos Incompletos", "Por favor completa los campos resaltados en rojo.");
        }
        return valido;
    }

    private void aplicarEstilosFocus() {
        String estiloNormal = "-fx-background-radius: 4; -fx-border-color: #cccccc; -fx-border-radius: 4;";
        String estiloFocus = "-fx-background-radius: 4; -fx-border-color: #13522d; -fx-border-radius: 4; -fx-border-width: 1.5;";

        configurarEstiloCampo(txtCodigo, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtDescripcion, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtPrecioBruto, estiloNormal, estiloFocus);
        configurarEstiloCampo(txtPorcentajeIva, estiloNormal, estiloFocus);
        comboCategoria.setStyle(estiloNormal);
    }

    private void configurarEstiloCampo(TextField campo, String normal, String focus) {
        campo.setStyle(normal);
        campo.focusedProperty().addListener((obs, oldVal, newVal) -> {
            campo.setStyle(newVal ? focus : normal);
        });
    }

    private void configurarComboCategorias() {
        List<ProductCategory> categories = inventoryService.findAllCategories();
        comboCategoria.setItems(FXCollections.observableArrayList(categories));
        comboCategoria.setConverter(new StringConverter<ProductCategory>() {
            @Override public String toString(ProductCategory cat) { return cat != null ? cat.getName() : ""; }
            @Override public ProductCategory fromString(String string) { return null; }
        });
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

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.show();
    }
}

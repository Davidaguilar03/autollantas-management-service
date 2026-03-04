package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.CategoriaProducto;
import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.repository.CategoriaProductoRepository;
import com.autollantas.gestion.repository.ProductoRepository;
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
public class FormularioProductoController {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaProductoRepository categoriaRepository;

    @FXML private Label lblTitulo;
    @FXML private TextField txtCodigo;
    @FXML private ComboBox<CategoriaProducto> comboCategoria;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPrecioBruto;
    @FXML private TextField txtPorcentajeIva;
    @FXML private TextField txtMontoIva;
    @FXML private TextField txtTotal;
    @FXML private Spinner<Integer> spinnerStock;

    private boolean guardado = false;
    private Producto productoActual;

    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        formatoMoneda.setMaximumFractionDigits(0);
        spinnerStock.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0));

        configurarComboCategorias();
        configurarLogicaPrecios();

        this.productoActual = new Producto();

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
                String formateado = formatoMoneda.format(numero);

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

            txtMontoIva.setText(formatoMoneda.format(montoIva));
            txtTotal.setText(formatoMoneda.format(total));

        } catch (NumberFormatException e) {
            txtMontoIva.setText("$ 0");
            txtTotal.setText("$ 0");
        }
    }

    public void setProducto(Producto producto) {
        this.productoActual = producto;

        if (producto.getIdProducto() != null) {
            lblTitulo.setText("Editar Producto");
            txtCodigo.setEditable(false);
        } else {
            lblTitulo.setText("Nuevo Producto");
        }

        txtCodigo.setText(producto.getCodigoProducto() != null ? producto.getCodigoProducto() : "");
        txtDescripcion.setText(producto.getDescripcion() != null ? producto.getDescripcion() : "");

        if (producto.getPrecioBrutoProducto() > 0) {
            txtPrecioBruto.setText(formatoMoneda.format(producto.getPrecioBrutoProducto()));

            if (producto.getIvaProducto() > 0) {
                double porc = (producto.getIvaProducto() / producto.getPrecioBrutoProducto()) * 100;
                txtPorcentajeIva.setText(String.format("%.0f", porc));
            } else {
                txtPorcentajeIva.setText("0");
            }
        } else {
            txtPrecioBruto.setText("");
            txtPorcentajeIva.setText("19");
        }

        spinnerStock.getValueFactory().setValue(producto.getCantidad());

        if (producto.getCategoria() != null) {
            comboCategoria.setValue(producto.getCategoria());
        }

        calcularMontos();
    }

    @FXML
    public void guardarProducto() {
        if (!validarCampos()) return;

        try {
            productoActual.setCodigoProducto(txtCodigo.getText());
            productoActual.setDescripcion(txtDescripcion.getText());
            productoActual.setCategoria(comboCategoria.getValue());

            String brutoLimpio = txtPrecioBruto.getText().replaceAll("[^0-9]", "");
            double bruto = Double.parseDouble(brutoLimpio);

            String porcentajeStr = txtPorcentajeIva.getText().replaceAll("[^0-9.]", "");
            double porcentaje = porcentajeStr.isEmpty() ? 0 : Double.parseDouble(porcentajeStr);

            double montoIva = bruto * (porcentaje / 100);
            double total = bruto + montoIva;

            productoActual.setPrecioBrutoProducto(bruto);
            productoActual.setIvaProducto(montoIva);
            productoActual.setPrecioIvaProducto(total);
            productoActual.setCantidad(spinnerStock.getValue());

            productoRepository.save(productoActual);

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
        List<CategoriaProducto> categorias = categoriaRepository.findAll();
        comboCategoria.setItems(FXCollections.observableArrayList(categorias));
        comboCategoria.setConverter(new StringConverter<CategoriaProducto>() {
            @Override public String toString(CategoriaProducto cat) { return cat != null ? cat.getNombreCategoriaProducto() : ""; }
            @Override public CategoriaProducto fromString(String string) { return null; }
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
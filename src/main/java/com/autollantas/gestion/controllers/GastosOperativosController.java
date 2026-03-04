package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.GastoOperativo;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.GastoOperativoRepository;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("ALL")
@Component
public class GastosOperativosController {

    @Autowired private GastoOperativoRepository gastoRepo;
    @Autowired private CuentaRepository cuentaRepo;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtConcepto;
    @FXML private TextField txtMonto;
    @FXML private TextField txtObservaciones;
    @FXML private ComboBox<Cuenta> comboCuenta;
    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    @FXML private TableView<GastoOperativo> tablaCostos;
    @FXML private TableColumn<GastoOperativo, LocalDate> colFecha;
    @FXML private TableColumn<GastoOperativo, String> colConcepto;
    @FXML private TableColumn<GastoOperativo, String> colObservaciones;
    @FXML private TableColumn<GastoOperativo, String> colCuenta;
    @FXML private TableColumn<GastoOperativo, Double> colMonto;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<GastoOperativo> masterData = FXCollections.observableArrayList();
    private FilteredList<GastoOperativo> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<GastoOperativo> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaCostos.comparatorProperty());
        tablaCostos.setItems(sortedData);

        configurarColumnas();
        configurarComboCuentas();
        configurarFormatosFecha();
        configurarListeners();

        cargarDatosDB();
    }

    private void cargarDatosDB() {
        if (gastoRepo == null) return;
        Platform.runLater(() -> {
            masterData.clear();
            List<GastoOperativo> lista = gastoRepo.findAll();
            masterData.setAll(lista);
            actualizarInfoRegistros();
        });
    }

    private void actualizarInfoRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaGasto"));
        colFecha.setCellFactory(col -> crearCeldaFecha());

        colConcepto.setCellValueFactory(new PropertyValueFactory<>("conceptoGasto"));
        estilizarColumnaTexto(colConcepto);

        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
        estilizarColumnaTexto(colObservaciones);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null)
                return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            return new SimpleStringProperty("Sin Cuenta");
        });
        estilizarColumnaTexto(colCuenta);

        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoGasto"));
        colMonto.setCellFactory(col -> crearCeldaMonto());
    }

    private void estilizarColumnaTexto(TableColumn<GastoOperativo, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setAlignment(Pos.CENTER_LEFT);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setWrapText(true);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                }
            }
        });
    }

    private TableCell<GastoOperativo, LocalDate> crearCeldaFecha() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(fechaFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<GastoOperativo, Double> crearCeldaMonto() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setAlignment(Pos.CENTER_RIGHT);
                    lbl.setStyle("-fx-font-weight: bold;");
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#c0392b")));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }

    private void configurarListeners() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> {
            aplicarFiltros();
            actualizarInfoRegistros();
        };

        txtConcepto.textProperty().addListener(changeListener);
        txtMonto.textProperty().addListener(changeListener);
        txtObservaciones.textProperty().addListener(changeListener);
        comboCuenta.valueProperty().addListener(changeListener);
        dpFechaDesde.valueProperty().addListener(changeListener);
        dpFechaHasta.valueProperty().addListener(changeListener);

        tablaCostos.getSelectionModel().selectedItemProperty().addListener((obs, old, nueva) -> {
            boolean haySeleccion = (nueva != null);
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
        });
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(gasto -> {
            if (!filtroTexto(txtConcepto.getText(), gasto.getConceptoGasto())) return false;
            if (!filtroTexto(txtObservaciones.getText(), gasto.getObservaciones())) return false;

            Cuenta cuentaSel = comboCuenta.getValue();
            if (cuentaSel != null && !cuentaSel.getNombreCuenta().equals("Todas")) {
                if (gasto.getCuenta() == null || !gasto.getCuenta().getIdCuenta().equals(cuentaSel.getIdCuenta())) {
                    return false;
                }
            }

            String montoInput = txtMonto.getText().replaceAll("[^0-9]", "");
            if (!montoInput.isEmpty()) {
                if (gasto.getMontoGasto() == null) return false;
                String montoS = String.valueOf(gasto.getMontoGasto().longValue());
                if (!montoS.startsWith(montoInput)) return false;
            }

            if (dpFechaDesde.getValue() != null && (gasto.getFechaGasto() == null || gasto.getFechaGasto().isBefore(dpFechaDesde.getValue()))) return false;
            if (dpFechaHasta.getValue() != null && (gasto.getFechaGasto() == null || gasto.getFechaGasto().isAfter(dpFechaHasta.getValue()))) return false;

            return true;
        });
    }

    private boolean filtroTexto(String filtro, String valor) {
        return filtro == null || filtro.isEmpty() || (valor != null && valor.toLowerCase().contains(filtro.toLowerCase()));
    }

    private void configurarComboCuentas() {
        if (cuentaRepo == null) return;
        List<Cuenta> cuentas = cuentaRepo.findAll();
        comboCuenta.getItems().clear();

        Cuenta todas = new Cuenta();
        todas.setNombreCuenta("Todas");
        comboCuenta.getItems().add(todas);
        comboCuenta.getItems().addAll(cuentas);

        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Cuenta c) { return c != null ? c.getNombreCuenta() : ""; }
            @Override public Cuenta fromString(String s) { return null; }
        });
        comboCuenta.getSelectionModel().selectFirst();
    }

    @FXML
    void btnNuevoCostoClick(ActionEvent event) {
        abrirFormulario(new GastoOperativo());
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        GastoOperativo seleccionado = tablaCostos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirFormulario(seleccionado);
        }
    }

    public void abrirFormulario(GastoOperativo gasto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioGastoOperativo.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));

            Parent root = loader.load();

            FormularioGastoOperativoController controller = loader.getController();
            controller.setGasto(gasto);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaCostos.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();

            if (controller.isGuardado()) {
                cargarDatosDB();
                if (gasto.getIdGasto() != null) {
                    tablaCostos.getSelectionModel().select(gasto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error UI", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        GastoOperativo seleccionado = tablaCostos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar Eliminación");
            alert.setHeaderText("¿Eliminar gasto?");
            alert.setContentText("Concepto: " + seleccionado.getConceptoGasto() + "\nMonto: " + currencyFormat.format(seleccionado.getMontoGasto()));

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    gastoRepo.delete(seleccionado);
                    masterData.remove(seleccionado);
                    actualizarInfoRegistros();
                } catch (Exception e) {
                    mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo eliminar: " + e.getMessage());
                }
            }
        }
    }

    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtConcepto.clear();
        txtMonto.clear();
        txtObservaciones.clear();
        comboCuenta.getSelectionModel().selectFirst();
        dpFechaDesde.setValue(null);
        dpFechaHasta.setValue(null);
        aplicarFiltros();
    }

    private void configurarFormatosFecha() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override public String toString(LocalDate date) { return (date != null) ? fechaFormatter.format(date) : ""; }
            @Override public LocalDate fromString(String string) { return (string != null && !string.isEmpty()) ? LocalDate.parse(string, fechaFormatter) : null; }
        };
        dpFechaDesde.setConverter(converter);
        dpFechaHasta.setConverter(converter);
    }

    private void mostrarAlerta(Alert.AlertType type, String titulo, String contenido) {
        Alert alert = new Alert(type);
        alert.setTitle("Gestión de Gastos Operativos");
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
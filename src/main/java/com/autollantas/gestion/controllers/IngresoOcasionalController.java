package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.IngresoOcasional;
import com.autollantas.gestion.service.TesoreriaService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("ALL")
@Component
public class IngresoOcasionalController {

    @Autowired private TesoreriaService tesoreriaService;
    @Autowired
    private org.springframework.context.ApplicationContext springContext;

    @FXML private TextField txtConcepto;
    @FXML private TextField txtMonto;
    @FXML private TextField txtObservaciones;
    @FXML private ComboBox<Cuenta> comboCuenta;

    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    @FXML private TableView<IngresoOcasional> tablaIngresos;

    @FXML private TableColumn<IngresoOcasional, LocalDate> colFecha;
    @FXML private TableColumn<IngresoOcasional, String> colConcepto;
    @FXML private TableColumn<IngresoOcasional, String> colObservaciones;
    @FXML private TableColumn<IngresoOcasional, String> colCuenta;
    @FXML private TableColumn<IngresoOcasional, Double> colMonto;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;


    private final ObservableList<IngresoOcasional> masterData = FXCollections.observableArrayList();
    private FilteredList<IngresoOcasional> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<IngresoOcasional> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaIngresos.comparatorProperty());
        tablaIngresos.setItems(sortedData);

        configurarColumnas();
        configurarFiltrosUI();
        configurarFormatosFecha();
        setupListenersBusqueda();
        setupListenerSeleccion();

        cargarDatosDB();
    }

    private void cargarDatosDB() {
        Platform.runLater(() -> {
            List<IngresoOcasional> lista = tesoreriaService.findAllIngresosOcasionales();
            masterData.setAll(lista);
            actualizarLabelRegistros();
        });
    }

    private void actualizarLabelRegistros() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }



    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaIngreso"));
        colFecha.setCellFactory(col -> crearCeldaFecha());
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("conceptoIngreso"));
        estilizarColumnaTexto(colConcepto, "-fx-alignment: CENTER;");
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));
        estilizarColumnaTexto(colObservaciones, "-fx-alignment: CENTER-LEFT;");
        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getCuenta() != null) {
                return new SimpleStringProperty(cell.getValue().getCuenta().getNombreCuenta());
            }
            return new SimpleStringProperty("N/A");
        });
        estilizarColumnaTexto(colCuenta, "-fx-alignment: CENTER;");

        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoIngreso"));
        colMonto.setCellFactory(col -> crearCeldaMonto());
    }

    private void estilizarColumnaTexto(TableColumn<IngresoOcasional, String> col, String alignmentStyle) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setPadding(new Insets(5));
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setStyle(alignmentStyle);
                }
            }
        });
    }

    private TableCell<IngresoOcasional, LocalDate> crearCeldaFecha() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(fechaFormatter.format(item));
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        };
    }

    private TableCell<IngresoOcasional, Double> crearCeldaMonto() {
        return new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(currencyFormat.format(item));
                    lbl.setStyle("-fx-font-weight: bold;");
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.web("#13522d")));
                    setGraphic(lbl);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        };
    }

    private void guardarIngreso(IngresoOcasional nuevo) {
        if (nuevo.getCuenta() == null || nuevo.getMontoIngreso() <= 0) {
            mostrarAlerta("Error", "Datos inválidos. Seleccione cuenta y monto mayor a 0.");
            return;
        }

        tesoreriaService.saveIngresoOcasional(nuevo, true);

        cargarDatosDB();
        mostrarAlerta("Éxito", "Ingreso registrado y saldo actualizado.");
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        IngresoOcasional seleccionado = tablaIngresos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Eliminar Ingreso");
            alert.setHeaderText("¿Está seguro de eliminar este registro?");
            alert.setContentText("El dinero (" + currencyFormat.format(seleccionado.getMontoIngreso()) +
                    ") será descontado de " + seleccionado.getCuenta().getNombreCuenta());

            Optional<ButtonType> res = alert.showAndWait();
            if(res.isPresent() && res.get() == ButtonType.OK) {

                tesoreriaService.deleteIngresoOcasional(seleccionado);
                masterData.remove(seleccionado);
                actualizarLabelRegistros();
            }
        }
    }

    @FXML
    void btnNuevoIngresoClick(ActionEvent event) {
        abrirFormulario(new IngresoOcasional());
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        IngresoOcasional seleccionado = tablaIngresos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirFormulario(seleccionado);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Selección requerida");
            alert.setContentText("Selecciona un ingreso para editar.");
            alert.show();
        }
    }

    public void abrirFormulario(IngresoOcasional ingreso) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/FormularioIngresoOcasional.fxml"));

            loader.setControllerFactory(param -> springContext.getBean(param));

            javafx.scene.Parent root = loader.load();

            FormularioIngresoOcasionalController controller = loader.getController();
            controller.setIngreso(ingreso);

            Stage modalStage = new Stage();
            modalStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Stage ventanaPrincipal = (Stage) tablaIngresos.getScene().getWindow();
            modalStage.initOwner(ventanaPrincipal);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(ventanaPrincipal.getX());
            modalStage.setY(ventanaPrincipal.getY());
            modalStage.setWidth(ventanaPrincipal.getWidth());
            modalStage.setHeight(ventanaPrincipal.getHeight());

            modalStage.showAndWait();

            if (controller.isGuardado()) {
                cargarDatosDB();
                if (ingreso.getIdIngreso() != null) {
                    tablaIngresos.getSelectionModel().select(ingreso);
                } else {
                    mostrarAlerta("Éxito", "Ingreso registrado correctamente.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error UI");
            alert.setHeaderText("Error al abrir formulario");
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }


    private void setupListenersBusqueda() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> aplicarFiltros();

        txtConcepto.textProperty().addListener(changeListener);
        txtMonto.textProperty().addListener(changeListener);
        txtObservaciones.textProperty().addListener(changeListener);
        comboCuenta.valueProperty().addListener(changeListener);
        dpFechaDesde.valueProperty().addListener(changeListener);
        dpFechaHasta.valueProperty().addListener(changeListener);
    }

    private void aplicarFiltros() {
        filteredData.setPredicate(ingreso -> {
            if (!matchTexto(ingreso.getConceptoIngreso(), txtConcepto.getText())) return false;
            if (!matchTexto(ingreso.getObservaciones(), txtObservaciones.getText())) return false;

            String montoFilter = txtMonto.getText().replaceAll("[^0-9]", "");
            if (!montoFilter.isEmpty()) {
                if (!String.valueOf(ingreso.getMontoIngreso().longValue()).contains(montoFilter)) return false;
            }

            Cuenta cuentaSel = comboCuenta.getValue();
            if (cuentaSel != null) {
                if (ingreso.getCuenta() == null || !ingreso.getCuenta().getIdCuenta().equals(cuentaSel.getIdCuenta())) return false;
            }

            if (fueraDeRango(ingreso.getFechaIngreso(), dpFechaDesde.getValue(), dpFechaHasta.getValue())) return false;

            return true;
        });
        actualizarLabelRegistros();
    }

    private boolean matchTexto(String valor, String filtro) {
        if (filtro == null || filtro.isEmpty()) return true;
        return valor != null && valor.toLowerCase().contains(filtro.toLowerCase());
    }

    private boolean fueraDeRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return true;
        if (desde != null && fecha.isBefore(desde)) return true;
        if (hasta != null && fecha.isAfter(hasta)) return true;
        return false;
    }


    @FXML void btnBuscarClick(ActionEvent event) { aplicarFiltros(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtConcepto.clear(); txtMonto.clear(); txtObservaciones.clear();
        comboCuenta.getSelectionModel().clearSelection();
        dpFechaDesde.setValue(null); dpFechaHasta.setValue(null);
    }

    private void setupListenerSeleccion() {
        btnEditar.setDisable(true); btnEliminar.setDisable(true);
        tablaIngresos.getSelectionModel().selectedItemProperty().addListener((obs, old, nueva) -> {
            boolean haySeleccion = (nueva != null);
            btnEditar.setDisable(!haySeleccion);
            btnEliminar.setDisable(!haySeleccion);
        });
    }

    private void configurarFiltrosUI() {
        List<Cuenta> cuentas = tesoreriaService.findAllCuentas();
        comboCuenta.setConverter(getStringConverterCuenta());
        comboCuenta.getItems().setAll(cuentas);
    }

    private StringConverter<Cuenta> getStringConverterCuenta() {
        return new StringConverter<>() {
            @Override public String toString(Cuenta c) { return c != null ? c.getNombreCuenta() : ""; }
            @Override public Cuenta fromString(String s) { return null; }
        };
    }

    private void configurarFormatosFecha() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override public String toString(LocalDate date) { return (date != null) ? fechaFormatter.format(date) : ""; }
            @Override public LocalDate fromString(String string) { return (string != null && !string.isEmpty()) ? LocalDate.parse(string, fechaFormatter) : null; }
        };
        dpFechaDesde.setConverter(converter);
        dpFechaHasta.setConverter(converter);
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ingresos Ocasionales");
        alert.setHeaderText(titulo);
        alert.setContentText(contenido);
        alert.showAndWait();
    }
}
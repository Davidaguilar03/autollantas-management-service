package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.shared.util.ToastNotification;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class OccasionalIncomeController {

    @Autowired private TreasuryService treasuryService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtConcepto;
    @FXML private TextField txtMonto;
    @FXML private TextField txtObservaciones;
    @FXML private ComboBox<Account> comboCuenta;

    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    @FXML private TableView<OccasionalIncome> tablaIngresos;

    @FXML private TableColumn<OccasionalIncome, LocalDate> colFecha;
    @FXML private TableColumn<OccasionalIncome, String> colConcepto;
    @FXML private TableColumn<OccasionalIncome, String> colObservaciones;
    @FXML private TableColumn<OccasionalIncome, String> colCuenta;
    @FXML private TableColumn<OccasionalIncome, Double> colMonto;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<OccasionalIncome> masterData = FXCollections.observableArrayList();
    private FilteredList<OccasionalIncome> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<OccasionalIncome> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaIngresos.comparatorProperty());
        tablaIngresos.setItems(sortedData);

        configureColumns();
        configureFiltersUI();
        configureDateFormats();
        setupSearchListeners();
        setupSelectionListener();

        loadDatabaseData();
    }

    private void loadDatabaseData() {
        Platform.runLater(() -> {
            List<OccasionalIncome> list = treasuryService.findAllOccasionalIncomes();
            masterData.setAll(list);
            updateRecordsLabel();
        });
    }

    private void updateRecordsLabel() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configureColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("date"));
        colFecha.setCellFactory(col -> createDateCell());
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concept"));
        styleTextColumn(colConcepto, "-fx-alignment: CENTER;");
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("notes"));
        styleTextColumn(colObservaciones, "-fx-alignment: CENTER-LEFT;");
        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null) {
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            }
            return new SimpleStringProperty("N/A");
        });
        styleTextColumn(colCuenta, "-fx-alignment: CENTER;");

        colMonto.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colMonto.setCellFactory(col -> createAmountCell());
    }

    private void styleTextColumn(TableColumn<OccasionalIncome, String> col, String alignmentStyle) {
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

    private TableCell<OccasionalIncome, LocalDate> createDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormatter.format(item));
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        };
    }

    private TableCell<OccasionalIncome, Double> createAmountCell() {
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

    @FXML
    void btnEliminarClick(ActionEvent event) {
        OccasionalIncome selected = tablaIngresos.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CustomDialog.danger(
            tablaIngresos,
            "Eliminar ingreso ocasional",
            "Estás a punto de eliminar el ingreso \"" + selected.getConcept() + "\" por " +
            currencyFormat.format(selected.getAmount()) + ". Este monto será descontado de " +
            selected.getAccount().getName() + ". Esta acción no se puede deshacer.",
            () -> {
                treasuryService.deleteOccasionalIncome(selected);
                masterData.remove(selected);
                updateRecordsLabel();
                ToastNotification.success(tablaIngresos,
                    "Ingreso \"" + selected.getConcept() + "\" eliminado correctamente");
            },
            null
        );
    }

    @FXML
    void btnNuevoIngresoClick(ActionEvent event) {
        openForm(new OccasionalIncome());
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        OccasionalIncome selected = tablaIngresos.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.warning(tablaIngresos, "Selecciona un ingreso para editar");
            return;
        }

        openForm(selected);
    }

    public void openForm(OccasionalIncome income) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/autollantas/gestion/treasury/views/OccasionalIncomeForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));

            javafx.scene.Parent root = loader.load();

            OccasionalIncomeFormController controller = loader.getController();
            boolean esEdicion = (income.getId() != null);
            controller.setIncome(income);

            Stage modalStage = new Stage();
            modalStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Stage mainWindow = (Stage) tablaIngresos.getScene().getWindow();
            modalStage.initOwner(mainWindow);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            modalStage.setScene(scene);

            modalStage.setX(mainWindow.getX());
            modalStage.setY(mainWindow.getY());
            modalStage.setWidth(mainWindow.getWidth());
            modalStage.setHeight(mainWindow.getHeight());

            modalStage.showAndWait();

            if (controller.isSaved()) {
                loadDatabaseData();
                if (esEdicion) {
                    ToastNotification.success(
                        MainLayoutController.getInstance().getContentArea(),
                        "Ingreso \"" + income.getConcept() + "\" actualizado correctamente");
                } else {
                    ToastNotification.success(
                        MainLayoutController.getInstance().getContentArea(),
                        "Ingreso ocasional registrado correctamente");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaIngresos, "No se pudo abrir el formulario de ingreso");
        }
    }

    private void setupSearchListeners() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> applyFilters();

        txtConcepto.textProperty().addListener(changeListener);
        txtMonto.textProperty().addListener(changeListener);
        txtObservaciones.textProperty().addListener(changeListener);
        comboCuenta.valueProperty().addListener(changeListener);
        dpFechaDesde.valueProperty().addListener(changeListener);
        dpFechaHasta.valueProperty().addListener(changeListener);
    }

    private void applyFilters() {
        filteredData.setPredicate(income -> {
            if (!matchText(income.getConcept(), txtConcepto.getText())) return false;
            if (!matchText(income.getNotes(), txtObservaciones.getText())) return false;

            String amountFilter = txtMonto.getText().replaceAll("[^0-9]", "");
            if (!amountFilter.isEmpty()) {
                if (!String.valueOf(income.getAmount().longValue()).contains(amountFilter)) return false;
            }

            Account selectedAccount = comboCuenta.getValue();
            if (selectedAccount != null) {
                if (income.getAccount() == null || !income.getAccount().getId().equals(selectedAccount.getId())) return false;
            }

            if (outOfRange(income.getDate(), dpFechaDesde.getValue(), dpFechaHasta.getValue())) return false;

            return true;
        });
        updateRecordsLabel();
    }

    private boolean matchText(String value, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }

    private boolean outOfRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) return true;
        if (from != null && date.isBefore(from)) return true;
        if (to != null && date.isAfter(to)) return true;
        return false;
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtConcepto.clear(); txtMonto.clear(); txtObservaciones.clear();
        comboCuenta.getSelectionModel().clearSelection();
        dpFechaDesde.setValue(null); dpFechaHasta.setValue(null);
    }

    private void setupSelectionListener() {
        btnEditar.setDisable(true); btnEliminar.setDisable(true);
        tablaIngresos.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = (selected != null);
            btnEditar.setDisable(!hasSelection);
            btnEliminar.setDisable(!hasSelection);
        });
    }

    private void configureFiltersUI() {
        List<Account> accounts = treasuryService.findAllAccounts();
        comboCuenta.setConverter(getAccountConverter());
        comboCuenta.getItems().setAll(accounts);
    }

    private StringConverter<Account> getAccountConverter() {
        return new StringConverter<>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        };
    }

    private void configureDateFormats() {
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override public String toString(LocalDate date) { return (date != null) ? dateFormatter.format(date) : ""; }
            @Override public LocalDate fromString(String s) { return (s != null && !s.isEmpty()) ? LocalDate.parse(s, dateFormatter) : null; }
        };
        dpFechaDesde.setConverter(converter);
        dpFechaHasta.setConverter(converter);
    }

}

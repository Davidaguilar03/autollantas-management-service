package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.shared.controller.MainLayoutController;
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

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.autollantas.gestion.shared.util.CustomDialog;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
@Component
public class OperationalExpensesController {

    @Autowired private TreasuryService treasuryService;
    @Autowired private ApplicationContext springContext;

    @FXML private TextField txtConcepto;
    @FXML private TextField txtMontoMin;
    @FXML private TextField txtMontoMax;
    @FXML private TextField txtObservaciones;
    @FXML private ComboBox<Account> comboCuenta;
    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    @FXML private TableView<OperationalExpense> tablaCostos;
    @FXML private TableColumn<OperationalExpense, LocalDate> colFecha;
    @FXML private TableColumn<OperationalExpense, String> colConcepto;
    @FXML private TableColumn<OperationalExpense, String> colObservaciones;
    @FXML private TableColumn<OperationalExpense, String> colCuenta;
    @FXML private TableColumn<OperationalExpense, Double> colMonto;

    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Label lblInfoRegistros;

    private final ObservableList<OperationalExpense> masterData = FXCollections.observableArrayList();
    private FilteredList<OperationalExpense> filteredData;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<OperationalExpense> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaCostos.comparatorProperty());
        tablaCostos.setItems(sortedData);

        configureColumns();
        configureAccountCombo();
        configureDateFormats();
        configureListeners();

        loadDatabaseData();
    }

    private void loadDatabaseData() {
        if (treasuryService == null) return;
        Platform.runLater(() -> {
            masterData.clear();
            List<OperationalExpense> list = treasuryService.findAllOperationalExpenses();
            masterData.setAll(list);
            updateRecordsInfo();
        });
    }

    private void updateRecordsInfo() {
        if (lblInfoRegistros != null) {
            lblInfoRegistros.setText("Mostrando " + filteredData.size() + " de " + masterData.size() + " registros");
        }
    }

    private void configureColumns() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("date"));
        colFecha.setCellFactory(col -> createDateCell());

        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concept"));
        styleTextColumn(colConcepto);

        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("notes"));
        styleTextColumn(colObservaciones);

        colCuenta.setCellValueFactory(cell -> {
            if (cell.getValue().getAccount() != null)
                return new SimpleStringProperty(cell.getValue().getAccount().getName());
            return new SimpleStringProperty("Sin Cuenta");
        });
        styleTextColumn(colCuenta);

        colMonto.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colMonto.setCellFactory(col -> createAmountCell());
    }

    private void styleTextColumn(TableColumn<OperationalExpense, String> col) {
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

    private TableCell<OperationalExpense, LocalDate> createDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(dateFormatter.format(item));
                    lbl.setAlignment(Pos.CENTER);
                    lbl.textFillProperty().bind(Bindings.when(selectedProperty()).then(Color.WHITE).otherwise(Color.BLACK));
                    setGraphic(lbl);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private TableCell<OperationalExpense, Double> createAmountCell() {
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

    private void configureListeners() {
        javafx.beans.value.ChangeListener<Object> changeListener = (obs, oldVal, newVal) -> {
            applyFilters();
            updateRecordsInfo();
        };

        txtConcepto.textProperty().addListener(changeListener);
        txtMontoMin.textProperty().addListener(changeListener);
        txtMontoMax.textProperty().addListener(changeListener);
        txtObservaciones.textProperty().addListener(changeListener);
        comboCuenta.valueProperty().addListener(changeListener);
        dpFechaDesde.valueProperty().addListener(changeListener);
        dpFechaHasta.valueProperty().addListener(changeListener);

        tablaCostos.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = (selected != null);
            btnEditar.setDisable(!hasSelection);
            btnEliminar.setDisable(!hasSelection);
        });
    }

    private void applyFilters() {
        filteredData.setPredicate(expense -> {
            if (!matchText(txtConcepto.getText(), expense.getConcept())) return false;
            if (!matchText(txtObservaciones.getText(), expense.getNotes())) return false;

            Account selectedAccount = comboCuenta.getValue();
            if (selectedAccount != null && !selectedAccount.getName().equals("Todas")) {
                if (expense.getAccount() == null || !expense.getAccount().getId().equals(selectedAccount.getId())) {
                    return false;
                }
            }

            String minStr = txtMontoMin.getText().replaceAll("[^0-9]", "");
            String maxStr = txtMontoMax.getText().replaceAll("[^0-9]", "");
            if (!minStr.isEmpty() || !maxStr.isEmpty()) {
                if (expense.getAmount() == null) return false;
                long amount = expense.getAmount().longValue();
                if (!minStr.isEmpty() && amount < Long.parseLong(minStr)) return false;
                if (!maxStr.isEmpty() && amount > Long.parseLong(maxStr)) return false;
            }

            if (dpFechaDesde.getValue() != null && (expense.getDate() == null || expense.getDate().isBefore(dpFechaDesde.getValue()))) return false;
            if (dpFechaHasta.getValue() != null && (expense.getDate() == null || expense.getDate().isAfter(dpFechaHasta.getValue()))) return false;

            return true;
        });
    }

    private boolean matchText(String filter, String value) {
        return filter == null || filter.isEmpty() || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
    }

    private void configureAccountCombo() {
        if (treasuryService == null) return;
        List<Account> accounts = treasuryService.findAllAccounts();
        comboCuenta.getItems().clear();

        Account all = new Account();
        all.setName("Todas");
        comboCuenta.getItems().add(all);
        comboCuenta.getItems().addAll(accounts);

        comboCuenta.setConverter(new StringConverter<>() {
            @Override public String toString(Account a) { return a != null ? a.getName() : ""; }
            @Override public Account fromString(String s) { return null; }
        });
        comboCuenta.getSelectionModel().selectFirst();
    }

    @FXML
    void btnNuevoCostoClick(ActionEvent event) {
        openForm(new OperationalExpense());
    }

    @FXML
    void btnEditarClick(ActionEvent event) {
        OperationalExpense selected = tablaCostos.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    public void openForm(OperationalExpense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/treasury/views/OperationalExpenseForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));

            Parent root = loader.load();

            OperationalExpenseFormController controller = loader.getController();
            boolean esEdicion = (expense.getId() != null);
            controller.setExpense(expense);

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage mainWindow = (Stage) tablaCostos.getScene().getWindow();
            modalStage.initOwner(mainWindow);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
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
                        "Gasto \"" + expense.getConcept() + "\" actualizado correctamente");
                } else {
                    ToastNotification.success(
                        MainLayoutController.getInstance().getContentArea(),
                        "Gasto operativo registrado correctamente");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ToastNotification.error(tablaCostos, "No se pudo abrir el formulario de gasto");
        }
    }

    @FXML
    void btnEliminarClick(ActionEvent event) {
        OperationalExpense selected = tablaCostos.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        CustomDialog.danger(tablaCostos,
            "Eliminar gasto operativo",
            "Estás a punto de eliminar el gasto \"" + selected.getConcept() + "\" por "
                + currencyFormat.format(selected.getAmount()) + ". "
                + "Este monto será descontado de " + (selected.getAccount() != null ? selected.getAccount().getName() : "la cuenta") + ". "
                + "Esta acción no se puede deshacer.",
            () -> {
                try {
                    treasuryService.deleteOperationalExpense(selected);
                    masterData.remove(selected);
                    updateRecordsInfo();
                    ToastNotification.success(tablaCostos,
                        "Gasto \"" + selected.getConcept() + "\" eliminado");
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastNotification.error(tablaCostos, "No se pudo eliminar el gasto");
                }
            },
            null);
    }

    @FXML void btnBuscarClick(ActionEvent event) { applyFilters(); }

    @FXML void btnLimpiarFiltrosClick(ActionEvent event) {
        txtConcepto.clear();
        txtMontoMin.clear();
        txtMontoMax.clear();
        txtObservaciones.clear();
        comboCuenta.getSelectionModel().selectFirst();
        dpFechaDesde.setValue(null);
        dpFechaHasta.setValue(null);
        applyFilters();
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

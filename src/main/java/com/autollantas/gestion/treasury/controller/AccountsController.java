package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.Transfer;
import com.autollantas.gestion.treasury.service.TreasuryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class AccountsController {

    @Autowired private TreasuryService treasuryService;
    @Autowired private ApplicationContext springContext;

    @FXML private Label lblTotalGlobal;

    @FXML private Label lblSaldoCaja;
    @FXML private TableView<MovementDTO> tablaMovCaja;
    @FXML private TableColumn<MovementDTO, LocalDate> colCajaMovFecha;
    @FXML private TableColumn<MovementDTO, String> colCajaMovTipo;
    @FXML private TableColumn<MovementDTO, String> colCajaMovConcepto;
    @FXML private TableColumn<MovementDTO, Double> colCajaMovMonto;
    @FXML private Label lblInfoMovCaja;

    @FXML private TableView<TransferDTO> tablaTransCaja;
    @FXML private TableColumn<TransferDTO, String> colCajaTransConcepto;
    @FXML private TableColumn<TransferDTO, LocalDate> colCajaTransFecha;
    @FXML private TableColumn<TransferDTO, Double> colCajaTransValor;
    @FXML private TableColumn<TransferDTO, String> colCajaTransOrigen;
    @FXML private TableColumn<TransferDTO, String> colCajaTransDestino;
    @FXML private Label lblInfoTransCaja;

    @FXML private Label lblSaldoBanco;
    @FXML private TableView<MovementDTO> tablaMovBanco;
    @FXML private TableColumn<MovementDTO, LocalDate> colBancoMovFecha;
    @FXML private TableColumn<MovementDTO, String> colBancoMovTipo;
    @FXML private TableColumn<MovementDTO, String> colBancoMovConcepto;
    @FXML private TableColumn<MovementDTO, Double> colBancoMovMonto;
    @FXML private Label lblInfoMovBanco;

    @FXML private TableView<TransferDTO> tablaTransBanco;
    @FXML private TableColumn<TransferDTO, String> colBancoTransConcepto;
    @FXML private TableColumn<TransferDTO, LocalDate> colBancoTransFecha;
    @FXML private TableColumn<TransferDTO, Double> colBancoTransValor;
    @FXML private TableColumn<TransferDTO, String> colBancoTransOrigen;
    @FXML private TableColumn<TransferDTO, String> colBancoTransDestino;
    @FXML private Label lblInfoTransBanco;

    private final ObservableList<MovementDTO> cashMovementList = FXCollections.observableArrayList();
    private final ObservableList<TransferDTO> cashTransferList = FXCollections.observableArrayList();
    private final ObservableList<MovementDTO> bankMovementList = FXCollections.observableArrayList();
    private final ObservableList<TransferDTO> bankTransferList = FXCollections.observableArrayList();

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Account cashAccount;
    private Account bankAccount;

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        configureTables();
        loadDatabaseData();
    }

    private void loadDatabaseData() {
        if (treasuryService == null) return;

        new Thread(() -> {
            List<Account> accounts = treasuryService.findAllAccounts();
            cashAccount = accounts.stream().filter(c -> c.getName().toUpperCase().contains("CAJA") || c.getName().toUpperCase().contains("EFECTIVO")).findFirst().orElse(null);
            bankAccount = accounts.stream().filter(c -> c.getName().toUpperCase().contains("BANCO") || c.getName().toUpperCase().contains("COLOMBIA")).findFirst().orElse(null);

            if (cashAccount == null && !accounts.isEmpty()) cashAccount = accounts.get(0);
            if (bankAccount == null && accounts.size() > 1) bankAccount = accounts.get(1);

            Platform.runLater(() -> {
                updateBalancesUI();
                loadTables();
            });
        }).start();
    }

    private void loadTables() {
        if (cashAccount != null) {
            loadMovementsByAccount(cashAccount.getId(), cashMovementList, lblInfoMovCaja);
            loadTransfersByAccount(cashAccount.getId(), cashTransferList, lblInfoTransCaja);
        }
        if (bankAccount != null) {
            loadMovementsByAccount(bankAccount.getId(), bankMovementList, lblInfoMovBanco);
            loadTransfersByAccount(bankAccount.getId(), bankTransferList, lblInfoTransBanco);
        }
    }

    private void loadMovementsByAccount(Integer accountId, ObservableList<MovementDTO> list, Label lblInfo) {
        list.clear();
        List<Movement> movements = treasuryService.findMovementsByAccountId(accountId);
        for (Movement m : movements) {
            list.add(new MovementDTO(m));
        }
        if (lblInfo != null) lblInfo.setText("Registros: " + list.size());
    }

    private void loadTransfersByAccount(Integer accountId, ObservableList<TransferDTO> list, Label lblInfo) {
        list.clear();
        List<Transfer> transfers = treasuryService.findTransfersByAccountId(accountId);
        for (Transfer t : transfers) {
            list.add(new TransferDTO(t));
        }
        if (lblInfo != null) lblInfo.setText("Registros: " + list.size());
    }

    private void updateBalancesUI() {
        double cashBalance = (cashAccount != null) ? cashAccount.getCurrentBalance() : 0.0;
        double bankBalance = (bankAccount != null) ? bankAccount.getCurrentBalance() : 0.0;
        double total = cashBalance + bankBalance;

        if (lblSaldoCaja != null) lblSaldoCaja.setText(currencyFormat.format(cashBalance));
        if (lblSaldoBanco != null) lblSaldoBanco.setText(currencyFormat.format(bankBalance));
        if (lblTotalGlobal != null) lblTotalGlobal.setText("Total: " + currencyFormat.format(total));
    }

    private void configureTables() {
        tablaMovCaja.setItems(cashMovementList);
        colCajaMovFecha.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colCajaMovTipo.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colCajaMovConcepto.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        colCajaMovMonto.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        applyDateFormat(colCajaMovFecha);
        applyCurrencyFormat(colCajaMovMonto);
        formatTypeColumn(colCajaMovTipo);

        tablaTransCaja.setItems(cashTransferList);
        configureTransferColumns(colCajaTransConcepto, colCajaTransFecha, colCajaTransValor, colCajaTransOrigen, colCajaTransDestino);

        tablaMovBanco.setItems(bankMovementList);
        colBancoMovFecha.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colBancoMovTipo.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colBancoMovConcepto.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        colBancoMovMonto.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        applyDateFormat(colBancoMovFecha);
        applyCurrencyFormat(colBancoMovMonto);
        formatTypeColumn(colBancoMovTipo);

        tablaTransBanco.setItems(bankTransferList);
        configureTransferColumns(colBancoTransConcepto, colBancoTransFecha, colBancoTransValor, colBancoTransOrigen, colBancoTransDestino);
    }

    private void formatTypeColumn(TableColumn<MovementDTO, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.setAlignment(Pos.CENTER);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                    if (item.equalsIgnoreCase("Ingreso") || item.equalsIgnoreCase("Venta")) {
                        lbl.setTextFill(Color.web("#27ae60"));
                    } else {
                        lbl.setTextFill(Color.web("#c0392b"));
                    }
                    setGraphic(lbl);
                }
            }
        });
    }

    private void configureTransferColumns(TableColumn<TransferDTO, String> colDescription,
                                          TableColumn<TransferDTO, LocalDate> colDate,
                                          TableColumn<TransferDTO, Double> colAmount,
                                          TableColumn<TransferDTO, String> colSource,
                                          TableColumn<TransferDTO, String> colDestination) {
        colDescription.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colAmount.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        colSource.setCellValueFactory(cell -> cell.getValue().sourceProperty());
        colDestination.setCellValueFactory(cell -> cell.getValue().destinationProperty());
        applyDateFormat(colDate);
        applyCurrencyFormat(colAmount);
    }

    @FXML
    void abrirModalTransferencia(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/treasury/views/TransferForm.fxml"));
            loader.setControllerFactory(param -> springContext.getBean(param));
            Parent root = loader.load();

            TransferFormController controller = loader.getController();

            Stage modalStage = new Stage();
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Stage mainWindow = (Stage) lblTotalGlobal.getScene().getWindow();
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
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir el formulario: " + e.getMessage()).show();
        }
    }

    private <T> void applyDateFormat(TableColumn<T, LocalDate> column) {
        column.setCellFactory(col -> new TableCell<T, LocalDate>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormat.format(item));
            }
        });
    }

    private <T> void applyCurrencyFormat(TableColumn<T, Double> column) {
        column.setCellFactory(col -> new TableCell<T, Double>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : currencyFormat.format(item));
            }
        });
    }

    public static class MovementDTO {
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleStringProperty type;
        private final SimpleStringProperty description;
        private final SimpleDoubleProperty amount;

        public MovementDTO(Movement m) {
            this.date = new SimpleObjectProperty<>(m.getDate());
            this.type = new SimpleStringProperty(m.getType());
            String desc = m.getSourceTable() != null ? m.getSourceTable() + " #" + m.getSourceId() : "General";
            this.description = new SimpleStringProperty(desc);
            this.amount = new SimpleDoubleProperty(m.getAmount());
        }
        public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
        public SimpleStringProperty typeProperty() { return type; }
        public SimpleStringProperty descriptionProperty() { return description; }
        public SimpleDoubleProperty amountProperty() { return amount; }
    }

    public static class TransferDTO {
        private final SimpleStringProperty description;
        private final SimpleObjectProperty<LocalDate> date;
        private final SimpleDoubleProperty amount;
        private final SimpleStringProperty source;
        private final SimpleStringProperty destination;

        public TransferDTO(Transfer t) {
            this.description = new SimpleStringProperty("Transfer ID " + t.getId());
            this.date = new SimpleObjectProperty<>(t.getDate());
            this.amount = new SimpleDoubleProperty(t.getAmount());
            this.source = new SimpleStringProperty(t.getSourceAccount() != null ? t.getSourceAccount().getName() : "N/A");
            this.destination = new SimpleStringProperty(t.getDestinationAccount() != null ? t.getDestinationAccount().getName() : "N/A");
        }
        public SimpleStringProperty descriptionProperty() { return description; }
        public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
        public SimpleDoubleProperty amountProperty() { return amount; }
        public SimpleStringProperty sourceProperty() { return source; }
        public SimpleStringProperty destinationProperty() { return destination; }
    }
}

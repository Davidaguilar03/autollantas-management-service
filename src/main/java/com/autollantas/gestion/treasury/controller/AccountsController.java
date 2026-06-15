package com.autollantas.gestion.treasury.controller;

import com.autollantas.gestion.shared.util.ToastNotification;
import com.autollantas.gestion.treasury.dto.MovementDTO;
import com.autollantas.gestion.treasury.dto.TransferDTO;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.AccountType;
import com.autollantas.gestion.treasury.service.TreasuryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class AccountsController {

    @Autowired private TreasuryService treasuryService;
    @Autowired private ApplicationContext springContext;

    @FXML private Label lblTotalGlobal;

    // ── Caja ──────────────────────────────────────────────────────────────
    @FXML private Label              lblSaldoCaja;
    @FXML private Label              lblIngCaja;
    @FXML private Label              lblEgrCaja;
    @FXML private ComboBox<String>   comboPeriodoCaja;
    @FXML private VBox               rangoCaja;
    @FXML private DatePicker         datesDesdeCaja;
    @FXML private DatePicker         datesHastaCaja;
    @FXML private TableView<MovementDTO>              tablaMovCaja;
    @FXML private TableColumn<MovementDTO, LocalDate> colCajaMovFecha;
    @FXML private TableColumn<MovementDTO, String>    colCajaMovTipo;
    @FXML private TableColumn<MovementDTO, String>    colCajaMovConcepto;
    @FXML private TableColumn<MovementDTO, Double>    colCajaMovMonto;
    @FXML private Label              lblInfoMovCaja;

    // ── Banco ──────────────────────────────────────────────────────────────
    @FXML private Label              lblSaldoBanco;
    @FXML private Label              lblIngBanco;
    @FXML private Label              lblEgrBanco;
    @FXML private ComboBox<String>   comboPeriodoBanco;
    @FXML private VBox               rangoBanco;
    @FXML private DatePicker         datesDesdeBanco;
    @FXML private DatePicker         datesHastaBanco;
    @FXML private TableView<MovementDTO>              tablaMovBanco;
    @FXML private TableColumn<MovementDTO, LocalDate> colBancoMovFecha;
    @FXML private TableColumn<MovementDTO, String>    colBancoMovTipo;
    @FXML private TableColumn<MovementDTO, String>    colBancoMovConcepto;
    @FXML private TableColumn<MovementDTO, Double>    colBancoMovMonto;
    @FXML private Label              lblInfoMovBanco;

    // ── Transferencias ─────────────────────────────────────────────────────
    @FXML private TableView<TransferDTO>               tablaTransferencias;
    @FXML private TableColumn<TransferDTO, LocalDate>  colTransFecha;
    @FXML private TableColumn<TransferDTO, String>     colTransConcepto;
    @FXML private TableColumn<TransferDTO, Double>     colTransValor;
    @FXML private TableColumn<TransferDTO, String>     colTransOrigen;
    @FXML private TableColumn<TransferDTO, String>     colTransDestino;
    @FXML private Label              lblInfoTrans;
    @FXML private Label              lblEnviadoCaja;
    @FXML private Label              lblEnviadoBanco;
    @FXML private ComboBox<String>   comboPeriodoTrans;
    @FXML private VBox               rangoTrans;
    @FXML private DatePicker         datesDesdeTrans;
    @FXML private DatePicker         datesHastaTrans;

    // ── Listas master (sin filtrar) ────────────────────────────────────────
    private final ObservableList<MovementDTO> cashMovementList = FXCollections.observableArrayList();
    private final ObservableList<MovementDTO> bankMovementList = FXCollections.observableArrayList();
    private final ObservableList<TransferDTO> allTransferList  = FXCollections.observableArrayList();

    private final NumberFormat      currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter dateFormat     = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Account cashAccount;
    private Account bankAccount;

    private static final String PERSONALIZADO = "Personalizado";
    private static final List<String> PERIODOS = List.of(
            "Hoy", "Esta semana", "Este mes", "Mes anterior", "Este año", PERSONALIZADO
    );

    // ── Init ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
        comboPeriodoCaja.getItems().setAll(PERIODOS);
        comboPeriodoBanco.getItems().setAll(PERIODOS);
        comboPeriodoTrans.getItems().setAll(PERIODOS);
        comboPeriodoCaja.setValue("Hoy");
        comboPeriodoBanco.setValue("Hoy");
        comboPeriodoTrans.setValue("Hoy");
        configureTables();
        loadDatabaseData();
        Platform.runLater(this::quitarFocusGlow);
    }

    private void quitarFocusGlow() {
        String noGlow = "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
        for (javafx.scene.control.DatePicker dp : List.of(
                datesDesdeCaja, datesHastaCaja,
                datesDesdeBanco, datesHastaBanco,
                datesDesdeTrans, datesHastaTrans)) {
            dp.setStyle(noGlow);
            dp.getEditor().setStyle(noGlow);
        }
        for (ComboBox<String> cb : List.of(comboPeriodoCaja, comboPeriodoBanco, comboPeriodoTrans)) {
            cb.setStyle(noGlow);
        }
    }

    // ── Carga BD ───────────────────────────────────────────────────────────

    private void loadDatabaseData() {
        if (treasuryService == null) return;

        new Thread(() -> {
            treasuryService.migrateAccountTypes();
            List<Account> accounts = treasuryService.findAllAccounts();
            cashAccount = accounts.stream().filter(a -> AccountType.CASH.equals(a.getType())).findFirst().orElse(null);
            bankAccount = accounts.stream().filter(a -> AccountType.BANK.equals(a.getType())).findFirst().orElse(null);

            List<MovementDTO> cashMovs = cashAccount != null
                    ? treasuryService.findMovementsByAccountId(cashAccount.getId()).stream()
                        .map(m -> new MovementDTO(m, treasuryService.resolveDescription(m))).toList()
                    : List.of();
            List<MovementDTO> bankMovs = bankAccount != null
                    ? treasuryService.findMovementsByAccountId(bankAccount.getId()).stream()
                        .map(m -> new MovementDTO(m, treasuryService.resolveDescription(m))).toList()
                    : List.of();
            List<TransferDTO> cashTrans = cashAccount != null
                    ? treasuryService.findTransfersByAccountId(cashAccount.getId()).stream().map(TransferDTO::new).toList()
                    : List.of();
            List<TransferDTO> bankTrans = bankAccount != null
                    ? treasuryService.findTransfersByAccountId(bankAccount.getId()).stream().map(TransferDTO::new).toList()
                    : List.of();

            List<TransferDTO> allTrans = mergeTransfers(cashTrans, bankTrans);

            Platform.runLater(() -> {
                cashMovementList.setAll(cashMovs);
                bankMovementList.setAll(bankMovs);
                allTransferList.setAll(allTrans);

                // Reaplica filtros activos si ya había uno seleccionado
                refrescarCaja();
                refrescarBanco();
                refrescarTrans();

                updateGlobalBalance();
            });
        }).start();
    }

    private List<TransferDTO> mergeTransfers(List<TransferDTO> a, List<TransferDTO> b) {
        List<TransferDTO> merged = new ArrayList<>(a);
        for (TransferDTO t : b) {
            boolean dup = merged.stream().anyMatch(x ->
                    x.descriptionProperty().get().equals(t.descriptionProperty().get())
                    && x.dateProperty().get() != null
                    && x.dateProperty().get().equals(t.dateProperty().get())
                    && Double.compare(x.amountProperty().get(), t.amountProperty().get()) == 0);
            if (!dup) merged.add(t);
        }
        merged.sort(Comparator.comparing(t -> t.dateProperty().get(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return merged;
    }

    // ── Refresco de vistas con filtro activo ───────────────────────────────

    private void refrescarCaja() {
        LocalDate desde = resolverDesde(comboPeriodoCaja.getValue(), datesDesdeCaja.getValue());
        LocalDate hasta = resolverHasta(comboPeriodoCaja.getValue(), datesHastaCaja.getValue());
        ObservableList<MovementDTO> filtrada = filtrarMovimientos(cashMovementList, desde, hasta);
        tablaMovCaja.setItems(filtrada);
        if (lblInfoMovCaja != null) lblInfoMovCaja.setText("Registros: " + filtrada.size());
        actualizarKpisCaja(filtrada);
    }

    private void refrescarBanco() {
        LocalDate desde = resolverDesde(comboPeriodoBanco.getValue(), datesDesdeBanco.getValue());
        LocalDate hasta = resolverHasta(comboPeriodoBanco.getValue(), datesHastaBanco.getValue());
        ObservableList<MovementDTO> filtrada = filtrarMovimientos(bankMovementList, desde, hasta);
        tablaMovBanco.setItems(filtrada);
        if (lblInfoMovBanco != null) lblInfoMovBanco.setText("Registros: " + filtrada.size());
        actualizarKpisBanco(filtrada);
    }

    private void refrescarTrans() {
        LocalDate desde = resolverDesde(comboPeriodoTrans.getValue(), datesDesdeTrans.getValue());
        LocalDate hasta = resolverHasta(comboPeriodoTrans.getValue(), datesHastaTrans.getValue());
        ObservableList<TransferDTO> filtrada = filtrarTransferencias(allTransferList, desde, hasta);
        tablaTransferencias.setItems(filtrada);
        if (lblInfoTrans != null) lblInfoTrans.setText("Registros: " + filtrada.size());
        actualizarKpisTrans(filtrada);
    }

    // ── KPIs ───────────────────────────────────────────────────────────────

    private void actualizarKpisCaja(ObservableList<MovementDTO> lista) {
        double ingreso = sumMov(lista, "Ingreso");
        double egreso  = sumMov(lista, "Egreso");
        double saldo   = cashAccount != null ? cashAccount.getCurrentBalance() : 0.0;
        if (lblIngCaja  != null) lblIngCaja.setText(currencyFormat.format(ingreso));
        if (lblEgrCaja  != null) lblEgrCaja.setText(currencyFormat.format(egreso));
        if (lblSaldoCaja != null) lblSaldoCaja.setText(currencyFormat.format(saldo));
    }

    private void actualizarKpisBanco(ObservableList<MovementDTO> lista) {
        double ingreso = sumMov(lista, "Ingreso");
        double egreso  = sumMov(lista, "Egreso");
        double saldo   = bankAccount != null ? bankAccount.getCurrentBalance() : 0.0;
        if (lblIngBanco  != null) lblIngBanco.setText(currencyFormat.format(ingreso));
        if (lblEgrBanco  != null) lblEgrBanco.setText(currencyFormat.format(egreso));
        if (lblSaldoBanco != null) lblSaldoBanco.setText(currencyFormat.format(saldo));
    }

    private void actualizarKpisTrans(ObservableList<TransferDTO> lista) {
        double enviadoCaja = lista.stream()
                .filter(t -> cashAccount != null && cashAccount.getName().equals(t.sourceProperty().get()))
                .mapToDouble(t -> t.amountProperty().get()).sum();
        double enviadoBanco = lista.stream()
                .filter(t -> bankAccount != null && bankAccount.getName().equals(t.sourceProperty().get()))
                .mapToDouble(t -> t.amountProperty().get()).sum();
        if (lblEnviadoCaja  != null) lblEnviadoCaja.setText(currencyFormat.format(enviadoCaja));
        if (lblEnviadoBanco != null) lblEnviadoBanco.setText(currencyFormat.format(enviadoBanco));
    }

    private void updateGlobalBalance() {
        double cashBal = cashAccount != null ? cashAccount.getCurrentBalance() : 0.0;
        double bankBal = bankAccount != null ? bankAccount.getCurrentBalance() : 0.0;
        if (lblTotalGlobal != null) lblTotalGlobal.setText(currencyFormat.format(cashBal + bankBal));
    }

    private double sumMov(ObservableList<MovementDTO> list, String tipo) {
        return list.stream()
                .filter(m -> tipo.equalsIgnoreCase(m.typeProperty().get()))
                .mapToDouble(m -> m.amountProperty().get())
                .sum();
    }

    // ── Eventos combo periodo ──────────────────────────────────────────────

    @FXML void onPeriodoCajaChanged(ActionEvent e) {
        boolean personalizado = PERSONALIZADO.equals(comboPeriodoCaja.getValue());
        setRangoVisible(rangoCaja, personalizado);
        if (!personalizado) refrescarCaja();
    }

    @FXML void onPeriodoBancoChanged(ActionEvent e) {
        boolean personalizado = PERSONALIZADO.equals(comboPeriodoBanco.getValue());
        setRangoVisible(rangoBanco, personalizado);
        if (!personalizado) refrescarBanco();
    }

    @FXML void onPeriodoTransChanged(ActionEvent e) {
        boolean personalizado = PERSONALIZADO.equals(comboPeriodoTrans.getValue());
        setRangoVisible(rangoTrans, personalizado);
        if (!personalizado) refrescarTrans();
    }

    private void setRangoVisible(VBox rango, boolean visible) {
        rango.setVisible(visible);
        rango.setManaged(visible);
    }

    // ── Acciones filtro Caja ───────────────────────────────────────────────

    @FXML void resetFiltrosCaja(ActionEvent e) {
        datesDesdeCaja.setValue(null);
        datesHastaCaja.setValue(null);
        setRangoVisible(rangoCaja, false);
        comboPeriodoCaja.setValue("Hoy");
        refrescarCaja();
    }

    @FXML void aplicarFiltrosCaja(ActionEvent e) {
        if (!validarRangoPersonalizado(comboPeriodoCaja, datesDesdeCaja.getValue(), datesHastaCaja.getValue(), "Caja")) return;
        refrescarCaja();
    }

    // ── Acciones filtro Banco ──────────────────────────────────────────────

    @FXML void resetFiltrosBanco(ActionEvent e) {
        datesDesdeBanco.setValue(null);
        datesHastaBanco.setValue(null);
        setRangoVisible(rangoBanco, false);
        comboPeriodoBanco.setValue("Hoy");
        refrescarBanco();
    }

    @FXML void aplicarFiltrosBanco(ActionEvent e) {
        if (!validarRangoPersonalizado(comboPeriodoBanco, datesDesdeBanco.getValue(), datesHastaBanco.getValue(), "Banco")) return;
        refrescarBanco();
    }

    // ── Acciones filtro Trans ──────────────────────────────────────────────

    @FXML void resetFiltrosTrans(ActionEvent e) {
        datesDesdeTrans.setValue(null);
        datesHastaTrans.setValue(null);
        setRangoVisible(rangoTrans, false);
        comboPeriodoTrans.setValue("Hoy");
        refrescarTrans();
    }

    @FXML void aplicarFiltrosTrans(ActionEvent e) {
        if (!validarRangoPersonalizado(comboPeriodoTrans, datesDesdeTrans.getValue(), datesHastaTrans.getValue(), "Transferencias")) return;
        refrescarTrans();
    }

    private boolean validarRangoPersonalizado(ComboBox<String> combo, LocalDate desde, LocalDate hasta, String seccion) {
        if (!PERSONALIZADO.equals(combo.getValue())) return true;
        if (desde == null || hasta == null) {
            ToastNotification.warning(combo, "Selecciona ambas fechas para filtrar " + seccion);
            return false;
        }
        if (desde.isAfter(hasta)) {
            ToastNotification.warning(combo, "La fecha de inicio no puede ser posterior a la fecha final");
            return false;
        }
        return true;
    }

    // ── Modal transferencia ────────────────────────────────────────────────

    @FXML void abrirModalTransferencia(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/autollantas/gestion/treasury/views/TransferForm.fxml"));
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
                ToastNotification.success(lblTotalGlobal, "Transferencia registrada correctamente");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            ToastNotification.error(lblTotalGlobal, "No se pudo abrir el formulario de transferencia");
        }
    }

    // ── Configuración de tablas ────────────────────────────────────────────

    private void configureTables() {
        tablaMovCaja.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaMovBanco.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaTransferencias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tablaMovCaja.setItems(cashMovementList);
        colCajaMovFecha.setCellValueFactory(c -> c.getValue().dateProperty());
        colCajaMovTipo.setCellValueFactory(c -> c.getValue().typeProperty());
        colCajaMovConcepto.setCellValueFactory(c -> c.getValue().descriptionProperty());
        colCajaMovMonto.setCellValueFactory(c -> c.getValue().amountProperty().asObject());
        applyDateFormat(colCajaMovFecha);
        applyCurrencyFormat(colCajaMovMonto);
        formatTypeColumn(colCajaMovTipo);

        tablaMovBanco.setItems(bankMovementList);
        colBancoMovFecha.setCellValueFactory(c -> c.getValue().dateProperty());
        colBancoMovTipo.setCellValueFactory(c -> c.getValue().typeProperty());
        colBancoMovConcepto.setCellValueFactory(c -> c.getValue().descriptionProperty());
        colBancoMovMonto.setCellValueFactory(c -> c.getValue().amountProperty().asObject());
        applyDateFormat(colBancoMovFecha);
        applyCurrencyFormat(colBancoMovMonto);
        formatTypeColumn(colBancoMovTipo);

        tablaTransferencias.setItems(allTransferList);
        colTransFecha.setCellValueFactory(c -> c.getValue().dateProperty());
        colTransConcepto.setCellValueFactory(c -> c.getValue().descriptionProperty());
        colTransValor.setCellValueFactory(c -> c.getValue().amountProperty().asObject());
        colTransOrigen.setCellValueFactory(c -> c.getValue().sourceProperty());
        colTransDestino.setCellValueFactory(c -> c.getValue().destinationProperty());
        applyDateFormat(colTransFecha);
        applyCurrencyFormat(colTransValor);
    }

    private void formatTypeColumn(TableColumn<MovementDTO, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item.toUpperCase());
                lbl.setAlignment(Pos.CENTER);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                lbl.setTextFill(Color.web(
                        item.equalsIgnoreCase("Ingreso") || item.equalsIgnoreCase("Venta")
                                ? "#27ae60" : "#c0392b"));
                setGraphic(lbl);
            }
        });
    }

    // ── Helpers fecha ──────────────────────────────────────────────────────

    private LocalDate resolverDesde(String periodo, LocalDate manual) {
        if (periodo == null || periodo.equals(PERSONALIZADO)) return manual;
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case "Hoy"          -> hoy;
            case "Esta semana"  -> hoy.with(DayOfWeek.MONDAY);
            case "Este mes"     -> hoy.withDayOfMonth(1);
            case "Mes anterior" -> hoy.minusMonths(1).withDayOfMonth(1);
            case "Este año"     -> hoy.withDayOfYear(1);
            default             -> manual;
        };
    }

    private LocalDate resolverHasta(String periodo, LocalDate manual) {
        if (periodo == null || periodo.equals(PERSONALIZADO)) return manual;
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case "Hoy"          -> hoy;
            case "Esta semana"  -> hoy.with(DayOfWeek.SUNDAY);
            case "Este mes"     -> hoy.withDayOfMonth(hoy.lengthOfMonth());
            case "Mes anterior" -> {
                LocalDate p = hoy.minusMonths(1).withDayOfMonth(1);
                yield p.withDayOfMonth(p.lengthOfMonth());
            }
            case "Este año"     -> hoy.withDayOfYear(hoy.lengthOfYear());
            default             -> manual;
        };
    }

    private ObservableList<MovementDTO> filtrarMovimientos(ObservableList<MovementDTO> src,
                                                           LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return src;
        return src.filtered(m -> {
            LocalDate f = m.dateProperty().get();
            return f != null
                    && (desde == null || !f.isBefore(desde))
                    && (hasta == null || !f.isAfter(hasta));
        });
    }

    private ObservableList<TransferDTO> filtrarTransferencias(ObservableList<TransferDTO> src,
                                                              LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return src;
        return src.filtered(t -> {
            LocalDate f = t.dateProperty().get();
            return f != null
                    && (desde == null || !f.isBefore(desde))
                    && (hasta == null || !f.isAfter(hasta));
        });
    }

    // ── Formato celdas ─────────────────────────────────────────────────────

    private <T> void applyDateFormat(TableColumn<T, LocalDate> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormat.format(item));
            }
        });
    }

    private <T> void applyCurrencyFormat(TableColumn<T, Double> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });
    }
}

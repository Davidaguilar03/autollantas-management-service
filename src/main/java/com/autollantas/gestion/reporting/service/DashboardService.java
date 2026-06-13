package com.autollantas.gestion.reporting.service;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.reporting.model.MovementDto;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.repository.OccasionalIncomeRepository;
import com.autollantas.gestion.treasury.repository.OperationalExpenseRepository;
import com.autollantas.gestion.treasury.service.TreasuryService;
import com.autollantas.gestion.inventory.service.InventoryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final SalesService salesService;
    private final SaleRepository saleRepository;
    private final PurchasesService purchasesService;
    private final PurchaseRepository purchaseRepository;
    private final TreasuryService treasuryService;
    private final InventoryService inventoryService;
    private final OperationalExpenseRepository expenseRepository;
    private final OccasionalIncomeRepository incomeRepository;

    public DashboardService(SalesService salesService,
                            SaleRepository saleRepository,
                            PurchasesService purchasesService,
                            PurchaseRepository purchaseRepository,
                            TreasuryService treasuryService,
                            InventoryService inventoryService,
                            OperationalExpenseRepository expenseRepository,
                            OccasionalIncomeRepository incomeRepository) {
        this.salesService        = salesService;
        this.saleRepository      = saleRepository;
        this.purchasesService    = purchasesService;
        this.purchaseRepository  = purchaseRepository;
        this.treasuryService     = treasuryService;
        this.inventoryService    = inventoryService;
        this.expenseRepository   = expenseRepository;
        this.incomeRepository    = incomeRepository;
    }

    /** Máximo de movimientos devueltos por periodo para no saturar la UI. */
    public static final int MAX_MOVEMENTS = 100;

    @Transactional(readOnly = true)
    public List<MovementDto> getMovements(LocalDate start, LocalDate end) {
        List<MovementDto> movements = new ArrayList<>();

        for (Sale sale : salesService.findSalesByDateBetween(start, end)) {
            String customerName = (sale.getCustomer() != null && sale.getCustomer().getName() != null)
                    ? sale.getCustomer().getName() : "";
            String invoiceNumber = sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "Venta";
            String concept = customerName.isBlank() ? invoiceNumber : invoiceNumber + " · " + customerName;

            MovementDto dto = new MovementDto(
                    sale.getSaleDate(), sale.getId(), "Venta", sale.getTotal(), sale.getAccount());
            dto.setSourceTable("VENTA");
            dto.setConcept(concept);
            movements.add(dto);
        }

        for (Purchase purchase : purchasesService.findPurchasesByDateBetween(start, end)) {
            String supplierName = (purchase.getSupplier() != null && purchase.getSupplier().getName() != null)
                    ? purchase.getSupplier().getName() : "";
            String invoiceNumber = purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "Compra";
            String concept = supplierName.isBlank() ? invoiceNumber : invoiceNumber + " · " + supplierName;

            MovementDto dto = new MovementDto(
                    purchase.getPurchaseDate(), purchase.getId(), "Costo", purchase.getTotal(), purchase.getAccount());
            dto.setSourceTable("COMPRA");
            dto.setConcept(concept);
            movements.add(dto);
        }

        for (OperationalExpense expense : treasuryService.findExpensesByDateBetween(start, end)) {
            String concept = expense.getConcept() != null && !expense.getConcept().isBlank()
                    ? expense.getConcept() : "Gasto operativo";

            MovementDto dto = new MovementDto(
                    expense.getDate(), expense.getId(), "Gasto", expense.getAmount(), expense.getAccount());
            dto.setSourceTable("GASTO");
            dto.setConcept(concept);
            movements.add(dto);
        }

        for (OccasionalIncome income : treasuryService.findIncomesByDateBetween(start, end)) {
            String concept = income.getConcept() != null && !income.getConcept().isBlank()
                    ? income.getConcept() : "Ingreso ocasional";

            MovementDto dto = new MovementDto(
                    income.getDate(), income.getId(), "Ingreso", income.getAmount(), income.getAccount());
            dto.setSourceTable("INGRESO");
            dto.setConcept(concept);
            movements.add(dto);
        }

        movements.sort((m1, m2) -> m2.getDate().compareTo(m1.getDate()));

        if (movements.size() > MAX_MOVEMENTS) {
            return movements.subList(0, MAX_MOVEMENTS);
        }
        return movements;
    }

    /**
     * KPIs del periodo calculados con queries SUM directas en la BD,
     * sin cargar entidades completas en memoria.
     */
    @Transactional(readOnly = true)
    public PeriodKpis getPeriodKpis(LocalDate start, LocalDate end) {
        double totalIncome   = saleRepository.sumTotalByDateBetween(start, end)
                             + incomeRepository.sumAmountByDateBetween(start, end);
        double totalCosts    = purchaseRepository.sumTotalByDateBetween(start, end);
        double totalExpenses = expenseRepository.sumAmountByDateBetween(start, end);
        double netResult     = totalIncome - totalCosts - totalExpenses;

        return new PeriodKpis(totalIncome, totalExpenses, totalCosts, netResult);
    }

    @Transactional(readOnly = true)
    public DashboardKpis getGlobalKpis() {
        double totalReceivable = saleRepository.sumPendingReceivable();
        double totalPayable    = purchaseRepository.sumPendingPayable();

        return new DashboardKpis(
                totalReceivable,
                totalPayable,
                treasuryService.getTotalAccountBalance(),
                inventoryService.countStockAlerts()
        );
    }

    // ── DTOs de resultado ────────────────────────────────────────────────────

    public static class PeriodKpis {
        private final double totalIncome;
        private final double totalExpenses;
        private final double totalCosts;
        private final double netResult;

        public PeriodKpis(double totalIncome, double totalExpenses, double totalCosts, double netResult) {
            this.totalIncome   = totalIncome;
            this.totalExpenses = totalExpenses;
            this.totalCosts    = totalCosts;
            this.netResult     = netResult;
        }

        public double getTotalIncome()    { return totalIncome; }
        public double getTotalExpenses()  { return totalExpenses; }
        public double getTotalCosts()     { return totalCosts; }
        public double getNetResult()      { return netResult; }
    }

    public static class DashboardKpis {
        private final double totalReceivable;
        private final double totalPayable;
        private final double totalBalance;
        private final long alertCount;

        public DashboardKpis(double totalReceivable, double totalPayable, double totalBalance, long alertCount) {
            this.totalReceivable = totalReceivable;
            this.totalPayable    = totalPayable;
            this.totalBalance    = totalBalance;
            this.alertCount      = alertCount;
        }

        public double getTotalReceivable() { return totalReceivable; }
        public double getTotalPayable()    { return totalPayable; }
        public double getTotalBalance()    { return totalBalance; }
        public long getAlertCount()        { return alertCount; }
    }
}

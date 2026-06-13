package com.autollantas.gestion.treasury.service;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.AccountType;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.Transfer;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.CollectionRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import com.autollantas.gestion.treasury.repository.OccasionalIncomeRepository;
import com.autollantas.gestion.treasury.repository.OperationalExpenseRepository;
import com.autollantas.gestion.treasury.repository.PaymentRepository;
import com.autollantas.gestion.treasury.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TreasuryService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final TransferRepository transferRepository;
    private final OperationalExpenseRepository operationalExpenseRepository;
    private final OccasionalIncomeRepository occasionalIncomeRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final CollectionRepository collectionRepository;
    private final PaymentRepository paymentRepository;

    public TreasuryService(AccountRepository accountRepository,
                           MovementRepository movementRepository,
                           TransferRepository transferRepository,
                           OperationalExpenseRepository operationalExpenseRepository,
                           OccasionalIncomeRepository occasionalIncomeRepository,
                           SaleRepository saleRepository,
                           PurchaseRepository purchaseRepository,
                           CollectionRepository collectionRepository,
                           PaymentRepository paymentRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.transferRepository = transferRepository;
        this.operationalExpenseRepository = operationalExpenseRepository;
        this.occasionalIncomeRepository = occasionalIncomeRepository;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.collectionRepository = collectionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Assigns AccountType to existing accounts that still have type=null,
     * inferring from the account name. Safe to call on every startup.
     */
    @Transactional
    public void migrateAccountTypes() {
        for (Account account : accountRepository.findAll()) {
            if (account.getType() != null) continue;
            String upper = account.getName().toUpperCase();
            if (upper.contains("CAJA") || upper.contains("EFECTIVO")) {
                account.setType(AccountType.CASH);
            } else if (upper.contains("BANCO") || upper.contains("COLOMBIA")) {
                account.setType(AccountType.BANK);
            }
            accountRepository.save(account);
        }
    }

    @Transactional
    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public double getTotalAccountBalance() {
        return accountRepository.findAll().stream()
                .mapToDouble(a -> a.getCurrentBalance() != null ? a.getCurrentBalance() : 0.0)
                .sum();
    }

    @Transactional(readOnly = true)
    public List<Movement> findMovementsByAccountId(Integer accountId) {
        return movementRepository.findByAccount_IdOrderByDateDesc(accountId);
    }

    @Transactional(readOnly = true)
    public String resolveDescription(Movement m) {
        if (m.getSourceTable() == null || m.getSourceId() == null) return "General";
        return switch (m.getSourceTable()) {
            case "VENTAS" -> saleRepository.findById(m.getSourceId())
                    .map(s -> {
                        String inv = s.getInvoiceNumber() != null ? s.getInvoiceNumber() : "Venta";
                        String cli = s.getCustomer() != null ? s.getCustomer().getName() : "";
                        return cli.isBlank() ? inv : inv + " · " + cli;
                    }).orElse("Venta #" + m.getSourceId());
            case "COMPRAS" -> purchaseRepository.findById(m.getSourceId())
                    .map(p -> {
                        String inv = p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "Compra";
                        String prov = p.getSupplier() != null ? p.getSupplier().getName() : "";
                        return prov.isBlank() ? inv : inv + " · " + prov;
                    }).orElse("Compra #" + m.getSourceId());
            case "RECAUDOS" -> collectionRepository.findById(m.getSourceId())
                    .map(c -> {
                        Sale s = c.getSale();
                        String inv = (s != null && s.getInvoiceNumber() != null) ? s.getInvoiceNumber() : "Recaudo";
                        String cli = (s != null && s.getCustomer() != null) ? s.getCustomer().getName() : "";
                        return "Recaudo " + (cli.isBlank() ? inv : inv + " · " + cli);
                    }).orElse("Recaudo #" + m.getSourceId());
            case "PAGOS" -> paymentRepository.findById(m.getSourceId())
                    .map(p -> {
                        Purchase pu = p.getPurchase();
                        String inv = (pu != null && pu.getInvoiceNumber() != null) ? pu.getInvoiceNumber() : "Pago";
                        String prov = (pu != null && pu.getSupplier() != null) ? pu.getSupplier().getName() : "";
                        return "Pago " + (prov.isBlank() ? inv : inv + " · " + prov);
                    }).orElse("Pago #" + m.getSourceId());
            case "GASTOS_OPERATIVOS" -> operationalExpenseRepository.findById(m.getSourceId().longValue())
                    .map(e -> e.getConcept() != null ? e.getConcept() : "Gasto operativo")
                    .orElse("Gasto #" + m.getSourceId());
            case "INGRESOS_OCASIONALES" -> occasionalIncomeRepository.findById(m.getSourceId())
                    .map(i -> i.getConcept() != null ? i.getConcept() : "Ingreso ocasional")
                    .orElse("Ingreso #" + m.getSourceId());
            case "TRANSFERENCIAS" -> transferRepository.findById(m.getSourceId())
                    .map(t -> {
                        String src = t.getSourceAccount() != null ? t.getSourceAccount().getName() : "?";
                        String dst = t.getDestinationAccount() != null ? t.getDestinationAccount().getName() : "?";
                        String con = (t.getConcept() != null && !t.getConcept().isBlank()) ? " · " + t.getConcept() : "";
                        return src + " → " + dst + con;
                    }).orElse(m.getSourceId().toString());
            default -> m.getSourceTable() + " #" + m.getSourceId();
        };
    }

    @Transactional(readOnly = true)
    public List<Transfer> findTransfersByAccountId(Integer accountId) {
        return transferRepository.findBySourceAccount_IdOrDestinationAccount_IdOrderByDateDesc(accountId, accountId);
    }

    @Transactional
    public void registerTransfer(Account source, Account destination, double amount, LocalDate date, String concept) {
        source.setCurrentBalance(source.getCurrentBalance() - amount);
        destination.setCurrentBalance(destination.getCurrentBalance() + amount);

        Transfer transfer = new Transfer();
        transfer.setDate(date);
        transfer.setAmount(amount);
        transfer.setSourceAccount(source);
        transfer.setDestinationAccount(destination);
        transfer.setConcept(concept != null && !concept.isBlank() ? concept : null);

        accountRepository.save(source);
        accountRepository.save(destination);
        transferRepository.save(transfer);

        Movement egreso = new Movement(date, transfer.getId(), "Egreso", amount, source);
        egreso.setSourceTable("TRANSFERENCIAS");
        movementRepository.save(egreso);

        Movement ingreso = new Movement(date, transfer.getId(), "Ingreso", amount, destination);
        ingreso.setSourceTable("TRANSFERENCIAS");
        movementRepository.save(ingreso);
    }

    @Transactional(readOnly = true)
    public List<OperationalExpense> findAllOperationalExpenses() {
        return operationalExpenseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OperationalExpense> findExpensesByDateBetween(LocalDate start, LocalDate end) {
        return operationalExpenseRepository.findByDateBetween(start, end);
    }

    @Transactional
    public OperationalExpense saveOperationalExpense(OperationalExpense expense) {
        OperationalExpense saved = operationalExpenseRepository.save(expense);
        if (expense.getAccount() != null && expense.getAmount() != null) {
            Account account = expense.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current - expense.getAmount());
            accountRepository.save(account);
            Movement movement = new Movement(
                    expense.getDate() != null ? expense.getDate() : LocalDate.now(),
                    saved.getId(), "Egreso", expense.getAmount(), account);
            movement.setSourceTable("GASTOS_OPERATIVOS");
            movementRepository.save(movement);
        }
        return saved;
    }

    @Transactional
    public void deleteOperationalExpense(OperationalExpense expense) {
        if (expense.getAccount() != null && expense.getAmount() != null) {
            Account account = expense.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current + expense.getAmount());
            accountRepository.save(account);
        }
        if (expense.getId() != null) {
            for (Movement m : movementRepository.findBySourceIdAndSourceTable(expense.getId(), "GASTOS_OPERATIVOS")) {
                movementRepository.delete(m);
            }
        }
        operationalExpenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<OccasionalIncome> findAllOccasionalIncomes() {
        return occasionalIncomeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OccasionalIncome> findIncomesByDateBetween(LocalDate start, LocalDate end) {
        return occasionalIncomeRepository.findByDateBetween(start, end);
    }

    @Transactional
    public OccasionalIncome saveOccasionalIncome(OccasionalIncome income, boolean updateAccountBalance) {
        if (updateAccountBalance && income.getAccount() != null && income.getAmount() != null) {
            Account account = income.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current + income.getAmount());
            accountRepository.save(account);
        }
        OccasionalIncome saved = occasionalIncomeRepository.save(income);
        if (updateAccountBalance && income.getAccount() != null && income.getAmount() != null) {
            Movement movement = new Movement(
                    income.getDate() != null ? income.getDate() : LocalDate.now(),
                    saved.getId(), "Ingreso", income.getAmount(), income.getAccount());
            movement.setSourceTable("INGRESOS_OCASIONALES");
            movementRepository.save(movement);
        }
        return saved;
    }

    @Transactional
    public void deleteOccasionalIncome(OccasionalIncome income) {
        Account account = income.getAccount();
        if (account != null && income.getAmount() != null) {
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current - income.getAmount());
            accountRepository.save(account);
        }
        if (income.getId() != null) {
            for (Movement m : movementRepository.findBySourceIdAndSourceTable(income.getId(), "INGRESOS_OCASIONALES")) {
                movementRepository.delete(m);
            }
        }
        occasionalIncomeRepository.delete(income);
    }
}

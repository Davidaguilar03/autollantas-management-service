package com.autollantas.gestion.treasury.service;

import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.Transfer;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import com.autollantas.gestion.treasury.repository.OccasionalIncomeRepository;
import com.autollantas.gestion.treasury.repository.OperationalExpenseRepository;
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

    public TreasuryService(AccountRepository accountRepository,
                           MovementRepository movementRepository,
                           TransferRepository transferRepository,
                           OperationalExpenseRepository operationalExpenseRepository,
                           OccasionalIncomeRepository occasionalIncomeRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.transferRepository = transferRepository;
        this.operationalExpenseRepository = operationalExpenseRepository;
        this.occasionalIncomeRepository = occasionalIncomeRepository;
    }

    @Transactional(readOnly = true)
    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
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
    public List<Transfer> findTransfersByAccountId(Integer accountId) {
        return transferRepository.findBySourceAccount_IdOrDestinationAccount_IdOrderByDateDesc(accountId, accountId);
    }

    @Transactional
    public void registerTransfer(Account source, Account destination, double amount, LocalDate date) {
        source.setCurrentBalance(source.getCurrentBalance() - amount);
        destination.setCurrentBalance(destination.getCurrentBalance() + amount);

        Transfer transfer = new Transfer();
        transfer.setDate(date);
        transfer.setAmount(amount);
        transfer.setSourceAccount(source);
        transfer.setDestinationAccount(destination);

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

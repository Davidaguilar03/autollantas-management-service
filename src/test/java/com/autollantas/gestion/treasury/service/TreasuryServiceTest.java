package com.autollantas.gestion.treasury.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

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

// HALLAZGO ARQUITECTURAL: movementRepository.save() NUNCA se invoca en producción.
// TreasuryService inyecta MovementRepository solo para lecturas
// (findByAccount_IdOrderByDateDesc). Las escrituras de Movement solo ocurren en
// DataInitializer (seed). Los controllers JavaFX no tienen acceso a
// MovementRepository; delegan TODO en TreasuryService, que tampoco crea
// movimientos al registrar transferencias, gastos ni ingresos.
//
// Consecuencia: no se puede testear "crea Movement de Egreso/Ingreso" porque esa
// lógica no existe en ningún Service. Requiere refactor (mover la creación de
// Movement a TreasuryService) antes de poder cubrirla con unit tests.

@ExtendWith(MockitoExtension.class)
class TreasuryServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock MovementRepository movementRepository;
    @Mock TransferRepository transferRepository;
    @Mock OperationalExpenseRepository operationalExpenseRepository;
    @Mock OccasionalIncomeRepository occasionalIncomeRepository;

    @InjectMocks TreasuryService treasuryService;

    // ===== Transferencias entre cuentas =====

    @Nested
    class Transferencias {

        @Test
        void registrarTransferencia_deberia_debitar_cuentaOrigen() {
            Account source = new Account();
            source.setCurrentBalance(1000000.0);
            Account dest = new Account();
            dest.setCurrentBalance(500000.0);

            treasuryService.registerTransfer(source, dest, 300000.0, LocalDate.now(), null);

            assertThat(source.getCurrentBalance()).isEqualTo(700000.0);
        }

        @Test
        void registrarTransferencia_deberia_acreditar_cuentaDestino() {
            Account source = new Account();
            source.setCurrentBalance(1000000.0);
            Account dest = new Account();
            dest.setCurrentBalance(500000.0);

            treasuryService.registerTransfer(source, dest, 300000.0, LocalDate.now(), null);

            assertThat(dest.getCurrentBalance()).isEqualTo(800000.0);
        }

        @Test
        void registrarTransferencia_deberia_guardar_transferencia_y_ambas_cuentas() {
            Account source = new Account();
            source.setCurrentBalance(2000000.0);
            Account dest = new Account();
            dest.setCurrentBalance(0.0);

            treasuryService.registerTransfer(source, dest, 500000.0, LocalDate.now(), null);

            verify(transferRepository).save(any(Transfer.class));
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(movementRepository, times(2)).save(any());
        }

        @Test
        void registrarTransferencia_conSaldoInsuficiente_permiteBalanceNegativo() {
            // La validación de saldo insuficiente vive en TransferFormController
            // (controller JavaFX), no en TreasuryService. El servicio no lanza
            // excepción; simplemente deja el saldo en negativo.
            Account source = new Account();
            source.setCurrentBalance(100000.0);
            Account dest = new Account();
            dest.setCurrentBalance(0.0);

            treasuryService.registerTransfer(source, dest, 500000.0, LocalDate.now(), null);

            assertThat(source.getCurrentBalance()).isEqualTo(-400000.0);
        }
    }

    // ===== Gastos operativos =====

    @Nested
    class GastosOperativos {

        @Test
        void guardarGasto_deberia_persistir_elGasto() {
            OperationalExpense expense = new OperationalExpense();
            expense.setConcept("Electricidad");
            expense.setAmount(150000.0);
            when(operationalExpenseRepository.save(any())).thenReturn(expense);

            treasuryService.saveOperationalExpense(expense);

            verify(operationalExpenseRepository).save(expense);
        }

        @Test
        void guardarGasto_noDeberia_tocar_saldoDeCuenta() {
            // saveOperationalExpense() solo persiste la entidad. La actualización
            // del balance de cuenta NO está implementada en TreasuryService;
            // tampoco crea Movement. Es una brecha de cobertura documentada arriba.
            OperationalExpense expense = new OperationalExpense();
            expense.setAmount(50000.0);
            when(operationalExpenseRepository.save(any())).thenReturn(expense);

            treasuryService.saveOperationalExpense(expense);

            verify(accountRepository, never()).save(any());
            verify(movementRepository, never()).save(any());
        }
    }

    // ===== Ingresos ocasionales =====

    @Nested
    class IngresosOcasionales {

        @Test
        void guardarIngreso_conUpdateBalance_deberia_aumentar_saldoDeCuenta() {
            Account account = new Account();
            account.setCurrentBalance(200000.0);

            OccasionalIncome income = new OccasionalIncome();
            income.setAccount(account);
            income.setAmount(50000.0);
            when(occasionalIncomeRepository.save(any())).thenReturn(income);

            treasuryService.saveOccasionalIncome(income, true);

            assertThat(account.getCurrentBalance()).isEqualTo(250000.0);
            verify(accountRepository).save(account);
        }

        @Test
        void guardarIngreso_sinUpdateBalance_noDeberiaTocar_cuenta() {
            Account account = new Account();
            account.setCurrentBalance(200000.0);

            OccasionalIncome income = new OccasionalIncome();
            income.setAccount(account);
            income.setAmount(50000.0);
            when(occasionalIncomeRepository.save(any())).thenReturn(income);

            treasuryService.saveOccasionalIncome(income, false);

            assertThat(account.getCurrentBalance()).isEqualTo(200000.0);
            verify(accountRepository, never()).save(any());
        }

        @Test
        void eliminarIngreso_deberia_revertir_saldo_y_eliminar() {
            Account account = new Account();
            account.setCurrentBalance(250000.0);

            OccasionalIncome income = new OccasionalIncome();
            income.setAccount(account);
            income.setAmount(50000.0);

            treasuryService.deleteOccasionalIncome(income);

            assertThat(account.getCurrentBalance()).isEqualTo(200000.0);
            verify(occasionalIncomeRepository).delete(income);
        }
    }

    // ===== Balance total de cuentas =====

    @Nested
    class BalanceTotal {

        @Test
        void getTotalAccountBalance_deberia_sumar_saldosIgnorandoNulos() {
            Account a1 = new Account();
            a1.setCurrentBalance(1000000.0);
            Account a2 = new Account();
            a2.setCurrentBalance(500000.0);
            Account a3 = new Account();
            a3.setCurrentBalance(null); // se trata como 0

            when(accountRepository.findAll()).thenReturn(List.of(a1, a2, a3));

            double total = treasuryService.getTotalAccountBalance();

            assertThat(total).isEqualTo(1500000.0);
        }
    }

    // ===== Nuevos tests para fixes de integridad financiera =====

    @Nested
    class IntegridadFinanciera {

        @Test
        void guardarGasto_conCuenta_deberia_reducir_saldoCuenta() {
            Account account = new Account();
            account.setCurrentBalance(1000000.0);

            OperationalExpense expense = new OperationalExpense();
            expense.setAccount(account);
            expense.setAmount(150000.0);
            expense.setDate(LocalDate.now());
            when(operationalExpenseRepository.save(any())).thenReturn(expense);

            treasuryService.saveOperationalExpense(expense);

            assertThat(account.getCurrentBalance()).isEqualTo(850000.0);
            verify(accountRepository).save(account);
            verify(movementRepository).save(any());
        }

        @Test
        void eliminarGasto_deberia_revertir_saldoCuenta() {
            Account account = new Account();
            account.setCurrentBalance(850000.0);

            OperationalExpense expense = new OperationalExpense();
            expense.setId(1);
            expense.setAccount(account);
            expense.setAmount(150000.0);
            when(movementRepository.findBySourceIdAndSourceTable(1, "GASTOS_OPERATIVOS"))
                    .thenReturn(Collections.emptyList());

            treasuryService.deleteOperationalExpense(expense);

            assertThat(account.getCurrentBalance()).isEqualTo(1000000.0);
            verify(accountRepository).save(account);
            verify(operationalExpenseRepository).delete(expense);
        }

        @Test
        void eliminarGasto_deberia_eliminar_movimientoAsociado() {
            Account account = new Account();
            account.setCurrentBalance(500000.0);

            OperationalExpense expense = new OperationalExpense();
            expense.setId(5);
            expense.setAccount(account);
            expense.setAmount(100000.0);

            Movement existingMovement = new Movement();
            when(movementRepository.findBySourceIdAndSourceTable(5, "GASTOS_OPERATIVOS"))
                    .thenReturn(List.of(existingMovement));

            treasuryService.deleteOperationalExpense(expense);

            verify(movementRepository).delete(existingMovement);
        }

        @Test
        void registrarTransferencia_deberia_crear_dosMovimientos() {
            Account source = new Account();
            source.setCurrentBalance(1000000.0);
            Account dest = new Account();
            dest.setCurrentBalance(0.0);

            treasuryService.registerTransfer(source, dest, 400000.0, LocalDate.now(), null);

            verify(movementRepository, times(2)).save(any());
        }
    }
}

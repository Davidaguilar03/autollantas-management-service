package com.autollantas.gestion.service;

import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.GastoOperativo;
import com.autollantas.gestion.model.IngresoOcasional;
import com.autollantas.gestion.model.Movimiento;
import com.autollantas.gestion.model.Transferencia;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.GastoOperativoRepository;
import com.autollantas.gestion.repository.IngresoOcasionalRepository;
import com.autollantas.gestion.repository.MovimientoRepository;
import com.autollantas.gestion.repository.TransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TesoreriaService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final GastoOperativoRepository gastoOperativoRepository;
    private final IngresoOcasionalRepository ingresoOcasionalRepository;

    public TesoreriaService(CuentaRepository cuentaRepository,
                            MovimientoRepository movimientoRepository,
                            TransferenciaRepository transferenciaRepository,
                            GastoOperativoRepository gastoOperativoRepository,
                            IngresoOcasionalRepository ingresoOcasionalRepository) {
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.transferenciaRepository = transferenciaRepository;
        this.gastoOperativoRepository = gastoOperativoRepository;
        this.ingresoOcasionalRepository = ingresoOcasionalRepository;
    }

    @Transactional(readOnly = true)
    public List<Cuenta> findAllCuentas() {
        return cuentaRepository.findAll();
    }

    @Transactional
    public Cuenta saveCuenta(Cuenta cuenta) {
        return cuentaRepository.save(cuenta);
    }

    @Transactional(readOnly = true)
    public double getSaldoTotalCuentas() {
        return cuentaRepository.findAll().stream()
                .mapToDouble(c -> c.getSaldoActual() != null ? c.getSaldoActual() : 0.0)
                .sum();
    }

    @Transactional(readOnly = true)
    public List<Movimiento> findMovimientosByCuentaId(Integer idCuenta) {
        return movimientoRepository.findByCuenta_IdCuentaOrderByFechaMovimientoDesc(idCuenta);
    }

    @Transactional(readOnly = true)
    public List<Transferencia> findTransferenciasByCuentaId(Integer idCuenta) {
        return transferenciaRepository.findByCuentaOrigen_IdCuentaOrCuentaDestino_IdCuentaOrderByFechaTransferenciaDesc(idCuenta, idCuenta);
    }

    @Transactional
    public void registrarTransferencia(Cuenta origen, Cuenta destino, double monto, LocalDate fechaTransferencia) {
        origen.setSaldoActual(origen.getSaldoActual() - monto);
        destino.setSaldoActual(destino.getSaldoActual() + monto);

        Transferencia transferencia = new Transferencia();
        transferencia.setFechaTransferencia(fechaTransferencia);
        transferencia.setMontoTransferencia(monto);
        transferencia.setCuentaOrigen(origen);
        transferencia.setCuentaDestino(destino);

        cuentaRepository.save(origen);
        cuentaRepository.save(destino);
        transferenciaRepository.save(transferencia);
    }

    @Transactional(readOnly = true)
    public List<GastoOperativo> findAllGastosOperativos() {
        return gastoOperativoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<GastoOperativo> findGastosByFechaBetween(LocalDate inicio, LocalDate fin) {
        return gastoOperativoRepository.findByFechaGastoBetween(inicio, fin);
    }

    @Transactional
    public GastoOperativo saveGastoOperativo(GastoOperativo gastoOperativo) {
        return gastoOperativoRepository.save(gastoOperativo);
    }

    @Transactional
    public void deleteGastoOperativo(GastoOperativo gastoOperativo) {
        gastoOperativoRepository.delete(gastoOperativo);
    }

    @Transactional(readOnly = true)
    public List<IngresoOcasional> findAllIngresosOcasionales() {
        return ingresoOcasionalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IngresoOcasional> findIngresosByFechaBetween(LocalDate inicio, LocalDate fin) {
        return ingresoOcasionalRepository.findByFechaIngresoBetween(inicio, fin);
    }

    @Transactional
    public IngresoOcasional saveIngresoOcasional(IngresoOcasional ingresoOcasional, boolean actualizarSaldoCuenta) {
        if (actualizarSaldoCuenta && ingresoOcasional.getCuenta() != null && ingresoOcasional.getMontoIngreso() != null) {
            Cuenta cuenta = ingresoOcasional.getCuenta();
            double saldoActual = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : 0.0;
            cuenta.setSaldoActual(saldoActual + ingresoOcasional.getMontoIngreso());
            cuentaRepository.save(cuenta);
        }
        return ingresoOcasionalRepository.save(ingresoOcasional);
    }

    @Transactional
    public void deleteIngresoOcasional(IngresoOcasional ingresoOcasional) {
        Cuenta cuenta = ingresoOcasional.getCuenta();
        if (cuenta != null && ingresoOcasional.getMontoIngreso() != null) {
            double saldoActual = cuenta.getSaldoActual() != null ? cuenta.getSaldoActual() : 0.0;
            cuenta.setSaldoActual(saldoActual - ingresoOcasional.getMontoIngreso());
            cuentaRepository.save(cuenta);
        }
        ingresoOcasionalRepository.delete(ingresoOcasional);
    }
}


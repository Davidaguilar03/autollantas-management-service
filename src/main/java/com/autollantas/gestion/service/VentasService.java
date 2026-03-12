package com.autollantas.gestion.service;

import com.autollantas.gestion.model.Cliente;
import com.autollantas.gestion.model.Cuenta;
import com.autollantas.gestion.model.DetalleVenta;
import com.autollantas.gestion.model.Producto;
import com.autollantas.gestion.model.Recaudo;
import com.autollantas.gestion.model.Venta;
import com.autollantas.gestion.repository.ClienteRepository;
import com.autollantas.gestion.repository.CuentaRepository;
import com.autollantas.gestion.repository.DetalleVentaRepository;
import com.autollantas.gestion.repository.ProductoRepository;
import com.autollantas.gestion.repository.RecaudoRepository;
import com.autollantas.gestion.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class VentasService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final RecaudoRepository recaudoRepository;
    private final CuentaRepository cuentaRepository;

    public VentasService(VentaRepository ventaRepository,
                         DetalleVentaRepository detalleVentaRepository,
                         ClienteRepository clienteRepository,
                         ProductoRepository productoRepository,
                         RecaudoRepository recaudoRepository,
                         CuentaRepository cuentaRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.recaudoRepository = recaudoRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public List<Venta> findAllVentas() {
        return ventaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Venta> findVentasByFechaVentaBetween(LocalDate inicio, LocalDate fin) {
        return ventaRepository.findByFechaVentaBetween(inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<DetalleVenta> findDetallesByVenta(Venta venta) {
        return detalleVentaRepository.findByVenta(venta);
    }

    @Transactional(readOnly = true)
    public List<Recaudo> findRecaudosByVenta(Venta venta) {
        return recaudoRepository.findByVenta(venta);
    }

    @Transactional(readOnly = true)
    public List<Cliente> findAllClientes() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String generarSiguienteNumeroFactura() {
        long maximo = 0;
        for (Venta venta : ventaRepository.findAll()) {
            String numeroFactura = venta.getNumeroFacturaVenta();
            if (numeroFactura == null || numeroFactura.isEmpty()) {
                continue;
            }
            String soloNumeros = numeroFactura.replaceAll("\\D+", "");
            if (soloNumeros.isEmpty()) {
                continue;
            }
            long actual = Long.parseLong(soloNumeros);
            if (actual > maximo) {
                maximo = actual;
            }
        }
        long siguienteNumero = maximo > 0 ? maximo + 1 : 1;
        return String.format("VEN-%05d", siguienteNumero);
    }

    @Transactional
    public Cliente guardarOActualizarCliente(Cliente seleccionado,
                                             String nombre,
                                             String documento,
                                             String correo,
                                             String celular) {
        if (seleccionado != null && seleccionado.getNombreCliente() != null
                && seleccionado.getNombreCliente().equalsIgnoreCase(nombre)) {
            seleccionado.setNumeroDocumentoCliente(documento);
            seleccionado.setCorreoCliente(correo);
            seleccionado.setCelularCliente(celular);
            return clienteRepository.save(seleccionado);
        }

        Optional<Cliente> existente = clienteRepository.findByNumeroDocumentoCliente(documento);
        Cliente cliente = existente.orElse(new Cliente());
        cliente.setNombreCliente(nombre);
        cliente.setNumeroDocumentoCliente(documento);
        cliente.setCorreoCliente(correo);
        cliente.setCelularCliente(celular);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Venta guardarVentaConDetalles(Venta venta, List<DetalleVenta> nuevosDetalles, boolean modoEdicion) {
        Venta ventaGuardada = ventaRepository.save(venta);

        if (modoEdicion) {
            List<DetalleVenta> detallesAntiguos = detalleVentaRepository.findByVenta(ventaGuardada);
            for (DetalleVenta detalleAntiguo : detallesAntiguos) {
                Producto producto = detalleAntiguo.getProducto();
                if (producto != null) {
                    producto.setCantidad(producto.getCantidad() + detalleAntiguo.getCantidadVenta());
                    productoRepository.save(producto);
                }
            }
            detalleVentaRepository.deleteAll(detallesAntiguos);
        }

        for (DetalleVenta detalle : nuevosDetalles) {
            detalle.setVenta(ventaGuardada);
            detalleVentaRepository.save(detalle);

            Producto producto = detalle.getProducto();
            if (producto == null || producto.getIdProducto() == null) {
                continue;
            }

            productoRepository.findById(producto.getIdProducto()).ifPresent(productoReal -> {
                productoReal.setCantidad(productoReal.getCantidad() - detalle.getCantidadVenta());
                productoRepository.save(productoReal);
            });
        }

        return ventaGuardada;
    }

    @Transactional
    public void anularVenta(Venta venta) {
        List<DetalleVenta> detalles = detalleVentaRepository.findByVenta(venta);
        for (DetalleVenta detalle : detalles) {
            Producto producto = detalle.getProducto();
            if (producto != null) {
                producto.setCantidad(producto.getCantidad() + detalle.getCantidadVenta());
                productoRepository.save(producto);
            }
        }
        venta.setEstadoVenta("ANULADA");
        ventaRepository.save(venta);
    }

    @Transactional
    public void registrarRecaudo(Venta venta, Cuenta cuentaDestino, LocalDate fechaPago, String metodoPago, double montoAbono) {
        Recaudo nuevoRecaudo = new Recaudo();
        nuevoRecaudo.setVenta(venta);
        nuevoRecaudo.setCuenta(cuentaDestino);
        nuevoRecaudo.setFechaRecaudo(fechaPago);
        nuevoRecaudo.setMetodoPagoRecaudo(metodoPago);
        nuevoRecaudo.setValorRecaudo(montoAbono);
        recaudoRepository.save(nuevoRecaudo);

        double saldoCuenta = cuentaDestino.getSaldoActual() != null ? cuentaDestino.getSaldoActual() : 0.0;
        cuentaDestino.setSaldoActual(saldoCuenta + montoAbono);
        cuentaRepository.save(cuentaDestino);

        double deudaActual = venta.getSaldoPendiente() != null ? venta.getSaldoPendiente() : venta.getTotalVenta();
        double nuevoSaldo = deudaActual - montoAbono;
        if (nuevoSaldo < 0) {
            nuevoSaldo = 0.0;
        }

        venta.setSaldoPendiente(nuevoSaldo);
        venta.setCuenta(cuentaDestino);
        venta.setMedioPagoVenta(metodoPago);
        venta.setEstadoVenta(nuevoSaldo <= 0 ? "PAGADA" : "PENDIENTE");
        ventaRepository.save(venta);
    }
}


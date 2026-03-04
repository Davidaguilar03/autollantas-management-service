package com.autollantas.gestion;

import com.autollantas.gestion.model.*;
import com.autollantas.gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@Configuration
public class CargaDatos {

    @Bean
    CommandLineRunner importarDatosReales(CategoriaProductoRepository categoriaRepo,
                                          ProductoRepository productoRepo) {
        return args -> {
            if (productoRepo.count() > 5) {
                System.out.println("⚠️ La base de datos ya tiene productos. Omitiendo carga CSV.");
                return;
            }

            System.out.println("🚀 Iniciando carga masiva de inventario...");

            String nombreArchivo = "/INVENTARIO 2025.csv";
            Map<String, CategoriaProducto> cacheCategorias = new HashMap<>();

            try (InputStream is = getClass().getResourceAsStream(nombreArchivo)) {

                if (is == null) {
                    System.err.println("❌ ERROR: No encuentro '/INVENTARIO 2025.csv' en resources.");
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                br.readLine();

                String linea;
                int contador = 0;

                while ((linea = br.readLine()) != null) {
                    String[] datos = linea.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (datos.length < 8) continue;

                    String nombreCategoria = limpiarTexto(datos[0]);
                    String codigo = limpiarTexto(datos[1]);
                    String descripcion = limpiarTexto(datos[2]);

                    if (codigo.isEmpty() || nombreCategoria.isEmpty()) continue;

                    CategoriaProducto categoria = cacheCategorias.get(nombreCategoria);
                    if (categoria == null) {
                        categoria = new CategoriaProducto();
                        categoria.setNombreCategoriaProducto(nombreCategoria);
                        categoria.setStockMinAmarillo(5);
                        categoria.setStockMinRojo(2);
                        categoria = categoriaRepo.save(categoria);
                        cacheCategorias.put(nombreCategoria, categoria);
                    }

                    double precioBruto = limpiarYConvertirPrecio(datos[3]);
                    double iva = limpiarYConvertirPrecio(datos[4]);
                    double precioFinal = limpiarYConvertirPrecio(datos[5]);
                    double stockDouble = limpiarYConvertirPrecio(datos[6]);
                    String tipoItem = limpiarTexto(datos[7]);

                    int stock = (int) stockDouble;

                    Producto p = new Producto();
                    p.setCodigoProducto(codigo);
                    p.setDescripcion(descripcion);
                    p.setCantidad(stock);
                    p.setPrecioBrutoProducto(precioBruto);
                    p.setIvaProducto(iva);
                    p.setPrecioIvaProducto(precioFinal);
                    p.setTipoItem(tipoItem);
                    p.setCategoria(categoria);

                    productoRepo.save(p);
                    contador++;
                }
                System.out.println("✅ ¡Carga Finalizada! Se importaron " + contador + " productos.");

            } catch (Exception e) {
                System.err.println("❌ Error leyendo el archivo: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private String limpiarTexto(String texto) {
        if (texto == null) return "";
        return texto.replace("\"", "").trim();
    }

    private double limpiarYConvertirPrecio(String precioStr) {
        try {
            if (precioStr == null || precioStr.trim().isEmpty()) return 0.0;
            String limpio = precioStr.replace("\"", "").trim();
            limpio = limpio.replace(",", "");
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Bean
    CommandLineRunner cargarDatosFicticios(
            ClienteRepository clienteRepo,
            ProveedorRepository proveedorRepo,
            CuentaRepository cuentaRepo,
            MovimientoRepository movimientoRepo,
            CategoriaProductoRepository categoriaRepo,
            ProductoRepository productoRepo,
            CompraRepository compraRepo,
            DetalleCompraRepository detCompraRepo,
            PagoRepository pagoRepo,
            VentaRepository ventaRepo,
            DetalleVentaRepository detVentaRepo,
            RecaudoRepository recaudoRepo,
            GastoOperativoRepository gastoRepo,
            IngresoOcasionalRepository ingresoRepo,
            ConfiguracionRepository configRepo,
            TransferenciaRepository transferenciaRepo) {

        return args -> {

            if (configRepo.count() == 0) {
                System.out.println("⚙️ Creando configuración de seguridad y sistema...");

                crearConfig(configRepo, "IVA", "0.19");
                crearConfig(configRepo, "EMPRESA", "AUTOLLANTAS S.A.S");
                crearConfig(configRepo, "MONEDA", "COP");
                crearConfig(configRepo, "admin_password", "1234");

                crearConfig(configRepo, "recovery_pregunta", "¿Cual es el nombre de tu primera mascota?");
                crearConfig(configRepo, "recovery_respuesta", "Firulais");

                crearConfig(configRepo, "recovery_pregunta_1", "¿Cuál es el nombre de tu primera mascota?");
                crearConfig(configRepo, "recovery_respuesta_1", "Firulais");

                crearConfig(configRepo, "recovery_pregunta_2", "¿En qué ciudad naciste?");
                crearConfig(configRepo, "recovery_respuesta_2", "Bogota");

                crearConfig(configRepo, "recovery_pregunta_3", "¿Cuál es tu comida favorita?");
                crearConfig(configRepo, "recovery_respuesta_3", "Pizza");
            }

            if (cuentaRepo.count() > 0) {
                System.out.println("⚠️ Ya existen cuentas. Saltando carga de datos ficticios.");
                return;
            }

            System.out.println("🚀 Cargando datos de prueba (Compras, Ventas, Gastos)... SOLO CAJA Y BANCO");

            Cuenta ctaCaja = cuentaRepo.save(new Cuenta(1000000.0, null, "Caja General", 1000000.0));
            Cuenta ctaBanco = cuentaRepo.save(new Cuenta(50000000.0, null, "Bancolombia", 50000000.0));

            Proveedor prov1 = proveedorRepo.save(new Proveedor("3001112233", "contacto@michelin.com", null, "Michelin Colombia", "900111222-1"));
            Proveedor prov2 = proveedorRepo.save(new Proveedor("3104445566", "ventas@terpel.com", null, "Terpel S.A.", "800333444-2"));
            Proveedor prov3 = proveedorRepo.save(new Proveedor("3207778899", "admin@bosch.com", null, "Bosch Autopartes", "700555666-3"));

            Cliente cli1 = clienteRepo.save(new Cliente(null, "Carlos Perez", "10102020", "3001234567", "carlos@gmail.com"));
            Cliente cli2 = clienteRepo.save(new Cliente(null, "Maria Rodriguez", "30304040", "3109876543", "maria@hotmail.com"));
            Cliente cli3 = clienteRepo.save(new Cliente(null, "Transportes SAS", "900500600", "6017778888", "logistica@transportes.com"));

            CategoriaProducto cat1 = categoriaRepo.save(new CategoriaProducto(null, "Llantas", 20, 5));
            CategoriaProducto cat2 = categoriaRepo.save(new CategoriaProducto(null, "Aceites", 15, 3));
            CategoriaProducto cat3 = categoriaRepo.save(new CategoriaProducto(null, "Frenos", 10, 2));

            Producto prod1 = productoRepo.save(new Producto(0, cat1, "LL-001", "Llanta 205/55 R16", null, 38000.0, 200000.0, 238000.0, "Producto"));
            Producto prod2 = productoRepo.save(new Producto(0, cat2, "AC-001", "Aceite Sintetico 5W30", null, 9500.0, 50000.0, 59500.0, "Producto"));
            Producto prod3 = productoRepo.save(new Producto(0, cat3, "FR-001", "Pastillas de Freno Cerámica", null, 15200.0, 80000.0, 95200.0, "Servicio"));

            Compra compra1 = compraRepo.save(new Compra(null, prov1, ctaBanco, "FAC-00001", LocalDate.now().minusDays(10), "Contado", LocalDate.now().minusDays(10), "Transferencia", "Stock Inicial", 2380000.0, "PAGADA"));
            detCompraRepo.save(new DetalleCompra(10, compra1, 0.0, null, 380000.0, 200000.0, prod1, 2380000.0));
            pagoRepo.save(new Pago(compra1, ctaBanco, LocalDate.now().minusDays(10), null, "Transferencia", 2380000.0));
            prod1.setCantidad(10);
            productoRepo.save(prod1);
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() - 2380000.0);
            cuentaRepo.save(ctaBanco);
            Movimiento movC1 = new Movimiento(LocalDate.now().minusDays(10), compra1.getIdCompra(), "Egreso", 2380000.0, ctaBanco);
            movC1.setTablaOrigenMovimiento("COMPRAS");
            movimientoRepo.save(movC1);

            Compra compra2 = compraRepo.save(new Compra(null, prov2, ctaCaja, "FAC-00002", LocalDate.now().minusDays(8), "Contado", LocalDate.now().minusDays(8), "Efectivo", "Reposición Aceites", 595000.0, "PAGADA"));
            detCompraRepo.save(new DetalleCompra(10, compra2, 0.0, null, 95000.0, 50000.0, prod2, 595000.0));
            pagoRepo.save(new Pago(compra2, ctaCaja, LocalDate.now().minusDays(8), null, "Efectivo", 595000.0));
            prod2.setCantidad(10);
            productoRepo.save(prod2);
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() - 595000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movC2 = new Movimiento(LocalDate.now().minusDays(8), compra2.getIdCompra(), "Egreso", 595000.0, ctaCaja);
            movC2.setTablaOrigenMovimiento("COMPRAS");
            movimientoRepo.save(movC2);

            Compra compra3 = compraRepo.save(new Compra(null, prov3, ctaBanco, "FAC-00003", LocalDate.now().minusDays(5), "Credito", LocalDate.now().plusDays(25), "Transferencia", "Repuestos Frenos", 952000.0, "PENDIENTE"));
            detCompraRepo.save(new DetalleCompra(10, compra3, 0.0, null, 152000.0, 80000.0, prod3, 952000.0));
            prod3.setCantidad(10);
            productoRepo.save(prod3);

            Venta venta1 = new Venta(cli1, ctaCaja, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Efectivo", "Venta mostrador", "VEN-00001", 476000.0);
            venta1.setSaldoPendiente(0.0);
            venta1 = ventaRepo.save(venta1);
            detVentaRepo.save(new DetalleVenta(2, 0.0, null, 76000.0, 238000.0, prod1, 476000.0, venta1));
            recaudoRepo.save(new Recaudo(ctaCaja, LocalDate.now(), null, "Efectivo", 476000.0, venta1));
            prod1.setCantidad(prod1.getCantidad() - 2);
            productoRepo.save(prod1);
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() + 476000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movV1 = new Movimiento(LocalDate.now(), venta1.getIdVenta(), "Ingreso", 476000.0, ctaCaja);
            movV1.setTablaOrigenMovimiento("VENTAS");
            movimientoRepo.save(movV1);

            Venta venta2 = new Venta(cli2, ctaBanco, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Transferencia", "Mantenimiento", "VEN-00002", 238000.0);
            venta2.setSaldoPendiente(0.0);
            venta2 = ventaRepo.save(venta2);
            detVentaRepo.save(new DetalleVenta(4, 0.0, null, 38000.0, 59500.0, prod2, 238000.0, venta2));
            recaudoRepo.save(new Recaudo(ctaBanco, LocalDate.now(), null, "Transferencia", 238000.0, venta2));
            prod2.setCantidad(prod2.getCantidad() - 4);
            productoRepo.save(prod2);
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() + 238000.0);
            cuentaRepo.save(ctaBanco);
            Movimiento movV2 = new Movimiento(LocalDate.now(), venta2.getIdVenta(), "Ingreso", 238000.0, ctaBanco);
            movV2.setTablaOrigenMovimiento("VENTAS");
            movimientoRepo.save(movV2);

            Venta venta3 = new Venta(cli3, ctaBanco, "PENDIENTE", LocalDate.now().minusDays(5), LocalDate.now().plusDays(15), "Credito", null, "Credito", "Flotilla Test", "VEN-00003", 1904000.0);
            venta3 = ventaRepo.save(venta3);
            detVentaRepo.save(new DetalleVenta(4, 0.0, null, 304000.0, 95200.0, prod3, 380800.0, venta3));
            detVentaRepo.save(new DetalleVenta(4, 0.0, null, 152000.0, 238000.0, prod1, 952000.0, venta3));
            Recaudo abonoInicial = new Recaudo(ctaCaja, LocalDate.now().minusDays(2), null, "Efectivo", 500000.0, venta3);
            recaudoRepo.save(abonoInicial);
            venta3.setSaldoPendiente(1404000.0);
            ventaRepo.save(venta3);
            prod3.setCantidad(prod3.getCantidad() - 4);
            prod1.setCantidad(prod1.getCantidad() - 4);
            productoRepo.save(prod3);
            productoRepo.save(prod1);

            GastoOperativo gasto1 = gastoRepo.save(new GastoOperativo("Pago Energia", ctaCaja, LocalDate.now(), null, 150000.0, "Enel Codensa"));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() - 150000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movG1 = new Movimiento(LocalDate.now(), gasto1.getIdGasto(), "Egreso", 150000.0, ctaCaja);
            movG1.setTablaOrigenMovimiento("GASTOS_OPERATIVOS");
            movimientoRepo.save(movG1);

            GastoOperativo gasto2 = gastoRepo.save(new GastoOperativo("Nomina Ayudante", ctaBanco, LocalDate.now(), null, 1300000.0, "Pago Quincena"));
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() - 1300000.0);
            cuentaRepo.save(ctaBanco);
            Movimiento movG2 = new Movimiento(LocalDate.now(), gasto2.getIdGasto(), "Egreso", 1300000.0, ctaBanco);
            movG2.setTablaOrigenMovimiento("GASTOS_OPERATIVOS");
            movimientoRepo.save(movG2);

            GastoOperativo gasto3 = gastoRepo.save(new GastoOperativo("Cafetería", ctaCaja, LocalDate.now(), null, 50000.0, "Insumos Varios"));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() - 50000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movG3 = new Movimiento(LocalDate.now(), gasto3.getIdGasto(), "Egreso", 50000.0, ctaCaja);
            movG3.setTablaOrigenMovimiento("GASTOS_OPERATIVOS");
            movimientoRepo.save(movG3);

            IngresoOcasional ing1 = ingresoRepo.save(new IngresoOcasional("Venta Chatarra", ctaCaja, LocalDate.now(), null, 60000.0, "Reciclaje"));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() + 60000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movI1 = new Movimiento(LocalDate.now(), ing1.getIdIngreso(), "Ingreso", 60000.0, ctaCaja);
            movI1.setTablaOrigenMovimiento("INGRESOS_OCASIONALES");
            movimientoRepo.save(movI1);

            IngresoOcasional ing2 = ingresoRepo.save(new IngresoOcasional("Reembolso Seguro", ctaBanco, LocalDate.now(), null, 200000.0, "Siniestro menor"));
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() + 200000.0);
            cuentaRepo.save(ctaBanco);
            Movimiento movI2 = new Movimiento(LocalDate.now(), ing2.getIdIngreso(), "Ingreso", 200000.0, ctaBanco);
            movI2.setTablaOrigenMovimiento("INGRESOS_OCASIONALES");
            movimientoRepo.save(movI2);

            IngresoOcasional ing3 = ingresoRepo.save(new IngresoOcasional("Propina Cliente", ctaCaja, LocalDate.now(), null, 20000.0, "Servicio extra"));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() + 20000.0);
            cuentaRepo.save(ctaCaja);
            Movimiento movI3 = new Movimiento(LocalDate.now(), ing3.getIdIngreso(), "Ingreso", 20000.0, ctaCaja);
            movI3.setTablaOrigenMovimiento("INGRESOS_OCASIONALES");
            movimientoRepo.save(movI3);

            Transferencia tr1 = transferenciaRepo.save(new Transferencia(ctaBanco, ctaCaja, LocalDate.now(), null, 100000.0));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() - 100000.0);
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() - 100000.0);
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() + 100000.0);
            cuentaRepo.save(ctaCaja);
            cuentaRepo.save(ctaBanco);
            Movimiento movTrSal1 = new Movimiento(LocalDate.now(), tr1.getIdTransferencia(), "Egreso", 100000.0, ctaBanco);
            movTrSal1.setTablaOrigenMovimiento("TRANSFERENCIAS");
            movimientoRepo.save(movTrSal1);
            Movimiento movTrEnt1 = new Movimiento(LocalDate.now(), tr1.getIdTransferencia(), "Ingreso", 100000.0, ctaCaja);
            movTrEnt1.setTablaOrigenMovimiento("TRANSFERENCIAS");
            movimientoRepo.save(movTrEnt1);

            Transferencia tr2 = transferenciaRepo.save(new Transferencia(ctaCaja, ctaBanco, LocalDate.now(), null, 500000.0));
            ctaCaja.setSaldoActual(ctaCaja.getSaldoActual() - 500000.0);
            ctaBanco.setSaldoActual(ctaBanco.getSaldoActual() + 500000.0);
            cuentaRepo.save(ctaBanco);
            cuentaRepo.save(ctaCaja);
            Movimiento movTrSal2 = new Movimiento(LocalDate.now(), tr2.getIdTransferencia(), "Egreso", 500000.0, ctaCaja);
            movTrSal2.setTablaOrigenMovimiento("TRANSFERENCIAS");
            movimientoRepo.save(movTrSal2);
            Movimiento movTrEnt2 = new Movimiento(LocalDate.now(), tr2.getIdTransferencia(), "Ingreso", 500000.0, ctaBanco);
            movTrEnt2.setTablaOrigenMovimiento("TRANSFERENCIAS");
            movimientoRepo.save(movTrEnt2);

            System.out.println("✅ ¡CARGA COMPLETADA! CONFIGURACIÓN REPARADA Y DATOS CARGADOS (SOLO CAJA Y BANCO).");
        };
    }

    private void crearConfig(ConfiguracionRepository repo, String clave, String valor) {
        Configuracion c = new Configuracion();
        c.setClave(clave);
        c.setValor(valor);
        repo.save(c);
    }
}
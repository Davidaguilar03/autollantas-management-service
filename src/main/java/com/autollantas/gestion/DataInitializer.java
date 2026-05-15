package com.autollantas.gestion;

import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.sales.model.Customer;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.purchases.model.Supplier;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.Payment;
import com.autollantas.gestion.treasury.model.Collection;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.Transfer;
import com.autollantas.gestion.config.model.SystemConfig;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.sales.repository.CustomerRepository;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.sales.repository.SaleDetailRepository;
import com.autollantas.gestion.treasury.repository.CollectionRepository;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.purchases.repository.PurchaseDetailRepository;
import com.autollantas.gestion.purchases.repository.SupplierRepository;
import com.autollantas.gestion.treasury.repository.PaymentRepository;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.TransferRepository;
import com.autollantas.gestion.treasury.repository.OperationalExpenseRepository;
import com.autollantas.gestion.treasury.repository.OccasionalIncomeRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import com.autollantas.gestion.config.repository.SystemConfigRepository;
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
public class DataInitializer {

    @Bean
    CommandLineRunner loadProductData(ProductCategoryRepository categoryRepo,
                                     ProductRepository productRepo) {
        return args -> {
            if (productRepo.count() > 5) {
                System.out.println("⚠️ La base de datos ya tiene productos. Omitiendo carga CSV.");
                return;
            }

            System.out.println("🚀 Iniciando carga masiva de inventario...");

            String fileName = "/INVENTARIO 2025.csv";
            Map<String, ProductCategory> categoryCache = new HashMap<>();

            try (InputStream is = getClass().getResourceAsStream(fileName)) {

                if (is == null) {
                    System.err.println("❌ ERROR: No encuentro '/INVENTARIO 2025.csv' en resources.");
                    return;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                br.readLine();

                String line;
                int count = 0;

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (data.length < 8) continue;

                    String categoryName = cleanText(data[0]);
                    String code = cleanText(data[1]);
                    String description = cleanText(data[2]);

                    if (code.isEmpty() || categoryName.isEmpty()) continue;

                    ProductCategory category = categoryCache.get(categoryName);
                    if (category == null) {
                        category = new ProductCategory();
                        category.setName(categoryName);
                        category.setYellowStockMin(5);
                        category.setRedStockMin(2);
                        category = categoryRepo.save(category);
                        categoryCache.put(categoryName, category);
                    }

                    double basePrice = cleanAndParsePrice(data[3]);
                    double taxAmount = cleanAndParsePrice(data[4]);
                    double priceWithTax = cleanAndParsePrice(data[5]);
                    double stockDouble = cleanAndParsePrice(data[6]);
                    String itemType = cleanText(data[7]);

                    int stock = (int) stockDouble;

                    Product p = new Product();
                    p.setCode(code);
                    p.setDescription(description);
                    p.setQuantity(stock);
                    p.setBasePrice(basePrice);
                    p.setTaxAmount(taxAmount);
                    p.setPriceWithTax(priceWithTax);
                    p.setItemType(itemType);
                    p.setCategory(category);

                    productRepo.save(p);
                    count++;
                }
                System.out.println("✅ ¡Carga Finalizada! Se importaron " + count + " productos.");

            } catch (Exception e) {
                System.err.println("❌ Error leyendo el archivo: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replace("\"", "").trim();
    }

    private double cleanAndParsePrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.trim().isEmpty()) return 0.0;
            String clean = priceStr.replace("\"", "").trim();
            clean = clean.replace(",", "");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Bean
    CommandLineRunner loadSampleData(
            CustomerRepository customerRepo,
            SupplierRepository supplierRepo,
            AccountRepository accountRepo,
            MovementRepository movementRepo,
            ProductCategoryRepository categoryRepo,
            ProductRepository productRepo,
            PurchaseRepository purchaseRepo,
            PurchaseDetailRepository purchaseDetailRepo,
            PaymentRepository paymentRepo,
            SaleRepository saleRepo,
            SaleDetailRepository saleDetailRepo,
            CollectionRepository collectionRepo,
            OperationalExpenseRepository expenseRepo,
            OccasionalIncomeRepository incomeRepo,
            SystemConfigRepository configRepo,
            TransferRepository transferRepo) {

        return args -> {

            if (configRepo.count() == 0) {
                System.out.println("⚙️ Creando configuración de seguridad y sistema...");

                initConfig(configRepo, "IVA", "0.19");
                initConfig(configRepo, "EMPRESA", "AUTOLLANTAS S.A.S");
                initConfig(configRepo, "MONEDA", "COP");
                initConfig(configRepo, "admin_password", "1234");

                initConfig(configRepo, "recovery_pregunta", "¿Cual es el nombre de tu primera mascota?");
                initConfig(configRepo, "recovery_respuesta", "Firulais");

                initConfig(configRepo, "recovery_pregunta_1", "¿Cuál es el nombre de tu primera mascota?");
                initConfig(configRepo, "recovery_respuesta_1", "Firulais");

                initConfig(configRepo, "recovery_pregunta_2", "¿En qué ciudad naciste?");
                initConfig(configRepo, "recovery_respuesta_2", "Bogota");

                initConfig(configRepo, "recovery_pregunta_3", "¿Cuál es tu comida favorita?");
                initConfig(configRepo, "recovery_respuesta_3", "Pizza");
            }

            if (accountRepo.count() > 0) {
                System.out.println("⚠️ Ya existen cuentas. Saltando carga de datos ficticios.");
                return;
            }

            System.out.println("🚀 Cargando datos de prueba (Compras, Ventas, Gastos)... SOLO CAJA Y BANCO");

            Account ctaCaja = accountRepo.save(new Account(1000000.0, null, "Caja General", 1000000.0));
            Account ctaBanco = accountRepo.save(new Account(50000000.0, null, "Bancolombia", 50000000.0));

            Supplier prov1 = supplierRepo.save(new Supplier("3001112233", "contacto@michelin.com", null, "Michelin Colombia", "900111222-1"));
            Supplier prov2 = supplierRepo.save(new Supplier("3104445566", "ventas@terpel.com", null, "Terpel S.A.", "800333444-2"));
            Supplier prov3 = supplierRepo.save(new Supplier("3207778899", "admin@bosch.com", null, "Bosch Autopartes", "700555666-3"));

            Customer cli1 = customerRepo.save(new Customer(null, "Carlos Perez", "10102020", "carlos@gmail.com", "3001234567"));
            Customer cli2 = customerRepo.save(new Customer(null, "Maria Rodriguez", "30304040", "maria@hotmail.com", "3109876543"));
            Customer cli3 = customerRepo.save(new Customer(null, "Transportes SAS", "900500600", "logistica@transportes.com", "6017778888"));

            ProductCategory cat1 = categoryRepo.save(new ProductCategory(null, "Llantas", 20, 5));
            ProductCategory cat2 = categoryRepo.save(new ProductCategory(null, "Aceites", 15, 3));
            ProductCategory cat3 = categoryRepo.save(new ProductCategory(null, "Frenos", 10, 2));

            Product prod1 = productRepo.save(new Product(0, cat1, "LL-001", "Llanta 205/55 R16", null, 38000.0, 200000.0, 238000.0, "Producto"));
            Product prod2 = productRepo.save(new Product(0, cat2, "AC-001", "Aceite Sintetico 5W30", null, 9500.0, 50000.0, 59500.0, "Producto"));
            Product prod3 = productRepo.save(new Product(0, cat3, "FR-001", "Pastillas de Freno Cerámica", null, 15200.0, 80000.0, 95200.0, "Servicio"));

            Purchase purchase1 = purchaseRepo.save(new Purchase(null, prov1, ctaBanco, "FAC-00001", LocalDate.now().minusDays(10), "Contado", LocalDate.now().minusDays(10), "Transferencia", "Stock Inicial", 2380000.0, "PAGADA"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase1, 0.0, null, 380000.0, 200000.0, prod1, 2380000.0));
            paymentRepo.save(new Payment(purchase1, ctaBanco, LocalDate.now().minusDays(10), null, "Transferencia", 2380000.0));
            prod1.setQuantity(10);
            productRepo.save(prod1);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() - 2380000.0);
            accountRepo.save(ctaBanco);
            Movement movC1 = new Movement(LocalDate.now().minusDays(10), purchase1.getId(), "Egreso", 2380000.0, ctaBanco);
            movC1.setSourceTable("COMPRAS");
            movementRepo.save(movC1);

            Purchase purchase2 = purchaseRepo.save(new Purchase(null, prov2, ctaCaja, "FAC-00002", LocalDate.now().minusDays(8), "Contado", LocalDate.now().minusDays(8), "Efectivo", "Reposición Aceites", 595000.0, "PAGADA"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase2, 0.0, null, 95000.0, 50000.0, prod2, 595000.0));
            paymentRepo.save(new Payment(purchase2, ctaCaja, LocalDate.now().minusDays(8), null, "Efectivo", 595000.0));
            prod2.setQuantity(10);
            productRepo.save(prod2);
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 595000.0);
            accountRepo.save(ctaCaja);
            Movement movC2 = new Movement(LocalDate.now().minusDays(8), purchase2.getId(), "Egreso", 595000.0, ctaCaja);
            movC2.setSourceTable("COMPRAS");
            movementRepo.save(movC2);

            Purchase purchase3 = purchaseRepo.save(new Purchase(null, prov3, ctaBanco, "FAC-00003", LocalDate.now().minusDays(5), "Credito", LocalDate.now().plusDays(25), "Transferencia", "Repuestos Frenos", 952000.0, "PENDIENTE"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase3, 0.0, null, 152000.0, 80000.0, prod3, 952000.0));
            prod3.setQuantity(10);
            productRepo.save(prod3);

            Sale sale1 = new Sale(cli1, ctaCaja, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Efectivo", "Venta mostrador", "VEN-00001", 476000.0);
            sale1.setPendingBalance(0.0);
            sale1 = saleRepo.save(sale1);
            saleDetailRepo.save(new SaleDetail(2, 0.0, null, 76000.0, 238000.0, prod1, 476000.0, sale1));
            collectionRepo.save(new Collection(ctaCaja, LocalDate.now(), null, "Efectivo", 476000.0, sale1));
            prod1.setQuantity(prod1.getQuantity() - 2);
            productRepo.save(prod1);
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() + 476000.0);
            accountRepo.save(ctaCaja);
            Movement movV1 = new Movement(LocalDate.now(), sale1.getId(), "Ingreso", 476000.0, ctaCaja);
            movV1.setSourceTable("VENTAS");
            movementRepo.save(movV1);

            Sale sale2 = new Sale(cli2, ctaBanco, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Transferencia", "Mantenimiento", "VEN-00002", 238000.0);
            sale2.setPendingBalance(0.0);
            sale2 = saleRepo.save(sale2);
            saleDetailRepo.save(new SaleDetail(4, 0.0, null, 38000.0, 59500.0, prod2, 238000.0, sale2));
            collectionRepo.save(new Collection(ctaBanco, LocalDate.now(), null, "Transferencia", 238000.0, sale2));
            prod2.setQuantity(prod2.getQuantity() - 4);
            productRepo.save(prod2);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() + 238000.0);
            accountRepo.save(ctaBanco);
            Movement movV2 = new Movement(LocalDate.now(), sale2.getId(), "Ingreso", 238000.0, ctaBanco);
            movV2.setSourceTable("VENTAS");
            movementRepo.save(movV2);

            Sale sale3 = new Sale(cli3, ctaBanco, "PENDIENTE", LocalDate.now().plusDays(15), LocalDate.now().minusDays(5), "Credito", null, "Credito", "Flotilla Test", "VEN-00003", 1904000.0);
            sale3 = saleRepo.save(sale3);
            saleDetailRepo.save(new SaleDetail(4, 0.0, null, 304000.0, 95200.0, prod3, 380800.0, sale3));
            saleDetailRepo.save(new SaleDetail(4, 0.0, null, 152000.0, 238000.0, prod1, 952000.0, sale3));
            collectionRepo.save(new Collection(ctaCaja, LocalDate.now().minusDays(2), null, "Efectivo", 500000.0, sale3));
            sale3.setPendingBalance(1404000.0);
            saleRepo.save(sale3);
            prod3.setQuantity(prod3.getQuantity() - 4);
            prod1.setQuantity(prod1.getQuantity() - 4);
            productRepo.save(prod3);
            productRepo.save(prod1);

            OperationalExpense expense1 = expenseRepo.save(new OperationalExpense("Pago Energia", ctaCaja, LocalDate.now(), null, 150000.0, "Enel Codensa"));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 150000.0);
            accountRepo.save(ctaCaja);
            Movement movG1 = new Movement(LocalDate.now(), expense1.getId(), "Egreso", 150000.0, ctaCaja);
            movG1.setSourceTable("GASTOS_OPERATIVOS");
            movementRepo.save(movG1);

            OperationalExpense expense2 = expenseRepo.save(new OperationalExpense("Nomina Ayudante", ctaBanco, LocalDate.now(), null, 1300000.0, "Pago Quincena"));
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() - 1300000.0);
            accountRepo.save(ctaBanco);
            Movement movG2 = new Movement(LocalDate.now(), expense2.getId(), "Egreso", 1300000.0, ctaBanco);
            movG2.setSourceTable("GASTOS_OPERATIVOS");
            movementRepo.save(movG2);

            OperationalExpense expense3 = expenseRepo.save(new OperationalExpense("Cafetería", ctaCaja, LocalDate.now(), null, 50000.0, "Insumos Varios"));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 50000.0);
            accountRepo.save(ctaCaja);
            Movement movG3 = new Movement(LocalDate.now(), expense3.getId(), "Egreso", 50000.0, ctaCaja);
            movG3.setSourceTable("GASTOS_OPERATIVOS");
            movementRepo.save(movG3);

            OccasionalIncome income1 = incomeRepo.save(new OccasionalIncome("Venta Chatarra", ctaCaja, LocalDate.now(), null, 60000.0, "Reciclaje"));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() + 60000.0);
            accountRepo.save(ctaCaja);
            Movement movI1 = new Movement(LocalDate.now(), income1.getId(), "Ingreso", 60000.0, ctaCaja);
            movI1.setSourceTable("INGRESOS_OCASIONALES");
            movementRepo.save(movI1);

            OccasionalIncome income2 = incomeRepo.save(new OccasionalIncome("Reembolso Seguro", ctaBanco, LocalDate.now(), null, 200000.0, "Siniestro menor"));
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() + 200000.0);
            accountRepo.save(ctaBanco);
            Movement movI2 = new Movement(LocalDate.now(), income2.getId(), "Ingreso", 200000.0, ctaBanco);
            movI2.setSourceTable("INGRESOS_OCASIONALES");
            movementRepo.save(movI2);

            OccasionalIncome income3 = incomeRepo.save(new OccasionalIncome("Propina Cliente", ctaCaja, LocalDate.now(), null, 20000.0, "Servicio extra"));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() + 20000.0);
            accountRepo.save(ctaCaja);
            Movement movI3 = new Movement(LocalDate.now(), income3.getId(), "Ingreso", 20000.0, ctaCaja);
            movI3.setSourceTable("INGRESOS_OCASIONALES");
            movementRepo.save(movI3);

            Transfer tr1 = transferRepo.save(new Transfer(ctaCaja, ctaBanco, LocalDate.now(), null, 100000.0));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 100000.0);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() - 100000.0);
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() + 100000.0);
            accountRepo.save(ctaCaja);
            accountRepo.save(ctaBanco);
            Movement movTrOut1 = new Movement(LocalDate.now(), tr1.getId(), "Egreso", 100000.0, ctaBanco);
            movTrOut1.setSourceTable("TRANSFERENCIAS");
            movementRepo.save(movTrOut1);
            Movement movTrIn1 = new Movement(LocalDate.now(), tr1.getId(), "Ingreso", 100000.0, ctaCaja);
            movTrIn1.setSourceTable("TRANSFERENCIAS");
            movementRepo.save(movTrIn1);

            Transfer tr2 = transferRepo.save(new Transfer(ctaBanco, ctaCaja, LocalDate.now(), null, 500000.0));
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 500000.0);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() + 500000.0);
            accountRepo.save(ctaBanco);
            accountRepo.save(ctaCaja);
            Movement movTrOut2 = new Movement(LocalDate.now(), tr2.getId(), "Egreso", 500000.0, ctaCaja);
            movTrOut2.setSourceTable("TRANSFERENCIAS");
            movementRepo.save(movTrOut2);
            Movement movTrIn2 = new Movement(LocalDate.now(), tr2.getId(), "Ingreso", 500000.0, ctaBanco);
            movTrIn2.setSourceTable("TRANSFERENCIAS");
            movementRepo.save(movTrIn2);

            System.out.println("✅ ¡CARGA COMPLETADA! CONFIGURACIÓN REPARADA Y DATOS CARGADOS (SOLO CAJA Y BANCO).");
        };
    }

    private void initConfig(SystemConfigRepository repo, String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setKey(key);
        c.setValue(value);
        repo.save(c);
    }
}

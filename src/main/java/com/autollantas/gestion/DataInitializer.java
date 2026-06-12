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
import com.autollantas.gestion.inventory.service.InventoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
@Configuration
public class DataInitializer {

    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("LLANTA", "LLANTAS"),
            Map.entry("FILTRO AIRE", "FILTROS DE AIRE"),
            Map.entry("ACEITE", "ACEITES"),
            Map.entry("LUBRICANTE", "OTROS"),
            Map.entry("BATERIA", "BATERÍAS"),
            Map.entry("BETERIA", "BATERÍAS"),
            Map.entry("FILTRO ACEITE", "FILTROS DE ACEITE"),
            Map.entry("PLUMILLA", "PLUMILLAS"),
            Map.entry("BOMBILLO AUTO", "ELÉCTRICOS"),
            Map.entry("BORNES", "ELÉCTRICOS"),
            Map.entry("ANTISULFATANT", "OTROS"),
            Map.entry("ARANDELA", "OTROS"),
            Map.entry("CAJA", "OTROS")
    );

    private String normalizeCategoryName(String raw) {
        String cleaned = raw.trim().replaceAll("\\s+", " ").toUpperCase();
        return CATEGORY_MAP.getOrDefault(cleaned, "OTROS");
    }

    @Bean
    @ConditionalOnProperty(name = "app.data.init.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner loadProductData(ProductCategoryRepository categoryRepo,
                                     ProductRepository productRepo,
                                     InventoryService inventoryService) {
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

                inventoryService.ensureVatExists();

                if (categoryRepo.count() == 0) {
                    for (String catName : List.of("LLANTAS", "FILTROS DE AIRE",
                            "ACEITES", "BATERÍAS", "FILTROS DE ACEITE",
                            "PLUMILLAS", "ELÉCTRICOS", "OTROS")) {
                        inventoryService.createCategory(catName, 5, 2);
                    }
                }

                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (data.length < 8) continue;

                    String categoryNameRaw = cleanText(data[0]);
                    String categoryName = normalizeCategoryName(categoryNameRaw);
                    String code = cleanText(data[1]);
                    String description = cleanText(data[2]);

                    if (code.isEmpty() || categoryNameRaw.isEmpty()) continue;

                    ProductCategory category = categoryCache.get(categoryName);
                    if (category == null) {
                        category = categoryRepo.findByName(categoryName).orElse(null);
                        if (category == null) {
                            category = new ProductCategory();
                            category.setName(categoryName);
                            category.setYellowStockMin(5);
                            category.setRedStockMin(2);
                            category = categoryRepo.save(category);
                            inventoryService.assignVatToCategory(category);
                        }
                        categoryCache.put(categoryName, category);
                    }

                    double stockDouble = cleanAndParsePrice(data[6]);
                    String itemType = cleanText(data[7]);

                    int stock = (int) stockDouble;

                    Product p = new Product();
                    p.setCode(code);
                    p.setDescription(description);
                    p.setQuantity(stock);
                    p.setPurchaseCost(cleanAndParsePrice(data[5]));
                    p.setItemType(itemType);
                    p.setCategory(category);
                    productRepo.save(p);
                    inventoryService.recalculateMinSalePrice(p);
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
    @ConditionalOnProperty(name = "app.data.init.enabled", havingValue = "true", matchIfMissing = true)
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
            TransferRepository transferRepo,
            InventoryService inventoryService) {

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
            cli1.setDocumentType("CC"); customerRepo.save(cli1);
            Customer cli2 = customerRepo.save(new Customer(null, "Maria Rodriguez", "30304040", "maria@hotmail.com", "3109876543"));
            cli2.setDocumentType("CC"); customerRepo.save(cli2);
            Customer cli3 = customerRepo.save(new Customer(null, "Transportes SAS", "900500600", "logistica@transportes.com", "6017778888"));
            cli3.setDocumentType("NIT"); customerRepo.save(cli3);

            inventoryService.ensureVatExists();
            ProductCategory cat1 = categoryRepo.findByName("LLANTAS")
                    .orElseGet(() -> inventoryService.createCategory("LLANTAS", 20, 5));
            ProductCategory cat2 = categoryRepo.findByName("ACEITES")
                    .orElseGet(() -> inventoryService.createCategory("ACEITES", 15, 3));
            ProductCategory cat3 = categoryRepo.findByName("OTROS")
                    .orElseGet(() -> inventoryService.createCategory("OTROS", 10, 2));

            Product prod1 = new Product();
            prod1.setCode("LL-001"); prod1.setDescription("Llanta 205/55 R16");
            prod1.setQuantity(0); prod1.setPurchaseCost(238000.0);
            prod1.setItemType("Producto"); prod1.setCategory(cat1);
            prod1 = productRepo.save(prod1);
            inventoryService.recalculateMinSalePrice(prod1);

            Product prod2 = new Product();
            prod2.setCode("AC-001"); prod2.setDescription("Aceite Sintetico 5W30");
            prod2.setQuantity(0); prod2.setPurchaseCost(59500.0);
            prod2.setItemType("Producto"); prod2.setCategory(cat2);
            prod2 = productRepo.save(prod2);
            inventoryService.recalculateMinSalePrice(prod2);

            Product prod3 = new Product();
            prod3.setCode("FR-001"); prod3.setDescription("Pastillas de Freno Cerámica");
            prod3.setQuantity(0); prod3.setPurchaseCost(95200.0);
            prod3.setItemType("Servicio"); prod3.setCategory(cat3);
            prod3 = productRepo.save(prod3);
            inventoryService.recalculateMinSalePrice(prod3);

            Purchase purchase1 = purchaseRepo.save(new Purchase(null, prov1, ctaBanco, "FAC-00001", LocalDate.now().minusDays(10), "Contado", LocalDate.now().minusDays(10), "Transferencia", "Stock Inicial", 2380000.0, "PAGADA"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase1, 0.0, null, prod1.getTaxAmount(), 200000.0, prod1, 2380000.0));
            paymentRepo.save(new Payment(purchase1, ctaBanco, LocalDate.now().minusDays(10), null, "Transferencia", 2380000.0));
            prod1.setQuantity(10);
            productRepo.save(prod1);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() - 2380000.0);
            accountRepo.save(ctaBanco);
            Movement movC1 = new Movement(LocalDate.now().minusDays(10), purchase1.getId(), "Egreso", 2380000.0, ctaBanco);
            movC1.setSourceTable("COMPRAS");
            movementRepo.save(movC1);
            purchase1.setPendingBalance(0.0);
            purchaseRepo.save(purchase1);

            Purchase purchase2 = purchaseRepo.save(new Purchase(null, prov2, ctaCaja, "FAC-00002", LocalDate.now().minusDays(8), "Contado", LocalDate.now().minusDays(8), "Efectivo", "Reposición Aceites", 595000.0, "PAGADA"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase2, 0.0, null, prod2.getTaxAmount(), 50000.0, prod2, 595000.0));
            paymentRepo.save(new Payment(purchase2, ctaCaja, LocalDate.now().minusDays(8), null, "Efectivo", 595000.0));
            prod2.setQuantity(10);
            productRepo.save(prod2);
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() - 595000.0);
            accountRepo.save(ctaCaja);
            Movement movC2 = new Movement(LocalDate.now().minusDays(8), purchase2.getId(), "Egreso", 595000.0, ctaCaja);
            movC2.setSourceTable("COMPRAS");
            movementRepo.save(movC2);
            purchase2.setPendingBalance(0.0);
            purchaseRepo.save(purchase2);

            Purchase purchase3 = purchaseRepo.save(new Purchase(null, prov3, ctaBanco, "FAC-00003", LocalDate.now().minusDays(5), "Credito", LocalDate.now().plusDays(25), "Transferencia", "Repuestos Frenos", 952000.0, "PENDIENTE"));
            purchaseDetailRepo.save(new PurchaseDetail(10, purchase3, 0.0, null, prod3.getTaxAmount(), 80000.0, prod3, 952000.0));
            prod3.setQuantity(10);
            productRepo.save(prod3);
            purchase3.setPendingBalance(952000.0);
            purchaseRepo.save(purchase3);

            Sale sale1 = new Sale(cli1, ctaCaja, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Efectivo", "Venta mostrador", "VEN-00001", 560000.0);
            sale1.setPendingBalance(0.0);
            sale1 = saleRepo.save(sale1);
            SaleDetail sd1 = new SaleDetail(2, 0.0, null, 76000.0, 280000.0, prod1, 560000.0, sale1);
            sd1.setProfitAmount((280000.0 - prod1.getMinSalePrice()) * 2);
            sd1.setIvaDifference((280000.0 * getIvaRate(prod1.getCategory()) * 2) - (prod1.getTaxAmount() * 2));
            saleDetailRepo.save(sd1);
            collectionRepo.save(new Collection(ctaCaja, LocalDate.now(), null, "Efectivo", 560000.0, sale1));
            prod1.setQuantity(prod1.getQuantity() - 2);
            productRepo.save(prod1);
            ctaCaja.setCurrentBalance(ctaCaja.getCurrentBalance() + 560000.0);
            accountRepo.save(ctaCaja);
            Movement movV1 = new Movement(LocalDate.now(), sale1.getId(), "Ingreso", 560000.0, ctaCaja);
            movV1.setSourceTable("VENTAS");
            movementRepo.save(movV1);

            Sale sale2 = new Sale(cli2, ctaBanco, "PAGADA", LocalDate.now(), LocalDate.now(), "Contado", null, "Transferencia", "Mantenimiento", "VEN-00002", 288000.0);
            sale2.setPendingBalance(0.0);
            sale2 = saleRepo.save(sale2);
            SaleDetail sd2 = new SaleDetail(4, 0.0, null, 38000.0, 72000.0, prod2, 288000.0, sale2);
            sd2.setProfitAmount((72000.0 - prod2.getMinSalePrice()) * 4);
            sd2.setIvaDifference((72000.0 * getIvaRate(prod2.getCategory()) * 4) - (prod2.getTaxAmount() * 4));
            saleDetailRepo.save(sd2);
            collectionRepo.save(new Collection(ctaBanco, LocalDate.now(), null, "Transferencia", 288000.0, sale2));
            prod2.setQuantity(prod2.getQuantity() - 4);
            productRepo.save(prod2);
            ctaBanco.setCurrentBalance(ctaBanco.getCurrentBalance() + 288000.0);
            accountRepo.save(ctaBanco);
            Movement movV2 = new Movement(LocalDate.now(), sale2.getId(), "Ingreso", 288000.0, ctaBanco);
            movV2.setSourceTable("VENTAS");
            movementRepo.save(movV2);

            Sale sale3 = new Sale(cli3, ctaBanco, "PENDIENTE", LocalDate.now().plusDays(15), LocalDate.now().minusDays(5), "Credito", null, "Credito", "Flotilla Test", "VEN-00003", 1580000.0);
            sale3 = saleRepo.save(sale3);
            SaleDetail sd3a = new SaleDetail(4, 0.0, null, 304000.0, 115000.0, prod3, 460000.0, sale3);
            sd3a.setProfitAmount((115000.0 - prod3.getMinSalePrice()) * 4);
            sd3a.setIvaDifference((115000.0 * getIvaRate(prod3.getCategory()) * 4) - (prod3.getTaxAmount() * 4));
            saleDetailRepo.save(sd3a);
            SaleDetail sd3b = new SaleDetail(4, 0.0, null, 152000.0, 280000.0, prod1, 1120000.0, sale3);
            sd3b.setProfitAmount((280000.0 - prod1.getMinSalePrice()) * 4);
            sd3b.setIvaDifference((280000.0 * getIvaRate(prod1.getCategory()) * 4) - (prod1.getTaxAmount() * 4));
            saleDetailRepo.save(sd3b);
            collectionRepo.save(new Collection(ctaCaja, LocalDate.now().minusDays(2), null, "Efectivo", 500000.0, sale3));
            sale3.setPendingBalance(1080000.0);
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

    private double getIvaRate(ProductCategory cat) {
        if (cat == null || cat.getTaxTypes() == null) return 0.0;
        return cat.getTaxTypes().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsVat()))
                .mapToDouble(t -> t.getRate() != null ? t.getRate() : 0.0)
                .findFirst().orElse(0.0);
    }

    private void initConfig(SystemConfigRepository repo, String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setKey(key);
        c.setValue(value);
        repo.save(c);
    }
}

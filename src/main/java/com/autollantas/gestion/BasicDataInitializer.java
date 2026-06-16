package com.autollantas.gestion;

import com.autollantas.gestion.config.model.SystemConfig;
import com.autollantas.gestion.config.repository.SystemConfigRepository;
import com.autollantas.gestion.inventory.model.ProductCategory;
import com.autollantas.gestion.inventory.repository.ProductCategoryRepository;
import com.autollantas.gestion.inventory.model.TaxType;
import com.autollantas.gestion.inventory.repository.TaxTypeRepository;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.AccountType;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class BasicDataInitializer implements CommandLineRunner {

    private final SystemConfigRepository systemConfigRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final AccountRepository accountRepository;
    private final TaxTypeRepository taxTypeRepository;

    @Override
    public void run(String... args) {
        seedCredentials();
        seedCategories();
        seedAccounts();
        seedTaxTypes();
    }

    private void seedCredentials() {
        seedConfig("admin_password", "1234");
        seedConfig("recovery_pregunta_1", "¿Cual es el nombre de la mascota de la familia? (Respuesta: una palabra, primera letra en mayúscula)");
        seedConfig("recovery_respuesta_1", "Paloma");
        seedConfig("recovery_pregunta_2", "¿Cual es el hij@ mas joven de la familia? (Respuesta: una palabra, primera letra en mayúscula)");
        seedConfig("recovery_respuesta_2", "Sofia");
        seedConfig("recovery_pregunta_3", "¿Cual es el nombre del pueblo natal de la familia? (Respuesta: una palabra, primera letra en mayúscula)");
        seedConfig("recovery_respuesta_3", "Vianí");
    }

    private void seedConfig(String key, String value) {
        if (systemConfigRepository.findByKey(key).isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setKey(key);
            config.setValue(value);
            systemConfigRepository.save(config);
        }
    }

    private void seedCategories() {
        if (productCategoryRepository.count() != 0) return;

        String[][] cats = {
            {"LLANTAS",           "#e74c3c"},
            {"FILTROS DE AIRE",   "#e67e22"},
            {"ACEITES",           "#f1c40f"},
            {"BATERÍAS",          "#2ecc71"},
            {"FILTROS DE ACEITE", "#1abc9c"},
            {"PLUMILLAS",         "#3498db"},
            {"ELÉCTRICOS",        "#9b59b6"},
            {"OTROS",             "#95a5a6"},
        };

        for (String[] cat : cats) {
            ProductCategory category = new ProductCategory();
            category.setName(cat[0]);
            category.setColor(cat[1]);
            category.setYellowStockMin(null);
            category.setRedStockMin(null);
            category.setTargetMargin(null);
            category.setTaxTypes(new ArrayList<>());
            productCategoryRepository.save(category);
        }
    }

    private void seedAccounts() {
        if (accountRepository.count() != 0) return;

        Account bancolombia = new Account();
        bancolombia.setName("Bancolombia");
        bancolombia.setInitialBalance(0.0);
        bancolombia.setCurrentBalance(0.0);
        bancolombia.setType(AccountType.BANK);
        accountRepository.save(bancolombia);

        Account caja = new Account();
        caja.setName("Caja General");
        caja.setInitialBalance(0.0);
        caja.setCurrentBalance(0.0);
        caja.setType(AccountType.CASH);
        accountRepository.save(caja);
    }

    private void seedTaxTypes() {
        if (taxTypeRepository.count() != 0) return;

        TaxType iva = new TaxType();
        iva.setName("IVA");
        iva.setRate(0.19);
        iva.setDescription("Impuesto al Valor Agregado 19%");
        iva.setIsVat(true);
        iva.setAppliesToTransaction(true);
        taxTypeRepository.save(iva);
    }
}

package com.autollantas.gestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Smoke Test — FXML válidos")
class FxmlSmokeTest {

    private static final String BASE =
        "/com/autollantas/gestion/";

    @ParameterizedTest(name = "{0}")
    @DisplayName("FXML existe y es XML bien formado")
    @ValueSource(strings = {
        "auth/views/Login.fxml",
        "auth/views/PasswordRecovery.fxml",
        "inventory/views/CategoryForm.fxml",
        "inventory/views/CategoryManagement.fxml",
        "inventory/views/CategoryMargins.fxml",
        "inventory/views/ProductForm.fxml",
        "inventory/views/Products.fxml",
        "inventory/views/StockAlerts.fxml",
        "inventory/views/TaxForm.fxml",
        "inventory/views/TaxManagement.fxml",
        "purchases/views/PaymentForm.fxml",
        "purchases/views/PaymentHistory.fxml",
        "purchases/views/Payments.fxml",
        "purchases/views/PurchaseDetails.fxml",
        "purchases/views/PurchaseForm.fxml",
        "purchases/views/PurchaseInvoices.fxml",
        "reporting/views/Dashboard.fxml",
        "reporting/views/ReportGeneration.fxml",
        "sales/views/CollectionForm.fxml",
        "sales/views/CollectionHistory.fxml",
        "sales/views/Collections.fxml",
        "sales/views/SaleDetails.fxml",
        "sales/views/SaleForm.fxml",
        "sales/views/SaleInvoices.fxml",
        "shared/views/MainLayout.fxml",
        "treasury/views/Accounts.fxml",
        "treasury/views/OccasionalIncome.fxml",
        "treasury/views/OccasionalIncomeForm.fxml",
        "treasury/views/OperationalExpenseForm.fxml",
        "treasury/views/OperationalExpenses.fxml",
        "treasury/views/TransferForm.fxml"
    })
    void fxmlExisteYEsXmlValido(String ruta) throws Exception {
        String fullPath = BASE + ruta;

        // 1. El recurso existe en el classpath
        InputStream is = getClass()
            .getResourceAsStream(fullPath);
        assertNotNull(is,
            "Recurso no encontrado: " + fullPath);

        // 2. Es XML bien formado — sin errores de parsing
        try (InputStream stream = is) {
            assertDoesNotThrow(() ->
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(stream),
                "XML malformado en: " + fullPath
            );
        }
    }
}

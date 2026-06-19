package com.autollantas.gestion.shared.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.autollantas.gestion.inventory.controller.ProductsController;
import com.autollantas.gestion.shared.util.CustomDialog;
import com.autollantas.gestion.purchases.controller.PurchaseInvoicesController;
import com.autollantas.gestion.sales.controller.SaleInvoicesController;
import com.autollantas.gestion.treasury.controller.OccasionalIncomeController;
import com.autollantas.gestion.treasury.controller.OperationalExpensesController;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

@SuppressWarnings("ALL")
@Component
public class MainLayoutController {

    @Getter
    private static MainLayoutController instance;

    @Autowired
    private ApplicationContext springContext;

    @FXML private StackPane contentArea;
    @FXML private VBox sidebarContainer;
    @FXML private HBox topBar;
    @FXML private Button btnMenu;
    @FXML private ImageView imgLogoSidebar;

    @FXML private TitledPane tpPanelControl;
    @FXML private TitledPane tpIngresos;
    @FXML private TitledPane tpEgresos;
    @FXML private TitledPane tpInventario;
    @FXML private TitledPane tpCuentas;


    @FXML private Button btnVentas;
    @FXML private Button btnRecaudos;
    @FXML private Button btnIngresoOcasional;
    @FXML private Button btnCompras;
    @FXML private Button btnPagos;
    @FXML private Button btnCostosOperativos;
    @FXML private Button btnProductos;
    @FXML private Button btnAlertas;
    @FXML private Button btnImpuestos;

    private TitledPane activePane = null;
    private Button activeSubBtn = null;

    /** Controlador de la vista actualmente cargada en el área de contenido. */
    @Getter
    private Object activeViewController = null;

    private Parent cachedSaleFormNode = null;
    private Parent cachedPurchaseFormNode = null;

    private static final String SALE_FORM_FXML = "/com/autollantas/gestion/sales/views/SaleForm.fxml";
    private static final String PURCHASE_FORM_FXML = "/com/autollantas/gestion/purchases/views/PurchaseForm.fxml";
    private static final String SALE_INVOICES_FXML = "/com/autollantas/gestion/sales/views/SaleInvoices.fxml";
    private static final String PURCHASE_INVOICES_FXML = "/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml";

    private static final double RESPONSIVE_THRESHOLD = 1220.0;

    @FXML
    public void initialize() {
        instance = this;
        loadLogo();

        if (contentArea != null) {
            loadView("/com/autollantas/gestion/reporting/views/Dashboard.fxml");
        }
        startResponsiveListener();

        Platform.runLater(() -> {
            tpPanelControl.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
            tpIngresos.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
            tpEgresos.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
            tpInventario.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
            tpCuentas.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
            setActive(tpPanelControl, null);
            contentArea.setFocusTraversable(true);
            contentArea.requestFocus();
        });
    }

    private void startResponsiveListener() {
        Platform.runLater(() -> {
            if (contentArea.getScene() != null) {
                Scene scene = contentArea.getScene();
                scene.widthProperty().addListener((obs, oldVal, newVal) -> adjustLayout(newVal.doubleValue()));
                adjustLayout(scene.getWidth());
                com.autollantas.gestion.shared.util.KeyboardShortcuts.install(scene);
            }
        });
    }

    private void adjustLayout(double currentWidth) {
        if (sidebarContainer == null || topBar == null) return;
        boolean isSmallScreen = currentWidth < RESPONSIVE_THRESHOLD;
        if (isSmallScreen) {
            if (!topBar.isVisible()) {
                sidebarContainer.setVisible(false); sidebarContainer.setManaged(false);
                topBar.setVisible(true); topBar.setManaged(true);
            }
        } else {
            sidebarContainer.setVisible(true); sidebarContainer.setManaged(true);
            topBar.setVisible(false); topBar.setManaged(false);
        }
    }

    @FXML void toggleSidebar(ActionEvent event) {
        boolean isVisible = sidebarContainer.isVisible();
        sidebarContainer.setVisible(!isVisible);
        sidebarContainer.setManaged(!isVisible);
    }

    /**
     * Carga una vista Y actualiza el estado activo del sidebar en un solo paso.
     * Usar desde controladores externos en lugar de llamar loadView + setActive por separado.
     */
    public Object navigateTo(String fxmlPath, TitledPane pane, Button subBtn) {
        Object controller = loadView(fxmlPath);
        setActive(pane, subBtn);
        closeSidebarIfMobile();
        return controller;
    }

    public StackPane getContentArea()    { return contentArea; }

    public TitledPane getTpIngresos()    { return tpIngresos; }
    public TitledPane getTpEgresos()     { return tpEgresos; }
    public TitledPane getTpInventario()  { return tpInventario; }
    public TitledPane getTpPanelControl(){ return tpPanelControl; }
    public TitledPane getTpCuentas()     { return tpCuentas; }
    public Button getBtnVentas()              { return btnVentas; }
    public Button getBtnIngresoOcasional()    { return btnIngresoOcasional; }
    public Button getBtnCompras()             { return btnCompras; }
    public Button getBtnCostosOperativos()    { return btnCostosOperativos; }
    public Button getBtnAlertas()             { return btnAlertas; }

    public Object loadView(String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("❌ ERROR: No se encontró el FXML: " + fxmlPath);
                return null;
            }

            if (!contentArea.getChildren().isEmpty()) {
                Parent currentNode = (Parent) contentArea.getChildren().get(0);
                if (currentNode.getUserData() != null) {
                    String currentFxml = (String) currentNode.getUserData();
                    if (SALE_FORM_FXML.equals(currentFxml)) {
                        cachedSaleFormNode = currentNode;
                    } else if (PURCHASE_FORM_FXML.equals(currentFxml)) {
                        cachedPurchaseFormNode = currentNode;
                    }
                }
            }

            if (SALE_FORM_FXML.equals(fxmlPath) && cachedSaleFormNode != null) {
                contentArea.getChildren().setAll(cachedSaleFormNode);
                activeViewController = cachedSaleFormNode.getProperties().get("controller");
                return null;
            }
            if (PURCHASE_FORM_FXML.equals(fxmlPath) && cachedPurchaseFormNode != null) {
                contentArea.getChildren().setAll(cachedPurchaseFormNode);
                activeViewController = cachedPurchaseFormNode.getProperties().get("controller");
                return null;
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(springContext::getBean);

            Parent vista = loader.load();
            vista.setUserData(fxmlPath);
            vista.getProperties().put("controller", loader.getController());
            contentArea.getChildren().setAll(vista);

            activeViewController = loader.getController();
            return activeViewController;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clearSaleFormCache() { cachedSaleFormNode = null; }
    public void clearPurchaseFormCache() { cachedPurchaseFormNode = null; }
    public boolean hasCachedSaleForm() { return cachedSaleFormNode != null; }
    public boolean hasCachedPurchaseForm() { return cachedPurchaseFormNode != null; }

    private void loadLogo() {
        try {
            Image logoHD = new Image(getClass().getResourceAsStream("/com/autollantas/gestion/images/logo_negro.png"), 250, 250, true, true);
            if (imgLogoSidebar != null) imgLogoSidebar.setImage(logoHD);
        } catch (Exception e) { System.err.println("⚠️ Error logo: " + e.getMessage()); }
    }

    private javafx.scene.paint.Color colorFor(TitledPane pane) {
        return javafx.scene.paint.Color.web("#2e1a00");
    }

    private javafx.scene.paint.Color activeColorFor(TitledPane pane) {
        return javafx.scene.paint.Color.web("#2e1a00");
    }

    private void setActive(TitledPane pane, Button subBtn) {
        if (activePane != null) {
            activePane.getStyleClass().remove("sidebar-title-active");
            activePane.setTextFill(colorFor(activePane));
        }
        if (activeSubBtn != null) {
            activeSubBtn.getStyleClass().remove("submenu-btn-active");
            activeSubBtn.setStyle("");
        }

        activePane = pane;
        activeSubBtn = subBtn;

        if (activePane != null) {
            activePane.getStyleClass().add("sidebar-title-active");
            activePane.setTextFill(activeColorFor(activePane));
            if (!activePane.isExpanded()) activePane.setExpanded(true);
        }

        if (activeSubBtn != null && !activeSubBtn.getStyleClass().contains("submenu-btn-active"))
            activeSubBtn.getStyleClass().add("submenu-btn-active");
    }

    private void closeSidebarIfMobile() {
        if (topBar.isVisible() && sidebarContainer.isVisible()) {
            sidebarContainer.setVisible(false); sidebarContainer.setManaged(false);
        }
    }

    @FXML void btnPanelControlClick(MouseEvent event) {
        loadView("/com/autollantas/gestion/reporting/views/Dashboard.fxml");
        setActive(tpPanelControl, null);
    }

    // ------------------------------------------------------------------------
    // Navegación accesible por atajos de teclado (sin necesidad de un evento).
    // Reutilizan exactamente la misma lógica que los clics del sidebar.
    // ------------------------------------------------------------------------
    public void fireDashboard() {
        loadView("/com/autollantas/gestion/reporting/views/Dashboard.fxml");
        setActive(tpPanelControl, null);
    }
    public void fireVentas()            { btnVentasClick(null); }
    public void fireRecaudos()          { btnRecaudosClick(null); }
    public void fireIngresoOcasional()  { btnIngresoOcasionalClick(null); }
    public void fireCompras()           { btnComprasClick(null); }
    public void firePagos()             { btnPagosClick(null); }
    public void fireCostosOperativos()  { btnCostosOperativosClick(null); }
    public void fireProductos()         { btnProductosClick(null); }
    public void fireAlertas()           { btnAlertasClick(null); }
    public void fireImpuestos()         { btnImpuestosClick(null); }
    public void fireCuentas() {
        loadView("/com/autollantas/gestion/treasury/views/Accounts.fxml");
        setActive(tpCuentas, null);
    }

    public void fireCerrarSesion() {
        CustomDialog.confirm(
            sidebarContainer,
            "Cerrar sesión",
            "¿Seguro que deseas salir? Asegúrate de haber guardado cualquier cambio pendiente antes de continuar.",
            () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/auth/views/Login.fxml"));
                    loader.setControllerFactory(springContext::getBean);
                    Parent root = loader.load();
                    Stage stage = (Stage) sidebarContainer.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            },
            null
        );
    }

    @FXML void btnVentasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/sales/views/SaleInvoices.fxml");
        setActive(tpIngresos, btnVentas);
        closeSidebarIfMobile();
    }
    @FXML void btnRecaudosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/sales/views/Collections.fxml");
        setActive(tpIngresos, btnRecaudos);
        closeSidebarIfMobile();
    }
    @FXML void btnIngresoOcasionalClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml");
        setActive(tpIngresos, btnIngresoOcasional);
        closeSidebarIfMobile();
    }
    @FXML void btnComprasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml");
        setActive(tpEgresos, btnCompras);
        closeSidebarIfMobile();
    }
    @FXML void btnPagosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/purchases/views/Payments.fxml");
        setActive(tpEgresos, btnPagos);
        closeSidebarIfMobile();
    }
    @FXML void btnCostosOperativosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml");
        setActive(tpEgresos, btnCostosOperativos);
        closeSidebarIfMobile();
    }
    @FXML void btnProductosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/inventory/views/Products.fxml");
        setActive(tpInventario, btnProductos);
        closeSidebarIfMobile();
    }
    @FXML void btnAlertasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/inventory/views/StockAlerts.fxml");
        setActive(tpInventario, btnAlertas);
        closeSidebarIfMobile();
    }
    @FXML void btnImpuestosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/inventory/views/TaxManagement.fxml");
        setActive(tpInventario, btnImpuestos);
        closeSidebarIfMobile();
    }
    @FXML void btnCuentasClick(MouseEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/Accounts.fxml");
        setActive(tpCuentas, null);
    }

    @FXML void btnVentasPlusClick(ActionEvent event) {
        Object controller = loadView("/com/autollantas/gestion/sales/views/SaleInvoices.fxml");
        if (controller instanceof SaleInvoicesController c) {
           c.openForm();
        }
        closeSidebarIfMobile();
    }

    @FXML void btnIngresoOcasionalPlusClick(ActionEvent event) {
        Object controller = loadView("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml");
        if (controller instanceof OccasionalIncomeController c) {
            c.openForm(new OccasionalIncome());
        }
        closeSidebarIfMobile();
    }

    @FXML void btnComprasPlusClick(ActionEvent event) {
        Object controller = loadView("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml");
        if (controller instanceof PurchaseInvoicesController c) {
            c.openForm();
        }
        closeSidebarIfMobile();
    }

    @FXML void btnCostosOperativosPlusClick(ActionEvent event) {
        Object controller = loadView("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml");
        if (controller instanceof OperationalExpensesController c) {
            c.openForm(new OperationalExpense());
        }
        closeSidebarIfMobile();
    }

    @FXML void btnProductosPlusClick(ActionEvent event) {
        Object controller = loadView("/com/autollantas/gestion/inventory/views/Products.fxml");
        if (controller instanceof ProductsController c) {
            c.abrirModalProduct(null, "Nuevo Producto");
        }
        closeSidebarIfMobile();
    }

    @FXML
    private void btnCerrarSesionClick(ActionEvent event) {
        com.autollantas.gestion.shared.util.CustomDialog.confirm(
            sidebarContainer,
            "Cerrar sesión",
            "¿Seguro que deseas salir? Asegúrate de haber guardado cualquier cambio pendiente antes de continuar.",
            () -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/auth/views/Login.fxml"));
                    loader.setControllerFactory(springContext::getBean);
                    Parent root = loader.load();
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.centerOnScreen();
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            },
            null
        );
    }
}

package com.autollantas.gestion.shared.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.autollantas.gestion.inventory.controller.ProductsController;
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

    private TitledPane activePane = null;
    private Button activeSubBtn = null;

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
            setActive(tpPanelControl, null);
            applySemanticColors();
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

    public Object loadView(String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("❌ ERROR: No se encontró el FXML: " + fxmlPath);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(springContext::getBean);

            Parent vista = loader.load();
            contentArea.getChildren().setAll(vista);

            return loader.getController();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadLogo() {
        try {
            Image logoHD = new Image(getClass().getResourceAsStream("/com/autollantas/gestion/images/logo_negro.png"), 250, 250, true, true);
            if (imgLogoSidebar != null) imgLogoSidebar.setImage(logoHD);
        } catch (Exception e) { System.err.println("⚠️ Error logo: " + e.getMessage()); }
    }

    private void applySemanticColors() {
        tpIngresos.setTextFill(javafx.scene.paint.Color.web("#22B14C"));
        tpEgresos.setTextFill(javafx.scene.paint.Color.web("#ED1C24"));
    }

    private static final String ACTIVE_TITLE_STYLE =
        "-fx-background-color: rgba(255,255,255,0.82);" +
        "-fx-border-color: transparent transparent transparent #0d81ec;" +
        "-fx-border-width: 0 0 0 4px;" +
        "-fx-text-fill: #7a3e00;";

    private static final String PARENT_TITLE_STYLE =
        "-fx-background-color: rgba(255,255,255,0.22);" +
        "-fx-border-color: transparent transparent transparent #0d81ec;" +
        "-fx-border-width: 0 0 0 4px;";

    private void restoreSemanticColor(TitledPane pane) {
        if (pane == null) return;
        if (pane == tpIngresos) pane.setTextFill(javafx.scene.paint.Color.web("#22B14C"));
        else if (pane == tpEgresos) pane.setTextFill(javafx.scene.paint.Color.web("#ED1C24"));
        else pane.setTextFill(javafx.scene.paint.Color.web("#2e1a00"));
    }

    private void setTitleInlineStyle(TitledPane pane, String style) {
        if (pane == null) return;
        javafx.scene.Node titleNode = pane.lookup(".title");
        if (titleNode != null) titleNode.setStyle(style);
    }

    private void setActive(TitledPane pane, Button subBtn) {
        setTitleInlineStyle(activePane, "");
        restoreSemanticColor(activePane);
        if (activeSubBtn != null) activeSubBtn.getStyleClass().remove("submenu-btn-active");

        activePane = pane;
        activeSubBtn = subBtn;

        if (activePane != null)
            setTitleInlineStyle(activePane, subBtn == null ? ACTIVE_TITLE_STYLE : PARENT_TITLE_STYLE);

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
    }
}

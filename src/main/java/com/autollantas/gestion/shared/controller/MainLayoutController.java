package com.autollantas.gestion.shared.controller;

import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.sales.controller.SaleInvoicesController;
import com.autollantas.gestion.treasury.controller.OccasionalIncomeController;
import com.autollantas.gestion.purchases.controller.PurchaseInvoicesController;
import com.autollantas.gestion.treasury.controller.OperationalExpensesController;
import com.autollantas.gestion.inventory.controller.ProductsController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
            Image logoHD = new Image(getClass().getResourceAsStream("/com/autollantas/gestion/images/Logo Negro.png"), 250, 250, true, true);
            if (imgLogoSidebar != null) imgLogoSidebar.setImage(logoHD);
        } catch (Exception e) { System.err.println("⚠️ Error logo: " + e.getMessage()); }
    }

    private void closeSidebarIfMobile() {
        if (topBar.isVisible() && sidebarContainer.isVisible()) {
            sidebarContainer.setVisible(false); sidebarContainer.setManaged(false);
        }
    }

    @FXML void btnPanelControlClick(MouseEvent event) { loadView("/com/autollantas/gestion/reporting/views/Dashboard.fxml"); }

    @FXML void btnVentasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/sales/views/SaleInvoices.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnRecaudosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/sales/views/Collections.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnIngresoOcasionalClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/OccasionalIncome.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnComprasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/purchases/views/PurchaseInvoices.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnPagosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/purchases/views/Payments.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnCostosOperativosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/OperationalExpenses.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnProductosClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/inventory/views/Products.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnAlertasClick(ActionEvent event) {
        loadView("/com/autollantas/gestion/inventory/views/StockAlerts.fxml");
        closeSidebarIfMobile();
    }
    @FXML void btnCuentasClick(MouseEvent event) {
        loadView("/com/autollantas/gestion/treasury/views/Accounts.fxml");
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

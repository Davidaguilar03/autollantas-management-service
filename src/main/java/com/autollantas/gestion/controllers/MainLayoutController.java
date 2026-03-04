package com.autollantas.gestion.controllers;

import com.autollantas.gestion.model.GastoOperativo;
import com.autollantas.gestion.model.IngresoOcasional;
import com.autollantas.gestion.model.Producto;
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

    private static final double UMBRAL_RESPONSIVE = 1220.0;

    @FXML
    public void initialize() {
        instance = this;
        cargarLogo();

        if (contentArea != null) {
            cargarVista("/com/autollantas/gestion/views/PanelControl.fxml");
        }
        iniciarListenerResponsive();

        Platform.runLater(() -> {
            contentArea.setFocusTraversable(true);
            contentArea.requestFocus();
        });
    }

    private void iniciarListenerResponsive() {
        Platform.runLater(() -> {
            if (contentArea.getScene() != null) {
                Scene scene = contentArea.getScene();
                scene.widthProperty().addListener((obs, oldVal, newVal) -> ajustarInterfaz(newVal.doubleValue()));
                ajustarInterfaz(scene.getWidth());
            }
        });
    }

    private void ajustarInterfaz(double anchoActual) {
        if (sidebarContainer == null || topBar == null) return;
        boolean esPantallaPequena = anchoActual < UMBRAL_RESPONSIVE;
        if (esPantallaPequena) {
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
        boolean estaVisible = sidebarContainer.isVisible();
        sidebarContainer.setVisible(!estaVisible);
        sidebarContainer.setManaged(!estaVisible);
    }


    public Object cargarVista(String fxmlPath) {
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

    private void cargarLogo() {
        try {
            Image logoHD = new Image(getClass().getResourceAsStream("/com/autollantas/gestion/images/Logo Negro.png"), 250, 250, true, true);
            if (imgLogoSidebar != null) imgLogoSidebar.setImage(logoHD);
        } catch (Exception e) { System.err.println("⚠️ Error logo: " + e.getMessage()); }
    }

    private void cerrarSidebarSiEsMovil() {
        if (topBar.isVisible() && sidebarContainer.isVisible()) {
            sidebarContainer.setVisible(false); sidebarContainer.setManaged(false);
        }
    }

    @FXML void btnPanelControlClick(MouseEvent event) { cargarVista("/com/autollantas/gestion/views/PanelControl.fxml"); }

    @FXML void btnVentasClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/FacturasVenta.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnRecaudosClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/Recaudos.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnIngresoOcasionalClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/IngresoOcasional.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnComprasClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/FacturasCompra.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnPagosClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/Pagos.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnCostosOperativosClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/GastosOperativos.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnProductosClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/Productos.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnAlertasClick(ActionEvent event) {
        cargarVista("/com/autollantas/gestion/views/AlertasStock.fxml");
        cerrarSidebarSiEsMovil();
    }
    @FXML void btnCuentasClick(MouseEvent event) {
        cargarVista("/com/autollantas/gestion/views/Cuentas.fxml");
    }

    @FXML void btnVentasPlusClick(ActionEvent event) {
        Object controller = cargarVista("/com/autollantas/gestion/views/FacturasVenta.fxml");
        if (controller instanceof FacturasVentaController c) {
           c.abrirModal();
        }
        cerrarSidebarSiEsMovil();
    }

    @FXML void btnIngresoOcasionalPlusClick(ActionEvent event) {
        Object controller = cargarVista("/com/autollantas/gestion/views/IngresoOcasional.fxml");
        if (controller instanceof IngresoOcasionalController c) {
            c.abrirFormulario(new IngresoOcasional());
        }
        cerrarSidebarSiEsMovil();
    }

    @FXML void btnComprasPlusClick(ActionEvent event) {
        Object controller = cargarVista("/com/autollantas/gestion/views/FacturasCompra.fxml");
        if (controller instanceof FacturasCompraController c) {
    c.abrirModal();
        }
        cerrarSidebarSiEsMovil();
    }

    @FXML void btnCostosOperativosPlusClick(ActionEvent event) {
        Object controller = cargarVista("/com/autollantas/gestion/views/GastosOperativos.fxml");
        if (controller instanceof GastosOperativosController c) {
            c.abrirFormulario(new GastoOperativo());
        }
        cerrarSidebarSiEsMovil();
    }

    @FXML void btnProductosPlusClick(ActionEvent event) {
        Object controller = cargarVista("/com/autollantas/gestion/views/Productos.fxml");
        if (controller instanceof ProductosController c) {
           c.abrirModalProducto(null,
                   "Nuevo Producto");
        }
        cerrarSidebarSiEsMovil();
    }

    @FXML
    private void btnCerrarSesionClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/autollantas/gestion/views/Login.fxml"));
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
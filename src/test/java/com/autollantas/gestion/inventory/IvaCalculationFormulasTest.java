package com.autollantas.gestion.inventory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class IvaCalculationFormulasTest {

    @Test
    void escenarioCompleto_ivaDescontable_formulasCorrectas() {
        double purchaseCost = 100000.0;
        double tasaIva = 0.19;
        double otrosTasa = 0.0;
        double precioVenta = 130000.0;
        int cantidad = 1;

        // PurchaseDetail.tax (IVA descontable por unidad) = purchaseCost * tasaIva
        double taxAmount = purchaseCost * tasaIva;
        assertThat(taxAmount).isCloseTo(19000.0, within(0.01));

        // minSalePrice: el IVA NO se suma (se compensa con taxAmount)
        double minSalePrice = purchaseCost + (purchaseCost * otrosTasa);
        assertThat(minSalePrice).isCloseTo(100000.0, within(0.01));

        // profitAmount = (salePrice - minSalePrice) * qty
        double profitAmount = (precioVenta - minSalePrice) * cantidad;
        assertThat(profitAmount).isCloseTo(30000.0, within(0.01));

        // ivaGenerado = salePrice * tasaIva * qty
        double ivaGenerado = precioVenta * tasaIva * cantidad;
        assertThat(ivaGenerado).isCloseTo(24700.0, within(0.01));

        // ivaDifference (IVA por pagar) = ivaGenerado - taxAmount * qty
        double ivaDifference = ivaGenerado - (taxAmount * cantidad);
        assertThat(ivaDifference).isCloseTo(5700.0, within(0.01));
    }

    @Test
    void escenarioConOtrosImpuestos_minimoIncluye3pct() {
        double purchaseCost = 100000.0;
        double tasaIva = 0.19;
        double otrosTasa = 0.03;
        double precioVenta = 140000.0;
        int cantidad = 2;

        double taxAmount = purchaseCost * tasaIva;
        assertThat(taxAmount).isCloseTo(19000.0, within(0.01));

        double minSalePrice = purchaseCost + (purchaseCost * otrosTasa);
        assertThat(minSalePrice).isCloseTo(103000.0, within(0.01));

        double profitAmount = (precioVenta - minSalePrice) * cantidad;
        assertThat(profitAmount).isCloseTo(74000.0, within(0.01));

        double ivaGenerado = precioVenta * tasaIva * cantidad;
        assertThat(ivaGenerado).isCloseTo(53200.0, within(0.01));

        double ivaDifference = ivaGenerado - (taxAmount * cantidad);
        assertThat(ivaDifference).isCloseTo(15200.0, within(0.01));
    }
}

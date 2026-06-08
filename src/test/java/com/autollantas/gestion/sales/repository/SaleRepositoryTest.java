package com.autollantas.gestion.sales.repository;

import com.autollantas.gestion.sales.model.Sale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleRepositoryTest {

    @Autowired
    SaleRepository saleRepository;

    @Test
    void findByStatus_PENDIENTE_deberia_retornar_soloVentasPendientes() {
        Sale pendiente1 = new Sale();
        pendiente1.setStatus("PENDIENTE");
        pendiente1.setTotal(100000.0);

        Sale pendiente2 = new Sale();
        pendiente2.setStatus("PENDIENTE");
        pendiente2.setTotal(200000.0);

        Sale pagada = new Sale();
        pagada.setStatus("PAGADA");
        pagada.setTotal(300000.0);

        saleRepository.saveAll(List.of(pendiente1, pendiente2, pagada));

        List<Sale> result = saleRepository.findByStatus("PENDIENTE");

        assertThat(result).hasSize(2)
                .allMatch(s -> "PENDIENTE".equals(s.getStatus()));
    }

    @Test
    void findBySaleDateBetween_deberia_retornar_ventasDelRango() {
        // SaleRepository no tiene findByCustomer; se testea findBySaleDateBetween.
        LocalDate today = LocalDate.now();

        Sale dentro1 = new Sale();
        dentro1.setSaleDate(today.minusDays(2));
        dentro1.setStatus("PAGADA");

        Sale dentro2 = new Sale();
        dentro2.setSaleDate(today);
        dentro2.setStatus("PAGADA");

        Sale fuera = new Sale();
        fuera.setSaleDate(today.minusDays(10));
        fuera.setStatus("PAGADA");

        saleRepository.saveAll(List.of(dentro1, dentro2, fuera));

        List<Sale> result = saleRepository.findBySaleDateBetween(today.minusDays(5), today);

        assertThat(result).hasSize(2)
                .allMatch(s -> !s.getSaleDate().isBefore(today.minusDays(5)));
    }

    @Test
    void findTopByOrderByIdDesc_deberia_retornar_ultimaVentaGuardada() {
        // Verifica la @Query nativa: SELECT * FROM ventas ORDER BY id_venta DESC LIMIT 1
        Sale primera = new Sale();
        primera.setStatus("PAGADA");
        primera.setInvoiceNumber("VEN-00001");
        saleRepository.save(primera);

        Sale ultima = new Sale();
        ultima.setStatus("PENDIENTE");
        ultima.setInvoiceNumber("VEN-00002");
        saleRepository.save(ultima);

        Optional<Sale> result = saleRepository.findTopByOrderByIdDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getInvoiceNumber()).isEqualTo("VEN-00002");
    }
}

package com.autollantas.gestion.purchases.repository;

import com.autollantas.gestion.purchases.model.Purchase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PurchaseRepositoryTest {

    @Autowired
    PurchaseRepository purchaseRepository;

    @Test
    void findByStatus_deberia_retornar_soloComprasPendientes() {
        Purchase pendiente1 = new Purchase();
        pendiente1.setStatus("PENDIENTE");
        pendiente1.setTotal(1000000.0);

        Purchase pendiente2 = new Purchase();
        pendiente2.setStatus("PENDIENTE");
        pendiente2.setTotal(750000.0);

        Purchase pagada = new Purchase();
        pagada.setStatus("PAGADA");
        pagada.setTotal(500000.0);

        purchaseRepository.saveAll(List.of(pendiente1, pendiente2, pagada));

        List<Purchase> result = purchaseRepository.findByStatus("PENDIENTE");

        assertThat(result).hasSize(2)
                .allMatch(p -> "PENDIENTE".equals(p.getStatus()));
    }

    @Test
    void findByPurchaseDateBetween_deberia_retornar_comprasEnRango() {
        LocalDate today = LocalDate.now();

        Purchase dentro = new Purchase();
        dentro.setPurchaseDate(today.minusDays(1));
        dentro.setStatus("PAGADA");

        Purchase fuera = new Purchase();
        fuera.setPurchaseDate(today.minusDays(20));
        fuera.setStatus("PAGADA");

        purchaseRepository.saveAll(List.of(dentro, fuera));

        List<Purchase> result = purchaseRepository.findByPurchaseDateBetween(
                today.minusDays(5), today);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPurchaseDate()).isEqualTo(today.minusDays(1));
    }

    @Test
    void findByStatus_sinCoincidencias_deberia_retornar_listaVacia() {
        // PurchaseRepository no tiene @Query nativa; tercer test cubre caso borde.
        Purchase p = new Purchase();
        p.setStatus("PAGADA");
        purchaseRepository.save(p);

        List<Purchase> result = purchaseRepository.findByStatus("ANULADA");

        assertThat(result).isEmpty();
    }
}

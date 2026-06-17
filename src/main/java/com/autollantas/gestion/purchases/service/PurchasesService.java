package com.autollantas.gestion.purchases.service;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.model.Payment;
import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.service.InventoryService;
import com.autollantas.gestion.purchases.model.Supplier;
import com.autollantas.gestion.purchases.repository.PurchaseRepository;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.purchases.repository.PurchaseDetailRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import com.autollantas.gestion.treasury.repository.PaymentRepository;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PurchasesService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseDetailRepository purchaseDetailRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final InventoryService inventoryService;

    public PurchasesService(PurchaseRepository purchaseRepository,
                            PurchaseDetailRepository purchaseDetailRepository,
                            SupplierRepository supplierRepository,
                            ProductRepository productRepository,
                            PaymentRepository paymentRepository,
                            AccountRepository accountRepository,
                            MovementRepository movementRepository,
                            InventoryService inventoryService) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseDetailRepository = purchaseDetailRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<Purchase> findAllPurchases() {
        return purchaseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Purchase> findPurchasesByDateBetween(LocalDate start, LocalDate end) {
        return purchaseRepository.findByPurchaseDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDetail> findDetailsByPurchase(Purchase purchase) {
        return purchaseDetailRepository.findByPurchase(purchase);
    }

    @Transactional(readOnly = true)
    public List<Payment> findPaymentsByPurchase(Purchase purchase) {
        return paymentRepository.findByPurchase(purchase);
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String generateNextInvoiceNumber() {
        long max = 0;
        for (Purchase purchase : purchaseRepository.findAll()) {
            String invoiceNumber = purchase.getInvoiceNumber();
            if (invoiceNumber == null || invoiceNumber.isEmpty()) continue;
            String digits = invoiceNumber.replaceAll("\\D+", "");
            if (digits.isEmpty()) continue;
            long current = Long.parseLong(digits);
            if (current > max) max = current;
        }
        long next = max > 0 ? max + 1 : 1;
        return String.format("FAC-%05d", next);
    }

    @Transactional(readOnly = true)
    public double calculateIvaFavor(Purchase purchase) {
        return purchaseDetailRepository.findByPurchase(purchase).stream()
                .mapToDouble(d -> (d.getTax() != null ? d.getTax() : 0.0)
                        * (d.getQuantity() != null ? d.getQuantity() : 0))
                .sum();
    }

    @Transactional
    public Supplier saveOrUpdateSupplier(Supplier selected, String name, String nitNumber,
                                         String email, String phone) {
        if (selected != null && selected.getName() != null
                && selected.getName().equalsIgnoreCase(name)) {
            selected.setNitNumber(nitNumber);
            selected.setEmail(email);
            selected.setPhone(phone);
            return supplierRepository.save(selected);
        }

        Optional<Supplier> existing = supplierRepository.findByNitNumber(nitNumber);
        Supplier supplier = existing.orElse(new Supplier());
        supplier.setName(name);
        supplier.setNitNumber(nitNumber);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Purchase savePurchaseWithDetails(Purchase purchase, List<PurchaseDetail> newDetails,
                                            boolean editMode) {
        Purchase savedPurchase = purchaseRepository.save(purchase);

        if (editMode) {
            List<PurchaseDetail> oldDetails = purchaseDetailRepository.findByPurchase(savedPurchase);
            for (PurchaseDetail oldDetail : oldDetails) {
                Product product = oldDetail.getProduct();
                if (product != null) {
                    product.setQuantity(product.getQuantity() - oldDetail.getQuantity());
                    productRepository.save(product);
                }
            }
            purchaseDetailRepository.deleteAll(oldDetails);
        }

        for (PurchaseDetail detail : newDetails) {
            detail.setPurchase(savedPurchase);
            purchaseDetailRepository.save(detail);

            Product product = detail.getProduct();
            if (product == null || product.getId() == null) continue;

            productRepository.findById(product.getId()).ifPresent(realProduct -> {
                realProduct.setQuantity(realProduct.getQuantity() + detail.getQuantity());
                if (detail.getUnitPrice() != null) {
                    realProduct.setPurchaseCost(detail.getUnitPrice());
                    inventoryService.recalculateMinSalePrice(realProduct);
                }
                productRepository.save(realProduct);
            });
        }

        if (!editMode && "Contado".equals(purchase.getPaymentType()) && purchase.getAccount() != null) {
            Account account = purchase.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current - savedPurchase.getTotal());
            accountRepository.save(account);
            Movement movement = new Movement(
                    purchase.getPurchaseDate() != null ? purchase.getPurchaseDate() : LocalDate.now(),
                    savedPurchase.getId(), "Egreso", savedPurchase.getTotal(), account);
            movement.setSourceTable("COMPRAS");
            movementRepository.save(movement);
        }

        return savedPurchase;
    }

    @Transactional
    public void cancelPurchase(Purchase purchase) {
        if ("Contado".equals(purchase.getPaymentType()) && "PAGADA".equals(purchase.getStatus())
                && purchase.getAccount() != null && purchase.getTotal() != null) {
            Account account = purchase.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current + purchase.getTotal());
            accountRepository.save(account);
            if (purchase.getId() != null) {
                for (Movement m : movementRepository.findBySourceIdAndSourceTable(purchase.getId(), "COMPRAS")) {
                    movementRepository.delete(m);
                }
            }
        }

        List<PurchaseDetail> details = purchaseDetailRepository.findByPurchase(purchase);
        for (PurchaseDetail detail : details) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setQuantity(product.getQuantity() - detail.getQuantity());
                productRepository.save(product);
            }
        }
        purchase.setStatus("ANULADA");
        purchaseRepository.save(purchase);
    }

    @Transactional
    public Purchase restorePurchase(Purchase purchase) {
        purchase.setStatus("PENDIENTE");
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public void registerPayment(Purchase purchase, Account account, LocalDate paymentDate,
                                String paymentMethod, double amount) {
        Payment newPayment = new Payment();
        newPayment.setPurchase(purchase);
        newPayment.setAccount(account);
        newPayment.setDate(paymentDate);
        newPayment.setPaymentMethod(paymentMethod);
        newPayment.setAmount(amount);
        paymentRepository.save(newPayment);

        double currentBalance = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
        account.setCurrentBalance(currentBalance - amount);
        accountRepository.save(account);

        Movement movement = new Movement(paymentDate, newPayment.getId(), "Egreso", amount, account);
        movement.setSourceTable("PAGOS");
        movementRepository.save(movement);

        double currentDebt = purchase.getPendingBalance() != null
                ? purchase.getPendingBalance() : purchase.getTotal();
        double newBalance = currentDebt - amount;
        if (newBalance < 0) newBalance = 0.0;

        purchase.setPendingBalance(newBalance);
        purchase.setAccount(account);
        purchase.setPaymentMethod(paymentMethod);
        purchase.setStatus(newBalance <= 0 ? "PAGADA" : "PENDIENTE");
        purchaseRepository.save(purchase);
    }
}

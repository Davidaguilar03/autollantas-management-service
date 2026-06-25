package com.autollantas.gestion.sales.service;

import com.autollantas.gestion.inventory.model.Product;
import com.autollantas.gestion.inventory.repository.ProductRepository;
import com.autollantas.gestion.sales.model.Customer;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.sales.repository.CustomerRepository;
import com.autollantas.gestion.sales.repository.SaleDetailRepository;
import com.autollantas.gestion.sales.repository.SaleRepository;
import com.autollantas.gestion.treasury.model.Account;
import com.autollantas.gestion.treasury.model.Collection;
import com.autollantas.gestion.treasury.model.Movement;
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.CollectionRepository;
import com.autollantas.gestion.treasury.repository.MovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SalesService {

    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CollectionRepository collectionRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public SalesService(SaleRepository saleRepository,
                        SaleDetailRepository saleDetailRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        CollectionRepository collectionRepository,
                        AccountRepository accountRepository,
                        MovementRepository movementRepository) {
        this.saleRepository = saleRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.collectionRepository = collectionRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<Sale> findAllSales() {
        return saleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Sale> findSalesByDateBetween(LocalDate from, LocalDate to) {
        return saleRepository.findBySaleDateBetween(from, to);
    }

    @Transactional(readOnly = true)
    public List<SaleDetail> findSaleDetailsBySale(Sale sale) {
        return saleDetailRepository.findBySale(sale);
    }

    @Transactional(readOnly = true)
    public List<Collection> findCollectionsBySale(Sale sale) {
        return collectionRepository.findBySale(sale);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String generateNextInvoiceNumber() {
        long max = 0;
        for (Sale sale : saleRepository.findAll()) {
            String invoiceNumber = sale.getInvoiceNumber();
            if (invoiceNumber == null || invoiceNumber.isEmpty()) continue;
            String onlyNumbers = invoiceNumber.replaceAll("\\D+", "");
            if (onlyNumbers.isEmpty()) continue;
            long current = Long.parseLong(onlyNumbers);
            if (current > max) max = current;
        }
        long next = max > 0 ? max + 1 : 1;
        return String.format("VEN-%05d", next);
    }

    @Transactional(readOnly = true)
    public double calculateUtilidad(Sale sale) {
        return saleDetailRepository.findBySale(sale).stream()
                .mapToDouble(d -> d.getProfitAmount() != null ? d.getProfitAmount() : 0.0)
                .sum();
    }

    @Transactional(readOnly = true)
    public double calculateDiferenciaIva(Sale sale) {
        return saleDetailRepository.findBySale(sale).stream()
                .mapToDouble(d -> d.getIvaDifference() != null ? d.getIvaDifference() : 0.0)
                .sum();
    }

    @Transactional
    public Customer saveOrUpdateCustomer(Customer selected,
                                         String name,
                                         String documentNumber,
                                         String email,
                                         String phone,
                                         String documentType) {
        if (selected != null && selected.getName() != null
                && selected.getName().equalsIgnoreCase(name)) {
            selected.setDocumentNumber(documentNumber);
            selected.setEmail(email);
            selected.setPhone(phone);
            selected.setDocumentType(documentType);
            return customerRepository.save(selected);
        }

        Optional<Customer> existing = customerRepository.findByDocumentNumber(documentNumber);
        Customer customer = existing.orElse(new Customer());
        customer.setName(name);
        customer.setDocumentNumber(documentNumber);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setDocumentType(documentType);
        return customerRepository.save(customer);
    }

    @Transactional
    public List<StockAlert> saveSaleWithDetails(Sale sale, List<SaleDetail> newDetails, boolean editMode) {
        Sale savedSale = saleRepository.save(sale);

        if (editMode) {
            List<SaleDetail> oldDetails = saleDetailRepository.findBySale(savedSale);
            for (SaleDetail old : oldDetails) {
                Product product = old.getProduct();
                if (product != null) {
                    product.setQuantity(product.getQuantity() + old.getQuantity());
                    productRepository.save(product);
                }
            }
            saleDetailRepository.deleteAll(oldDetails);
        }

        List<StockAlert> alerts = new ArrayList<>();
        for (SaleDetail detail : newDetails) {
            detail.setSale(savedSale);
            saleDetailRepository.save(detail);

            Product product = detail.getProduct();
            if (product == null || product.getId() == null) continue;

            productRepository.findById(product.getId()).ifPresent(realProduct -> {
                realProduct.setQuantity(realProduct.getQuantity() - detail.getQuantity());
                productRepository.save(realProduct);

                int qty = realProduct.getQuantity() != null ? realProduct.getQuantity() : 0;
                var cat = realProduct.getCategory();
                if (cat != null) {
                    int red    = cat.getRedStockMin()    != null ? cat.getRedStockMin()    : 0;
                    int yellow = cat.getYellowStockMin() != null ? cat.getYellowStockMin() : 0;
                    if (qty <= red) {
                        alerts.add(new StockAlert(realProduct.getDescription(), qty, StockAlertLevel.CRITICAL));
                    } else if (qty <= yellow) {
                        alerts.add(new StockAlert(realProduct.getDescription(), qty, StockAlertLevel.WARNING));
                    }
                }
            });
        }

        if (!editMode && "Contado".equals(sale.getPaymentType()) && sale.getAccount() != null) {
            Account account = sale.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current + savedSale.getTotal());
            accountRepository.save(account);
            Movement movement = new Movement(
                    sale.getSaleDate() != null ? sale.getSaleDate() : LocalDate.now(),
                    savedSale.getId(), "Ingreso", savedSale.getTotal(), account);
            movement.setSourceTable("VENTAS");
            movementRepository.save(movement);
        }

        return alerts;
    }

    @Transactional
    public void cancelSale(Sale sale) {
        if ("Contado".equals(sale.getPaymentType()) && "PAGADA".equals(sale.getStatus())
                && sale.getAccount() != null && sale.getTotal() != null) {
            Account account = sale.getAccount();
            double current = account.getCurrentBalance() != null ? account.getCurrentBalance() : 0.0;
            account.setCurrentBalance(current - sale.getTotal());
            accountRepository.save(account);
            if (sale.getId() != null) {
                for (Movement m : movementRepository.findBySourceIdAndSourceTable(sale.getId(), "VENTAS")) {
                    movementRepository.delete(m);
                }
            }
        }

        List<SaleDetail> details = saleDetailRepository.findBySale(sale);
        for (SaleDetail detail : details) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setQuantity(product.getQuantity() + detail.getQuantity());
                productRepository.save(product);
            }
        }
        sale.setStatus("ANULADA");
        saleRepository.save(sale);
    }

    @Transactional
    public Sale restoreSale(Sale sale) {
        List<SaleDetail> details = saleDetailRepository.findBySale(sale);
        for (SaleDetail detail : details) {
            Product product = detail.getProduct();
            if (product != null) {
                product.setQuantity(product.getQuantity() - detail.getQuantity());
                productRepository.save(product);
            }
        }
        sale.setStatus("PENDIENTE");
        return saleRepository.save(sale);
    }

    @Transactional
    public void registerCollection(Sale sale, Account destinationAccount, LocalDate paymentDate, String paymentMethod, double amount) {
        Collection collection = new Collection();
        collection.setSale(sale);
        collection.setAccount(destinationAccount);
        collection.setDate(paymentDate);
        collection.setPaymentMethod(paymentMethod);
        collection.setAmount(amount);
        collectionRepository.save(collection);

        double currentBalance = destinationAccount.getCurrentBalance() != null ? destinationAccount.getCurrentBalance() : 0.0;
        destinationAccount.setCurrentBalance(currentBalance + amount);
        accountRepository.save(destinationAccount);

        Movement movement = new Movement(paymentDate, collection.getId(), "Ingreso", amount, destinationAccount);
        movement.setSourceTable("RECAUDOS");
        movementRepository.save(movement);

        double pendingDebt = sale.getPendingBalance() != null ? sale.getPendingBalance() : sale.getTotal();
        double newBalance = pendingDebt - amount;
        if (newBalance < 0) newBalance = 0.0;

        sale.setPendingBalance(newBalance);
        sale.setAccount(destinationAccount);
        sale.setPaymentMethod(paymentMethod);
        sale.setStatus(newBalance <= 0 ? "PAGADA" : "PENDIENTE");
        saleRepository.save(sale);
    }
}

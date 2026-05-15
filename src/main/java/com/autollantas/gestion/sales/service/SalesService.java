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
import com.autollantas.gestion.treasury.repository.AccountRepository;
import com.autollantas.gestion.treasury.repository.CollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    public SalesService(SaleRepository saleRepository,
                        SaleDetailRepository saleDetailRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        CollectionRepository collectionRepository,
                        AccountRepository accountRepository) {
        this.saleRepository = saleRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.collectionRepository = collectionRepository;
        this.accountRepository = accountRepository;
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

    @Transactional
    public Customer saveOrUpdateCustomer(Customer selected,
                                         String name,
                                         String documentNumber,
                                         String email,
                                         String phone) {
        if (selected != null && selected.getName() != null
                && selected.getName().equalsIgnoreCase(name)) {
            selected.setDocumentNumber(documentNumber);
            selected.setEmail(email);
            selected.setPhone(phone);
            return customerRepository.save(selected);
        }

        Optional<Customer> existing = customerRepository.findByDocumentNumber(documentNumber);
        Customer customer = existing.orElse(new Customer());
        customer.setName(name);
        customer.setDocumentNumber(documentNumber);
        customer.setEmail(email);
        customer.setPhone(phone);
        return customerRepository.save(customer);
    }

    @Transactional
    public Sale saveSaleWithDetails(Sale sale, List<SaleDetail> newDetails, boolean editMode) {
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

        for (SaleDetail detail : newDetails) {
            detail.setSale(savedSale);
            saleDetailRepository.save(detail);

            Product product = detail.getProduct();
            if (product == null || product.getId() == null) continue;

            productRepository.findById(product.getId()).ifPresent(realProduct -> {
                realProduct.setQuantity(realProduct.getQuantity() - detail.getQuantity());
                productRepository.save(realProduct);
            });
        }

        return savedSale;
    }

    @Transactional
    public void cancelSale(Sale sale) {
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

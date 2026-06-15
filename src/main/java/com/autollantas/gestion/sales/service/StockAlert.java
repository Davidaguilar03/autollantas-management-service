package com.autollantas.gestion.sales.service;

public record StockAlert(String productName, int quantity, StockAlertLevel level) {}

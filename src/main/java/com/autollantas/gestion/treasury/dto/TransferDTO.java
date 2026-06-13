package com.autollantas.gestion.treasury.dto;

import com.autollantas.gestion.treasury.model.Transfer;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;

public class TransferDTO {

    private final SimpleStringProperty description;
    private final SimpleObjectProperty<LocalDate> date;
    private final SimpleDoubleProperty amount;
    private final SimpleStringProperty source;
    private final SimpleStringProperty destination;

    public TransferDTO(Transfer t) {
        String label = (t.getConcept() != null && !t.getConcept().isBlank())
                ? t.getConcept()
                : "Transferencia #" + t.getId();
        this.description = new SimpleStringProperty(label);
        this.date = new SimpleObjectProperty<>(t.getDate());
        this.amount = new SimpleDoubleProperty(t.getAmount());
        this.source = new SimpleStringProperty(t.getSourceAccount() != null ? t.getSourceAccount().getName() : "N/A");
        this.destination = new SimpleStringProperty(t.getDestinationAccount() != null ? t.getDestinationAccount().getName() : "N/A");
    }

    public SimpleStringProperty descriptionProperty() { return description; }
    public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
    public SimpleDoubleProperty amountProperty() { return amount; }
    public SimpleStringProperty sourceProperty() { return source; }
    public SimpleStringProperty destinationProperty() { return destination; }
}

package com.autollantas.gestion.treasury.dto;

import com.autollantas.gestion.treasury.model.Movement;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;

public class MovementDTO {

    private final SimpleObjectProperty<LocalDate> date;
    private final SimpleStringProperty type;
    private final SimpleStringProperty description;
    private final SimpleDoubleProperty amount;

    public MovementDTO(Movement m, String resolvedDescription) {
        this.date = new SimpleObjectProperty<>(m.getDate());
        this.type = new SimpleStringProperty(m.getType());
        this.description = new SimpleStringProperty(resolvedDescription);
        this.amount = new SimpleDoubleProperty(m.getAmount());
    }

    public SimpleObjectProperty<LocalDate> dateProperty() { return date; }
    public SimpleStringProperty typeProperty() { return type; }
    public SimpleStringProperty descriptionProperty() { return description; }
    public SimpleDoubleProperty amountProperty() { return amount; }
}

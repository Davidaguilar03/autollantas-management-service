package com.autollantas.gestion.utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.stream.Stream;

public class ComboBoxAutoComplete<T> {

    private final ComboBox<T> cmb;
    private final ObservableList<T> originalItems;
    private boolean isFiltering = false;

    public ComboBoxAutoComplete(ComboBox<T> cmb) {
        this.cmb = cmb;
        this.originalItems = FXCollections.observableArrayList(cmb.getItems());

        cmb.setEditable(true);

        cmb.getEditor().setOnKeyPressed(this::handleOnKeyPressed);

        cmb.getEditor().textProperty().addListener((observable, oldValue, newValue) -> handleFilter(newValue));

        cmb.getEditor().setOnMouseClicked(event -> {
            if(cmb.getEditor().getText().isEmpty()) {
                cmb.show();
            }
        });
    }

    private void handleOnKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            if (!cmb.getItems().isEmpty()) {
                return;
            }
        }
        if (e.getCode() == KeyCode.ESCAPE) {
            cmb.hide();
        }
    }

    private void handleFilter(String filter) {
        if (isFiltering) return;
        if (filter == null) return;

        int caret = cmb.getEditor().getCaretPosition();

        Platform.runLater(() -> {
            isFiltering = true;

            if (filter.isEmpty()) {
                cmb.setItems(originalItems);
                cmb.hide();
            } else {
                ObservableList<T> filteredList = FXCollections.observableArrayList();
                StringConverter<T> converter = cmb.getConverter();

                for (T item : originalItems) {
                    String itemText = (converter != null) ? converter.toString(item) : item.toString();
                    if (itemText != null && itemText.toLowerCase().contains(filter.toLowerCase())) {
                        filteredList.add(item);
                    }
                }

                cmb.setItems(filteredList);
                cmb.getEditor().setText(filter);

                if (caret <= filter.length()) {
                    cmb.getEditor().positionCaret(caret);
                } else {
                    cmb.getEditor().positionCaret(filter.length());
                }

                if (!filteredList.isEmpty()) {
                    if (!cmb.isShowing()) cmb.show();
                } else {
                    cmb.hide();
                }
            }
            isFiltering = false;
        });
    }
}
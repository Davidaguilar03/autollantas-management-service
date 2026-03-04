package com.autollantas.gestion.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class AutoCompleteBoxListener<T> implements EventHandler<KeyEvent> {

    private ComboBox<T> comboBox;
    private ObservableList<T> data;
    private boolean moveCaretToPos = false;
    private int caretPos;

    public AutoCompleteBoxListener(final ComboBox<T> comboBox) {
        this.comboBox = comboBox;
        this.data = comboBox.getItems();

        this.comboBox.setEditable(true);

        this.comboBox.setOnKeyPressed(t -> {
            comboBox.hide();
        });

        this.comboBox.setOnKeyReleased(AutoCompleteBoxListener.this);
    }

    @Override
    public void handle(KeyEvent event) {
        if(event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN ||
                event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT ||
                event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END ||
                event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER) {
            return;
        }

        if(event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
            moveCaretToPos = true;
            caretPos = comboBox.getEditor().getCaretPosition();
        }

        String textTyped = comboBox.getEditor().getText();
        if (textTyped == null) textTyped = "";
        String lowerCaseText = textTyped.toLowerCase();

        ObservableList<T> list = FXCollections.observableArrayList();
        for (T item : data) {
            if (item != null) {
                String itemText = getItemText(item);
                if (itemText.toLowerCase().contains(lowerCaseText)) {
                    list.add(item);
                }
            }
        }

        comboBox.setItems(list);

        comboBox.getEditor().setText(textTyped);

        if(!moveCaretToPos) {
            caretPos = -1;
        }

        try {
            if (caretPos != -1 && caretPos <= textTyped.length()) {
                comboBox.getEditor().positionCaret(caretPos);
            } else {
                comboBox.getEditor().positionCaret(textTyped.length());
            }
        } catch (Exception ex) {
        }

        moveCaretToPos = false;

        if(!list.isEmpty()) {
            comboBox.show();
        }
    }

    private String getItemText(T item) {
        StringConverter<T> converter = comboBox.getConverter();
        if (converter != null) {
            return converter.toString(item);
        }
        return item.toString();
    }
}
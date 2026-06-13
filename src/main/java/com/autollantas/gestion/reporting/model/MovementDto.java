package com.autollantas.gestion.reporting.model;

import com.autollantas.gestion.treasury.model.Account;
import java.time.LocalDate;

public class MovementDto {

    private final LocalDate date;
    private final Integer sourceId;
    private final String type;
    private final Double amount;
    private final Account account;
    private String sourceTable;
    private String concept;

    public MovementDto(LocalDate date, Integer sourceId, String type, Double amount, Account account) {
        this.date = date;
        this.sourceId = sourceId;
        this.type = type;
        this.amount = amount;
        this.account = account;
    }

    public LocalDate getDate()        { return date; }
    public Integer getSourceId()      { return sourceId; }
    public String getType()           { return type; }
    public Double getAmount()         { return amount; }
    public Account getAccount()       { return account; }
    public String getSourceTable()    { return sourceTable; }
    public void setSourceTable(String t) { this.sourceTable = t; }
    public String getConcept()        { return concept; }
    public void setConcept(String c)  { this.concept = c; }
}

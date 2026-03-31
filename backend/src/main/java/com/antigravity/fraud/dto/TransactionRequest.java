package com.antigravity.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * DTO for incoming transaction analysis requests.
 */
public class TransactionRequest {

    @NotBlank(message = "senderId is required")
    private String senderId;

    @NotBlank(message = "receiverId is required")
    private String receiverId;

    @Positive(message = "amount must be positive")
    private double amount;

    @NotBlank(message = "txType is required")
    private String txType;

    private String mccCode;

    @NotBlank(message = "ipAddress is required")
    private String ipAddress;

    @NotBlank(message = "city is required")
    private String city;

    private LocalDateTime timestamp;

    private String currency;

    /**
     * No-args constructor.
     */
    public TransactionRequest() {
    }

    /**
     * Constructor with all fields.
     */
    public TransactionRequest(String senderId, String receiverId, double amount,
            String txType, String mccCode, String ipAddress, String city,
            LocalDateTime timestamp, String currency) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.txType = txType;
        this.mccCode = mccCode;
        this.ipAddress = ipAddress;
        this.city = city;
        this.timestamp = timestamp;
        this.currency = currency;
    }

    // Getters and Setters

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public String getMccCode() {
        return mccCode;
    }

    public void setMccCode(String mccCode) {
        this.mccCode = mccCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "TransactionRequest{" +
                "senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", amount=" + amount +
                ", txType='" + txType + '\'' +
                ", mccCode='" + mccCode + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", city='" + city + '\'' +
                ", timestamp=" + timestamp +
                ", currency='" + currency + '\'' +
                '}';
    }
}

package com.antigravity.fraud.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * UserProfile domain model - represents per-user baseline behavioral statistics.
 */
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    private String userId;
    private double avgAmount;
    private double stdDevAmount;
    private int totalTransactions;
    private Set<String> knownIpAddresses;
    private Set<String> knownCities;
    private LocalDateTime lastUpdated;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * No-args constructor for MongoDB.
     */
    public UserProfile() {
        this.knownIpAddresses = new HashSet<>();
        this.knownCities = new HashSet<>();
    }

    /**
     * All-args constructor.
     */
    public UserProfile(String id, String userId, double avgAmount, double stdDevAmount,
            int totalTransactions, Set<String> knownIpAddresses, Set<String> knownCities,
            LocalDateTime lastUpdated, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.avgAmount = avgAmount;
        this.stdDevAmount = stdDevAmount;
        this.totalTransactions = totalTransactions;
        this.knownIpAddresses = knownIpAddresses != null ? knownIpAddresses : new HashSet<>();
        this.knownCities = knownCities != null ? knownCities : new HashSet<>();
        this.lastUpdated = lastUpdated;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAvgAmount() {
        return avgAmount;
    }

    public void setAvgAmount(double avgAmount) {
        this.avgAmount = avgAmount;
    }

    public double getStdDevAmount() {
        return stdDevAmount;
    }

    public void setStdDevAmount(double stdDevAmount) {
        this.stdDevAmount = stdDevAmount;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public Set<String> getKnownIpAddresses() {
        return knownIpAddresses;
    }

    public void setKnownIpAddresses(Set<String> knownIpAddresses) {
        this.knownIpAddresses = knownIpAddresses != null ? knownIpAddresses : new HashSet<>();
    }

    public Set<String> getKnownCities() {
        return knownCities;
    }

    public void setKnownCities(Set<String> knownCities) {
        this.knownCities = knownCities != null ? knownCities : new HashSet<>();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", avgAmount=" + avgAmount +
                ", stdDevAmount=" + stdDevAmount +
                ", totalTransactions=" + totalTransactions +
                ", knownIpAddresses=" + knownIpAddresses +
                ", knownCities=" + knownCities +
                ", lastUpdated=" + lastUpdated +
                ", createdAt=" + createdAt +
                '}';
    }
}

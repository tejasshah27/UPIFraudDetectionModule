package com.antigravity.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fraud.rules")
public class RuleConfig {

    private AmountThresholdProperties amountThreshold = new AmountThresholdProperties();
    private OddHourProperties oddHour = new OddHourProperties();
    private VelocityProperties velocity = new VelocityProperties();
    private LocationAnomalyProperties locationAnomaly = new LocationAnomalyProperties();

    public AmountThresholdProperties getAmountThreshold() { return amountThreshold; }
    public void setAmountThreshold(AmountThresholdProperties amountThreshold) { this.amountThreshold = amountThreshold; }

    public OddHourProperties getOddHour() { return oddHour; }
    public void setOddHour(OddHourProperties oddHour) { this.oddHour = oddHour; }

    public VelocityProperties getVelocity() { return velocity; }
    public void setVelocity(VelocityProperties velocity) { this.velocity = velocity; }

    public LocationAnomalyProperties getLocationAnomaly() { return locationAnomaly; }
    public void setLocationAnomaly(LocationAnomalyProperties locationAnomaly) { this.locationAnomaly = locationAnomaly; }

    // -------------------------------------------------------------------------

    public static class AmountThresholdProperties {
        private boolean enabled = true;
        private double dayMax = 50000;
        private double nightMax = 30000;
        private List<String> highRiskMcc = List.of("7995", "5944");

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getDayMax() { return dayMax; }
        public void setDayMax(double dayMax) { this.dayMax = dayMax; }

        public double getNightMax() { return nightMax; }
        public void setNightMax(double nightMax) { this.nightMax = nightMax; }

        public List<String> getHighRiskMcc() { return highRiskMcc; }
        public void setHighRiskMcc(List<String> highRiskMcc) { this.highRiskMcc = highRiskMcc; }
    }

    public static class OddHourProperties {
        private boolean enabled = true;
        private int riskStartHour = 1;
        private int riskEndHour = 5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getRiskStartHour() { return riskStartHour; }
        public void setRiskStartHour(int riskStartHour) { this.riskStartHour = riskStartHour; }

        public int getRiskEndHour() { return riskEndHour; }
        public void setRiskEndHour(int riskEndHour) { this.riskEndHour = riskEndHour; }
    }

    public static class VelocityProperties {
        private boolean enabled = true;
        private int maxTxInWindow = 5;
        private int windowMinutes = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxTxInWindow() { return maxTxInWindow; }
        public void setMaxTxInWindow(int maxTxInWindow) { this.maxTxInWindow = maxTxInWindow; }

        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
    }

    public static class LocationAnomalyProperties {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

package com.emp_management.feature.appraisal.dto;
import com.emp_management.shared.enums.MetricType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class MetricMasterRequest {

    @NotNull(message = "Metric type is required")
    private MetricType metricType;

    @NotBlank(message = "Metric description is required")
    private String metricDescription;

    @NotNull @Min(1)
    private Integer minRating;

    @NotNull @Min(1)
    private Integer maxRating;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    private Boolean isActive = true;

    public MetricType getMetricType() { return metricType; }
    public void setMetricType(MetricType metricType) { this.metricType = metricType; }
    public String getMetricDescription() { return metricDescription; }
    public void setMetricDescription(String metricDescription) { this.metricDescription = metricDescription; }
    public Integer getMinRating() { return minRating; }
    public void setMinRating(Integer minRating) { this.minRating = minRating; }
    public Integer getMaxRating() { return maxRating; }
    public void setMaxRating(Integer maxRating) { this.maxRating = maxRating; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
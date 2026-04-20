package com.emp_management.feature.employee.dto;

import com.emp_management.shared.enums.TaxRegime;

public class TaxRegimeUpdateRequest {

    private TaxRegime taxRegime;

    public TaxRegime getTaxRegime() { return taxRegime; }
    public void setTaxRegime(TaxRegime taxRegime) { this.taxRegime = taxRegime; }
}
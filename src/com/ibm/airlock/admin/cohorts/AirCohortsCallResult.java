package com.ibm.airlock.admin.cohorts;

import com.ibm.airlock.admin.ValidationResults;

public class AirCohortsCallResult {
    public ValidationResults getValidationResults() {
        return validationResults;
    }

    public AirCohortsResponse getItem() {
        return item;
    }

    ValidationResults validationResults;
    AirCohortsResponse item;
    public AirCohortsCallResult(ValidationResults validationResults, AirCohortsResponse item) {
        this.validationResults=validationResults;
        this.item=item;
    }
}

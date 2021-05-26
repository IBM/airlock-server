package com.ibm.airlock.admin.dataimport;

import com.ibm.airlock.admin.ValidationResults;

public class DataImportCallResult {
    public ValidationResults getValidationResults() {
        return validationResults;
    }

    public DataImportResult getItem() {
        return item;
    }

    ValidationResults validationResults;
    DataImportResult item;
    public DataImportCallResult(ValidationResults validationResults, DataImportResult item) {
        this.validationResults=validationResults;
        this.item=item;
    }
}

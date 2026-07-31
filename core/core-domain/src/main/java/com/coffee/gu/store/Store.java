package com.coffee.gu.store;

import com.coffee.gu.enums.StoreStatus;

public class Store {
    private Long id;
    private String name;
    private String branchCode;
    private StoreStatus status;
    private SalesInformation salesInformation;
    private BusinessInformation businessInformation;

    public Store(Long id, String name, String branchCode, StoreStatus status, SalesInformation salesInformation, BusinessInformation businessInformation) {
        this.id = id;
        this.name = name;
        this.branchCode = branchCode;
        this.status = status;
        this.salesInformation = salesInformation;
        this.businessInformation = businessInformation;
    }

    public void fillSalesInformation(SalesInformation salesInformation) {
        this.salesInformation = salesInformation;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public SalesInformation getSalesInformation() {
        return salesInformation;
    }

    public BusinessInformation getBusinessInformation() {
        return businessInformation;
    }
}

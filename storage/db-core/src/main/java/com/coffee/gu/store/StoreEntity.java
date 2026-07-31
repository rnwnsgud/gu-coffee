package com.coffee.gu.store;

import com.coffee.gu.enums.StoreStatus;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.List;

@Table(name = "store")
@Entity
public class StoreEntity extends BaseEntity {
    private String name;
    private String branchCode;
    @Enumerated(EnumType.STRING)
    private StoreStatus status;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private String representative;
    private String tradeName;
    private String businessRegistrationNumber;
    private String businessAddress;

    public StoreEntity() {}

    private StoreEntity(String name, String branchCode, StoreStatus status, String address, Double latitude, Double longitude, String phoneNumber, String representative, String tradeName, String businessRegistrationNumber, String businessAddress) {
        this.name = name;
        this.branchCode = branchCode;
        this.status = status;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phoneNumber = phoneNumber;
        this.representative = representative;
        this.tradeName = tradeName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.businessAddress = businessAddress;
    }

    public static StoreEntity from(Store store) {
        return new StoreEntity(
                store.getName(),
                store.getBranchCode(),
                store.getStatus(),
                store.getSalesInformation().location().address(),
                store.getSalesInformation().location().latitude(),
                store.getSalesInformation().location().longitude(),
                store.getSalesInformation().phoneNumber(),
                store.getBusinessInformation().representative(),
                store.getBusinessInformation().tradeName(),
                store.getBusinessInformation().businessRegistrationNumber(),
                store.getBusinessInformation().businessAddress()
        );
    }

    public Store toModel() {
        return new Store(
          this.id,
          this.name,
          this.branchCode,
          this.status,
          new SalesInformation(
            new StoreLocation(this.address, this.latitude, this.longitude),
            List.of(),
            this.phoneNumber
          ),
          new BusinessInformation(
            this.representative,
            this.tradeName,
            this.businessRegistrationNumber,
            this.businessAddress
          )
        );
    }

    public String getName() {
        return name;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRepresentative() {
        return representative;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public String getBusinessAddress() {
        return businessAddress;
    }
}

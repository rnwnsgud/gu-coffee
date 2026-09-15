package com.coffee.gu.store

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.StoreStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "store")
@Entity
class StoreEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val name: String,
    val branchCode: String,
    @Enumerated(EnumType.STRING)
    val status: StoreStatus,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String,
    val representative: String,
    val tradeName: String,
    val businessRegistrationNumber: String,
    val businessAddress: String,
) : BaseEntity() {

    fun toModel(): Store {
        return Store(
            id = id,
            name = name,
            branchCode = branchCode,
            status = status,
            salesInformation = SalesInformation(
                location = StoreLocation(address, latitude, longitude),
                hours = emptyList(),
                phoneNumber = phoneNumber,
            ),
            businessInformation = BusinessInformation(
                representative = representative,
                tradeName = tradeName,
                businessRegistrationNumber = businessRegistrationNumber,
                businessAddress = businessAddress,
            ),
        )
    }

    companion object {
        fun from(store: Store): StoreEntity {
            val salesInfo = store.salesInformation ?: throw IllegalArgumentException("SalesInformation must not be null")
            val businessInfo = store.businessInformation ?: throw IllegalArgumentException("BusinessInformation must not be null")
            return StoreEntity(
                id = store.id,
                name = store.name,
                branchCode = store.branchCode,
                status = store.status,
                address = salesInfo.location.address,
                latitude = salesInfo.location.latitude,
                longitude = salesInfo.location.longitude,
                phoneNumber = salesInfo.phoneNumber,
                representative = businessInfo.representative,
                tradeName = businessInfo.tradeName,
                businessRegistrationNumber = businessInfo.businessRegistrationNumber,
                businessAddress = businessInfo.businessAddress,
            )
        }
    }
}

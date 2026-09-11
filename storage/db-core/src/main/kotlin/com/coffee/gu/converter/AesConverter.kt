package com.coffee.gu.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class AesConverter(
    private val aesHelper: AesHelper,
) : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null
        return aesHelper.encrypt(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null
        return aesHelper.decrypt(dbData)
    }
}

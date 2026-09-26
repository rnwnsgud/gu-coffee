package com.coffee.gu.menu

import com.coffee.gu.enums.MenuType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

class MenuDetailRedisSerializationTest {

    @Test
    fun `MenuDetailResult should be serialized and deserialized cleanly via Redis serializer`() {
        val jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
        val serializer = JacksonJsonRedisSerializer(jsonMapper, MenuDetailResult::class.java)

        val menu = Menu(
            id = 1L,
            name = "Americano",
            type = MenuType.DRINK,
            price = Price(costPrice = BigDecimal.valueOf(1000), salesPrice = BigDecimal.valueOf(2000)),
            imageUrl = "http://image.com/1.png",
            description = "Rich espresso with water",
            detail = MenuDetail(
                nutrition = Nutrition(capacity = 355.0, caffeine = 150.0, calories = 10.0),
                containedAllergens = "None",
                mayContainAllergens = "Milk"
            )
        )
        val optionGroup = OptionGroup(id = 10L, name = "Temperature", isExclusive = true, isRequired = true)
        val option = Option(id = 100L, optionGroupId = 10L, name = "ICE", extraPrice = BigDecimal.ZERO)

        val original = MenuDetailResult(
            menu = menu,
            optionGroups = listOf(optionGroup),
            options = listOf(option)
        )

        val bytes = serializer.serialize(original)
        assertThat(bytes).isNotNull

        val deserialized = serializer.deserialize(bytes) as MenuDetailResult

        assertThat(deserialized.menu.id).isEqualTo(1L)
        assertThat(deserialized.menu.name).isEqualTo("Americano")
        assertThat(deserialized.menu.salesPrice).isEqualByComparingTo(BigDecimal.valueOf(2000))
        assertThat(deserialized.optionGroups).hasSize(1)
        assertThat(deserialized.optionGroups[0].name).isEqualTo("Temperature")
        assertThat(deserialized.options).hasSize(1)
        assertThat(deserialized.options[0].name).isEqualTo("ICE")
    }
}

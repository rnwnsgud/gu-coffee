package com.coffee.gu.api.controller.v1.request

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateIngredientAvailabilityRequest(
    @get:JsonProperty("isAvailable")
    @param:JsonProperty("isAvailable")
    val isAvailable: Boolean,
)

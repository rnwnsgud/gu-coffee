package com.coffee.admin.domain;

public record AdminMenuDetail(
        AdminNutrition nutrition,
        String containedAllergens,
        String mayContainAllergens
) {

}

package com.coffee.gu.admin.domain;

public record AdminMenuDetail(
        AdminNutrition nutrition,
        String containedAllergens,
        String mayContainAllergens
) {

}

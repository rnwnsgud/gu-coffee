package com.coffee.admin.domain;

public record AdminNutrition(
        Double capacity,
        Double caffeine,
        Double calories,
        Double sodium,
        Double carbohydrate,
        Double sugar,
        Double fat,
        Double saturatedFat,
        Double protein
) {
}

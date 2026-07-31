package com.coffee.gu;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Table(name = "menu")
@Entity
public class AdminMenuEntity extends AdminBaseEntity {

    private String name;
    private BigDecimal costPrice;
    private BigDecimal salesPrice;
    private String imageUrl;
    private String description;
    private Double capacity;
    private Double caffeine;
    private Double calories;
    private Double sodium;
    private Double carbohydrate;
    private Double sugar;
    private Double fat;
    private Double saturatedFat;
    private Double protein;
    private String containedAllergens;
    private String mayContainAllergens;

    protected AdminMenuEntity() {}

    public AdminMenuEntity(String name, BigDecimal costPrice, BigDecimal salesPrice, String imageUrl, String description, Double capacity, Double caffeine, Double calories, Double sodium, Double carbohydrate, Double sugar, Double fat, Double saturatedFat, Double protein, String containedAllergens, String mayContainAllergens) {
        this.name = name;
        this.costPrice = costPrice;
        this.salesPrice = salesPrice;
        this.imageUrl = imageUrl;
        this.description = description;
        this.capacity = capacity;
        this.caffeine = caffeine;
        this.calories = calories;
        this.sodium = sodium;
        this.carbohydrate = carbohydrate;
        this.sugar = sugar;
        this.fat = fat;
        this.saturatedFat = saturatedFat;
        this.protein = protein;
        this.containedAllergens = containedAllergens;
        this.mayContainAllergens = mayContainAllergens;
    }

}


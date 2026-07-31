package com.coffee.gu.menu;

import com.coffee.gu.enums.MenuType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Table(name = "menu")
@Entity
public class MenuEntity extends BaseEntity {

    private String name;
    @Enumerated(EnumType.STRING)
    private MenuType type;
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

    public MenuEntity() {}

    private MenuEntity(String name, MenuType type, BigDecimal costPrice, BigDecimal salesPrice, String imageUrl, String description, Double capacity, Double caffeine, Double calories, Double sodium, Double carbohydrate, Double sugar, Double fat, Double saturatedFat, Double protein, String containedAllergens, String mayContainAllergens) {
        this.name = name;
        this.type = type;
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

    public static MenuEntity from(Menu menu) {
        return new MenuEntity(
                menu.getName(),
                menu.getType(),
                menu.getCostPrice(),
                menu.getSalesPrice(),
                menu.getImageUrl(),
                menu.getDescription(),
                menu.getDetail().nutrition().capacity(),
                menu.getDetail().nutrition().caffeine(),
                menu.getDetail().nutrition().calories(),
                menu.getDetail().nutrition().sodium(),
                menu.getDetail().nutrition().carbohydrate(),
                menu.getDetail().nutrition().sugar(),
                menu.getDetail().nutrition().fat(),
                menu.getDetail().nutrition().saturatedFat(),
                menu.getDetail().nutrition().protein(),
                menu.getDetail().containedAllergens(),
                menu.getDetail().mayContainAllergens()
        );
    }

    public Menu toModel() {
        return new Menu(
                this.id,
                this.name,
                this.type,
                new Price(this.costPrice, this.salesPrice),
                this.imageUrl,
                this.description,
                new MenuDetail(
                        new Nutrition(
                                this.capacity,
                                this.caffeine,
                                this.calories,
                                this.sodium,
                                this.carbohydrate,
                                this.sugar,
                                this.fat,
                                this.saturatedFat,
                                this.protein
                        ),
                        this.containedAllergens,
                        this.mayContainAllergens
                )
        );
    }

    public String getName() {
        return name;
    }
    public MenuType getType() {
        return type;
    }
    public BigDecimal getCostPrice() {
        return costPrice;
    }
    public BigDecimal getSalesPrice() {
        return salesPrice;
    }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }

    public Double getCapacity() {
        return capacity;
    }

    public Double getCaffeine() {
        return caffeine;
    }

    public Double getCalories() {
        return calories;
    }

    public Double getSodium() {
        return sodium;
    }

    public Double getCarbohydrate() {
        return carbohydrate;
    }

    public Double getSugar() {
        return sugar;
    }

    public Double getFat() {
        return fat;
    }

    public Double getSaturatedFat() {
        return saturatedFat;
    }

    public Double getProtein() {
        return protein;
    }

    public String getContainedAllergens() {
        return containedAllergens;
    }

    public String getMayContainAllergens() {
        return mayContainAllergens;
    }
}

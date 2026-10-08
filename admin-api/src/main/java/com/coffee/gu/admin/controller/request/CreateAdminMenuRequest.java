package com.coffee.gu.admin.controller.request;

import com.coffee.gu.admin.domain.CreateAdminMenu;
import com.coffee.gu.admin.domain.AdminMenuDetail;
import com.coffee.gu.admin.domain.AdminNutrition;
import com.coffee.gu.admin.domain.AdminPrice;

import java.math.BigDecimal;

public record CreateAdminMenuRequest(
        String name,
        Long costPrice,
        Long salesPrice,
        String imageUrl,
        String description,
        Double capacity,
        Double caffeine,
        Double calories,
        Double sodium,
        Double carbohydrate,
        Double sugar,
        Double fat,
        Double saturatedFat,
        Double protein,
        String containedAllergens,
        String mayContainAllergens
) {
    public CreateAdminMenuRequest{
        if (costPrice < 0 || salesPrice < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
    }
    public CreateAdminMenu toCommand() {
        return new CreateAdminMenu(
                name,
                new AdminPrice(
                        BigDecimal.valueOf(costPrice),
                        BigDecimal.valueOf(salesPrice)
                ),
                imageUrl,
                description,
                new AdminMenuDetail(
                        new AdminNutrition(capacity, caffeine, calories, sodium, carbohydrate, sugar, fat, saturatedFat, protein),
                        containedAllergens,
                        mayContainAllergens
                )
        );
    }
}

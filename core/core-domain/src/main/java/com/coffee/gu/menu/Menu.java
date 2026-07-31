package com.coffee.gu.menu;

import com.coffee.gu.enums.MenuType;

import java.math.BigDecimal;

public class Menu {
    private Long id;
    private String name;
    private MenuType type;
    private Price price;
    private String imageUrl;
    private String description;
    private MenuDetail detail;

    public Menu(Long id, String name, MenuType type, Price price, String imageUrl, String description, MenuDetail detail) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.imageUrl = imageUrl;
        this.description = description;
        this.detail = detail;
    }

    public static Menu createIdOnly(Long id) {
        return new Menu(id, null, null, null, null, null, null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MenuType getType() {
        return type;
    }

    public boolean isStampEligible() {
        return MenuType.DRINK.equals(this.type);
    }

    public BigDecimal getSalesPrice() {
        return price.salesPrice();
    }

    public BigDecimal getCostPrice() {
        return price.costPrice();
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public String getDescription() {
        return description;
    }
    public MenuDetail getDetail() {
        return detail;
    }
}

package com.coffee.gu.menu;

import java.math.BigDecimal;

public class Option { //ICED(+300), 샷추가(+500), 얼음많이(0)
    private Long id;
    private Long optionGroupId;
    private String name;
    private BigDecimal extraPrice;

    public Option(Long id, Long optionGroupId, String name, BigDecimal extraPrice) {
        this.id = id;
        this.optionGroupId = optionGroupId;
        this.name = name;
        this.extraPrice = extraPrice;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getExtraPrice() {
        return extraPrice;
    }

    public Long getOptionGroupId() {
        return optionGroupId;
    }
}

package com.coffee.gu.menu;

public class OptionGroup { // 온도(배타적/필수), 시럽(비배타적/선택) 상호 배타적 (함께 존재할 수 없음): 하나를 고르면 다른 하나는 선택할 수 없는 관계.

    private Long id;
    private String name;
    private Boolean isExclusive;
    private Boolean isRequired;

    public OptionGroup(Long id, String name, Boolean isExclusive, Boolean isRequired) {
        this.id = id;
        this.name = name;
        this.isExclusive = isExclusive;
        this.isRequired = isRequired;
    }

    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public Boolean getExclusive() {
        return isExclusive;
    }
    public Boolean getRequired() {
        return isRequired;
    }

}

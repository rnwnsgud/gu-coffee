package com.coffee.gu.api.controller.v1.response.menu;

import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuDetailResult;
import com.coffee.gu.menu.Option;
import com.coffee.gu.menu.OptionGroup;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MenuDetailResponse(
        Long id,
        String name,
        BigDecimal salesPrice,
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
        String mayContainAllergens,
        List<OptionGroupResponse> optionGroups
) {
    public static MenuDetailResponse from(MenuDetailResult menuDetail) {
        Menu menu = menuDetail.menu();

        return new MenuDetailResponse(
                menu.getId(),
                menu.getName(),
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
                menu.getDetail().mayContainAllergens(),
                OptionGroupResponse.of(menuDetail.optionGroups(), menuDetail.options())
        );
    }

    public record OptionGroupResponse(
            Long id,
            String name,
            Boolean isExclusive,
            Boolean isRequired,
            List<OptionResponse> options
    ) {
        private static List<OptionGroupResponse> of(List<OptionGroup> optionGroups, List<Option> options) {
            Map<Long, List<Option>> optionsByGroup = options.stream()
                    .collect(Collectors.groupingBy(Option::getOptionGroupId));
            return optionGroups.stream().map(optionGroup ->
                    new OptionGroupResponse(
                            optionGroup.getId(),
                            optionGroup.getName(),
                            optionGroup.getExclusive(),
                            optionGroup.getRequired(),
                            OptionResponse.from(optionsByGroup.getOrDefault(optionGroup.getId(), List.of()))
                    )
            ).toList();
        }

    }

    public record OptionResponse(
            Long id,
            String name,
            BigDecimal extraPrice
    ) {
        private static List<OptionResponse> from(List<Option> options) {
            return options.stream().map(option ->
                    new OptionResponse(
                            option.getId(),
                            option.getName(),
                            option.getExtraPrice()
                    )
            ).toList();
        }

    }

}


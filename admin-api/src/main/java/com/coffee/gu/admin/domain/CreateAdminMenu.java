package com.coffee.gu.admin.domain;

public record CreateAdminMenu(
        String name,
        AdminPrice price,
        String imageUrl,
        String description,
        AdminMenuDetail detail
) {
}

package com.coffee.admin.domain;

public record CreateAdminMenu(
        String name,
        AdminPrice price,
        String imageUrl,
        String description,
        AdminMenuDetail detail
) {
}

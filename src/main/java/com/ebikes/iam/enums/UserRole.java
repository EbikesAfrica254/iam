package com.ebikes.iam.enums;

import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {
    AGENT,
    BRANCH_ADMIN,
    BRANCH_CHECKER,
    BRANCH_OPERATOR,
    BRANCH_FLEET_MANAGER,
    BRANCH_FLEET_SUPPORT,
    BRANCH_INVENTORY_MANAGER,
    BRANCH_MAKER,
    CUSTOMER,
    ORGANIZATION_ADMIN,
    ORGANIZATION_CHECKER,
    ORGANIZATION_OPERATOR,
    ORGANIZATION_FLEET_MANAGER,
    ORGANIZATION_FLEET_SUPPORT,
    ORGANIZATION_INVENTORY_MANAGER,
    ORGANIZATION_MAKER,
    SYSTEM_ADMIN;

    public static UserRole fromString(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Set<String> getRoleNames(Set<UserRole> roles) {
        return roles.stream().map(UserRole::name).collect(Collectors.toSet());
    }

    public static String toCommaSeparated(Set<UserRole> roles) {
        return roles.stream().map(UserRole::name).sorted().collect(Collectors.joining(","));
    }
}

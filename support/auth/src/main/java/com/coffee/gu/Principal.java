package com.coffee.gu;

import com.coffee.gu.enums.PrincipalType;

import java.util.Objects;

public class Principal {
    private String key;
    private PrincipalType type;

    public Principal(String key, PrincipalType type) {
        this.key = key;
        this.type = type;
    }

    public Principal(String key, String type) {
        PrincipalType parsedType = PrincipalType.valueOf(type);
        if (key == null || key.isBlank()) {
            throw new CoreException(ErrorType.INVALID_REQUEST, null);
        }
        this.key = key;
        this.type = parsedType;
    }

    public static Principal user(String userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST, null);
        }
        return new Principal("U" + userId, PrincipalType.USER);
    }

    public static Principal guest(String guestKey) {
        if (guestKey == null || guestKey.isBlank()) {
            throw new CoreException(ErrorType.INVALID_REQUEST, null);
        }
        return new Principal("G" + guestKey, PrincipalType.GUEST);
    }

    public String getKey() {
        return key;
    }

    public PrincipalType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Principal that = (Principal) obj;
        return key.equals(that.key) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type);
    }

}

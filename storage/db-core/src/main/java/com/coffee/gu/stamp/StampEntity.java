package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.enums.StampState;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table(
        name = "stamp",
        indexes = {
                @Index(
                        name = "idx_stamp_user_state_expiry_created",
                        columnList = "principalKey, state, expiredAt, createdAt"
                )
        }
)
@Entity
public class StampEntity extends BaseEntity {
    private String orderKey;
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    @Enumerated(EnumType.STRING)
    private StampState state;
    private LocalDateTime expiredAt;

    public StampEntity() {}

    private StampEntity(String orderKey, String principalKey, PrincipalType principalType, StampState state, LocalDateTime expiredAt) {
        this.orderKey = orderKey;
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.state = state;
        this.expiredAt = expiredAt;
    }

    public static StampEntity from(Stamp stamp) {
        return new StampEntity(
                stamp.getOrderKey(),
                stamp.getPrincipal().getKey(),
                stamp.getPrincipal().getType(),
                stamp.getState(),
                stamp.getExpiredAt());
    }


    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public Stamp toModel() {
        return new Stamp(
                this.id,
                this.orderKey,
                new Principal(this.principalKey, this.principalType),
                this.state,
                this.getCreatedAt(),
                this.expiredAt);
    }
}

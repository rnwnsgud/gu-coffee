package com.coffee.gu.store;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Table(name = "sales_hour")
@Entity
public class SalesHourEntity extends BaseEntity {
    private Long storeId;
    @Column(name = "`day`")
    private DayOfWeek day;
    private LocalTime open;
    private LocalTime close;

    public SalesHourEntity() {}

    public SalesHourEntity(Long storeId, DayOfWeek day, LocalTime open, LocalTime close) {
        this.storeId = storeId;
        this.day = day;
        this.open = open;
        this.close = close;
    }

    public SalesHour toModel() {
        return new SalesHour(
                this.storeId,
                this.day,
                this.open,
                this.close
        );
    }
}

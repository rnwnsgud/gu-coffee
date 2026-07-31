package com.coffee.gu.enums;

public enum EventLogTarget {
    INTERNAL,  // 내부 스프링 핸들러가 처리할 것
    MESSAGE_QUEUE // 알림톡, 타사 시스템 등 외부 MQ로 보낼 것
}

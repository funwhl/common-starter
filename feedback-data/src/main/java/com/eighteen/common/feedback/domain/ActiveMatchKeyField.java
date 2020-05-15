package com.eighteen.common.feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ActiveMatchKeyField {
    private String matchKey;

    private String matchField;

    public ActiveMatchKeyField(String matchField, String matchKey) {
        this.matchField = matchField;
        this.matchKey = matchKey;
    }
}

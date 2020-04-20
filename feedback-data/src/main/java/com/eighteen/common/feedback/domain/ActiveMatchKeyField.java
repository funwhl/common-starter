package com.eighteen.common.feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveMatchKeyField {
    private String matchKey;

    private String matchField;
}

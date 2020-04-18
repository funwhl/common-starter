package com.eighteen.common.feedback.domain;


import com.eighteen.common.feedback.entity.ClickLog;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
public class ClickMatchRetryMessage implements Serializable {
    private ClickLog clickLog;

    /**
     * newuserretry唯一标识（datasource_id)
     */
    private String uniqueNewUserRetryId;
}

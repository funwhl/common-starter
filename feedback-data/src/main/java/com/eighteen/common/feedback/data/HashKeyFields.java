package com.eighteen.common.feedback.data;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * @author admin
 */
@Data
@Accessors(chain = true)
public class HashKeyFields {
    private String redisKey;

    private Set<String> hashFields;
}

package com.eighteen.common.feedback.domain;

import com.eighteen.common.feedback.constants.DsConstants;
import lombok.Data;

/**
 * 唯一标志点击日志
 * @author admin
 */
@Data
public class UniqueClickLog {

    private String clickType;

    private Long clickLogId;

    public UniqueClickLog(String clickType, Long clickLogId) {
        this.clickType = clickType;
        this.clickLogId = clickLogId;
    }

    public String ToUniqueId() {
        String uniqueClickLogId = String.format("%d_%d", ClickType.fromType(clickType).getId(), clickLogId);
        return uniqueClickLogId;
    }

    public static UniqueClickLog FromUniqueId(String uniqueId) {
        String[] uniqueIdArray = uniqueId.split("_");
        ClickType clickType = ClickType.fromId(Integer.valueOf(uniqueIdArray[0]).intValue());
        Long clickLogId = Long.valueOf(uniqueIdArray[1]);
        return new UniqueClickLog(clickType.getType(), clickLogId);
    }
}

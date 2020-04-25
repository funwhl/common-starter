package com.eighteen.common.feedback.domain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.constants.Constants;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : wangwei
 * @date : 2020/4/25 10:23
 */

@Entity
@Table(name = "t_feedback_config")
@Data
@Accessors(chain = true)
public class FeedbackConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 0:channel exclude , 1:channel include , 2:channel 字段配置 3：支持的媒体类型
     */
    private Integer type;

    /**
     * 类型为2时 value为json格式
     */
    private String value;
    private String description;
    private Date createTime;
    private Date updateTime;

}

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
    // 0:channel exclude , 1:channel include , 2:channel 字段配置
    private Integer type;
    private String value;
    private String description;
    private Date createTime;
    private Date updateTime;

    public List<String> getMatchWdByChannel(String channel) {
        if (Constants.FEEDBACK_CONFIG_TYPE.CHANNEL_WD != this.type || StringUtils.isBlank(channel)) return null;
        JSONObject parse = (JSONObject) JSON.parse(this.value);

        return null;
    }

    public static void main(String[] args) {
        Map<String, String> wds = new HashMap<>();
        wds.put("111", "0,1,2,3");
        wds.put("222", "0,1,2,3");
        String s = JSONObject.toJSONString(wds);
        Map<String,String> parse = (Map) JSON.parse(s);

        System.out.println(s);
    }

}

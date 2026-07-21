package top.wain.bolt.model.enums;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @Description: 交易类型，当前仅实现 RTB 实时竞价
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DealType.RTB.class, name = "rtb")
})
public sealed interface DealType {

    /**
     * RTB 实时竞价
     * @param bidFloor 底价，单位：分/CPM
     */
    record RTB(long bidFloor) implements DealType {}
}

package top.wain.bolt.model.enums;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @Description: 交易类型，RTB实时竞价 / PD优先交易 / PDB程序化保量
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DealType.RTB.class, name = "rtb"),
        @JsonSubTypes.Type(value = DealType.PD.class, name = "pd"),
        @JsonSubTypes.Type(value = DealType.PDB.class, name = "pdb")
})
public sealed interface DealType {

    /**
     * RTB 实时竞价
     * @param bidFloor 底价，单位：分/CPM
     */
    record RTB(long bidFloor) implements DealType {}

    /**
     * PD 优先交易（Preferred Deal）
     * @param dealId 交易ID
     * @param fixedPrice 固定结算价，单位：分/CPM
     */
    record PD(String dealId, long fixedPrice) implements DealType {}

    /**
     * PDB 程序化保量（Programmatic Guaranteed）
     * @param dealId 交易ID
     * @param fixedPrice 固定结算价，单位：分/CPM
     */
    record PDB(String dealId, long fixedPrice) implements DealType {}
}

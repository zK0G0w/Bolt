package top.wain.bolt.model.domain;

/**
 * @Description: 广告源配置，竞价编排的最小调度单元。按竞价模式分型：RTB实时竞价 / 固定出价
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public sealed interface AdSource {

    /** 广告源唯一标识 */
    String sourceId();

    /** 所属广告位ID */
    String adPositionId();

    /** 绑定的DSP平台ID */
    String platformId();

    /** 平台侧广告位ID */
    String platformSlotId();

    /** 请求超时，单位毫秒 */
    int timeoutMs();

    /**
     * RTB 竞价模式广告源
     * 携带底价、利润率、加价策略，竞价阶段参与实时竞拍
     * @param sourceId 广告源唯一标识
     * @param adPositionId 所属广告位ID
     * @param platformId 绑定的DSP平台ID
     * @param platformSlotId 平台侧广告位ID
     * @param timeoutMs 请求超时，单位毫秒
     * @param bidFloor 底价，分/CPM
     * @param profitRatio 利润率 0-100，用于从结算价中扣除引擎利润
     * @param markup 加价策略，发给DSP前对底价的调整方式
     */
    record RtbSource(
            String sourceId,
            String adPositionId,
            String platformId,
            String platformSlotId,
            int timeoutMs,
            long bidFloor,
            int profitRatio,
            PriceMarkup markup
    ) implements AdSource {}

    /**
     * 固定出价模式广告源
     * 不参与实时竞价，以固定价格直接结算
     * @param sourceId 广告源唯一标识
     * @param adPositionId 所属广告位ID
     * @param platformId 绑定的DSP平台ID
     * @param platformSlotId 平台侧广告位ID
     * @param timeoutMs 请求超时，单位毫秒
     * @param fixedBidPrice 固定出价，分/CPM
     */
    record FixedPriceSource(
            String sourceId,
            String adPositionId,
            String platformId,
            String platformSlotId,
            int timeoutMs,
            long fixedBidPrice
    ) implements AdSource {}

    /**
     * 加价策略，决定发给DSP的底价如何在原始底价基础上调整
     */
    sealed interface PriceMarkup {

        /** 比例加价：底价 * (100 + percent) / 100 */
        record Ratio(int percent) implements PriceMarkup {
            public Ratio {
                if (percent < 0) {
                    throw new IllegalArgumentException("percent must be >= 0, got: " + percent);
                }
            }
        }

        /** 固定提交价：直接用此值作为发给DSP的底价 */
        record Fixed(long price) implements PriceMarkup {}
    }
}

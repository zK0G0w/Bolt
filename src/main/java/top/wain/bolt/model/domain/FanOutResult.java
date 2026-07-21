package top.wain.bolt.model.domain;

import java.util.List;
import java.util.Map;

/**
 * @Description: DSP 扇出阶段的输出物，携带出价结果和已解析的广告源配置
 * @Author: WainZeng
 * @Date: 2026/07/21
 */
public record FanOutResult(
        List<DspBidResult> results,
        Map<String, AdSource> resolvedSources
) {}

package top.wain.bolt.repository;

import top.wain.bolt.model.domain.DspPlatform;

import java.util.Optional;

/**
 * @Description: DSP平台配置仓储，按平台ID查找平台信息（URL、限流参数等）
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
public interface DspPlatformRepository {

    Optional<DspPlatform> findById(String platformId);
}

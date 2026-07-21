package top.wain.bolt.repository;

import top.wain.bolt.model.domain.AdSource;

import java.util.List;

/**
 * @Description: 广告源配置仓储，按广告位ID查找绑定的广告源列表
 * @Author: WainZeng
 * @Date: 2026/07/22
 */
public interface AdSourceRepository {

    List<AdSource> findByAdPositionId(String adPositionId);
}

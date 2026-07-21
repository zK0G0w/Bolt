package top.wain.bolt.cache;

/**
 * @Description: 缓存失效消息，对应 Pub/Sub 频道的 JSON payload
 * @Author: WainZeng
 * @Date: 2026/07/21
 * @param entity 实体类型：adsource / dsp
 * @param id 实体唯一标识
 * @param action 操作类型：update / delete
 */
public record CacheInvalidationMessage(String entity, String id, String action) {
}

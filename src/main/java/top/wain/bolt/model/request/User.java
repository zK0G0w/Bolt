package top.wain.bolt.model.request;

import java.util.List;

/**
 * @Description: 用户信息
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 用户唯一标识
 * @param tags 用户兴趣标签列表
 */
public record User(
        String id,
        List<String> tags
) {}

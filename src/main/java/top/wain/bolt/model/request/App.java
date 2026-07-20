package top.wain.bolt.model.request;

/**
 * @Description: 应用信息
 * @Author: WainZeng
 * @Date: 2026/07/20
 * @param id 应用ID
 * @param name 应用名称
 * @param bundle 应用包名，如 com.example.app
 * @param version 应用版本号
 */
public record App(
        String id,
        String name,
        String bundle,
        String version
) {}

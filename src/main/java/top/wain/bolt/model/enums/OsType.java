package top.wain.bolt.model.enums;

/**
 * @Description: 操作系统类型枚举
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public enum OsType {

    UNKNOWN(0, "未知"),
    ANDROID(1, "Android"),
    IOS(2, "iOS"),
    WINDOWS(3, "Windows"),
    MAC(4, "macOS"),
    LINUX(5, "Linux"),
    HARMONY(6, "HarmonyOS");

    private final int code;
    private final String desc;

    OsType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OsType fromCode(int code) {
        for (OsType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

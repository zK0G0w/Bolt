package top.wain.bolt.model.enums;

/**
 * @Description: 运营商类型枚举
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public enum Carrier {

    UNKNOWN(0, "未知"),
    CHINA_MOBILE(1, "中国移动"),
    CHINA_UNICOM(2, "中国联通"),
    CHINA_TELECOM(3, "中国电信");

    private final int code;
    private final String desc;

    Carrier(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static Carrier fromCode(int code) {
        for (Carrier type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

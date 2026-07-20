package top.wain.bolt.model.enums;

/**
 * @Description: 设备类型枚举
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public enum DeviceType {

    UNKNOWN(0, "未知"),
    PC(1, "PC"),
    PHONE(2, "手机"),
    PAD(3, "平板"),
    TV(4, "电视");

    private final int code;
    private final String desc;

    DeviceType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DeviceType fromCode(int code) {
        for (DeviceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

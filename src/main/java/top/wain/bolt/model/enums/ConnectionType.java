package top.wain.bolt.model.enums;

/**
 * @Description: 网络连接类型枚举
 * @Author: WainZeng
 * @Date: 2026/07/20
 */
public enum ConnectionType {

    UNKNOWN(0, "未知"),
    WIFI(1, "WIFI"),
    CELLULAR_2G(2, "2G"),
    CELLULAR_3G(3, "3G"),
    CELLULAR_4G(4, "4G"),
    CELLULAR_5G(5, "5G");

    private final int code;
    private final String desc;

    ConnectionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ConnectionType fromCode(int code) {
        for (ConnectionType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

package com.berrontech.weight.scale.ble;

/**
 * Create by levent8421 2021/1/27 20:50
 * BleCommandMetadata
 * 蓝牙命令相关常量
 *
 * @author levent8421
 */
public class BleCommandMetadata {
    /**
     * 等待消息回复的超时
     */
    public static final int RESPONSE_TIMEOUT = 2 * 1000;
    /**
     * 蓝牙连接超时
     */
    public static final int CONNECT_TIMEOUT = 20 * 1000;
    /**
     * 行尾结束符
     */
    public static final byte[] LINE_END = {'\r', '\n'};
    /**
     * 数据包结束符
     */
    public static final byte[] PACKAGE_END = {0x0D, 0x0A};
    /**
     * 空格
     */
    public static final String SP = " ";
    public static final byte SP_BYTE = ' ';
    /**
     * 取重量命令
     */
    public static final byte[] CMD_READ_WEIGHT = {'W'};
    /**
     * 取重量命令回应数据长度
     */
    public static final int CMD_READ_WEIGHT_RESPONSE_LENGTH = 8;
    public static final byte[] CMD_CLEAR_TARE = {'T'};
    /**
     * 清零命令
     */
    public static final byte[] ZERO_CMD = {'Z'};
    /**
     * 发送数据指令
     */
    public static final byte[] CMD_SEND_DATA = {'S', 'E', 'N', 'D'};
    /**
     * 获取容量命令
     */
    public static final byte[] CMD_CAPACITY = {'C', 'A', 'P', 'A', 'C', 'I', 'T', 'Y'};
    /**
     * 设置小数命令
     */
    public static final byte[] CMD_DECIMAL = {'D', 'E', 'C', 'I', 'M', 'A', 'L'};
}

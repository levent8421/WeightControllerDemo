package com.berrontech.weight.scale.utils;

/**
 * Create by levent8421 2021/1/28 14:35
 * CmdUtils
 * 命令工具
 *
 * @author levent8421
 */
public class CmdUtils {
    /**
     * 将命令回复转换为可读字符串
     *
     * @param response 回应数据
     * @return 字符串
     */
    public static String asPlainText(String[] response) {
        final StringBuilder sb = new StringBuilder();
        for (String item : response) {
            sb.append(item).append(' ');
        }
        return sb.toString();
    }
}

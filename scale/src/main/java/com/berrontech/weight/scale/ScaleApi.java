package com.berrontech.weight.scale;

import android.content.Context;

/**
 * Create by levent8421 2021/1/27 16:55
 * ScaleApi
 * Scale Api
 *
 * @author levent8421
 */
public interface ScaleApi {
    /**
     * 状态：正常
     */
    int STATUS_OK = 0x00;
    /**
     * 状态：异常
     */
    int STATUS_ERROR = 0x01;

    /**
     * Api Init
     *
     * @param context context
     * @throws Exception exception
     */
    void init(Context context) throws Exception;

    /**
     * 连接
     *
     * @throws Exception e
     */
    void connect() throws Exception;

    /**
     * 断开
     *
     * @throws Exception e
     */
    void close() throws Exception;

    /**
     * 去皮
     *
     * @return weight
     * @throws Exception e
     */
    int clearTare() throws Exception;

    /**
     * 获取重量
     *
     * @return [重量，单位]
     * @throws Exception exception
     */
    String[] getWeight() throws Exception;

    /**
     * 清零
     *
     * @return status
     * @throws Exception E
     */
    int zeroClear() throws Exception;

    /**
     * 获取状态
     *
     * @return status
     * @throws Exception any error
     */
    int getStatus() throws Exception;

    /**
     * 获取最大量程
     *
     * @return 量程
     * @throws Exception any error
     */
    float getMaxWeight() throws Exception;

    /**
     * 设置保留小数位数
     *
     * @param num num
     * @throws Exception any error
     */
    void setPoint(int num) throws Exception;

    int sendCmd(byte[] bytes, int timeout) throws Exception;
}

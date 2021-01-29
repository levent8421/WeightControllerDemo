package com.berrontech.weight.scale.ble;

import android.util.Log;

import com.berrontech.weight.scale.commons.BleConnection;
import com.berrontech.weight.scale.commons.BleConnectionReceiver;

/**
 * Create by levent8421 2021/1/27 19:38
 * BleConnectionStateListener
 * 蓝牙连接状态监听器
 *
 * @author levent8421
 */
public class BleConnectionStateListener implements BleConnectionReceiver.BleConnectionListener {
    private static final String TAG = "StateListener";
    private final BleScaleApi scaleApi;

    public BleConnectionStateListener(BleScaleApi scaleApi) {
        this.scaleApi = scaleApi;
    }

    @Override
    public void onConnected(BleConnection connection) {
    }

    @Override
    public void onSppReady(BleConnection connection) {
        scaleApi.setReadyState(true);
    }

    @Override
    public void onDataSent(BleConnection connection) {

    }

    @Override
    public void onDataSentError(BleConnection connection) {

    }

    @Override
    public void onDataReceived(BleConnection connection) {
        final byte[] lineBytes = connection.getRecvBuffer().readLine(BleCommandMetadata.LINE_END);
        final String line = new String(lineBytes);
        Log.d(TAG, "onDataReceived: " + line);
    }

    @Override
    public void onDisconnected(BleConnection connection) {
        scaleApi.setReadyState(false);
    }
}

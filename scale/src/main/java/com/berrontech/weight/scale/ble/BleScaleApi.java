package com.berrontech.weight.scale.ble;

import android.content.Context;
import android.util.Log;

import com.berrontech.weight.scale.InvalidateResponseException;
import com.berrontech.weight.scale.OperationFailedException;
import com.berrontech.weight.scale.ScaleApi;
import com.berrontech.weight.scale.ScaleApiConfig;
import com.berrontech.weight.scale.commons.BleConnection;
import com.berrontech.weight.scale.utils.CmdUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Create by levent8421 2021/1/27 16:59
 * BleScaleApi
 * Bluetooth Scale APi
 *
 * @author levent8421
 */
public class BleScaleApi implements ScaleApi {
    public static final String TAG = "BleScaleApi";
    private static final String UNIT_KG = "kg";
    private static final String UNIT_G = "g";

    private static final byte[] DEFAULT_DATA_CHANNEL = {'0'};
    /**
     * 清零指令最小回应数据数量
     */
    public static final int ZERO_RESPONSE_MIN_ITEMS = 2;
    public static final String STATUS_SUCCESS = "A";
    private static final String SEND_DATA_STATUS_READY = "B";
    private static final String[] EMPTY_RESPONSE = {};

    private final ScaleApiConfig scaleApiConfig;
    private Context context;
    private BleConnection connection;
    private final BleConnectionStateListener stateListener;
    private boolean ready;
    private CountDownLatch readyWatcherLatch;

    public BleScaleApi(ScaleApiConfig scaleApiConfig) {
        this.scaleApiConfig = scaleApiConfig;
        stateListener = new BleConnectionStateListener(this);
    }

    @Override
    public void init(Context context) throws Exception {
        this.context = context;
    }

    @Override
    public synchronized void connect() throws Exception {
        connection = new BleConnection(context, stateListener);
        final String name = scaleApiConfig.get(ScaleApiConfig.DEVICE_NAME, String.class);
        final String address = scaleApiConfig.get(ScaleApiConfig.DEVICE_ADDRESS, String.class);
        final Integer timeout = scaleApiConfig.get(ScaleApiConfig.CONNECT_TIMEOUT, Integer.class);
        readyWatcherLatch = new CountDownLatch(1);
        connection.setParam(name, address)
                .setCanDoNotify(false)
                .open();
        final boolean success = readyWatcherLatch.await(timeout, TimeUnit.MILLISECONDS);
        if (!success) {
            close();
            throw new BleConnectionException("Connection timeout");
        }
    }

    public void setReadyState(boolean ready) {
        this.ready = ready;
        if (ready && this.readyWatcherLatch != null) {
            this.readyWatcherLatch.countDown();
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    private void makeSureReady() throws BleConnectionException {
        if (!ready || !connection.isConnected()) {
            throw new BleConnectionException("Connection closed!");
        }
    }

    @Override
    public int clearTare() throws Exception {
        final String[] response = sendCmd4Response(BleCommandMetadata.CMD_CLEAR_TARE);
        if (response.length < 2) {
            throw new InvalidateResponseException("Invalidate response[T]:" + CmdUtils.asPlainText(response));
        }
        return 0;
    }

    @Override
    public String[] getWeight() throws Exception {
        // Response Format:
        // 0 1 2  3    4    5    6   7
        // W A = D/S Gross Tare Net kg/g
        final String[] response = sendCmd4Response(BleCommandMetadata.CMD_READ_WEIGHT);
        if (response.length != BleCommandMetadata.CMD_READ_WEIGHT_RESPONSE_LENGTH) {
            throw new InvalidateResponseException("Invalidate Response(W) from device:" + CmdUtils.asPlainText(response));
        }
        return new String[]{response[6], response[7]};
    }


    @Override
    public int zeroClear() throws Exception {
        final String[] response = sendCmd4Response(BleCommandMetadata.ZERO_CMD);
        if (response.length < ZERO_RESPONSE_MIN_ITEMS) {
            throw new InvalidateResponseException("Invalidate response(Z) from device:" + CmdUtils.asPlainText(response));
        }
        if (!STATUS_SUCCESS.equals(response[1])) {
            throw new OperationFailedException("Operation[Z] failed:" + CmdUtils.asPlainText(response));
        }
        return 0;
    }

    @Override
    public int getStatus() {
        return (ready && connection.isConnected()) ? STATUS_OK : STATUS_ERROR;
    }

    @Override
    public float getMaxWeight() throws Exception {
        final String[] response = sendCmd4Response(BleCommandMetadata.CMD_CAPACITY);
        if (response.length < 4) {
            throw new InvalidateResponseException("Invalidate response[CAPACITY]:" + CmdUtils.asPlainText(response));
        }
        final String status = response[1];
        if (!STATUS_SUCCESS.equals(status)) {
            throw new OperationFailedException("Operation[CAPACITY] fail:" + CmdUtils.asPlainText(response));
        }
        final String unit = response[3];
        final String capacity = response[2];
        final float maxWeight = Float.parseFloat(capacity);
        switch (unit.toLowerCase()) {
            case UNIT_G:
                return maxWeight / 1000;
            case UNIT_KG:
                return maxWeight;
            default:
                throw new InvalidateResponseException("Invalidate unit response[CAPACITY]:" + CmdUtils.asPlainText(response));
        }
    }

    @Override
    public void setPoint(int num) throws Exception {
        final byte[] numBytes = String.valueOf(num).getBytes();
        final byte[] cmdBytes = new byte[BleCommandMetadata.CMD_DECIMAL.length + numBytes.length + 1];
        System.arraycopy(BleCommandMetadata.CMD_DECIMAL, 0, cmdBytes, 0, BleCommandMetadata.CMD_DECIMAL.length);
        cmdBytes[BleCommandMetadata.CMD_DECIMAL.length] = BleCommandMetadata.SP_BYTE;
        System.arraycopy(numBytes, 0, cmdBytes, BleCommandMetadata.CMD_DECIMAL.length + 1, numBytes.length);

        final String[] response = sendCmd4Response(cmdBytes);
        if (response.length < 2) {
            throw new InvalidateResponseException("Invalidate response[DECIMAL]:" + CmdUtils.asPlainText(response));
        }
        final String status = response[1];
        if (!STATUS_SUCCESS.equals(status)) {
            throw new OperationFailedException("Operation[DECIMAL] fail:" + CmdUtils.asPlainText(response));
        }
    }


    @Override
    public int sendCmd(byte[] bytes, int timeout) throws Exception {
        Log.e(TAG, "sendCmd: LEN=" + bytes.length);
        final byte[] len = String.valueOf(bytes.length).getBytes();
        final byte[] timeoutBytes = String.valueOf(timeout).getBytes();
        // FORMAT: SEND Channel length timeout CR LF
        final byte[] cmdBytes = new byte[BleCommandMetadata.CMD_SEND_DATA.length
                + DEFAULT_DATA_CHANNEL.length
                + len.length
                + timeoutBytes.length
                + 3];
        // SEND
        int pos = 0;
        System.arraycopy(BleCommandMetadata.CMD_SEND_DATA, 0, cmdBytes, pos, BleCommandMetadata.CMD_SEND_DATA.length);
        pos += BleCommandMetadata.CMD_SEND_DATA.length;
        cmdBytes[pos] = BleCommandMetadata.SP_BYTE;
        pos++;
        // Channel
        System.arraycopy(DEFAULT_DATA_CHANNEL, 0, cmdBytes, pos, DEFAULT_DATA_CHANNEL.length);
        pos += DEFAULT_DATA_CHANNEL.length;
        cmdBytes[pos] = BleCommandMetadata.SP_BYTE;
        pos++;
        // Length
        System.arraycopy(len, 0, cmdBytes, pos, len.length);
        pos += len.length;
        cmdBytes[pos] = BleCommandMetadata.SP_BYTE;
        pos++;
        // Timeout
        System.arraycopy(timeoutBytes, 0, cmdBytes, pos, timeoutBytes.length);
        //        pos += timeoutBytes.length;

        final String[] response = sendCmd4Response(cmdBytes);
        if (response.length < 2) {
            throw new InvalidateResponseException("Invalidate response[SEND]:" + CmdUtils.asPlainText(response));
        }
        final String readyStatus = response[1];
        if (!SEND_DATA_STATUS_READY.equals(readyStatus)) {
            throw new OperationFailedException("Operation fail:" + CmdUtils.asPlainText(response));
        }
        connection.write(bytes);
        final byte[] lineBytes = connection.getRecvBuffer().readLine(BleCommandMetadata.LINE_END, timeout);
        final String[] res = parseResponse(lineBytes);
        if (res.length < 2) {
            throw new InvalidateResponseException("Invalidate response[SEND RES]:" + CmdUtils.asPlainText(res));
        }
        final String sendStatus = res[1];
        if (!STATUS_SUCCESS.equals(sendStatus)) {
            throw new InvalidateResponseException("Invalidate response[SEND RES]:" + CmdUtils.asPlainText(res));
        }
        return bytes.length;
    }

    private String[] parseResponse(byte[] bytes) {
        if (bytes == null) {
            return EMPTY_RESPONSE;
        }
        final String line = new String(bytes);
        return line.split(BleCommandMetadata.SP);
    }

    private String[] sendCmd4Response(byte[] cmd) throws Exception {
        makeSureReady();
        sendDummy();
        final byte[] bytes = new byte[cmd.length + BleCommandMetadata.PACKAGE_END.length];
        System.arraycopy(cmd, 0, bytes, 0, cmd.length);
        System.arraycopy(BleCommandMetadata.PACKAGE_END, 0, bytes, cmd.length, BleCommandMetadata.PACKAGE_END.length);
        connection.write(bytes);

        final int timeout = scaleApiConfig.get(ScaleApiConfig.CMD_TIMEOUT, Integer.class);
        final byte[] response = connection.getRecvBuffer()
                .readLine(BleCommandMetadata.LINE_END, timeout);
        return parseResponse(response);
    }

    private void sendDummy() throws Exception {
        connection.getRecvBuffer().clear();
        connection.write("xxx\r\n");
        connection.getRecvBuffer().readLine(BleCommandMetadata.LINE_END, 200);
    }
}

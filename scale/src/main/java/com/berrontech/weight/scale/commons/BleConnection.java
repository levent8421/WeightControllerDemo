package com.berrontech.weight.scale.commons;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.UUID;


/**
 * Create by lastnika 2017/8/21.
 * BleConnection
 *
 * @author lastnika
 */

public class BleConnection extends BasicConnection {
    private final static String TAG = BleConnection.class.getSimpleName();

    /**
     * <summary>
     * MT BLE transfer service GUID
     * </summary>
     */
    public final static String SERVICE_GUID_NAME = "0000fff0-0000-1000-8000-00805f9b34fb";
    public final static UUID SERVICE_GUID = UUID.fromString(SERVICE_GUID_NAME);

    /**
     * <summary>
     * MT BLE SPP characteristic GUID
     * </summary>
     */
    public final static String SPP_CHARACTERISTIC_GUID_NAME = "0000fff1-0000-1000-8000-00805f9b34fb";
    public final static UUID SPP_CHARACTERISTIC_GUID = UUID.fromString(SPP_CHARACTERISTIC_GUID_NAME);
    public final static String SPP_WRITE_CHARACTERISTIC_GUID_NAME = "0000fff2-0000-1000-8000-00805f9b34fb";
    public final static UUID SPP_WRITE_CHARACTERISTIC_GUID = UUID.fromString(SPP_WRITE_CHARACTERISTIC_GUID_NAME);
    public static final int STATE_INVALID = -1;
    public static final int STATE_UNREACHABLE = -2;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;

    public final static String ACTION_GATT_CONNECTED = "com.monolith.iot.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.monolith.iot.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_DATA_RECEIVED = "com.monolith.iot.bluetooth.le.ACTION_DATA_RECEIVED";
    public final static String ACTION_SPP_READY = "com.monolith.iot.bluetooth.le.ACTION_SPP_READY";
    public final static String ACTION_DATA_SENT = "com.monolith.iot.bluetooth.le.ACTION_DATA_SENT";
    public final static String ACTION_DATA_SEND_ERROR = "com.monolith.iot.bluetooth.le.ACTION_DATA_SEND_ERROR";
    public final static String EXTRA_DEVICE_TAG = "com.monolith.iot.bluetooth.le.EXTRA_DEVICE_TAG";

    private static final int COMBO_ERROR_MAX = 3;

    private final BleConnectionReceiver receiver;
    private int comboErrorCnt = 0;
    protected String name;
    protected String address;
    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private BluetoothDevice bleDevice;
    private BluetoothGatt bleGatt;
    private BluetoothGattCharacteristic sppCharacteristic;
    private BluetoothGattCharacteristic sppWriteCharacteristic;

    public BleConnection(Context context, BleConnectionReceiver.BleConnectionListener listener) {
        setContext(context);
        this.receiver = new BleConnectionReceiver().setConnection(this).setListener(listener);
    }

    /**
     * 设置连接参数
     *
     * @param name    蓝牙设备名称
     * @param address 蓝牙设备地址
     * @return this connection
     */
    public BleConnection setParam(String name, String address) {
        this.name = name;
        this.address = address;
        return this;
    }

    public String getAddress() {
        return address;
    }

    /**
     * Implements callback methods for GATT events that the app cares about.  For example,
     * connection change and services discovered.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status=" + status + ",newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    setConnected(true);
                    Log.d(TAG, "onConnectionStateChange: Connected to GATT server.");
                    doNotification(ACTION_GATT_CONNECTED);
                    // Attempts to discover services after successful connection.
                    boolean rst = bleGatt.discoverServices();
                    Log.d(TAG, "onConnectionStateChange: Attempting to start service discovery: " + rst);
                } else {
                    // something error
                    setConnected(false);
                    Log.d(TAG, "onConnectionStateChange: Disconnected with GATT error.");
                    doNotification(ACTION_GATT_DISCONNECTED);
                    if (bleGatt != null) {
                        Log.d(TAG, "onConnectionStateChange: Close GATT");
                        bleGatt.close();
                        bleGatt = null;
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                setConnected(false);
                Log.d(TAG, "onConnectionStateChange: Disconnected from GATT server.");
                doNotification(ACTION_GATT_DISCONNECTED);

                if (bleGatt != null) {
                    Log.d(TAG, "onConnectionStateChange: Close GATT");
                    bleGatt.close();
                    bleGatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status=" + status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            for (BluetoothGattService gattService : getSupportedGattServices()) {
                if (!gattService.getUuid().equals(SERVICE_GUID)) {
                    continue;
                }
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().equals(SPP_CHARACTERISTIC_GUID)) {
                        sppCharacteristic = gattCharacteristic;
                        setCharacteristicNotification(sppCharacteristic, true);
                    } else if (gattCharacteristic.getUuid().equals(SPP_WRITE_CHARACTERISTIC_GUID)) {
                        sppWriteCharacteristic = gattCharacteristic;
                    }
                }

                // if sppCharacteristic can be written, write to sppCharacteristic
                if (sppCharacteristic != null &&
                        (sppCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                    sppWriteCharacteristic = sppCharacteristic;
                }
                // force write with response
                sppWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            }
            if (sppCharacteristic != null) {
                bufSend.clear();
                bufSend.resetWorkingCounter();
                doNotification(ACTION_SPP_READY);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (SPP_CHARACTERISTIC_GUID.equals(characteristic.getUuid())) {
                    final byte[] buf = characteristic.getValue();
                    bufRecv.push(buf);
                    doNotification(ACTION_DATA_RECEIVED);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (SPP_CHARACTERISTIC_GUID.equals(characteristic.getUuid())) {
                final byte[] buf = characteristic.getValue();
                bufRecv.push(buf);
                doNotification(ACTION_DATA_RECEIVED);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                comboErrorCnt = 0;
                if (bufSend.isEmpty()) {
                    bufSend.setWorking(false);
                    doNotification(ACTION_DATA_SENT);
                } else {
                    // send next part
                    final byte[] bytes = bufSend.pop(20);    // maximum 20 bytes can be written to characteristic
                    writeCharacteristic(characteristic, bytes);
                }
            } else {
                comboErrorCnt++;
                Log.w(TAG, "onCharacteristicWrite: errors: " + comboErrorCnt);
                if (comboErrorCnt < COMBO_ERROR_MAX) {
                    // resend
                    gatt.writeCharacteristic(characteristic);
                } else {
                    comboErrorCnt = 0;
                    bufSend.clear();
                    doNotification(ACTION_DATA_SEND_ERROR);
                }
            }
        }
    };

    /**
     * Initializes a reference to the local Bluetooth adapter.
     */
    private void initialize() throws Exception {
        Log.d(TAG, "initialize: start");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bleManager == null) {
            bleManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bleManager == null) {
                String msg = "initialize: Unable to initialize BluetoothManager";
                Log.e(TAG, msg);
                throw new Exception(msg);
            }
        }

        bleAdapter = bleManager.getAdapter();
        if (bleAdapter == null) {
            String msg = "initialize: Unable to obtain a BluetoothAdapter";
            Log.e(TAG, msg);
            throw new Exception(msg);
        }

        getContext().registerReceiver(receiver, makeGattUpdateIntentFilter());
    }

    private void deInitialize() {
        Log.d(TAG, "deInitialize: start");
        getContext().unregisterReceiver(receiver);
    }


    boolean isClosing = false;

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     */
    @Override
    public void open() throws Exception {
        isClosing = false;

        if (bleAdapter == null) {
            Log.d(TAG, "open: Initialize");
            initialize();
        }

        if (TextUtils.isEmpty(address)) {
            String msg = "open: Address is empty";
            Log.w(TAG, msg);
            throw new Exception(msg);
        }

        // Previously connected device.  Try to reconnect.
        if (bleGatt != null) {
            Log.d(TAG, "open: Trying to use an existing mBluetoothGatt for connection.");
            if (!bleGatt.connect()) {
                String msg = "open: Unable to do connect";
                Log.w(TAG, msg);
                throw new Exception(msg);
            }
        } else {
            Log.d(TAG, "open: Trying to create a new connection.");
            bleDevice = bleAdapter.getRemoteDevice(address);
            if (bleDevice == null) {
                String msg = "open: Device not found. Unable to open.";
                Log.w(TAG, msg);
                throw new Exception(msg);
            }
            Log.d(TAG, "open: Connect new GATT");
            bleGatt = bleDevice.connectGatt(getContext(), false, gattCallback);
        }
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @Override
    public void close() {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "close: BluetoothAdapter not initialized");
            return;
        }
        try {
            isClosing = true;
            int status = getConnectionState();
            Log.d(TAG, "close: connect status=" + status);
            if (isConnected()) {
                Log.d(TAG, "close: connected, disconnect GATT first");
                bleGatt.disconnect();
            } else {
                Log.d(TAG, "close: not connected, disconnect and close GATT");
                bleGatt.disconnect();
                bleGatt.close();
                bleGatt = null;
            }
            deInitialize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public boolean isConnected() {
        return (getConnectionState() == STATE_CONNECTED);
    }

    public boolean isConnecting() {
        return (getConnectionState() == STATE_CONNECTING);
    }

    @SuppressLint("MissingPermission")
    public int getConnectionState() {
        if (bleAdapter == null || !bleAdapter.isEnabled() || bleManager == null) {
            return STATE_INVALID;
        }
        if (bleDevice == null) {
            return STATE_UNREACHABLE;
        }
        switch (bleManager.getConnectionState(bleDevice, BluetoothProfile.GATT)) {
            default:
            case BluetoothProfile.STATE_DISCONNECTED:
                return STATE_DISCONNECTED;
            case BluetoothProfile.STATE_CONNECTING:
                return STATE_CONNECTING;
            case BluetoothProfile.STATE_CONNECTED:
                return STATE_CONNECTED;
            case BluetoothProfile.STATE_DISCONNECTING:
                return STATE_DISCONNECTING;
        }
    }

    @SuppressWarnings("MissingPermission")
    public boolean canDoConnect() {
        if (bleManager == null) {
            return false;
        }
        if (bleDevice == null) {
            return true;
        }
        return bleManager.getConnectionState(bleDevice, BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bleGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] bytes) {
        if (bytes == null || characteristic == null) {
            return;
        }
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        // less than 20 bytes each time
        characteristic.setValue(bytes);
        bleGatt.writeCharacteristic(characteristic);
    }

    @Override
    public void writeBuf(byte[] buf, int offset, int count) {
        bufSend.push(buf, offset, count);
        if (!bufSend.isWorking()) {
            bufSend.setWorking(true);
            byte[] bytes = bufSend.pop(15);
            writeCharacteristic(sppWriteCharacteristic, bytes);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bleGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bleGatt == null) {
            return null;
        }
        return bleGatt.getServices();
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_SPP_READY);
        intentFilter.addAction(ACTION_DATA_RECEIVED);
        intentFilter.addAction(ACTION_DATA_SENT);
        intentFilter.addAction(ACTION_DATA_SEND_ERROR);
        return intentFilter;
    }


    private boolean canDoNotify = true;

    public boolean isCanDoNotify() {
        return canDoNotify;
    }

    public BleConnection setCanDoNotify(boolean enable) {
        canDoNotify = enable;
        return this;
    }

    protected void doNotification(String action) {
        if (receiver == null) {
            return;
        }
        switch (action) {
            case ACTION_GATT_CONNECTED:
            case ACTION_GATT_DISCONNECTED:
            case ACTION_SPP_READY: {
                receiver.onReceiverAsync(action);
                break;
            }
            case ACTION_DATA_SENT:
            case ACTION_DATA_SEND_ERROR:
            case ACTION_DATA_RECEIVED: {
                if (isCanDoNotify()) {
                    receiver.onReceiverAsync(action);
                }
                break;
            }
            default:
                // Do Nothing
        }
    }
}

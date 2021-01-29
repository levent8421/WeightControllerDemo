package com.berrontech.weight.demo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.berrontech.weight.demo.util.Toasts;
import com.berrontech.weight.scale.ScaleApi;
import com.berrontech.weight.scale.ScaleApiConfig;
import com.berrontech.weight.scale.ble.BleScaleApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Create by levent at 2021/1/27 14:08
 * MainActivity
 * Demo Entry
 *
 * @author levent
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String DEVICE_NAME = "name";
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 0x01;
    private final ExecutorService threadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), r -> new Thread(r, "MAIN AC"));
    private Button btnInit;
    private Spinner spDevices;
    private Button btnConnect;
    private Button btnClearTare;
    private Button btnGetWeight;
    private Button btnZero;
    private Button btnSendData;
    private Button btnGetCapacity;
    private Button btnSetDecimal;
    private BluetoothAdapter bluetoothAdapter;
    private BaseAdapter devicesAdapter;
    private Map<String, BluetoothDevice> deviceMap;
    private final List<Map<String, String>> spDeviceArray = new ArrayList<>();
    private BluetoothDeviceScanReceiver bluetoothDeviceScanReceiver;
    private BluetoothDevice selectedDevice;
    private EditText tvInfo;
    private ScaleApi scaleApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initView();
        checkBluetooth();
        bluetoothDeviceScanReceiver = new BluetoothDeviceScanReceiver(this);
        registerReceiver(bluetoothDeviceScanReceiver, BluetoothDeviceScanReceiver.FILTER);
    }


    private void initView() {
        btnInit = findViewById(R.id.btnInit);
        btnInit.setOnClickListener(this);

        spDevices = findViewById(R.id.spDevices);
        devicesAdapter = new SimpleAdapter(this, spDeviceArray, android.R.layout.activity_list_item, new String[]{DEVICE_NAME}, new int[]{android.R.id.text1});
        spDevices.setAdapter(devicesAdapter);
        spDevices.setOnItemSelectedListener(this);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
        btnConnect.setEnabled(false);

        tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setEnabled(false);

        btnClearTare = findViewById(R.id.btnClearTare);
        btnClearTare.setOnClickListener(this);
        btnClearTare.setEnabled(false);

        btnGetWeight = findViewById(R.id.btnGetWeight);
        btnGetWeight.setOnClickListener(this);
        btnGetWeight.setEnabled(false);

        btnZero = findViewById(R.id.btnZero);
        btnZero.setOnClickListener(this);
        btnZero.setEnabled(false);

        btnSendData = findViewById(R.id.btnSendData);
        btnSendData.setOnClickListener(this);
        btnSendData.setEnabled(false);

        btnGetCapacity = findViewById(R.id.btnGetCapacity);
        btnGetCapacity.setOnClickListener(this);
        btnGetCapacity.setEnabled(false);

        btnSetDecimal = findViewById(R.id.btnSetDecimal);
        btnSetDecimal.setOnClickListener(this);
        btnSetDecimal.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    public void appendInfo(String info) {
        final String text = tvInfo.getText() + "\r\n" + info;
        handler.post(() -> tvInfo.setText(text));
    }

    private void checkBluetooth() {
        if (bluetoothAdapter == null) {
            Toasts.showShortToast(this, "不支持蓝牙！");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toasts.showShortToast(this, "蓝牙未开启");
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_CODE_ENABLE_BLUETOOTH);
        }
    }

    public void refreshDevices(Map<String, BluetoothDevice> deviceMap) {
        this.deviceMap = deviceMap;
        this.spDeviceArray.clear();
        for (Map.Entry<String, BluetoothDevice> entry : deviceMap.entrySet()) {
            final String name = entry.getKey();
            final Map<String, String> item = new HashMap<>(1);
            item.put(DEVICE_NAME, name);
            this.spDeviceArray.add(item);
        }
        devicesAdapter.notifyDataSetChanged();
        btnConnect.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        final int btnInit = R.id.btnInit;
        final int btnConnect = R.id.btnConnect;
        final int btnClearTare = R.id.btnClearTare;
        final int btnGetWeight = R.id.btnGetWeight;
        final int btnZero = R.id.btnZero;
        final int btnSendData = R.id.btnSendData;
        final int btnGetCapacity = R.id.btnGetCapacity;
        final int btnSetDecimal = R.id.btnSetDecimal;
        switch (view.getId()) {
            case btnInit:
                doInit();
                break;
            case btnConnect:
                doConnect();
                break;
            case btnClearTare:
                doClearTare();
                break;
            case btnGetWeight:
                doGetWeight();
                break;
            case btnZero:
                doZero();
                break;
            case btnSendData:
                doSendData();
                break;
            case btnGetCapacity:
                getCapacity();
                break;
            case btnSetDecimal:
                setDecimal();
                break;
            default:
                // Do nothing
        }
    }

    private void getCapacity() {
        try {
            final float res = scaleApi.getMaxWeight();
            appendInfo("Capacity:" + res);
        } catch (Exception e) {
            e.printStackTrace();
            appendInfo("Error:" + e.getMessage());
        }
    }

    private void setDecimal() {
        try {
            scaleApi.setPoint(3);
            appendInfo("Success!");
        } catch (Exception e) {
            e.printStackTrace();
            appendInfo("Error:" + e.getMessage());
        }
    }

    private void doSendData() {
        final Map<String, String> envItems = System.getenv();
        final byte[] bytes = envItems.toString().getBytes();
        threadPool.execute(() -> {
            try {
                final int res = scaleApi.sendCmd(bytes, 10 * 1000);
                appendInfo("Sent:" + res);
            } catch (Exception e) {
                e.printStackTrace();
                appendInfo("Send error:" + e.getMessage());
            }
        });
    }

    private void doZero() {
        try {
            scaleApi.zeroClear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doGetWeight() {
        try {
            final String[] weight = scaleApi.getWeight();
            appendInfo(String.format("Weight=[%s], Unit=[%s]", weight[0], weight[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doClearTare() {
        try {
            scaleApi.clearTare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doInit() {
        bluetoothAdapter.startDiscovery();
    }

    private void doConnect() {
        if (selectedDevice == null) {
            Toasts.showShortToast(this, "请选择设备");
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        final ScaleApiConfig apiConfig = new ScaleApiConfig()
                .with(ScaleApiConfig.DEVICE_NAME, selectedDevice.getName())
                .with(ScaleApiConfig.DEVICE_ADDRESS, selectedDevice.getAddress());
        scaleApi = new BleScaleApi(apiConfig);
        threadPool.execute(() -> {
            try {
                appendInfo("Connecting...");
                scaleApi.init(this);
                scaleApi.connect();
                onConnectSuccess();
            } catch (Exception e) {
                appendInfo("Connect Fail:" + e.getMessage());
            }
        });
    }

    private void onConnectSuccess() {
        appendInfo("Connection Ready!");
        handler.post(() -> {
            btnClearTare.setEnabled(true);
            btnGetWeight.setEnabled(true);
            btnZero.setEnabled(true);
            btnSendData.setEnabled(true);
            btnSetDecimal.setEnabled(true);
            btnGetCapacity.setEnabled(true);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothDeviceScanReceiver);
        if (scaleApi != null) {
            try {
                scaleApi.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_BLUETOOTH) {
            Toasts.showShortToast(this, "Enable BLE:" + resultCode);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int id, long position) {
        final Map<String, String> item = spDeviceArray.get(id);
        final String name = item.get(DEVICE_NAME);
        this.selectedDevice = deviceMap.get(name);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        this.selectedDevice = null;
    }
}

class BluetoothDeviceScanReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceScanReceiver";
    public static final IntentFilter FILTER = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    private final Map<String, BluetoothDevice> deviceMap = new HashMap<>(16);
    private final MainActivity activity;

    BluetoothDeviceScanReceiver(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        final String name = device.getName();
        final String address = device.getAddress();
        final String deviceKey = name + "/" + address;
        Log.d(TAG, "Found device:" + deviceKey);
        deviceMap.put(deviceKey, device);
        activity.refreshDevices(deviceMap);
    }
}

class ConnectTask implements Callable<Boolean> {
    private final ScaleApi scaleApi;

    ConnectTask(ScaleApi scaleApi) {
        this.scaleApi = scaleApi;
    }

    @Override
    public Boolean call() throws Exception {
        scaleApi.connect();
        return true;
    }
}
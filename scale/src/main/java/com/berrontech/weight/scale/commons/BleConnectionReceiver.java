package com.berrontech.weight.scale.commons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Create by Lastnika 2021/1/27 19:27
 * BleConnectionReceiver
 * BLE Data Receiver
 *
 * @author Lastnika
 */
public class BleConnectionReceiver extends BroadcastReceiver implements ThreadFactory {
    private BleConnection connection;
    private BleConnectionListener listener;
    private final ExecutorService threadPool = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), this);

    public BleConnection getConnection() {
        return connection;
    }

    public BleConnectionReceiver setConnection(BleConnection connection) {
        this.connection = connection;
        return this;
    }

    public BleConnectionListener getListener() {
        return listener;
    }

    public BleConnectionReceiver setListener(BleConnectionListener listener) {
        this.listener = listener;
        return this;
    }

    public void onReceiverAsync(String action) {
        if (listener != null) {
            threadPool.execute(() -> {
                switch (action) {
                    case BleConnection.ACTION_GATT_CONNECTED: {
                        listener.onConnected(connection);
                        break;
                    }
                    case BleConnection.ACTION_SPP_READY: {
                        listener.onSppReady(connection);
                        break;
                    }
                    case BleConnection.ACTION_DATA_SENT: {
                        listener.onDataSent(connection);
                        break;
                    }
                    case BleConnection.ACTION_DATA_SEND_ERROR: {
                        listener.onDataSentError(connection);
                        break;
                    }
                    case BleConnection.ACTION_DATA_RECEIVED: {
                        listener.onDataReceived(connection);
                        break;
                    }
                    case BleConnection.ACTION_GATT_DISCONNECTED: {
                        listener.onDisconnected(connection);
                        break;
                    }
                    default:
                        // Do nothing
                }
            });
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (listener != null) {
            switch (action) {
                case BleConnection.ACTION_GATT_CONNECTED: {
                    listener.onConnected(connection);
                    break;
                }
                case BleConnection.ACTION_SPP_READY: {
                    listener.onSppReady(connection);
                    break;
                }
                case BleConnection.ACTION_DATA_SENT: {
                    listener.onDataSent(connection);
                    break;
                }
                case BleConnection.ACTION_DATA_SEND_ERROR: {
                    listener.onDataSentError(connection);
                    break;
                }
                case BleConnection.ACTION_DATA_RECEIVED: {
                    listener.onDataReceived(connection);
                    break;
                }
                case BleConnection.ACTION_GATT_DISCONNECTED: {
                    listener.onDisconnected(connection);
                    break;
                }
                default:
                    // Do nothing
            }
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "BLEReceiver");
    }

    public interface BleConnectionListener {
        /**
         * Call on connected
         *
         * @param connection connection
         */
        void onConnected(BleConnection connection);

        /**
         * Call on SPP Ready
         *
         * @param connection connection
         */
        void onSppReady(BleConnection connection);

        /**
         * Call on SPP Ready
         *
         * @param connection connection
         */
        void onDataSent(BleConnection connection);

        /**
         * Call on Data Sent Error
         *
         * @param connection connection
         */
        void onDataSentError(BleConnection connection);

        /**
         * Call on Data Received
         *
         * @param connection connection
         */
        void onDataReceived(BleConnection connection);

        /**
         * Call on Disconnected
         *
         * @param connection connection
         */
        void onDisconnected(BleConnection connection);
    }
}



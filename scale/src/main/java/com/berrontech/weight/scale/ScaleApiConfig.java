package com.berrontech.weight.scale;

import com.berrontech.weight.scale.ble.BleCommandMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Create by levent8421 2021/1/27 17:01
 * ScaleApiConfig
 * Scale Api Config
 *
 * @author levent8421
 */
public class ScaleApiConfig {
    public static final String AUTO_CONNECT = "auto_connect";
    public static final String CONNECT_TIMEOUT = "connect_timeout";
    public static final String CMD_TIMEOUT = "cmd_timeout";
    /**
     * Required BLE device name
     */
    public static final String DEVICE_NAME = "device_name";

    public static final String DEVICE_ADDRESS = "device_address";

    private final Map<String, Object> config = new HashMap<>(16);

    public ScaleApiConfig() {
        loadDefaults();
    }

    private void loadDefaults() {
        with(AUTO_CONNECT, Boolean.FALSE)
                .with(CONNECT_TIMEOUT, BleCommandMetadata.CONNECT_TIMEOUT)
                .with(CMD_TIMEOUT, BleCommandMetadata.RESPONSE_TIMEOUT);
    }

    public ScaleApiConfig with(String name, Object value) {
        config.put(name, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> klass) {
        final Object value = config.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Can not find config for name [" + name + "]");
        }
        if (!klass.isInstance(value)) {
            throw new IllegalArgumentException("Can not convert [" + value.getClass().getName() + "] to [" + klass.getName() + "]");
        }
        return (T) value;
    }
}

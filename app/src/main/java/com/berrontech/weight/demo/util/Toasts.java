package com.berrontech.weight.demo.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Create by levent8421 2021/1/27 14:26
 * Toasts
 * Toast Utils
 *
 * @author levent8421
 */
public class Toasts {
    private static final Handler MAIN_LOOPER_HANDLER = new Handler(Looper.getMainLooper());

    public static void showShortToast(Context context, String msg) {
        MAIN_LOOPER_HANDLER.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}

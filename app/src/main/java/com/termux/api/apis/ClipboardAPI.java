package com.termux.api.apis;

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

public class ClipboardAPI {

    private static final String LOG_TAG = "ClipboardAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        boolean version2 = "2".equals(intent.getStringExtra("api_version"));
        if (version2) {
            boolean set = intent.getBooleanExtra("set", false);
            if (set) {
                ResultReturner.returnData(apiReceiver, intent, new ResultReturner.WithStringInput() {
                    @Override
                    protected boolean trimInput() {
                        return false;
                    }

                    @Override
                    public void writeResult(PrintWriter out) {
                        setPrimaryClipOnMainThread(clipboard, inputString);
                    }
                });
            } else {
                ResultReturner.returnData(apiReceiver, intent, out -> writePrimaryClipText(context, clipboard, out));
            }
        } else {
            final String newClipText = intent.getStringExtra("text");
            if (newClipText != null) {
                // Set clip without reading the current clip first.
                setPrimaryClipOnMainThread(clipboard, newClipText);
            }

            ResultReturner.returnData(apiReceiver, intent, out -> {
                if (newClipText == null) {
                    writePrimaryClipText(context, clipboard, out);
                }
            });
        }
    }

    private static void setPrimaryClipOnMainThread(final ClipboardManager clipboard, final String text) {
        if (clipboard == null || text == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", text));
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText("", text));
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void writePrimaryClipText(final Context context, final ClipboardManager clipboard, final PrintWriter out) {
        final ClipData clipData = clipboard != null ? clipboard.getPrimaryClip() : null;
        if (clipData == null) {
            out.print("");
            return;
        }

        int itemCount = clipData.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            Item item = clipData.getItemAt(i);
            CharSequence text = item.coerceToText(context);
            if (!TextUtils.isEmpty(text)) {
                out.print(text);
            }
        }
    }

}

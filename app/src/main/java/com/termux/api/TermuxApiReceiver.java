package com.termux.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.termux.api.apis.ClipboardAPI;
import com.termux.api.util.ResultReturner;
import com.termux.app.TermuxApplication;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.markdown.MarkdownUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.plugins.TermuxPluginUtils;

public class TermuxApiReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TermuxApiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        TermuxApplication.setLogConfig(context);
        ResultReturner.setContext(context);
        Logger.logDebug(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));

        try {
            doWork(context, intent);
        } catch (Throwable t) {
            String header = "Error in " + LOG_TAG + " for " + ResultReturner.getApiMethodLabel(intent) +
                    " sent by API server " + ResultReturner.getApiServerLabel(intent);
            String message = header + ":\n\n" +
                IntentUtils.getIntentString(intent) + "\n\n" +
                "Error";

            // Make sure never to throw exception from BroadCastReceiver to avoid "process is bad"
            // behaviour from the Android system.
            Logger.logStackTraceWithMessage(LOG_TAG, message, t);

            // Only add header to notification text.
            TermuxPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
                TermuxConstants.TERMUX_APP_NAME + " Error", header,
                MarkdownUtils.getMarkdownCodeForString(Logger.getMessageAndStackTraceString(message, t), true),
                false, false, true);

            ResultReturner.noteDone(this, intent);
        }
    }

    private void doWork(Context context, Intent intent) {
        String apiMethod = intent.getStringExtra("api_method");
        if (apiMethod == null) {
            Logger.logError(LOG_TAG, "Missing 'api_method' extra");
            return;
        }

        switch (apiMethod) {
            case "Clipboard":
                ClipboardAPI.onReceive(this, context, intent);
                break;
            default:
                Logger.logError(LOG_TAG, "Unrecognized 'api_method' extra: '" + apiMethod + "'");
        }
    }

}

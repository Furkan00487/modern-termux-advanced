package com.termux.x11;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.InputDevice;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.termux.x11.utils.KeyInterceptor;
import com.termux.x11.utils.SamsungDexUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bridge utilities exposing X11 preference metadata, permission checks, display resolution,
 * and broadcast notifications cleanly for the Compose Settings Hub in {@code :app}.
 */
public final class X11PreferenceBridge {

    public static final String ACTION_OPEN_SETTINGS = "com.termux.app.action.OPEN_SETTINGS";
    public static final String X11_SETTINGS_ALIAS_CLASS = "com.termux.app.activities.X11SettingsActivity";
    public static final String EXTRA_INITIAL_SECTION = "extra_initial_section";
    public static final String EXTRA_INITIAL_SUBSCREEN = "extra_initial_subscreen";
    public static final String EXTRA_DISPLAY_ID = "com.termux.x11.EXTRA_DISPLAY_ID";
    public static final String SECTION_TERMUX = "termux";
    public static final String SECTION_X11 = "x11";

    private X11PreferenceBridge() {}

    @NonNull
    public static Intent createX11SettingsIntent(@NonNull Context context, @Nullable String initialSubscreen, int displayId) {
        Intent target = new Intent(ACTION_OPEN_SETTINGS);
        target.setClassName(context.getPackageName(), X11_SETTINGS_ALIAS_CLASS);
        target.putExtra(EXTRA_INITIAL_SECTION, SECTION_X11);
        target.putExtra(EXTRA_DISPLAY_ID, displayId);
        if (initialSubscreen != null && !initialSubscreen.isEmpty()) {
            target.putExtra(EXTRA_INITIAL_SUBSCREEN, initialSubscreen);
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return target;
    }

    @Nullable
    public static Bundle createLaunchDisplayOptions(int displayId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId > Display.DEFAULT_DISPLAY) {
            try {
                return ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    public static int resolveDisplayId(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Display d = context.getDisplay();
                if (d != null) return d.getDisplayId();
            } catch (UnsupportedOperationException ignored) {
                // Non-display Context falls through to WindowManager lookup.
            }
        }
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                return wm.getDefaultDisplay().getDisplayId();
            }
        } catch (Throwable ignored) {
            // Robolectric or service contexts without WindowManager fall back to default display.
        }
        return Display.DEFAULT_DISPLAY;
    }

    @Nullable
    public static Prefs resolvePrefs(@NonNull Context context, int displayId) {
        Context app = context.getApplicationContext();
        if (!(app instanceof LorieApp lorieApp)) {
            return null;
        }
        if (displayId > Display.DEFAULT_DISPLAY) {
            return Boolean.TRUE.equals(lorieApp.builtInPrefs.storeSecondaryDisplayPreferencesSeparately.get())
                ? lorieApp.secondaryPrefs
                : lorieApp.builtInPrefs;
        }
        return lorieApp.getPrefs(context);
    }

    @NonNull
    public static Context resolveLocalizedContext(@NonNull Context context, int displayId) {
        if (displayId > Display.DEFAULT_DISPLAY) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display target = dm != null ? dm.getDisplay(displayId) : null;
            if (target != null) {
                return context.createDisplayContext(target);
            }
        }
        return context;
    }

    public static void notifyPreferenceChanged(@NonNull Context context, @NonNull String key) {
        Intent intent = new Intent(LoriePreferences.ACTION_PREFERENCES_CHANGED);
        intent.putExtra("key", key);
        intent.putExtra("fromBroadcast", true);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public static long readSystemScreenOffTimeoutMs(@NonNull Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0);
    }

    @NonNull
    public static String formatTimeout(@NonNull Context context, long timeoutMs) {
        if (timeoutMs < 60_000L) {
            return context.getString(R.string.lorie_timeout_seconds, (int) (timeoutMs / 1_000L));
        }
        return context.getString(R.string.lorie_timeout_minutes, (int) (timeoutMs / 60_000L));
    }

    public static boolean hasWriteSecureSettings(@NonNull Context context) {
        return context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PERMISSION_GRANTED;
    }

    public static boolean hasPostNotifications(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED;
    }

    public static boolean hasStylusInputDevice() {
        return Arrays.stream(InputDevice.getDeviceIds())
            .mapToObj(InputDevice::getDevice)
            .filter(Objects::nonNull)
            .anyMatch(d -> d.supportsSource(InputDevice.SOURCE_STYLUS));
    }

    public static boolean isDexAvailable() {
        return SamsungDexUtils.available();
    }

    public static boolean isKeyInterceptorLaunched() {
        return KeyInterceptor.isLaunched();
    }

    public static void shutdownKeyInterceptor(boolean force) {
        KeyInterceptor.shutdown(force);
    }

    public static boolean isValidCustomResolution(@Nullable String value) {
        if (value == null) return false;
        String[] parts = value.trim().split("x", -1);
        if (parts.length != 2) return false;
        try {
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            return width > 0 && height > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}

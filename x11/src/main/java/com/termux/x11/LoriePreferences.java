package com.termux.x11;

import static android.Manifest.permission.WRITE_SECURE_SETTINGS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.system.Os.getuid;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import com.termux.x11.utils.KeyInterceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 * Compatibility trampoline Activity forwarding X11 preference launches to the unified
 * Compose {@code X11SettingsActivity} alias while preserving task affinity, display ID,
 * {@link PrefsProto}, and {@link Receiver} for CLI and external callers.
 */
@SuppressWarnings("deprecation")
public class LoriePreferences extends ComponentActivity {
    public static final String ACTION_PREFERENCES_CHANGED = "com.termux.x11.ACTION_PREFERENCES_CHANGED";
    public static final String HW_ACCEL_FLAG_PATH = "/data/data/com.termux/files/usr/tmp/.lorie_hwaccel";

    public static void syncHardwareAccelerationFlag(boolean enabled) {
        try {
            java.io.File flag = new java.io.File(HW_ACCEL_FLAG_PATH);
            java.io.File parent = flag.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(flag)) {
                fos.write((enabled ? "1\n" : "0\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int displayId = X11PreferenceBridge.resolveDisplayId(this);
        String subscreen = getIntent() != null
                ? getIntent().getStringExtra(X11PreferenceBridge.EXTRA_INITIAL_SUBSCREEN)
                : null;
        Intent target = X11PreferenceBridge.createX11SettingsIntent(this, subscreen, displayId);
        Bundle options = X11PreferenceBridge.createLaunchDisplayOptions(displayId);
        startActivity(target, options);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    public void onClick(@SuppressWarnings("unused") View view) {
        int displayId = X11PreferenceBridge.resolveDisplayId(this);
        Intent target = X11PreferenceBridge.createX11SettingsIntent(this, "ekbar", displayId);
        startActivity(target, X11PreferenceBridge.createLaunchDisplayOptions(displayId));
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    public static class Receiver extends BroadcastReceiver {
        public Receiver() {
            super();
        }

        @Override
        public IBinder peekService(Context myContext, Intent service) {
            return super.peekService(myContext, service);
        }

        /** @noinspection StringConcatenationInLoop*/
        @SuppressLint("ApplySharedPref")
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent != null ? intent.getBundleExtra(null) : null;
            IBinder ibinder = bundle != null ? bundle.getBinder(null) : null;
            IRemoteCmdImterface remote = ibinder != null ? IRemoteCmdImterface.Stub.asInterface(ibinder) : null;

            try {
                if (intent != null && intent.getExtras() != null) {
                    Context app = context.getApplicationContext();
                    Prefs p;
                    if (app instanceof LorieApp lorieApp) {
                        MainActivity activity = MainActivity.getFocusedOrAnyInstance();
                        p = lorieApp.getPrefs(activity != null ? activity : context);
                    } else {
                        p = new Prefs();
                        p.attach(context, context.getSharedPreferences(LorieApp.X11_PREFS_NAME, Context.MODE_PRIVATE));
                    }

                    if (intent.getStringExtra("list") != null) {
                        StringBuilder result = new StringBuilder();
                        for (PrefsProto.Preference pref : p.keys.values()) {
                            if (pref.type == String.class)
                                result.append('"').append(pref.key).append("\"=\"").append(pref.asString().get()).append("\"\n");
                            else if (pref.type == int.class)
                                result.append('"').append(pref.key).append("\"=\"").append(pref.asInt().get()).append("\"\n");
                            else if (pref.type == boolean.class)
                                result.append('"').append(pref.key).append("\"=\"").append(pref.asBoolean().get()).append("\"\n");
                            else if (pref.type == String[].class) {
                                String[] entries = pref.asList().getEntries();
                                String[] values = pref.asList().getValues();
                                String value = pref.asList().get();
                                int index = Arrays.asList(values).indexOf(value);
                                if (index != -1)
                                    value = entries[index];
                                result.append('"').append(pref.key).append("\"=\"").append(value).append("\"\n");
                            }
                        }

                        String out = result.length() > 0 ? result.substring(0, result.length() - 1) : "";
                        sendResponse(remote, 0, 2, out);
                        return;
                    }

                    SharedPreferences.Editor edit = p.get().edit();
                    for (String key : intent.getExtras().keySet()) {
                        if (key == null)
                            continue;
                        String newValue = intent.getStringExtra(key);
                        if (newValue == null)
                            continue;

                        switch (key) {
                            case "displayResolutionCustom": {
                                if (!X11PreferenceBridge.isValidCustomResolution(newValue)) {
                                    sendResponse(remote, 1, 1, "displayResolutionCustom: " + context.getString(R.string.lorie_invalid_resolution_format));
                                    return;
                                }

                                edit.putString("displayResolutionCustom", newValue.trim());
                                break;
                            }
                            case "enableAccessibilityServiceAutomatically": {
                                if (!"true".equals(newValue))
                                    KeyInterceptor.shutdown(false);
                                else if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PERMISSION_GRANTED) {
                                    sendResponse(remote, 1, 1, context.getString(R.string.lorie_permission_denied_title) + "\n" +
                                            context.getString(R.string.lorie_write_secure_settings_message) + "\n" +
                                            "adb shell pm grant " + context.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS");
                                    return;
                                }

                                edit.putBoolean("enableAccessibilityServiceAutomatically", "true".contentEquals(newValue));
                                break;
                            }
                            case "extra_keys_config": {
                                edit.putString(key, newValue);
                                break;
                            }
                            case "storeSecondaryDisplayPreferencesSeparately": {
                                // Always canonical, regardless of which display targetCtx resolved to.
                                if (app instanceof LorieApp lorieApp) {
                                    lorieApp.builtInPrefs.preferences.edit()
                                            .putBoolean(key, "true".contentEquals(newValue)).commit();
                                } else {
                                    edit.putBoolean(key, "true".contentEquals(newValue));
                                }
                                break;
                            }
                            default: {
                                PrefsProto.Preference pref = p.keys.get(key);
                                if (pref != null && pref.type == boolean.class) {
                                    boolean val = "true".contentEquals(newValue);
                                    edit.putBoolean(key, val);
                                    if ("hardwareAcceleration".equals(key))
                                        syncHardwareAccelerationFlag(val);
                                    if ("showAdditionalKbd".contentEquals(key) && val)
                                        edit.putBoolean("additionalKbdVisible", true);
                                } else if (pref != null && pref.type == int.class) {
                                    try {
                                        edit.putInt(key, Integer.parseInt(newValue));
                                    } catch (NumberFormatException | PatternSyntaxException exception) {
                                        sendResponse(remote, 1, 4, key + ": " + context.getString(R.string.lorie_failed_parse_int, exception.toString()));
                                        return;
                                    }
                                } else if (pref != null && pref.type == String[].class) {
                                    PrefsProto.ListPreference _p = (PrefsProto.ListPreference) pref;
                                    String[] entries = _p.getEntries();
                                    String[] values = _p.getValues();
                                    int index = Arrays.asList(entries).indexOf(newValue);

                                    if (index == -1 && !Arrays.equals(entries, values))
                                        index = Arrays.asList(values).indexOf(newValue);

                                    if (index != -1) {
                                        edit.putString(key, values[index]);
                                        break;
                                    }

                                    sendResponse(remote, 1, 1, key + ": " + context.getString(R.string.lorie_invalid_list_value,
                                            newValue, Arrays.toString(entries) + (!Arrays.equals(entries, values) ? " / " + Arrays.toString(values) : "")));
                                    return;
                                } else {
                                    sendResponse(remote, 1, 4, key + ": " + context.getString(R.string.lorie_unrecognized_option));
                                    return;
                                }
                            }
                        }

                        X11PreferenceBridge.notifyPreferenceChanged(context, key);
                    }
                    edit.commit();
                }

                sendResponse(remote, 0, 2, context.getString(R.string.lorie_done));
            } catch (Exception e) {
                sendResponse(remote, 1, 4, e.toString());
            }
        }

        void sendResponse(IRemoteCmdImterface remote, int status, int oldStatus, String text) {
            if (remote != null) {
                try {
                    remote.exit(status, text);
                } catch (RemoteException ex) {
                    Log.e("LoriePreferences", "Failed to send response to commandline proxy", ex);
                }
            } else if (isOrderedBroadcast()) {
                setResultCode(oldStatus);
                setResultData(text);
            }
        }

        // For changing preferences from commandline
        private static final IBinder iface = new IRemoteCmdImterface.Stub() {
            @Override
            public void exit(int code, String output) {
                System.out.println(output);
                CmdEntryPoint.handler.post(() -> System.exit(code));
            }
        };

        private static void help() {
            System.err.print("termux-x11-preference [list] {key:value} [{key2:value2}]...");
            System.exit(0);
        }

        @Keep
        @SuppressLint("WrongConstant")
        public static void main(String[] args) {
            android.util.Log.i("LoriePreferences$Receiver", "commit " + BuildConfig.COMMIT);
            //noinspection resource
            ParcelFileDescriptor in = ParcelFileDescriptor.adoptFd(0);
            Intent i = new Intent("com.termux.x11.CHANGE_PREFERENCE");
            Bundle bundle = new Bundle();
            boolean inputIsFile = !android.system.Os.isatty(in.getFileDescriptor());

            in.detachFd();
            bundle.putBinder(null, iface);
            i.putExtra(null, bundle);
            i.setPackage("com.termux");

            if (getuid() == 0 || getuid() == 2000)
                i.setFlags(0x00400000 /* FLAG_RECEIVER_FROM_SHELL */);

            if (args.length == 0 && !inputIsFile)
                help();

            if (inputIsFile) {
                Scanner scanner = new Scanner(System.in);
                String line;
                String[] v;
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if (!line.contains("="))
                        help();

                    v = line.split("=");
                    if (v[0].startsWith("\"") && v[0].endsWith("\""))
                        v[0] = v[0].substring(1, v[0].length() - 1);
                    if (v[1].startsWith("\"") && v[1].endsWith("\""))
                        v[1] = v[1].substring(1, v[1].length() - 1);
                    i.putExtra(v[0], v[1]);
                }
            }

            for (String a: args) {
                if ("list".equals(a)) {
                    i.putExtra("list", "");
                } else if (a != null && a.contains(":")) {
                    String[] v = a.split(":");
                    i.putExtra(v[0], v[1]);
                } else
                    help();
            }

            CmdEntryPoint.handler.post(() -> CmdEntryPoint.sendBroadcast(i));
            CmdEntryPoint.handler.postDelayed(() -> {
                System.err.println("Did not receive any response from app.");
                System.exit(1);
            }, 5000);
            Looper.loop();
        }
    }

    static Handler handler = Looper.getMainLooper() != null ? new Handler(Looper.getMainLooper()) : null;

    /** @noinspection unused*/
    @SuppressLint("ApplySharedPref")
    public static class PrefsProto extends PreferenceDataStore {
        public static class Preference {
            protected final String key;
            protected final Class<?> type;
            protected final Object defValue;
            protected Preference(String key, Class<?> class_, Object default_) {
                this.key = key;
                this.type = class_;
                this.defValue = default_;
            }

            public ListPreference asList() {
                return (ListPreference) this;
            }

            public StringPreference asString() {
                return (StringPreference) this;
            }

            public IntPreference asInt() {
                return (IntPreference) this;
            }

            public BooleanPreference asBoolean() {
                return (BooleanPreference) this;
            }
        }

        public class BooleanPreference extends Preference {
            public BooleanPreference(String key, boolean defValue) {
                super(key, boolean.class, defValue);
            }

            public boolean get() {
                checkAttached();
                if (isSharedAcrossDisplays(key) && ctx.getApplicationContext() instanceof LorieApp app && app.builtInPrefs.preferences != null)
                    return app.builtInPrefs.preferences.getBoolean(key, (boolean) defValue);
                return preferences.getBoolean(key, (boolean) defValue);
            }

            public void put(boolean v) {
                checkAttached();
                if (isSharedAcrossDisplays(key) && ctx.getApplicationContext() instanceof LorieApp app && app.builtInPrefs.preferences != null)
                    app.builtInPrefs.preferences.edit().putBoolean(key, v).commit();
                preferences.edit().putBoolean(key, v).commit();
                if ("hardwareAcceleration".equals(key))
                    syncHardwareAccelerationFlag(v);
            }
        }

        public class IntPreference extends Preference {
            public IntPreference(String key, int defValue) {
                super(key, int.class, defValue);
            }

            public int get() {
                checkAttached();
                return preferences.getInt(key, (int) defValue);
            }

            public int defValue() {
                checkAttached();
                return preferences.getInt(key, (int) defValue);
            }

            public void put(int v) {
                checkAttached();
                preferences.edit().putInt(key, v).commit();
            }
        }

        public class StringPreference extends Preference {
            public StringPreference(String key, String defValue) {
                super(key, String.class, defValue);
            }

            public String get() {
                checkAttached();
                return preferences.getString(key, (String) defValue);
            }

            public void put(String v) {
                checkAttached();
                preferences.edit().putString(key, v).commit();
            }
        }

        public class ListPreference extends Preference {
            private final int entries, values;

            public ListPreference(String key, String defValue, int entries, int values) {
                super(key, String[].class, defValue);
                this.entries = entries;
                this.values = values;
            }

            public String get() {
                checkAttached();
                return preferences.getString(key, (String) defValue);
            }

            public void put(String v) {
                checkAttached();
                preferences.edit().putString(key, v).commit();
            }

            public String[] getEntries() {
                checkAttached();
                return getArrayItems(entries, ctx.getResources());
            }

            public String[] getEntries(Resources resources) {
                checkAttached();
                return getArrayItems(entries, resources);
            }

            public String[] getValues() {
                checkAttached();
                return getArrayItems(values, ctx.getResources());
            }

            public String[] getValues(Resources resources) {
                checkAttached();
                return getArrayItems(values, resources);
            }

            private String[] getArrayItems(int resourceId, Resources resources) {
                ArrayList<String> itemList = new ArrayList<>();
                TypedArray typedArray = resources.obtainTypedArray(resourceId);
                try {
                    for (int i = 0; i < typedArray.length(); i++) {
                        int type = typedArray.getType(i);
                        if (type == TypedValue.TYPE_STRING) {
                            itemList.add(typedArray.getString(i));
                        } else if (type == TypedValue.TYPE_REFERENCE) {
                            int resIdOfArray = typedArray.getResourceId(i, 0);
                            itemList.addAll(Arrays.asList(resources.getStringArray(resIdOfArray)));
                        }
                    }
                } finally {
                    typedArray.recycle();
                }

                Object[] objectArray = itemList.toArray();
                return Arrays.copyOf(objectArray, objectArray.length, String[].class);
            }
        }

        protected Context ctx;
        protected SharedPreferences preferences;

        protected PrefsProto() {}

        private void checkAttached() {
            if (preferences == null)
                throw new NullPointerException("Prefs used before attach() was called");
        }

        /** Settings that must have a single value app-wide, regardless of which store is otherwise current. */
        private static boolean isSharedAcrossDisplays(String key) {
            return "storeSecondaryDisplayPreferencesSeparately".contentEquals(key)
                    || "enableAccessibilityServiceAutomatically".contentEquals(key);
        }

        protected void attach(Context ctx, SharedPreferences preferences) {
            this.ctx = ctx;
            this.preferences = preferences;
        }

        @Override public void putBoolean(String k, boolean v) {
            checkAttached();
            if (isSharedAcrossDisplays(k) && ctx.getApplicationContext() instanceof LorieApp app && app.builtInPrefs.preferences != null)
                app.builtInPrefs.preferences.edit().putBoolean(k, v).commit();
            else
                preferences.edit().putBoolean(k, v).commit();
            if ("hardwareAcceleration".equals(k))
                syncHardwareAccelerationFlag(v);
        }
        @Override public boolean getBoolean(String k, boolean d) {
            checkAttached();
            if (isSharedAcrossDisplays(k) && ctx.getApplicationContext() instanceof LorieApp app && app.builtInPrefs.preferences != null)
                return app.builtInPrefs.preferences.getBoolean(k, d);
            return preferences.getBoolean(k, d);
        }
        @Override public void putString(String k, @Nullable String v) { get().edit().putString(k, v).commit(); }
        @Override public void putStringSet(String k, @Nullable Set<String> v) { get().edit().putStringSet(k, v).commit(); }
        @Override public void putInt(String k, int v) { get().edit().putInt(k, v).commit(); }
        @Override public void putLong(String k, long v) { get().edit().putLong(k, v).commit(); }
        @Override public void putFloat(String k, float v) { get().edit().putFloat(k, v).commit(); }
        @Nullable @Override public String getString(String k, @Nullable String d) { return get().getString(k, d); }
        @Nullable @Override public Set<String> getStringSet(String k, @Nullable Set<String> ds) { return get().getStringSet(k, ds); }
        @Override public int getInt(String k, int d) { return get().getInt(k, d); }
        @Override public long getLong(String k, long d) { return get().getLong(k, d); }
        @Override public float getFloat(String k, float d) { return get().getFloat(k, d); }

        public SharedPreferences get() {
            checkAttached();
            return preferences;
        }
    }
}

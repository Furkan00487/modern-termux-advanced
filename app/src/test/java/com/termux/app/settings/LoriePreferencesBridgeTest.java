package com.termux.app.settings;

import android.content.Context;
import android.content.Intent;

import com.termux.x11.LorieApp;
import com.termux.x11.LoriePreferences;
import com.termux.x11.Prefs;
import com.termux.x11.X11PreferenceBridge;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LoriePreferencesBridgeTest {

    @Test
    public void testLoriePreferencesTrampolineTargetsX11AliasAndFinishes() {
        LoriePreferences activity = Robolectric.buildActivity(LoriePreferences.class).create().get();
        Intent next = Shadows.shadowOf(activity).getNextStartedActivity();

        assertNotNull("Expected trampoline launch intent", next);
        assertEquals(X11PreferenceBridge.ACTION_OPEN_SETTINGS, next.getAction());
        assertNotNull(next.getComponent());
        assertEquals(X11PreferenceBridge.X11_SETTINGS_ALIAS_CLASS, next.getComponent().getClassName());
        assertEquals(X11PreferenceBridge.SECTION_X11, next.getStringExtra(X11PreferenceBridge.EXTRA_INITIAL_SECTION));
        assertTrue("Trampoline should finish immediately", activity.isFinishing());
    }

    @Test
    public void testLoriePreferencesForwardsSubscreenExtra() {
        Context app = RuntimeEnvironment.getApplication();
        Intent launch = new Intent(app, LoriePreferences.class);
        launch.putExtra(X11PreferenceBridge.EXTRA_INITIAL_SUBSCREEN, "ekbar");

        LoriePreferences activity = Robolectric.buildActivity(LoriePreferences.class, launch).create().get();
        Intent next = Shadows.shadowOf(activity).getNextStartedActivity();

        assertNotNull(next);
        assertEquals("ekbar", next.getStringExtra(X11PreferenceBridge.EXTRA_INITIAL_SUBSCREEN));
    }

    @Test
    @Config(qualifiers = "tr")
    public void testReceiverRemainsLocaleInvariantInTurkish() {
        verifyReceiverSetAndList();
    }

    @Test
    @Config(qualifiers = "en")
    public void testReceiverRemainsLocaleInvariantInEnglish() {
        verifyReceiverSetAndList();
    }

    private void verifyReceiverSetAndList() {
        Context context = RuntimeEnvironment.getApplication();
        LoriePreferences.Receiver receiver = new LoriePreferences.Receiver();

        Intent setIntent = new Intent("com.termux.x11.CHANGE_PREFERENCE");
        setIntent.putExtra("displayResolutionMode", "scaled");
        setIntent.putExtra("displayScale", "140");
        receiver.onReceive(context, setIntent);

        Prefs prefs = new Prefs();
        prefs.attach(context, context.getSharedPreferences(LorieApp.X11_PREFS_NAME, Context.MODE_PRIVATE));
        assertEquals("scaled", prefs.displayResolutionMode.get());
        assertEquals(140, prefs.displayScale.get());
    }

    @Test
    public void testCustomResolutionValidation() {
        assertTrue(X11PreferenceBridge.isValidCustomResolution("1920x1080"));
        assertTrue(X11PreferenceBridge.isValidCustomResolution("1280x720"));
        assertFalse(X11PreferenceBridge.isValidCustomResolution(null));
        assertFalse(X11PreferenceBridge.isValidCustomResolution(""));
        assertFalse(X11PreferenceBridge.isValidCustomResolution("1920"));
        assertFalse(X11PreferenceBridge.isValidCustomResolution("0x1080"));
        assertFalse(X11PreferenceBridge.isValidCustomResolution("-1920x1080"));
        assertFalse(X11PreferenceBridge.isValidCustomResolution("abcxdef"));
    }
}

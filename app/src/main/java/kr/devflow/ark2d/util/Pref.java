package kr.devflow.ark2d.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Pref {
    private static final String PREF_NAME = "ark2d";

    public static void savePref(Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String readPref(Context context, String key, String defValue) {
        return context.getSharedPreferences(PREF_NAME, 0).getString(key, defValue);
    }
}

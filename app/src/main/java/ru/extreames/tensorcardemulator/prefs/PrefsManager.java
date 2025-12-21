package ru.extreames.tensorcardemulator.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public PrefsManager(Context context, String name) {
        sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setValue(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getValue(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
}
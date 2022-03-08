package com.appme.story.settings.theme;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.os.Environment;

import com.appme.story.AppController;

public class ThemePreference {
    
    public static final int FRAGMENT_OPEN = 99;
    public static final String EXTRA_RECREATE = "recreate";
    public static final String KEY_PRIMARY_COLOR = "primaryColor";
    public static final String KEY_ACCENT_COLOR = "accentColor";
    public static final String KEY_DEFAULT_DIR = "defaultDir";
    
    public static int getPrimaryColor()
    {
        return PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext())
            .getInt(KEY_PRIMARY_COLOR, Color.parseColor("#455A64"));
    }
    
    public static void setPrimaryColor(int color)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_PRIMARY_COLOR, color);
        editor.commit();
    }

    public static int getAccentColor()
    {
        return PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext())
            .getInt(KEY_ACCENT_COLOR, Color.parseColor("#EF3A0F"));
    }

    public static void setAccentColor(int color)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_ACCENT_COLOR, color);
        editor.commit();
    }

    public static String getDefaultDir()
    {
        return PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext()).getString(KEY_DEFAULT_DIR, Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public static void setDefaultDir(String dir)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DEFAULT_DIR, dir);
        editor.commit();
    }
}

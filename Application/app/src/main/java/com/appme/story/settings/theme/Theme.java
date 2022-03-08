package com.appme.story.settings.theme;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.appme.story.R;
import com.appme.story.AppController;
import com.appme.story.settings.theme.ThemePreference;

public class Theme {

    public int mTheme = -1;
    public static String theme = "name_of_the_theme";
    public static final String THEME_PREFERENCES = "com.appme.story.themepref";
    public static final String RECREATE_ACTIVITY = "com.appme.story.recreateactivity";
    public static final String EXTRA_RECREATE = "recreate";
    public static final String THEME_SAVED = "com.appme.story.savedtheme";
    public static final String DARKTHEME = "com.appme.story.darktheme";
    public static final String LIGHTTHEME = "com.appme.story.lighttheme";

    private static volatile Theme Instance = null;
    private Context context;

    public static Theme getInstance() {
        Theme localInstance = Instance;
        if (localInstance == null) {
            synchronized (Theme.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new Theme(AppController.getAppContext());
                }
            }
        }
        return localInstance;
    }

    private Theme(Context context) {
        this.context = context;
    }

    public static Theme with(Context context) {
        return new Theme(context);
    }

    public void setTheme(Activity c) {
        theme = c.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE).getString(THEME_SAVED, LIGHTTHEME);
        if (theme.equals(LIGHTTHEME)) {
            // mTheme = R.style.CustomStyle_LightTheme;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // mTheme = R.style.CustomStyle_DarkTheme;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        c.setTheme(R.style.AppTheme_Application);
    }

    public void getTheme(AppCompatActivity c) {
        /*Note: the recreate_key's value is changed to false before calling recreate(), or we woudl have ended up in an infinite loop,
         as onResume() will be called on recreation, which will again call recreate() and so on....
         and get an ANR
         */
        if (c.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE).getBoolean(RECREATE_ACTIVITY, false)) {
            SharedPreferences.Editor editor = c.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE).edit();
            editor.putBoolean(RECREATE_ACTIVITY, false);
            editor.apply();
            c.recreate();
        }

    }

    public void setDefaultDir(String folder) {

    }

    public void getDefaultFolder() {

    }

    public int getPrimaryColor() {
        return ThemePreference.getPrimaryColor();
    }

    public void onToolbar(final AppCompatActivity activity, Toolbar mToolbar) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE);
        Drawable icon = null;
        //int bgColor;
        //int todoTextColor;
        if (sharedPreferences.getString(THEME_SAVED, LIGHTTHEME).equals(LIGHTTHEME)) {
            //bgColor = Color.WHITE;
            icon = AppCompatResources.getDrawable(activity, R.drawable.abc_ic_ab_back_material);     
            icon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        } else {
            //bgColor = Color.BLACK;  
            icon = AppCompatResources.getDrawable(activity, R.drawable.abc_ic_ab_back_material);     
            icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);          
        }
        mToolbar.setTitle(null);
        mToolbar.setNavigationIcon(icon);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    activity.finish();
                }
            });
        activity.setSupportActionBar(mToolbar);     
    }
}


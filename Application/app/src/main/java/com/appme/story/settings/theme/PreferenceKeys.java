package com.appme.story.settings.theme;

import android.content.res.Resources;
import com.appme.story.R;

public class PreferenceKeys {
    public final String night_mode_pref_key;

    public PreferenceKeys(Resources resources) {
        night_mode_pref_key = resources.getString(R.string.pref_key_night_mode);
    }
}



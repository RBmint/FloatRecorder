package com.android.floatball.recorderball;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TestPreference extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.floatrecord);
    }
}

package com.android.floatball.recorderball;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.ListPopupWindow;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingDemo extends PreferenceActivity {
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preference yybc = findPreference("floatRecorder");
        yybc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingDemo.this, TestPreference.class));
                return false;
            }
        });

        Preference sL = findPreference("switchLocation");
        sL.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(SettingDemo.this);
                return false;
            }
        });
    }
    @SuppressLint("ClickableViewAccessibility")
    public void showDialog(Activity activity){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.edit_two_texts);

        final EditText editText = dialog.findViewById(R.id.et);
        final List<String> items = new ArrayList<>();
        items.add("position1");
        items.add("position2");
        items.add("position3");
        final ListPopupWindow listPopupWindow = new ListPopupWindow(SettingDemo.this);
        listPopupWindow.setAdapter(new ArrayAdapter<>(SettingDemo.this,
                android.R.layout.simple_list_item_1, items));
        listPopupWindow.setAnchorView(editText);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                editText.setText(items.get(position));
                listPopupWindow.dismiss();
                editText.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        getResources().getDrawable(R.drawable.ic_round_keyboard_arrow_down_24px), null);
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (motionEvent.getX() >= (editText.getWidth() - editText
                            .getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        listPopupWindow.show();
                        editText.setCompoundDrawablesWithIntrinsicBounds(null, null,
                                getResources().getDrawable(R.drawable.ic_round_keyboard_arrow_up_24px), null);
                    }
                }
                return false;
            }
        });

        Button btnok = dialog.findViewById(R.id.btnok);
        btnok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });

        Button btncn = dialog.findViewById(R.id.btncn);
        btncn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });
        WindowManager.LayoutParams lp = Objects.requireNonNull(dialog.getWindow()).getAttributes();
        lp.width = getWindowManager().getDefaultDisplay().getWidth();
        dialog.getWindow().setAttributes(lp);
        dialog.show();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }
}

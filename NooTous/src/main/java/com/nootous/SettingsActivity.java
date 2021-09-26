package com.nootous;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

public class SettingsActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        Slider slider = (Slider)findViewById(R.id.slider_position);
        float value = getSharedPreferences("NOOTOUS", Context.MODE_PRIVATE).getFloat("BLURRING", 100.0f);
        slider.setValue(value);
        slider.setLabelFormatter(new MyLabelFormatter());

        Button okButton = (Button)findViewById(R.id.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                saveAndQuit();
            }
        });
    }

    void saveAndQuit(){
        Slider slider = (Slider)findViewById(R.id.slider_position);
        float value = slider.getValue();
        SharedPreferences prefs = getSharedPreferences("NOOTOUS", Activity.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat("BLURRING", value);
        ed.apply();
        setResult(100, new Intent());
        finish();
    }

    class MyLabelFormatter implements LabelFormatter {
        @NonNull
        @Override
        public String getFormattedValue(float value) {
            return ""+(int)value+"m";
        }
    }
}
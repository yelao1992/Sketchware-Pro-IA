package com.besome.sketch.design;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import pro.sketchware.R;

public class AiAssistActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_assist_activity);
        setTitle("AI Assist");

        Button generate = findViewById(R.id.btn_generate);
        generate.setOnClickListener(v -> Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show());

        Button apply = findViewById(R.id.btn_apply);
        apply.setEnabled(false);
    }
}

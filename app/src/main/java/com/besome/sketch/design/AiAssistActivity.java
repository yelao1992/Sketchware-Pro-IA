package com.besome.sketch.design;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import a.a.a.Ox;
import a.a.a.eC;
import a.a.a.jC;
import a.a.a.yq;
import pro.sketchware.R;

public class AiAssistActivity extends BaseAppCompatActivity {

    private TextView preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_assist_activity);
        setTitle("AI Assist");

        String screenName = getIntent().getStringExtra("screen_name");
        String currentXml = getIntent().getStringExtra("current_xml");
        String scId = getIntent().getStringExtra("sc_id");

        TextView screenNameView = findViewById(R.id.tv_screen_name);
        screenNameView.setText("Screen: " + (screenName == null ? "Unknown" : screenName));

        TextView xmlNameView = findViewById(R.id.tv_xml_path);
        xmlNameView.setText("XML: " + (currentXml == null ? "Unknown" : currentXml));

        preview = findViewById(R.id.tv_preview);

        loadPreview(scId, currentXml);

        Button generate = findViewById(R.id.btn_generate);
        generate.setOnClickListener(v -> Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show());

        Button apply = findViewById(R.id.btn_apply);
        apply.setEnabled(false);
    }

    private void loadPreview(String scId, String currentXml) {
        if (scId == null || currentXml == null) {
            preview.setText("Error loading XML preview: Missing sc_id or current_xml.");
            return;
        }

        new Thread(() -> {
            try {
                ProjectFileBean projectFile = jC.b(scId).b(currentXml);
                if (projectFile == null) {
                    throw new IllegalStateException("Project file not found for " + currentXml);
                }

                yq q = new yq(getApplicationContext(), scId);
                Ox xmlGenerator = new Ox(q.N, projectFile);
                var projectDataManager = jC.a(scId);
                var viewBeans = projectDataManager.d(currentXml);
                var viewFab = projectDataManager.h(currentXml);
                xmlGenerator.setExcludeAppCompat(true);
                xmlGenerator.a(eC.a(viewBeans), viewFab);
                String content = xmlGenerator.b();

                runOnUiThread(() -> preview.setText(content));
            } catch (Exception e) {
                String errorMessage = "Error loading XML preview: " + e.getMessage();
                runOnUiThread(() -> {
                    preview.setText(errorMessage);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}

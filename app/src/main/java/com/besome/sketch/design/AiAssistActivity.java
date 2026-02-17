package com.besome.sketch.design;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;

public class AiAssistActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SCREEN_NAME = "screen_name";
    public static final String EXTRA_XML_NAME = "xml_name";
    public static final String EXTRA_SC_ID = "sc_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_assist_activity);
        setTitle("AI Assist");

        String screenName = getIntent().getStringExtra(EXTRA_SCREEN_NAME);
        String xmlName = getIntent().getStringExtra(EXTRA_XML_NAME);
        String scId = getIntent().getStringExtra(EXTRA_SC_ID);
        String safeScreenName = screenName == null ? "Unknown" : screenName;
        String xmlPath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/files/resource/layout/" + xmlName;

        TextView screenNameView = findViewById(R.id.tv_screen_name);
        screenNameView.setText("Screen: " + safeScreenName);

        TextView xmlPathView = findViewById(R.id.tv_xml_path);
        xmlPathView.setText("XML: " + xmlPath);

        TextView preview = findViewById(R.id.tv_preview);
        try {
            if (scId == null || xmlName == null || !FileUtil.isExistFile(xmlPath)) {
                throw new IllegalStateException("Failed to locate layout XML file.");
            }
            preview.setText(FileUtil.readFileIfExist(xmlPath));
        } catch (Exception e) {
            String errorMessage = "Error loading XML preview: " + e.getMessage();
            preview.setText(errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }

        Button generate = findViewById(R.id.btn_generate);
        generate.setOnClickListener(v -> Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show());

        Button apply = findViewById(R.id.btn_apply);
        apply.setEnabled(false);
    }
}

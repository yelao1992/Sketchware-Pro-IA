package com.besome.sketch.design;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import a.a.a.Ox;
import a.a.a.cC;
import a.a.a.eC;
import a.a.a.jC;
import a.a.a.yq;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pro.sketchware.R;
import pro.sketchware.managers.inject.InjectRootLayoutManager;
import pro.sketchware.tools.ViewBeanParser;

public class AiAssistActivity extends BaseAppCompatActivity {

    private static final String PREF_AI_ASSIST = "ai_assist";
    private static final String PREF_API_KEY = "openai_api_key";
    private static final String OPENAI_URL = "https://api.openai.com/v1/responses";
    private static final String OPENAI_MODEL = "gpt-5.2";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private String scId;
    private String currentXml;
    private String currentXmlContent;
    private String previousXmlContent;
    private String proposedXml;

    private EditText instructions;
    private TextView preview;
    private TextView proposedPreview;
    private TextView explanation;
    private Button generate;
    private Button apply;
    private Button undo;

    // NOTE: must be public (BaseAppCompatActivity requires public onCreate)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_assist_activity);
        setTitle("AI Assist");

        String screenName = getIntent().getStringExtra("screen_name");
        currentXml = getIntent().getStringExtra("current_xml");
        scId = getIntent().getStringExtra("sc_id");

        ((TextView) findViewById(R.id.tv_screen_name)).setText("Screen: " + (screenName == null ? "Unknown" : screenName));
        ((TextView) findViewById(R.id.tv_xml_path)).setText("XML: " + (currentXml == null ? "Unknown" : currentXml));

        instructions = findViewById(R.id.et_instructions);
        preview = findViewById(R.id.tv_preview);
        proposedPreview = findViewById(R.id.tv_proposed_preview);
        explanation = findViewById(R.id.tv_explanation);
        generate = findViewById(R.id.btn_generate);
        apply = findViewById(R.id.btn_apply);
        undo = findViewById(R.id.btn_undo);
        Button setApiKey = findViewById(R.id.btn_set_api_key);

        generate.setOnClickListener(v -> generateProposal());
        apply.setOnClickListener(v -> applyProposedXml());
        undo.setOnClickListener(v -> undoLastApply());
        setApiKey.setOnClickListener(v -> showSetApiKeyDialog());

        apply.setEnabled(false);
        undo.setEnabled(false);

        loadPreview();
    }

    private void showSetApiKeyDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("sk-...");
        input.setText(getPreferences().getString(PREF_API_KEY, ""));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Set API key")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    getPreferences().edit().putString(PREF_API_KEY, input.getText().toString().trim()).apply();
                    Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREF_AI_ASSIST, MODE_PRIVATE);
    }

    private void loadPreview() {
        if (scId == null || currentXml == null) {
            preview.setText("Error loading XML preview: Missing sc_id or current_xml.");
            return;
        }

        new Thread(() -> {
            try {
                String generatedXml = generateXmlFromProject(scId, currentXml);
                currentXmlContent = generatedXml;
                runOnUiThread(() -> preview.setText(generatedXml));
            } catch (Exception e) {
                String errorMessage = "Error loading XML preview: " + e.getMessage();
                runOnUiThread(() -> {
                    preview.setText(errorMessage);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String generateXmlFromProject(String projectId, String xmlName) {
        ProjectFileBean projectFile = jC.b(projectId).b(xmlName);
        if (projectFile == null) {
            throw new IllegalStateException("Project file not found for " + xmlName);
        }

        yq q = new yq(getApplicationContext(), projectId);
        Ox xmlGenerator = new Ox(q.N, projectFile);
        var projectDataManager = jC.a(projectId);
        var viewBeans = projectDataManager.d(xmlName);
        var viewFab = projectDataManager.h(xmlName);
        xmlGenerator.setExcludeAppCompat(true);
        xmlGenerator.a(eC.a(viewBeans), viewFab);
        return xmlGenerator.b();
    }

    private void generateProposal() {
        String apiKey = getPreferences().getString(PREF_API_KEY, "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Set API key first", Toast.LENGTH_SHORT).show();
            return;
        }

        String instructionText = instructions.getText().toString().trim();
        if (instructionText.isEmpty()) {
            Toast.makeText(this, "Please enter instructions", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentXmlContent == null || currentXmlContent.isEmpty()) {
            Toast.makeText(this, "Current XML preview is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        generate.setEnabled(false);
        explanation.setText("Generating...");

        new Thread(() -> {
            try {
                GenerateResult result = requestProposal(apiKey, instructionText, currentXmlContent);
                proposedXml = result.updatedXml;
                runOnUiThread(() -> {
                    explanation.setText(result.explanation);
                    proposedPreview.setText(result.updatedXml);
                    apply.setEnabled(!result.updatedXml.trim().isEmpty());
                    generate.setEnabled(true);
                });
            } catch (Exception e) {
                String errorMessage = "Generate failed: " + e.getMessage();
                runOnUiThread(() -> {
                    explanation.setText(errorMessage);
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    generate.setEnabled(true);
                });
            }
        }).start();
    }

    private GenerateResult requestProposal(String apiKey, String instructionText, String xmlText) throws Exception {
        JSONObject schema = new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("updated_xml", new JSONObject().put("type", "string"))
                        .put("explanation", new JSONObject().put("type", "string")))
                .put("required", new JSONArray().put("updated_xml").put("explanation"))
                .put("additionalProperties", false);

        String prompt = "You are editing Android XML. Return only valid JSON according to the schema.\n" +
                "Instruction:\n" + instructionText + "\n\nCurrent XML:\n" + xmlText;

        JSONObject bodyJson = new JSONObject()
                .put("model", OPENAI_MODEL)
                .put("input", prompt)
                .put("text", new JSONObject().put("format", new JSONObject()
                        .put("type", "json_schema")
                        .put("name", "ai_assist_response")
                        .put("strict", true)
                        .put("schema", schema)));

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IllegalStateException("HTTP " + response.code() + ": " + err);
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JSONObject responseJson = new JSONObject(responseBody);
            String outputText = responseJson.optString("output_text", "");

            if (outputText.isEmpty()) {
                JSONArray output = responseJson.optJSONArray("output");
                if (output != null && output.length() > 0) {
                    JSONObject first = output.optJSONObject(0);
                    if (first != null) {
                        JSONArray content = first.optJSONArray("content");
                        if (content != null && content.length() > 0) {
                            JSONObject firstContent = content.optJSONObject(0);
                            if (firstContent != null) {
                                outputText = firstContent.optString("text", "");
                            }
                        }
                    }
                }
            }

            if (outputText.isEmpty()) {
                throw new IllegalStateException("OpenAI response did not contain output_text");
            }

            outputText = cleanJsonText(outputText);
            JSONObject structured = new JSONObject(outputText);
            String updatedXml = structured.optString("updated_xml", "");
            String explanationText = structured.optString("explanation", "");

            if (updatedXml.isEmpty()) {
                throw new IllegalStateException("updated_xml is empty");
            }

            return new GenerateResult(updatedXml, explanationText);
        }
    }

    private String cleanJsonText(String text) {
        String clean = text.trim();
        if (clean.startsWith("```") && clean.endsWith("```")) {
            int firstNewline = clean.indexOf('\n');
            if (firstNewline >= 0) {
                clean = clean.substring(firstNewline + 1, clean.length() - 3).trim();
            }
        }
        return clean;
    }

    private void applyProposedXml() {
        if (proposedXml == null || proposedXml.trim().isEmpty()) {
            Toast.makeText(this, "No proposed XML to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scId == null || currentXml == null) {
            Toast.makeText(this, "Missing sc_id or current_xml", Toast.LENGTH_SHORT).show();
            return;
        }

        previousXmlContent = currentXmlContent;
        new Thread(() -> {
            try {
                applyXmlToProject(proposedXml);
                currentXmlContent = proposedXml;
                runOnUiThread(() -> {
                    preview.setText(currentXmlContent);
                    Toast.makeText(this, "Applied", Toast.LENGTH_SHORT).show();
                    undo.setEnabled(previousXmlContent != null && !previousXmlContent.isEmpty());
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Apply failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void undoLastApply() {
        if (previousXmlContent == null || previousXmlContent.isEmpty()) {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            return;
        }

        String restoreXml = previousXmlContent;
        new Thread(() -> {
            try {
                applyXmlToProject(restoreXml);
                currentXmlContent = restoreXml;
                previousXmlContent = null;
                runOnUiThread(() -> {
                    preview.setText(currentXmlContent);
                    undo.setEnabled(false);
                    Toast.makeText(this, "Undo complete", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Undo failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void applyXmlToProject(String xmlContent) throws Exception {
        var parser = new ViewBeanParser(xmlContent);
        parser.setSkipRoot(true);
        var parsedLayout = parser.parse();
        var root = parser.getRootAttributes();

        var rootLayoutManager = new InjectRootLayoutManager(scId);
        rootLayoutManager.set(currentXml, InjectRootLayoutManager.toRoot(root));

        HistoryViewBean bean = new HistoryViewBean();
        bean.actionOverride(parsedLayout, jC.a(scId).d(currentXml));
        var cc = cC.c(scId);
        if (!cc.c.containsKey(currentXml)) {
            cc.e(currentXml);
        }
        cc.a(currentXml);
        cc.a(currentXml, bean);
        jC.a(scId).c.put(currentXml, parsedLayout);

        runOnUiThread(() -> setResult(RESULT_OK));
    }

    private static class GenerateResult {
        final String updatedXml;
        final String explanation;

        GenerateResult(String updatedXml, String explanation) {
            this.updatedXml = updatedXml;
            this.explanation = explanation;
        }
    }
}

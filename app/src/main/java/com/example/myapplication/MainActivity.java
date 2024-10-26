package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String modelUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/model_mnist.tflite";
    private String featuresUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_sample_feat.bin";
    private String labelsUrl= "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_sample_label.bin";
    private File modelFile;
    private FloatBuffer featuresBuffer;
    private FloatBuffer labelsBuffer;

    private TextView tvStatus;

    private Button btnSelectModel, btnSelectDataset, btnStartTraining, btnConfigurations;
    private ProgressBar progressBar;

    int batchSize = 32;
    int dimension = 784;
    int numBatches = 1;
    private ProgressBar downloadProgressBar;



    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectModel = findViewById(R.id.btn_select_model);
        btnSelectDataset = findViewById(R.id.btn_select_dataset);
        btnStartTraining = findViewById(R.id.btn_start_training);
        btnConfigurations = findViewById(R.id.btn_configurations);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progressBar);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);

        applyAnimations();

        btnSelectModel.setOnClickListener(v -> {
            // Download model from URL
            modelFile = new File(getFilesDir(), "model.tflite");
            downloadFile(modelUrl, "Model", true, modelFile);
        });

        btnSelectDataset.setOnClickListener(v -> {
            // Download dataset from URL
            downloadFile(featuresUrl, "Features", false, null);
            downloadFile(labelsUrl, "Labels", false, null);
        });

        btnStartTraining.setOnClickListener(v -> {
            if (modelFile != null && featuresBuffer != null && labelsBuffer != null) {
                startTraining();
            } else {
                tvStatus.setText("Please download both model and dataset");
            }
        });

        btnConfigurations.setOnClickListener(v -> {
            // Download model from URL
            showConfigurationsDialog();
        });
    }

    private void showConfigurationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Configuration");

        // Create labels and EditTexts for numeric input
        TextView batchLabel = new TextView(this);
        batchLabel.setText("Batch Size:");
        batchLabel.setTextSize(18);
        batchLabel.setTextColor(Color.WHITE);
        batchLabel.setPadding(0, 10, 0, 5);

        EditText editTextBatch = createNumberInput(batchSize);

        TextView dimensionLabel = new TextView(this);
        dimensionLabel.setText("Feature Dimensions:");
        dimensionLabel.setTextSize(18);
        dimensionLabel.setTextColor(Color.WHITE);
        dimensionLabel.setPadding(0, 10, 0, 5);

        EditText editTextDimension = createNumberInput(dimension);

        // Arrange labels and EditTexts vertically in a layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);  // Add padding around the layout
        layout.setElevation(10);  // Add elevation for shadow effect

        // Add components to the layout
        layout.addView(batchLabel);
        layout.addView(editTextBatch);
        layout.addView(dimensionLabel);
        layout.addView(editTextDimension);

        // Set layout to dialog builder
        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            batchSize = parseInteger(editTextBatch.getText().toString(), batchSize);
            dimension = parseInteger(editTextDimension.getText().toString(), dimension);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Helper method to create an EditText with number input type
    private EditText createNumberInput(int initialValue) {
        EditText editText = new EditText(this);
        editText.setText(String.valueOf(initialValue));      // Set initial value
        editText.setSelectAllOnFocus(true);                  // Auto-select text on focus for easy editing

        // Set padding and background
        editText.setPadding(20, 15, 20, 15);  // Add padding for better touch targets
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Add rounded corners using a shape drawable programmatically
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(10);
        background.setStroke(1, Color.GRAY); // Border color
        editText.setBackground(background);
        editText.setTextColor(Color.BLUE);
        editText.setHintTextColor(Color.GRAY);

        return editText;
    }

    // Helper method to parse input text or fall back to a default value
    private int parseInteger(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;  // Fallback to default if parsing fails
        }
    }

    private void applyAnimations() {
        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);

        // Apply animations to the elements
        tvStatus.startAnimation(fadeIn);
        btnSelectModel.startAnimation(slideUp);
        btnSelectDataset.startAnimation(slideUp);
        btnStartTraining.startAnimation(scaleUp);
    }

    private void downloadFile(String url, String fileType, boolean isModel, File saveFile) {
        tvStatus.setText("Downloading " + fileType + "...");
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(0);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText(fileType + " Download Failed: " + e.getMessage());
                    downloadProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, fileType + " download failed", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        tvStatus.setText(fileType + " Download Failed: " + response.message());
                        downloadProgressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;
                byte[] buffer = new byte[80000];
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                try (BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream())) {
                    int read;
                    while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                        byteStream.write(buffer, 0, read);
                        downloadedBytes += read;

                        Log.d("DownloadInfo", fileType + " Bytes read: " + downloadedBytes + "/" + totalBytes);

                        // Update progress
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        runOnUiThread(() -> downloadProgressBar.setProgress(progress));
                    }
                    byteStream.flush();
                    byte[] data = byteStream.toByteArray();

                    // For model files, save to disk
                    if (isModel && saveFile != null) {
                        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                            fos.write(data);
                            fos.flush();
                            runOnUiThread(() -> tvStatus.setText(fileType + " Downloaded"));
                        } catch (IOException e) {
                            runOnUiThread(() -> tvStatus.setText("Error saving " + fileType + ": " + e.getMessage()));
                        }
                    } else if (!isModel) {
                        // For datasets, wrap into ByteBuffer
                        ByteBuffer datasetBuffer = ByteBuffer.wrap(data);
                        datasetBuffer.order(ByteOrder.nativeOrder());
                        runOnUiThread(() -> tvStatus.setText(fileType + " Downloaded"));

                        if (fileType.equals("Features")) {
                            featuresBuffer = datasetBuffer.asFloatBuffer();
                            Log.d("Size", fileType + " " + featuresBuffer.capacity());
                        } else {
                            labelsBuffer = datasetBuffer.asFloatBuffer();
                            Log.d("Size", fileType + " " + labelsBuffer.capacity());
                        }
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error reading " + fileType + ": " + e.getMessage());
                    });
                } finally {
                    runOnUiThread(() -> downloadProgressBar.setVisibility(View.GONE));
                }
            }
        });
    }

    private void startTraining() {
        if (featuresBuffer == null || labelsBuffer == null) {
            tvStatus.setText("Please download both features and labels");
            return;
        }

        tvStatus.setText("Training Started...");
        progressBar.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {

                Interpreter tflite;

                try {
                    Interpreter.Options options = new Interpreter.Options();
                    options.setUseNNAPI(true);
                    tflite = new Interpreter(modelFile, options);
                } catch (IllegalArgumentException e) {
                    Log.e("Interpreter", "GPU Delegate failed, falling back to CPU.", e);
                    tflite = new Interpreter(modelFile);  // Fallback to CPU
                }

                // Combine features and labels into inputs map
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("x", featuresBuffer);
                //inputs.put("y", labelsBuffer);

                for (int j = 0; j < 28*28; j += 1) {
                    Log.d(TAG, "Features: " + featuresBuffer.get(j));
                }

                // Prepare the output buffer (e.g., loss)
                ByteBuffer outputBuffer = ByteBuffer.allocateDirect(10*4);
                outputBuffer.order(ByteOrder.nativeOrder());

                FloatBuffer floatOutputBuffer = outputBuffer.asFloatBuffer();

                // Output map to hold loss
                Map<String, Object> outputs = new HashMap<>();
                //outputs.put("loss", outputBuffer);
                outputs.put("output", outputBuffer);


                // Run the model with the training signature
                // tflite.runSignature(inputs, outputs, "train");

                tflite.runSignature(inputs, outputs, "infer");

                featuresBuffer.rewind();
                labelsBuffer.rewind();
                floatOutputBuffer.rewind();

                // Process the result to get the final category values.
                for (int j = 0; j < 10; j += 1) {
                    Log.d(TAG, "Inference: " + floatOutputBuffer.get(j));
                }

                // Retrieve and log the output (e.g., loss)
                outputBuffer.rewind();


                //float loss = outputBuffer.getFloat();
                //Log.d(TAG, "Training loss: " + loss);

            } catch (Exception e) {
                Log.e(TAG, "Error during training", e);
            } finally {
                long trainingTime = System.currentTimeMillis() - startTime;
                runOnUiThread(() -> {
                    tvStatus.setText("Training Completed. Time: " + trainingTime + "ms");
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

}

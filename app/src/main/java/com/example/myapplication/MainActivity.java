package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.ArrayList;


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
    List<Integer> dimensions = List.of(10000);
    private int BATCH_SIZE = 64;
    private int NUM_BATCHES = 100;

    private int NUM_EPOCHS = 1;

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
        TextView nepochsLabel = new TextView(this);
        nepochsLabel.setText("Number of epochs:");
        nepochsLabel.setTextSize(18);
        nepochsLabel.setTextColor(Color.WHITE);
        nepochsLabel.setPadding(0, 10, 0, 5);

        EditText editTextNEpochs = createNumberInput(NUM_EPOCHS);

        // Create labels and EditTexts for numeric input
        TextView nbatchesLabel = new TextView(this);
        nbatchesLabel.setText("Number of batches:");
        nbatchesLabel.setTextSize(18);
        nbatchesLabel.setTextColor(Color.WHITE);
        nbatchesLabel.setPadding(0, 10, 0, 5);

        EditText editTextNBatches = createNumberInput(NUM_BATCHES);

        // Create labels and EditTexts for numeric input
        TextView batchLabel = new TextView(this);
        batchLabel.setText("Batch Size:");
        batchLabel.setTextSize(18);
        batchLabel.setTextColor(Color.WHITE);
        batchLabel.setPadding(0, 10, 0, 5);

        EditText editTextBatch = createNumberInput(BATCH_SIZE);

        TextView dimensionLabel = new TextView(this);
        dimensionLabel.setText("Feature Dimensions:");
        dimensionLabel.setTextSize(18);
        dimensionLabel.setTextColor(Color.WHITE);
        dimensionLabel.setPadding(0, 10, 0, 5);

        EditText editTextDimension = createNumberListInput(dimensions);


        // Arrange labels and EditTexts vertically in a layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);  // Add padding around the layout
        layout.setElevation(10);  // Add elevation for shadow effect

        // Add components to the layout
        layout.addView(nepochsLabel);
        layout.addView(editTextNEpochs);
        layout.addView(nbatchesLabel);
        layout.addView(editTextNBatches);
        layout.addView(batchLabel);
        layout.addView(editTextBatch);
        layout.addView(dimensionLabel);
        layout.addView(editTextDimension);

        // Set layout to dialog builder
        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            NUM_EPOCHS = parseInteger(editTextNEpochs.getText().toString(), NUM_EPOCHS);
            NUM_BATCHES = parseInteger(editTextNBatches.getText().toString(), NUM_BATCHES);
            BATCH_SIZE = parseInteger(editTextBatch.getText().toString(), BATCH_SIZE);
            dimensions = parseIntegerList(editTextDimension.getText().toString(), dimensions);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Helper method to create an EditText with number input type
    private EditText createNumberInput(Integer initialValue) {
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

    private EditText createNumberListInput(List<Integer> initialValue) {
        EditText editText = new EditText(this);
        editText.setText(listToString(initialValue));
        editText.setSelectAllOnFocus(true);

        // Set padding and background
        editText.setPadding(20, 15, 20, 15);
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

    private List<Integer> parseIntegerList(String text, List<Integer> defaultValue) {
        try {
            return stringToList(text);
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

    private String listToString(List<Integer> numberList) {
        return numberList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Integer> stringToList(String numbers) {
        return Arrays.stream(numbers.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
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

                Interpreter tfliteInterpreter;

                try {
                    Interpreter.Options options = new Interpreter.Options();
                    options.setUseNNAPI(true);
                    tfliteInterpreter = new Interpreter(modelFile, options);
                } catch (IllegalArgumentException e) {
                    Log.e("Interpreter", "GPU Delegate failed, falling back to CPU.", e);
                    tfliteInterpreter = new Interpreter(modelFile);  // Fallback to CPU
                }

                List<FloatBuffer> trainImageBatches = new ArrayList<>(NUM_BATCHES);
                List<FloatBuffer> trainLabelBatches = new ArrayList<>(NUM_BATCHES);

                int FLATTENED_FEATURES_SIZE = 1;
                for (int dimension : dimensions){
                    FLATTENED_FEATURES_SIZE *= dimension;
                }

                int LABELS_SIZE = tfliteInterpreter.getOutputTensor(0).numBytes();

                // Prepare training batches.
                for (int i = 0; i < NUM_BATCHES; ++i) {
                    ByteBuffer  trainImages = ByteBuffer.allocateDirect(BATCH_SIZE * FLATTENED_FEATURES_SIZE).order(ByteOrder.nativeOrder());
                    FloatBuffer trainFeaturesFloat = trainImages.asFloatBuffer();
                    ByteBuffer trainLabels = ByteBuffer.allocateDirect(BATCH_SIZE * LABELS_SIZE).order(ByteOrder.nativeOrder());
                    FloatBuffer trainLabelsFloat = trainLabels.asFloatBuffer();

                    // Slice the required portion from featuresBuffer for this batch
                    int features_start = i * BATCH_SIZE * FLATTENED_FEATURES_SIZE;
                    int features_end = features_start + BATCH_SIZE * FLATTENED_FEATURES_SIZE;
                    FloatBuffer featuresBatchSlice = featuresBuffer.duplicate();  // Duplicate to avoid modifying original buffer position
                    featuresBatchSlice.position(features_start);
                    featuresBatchSlice.limit(features_end);

                    Log.e("FEATURES", "Features start: " + features_start + ",  Features end:" + features_end);

                    // Copy the sliced data to trainFeaturesFloat
                    trainFeaturesFloat.put(featuresBatchSlice);

                    int labels_start = i * BATCH_SIZE * LABELS_SIZE;
                    int labels_end = labels_start + BATCH_SIZE * LABELS_SIZE;
                    FloatBuffer labelsBatchSlice = featuresBuffer.duplicate();  // Duplicate to avoid modifying original buffer position
                    labelsBatchSlice.position(labels_start);
                    labelsBatchSlice.limit(labels_end);

                    Log.e("Labels", "Labels start: " + features_start + ",  Labels end:" + features_end);

                    // Copy the sliced data to trainLabelsFloat
                    trainLabelsFloat.put(labelsBatchSlice);

                    trainFeaturesFloat.clear();
                    trainLabelsFloat.clear();

                    trainImageBatches.add(trainFeaturesFloat);
                    trainLabelBatches.add(trainLabelsFloat);
                }

                // Run training for a few steps.
                float[] losses = new float[NUM_EPOCHS];
                for (int epoch = 0; epoch < NUM_EPOCHS; ++epoch) {
                    FloatBuffer loss = FloatBuffer.allocate(4).put(1);
                    for (int batchIdx = 0; batchIdx < NUM_BATCHES; ++batchIdx) {
                        Map<String, Object> inputs = new HashMap<>();
                        inputs.put("x", trainImageBatches.get(batchIdx));
                        inputs.put("y", trainLabelBatches.get(batchIdx));

                        Map<String, Object> outputs = new HashMap<>();
                        loss = FloatBuffer.allocate(1);
                        outputs.put("loss", loss);

                        tfliteInterpreter.runSignature(inputs, outputs, "train");

                        // Record the last loss.
                        if (batchIdx == NUM_BATCHES - 1) losses[epoch] = loss.get(0);
                    }

                    System.out.println("Finished " + (epoch + 1) + " epochs, current loss: " + loss.get(0));
                }

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

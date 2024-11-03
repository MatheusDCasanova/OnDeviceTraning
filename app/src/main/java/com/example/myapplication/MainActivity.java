package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.BatteryManager;
import android.content.Context;

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

import android.content.Intent;
import android.content.IntentFilter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String modelUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/model_mnist_fixed_batch_size.tflite";
    private String featuresUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_feats.bin";
    private String labelsUrl= "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_labels.bin";
    private File modelFile;
    private FloatBuffer featuresBuffer;
    private FloatBuffer labelsBuffer;

    private TextView tvStatus;
    private TextView tvConfigurations;
    private Button btnConfigurations;
    private ImageView checkmarkConfigurations;
    private Button btnSelectModel;
    private ImageView checkmarkSelectModel;
    private Button btnSelectDataset;
    private ImageView checkmarkSelectDataset;
    private Button btnStartTraining;
    private ImageView checkmarkStartTraining;
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

        initViews();

        applyAnimations();

        btnSelectModel.setOnClickListener(v -> selectModel());
        btnSelectDataset.setOnClickListener(v -> selectDataset());
        btnStartTraining.setOnClickListener(v -> startTrainingIfReady());
        btnConfigurations.setOnClickListener(v -> showConfigurationsDialog());
    }

    private void initViews(){
        btnConfigurations = findViewById(R.id.btn_configurations);
        checkmarkConfigurations = findViewById(R.id.checkmark_configurations);
        btnSelectModel = findViewById(R.id.btn_select_model);
        checkmarkSelectModel = findViewById(R.id.checkmark_model);
        btnSelectDataset = findViewById(R.id.btn_select_dataset);
        checkmarkSelectDataset = findViewById(R.id.checkmark_dataset);
        btnStartTraining = findViewById(R.id.btn_start_training);
        checkmarkStartTraining = findViewById(R.id.checkmark_training);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progressBar);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        tvConfigurations = findViewById(R.id.tv_configurations);
    }

    private void startTrainingIfReady() {
        if (modelFile != null && featuresBuffer != null && labelsBuffer != null) {
            startTraining();
        } else {
            tvStatus.setText("Please download both model and dataset");
        }
    }

    private void selectDataset(){
        // Download dataset from URL
        downloadFile(featuresUrl, "Features", false, null);
        downloadFile(labelsUrl, "Labels", false, null);
    }

    private void selectModel(){
        // Download model from URL
        modelFile = new File(getFilesDir(), "model.tflite");
        downloadFile(modelUrl, "Model", true, modelFile);
    }

    private void showConfigurationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Configuration");
        LinearLayout layout = createConfigurationsLayout();
        builder.setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    updateConfigurations(layout);
                    checkmarkConfigurations.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private LinearLayout createConfigurationsLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        layout.addView(createLabel("Number of epochs:"));
        layout.addView(createNumberInput(NUM_EPOCHS));

        layout.addView(createLabel("Number of batches:"));
        layout.addView(createNumberInput(NUM_BATCHES));

        layout.addView(createLabel("Batch Size:"));
        layout.addView(createNumberInput(BATCH_SIZE));

        layout.addView(createLabel("Feature Dimensions:"));
        layout.addView(createNumberInputList(dimensions));

        return layout;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(18);
        label.setTextColor(Color.WHITE);
        label.setPadding(0, 10, 0, 5);
        return label;
    }

    private EditText createNumberInput(int value) {
        EditText input = new EditText(this);
        input.setText(String.valueOf(value));
        applyEditTextStyle(input);
        return input;
    }

    private EditText createNumberInputList(List<Integer> values) {
        EditText input = new EditText(this);
        input.setText(values.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        applyEditTextStyle(input);
        return input;
    }

    private void applyEditTextStyle(EditText editText) {
        editText.setPadding(20, 15, 20, 15);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(10);
        background.setStroke(1, Color.GRAY);
        editText.setBackground(background);
        editText.setTextColor(Color.BLUE);
    }

    private void updateConfigurations(LinearLayout layout) {
        NUM_EPOCHS = Integer.parseInt(((EditText) layout.getChildAt(1)).getText().toString());
        NUM_BATCHES = Integer.parseInt(((EditText) layout.getChildAt(3)).getText().toString());
        BATCH_SIZE = Integer.parseInt(((EditText) layout.getChildAt(5)).getText().toString());
        dimensions = Arrays.stream(((EditText) layout.getChildAt(7)).getText().toString().split(","))
                .map(Integer::parseInt).collect(Collectors.toList());
        setConfigurationsTextView();
    }

    private void setConfigurationsTextView() {
        String formattedText = "<b>Configuration Details</b><br>" +
                "<font color='#4CAF50'>Epochs:</font> " + NUM_EPOCHS + "<br>" +
                "<font color='#4CAF50'>Batches:</font> " + NUM_BATCHES + "<br>" +
                "<font color='#4CAF50'>Batch Size:</font> " + BATCH_SIZE + "<br>" +
                "<font color='#4CAF50'>Dimensions:</font> " + listToString(dimensions);
        tvConfigurations.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY));
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
        setConfigurationsTextView();
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
                    runOnUiThread(() -> {
                        downloadProgressBar.setVisibility(View.GONE);
                        if (isModel) {
                            checkmarkSelectModel.setVisibility(View.VISIBLE);
                        } else {
                            checkmarkSelectDataset.setVisibility(View.VISIBLE);
                        }
                    });

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

    private double calculateEnergy(Long consumed_charge, int voltage){
        double float_charge = consumed_charge.doubleValue(); // microAmpere-hour

        return (float_charge/1e6) * (((double) voltage)/1e3) * 3600;
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

                int LABELS_SIZE = tfliteInterpreter.getOutputTensor(0).numBytes() / (4*BATCH_SIZE);

                // Prepare training batches.
                for (int i = 0; i < NUM_BATCHES; i++) {
                    Log.d("I", "Counter: " + i);
                    ByteBuffer  trainFeatures = ByteBuffer.allocateDirect(BATCH_SIZE * FLATTENED_FEATURES_SIZE * 4).order(ByteOrder.nativeOrder());
                    FloatBuffer trainFeaturesFloat = trainFeatures.asFloatBuffer();
                    ByteBuffer trainLabels = ByteBuffer.allocateDirect(BATCH_SIZE * LABELS_SIZE * 4).order(ByteOrder.nativeOrder());
                    FloatBuffer trainLabelsFloat = trainLabels.asFloatBuffer();

                    // Slice the required portion from featuresBuffer for this batch
                    int features_start = i * BATCH_SIZE * FLATTENED_FEATURES_SIZE;
                    int features_end = features_start + BATCH_SIZE * FLATTENED_FEATURES_SIZE;
                    FloatBuffer featuresBatchSlice = featuresBuffer.duplicate();  // Duplicate to avoid modifying original buffer position
                    featuresBatchSlice.position(features_start);
                    featuresBatchSlice.limit(features_end);

                    Log.d("Features", "Features start: " + features_start + ",  Features end:" + features_end);

                    // Copy the sliced data to trainFeaturesFloat
                    trainFeaturesFloat.put(featuresBatchSlice);

                    int labels_start = i * BATCH_SIZE * LABELS_SIZE;
                    int labels_end = labels_start + BATCH_SIZE * LABELS_SIZE;
                    FloatBuffer labelsBatchSlice = labelsBuffer.duplicate();  // Duplicate to avoid modifying original buffer position
                    labelsBatchSlice.position(labels_start);
                    labelsBatchSlice.limit(labels_end);

                    Log.d("Labels", "Labels start: " + labels_start + ",  Labels end:" + labels_end);
                    Log.d("Labels", "LABELS SIZE: " + LABELS_SIZE);

                    // Copy the sliced data to trainLabelsFloat
                    trainLabelsFloat.put(labelsBatchSlice);

                    trainFeaturesFloat.clear();
                    trainLabelsFloat.clear();

                    trainImageBatches.add(trainFeaturesFloat);
                    trainLabelBatches.add(trainLabelsFloat);
                }

                // Run training for a few steps.
                float[] losses = new float[NUM_EPOCHS];

                startTime = System.currentTimeMillis();
                BatteryManager mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
                Long start_charge = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

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

                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = this.registerReceiver(null, filter);
                int voltageMillivolts = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                Long finish_charge = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

                Long consumed_charge = start_charge - finish_charge;

                Log.d("ENERGY", "Millivolts " + (double) voltageMillivolts);

                Log.d("ENERGY", "Microampere hour " + consumed_charge);

                double energy = calculateEnergy(consumed_charge, voltageMillivolts);

                Log.d("ENERGY", "Joules " + energy);


                long trainingTime = System.currentTimeMillis() - startTime;

                runOnUiThread(() -> {
                    tvStatus.setText("Training Completed. Time: " + trainingTime + "ms");
                    checkmarkStartTraining.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during training", e);
                runOnUiThread(() -> {
                    tvStatus.setText("Error during training");
                    checkmarkStartTraining.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

}

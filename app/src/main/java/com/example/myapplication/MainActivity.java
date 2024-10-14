package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
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
    private String modelUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/model.tflite";
    private String featuresUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/features.bin";
    private String labelsUrl= "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/labels.bin";
    private File modelFile;
    private ByteBuffer featuresBuffer;
    private ByteBuffer labelsBuffer;

    private TextView tvStatus;

    private Button btnSelectModel, btnSelectDataset, btnStartTraining;
    private ProgressBar progressBar;

    private ProgressBar downloadProgressBar;



    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectModel = findViewById(R.id.btn_select_model);
        btnSelectDataset = findViewById(R.id.btn_select_dataset);
        btnStartTraining = findViewById(R.id.btn_start_training);
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
                            featuresBuffer = datasetBuffer;
                            Log.d("Size", fileType + " " + featuresBuffer.capacity());
                        } else {
                            labelsBuffer = datasetBuffer;
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

                // Prepare the output buffer (e.g., loss)
                ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4);
                outputBuffer.order(ByteOrder.nativeOrder());

                // Combine features and labels into inputs map
                Map<String, Object> inputs = new HashMap<>();
                inputs.put("x", featuresBuffer);
                inputs.put("y", labelsBuffer);

                // Output map to hold loss
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("loss", outputBuffer);

                // Run the model with the training signature
                tflite.runSignature(inputs, outputs, "train");

                // Retrieve and log the output (e.g., loss)
                outputBuffer.rewind();
                float loss = outputBuffer.getFloat();
                Log.d(TAG, "Training loss: " + loss);

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

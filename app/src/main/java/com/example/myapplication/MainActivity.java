package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
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


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String modelUrl = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224_quant_and_labels.zip";
    private String datasetUrl = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224_quant_and_labels.zip";
    private byte[] modelData;
    private byte[] datasetData;
    private TextView tvStatus;

    private Button btnSelectModel, btnSelectDataset, btnStartTraining;
    private ProgressBar progressBar;

    private ProgressBar downloadProgressBar;


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
            downloadFile(modelUrl, true);
        });

        btnSelectDataset.setOnClickListener(v -> {
            // Download dataset from URL
            downloadFile(datasetUrl, false);
        });

        btnStartTraining.setOnClickListener(v -> {
            if (modelData != null && datasetData != null) {
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

    private void downloadFile(String url, boolean isModel) {
        tvStatus.setText("Downloading " + (isModel ? "Model" : "Dataset") + "...");
        downloadProgressBar.setVisibility(View.VISIBLE); // Show the download progress bar
        downloadProgressBar.setProgress(0); // Reset progress to 0

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Download Failed: " + e.getMessage());
                    downloadProgressBar.setVisibility(View.GONE); // Hide the progress bar on failure
                    Log.e(TAG, "Download failed", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Download Failed: " + response.message());
                        downloadProgressBar.setVisibility(View.GONE); // Hide on failure
                    });
                    return;
                }

                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;
                byte[] fileData = new byte[8192]; // Buffer size
                try (BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream())) {
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    int read;
                    while ((read = inputStream.read(fileData, 0, fileData.length)) != -1) {
                        byteBuffer.write(fileData, 0, read);
                        downloadedBytes += read;

                        // Update progress
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        runOnUiThread(() -> downloadProgressBar.setProgress(progress));
                    }
                    byteBuffer.flush();
                    if (isModel) {
                        modelData = byteBuffer.toByteArray();
                        runOnUiThread(() -> tvStatus.setText("Model Downloaded"));
                    } else {
                        datasetData = byteBuffer.toByteArray();
                        runOnUiThread(() -> tvStatus.setText("Dataset Downloaded"));
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error reading data: " + e.getMessage());
                    });
                } finally {
                    runOnUiThread(() -> {
                        downloadProgressBar.setVisibility(View.GONE); // Hide when done
                    });
                }
            }
        });
    }

    private void startTraining() {
        tvStatus.setText("Training Started...");
        progressBar.setVisibility(View.VISIBLE); // Show the ProgressBar at the start

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // Simulate training (replace with actual TensorFlow Lite code)
                Thread.sleep(5000); // Simulating training time
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Handle interruption
            } finally {
                long trainingTime = System.currentTimeMillis() - startTime;

                // Update UI on the main thread
                runOnUiThread(() -> {
                    tvStatus.setText("Training Completed. Time: " + trainingTime + "ms");
                    progressBar.setVisibility(View.GONE); // Hide the ProgressBar when done
                });
            }
        });
    }

}

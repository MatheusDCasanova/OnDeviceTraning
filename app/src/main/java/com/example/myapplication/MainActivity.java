package com.example.myapplication;

import android.annotation.SuppressLint;
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
import java.io.File;
import java.io.FileOutputStream;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String modelUrl = "https://github.com/tensorflow/tflite-micro/raw/refs/heads/main/tensorflow/lite/micro/examples/micro_speech/models/micro_speech_quantized.tflite";
    private String datasetUrl = "https://github.com/tensorflow/tflite-micro/raw/refs/heads/main/tensorflow/lite/micro/examples/micro_speech/models/micro_speech_quantized.tflite";
    private File modelFile;
    private byte[] modelData;
    private byte[] datasetData;
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
            downloadFile(modelUrl, true);
        });

        btnSelectDataset.setOnClickListener(v -> {
            // Download dataset from URL
            downloadFile(datasetUrl, false);
        });

        btnStartTraining.setOnClickListener(v -> {
            if (modelFile != null && datasetData != null) {
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
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(0);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Download Failed: " + e.getMessage());
                    downloadProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Download failed", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Download Failed: " + response.message());
                        downloadProgressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;
                byte[] fileData = new byte[80000];
                try (BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream())) {
                    if (isModel) {
                        // Salvar o modelo como um arquivo .tflite
                        modelFile = new File(getFilesDir(), "model.tflite");
                        Log.d("tag", modelFile.getAbsolutePath());
                        try (FileOutputStream fos = new FileOutputStream(modelFile)) {
                            int read;
                            while ((read = inputStream.read(fileData, 0, fileData.length)) != -1) {
                                fos.write(fileData, 0, read);
                                downloadedBytes += read;

                                // Atualizar progresso
                                int progress = (int) ((downloadedBytes * 100) / totalBytes);
                                runOnUiThread(() -> downloadProgressBar.setProgress(progress));
                            }
                            fos.flush();
                            runOnUiThread(() -> tvStatus.setText("Model Downloaded"));
                        } catch (IOException e) {
                            runOnUiThread(() -> tvStatus.setText("Error saving model: " + e.getMessage()));
                        }
                    } else {
                        // Lógica para salvar dataset
                        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                        int read;
                        while ((read = inputStream.read(fileData, 0, fileData.length)) != -1) {
                            byteBuffer.write(fileData, 0, read);
                            downloadedBytes += read;
                            int progress = (int) ((downloadedBytes * 100) / totalBytes);
                            runOnUiThread(() -> downloadProgressBar.setProgress(progress));
                        }
                        byteBuffer.flush();
                        datasetData = byteBuffer.toByteArray();
                        runOnUiThread(() -> tvStatus.setText("Dataset Downloaded"));
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error reading data: " + e.getMessage());
                    });
                } finally {
                    runOnUiThread(() -> downloadProgressBar.setVisibility(View.GONE));
                }
            }
        });
    }

    private void startTraining() {
        tvStatus.setText("Training Started...");
        progressBar.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Interpreter tflite = new Interpreter(modelFile);
                Log.d("DEBUG", Long.toString(modelFile.length()));
                Log.d("DEBUG", Integer.toString(tflite.getInputTensorCount()));
                Log.d("DEBUG", tflite.getInputTensor(0).toString());
                Thread.sleep(5000);
            } catch (InterruptedException | IllegalArgumentException e) {
                e.printStackTrace();
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

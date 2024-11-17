package com.example.ondevicetraining;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileDownloader {
    private Context context;
    private MainActivity mainActivity;
    public  TextView tvStatus;
    public FileDownloader(Context context, MainActivity mainActivity, TextView tvStatus, ProgressBar downloadProgressBar) {
        this.context = context;
        this.mainActivity = mainActivity;
        this.tvStatus = tvStatus;
        this.mainActivity.downloadProgressBar = downloadProgressBar;
        mainActivity = this.mainActivity;
    }

    public void downloadFile(String url, String fileType, boolean isModel, File saveFile, boolean replicateSingleFeature) {
        Log.d("mainActivity", String.valueOf(mainActivity));
        Log.d("DownloadLink", "Download LINK: " + url);
        mainActivity.setTvStatus("Downloading " + fileType + "...");
        mainActivity.downloadProgressBar.setVisibility(View.VISIBLE);
        mainActivity.downloadProgressBar.setProgress(0);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainActivity.runOnUiThread(() -> {
                    mainActivity.setTvStatus(fileType + " Download Failed: " + e.getMessage());
                    mainActivity.downloadProgressBar.setVisibility(View.GONE);
                    Log.e(mainActivity.TAG, fileType + " download failed", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainActivity.runOnUiThread(() -> {
                        mainActivity.setTvStatus(fileType + " Download Failed: " + response.message());
                        mainActivity.downloadProgressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                long totalBytes = response.body().contentLength();
                long downloadedBytes = 0;
                byte[] buffer = new byte[80000];
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                try (BufferedInputStream inputStream = new BufferedInputStream(response.body().byteStream())) {
                    int read;
                    long startTime = System.currentTimeMillis();
                    while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                        byteStream.write(buffer, 0, read);
                        downloadedBytes += read;

                        Log.d("DownloadInfo", fileType + " Bytes read: " + downloadedBytes + "/" + totalBytes);

                        // Update progress
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        mainActivity.runOnUiThread(() -> mainActivity.downloadProgressBar.setProgress(progress));
                    }
                    long downloadTime = System.currentTimeMillis() - startTime;
                    byteStream.flush();
                    byte[] data = byteStream.toByteArray();

                    // For model files, save to disk
                    if (isModel && saveFile != null) {
                        mainActivity.runOnUiThread(()->mainActivity.adapter.changeModelSize(data.length));


                        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                            fos.write(data);
                            fos.flush();
                            mainActivity.runOnUiThread(() -> mainActivity.setTvStatus(fileType + " Downloaded in " + downloadTime + "ms\nsize: "+
                                    data.length + " Bytes"));
                        } catch (IOException e) {
                            mainActivity.runOnUiThread(() -> mainActivity.setTvStatus("Error saving " + fileType + ": " + e.getMessage()));
                        }
                    } else if (!isModel) {
                        // For datasets, wrap into ByteBuffer
                        ByteBuffer datasetBuffer = ByteBuffer.wrap(data);
                        datasetBuffer.order(ByteOrder.nativeOrder());
                        mainActivity.runOnUiThread(() -> mainActivity.setTvStatus(fileType + " Downloaded in " + downloadTime + "ms" ));

                        if (replicateSingleFeature) {
                            int DATA_SIZE = datasetBuffer.capacity();
                            Log.d("Single data size", DATA_SIZE + " bytes");
                            // replicate datasetBuffer's content NUM_BATCHES X BATCH_SIZE times
                            ByteBuffer replicatedBuffer = ByteBuffer.allocate(
                                    mainActivity.currentConfig.getBatchSize()
                                            * mainActivity.currentConfig.getBatches()
                                            * DATA_SIZE
                                        ).order(ByteOrder.nativeOrder());
                            for (int i = 0; i < mainActivity.currentConfig.getBatches() * mainActivity.currentConfig.getBatchSize(); i++) {
                                // Copy the original data into the replicated buffer
                                replicatedBuffer.put(datasetBuffer.array());
                            }

                            // Reset the position of the buffer to allow reading from the start
                            replicatedBuffer.flip();
                            datasetBuffer = replicatedBuffer;
                        }
                        if (fileType.equals("Features")) {
                            mainActivity.featuresBuffer = datasetBuffer.asFloatBuffer();
                            Log.d("Size", fileType + " " + mainActivity.featuresBuffer.capacity());
                        } else {
                            mainActivity.labelsBuffer = datasetBuffer.asFloatBuffer();
                            Log.d("Size", fileType + " " + mainActivity.labelsBuffer.capacity());
                        }
                    }
                } catch (IOException e) {
                    mainActivity.runOnUiThread(() -> {
                        mainActivity.setTvStatus("Error reading " + fileType + ": " + e.getMessage());
                    });
                } finally {

                    mainActivity.runOnUiThread(() -> {
                        mainActivity.downloadProgressBar.setVisibility(View.GONE);
                    });

                }
            }
        });
    }
}

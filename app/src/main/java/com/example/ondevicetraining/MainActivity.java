package com.example.ondevicetraining;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.BatteryManager;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;


import java.util.List;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import java.io.File;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.ArrayList;

import android.content.IntentFilter;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public File modelFile;

    public ModelConfig currentConfig;
    public FloatBuffer featuresBuffer;
    public FloatBuffer labelsBuffer;
    public TextView tvStatus;

    private Button btnSelectModel;
    private ImageView checkmarkSelectModel;
    private Button btnSelectDataset;
    private Button btnShowHistory;

    private ImageView checkmarkStartTraining;
    private ProgressBar progressBar;

    public ProgressBar downloadProgressBar;

    private DrawerLayout drawerLayout;
    public ViewPager2 viewPager;
    public CardAdapter adapter;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_main);
        currentConfig = new ModelConfig();
        initViews();

        //applyAnimations();

        btnShowHistory.setOnClickListener(v -> showHistory());
    }

    public void showHistory() {
        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    private void initViews(){
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progressBar);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        drawerLayout = findViewById(R.id.drawer_layout); // Ensure this matches your DrawerLayout ID
        ImageButton btnOpenHistory = findViewById(R.id.btn_open_history);
        btnShowHistory = findViewById(R.id.btn_show_history);


        viewPager = findViewById(R.id.viewPager);

        // Sample data for the cards
        DotsIndicator dotsIndicator = findViewById(R.id.dots_indicator);
        adapter = new CardAdapter(viewPager, this,this, tvStatus);
        viewPager.setAdapter(adapter);
        dotsIndicator.setViewPager2(viewPager);

        // Set click listener to open the sidebar
        btnOpenHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    public void startTrainingIfReady() {
        if (modelFile != null && featuresBuffer != null && labelsBuffer != null) {
            startTraining();
        } else {
            tvStatus.setText("Please download both model and dataset");
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
        btnShowHistory.startAnimation(scaleUp);
    }

    private double calculateEnergy(Long consumed_charge, int voltage){
        double float_charge = consumed_charge.doubleValue(); // microAmpere-hour

        return (float_charge/1e6) * (((double) voltage)/1e3) * 3600;
    }

    public void setTvStatus(String text) {
        this.tvStatus.setText(text);
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

                List<FloatBuffer> trainImageBatches = new ArrayList<>(currentConfig.getBatches());
                List<FloatBuffer> trainLabelBatches = new ArrayList<>(currentConfig.getBatches());

                int FLATTENED_FEATURES_SIZE = 1;
                for (int dimension : TypeConverter.stringToList(currentConfig.getDimensions())){
                    FLATTENED_FEATURES_SIZE *= dimension;
                }

                int LABELS_SIZE = tfliteInterpreter.getOutputTensor(0).numBytes() / (Float.BYTES*currentConfig.getBatchSize());

                // Prepare training batches.
                for (int i = 0; i < currentConfig.getBatches(); i++) {
                    Log.d("I", "Counter: " + i);
                    ByteBuffer  trainFeatures = ByteBuffer.allocateDirect(currentConfig.getBatchSize() * FLATTENED_FEATURES_SIZE * 4).order(ByteOrder.nativeOrder());
                    FloatBuffer trainFeaturesFloat = trainFeatures.asFloatBuffer();
                    ByteBuffer trainLabels = ByteBuffer.allocateDirect(currentConfig.getBatchSize() * LABELS_SIZE * 4).order(ByteOrder.nativeOrder());
                    FloatBuffer trainLabelsFloat = trainLabels.asFloatBuffer();

                    // Slice the required portion from featuresBuffer for this batch
                    int features_start = i * currentConfig.getBatchSize() * FLATTENED_FEATURES_SIZE;
                    int features_end = features_start + currentConfig.getBatchSize() * FLATTENED_FEATURES_SIZE;
                    FloatBuffer featuresBatchSlice = featuresBuffer.duplicate();  // Duplicate to avoid modifying original buffer position
                    featuresBatchSlice.position(features_start);
                    featuresBatchSlice.limit(features_end);

                    Log.d("Features", "Features start: " + features_start + ",  Features end:" + features_end);

                    // Copy the sliced data to trainFeaturesFloat
                    trainFeaturesFloat.put(featuresBatchSlice);

                    int labels_start = i * currentConfig.getBatchSize() * LABELS_SIZE;
                    int labels_end = labels_start + currentConfig.getBatchSize() * LABELS_SIZE;
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
                float[] losses = new float[currentConfig.getEpochs()];

                startTime = System.currentTimeMillis();
                BatteryManager mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);
                Long start_charge = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

                for (int epoch = 0; epoch < currentConfig.getEpochs(); ++epoch) {
                    FloatBuffer loss = FloatBuffer.allocate(4).put(1);
                    for (int batchIdx = 0; batchIdx < currentConfig.getBatches(); ++batchIdx) {
                        Map<String, Object> inputs = new HashMap<>();
                        inputs.put("x", trainImageBatches.get(batchIdx));
                        inputs.put("y", trainLabelBatches.get(batchIdx));

                        Map<String, Object> outputs = new HashMap<>();
                        loss = FloatBuffer.allocate(1);
                        outputs.put("loss", loss);

                        tfliteInterpreter.runSignature(inputs, outputs, "train");

                        // Record the last loss.
                        if (batchIdx == currentConfig.getBatches() - 1) losses[epoch] = loss.get(0);
                    }

                    System.out.println("Finished " + (epoch + 1) + " epochs, current loss: " + loss.get(0));
                }

                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = this.registerReceiver(null, filter);
                int voltageMilivolts = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                Long finish_charge = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

                Long consumed_charge = start_charge - finish_charge;

                Log.d("ENERGY", "Milivolts " + (double) voltageMilivolts);

                Log.d("ENERGY", "Microampere hour " + consumed_charge);

                double energy = calculateEnergy(consumed_charge, voltageMilivolts);

                Log.d("ENERGY", "Joules " + energy);

                long trainingTime = System.currentTimeMillis() - startTime;

                int numberSamples = (currentConfig.getBatches() * currentConfig.getBatchSize());

                double sampleEnergy = energy / (double) numberSamples;

                double sampleTime = (double) trainingTime / (double) numberSamples;

                saveToHistory(trainingTime, energy, sampleEnergy, sampleTime);

                // Save to database
                FirebaseHelper database = new FirebaseHelper();
                EnergyMetric metric = new EnergyMetric(energy, sampleEnergy, sampleTime, currentConfig.getEpochs(), currentConfig.getBatches(), currentConfig.getBatchSize(), currentConfig.getModelLink(), currentConfig.getLabelsLink(), currentConfig.getFeaturesLink(), trainingTime, currentConfig.getModelSize());

                database.sendMetrics(metric);

                runOnUiThread(() -> {

                    tvStatus.setText("Training Completed. Time: " + trainingTime + "ms");
                    progressBar.setVisibility(View.GONE);
                    adapter.updateLastTrainingInfo(getLastTrainingInfo());
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during training", e);
                runOnUiThread(() -> {
                    tvStatus.setText("Error during training");
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void saveToHistory(long trainingTime, double energy, double sampleEnergy, double sampleTime) {
        currentConfig.setTime((int) trainingTime);
        currentConfig.setEnergy(energy);
        currentConfig.setSampleEnergy(sampleEnergy);
        currentConfig.setSampleTime(sampleTime);

        HistoryManager historyManager = new HistoryManager(this);
        List<ModelConfig> configs = historyManager.readJsonFileToList();
        if (configs == null) {
            configs = new ArrayList<>();
        }
        configs.add(0, currentConfig);
        historyManager.saveListToJsonFile(configs);
        historyManager.printJsonFileContent();
    }

    public String getLastTrainingInfo() {
        HistoryManager historyManager = new HistoryManager(this);
        List<ModelConfig> configs = historyManager.readJsonFileToList();
        if (configs == null || configs.isEmpty()) {
            return "";
        }
        ModelConfig config = configs.get(0);

        return"<font color='#4CAF50'>Epochs:</font> " + config.getEpochs() + "<br>" +
                "<font color='#4CAF50'>Batches:</font> " + config.getBatches() + "<br>" +
                "<font color='#4CAF50'>Batch Size:</font> " +config.getBatchSize() + "<br>" +
                "<font color='#4CAF50'>Dimensions:</font> " + config.getDimensions() + "<br>" +
                "<font color='#4CAF50'>Time:</font> " + config.getTime() + " ms<br>" +
                "<font color='#4CAF50'>Time per sample:</font> " + String.format("%.3f",config.getSampleTime()) + " ms<br>" +
                "<font color='#4CAF50'>Total Energy:</font> " + String.format("%.4f",config.getEnergy()) + " J<br>" +
                "<font color='#4CAF50'>Energy per sample:</font> " + String.format("%.6f",config.getSampleEnergy()) + " J<br>"+
                "<font color='#4CAF50'>Model Size:</font>" + String.format("%d",config.getModelSize()) + " Bytes";

    }

}

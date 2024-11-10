package com.example.myapplication;// CardAdapter.java

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private MainActivity mainActivity;
    private final Context context;
    private ViewPager2 viewPager; // Reference to ViewPager2
    private String modelUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/model_mnist_fixed_batch_size.tflite";
    private String featuresUrl = "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_feats.bin";
    private String labelsUrl= "https://github.com/MatheusDCasanova/OnDeviceTraning/raw/refs/heads/master/mnist_labels.bin";
    private String lastTrainingInfo = "";
    private String configsString = "";

    private List<String> titles = Arrays.asList("Model", "Dataset", "Configurations", "Last Training info");
    private List<String> contents = Arrays.asList("Content for card 1", "Content for card 2", "no info", "no info");
    private List<String> buttonNames = Arrays.asList("set Model", "set Dataset", "set Configurations", "show History");

    public FileDownloader fileDownloader;

    public CardAdapter(ViewPager2 viewPager, Context context, MainActivity mainActivity, TextView tvStatus) {
        this.viewPager = viewPager; // Initialize ViewPager2 reference
        this.context = context;
        this.mainActivity = mainActivity;
        fileDownloader = new FileDownloader(context, mainActivity, tvStatus, mainActivity.downloadProgressBar);
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.titleTextView.setText(titles.get(position));
        holder.contentTextView.setText(contents.get(position));
        holder.nextButton.setText(buttonNames.get(position));

        switch (position) {
            case 0:
                holder.contentTextView.setText(Html.fromHtml("<a href=" +modelUrl+ ">model</a>\n", Html.FROM_HTML_MODE_LEGACY));
                holder.nextButton.setOnClickListener(v -> {
                    showModelDialog(holder);
                });
                holder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
                break;
            case 1:
                String formattedText = "labels URL: <a href=" +labelsUrl+ ">labels</a><br>"
                        + "features URL: <a href=" +featuresUrl+ ">features</a>";
                holder.contentTextView.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY));
                holder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
                holder.nextButton.setOnClickListener(v -> {
                    showDatasetDialog(holder);
                });
                break;
            case 2:
                setConfigurationsTextView(holder);
                holder.contentTextView.setText(Html.fromHtml(configsString, Html.FROM_HTML_MODE_LEGACY));
                holder.nextButton.setOnClickListener(v -> {
                    showConfigurationsDialog(holder);
                });
                break;
            case 3:
                lastTrainingInfo = this.mainActivity.getLastTrainingInfo();
                holder.contentTextView.setText(Html.fromHtml(lastTrainingInfo, Html.FROM_HTML_MODE_LEGACY));
                holder.nextButton.setOnClickListener(v -> {
                    this.mainActivity.showHistory();
                });
        }
    }

    public void updateLastTrainingInfo(String info) {
        this.lastTrainingInfo = info;
        notifyDataSetChanged();
    }

    private void showDatasetDialog(CardViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context, R.style.AlertDialogTheme);
        builder.setTitle("Set Dataset");
        LinearLayout layout = createDatasetLayout();
        builder.setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    updateDataset(layout);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
        setDatasetTextView(holder);
    }

    private void updateDataset(LinearLayout layout) {
        fileDownloader.downloadFile(featuresUrl, "Features", false, null);
        fileDownloader.downloadFile(labelsUrl, "Labels", false, null);
    }

    private void setDatasetTextView(CardViewHolder holder) {
        String formattedText = "labels URL: <a href=" +labelsUrl+ ">labels</a><br>"
                + "features URL: <a href=" +featuresUrl+ ">features</a>";
        holder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        holder.contentTextView.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY));
    }

    private LinearLayout createDatasetLayout() {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        layout.addView(createLabel("Features URL:"));
        layout.addView(createStringInput(featuresUrl));
        layout.addView(createLabel("Labels URL:"));
        layout.addView(createStringInput(labelsUrl));


        return layout;
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, contentTextView;
        Button nextButton;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            nextButton = itemView.findViewById(R.id.nextButton);
        }
    }

    private void showConfigurationsDialog(CardViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context, R.style.AlertDialogTheme);
        builder.setTitle("Set Configuration");
        LinearLayout layout = createConfigurationsLayout();
        builder.setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    updateConfigurations(layout);
                    setConfigurationsTextView(holder);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
        setConfigurationsTextView(holder);
    }

    private void showModelDialog(CardViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context, R.style.AlertDialogTheme);
        builder.setTitle("Set Model");
        LinearLayout layout = createModelLayout();
        builder.setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    updateModel(layout);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
        setModelTextView(holder);
    }

    private void setModelTextView(CardViewHolder holder) {
        String formattedText = "<a href=" +modelUrl+ ">model</a>\n";
        holder.contentTextView.setMovementMethod(LinkMovementMethod.getInstance());
        holder.contentTextView.setText(Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY));
    }

    private LinearLayout createModelLayout() {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        layout.addView(createLabel("Model URL:"));
        layout.addView(createStringInput(modelUrl));

        return layout;
    }

    private void updateModel(LinearLayout layout) {
        modelUrl = ((EditText) layout.getChildAt(1)).getText().toString();
        this.mainActivity.currentConfig.setModelLink(modelUrl);
        this.mainActivity.modelFile = new File(this.context.getFilesDir(), "model.tflite");
        fileDownloader.downloadFile(modelUrl, "Model", true, this.mainActivity.modelFile);
    }

    private LinearLayout createConfigurationsLayout() {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        layout.addView(createLabel("Number of epochs:"));
        layout.addView(createNumberInput(this.mainActivity.NUM_EPOCHS));

        layout.addView(createLabel("Number of batches:"));
        layout.addView(createNumberInput(this.mainActivity.NUM_BATCHES));

        layout.addView(createLabel("Batch Size:"));
        layout.addView(createNumberInput(this.mainActivity.BATCH_SIZE));

        layout.addView(createLabel("Feature Dimensions:"));
        layout.addView(createNumberInputList(this.mainActivity.dimensions));

        return layout;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this.context);
        label.setText(text);
        label.setTextSize(18);
        label.setTextColor(Color.WHITE);
        label.setPadding(0, 10, 0, 5);
        return label;
    }

    private EditText createNumberInput(int value) {
        EditText input = new EditText(this.context);
        input.setText(String.valueOf(value));
        applyEditTextStyle(input);
        return input;
    }

    private EditText createNumberInputList(List<Integer> values) {
        EditText input = new EditText(this.context);
        input.setText(values.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        applyEditTextStyle(input);
        return input;
    }

    private EditText createStringInput(String value) {
        EditText input = new EditText(this.context);
        input.setText(value);
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

    private void setConfigurationsTextView(CardViewHolder holder) {
        configsString = "<b>Configuration Details</b><br>" +
                "<font color='#4CAF50'>Epochs:</font> " + this.mainActivity.NUM_EPOCHS + "<br>" +
                "<font color='#4CAF50'>Batches:</font> " + this.mainActivity.NUM_BATCHES + "<br>" +
                "<font color='#4CAF50'>Batch Size:</font> " + this.mainActivity.BATCH_SIZE + "<br>" +
                "<font color='#4CAF50'>Dimensions:</font> " + TypeConverter.listToString(this.mainActivity.dimensions);
        holder.contentTextView.setText(Html.fromHtml(configsString, Html.FROM_HTML_MODE_LEGACY));
    }


    private void updateConfigurations(LinearLayout layout) {
        this.mainActivity.currentConfig.setEpochs(Integer.parseInt(((EditText) layout.getChildAt(1)).getText().toString()));
        this.mainActivity.currentConfig.setBatches(Integer.parseInt(((EditText) layout.getChildAt(3)).getText().toString()));
        this.mainActivity.currentConfig.setBatchSize(Integer.parseInt(((EditText) layout.getChildAt(5)).getText().toString()));
        this.mainActivity.currentConfig.setDimensions(TypeConverter.stringToList(((EditText) layout.getChildAt(7)).getText().toString()));
    }

}
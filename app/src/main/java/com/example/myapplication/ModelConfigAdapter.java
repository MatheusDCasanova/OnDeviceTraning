package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ModelConfigAdapter extends RecyclerView.Adapter<ModelConfigAdapter.ViewHolder> {
    private List<ModelConfig> modelConfigs;

    public ModelConfigAdapter(List<ModelConfig> modelConfigs) {
        this.modelConfigs = modelConfigs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelConfig modelConfig = modelConfigs.get(position);
        holder.epochsText.setText("Epochs: " + modelConfig.getEpochs());
        holder.batchesText.setText("Batches: " + modelConfig.getBatches());
        holder.batchSizeText.setText("Batch Size: " + modelConfig.getBatchSize());
        holder.dimensionsText.setText("Dimensions: " + modelConfig.getDimensions());
        holder.modelLinkText.setText("Model Link: " + modelConfig.getModelLink());
        holder.datasetLinkText.setText("Dataset Link: " + modelConfig.getDatasetLink());
        holder.timeText.setText("Time: " + modelConfig.getTime() + " seconds");
    }

    @Override
    public int getItemCount() {
        return modelConfigs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView epochsText;
        TextView batchesText;
        TextView batchSizeText;
        TextView dimensionsText;
        TextView modelLinkText;
        TextView datasetLinkText;
        TextView timeText;

        ViewHolder(View itemView) {
            super(itemView);
            epochsText = itemView.findViewById(R.id.epochs_text);
            batchesText = itemView.findViewById(R.id.batches_text);
            batchSizeText = itemView.findViewById(R.id.batch_size_text);
            dimensionsText = itemView.findViewById(R.id.dimensions_text);
            modelLinkText = itemView.findViewById(R.id.model_link_text);
            datasetLinkText = itemView.findViewById(R.id.dataset_link_text);
            timeText = itemView.findViewById(R.id.time_text);
        }
    }
}

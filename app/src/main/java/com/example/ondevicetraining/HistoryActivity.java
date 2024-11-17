package com.example.ondevicetraining;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private Button btnBack; // Declare the button here
    private RecyclerView recyclerView;
    private HistoryManager historyManager;
    private ModelConfigAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history); // Set the content view here
        // Sample data
        List<ModelConfig> modelConfigs = new ArrayList<>();
        historyManager = new HistoryManager(this);
        modelConfigs = historyManager.readJsonFileToList();

        // Add more ModelConfig objects as needed...

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelConfigAdapter(modelConfigs);
        recyclerView.setAdapter(adapter);
        // Initialize the button after setting the content view
        btnBack = findViewById(R.id.back_button);
        ImageButton moreOptionsButton = findViewById(R.id.button_more_options);
        moreOptionsButton.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.share_history:
                        shareHistory();
                        return true;
                    case R.id.clear_history:
                        historyManager.saveListToJsonFile(new ArrayList<>());
                        adapter.notifyDataSetChanged();
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        });


        // Set the onClickListener for the button
        btnBack.setOnClickListener(v -> {
            backPressed(); // Call the showHistory() method when the button is clicked
        });
    }

    private void backPressed() {
        Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void shareHistory() {
        File jsonFile = new File(getFilesDir(), "history.json"); // Specify your JSON file

        if (jsonFile.exists()) {
            // Use FileProvider to get the URI for the file
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", jsonFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri); // Attach the file URI
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared History Data");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission to read the file
            startActivity(Intent.createChooser(shareIntent, "Share History via"));
        } else {
            // Handle the case where the file does not exist
            Toast.makeText(this, "History file not found!", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String FILE_NAME = "history.json";

    private Context context;

    // Constructor to pass the context
    public HistoryManager(Context context) {
        this.context = context;
    }

    public List<ModelConfig> readJsonFileToList() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(getFilePath())) {
            Type listType = new TypeToken<List<ModelConfig>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            Log.e("HistoryManager", "Error reading JSON file", e);
            return new ArrayList<>();
        }
    }

    public void saveListToJsonFile(List<ModelConfig> list) {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(getFilePath())) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            Log.e("HistoryManager", "Error saving JSON file", e);
        }
    }

    public void deleteJsonFile() {
        File file = new File(getFilePath());
    }

    private String getFilePath() {
        // Get the internal storage directory for the app
        File dir = context.getFilesDir();
        Log.d("path", " getFilePath: " + dir.getAbsolutePath());
        return new File(dir, FILE_NAME).getAbsolutePath();
    }

    public void printJsonFileContent() {
        try (FileReader reader = new FileReader(getFilePath())) {
            StringBuilder jsonContent = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                jsonContent.append((char) character);
            }
            Log.d("json", "JSON File Content: " + jsonContent.toString());
        } catch (IOException e) {
            Log.e("json", "Error reading JSON file", e);
        }
    }
}

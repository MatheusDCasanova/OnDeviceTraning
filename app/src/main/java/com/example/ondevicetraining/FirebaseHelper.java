package com.example.ondevicetraining;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    private DatabaseReference database;

    public FirebaseHelper() {
        // Get a reference to the database
        database = FirebaseDatabase.getInstance().getReference("trainingMetrics");
    }

    public void sendMetrics(EnergyMetric metric) {
        // Create a unique key for each metric entry
        String metricId = database.push().getKey();

        // Save the metric to the database
        if (metricId != null) {
            database.child(metricId).setValue(metric)
                    .addOnSuccessListener(aVoid -> Log.d("FirebaseHelper", "Metrics successfully sent!"))
                    .addOnFailureListener(e -> Log.e("FirebaseHelper", "Error sending metrics", e));
        }
    }
}

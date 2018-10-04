package com.example.bringeraj.downloadmap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String JSON_CHARSET = "UTF-8";
    private static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    private OfflineManager offlineManager;
    private ArrayList<OfflineRegion> offlineRegionsList = new ArrayList<>();
    private TextView tvName, tvPercentage;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        offlineManager = OfflineManager.getInstance(this);
        tvName = findViewById(R.id.tv_name);
        tvPercentage = findViewById(R.id.tv_percentage);
        btnNext = findViewById(R.id.btn_next);
        btnNext.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getRegionsList();
    }

    private void getRegionsList() {
        // Query the DB asynchronously
        if (offlineManager != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                        @Override
                        public void onList(final OfflineRegion[] offlineRegions) {
                            offlineRegionsList.clear();
                            if (offlineRegions != null && offlineRegions.length > 0) {
                                offlineRegionsList.addAll(Arrays.asList(offlineRegions));
                            }
                            // Switch to main thread
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (offlineRegions != null) {
                                        int length = offlineRegions.length;
                                        if(length > 0) {
                                            OfflineRegion offlineRegion = offlineRegionsList.get(length - 1);
                                            tvName.setText(getRegionName(offlineRegion));

                                            offlineRegion.getStatus(new OfflineRegion.OfflineRegionStatusCallback() {
                                                @Override
                                                public void onStatus(OfflineRegionStatus status) {
                                                    // Calculate the download percentage
                                                    double percentage = status.getRequiredResourceCount() >= 0
                                                            ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                                                            0.0;
                                                    Integer percentageDownloaded = (int) Math.round(percentage);
                                                    if (status.isComplete()) {
                                                        tvPercentage.setText(percentageDownloaded + " ");
                                                    } else if (status.isRequiredResourceCountPrecise()) {
                                                        tvPercentage.setText(percentageDownloaded + " ");
                                                    }
                                                }

                                                @Override public void onError(String error) {

                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(TAG, "Error: " + error);
                                }
                            });
                        }
                    });
                }
            }).start();
        }
    }

    private String getRegionName(OfflineRegion offlineRegion) {
        // Get the region name from the offline region metadata
        String regionName;
        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, JSON_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to decode metadata: " + exception.getMessage());
            regionName = "Id - " + offlineRegion.getID();
        }
        return regionName;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(MainActivity.this, SimpleOfflineMapActivity.class);
        startActivity(intent);
    }
}

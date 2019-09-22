package com.example.scorpion.myservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.Date;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "myLogs";

    static final String ACTION_PROCESS_UPDATES = "com.example.scorpion.myservice.action.PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.


        if(intent != null){
            final String action = intent.getAction();

            if(ACTION_PROCESS_UPDATES.equals(action)){
                LocationResult result = LocationResult.extractResult(intent);
                if(result != null){
                    MyService.setLocations(result.getLocations());  //протестить!!!!!!!!!
                    for(Location location : result.getLocations()){
                        Log.d(LOG_TAG, "lat: " + location.getLatitude() + " long: " + location.getLongitude());

                        Date date = new Date(location.getTime());
                        Log.d(LOG_TAG, "date: " + date);
                        Log.d(LOG_TAG, " ");
                    }
                }
            }
        }
    }
}

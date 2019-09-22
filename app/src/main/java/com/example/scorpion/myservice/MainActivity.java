package com.example.scorpion.myservice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnStart;
    Button btnStop;

    private static final int REQUEST_ID_MULTIPLE_PERMISSION = 1;

    private static final String LOG_TAG = "myLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        checkAndRequestPermission(Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnStart:
                startService(new Intent(this, MyService.class));
                break;
            case R.id.btnStop:
                stopService(new Intent(this, MyService.class));
                break;
        }
    }

    public void checkAndRequestPermission(String ... permissions){
        List<String> listPermissionsNeeded = new ArrayList<>();
        for(String permission : permissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(permission);
            }
        }
        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                                                REQUEST_ID_MULTIPLE_PERMISSION);
        }
    }

}

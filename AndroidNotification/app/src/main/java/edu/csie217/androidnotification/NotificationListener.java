package edu.csie217.androidnotification;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class NotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();

    private void showToast(final String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "notificationListenerEnable = " + notificationListenerEnabled());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        String macAddress = intent.getStringExtra("macAddress");
        Log.i(TAG, macAddress);
        connectAsClient(macAddress);
        return res;
    }

    private boolean notificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (flat == null) return false;
        return flat.contains(getClass().getName());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "onNotificationPosted");
        String content = sbn.getNotification().tickerText.toString();

        final PackageManager pm = getApplicationContext().getPackageManager();
        String applicationName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(sbn.getPackageName(), 0);
            applicationName = (String) pm.getApplicationLabel(ai);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationName = "";
        }

        Log.i(TAG, content);
        Log.i(TAG, applicationName);


        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("title", "");
            jsonData.put("content", content);
            jsonData.put("app_name", applicationName);
        } catch (JSONException e) {
            Log.e(TAG, "json error");
            return;
        }
        try {
            String text = jsonData.toString();
            outStream.write(text.getBytes("UTF-8"));
        } catch (IOException e) {
            Log.i(TAG, "cannot write to outStream");
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    private BluetoothAdapter bluetoothAdapter;

    void connectAsClient(final String serverMacAddress) {
        Log.i(TAG, "connect as client");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(serverMacAddress);
        connectAsClient(device);
    }

    void connectAsClient(BluetoothDevice device) {
        Log.i(TAG, device.getName() + " (" + device.getAddress() + ")");
        showToast("Try to connect to device: " + device.getName());
        BluetoothSocket socket = null;
        try {
            Log.i(TAG, UUID.randomUUID().toString());
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(getString(R.string.bluetooth_service_uuid)));
        } catch (IOException e) {
            Log.i(TAG, "failed to create socket.");
            showToast("Connection failed");
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        try {
            socket.connect();
        } catch (IOException connectException) {
            Log.i(TAG, "cannot connect.");
            showToast("Connection failed");
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.i(TAG, "cannot close the client socket.");
            }
            return;
        }
        Log.i(TAG, "successfully connect to " + device.getAddress());
        showToast("Successfully connect to: " + device.getName());
        manageConnection(socket);
    }

    InputStream inStream = null;
    OutputStream outStream = null;

    void manageConnection(BluetoothSocket socket) {
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e) {
            showToast("Bluetooth connection error");
            Log.i(TAG, "cannot get input or output stream");
        }
    }
}

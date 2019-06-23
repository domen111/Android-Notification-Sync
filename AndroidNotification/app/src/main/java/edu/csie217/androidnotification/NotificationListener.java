package edu.csie217.androidnotification;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class NotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();

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
        Log.i(TAG, "**********  onNotificationPosted");
        Log.i(TAG, "ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
        Log.i(TAG, sbn.getNotification().extras.getString("android.title"));

        final PackageManager pm = getApplicationContext().getPackageManager();
        String applicationName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(sbn.getPackageName(), 0);
            applicationName = (String) pm.getApplicationLabel(ai);
        } catch (final PackageManager.NameNotFoundException e) {
            applicationName = "";
        }
        Log.i(TAG, applicationName);


        try {
            String text = applicationName + sbn.getNotification().tickerText;
            outStream.write(text.getBytes("UTF-8"));
        } catch (IOException e) {
            Log.i(TAG, "cannot write to outStream");
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "********** onNOtificationRemoved");
        Log.i(TAG, "ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
    }

    private BluetoothAdapter bluetoothAdapter;

    void connectAsClient(final String serverMacAddress) {
        Log.i(TAG, "connect as client");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(serverMacAddress);
        connectAsClient(device);
    }

    void connectAsClient(BluetoothDevice device) {
        Log.i(TAG, device.getName() + " (" + device.getAddress() + ")");
        BluetoothSocket socket = null;
        try {
            Log.i(TAG, UUID.randomUUID().toString());
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(getString(R.string.bluetooth_service_uuid)));
        } catch (IOException e) {
            Log.i(TAG, "failed to create socket.");
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        try {
            socket.connect();
        } catch (IOException connectException) {
            Log.i(TAG, "cannot connect.");
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.i(TAG, "cannot close the client socket.");
            }
            return;
        }
        Log.i(TAG, "successfully connect to " + device.getAddress());
        manageConnection(socket);
    }

    InputStream inStream = null;
    OutputStream outStream = null;

    void manageConnection(BluetoothSocket socket) {
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.i(TAG, "cannot get input or output stream");
        }
    }
}

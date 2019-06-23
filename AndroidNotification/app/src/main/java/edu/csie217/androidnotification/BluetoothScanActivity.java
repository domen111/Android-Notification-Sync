package edu.csie217.androidnotification;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BluetoothScanActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    List<String> devicesMacAddress = new ArrayList<>();
    ArrayAdapter<String> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_scan);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Register bluetooth discovering intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDiscoveringReceiver, filter);

        ListView deviceListView = findViewById(R.id.deviceList);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener((AdapterView<?> adapterView,
                                               View view,
                                               int position,
                                               long l) -> {
            String macAddress = devicesMacAddress.get(position);
            Intent intent = new Intent(BluetoothScanActivity.this, NotificationListener.class);
            intent.putExtra("macAddress", macAddress);
            startService(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothDiscoveringReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void scanFabOnClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        if (checkAndEnableBluetooth()) {
            scanAllDevices();
        }
    }

    void showToast(final String text) {
        final Activity thisActivity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(thisActivity, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }


    private static final int REQUEST_ENABLE_BT = 2;
    BluetoothAdapter bluetoothAdapter;

    private boolean checkAndEnableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("bluetooth is not supported");
            return false;
        } else {
            Log.i(TAG, "successfully get default adapter");
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        } else {
            return true;
        }
    }

    void scanAllDevices() {
        queryPairedDevices();
        startDiscovery();
    }

    void queryPairedDevices() {
        Log.i(TAG, "query pairs...");
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null) {
            showToast("Error on queryPairedDevices");
            return;
        }
        Log.i(TAG, pairedDevices.size() + " paired devices.");
        for (BluetoothDevice device : pairedDevices) {
            addDevice(device);
        }
    }

    void startDiscovery() {
        showToast("discovering...");
        bluetoothAdapter.cancelDiscovery();
        requestBluetoothPermission();
        if (!bluetoothAdapter.startDiscovery()) {
            showToast("discovery failed");
            finish();
        } else {
            showToast("discovery success");
        }
    }

    // https://stackoverflow.com/questions/34966133/android-bluetooth-discovery-doesnt-find-any-device
    void requestBluetoothPermission() {
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }

    private final BroadcastReceiver bluetoothDiscoveringReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDevice(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showToast("discovery finished.");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                showToast("discovery started.");
            }
        }
    };

    void addDevice(final BluetoothDevice device) {
        String deviceName = device.getName();
        String deviceHardwareAddress = device.getAddress();
        if (deviceName == null)
            return;
        devicesMacAddress.add(deviceHardwareAddress);
        deviceListAdapter.add(deviceName);
//        Button button = new Button(this);
//        button.setText(deviceName + ": " + deviceHardwareAddress);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Button b = (Button) view;
//                //connectAsClient(device);
//                String macAddress = device.getAddress();
//                Intent intent = new Intent(BluetoothScanActivity.this, NotificationListener.class);
//                intent.putExtra("macAddress", macAddress);
//                startService(intent);
//            }
//        });
//        linearLayout.addView(button);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                showToast("you need to enable bluetooth in order to continue.");
                finish();
            } else {
                showToast("bluetooth enabled");
                scanAllDevices();
            }
        }
    }
}

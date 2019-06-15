package com.example.myapplication;

//import java.util.concurrent.*;
import java.util.*;
import java.lang.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.*;
import android.os.Bundle;
import android.content.*;
import android.view.*;
import android.widget.*;

public class BluetoothServer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_server);
        editText=findViewById(R.id.editText);
        linearLayout=findViewById(R.id.linearLayout);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
    }
    EditText editText;
    LinearLayout linearLayout;
    void showToast(String text){
        Toast toast=Toast.makeText(this,text,Toast.LENGTH_SHORT);
        toast.show();
        editText.append(text+"\n");
    }
    BluetoothAdapter bluetoothAdapter;
    void Stage1(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){showToast("bluetooth not supported");return;}
        else {showToast("get default adapter success");}
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Stage2();
        }
    }
    void Stage2(){
        showToast("query pairs...");
        Set<BluetoothDevice>pairedDevices=bluetoothAdapter.getBondedDevices();
        showToast(pairedDevices.size()+" paired devices.");
        if(pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                String deviceName=device.getName();
                String deviceHardwareAddress=device.getAddress();
                showToast(deviceName+": "+deviceHardwareAddress);
                addDevice(deviceName,deviceHardwareAddress);
            }
        }
        Stage3();
    }
    // https://stackoverflow.com/questions/34966133/android-bluetooth-discovery-doesnt-find-any-device
    void requestBluetoothPermission(){
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }
    void Stage3(){
        showToast("discovering...");
        bluetoothAdapter.cancelDiscovery();
        requestBluetoothPermission();
        if(!bluetoothAdapter.startDiscovery()){
            showToast("discovery failed.");
            finish();
        } else {
            showToast("discovery success.");
        }
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//        startActivity(discoverableIntent);
    }
    void addDevice(String deviceName,String deviceHardwareAddress){
        Button button=new Button(this);
        button.setText(deviceName+": "+deviceHardwareAddress);
        linearLayout.addView(button);
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                showToast(deviceName+": "+deviceHardwareAddress);
                addDevice(deviceName,deviceHardwareAddress);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                showToast("discovery finished.");
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                showToast("discovery started.");
            }
        }
    };
    private static final int REQUEST_ENABLE_BT = 2;
    void onButtonClick(View view){
        showToast("clicked");
        Stage1();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode != RESULT_OK){
                showToast("you need to enable bluetooth in order to continue.");
                finish();
            }else{
                showToast("bluetooth enabled");
                Stage2();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }
}

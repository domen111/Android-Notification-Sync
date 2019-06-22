package com.example.myapplication;

//import java.util.concurrent.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.lang.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.os.Bundle;
import android.content.*;
import android.view.*;
import android.widget.*;

public class BluetoothClientActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_client);
        editText=findViewById(R.id.editText);
        linearLayout=findViewById(R.id.linearLayout);
        setOnSendButtonClick();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
    }
    void setOnSendButtonClick(){
        Button sendButton=findViewById(R.id.button3);
        sendButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                String text=((EditText)BluetoothClientActivity.this.findViewById(R.id.editText3)).getText().toString();
                showToast("you typed: "+text);
                try{
                    outStream.write(text.getBytes("UTF-8"));
                }catch (IOException e){
                    showToast("cannot write to outStream");
                }
            }
        });
    }
    EditText editText;
    LinearLayout linearLayout;
    void showToast(final String text){
        final Activity thisActivity=this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast=Toast.makeText(thisActivity,text,Toast.LENGTH_SHORT);
                toast.show();
                editText.append(text+"\n");
            }
        });
    }
    BluetoothAdapter bluetoothAdapter;
    void turnOnBluetooth(){
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){showToast("bluetooth not supported");return;}
        else {showToast("get default adapter success");}
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            queryPairedDevices();
        }
    }
    void queryPairedDevices(){
        showToast("query pairs...");
        Set<BluetoothDevice>pairedDevices=bluetoothAdapter.getBondedDevices();
        showToast(pairedDevices.size()+" paired devices.");
        if(pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                showToast(device.getName()+": "+device.getAddress());
                addDevice(device);
            }
        }
        startDiscovery();
    }
    // https://stackoverflow.com/questions/34966133/android-bluetooth-discovery-doesnt-find-any-device
    void requestBluetoothPermission(){
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }
    // discovery
    void startDiscovery(){
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
    void addDevice(final BluetoothDevice device){
        String deviceName=device.getName();
        String deviceHardwareAddress=device.getAddress();
        Button button=new Button(this);
        button.setText(deviceName+": "+deviceHardwareAddress);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button b=(Button)view;
                connectAsClient(device);
            }
        });
        linearLayout.addView(button);
    }
    void connectAsClient(final BluetoothDevice device){
        new Thread(){
            @Override
            public void run(){
                connectAsClientNaive(device);
            }
        }.start();
    }
    void connectAsClientNaive(BluetoothDevice device){
        showToast(device.getName()+" ("+device.getAddress()+")");
        BluetoothSocket socket=null;
        try{
            showToast(UUID.randomUUID().toString());
            socket=device.createRfcommSocketToServiceRecord(UUID.fromString(getString(R.string.bluetooth_service_uuid)));
        } catch (IOException e){
            showToast("failed to create socket.");
            return;
        }
        bluetoothAdapter.cancelDiscovery();
        try{
            socket.connect();
        } catch (IOException connectException) {
            showToast("cannot connect.");
            try {
                socket.close();
            } catch (IOException closeException) {
                showToast("cannot close the client socket.");
            }
            return;
        }
        showToast("successfully connect to "+device.getAddress());
        manageConnection(socket);
    }
    InputStream inStream =null;
    OutputStream outStream =null;
    void manageConnection(BluetoothSocket socket){
        try{
            inStream =socket.getInputStream();
            outStream =socket.getOutputStream();
        }catch (IOException e){
            showToast("cannot get input or output stream");
            return;
        }
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                showToast(device.getName()+": "+device.getAddress());
                addDevice(device);
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
        turnOnBluetooth();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode != RESULT_OK){
                showToast("you need to enable bluetooth in order to continue.");
                finish();
            }else{
                showToast("bluetooth enabled");
                queryPairedDevices();
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

package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.*;
import java.util.*;

public class BluetoothServerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_server);
        editText=findViewById(R.id.editText);
        linearLayout=findViewById(R.id.linearLayout);
        setOnSendButtonClick();
        readInStream();
    }
    void setOnSendButtonClick(){
        Button sendButton=findViewById(R.id.button2);
        sendButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view){
                EditText txtBox=BluetoothServerActivity.this.findViewById(R.id.editText2);
                String text=txtBox.getText().toString();
                txtBox.setText("");
                showToast("you typed: "+text);
                try{
                    outStream.write(text.getBytes("UTF-8"));
                }catch (IOException e){
                    showToast("cannot write to outStream");
                }
            }
        });
    }
    void readInStream(){
        new Thread(){
            @Override
            public void run(){
                byte[]buffer=new byte[1024];
                while(true){
                    if(inStream==null){
                        try {
                            Thread.sleep(100);
                        }catch (InterruptedException e){}
                        continue;
                    }
                    int sz=0;
                    try{
                        sz=inStream.read(buffer);
                    }catch (IOException e){
                        showToast("cannot read from inStream");
                    }
                    if(sz!=0){
                        String str=null;
                        try {
                            str = new String(buffer, "UTF-8");
                        }catch (UnsupportedEncodingException e){
                            showToast("unsupported encoding exception");
                        }
                        if(str!=null){
                            showToast("received: "+str);
                        }
                    }
                }
            }
        }.start();
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
            enableDiscoverability();
        }
    }
    void enableDiscoverability(){
        Intent discoverableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivityForResult(discoverableIntent, ENABLE_DISCOVERABILITY_REQUEST_CODE);
    }
    final int ENABLE_DISCOVERABILITY_REQUEST_CODE =0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ENABLE_DISCOVERABILITY_REQUEST_CODE){
            if(resultCode == RESULT_CANCELED){
                showToast("you need to enable bluetooth in order to continue.");
                finish();
            }else{
                showToast("you're now in discoverable mode");
                connectAsServer();
            }
        }
    }
    // https://stackoverflow.com/questions/34966133/android-bluetooth-discovery-doesnt-find-any-device
    void requestBluetoothPermission(){
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }
    void connectAsServer() {
        new Thread(){
            @Override
            public void run(){
                connectAsServerNaive();
            }
        }.start();
    }
    InputStream inStream =null;
    OutputStream outStream =null;
    void connectAsServerNaive(){
        BluetoothServerSocket serverSocket=null;
        try{
            serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord("My Application",UUID.fromString(getString(R.string.bluetooth_service_uuid)));
        } catch (IOException e) {
            showToast("cannot listen.");
            return;
        }
        BluetoothSocket socket=null;
        while(true){
            try{
                socket=serverSocket.accept();
            }catch (IOException e){
                showToast("failed to accept.");
                break;
            }
            if(socket!=null){
                try {
                    serverSocket.close();
                    inStream=socket.getInputStream();
                    outStream=socket.getOutputStream();
                    showToast("server socket closed.");
                }
                catch (Exception e){
                    showToast("failed to close");
                }
                break;
            }
        }
    }
    private static final int REQUEST_ENABLE_BT = 2;
    void onButtonClick(View view){
        showToast("clicked");
        turnOnBluetooth();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

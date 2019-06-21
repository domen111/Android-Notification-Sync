package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.content.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        final Button button=findViewById(R.id.button);
    }
    int counter=0;
    void clientButtonOnClick(View view){
        Button button=(Button)view;
        button.setText("clicked #"+Integer.toString(++counter));
        Intent intent=new Intent(this, BluetoothClient.class);
        startActivity(intent);
    }
    void serverButtonOnClick(View view){
        Button button=(Button)view;
        button.setText("clicked #"+Integer.toString(++counter));
        Intent intent=new Intent(this, BluetoothServer.class);
        startActivity(intent);
    }
}

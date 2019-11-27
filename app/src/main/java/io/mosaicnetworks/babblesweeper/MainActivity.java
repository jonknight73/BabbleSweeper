package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.mosaicnetworks.babble.node.KeyPair;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "BabbleSweeper-NOV27";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



    public void startNewNetworkClick(View view) {
        Intent intent = new Intent(this, NewNetworkActivity.class);
        Log.i(TAG, "startNewNetworkClick");
        startActivity(intent);
    }


    public void joinNetworkClick(View view) {
        Intent intent = new Intent(this, JoinNetworkActivity.class);
        Log.i(TAG, "joinNetworkClick");
        startActivity(intent);
    }



}

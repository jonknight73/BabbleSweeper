package io.mosaicnetworks.babblesweeper;


import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import io.mosaicnetworks.babble.configure.BaseConfigActivity;
import io.mosaicnetworks.babble.node.BabbleService;


public class MainActivity extends BaseConfigActivity {

    public static final String TAG = "BabbleSweeper-NOV27";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);


    }

    @Override
    public BabbleService getBabbleService() {
        return MessagingService.getInstance();
    }

    @Override
    public void onJoined(String moniker) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MONIKER", moniker);
        startActivity(intent);
    }

    @Override
    public void onStartedNew(String moniker) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MONIKER", moniker);
        startActivity(intent);
    }


}

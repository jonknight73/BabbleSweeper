package io.mosaicnetworks.babblesweeper;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.mosaicnetworks.babble.configure.BaseConfigActivity;
import io.mosaicnetworks.babble.node.BabbleService;


public class MainActivity extends BaseConfigActivity {

    public static final String TAG = "BabbleSweeper-NOV27";


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

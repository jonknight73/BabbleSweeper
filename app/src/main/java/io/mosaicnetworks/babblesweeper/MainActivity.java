package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import io.mosaicnetworks.babble.node.KeyPair;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "BabbleSweeper-NOV25";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



//TODO remove this - it was the old pre-babble invocation code

    public void startGameClick(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        Log.i(TAG, "Start Game Click");

   //     EditText editText = (EditText) findViewById(R.id.editText);
   //     intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
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






    public void btTestClick(View view) {

        KeyPair kp = new KeyPair();
        Log.i("Yippee",kp.privateKey);
    }


}

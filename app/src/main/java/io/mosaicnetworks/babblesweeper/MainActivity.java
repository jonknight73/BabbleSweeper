package io.mosaicnetworks.babblesweeper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "JK-NOV21";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    public void startGameClick(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        Log.i(TAG, "Start Game Click");

   //     EditText editText = (EditText) findViewById(R.id.editText);
   //     intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);




    }


}

package io.mosaicnetworks.babblesweeper;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class NewNetworkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_network);

        SharedPreferences sharedPref = NewNetworkActivity.this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);


        EditText editText = findViewById(R.id.editText);
        editText.setText(sharedPref.getString("moniker", "Me"));


        Log.i(MainActivity.TAG, "Moniker: " + editText.getText());
    }

    // called when the user presses the start chat button
    public void startNetworkClick(View view) {

        Log.i(MainActivity.TAG, "startNetwork");

        //get moniker
        EditText editText = findViewById(R.id.editText);
        String moniker = editText.getText().toString();
        if (moniker.isEmpty()) {
            displayOkAlertDialog(R.string.no_moniker_alert_title, R.string.no_moniker_alert_message);
            return;
        }

        MessagingService messagingService = MessagingService.getInstance();
        try {
            messagingService.configureNew(moniker, Utils.getIPAddr(this));
        } catch (IllegalStateException ex) {
            //we tried to reconfigure before a leave completed
            displayOkAlertDialog(R.string.babble_busy_title, R.string.babble_busy_message);
            return;
        }


     // Store moniker entered
        SharedPreferences sharedPref = NewNetworkActivity.this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("moniker", moniker);
        editor.commit();

        Log.i(MainActivity.TAG, "Moniker: " + moniker);


        messagingService.start();
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MONIKER", moniker);
        startActivity(intent);
    }


    private void displayOkAlertDialog(@StringRes int titleId, @StringRes int messageId) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(messageId)
                .setNeutralButton(R.string.ok_button, null)
                .create();
        alertDialog.show();
    }
}

package com.indooratlas.android.examples.sharelocation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.indooratlas.android.examples.sharelocation.sharing.LocationChannel;
import com.indooratlas.android.examples.sharelocation.sharing.LocationSource;
import com.indooratlas.android.examples.sharelocation.sharing.pubnub.PubNubLocationChannelImpl;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

public class MainActivity extends AppCompatActivity {

    private IALocationManager mLocationManager;

    private LocationChannel mLocationChannel;

    private LocationSource mIdentity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = IALocationManager.create(this);
        mLocationChannel = new PubNubLocationChannelImpl(
                getString(R.string.pub_nub_publish_key),
                getString(R.string.pub_nub_subscribe_key),
                getString(R.string.pub_num_channel_name));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.requestLocationUpdates(IALocationRequest.create(), mLocationListener);
        if (mIdentity == null) {
            askIdentity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeLocationUpdates(mLocationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.destroy();
    }

    private void askIdentity() {

        final EditText text = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(R.string.share_dialog_title)
                .setView(text)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setUpChannel(text.getText().toString());
                    }
                })
                .setCancelable(true)
                .show();

    }

    private void setUpChannel(String name) {

        if (mLocationChannel != null) {
            mLocationChannel.shutdown();
        }

    }


    private IALocationListener mLocationListener = new IALocationListenerSupport() {

    };

}

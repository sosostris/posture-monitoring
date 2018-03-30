package com.example.xujia.posturemonitor.sensornode;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.util.CustomToast;

/**
 * Created by xujia on 2018-02-20.
 */

public class PostureMonitorApplication extends Application {

    private static final String TAG = "Application";

    public static final int NUMBER_OF_SENSORNODE = 2;

    public BluetoothAdapter mBtAdapter = null;
    private BluetoothLeService mBluetoothLeService;
    public static BluetoothManager mBluetoothManager;

    @Override
    public void onCreate() {

        // Check if Bluetooth is enabled
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableIntent);
        }

        startBluetoothLeService();

        super.onCreate();

    }

    // Code to manage Service life cycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(getApplicationContext(), "Unable to initialize BluetoothLeService", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i(TAG, "BluetoothLeService disconnected");
        }
    };

    private void startBluetoothLeService() {
        boolean serviceStarted;
        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        startService(bindIntent);
        serviceStarted = bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!serviceStarted) {
            CustomToast.middleBottom(this, "Bind to BluetoothLeService failed");
        }
    }

}
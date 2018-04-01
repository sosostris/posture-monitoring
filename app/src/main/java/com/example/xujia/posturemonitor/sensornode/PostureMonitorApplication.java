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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xujia on 2018-02-20.
 */

public class PostureMonitorApplication extends Application {

    private static final String TAG = "Application";

    public static final int NUMBER_OF_SENSORNODE = 3;
    public static Map<String, String> DEVICE_LIST = null;
    public static final String[] DEVICE_ADDRESS_LIST = {"B0:B4:48:BE:18:84", "B0:B4:48:BD:0C:84", "00:07:80:2D:9E:F2"};    // last one is sensornode
    public static final String[] DEVICE_NAME_LIST = {"SN0001", "SN0002", "SN0003"};

    public BluetoothAdapter mBtAdapter = null;
    private BluetoothLeService mBluetoothLeService;
    public static BluetoothManager mBluetoothManager;

    @Override
    public void onCreate() {

        DEVICE_LIST = new HashMap<>();
        for (int i=0; i<NUMBER_OF_SENSORNODE; i++) {
            DEVICE_LIST.put(DEVICE_ADDRESS_LIST[i], DEVICE_NAME_LIST[i]);
        }

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
/**
 * Xujia Zhou. Copyright (c) 2018-02-20.
 */

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application class for PostureMonitor which contains the global variables for the application.
 */
public class PostureMonitorApplication extends Application {

    private static final String TAG = "Application";

    public static int NUMBER_OF_SENSORNODE;
    public static String JAVA_IP = null;    // 192.168.1.33
    public static int JAVA_PORT_STREAM = 8000;
    public static int JAVA_PORT_GENERAL = 8001;
    public static String MATLAB_IP = null;    // 192.168.1.150
    public static int MATLAB_PORT = 30000;
    public static String USERNAME = null;
    public static String USER_ID = null;
    public static String[] DEVICE_ADDRESS_LIST = null;
    public static String[] DEVICE_NAME_LIST = null;
    public static String[] SN_BODY_LIST = null;
    public static List<String> BODY_LIST_USER = null;
    public static String[] DEVICE_TYPE_LIST = null;

    public static Map<String, String> ADDRESS_NAME_MAP = null;
    public static Map<String, String> ADDRESS_TYPE_MAP = null;
    public static Map<String, String> ADDRESS_BODY_MAP = null;

    /**
     * The following commented out lines are for debugging purpose.
     */
    // public static int NUMBER_OF_SENSORNODE = 1;
    // public static String[] DEVICE_ADDRESS_LIST = {"00:07:80:2D:87:D5", "00:07:80:2D:88:7E"};
    // public static String[] DEVICE_ADDRESS_LIST = {"00:07:80:2D:88:7E"};
    // public static String[] DEVICE_NAME_LIST = {"SN0004", "SN0005"};
    // public static String[] DEVICE_NAME_LIST = {"SN0005"};
    // public static String[] SN_BODY_LIST = {"Back"};
    // public static List<String> BODY_LIST_USER = new ArrayList<>();
    // public static String[] DEVICE_TYPE_LIST = {"BLE"};
    // public static Map<String, String> ADDRESS_NAME_MAP = new HashMap<>();
    // public static Map<String, String> ADDRESS_TYPE_MAP = new HashMap<>();
    // public static Map<String, String> ADDRESS_BODY_MAP = new HashMap<>();

    public BluetoothAdapter mBtAdapter = null;
    private BluetoothLeService mBluetoothLeService;
    public static BluetoothManager mBluetoothManager;

    @Override
    public void onCreate() {

        /**
         * The following commented out lines are for debugging purpose.
         */
        // BODY_LIST_USER.add("Back");
        // BODY_LIST_USER.add("LeftShoulder");

        // ADDRESS_NAME_MAP.put("00:07:80:2D:87:D5", "SN0004");
        // ADDRESS_NAME_MAP.put("00:07:80:2D:88:7E", "SN0005");

        // ADDRESS_TYPE_MAP.put("00:07:80:2D:87:D5", "BLE");
        // ADDRESS_TYPE_MAP.put("00:07:80:2D:88:7E", "BLE");

        // ADDRESS_BODY_MAP.put("00:07:80:2D:87:D5", "Back");
        // ADDRESS_BODY_MAP.put("00:07:80:2D:88:7E", "Back");

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        // Check if Bluetooth is enabled; if not, ask for permission
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableIntent);
        }

        startBluetoothLeService();

        super.onCreate();

    }

    /**
     * Code to manage Service life cycle.
     */
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

    /**
     * Start BluetoothLeService.
     */
    private void startBluetoothLeService() {
        boolean serviceStarted;
        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        startService(bindIntent);
        serviceStarted = bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!serviceStarted) {
            CustomToast.middleBottom(this, "Bind to BluetoothLeService failed");
        }
    }

    /**
     * Initialize HashMaps for sensor nodes.
     */
    public static void generateDeviceMap() {
        ADDRESS_NAME_MAP = new HashMap<>();
        ADDRESS_TYPE_MAP = new HashMap<>();
        ADDRESS_BODY_MAP = new HashMap<>();
        for (int i=0; i<NUMBER_OF_SENSORNODE; i++) {
            ADDRESS_NAME_MAP.put(DEVICE_ADDRESS_LIST[i], DEVICE_NAME_LIST[i]);
            ADDRESS_TYPE_MAP.put(DEVICE_ADDRESS_LIST[i], DEVICE_TYPE_LIST[i]);
            ADDRESS_BODY_MAP.put(DEVICE_ADDRESS_LIST[i], SN_BODY_LIST[i]);
        }
    }

}
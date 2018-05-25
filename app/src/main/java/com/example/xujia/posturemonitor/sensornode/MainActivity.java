/**
 * Xujia Zhou. Copyright (c) 2018-02-25.
 */

package com.example.xujia.posturemonitor.sensornode;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BleDeviceInfo;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MainActivity for setting up communication with sensor nodes as well as
 * transmitting sensor data to Java TCP serve.
 */
public class MainActivity extends ViewPagerActivity {

    private static final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;

    public static RequestQueue requestQueue;
    public static String URL = "http://192.168.1.33:8000/handlePost";

    // GUI
    private static MainActivity mThis;
    private ScanView mScanView;
    private GalaxySensorView mGalaxyView;

    // Broadcast to DeviceActivity
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    public final static String EXTRA_DATA = "com.example.xujia.posturemonitor.common.EXTRA_DATA";
    public final static String ACTION_ACC = "com.example.xujia.posturemonitor.common.ACTION_ACC";
    public final static String ACTION_MAG = "com.example.xujia.posturemonitor.common.ACTION_MAG";
    public final static String ACTION_GYR = "com.example.xujia.posturemonitor.common.ACTION_GYR";
    public final static String ACTION_BAR = "com.example.xujia.posturemonitor.common.ACTION_BAR";
    public final static String ACTION_BAT = "com.example.xujia.posturemonitor.common.ACTION_BAT";

    // BLE management
    private boolean mScanning = false;
    public static BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBtAdapter;
    public static List<BleDeviceInfo> mDeviceInfoList;

    // Handle BluetoothAdapter state change
    private IntentFilter mFilter;

    public MainActivity() {
        mThis = this;
        mResourceFragmentPager = R.layout.fragment_pager;
        mResourceIdPager = R.id.pager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the application has location permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();
        // BluetoothAdapter.getDefaultAdapter();    // It will give the same Bluetooth adapter

        mDeviceInfoList = new ArrayList<BleDeviceInfo>();

        // Create the fragments and add them to the view pager and tabs
        mScanView = new ScanView();
        mGalaxyView = new GalaxySensorView();
        mSectionsPagerAdapter.addSection(mScanView, "BLE Device List");
        mSectionsPagerAdapter.addSection(mGalaxyView, "Galaxy sensors");

        // Register the BroadcastReceiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, mFilter);

        // requestQueue = Volley.newRequestQueue(this);

        initBroadcastTimerTask();

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        // mActivity.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        // stopTimer();
        super.onPause();
        // mActivity.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * Set the text on the "Scan/Stop scanning" button.
     * Called when the "Scan/Stop scanning" button is clicked.
     */
    public void onBtnClick(View view) {
        showBusyIndicator(mScanning);
        mScanView.setScanButtonText(mScanning ? "Scan" : "Stop scanning");
        if (mScanning) {
            stopScan();
        } else {
            startScan();
        }
    }

    /**
     * Start scanning BLE devices.
     */
    private void startScan() {
        // Make sure the old information is deleted
        mDeviceInfoList.clear();
        // Update list view
        mScanView.notifyDataSetChanged();
        scanLeDevice(true);
    }

    /**
     * Stop scanning.
     */
    public void stopScan() {
        mScanning = false;
        scanLeDevice(false);
    }

    /**
     * Scan BLE devices.
     */
    private boolean scanLeDevice(boolean enable) {
        if (enable) {
            mScanning = mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        return mScanning;
    }

    /**
     * BLE device scan callback.
     * NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    // If the BLE device does not already exist in the list view
                    if (!deviceInfoExists(device.getAddress())) {
                        // Filter away all BLE devices that are not sensor nodes otherwise the list might be very long
                        if (deviceIsSensornodeOrSensortag(device)) {
                            // Get information of newly found sensornode or sensortag
                            String address = device.getAddress();
                            String deviceName = PostureMonitorApplication.ADDRESS_NAME_MAP.get(address);
                            String deviceType = PostureMonitorApplication.ADDRESS_TYPE_MAP.get(address);
                            String deviceBody = PostureMonitorApplication.ADDRESS_BODY_MAP.get(address);
                            BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi, address, deviceName, deviceType, deviceBody);
                            // Add the device to private member mDeviceInfoList
                            addDevice(deviceInfo);
                        }
                        // If all sensor nodes have been found, alert user and stop scanning
                        if (mDeviceInfoList.size()== PostureMonitorApplication.NUMBER_OF_SENSORNODE) {
                            CustomToast.middleBottom(mThis, "All devices have been found.");
                            stopScan();
                            for (int i=0; i<mDeviceInfoList.size(); i++) {
                                if (mDeviceInfoList.get(i).getType().equals("BLE")) {
                                    ScanView.deviceModel[i] = 0;    // 0 for BLE113 Sensornode
                                } else {
                                    ScanView.deviceModel[i] = 1;    // 1 for CC2650 Sensortag
                                }
                            }
                            mScanView.showConnectAllButton();
                            showBusyIndicator(true);
                        }
                    } else {
                        // If the BLE device is already in the list view, update its RSSI info
                        BleDeviceInfo deviceInfo = findDeviceInfo(device);
                        deviceInfo.updateRssi(rssi);
                        mScanView.notifyDataSetChanged();
                    }
                }

            });
        }
    };

    /**
     * Check if the BLE device already exists in the list view.
     */
    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the found BLE device is CC2650 sensortag or BLE113 sensornode
     */
    private boolean deviceIsSensornodeOrSensortag(BluetoothDevice device) {
        String address = device.getAddress();
        for (String deviceAddress : PostureMonitorApplication.DEVICE_ADDRESS_LIST) {
            if (deviceAddress.equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a BleDeviceInfo object that stores information about a sensor node.
     */
    private BleDeviceInfo createDeviceInfo(
            BluetoothDevice device, int rssi, String address, String name, String type, String body) {
        return new BleDeviceInfo(device, rssi, address, name, type, body);
    }

    protected List<BleDeviceInfo> getDeviceInfoList() {
        return mDeviceInfoList;
    }

    /**
     * Add the found sensor node to private member mDeviceInfoList and update list view.
     */
    private void addDevice(BleDeviceInfo device) {
        mDeviceInfoList.add(device);
        mScanView.notifyDataSetChanged();
    }

    /**
     * Find the corresponding BleDeviceInfo object of a BLE device.
     */
    private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(device.getAddress())) {
                return mDeviceInfoList.get(i);
            }
        }
        return null;
    }

    /**
     * Initialize the broadcast receiver that handles changed state of the local Bluetooth adapter.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        List <BluetoothGattCharacteristic> charList = new ArrayList<BluetoothGattCharacteristic>();

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth adapter state change
                switch (mBtAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(context, "Exiting application.", Toast.LENGTH_LONG)
                                .show();
                        Log.d(TAG, "Action STATE_OFF handled.");
                        finish();
                        break;
                    default:
                        Log.d(TAG, "Action STATE_CHANGED not processed.");
                        break;
                }
            }

        }
    };

    /**
     * Initializes a TimerTask that broadcast the new sensor data to DeviceActivity.
     */
    private void initBroadcastTimerTask() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        double[][] accelData = {ScanView.currentAccelX, ScanView.currentAccelY, ScanView.currentAccelZ};
                        double[][] magData = {ScanView.currentMagX, ScanView.currentMagY, ScanView.currentMagZ};
                        double[][] gyroData = {ScanView.currentGyroX, ScanView.currentGyroY, ScanView.currentGyroZ};
                        broadcastUpdate(ACTION_ACC, accelData);
                        broadcastUpdate(ACTION_MAG, magData);
                        broadcastUpdate(ACTION_GYR, gyroData);
                        broadcastUpdate(ACTION_BAR, ScanView.currentBaro);
                        broadcastUpdate(ACTION_BAT, ScanView.currentBatteryLevel);
                    }
                });
            }
        };
        timer.schedule(timerTask, 1000, 500);
    }

    /**
     * Broadcast double[][] data.
     */
    private void broadcastUpdate(final String action, final double[][] data) {
        final Intent intent = new Intent(action);
        // intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    /**
     * Broadcast double[] data.
     */
    private void broadcastUpdate(final String action, final double[] data) {
        final Intent intent = new Intent(action);
        // intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    /**
     * Broadcast String[] data.
     */
    private void broadcastUpdate(final String action, final String[] data) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Handles result of location permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }
                return;
            }
        }
    }

}

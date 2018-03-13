package com.example.xujia.posturemonitor.sensornode;

import android.Manifest;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BleDeviceInfo;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ViewPagerActivity {

    private static final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;
    public final int NUMBER_OF_DEVICES = 2;

    // GUI
    private static MainActivity mThis;
    private ScanView mScanView;
    private GalaxySensorView mGalaxyView;

    // BLE management
    private boolean mScanning = false;
    public static BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBtAdapter;
    private List<BleDeviceInfo> mDeviceInfoList;
    public static final String[] CC2650Addresses = {"B0:B4:48:BC:53:87", "B0:B4:48:BD:0C:84", "00:07:80:2D:9E:F2"};

    // Handle BluetoothAdapter state change
    private IntentFilter mFilter;

    // Temp
    public static BluetoothGattCharacteristic mBatteryC;

    public MainActivity() {
        mThis = this;
        mResourceFragmentPager = R.layout.fragment_pager;
        mResourceIdPager = R.id.pager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    }

    public void onBtnClick(View view) {
        showBusyIndicator(mScanning);
        mScanView.setButtonText(mScanning ? "Scan" : "Stop scanning");
        if (mScanning) {
            stopScan();
        } else {
            startScan();
        }
    }

    private void startScan() {
        mDeviceInfoList.clear();
        mScanView.notifyDataSetChanged();
        scanLeDevice(true);
    }

    public void stopScan() {
        mScanning = false;
        scanLeDevice(false);
    }

    private boolean scanLeDevice(boolean enable) {
        if (enable) {
            mScanning = mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        return mScanning;
    }

    // Device scan callback.
    // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (!deviceInfoExists(device.getAddress())) {
                        if (deviceIsCC2650(device)) {
                            // New CC2650 device
                            BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi, device.getAddress());
                            addDevice(deviceInfo);
                        }
                        if (mDeviceInfoList.size()== NUMBER_OF_DEVICES) {
                            CustomToast.middleBottom(mThis, "All devices have been found.");
                            stopScan();
                            mScanView.showConnectAllButton();
                            showBusyIndicator(true);
                        }
                    } else {
                        // Already in list, update RSSI info
                        BleDeviceInfo deviceInfo = findDeviceInfo(device);
                        deviceInfo.updateRssi(rssi);
                        mScanView.notifyDataSetChanged();
                    }
                }

            });
        }
    };

    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(address)) {
                return true;
            }
        }
        return false;
    }

    private boolean deviceIsCC2650(BluetoothDevice device) {
        String address = device.getAddress();
        for (String deviceAddress : CC2650Addresses) {
            if (deviceAddress.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi, String address) {
        return new BleDeviceInfo(device, rssi, address);
    }

    List<BleDeviceInfo> getDeviceInfoList() {
        return mDeviceInfoList;
    }

    private void addDevice(BleDeviceInfo device) {
        mDeviceInfoList.add(device);
        mScanView.notifyDataSetChanged();
    }

    private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(device.getAddress())) {
                return mDeviceInfoList.get(i);
            }
        }
        return null;
    }

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

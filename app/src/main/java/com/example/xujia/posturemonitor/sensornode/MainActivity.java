package com.example.xujia.posturemonitor.sensornode;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BleDeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ViewPagerActivity {

    private static final String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;

    // GUI
    private ScanView mScanView;

    // BLE management
    private boolean mScanning = false;
    public static BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBtAdapter;
    private List<BleDeviceInfo> mDeviceInfoList;

    public MainActivity() {
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
        mSectionsPagerAdapter.addSection(mScanView, "BLE Device List");
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
        // Start device discovery
        mDeviceInfoList.clear();
        mScanView.notifyDataSetChanged();
        scanLeDevice(true);
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
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (!deviceInfoExists(device.getAddress())) {
                        // New device
                        BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);
                        addDevice(deviceInfo);
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

    private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
        return new BleDeviceInfo(device, rssi);
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

    public void stopScan() {
        mScanning = false;
        scanLeDevice(false);
    }

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

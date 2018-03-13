package com.example.xujia.posturemonitor.common;

import android.bluetooth.BluetoothDevice;

/**
 * Created by xujia on 2018-02-20.
 */

public class BleDeviceInfo {
    // Data
    private BluetoothDevice mBtDevice;
    private int mRssi;
    private String mAddress;

    public BleDeviceInfo(BluetoothDevice device, int rssi, String address) {
        mBtDevice = device;
        mRssi = rssi;
        mAddress = address;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBtDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public String getAddress() {
        return mAddress;
    }

    public void updateRssi(int rssiValue) {
        mRssi = rssiValue;
    }

}

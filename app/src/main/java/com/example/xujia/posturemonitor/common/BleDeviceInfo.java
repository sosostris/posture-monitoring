package com.example.xujia.posturemonitor.common;

import android.bluetooth.BluetoothDevice;

/**
 * Created by xujia on 2018-02-20.
 */

public class BleDeviceInfo {
    // Data
    private BluetoothDevice mBtDevice;
    private int mRssi;

    public BleDeviceInfo(BluetoothDevice device, int rssi) {
        mBtDevice = device;
        mRssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBtDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public void updateRssi(int rssiValue) {
        mRssi = rssiValue;
    }

}

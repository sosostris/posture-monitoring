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
    private String mName;
    private String mType;
    private String mBody;

    public BleDeviceInfo(BluetoothDevice device, int rssi, String address, String name, String type, String body) {
        mBtDevice = device;
        mRssi = rssi;
        mAddress = address;
        mName = name;
        mType = type;
        mBody = body;
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

    public String getName() {
        return mName;
    }

    public String getType() {
        return mType;
    }

    public String getBody() {
        return mBody;
    }

    public void updateRssi(int rssiValue) {
        mRssi = rssiValue;
    }

}

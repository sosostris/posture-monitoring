/**
 * Xujia Zhou. Copyright (c) 2018-02-20.
 */

package com.example.xujia.posturemonitor.common;

import android.bluetooth.BluetoothDevice;

/**
 * Class that represents an identified sensor node (either CC2650 sensortag or BLE113 sensor node)
 */
public class BleDeviceInfo {

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

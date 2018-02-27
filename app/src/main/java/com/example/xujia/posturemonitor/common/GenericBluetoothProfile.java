package com.example.xujia.posturemonitor.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TableRow;

import java.util.Map;

public class GenericBluetoothProfile {

	protected BluetoothDevice mBTDevice;
	protected BluetoothGattService mBTService;
	protected BluetoothLeService mBTLeService;

	protected BluetoothGattCharacteristic dataC;
	protected BluetoothGattCharacteristic configC;
	protected BluetoothGattCharacteristic periodC;

	protected static final int GATT_TIMEOUT = 250; // milliseconds

	protected Context context;
	protected boolean isRegistered;
    public boolean isConfigured;
    public boolean isEnabled;

	public GenericBluetoothProfile(final Context con, BluetoothDevice device, BluetoothGattService service, BluetoothLeService controller) {
		super();
		this.mBTDevice = device;
		this.mBTService = service;
		this.mBTLeService = controller;
		this.dataC = null;
		this.periodC = null;
		this.configC = null;
		this.context = con;
		this.isRegistered = false;
	}

	public void onResume() {
		if (this.isRegistered == false) {
			// this.context.registerReceiver(guiReceiver, GenericBluetoothProfile.makeFilter());
			this.isRegistered = true;
		}
	}

	public void onPause() {
		if (this.isRegistered == true) {
			// this.context.unregisterReceiver(guiReceiver);
			this.isRegistered = false;
		}
	}

	public static boolean isCorrectService(BluetoothGattService service) {
		//Always return false in parent class
		return false;
	}

    public boolean isDataC(BluetoothGattCharacteristic c) {
        if (this.dataC == null) return false;
        if (c.equals(this.dataC)) return true;
        else return false;
    }

}

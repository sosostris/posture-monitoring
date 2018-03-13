package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.common.GattInfo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


@SuppressLint("InflateParams") public class DeviceActivity extends ViewPagerActivity implements SensorEventListener {

    // Log
    private static String TAG = "DeviceActivity";

    // Activity
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String EXTRA_DEVICE_POSITION = "EXTRA_DEVICE_POSITION";
    public static final String EXTRA_CHAR = "EXTRA_CHAR";
    private static final int PREF_ACT_REQ = 0;
    private static final int FWUPDATE_ACT_REQ = 1;

    // GUI
    private DeviceView mDeviceView = null;

    // BLE
    private BluetoothLeService mBtLeService;
    private BluetoothDevice mDevice;
    private int mDevicePosition;
    private boolean mIsReceiving = false;

    public DeviceActivity() {
        mResourceFragmentPager = R.layout.fragment_pager;
        mResourceIdPager = R.id.pager;
    }

    public static DeviceActivity getInstance() {
        return (DeviceActivity) mThis;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // BLE
        mBtLeService = BluetoothLeService.getInstance();
        mDevice = intent.getParcelableExtra(EXTRA_DEVICE);
        mDevicePosition = intent.getIntExtra(EXTRA_DEVICE_POSITION, -1);

        // GUI
        mDeviceView = new DeviceView();
        mSectionsPagerAdapter.addSection(mDeviceView, "Sensor data");

        // GATT database
        Resources res = getResources();
        XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
        new GattInfo(xpp);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsReceiving) {
            unregisterReceiver(mGattUpdateReceiver);
            mIsReceiving = false;
        }

        // View should be started again from scratch
        this.mDeviceView.first = true;
        this.mDeviceView = null;
        finishActivity(PREF_ACT_REQ);
        finishActivity(FWUPDATE_ACT_REQ);
    }

    @Override
    protected void onResume() {
        // Log.d(TAG, "onResume");
        super.onResume();
        if (!mIsReceiving) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            mIsReceiving = true;
        }
    }

    @Override
    protected void onPause() {
        // Log.d(TAG, "onPause");
        super.onPause();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        filter.addAction(BluetoothLeService.ACTION_DATA_READ);
        return filter;
    }

    void onViewInflated(View view) {
        Log.d(TAG, "Gatt view ready");
        // Set title bar to device name
        setTitle(mDevice.getName() + " " + mDevice.getAddress());
        if (mDevice.getAddress().equals(MainActivity.CC2650Addresses[0])) {
            mDeviceView.mTextSensorType.setText("Humidity sensor");
        } else if (mDevice.getAddress().equals(MainActivity.CC2650Addresses[1])) {
            mDeviceView.mTextSensorType.setText("Barometer sensor");
        } else if (mDevice.getAddress().equals(MainActivity.CC2650Addresses[2])) {
            mDeviceView.mTextSensorType.setText("Giraffe");
            mDeviceView.mButton.setVisibility(View.VISIBLE);
            mDeviceView.mHeightValue.setVisibility(View.VISIBLE);
            mDeviceView.mButton.setOnClickListener(v -> {
                mBtLeService.getBtGatt(mDevicePosition).readCharacteristic(MainActivity.mBatteryC);
            });
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, Intent intent) {
            final String action = intent.getAction();
            final int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                    BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {

                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                // If it' barometer data from BLE113
                if (uuidStr.contains("2aa3")) {
                    // Get hPa (from BLE113 it is little endian order)
                    byte[] reversedValue = new byte[4];
                    reversedValue[0] = 0;
                    reversedValue[1] = value[2];
                    reversedValue[2] = value[1];
                    reversedValue[3] = value[0];
                    int correctData = byteToInt(reversedValue);
                    int hPa = correctData / 4096;
                    mDeviceView.mTextSensorData.setText("hPa: " + hPa);
                    // Formula of hPa to height:
                    int temprature = 70;
                    double height = (Math.log((double) hPa * 100  / 101325)) * 287.053 * (temprature + 459.67) * 5 / 9 / -9.8;
                    mDeviceView.mHeightValue.setText("Height above sea level: " + height);
                } else {
                    StringBuilder builder = new StringBuilder();
                    for (int i=0; i<value.length; i++) {
                        builder.append(value[i]);
                        builder.append(" ");
                    }
                    mDeviceView.mTextSensorData.setText(builder.toString());
                }

                Log.d(TAG,"Got characteristic : " + uuidStr);

            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                StringBuilder builder = new StringBuilder();
                for (int i=0; i<value.length; i++) {
                    builder.append(value[i]);
                    builder.append(" ");
                }
                mDeviceView.mTextSensorData.setText(builder.toString());

                Log.d(TAG,"Read characteristic : " + uuidStr);
            }
        }
    };

    // Activity result handling
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            default:
                break;
        }
    }

    // Handles phone sensors
    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private int byteToInt(byte[] bytes) {
        int result = 0;
        for (int i=0; i<4; i++) {
            result = ( result << 8 ) + (int) bytes[i];
        }
        return result;
    }
}

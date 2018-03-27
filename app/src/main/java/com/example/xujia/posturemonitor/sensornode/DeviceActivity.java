package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.common.GattInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

@SuppressLint("InflateParams") public class DeviceActivity extends ViewPagerActivity implements SensorEventListener {

    // Log
    private static String TAG = "DeviceActivity";

    // TCP connection with MATLAB
    //private static final String host = "192.168.1.150";
    //private static final String host = "194.47.32.167";
    private static final String host = "192.168.0.108";
    private static final int PORT = 30000;
    private Socket socket;
    private OutputStream os;
    private DataOutputStream dos;

    // HTTP Client code
    RequestQueue requestQueue;
    String URL = "194.47.44.239";

    // Activity
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String EXTRA_DEVICE_POSITION = "EXTRA_DEVICE_POSITION";
    private static final int PREF_ACT_REQ = 0;
    private static final int FWUPDATE_ACT_REQ = 1;

    // GUI
    private DeviceView mDeviceView = null;

    // BLE
    private BluetoothLeService mBtLeService;
    private BluetoothDevice mDevice;
    private int mDevicePosition;
    private boolean mIsReceiving = false;

    // Temp
    private String batteryString = "";

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

        // Http client
        requestQueue = Volley.newRequestQueue(this);

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

        Thread matlabWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(host, PORT);
                    if (socket != null) {
                        try {
                            os = socket.getOutputStream();
                            dos = new DataOutputStream(os);
                            Log.d(TAG, "Connected with MATLAB server");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (socket != null) {
            matlabWorker.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (dos != null) {
                dos.close();
            }
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if (socket != null) {
            try {
                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
                Log.d(TAG, "OnResume: Connected with MATLAB server");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        // Log.d(TAG, "onPause");
        super.onPause();
        try {
            if (dos != null) {
                dos.close();
            }
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            // mBtLeService.getBtGatt(mDevicePosition).readCharacteristic(MainActivity.mBatteryC);
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

                // Battery level
                if (uuidStr.contains("2a19")) {
                    Date currentTime = Calendar.getInstance().getTime();
                    String currentBattery = currentTime + ": " + buildString(value);
                    batteryString = batteryString + " | " + currentBattery;
                    mDeviceView.mBatteryLevel.setText(batteryString);
                    Log.d(TAG,"Got characteristic : " + uuidStr + "battery level: " + buildString(value));
                }

                // Barometer data
                if (uuidStr.contains("8882")) {
                    // Get hPa (from BLE113 it is little endian order)
                    byte[] correctBaroData = getCorrectBaroData(value);
                    int correctData = byteToInt(correctBaroData);
                    int hPa = correctData / 4096;
                    byte[] displayData = new byte[3];
                    for (int i=0; i<3; i++) {
                        displayData[i] = correctBaroData[i+1];
                    }
                    mDeviceView.mBaroData.setText(buildString(displayData));
                    mDeviceView.mHpaValue.setText("hPa: " + hPa);
                    // Gyroscope data
                } else if (uuidStr.contains("8884")) {
                    byte[] correctGyroData = getReversedBytes(value, 6);
                    mDeviceView.mGyroData.setText(buildString(correctGyroData));
                // Accelerometer data
                } else if (uuidStr.contains("8886")) {
                    byte[] correctAccelData = getReversedBytes(value, 6);
                    mDeviceView.mAccelData.setText(buildString(correctAccelData));
                    if (dos != null) {
                        try {
                            dos.write(correctAccelData, 0, 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                //  Magnetometer data
                } else if (uuidStr.contains("8888")) {
                    byte[] correctMagData = getReversedBytes(value, 6);
                    mDeviceView.mMagData.setText(buildString(correctMagData));
                }

                Log.d(TAG,"Got characteristic : " + uuidStr);

            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {    // battery service
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                mDeviceView.mBatteryLevel.setText(buildString(value));
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

    private String buildString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<bytes.length; i++) {
            builder.append(bytes[i]);
            builder.append(" ");
        }
        return builder.toString();
    }

    private byte[] getCorrectBaroData(byte[] bytes) {
        // The data in BLE113 is stored in little endian order so we need to reverse order
        byte[] reversedBytes = new byte[4];
        reversedBytes[0] = 0;
        for (int i=1; i<4; i++) {
            reversedBytes[i] = bytes[3-i];
        }
        return reversedBytes;
    }

    private byte[] getReversedBytes(byte[] bytes, int length) {
        byte[] reversedBytes = new byte[length];
        for (int i=0; i<length; i++) {
            reversedBytes[i] = bytes[length-i-1];
        }
        return reversedBytes;
    }

}

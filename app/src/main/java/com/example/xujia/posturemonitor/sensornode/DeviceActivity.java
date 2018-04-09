package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.common.GattInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

@SuppressLint("InflateParams") public class DeviceActivity extends ViewPagerActivity implements SensorEventListener {

    // Log
    private static String TAG = "DeviceActivity";
    private static final int BLE113_SENSORNODE = 0;
    private static final int CC2650_SENSORTAG = 1;

    // TCP connection with MATLAB
    private Socket socket;
    private OutputStream os;
    private DataOutputStream dos;

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

    // MATLAB
    private boolean mIsStreaming;
    private String currentStreamingSensorType = null;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.opt_exit:
                Toast.makeText(this, "Exit...", Toast.LENGTH_SHORT).show();
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        // registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        // registerReceiver(mDataUpdateReceiver, makeDataUpdateIntentFilter());

        // BLE
        // mBtLeService = BluetoothLeService.getInstance();
        mDevice = intent.getParcelableExtra(EXTRA_DEVICE);
        mDevicePosition = intent.getIntExtra(EXTRA_DEVICE_POSITION, -1);

        // GUI
        mDeviceView = new DeviceView();
        mSectionsPagerAdapter.addSection(mDeviceView, "Sensor data");

        // GATT database
        Resources res = getResources();
        XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
        new GattInfo(xpp);

        mIsStreaming = false;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close MATLAB connection
        try {
            if (dos != null) {
                dos.close();
            }
            if (os != null) {
                os.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIsStreaming = false;
        currentStreamingSensorType = null;

//        if (mIsReceiving) {
//            unregisterReceiver(mGattUpdateReceiver);
//            mIsReceiving = false;
//        }
        // unregisterReceiver(mDataUpdateReceiver);

        // View should be started again from scratch
        this.mDeviceView = null;
        finishActivity(PREF_ACT_REQ);
        finishActivity(FWUPDATE_ACT_REQ);
    }

    @Override
    protected void onResume() {
        // Log.d(TAG, "onResume");
        super.onResume();
        // registerReceiver(mDataUpdateReceiver, makeDataUpdateIntentFilter());
//        if (!mIsReceiving) {
//            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//            mIsReceiving = true;
//        }
    }

    @Override
    protected void onPause() {
        // Log.d(TAG, "onPause");
        super.onPause();
        // Close MATLAB connection
        try {
            if (dos != null) {
                dos.close();
            }
            if (os != null) {
                os.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIsStreaming = false;
        currentStreamingSensorType = null;

        unregisterReceiver(mDataUpdateReceiver);
    }

//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
//        filter.addAction(BluetoothLeService.ACTION_DATA_READ);
//        return filter;
//    }

    private static IntentFilter makeDataUpdateIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_ACC);
        filter.addAction(MainActivity.ACTION_MAG);
        filter.addAction(MainActivity.ACTION_GYR);
        filter.addAction(MainActivity.ACTION_BAR);
        filter.addAction(MainActivity.ACTION_BAT);
        return filter;
    }

    void onViewInflated(View view) {
        Log.d(TAG, "Gatt view ready");
        // Set title bar to device name
        setTitle(mDevice.getName() + " " + mDevice.getAddress());
        mDeviceView.mSensornodeId.setText(PostureMonitorApplication.ADDRESS_NAME_MAP.get(mDevice.getAddress()));
        registerReceiver(mDataUpdateReceiver, makeDataUpdateIntentFilter());
        // mBtLeService.getBtGatt(mDevicePosition).readCharacteristic(MainActivity.mBatteryC);
    }

//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(final Context context, Intent intent) {
//            final String action = intent.getAction();
//            final int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
//                    BluetoothGatt.GATT_SUCCESS);
//
//            if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
//                // Notification
//                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
//
//                // Battery level
//                if (uuidStr.contains("2a19")) {
//                    Date currentTime = Calendar.getInstance().getTime();
//                    String currentBattery = currentTime + ": " + buildString(value);
//                    batteryString = batteryString + " | " + currentBattery;
//                    mDeviceView.mBatteryLevel.setText(batteryString);
//                    Log.d(TAG,"Got characteristic : " + uuidStr + "battery level: " + buildString(value));
//                }
//
//                // Barometer data
//                if (uuidStr.contains("8882")) {
//                    // Get hPa (from BLE113 it is little endian order)
//                    byte[] correctBaroData = getCorrectBaroData(value);
//                    int correctData = byteToInt(correctBaroData);
//                    int hPa = correctData / 4096;
//                    byte[] displayData = new byte[3];
//                    for (int i=0; i<3; i++) {
//                        displayData[i] = correctBaroData[i+1];
//                    }
//                    mDeviceView.mBaroData.setText(buildString(displayData));
//                    mDeviceView.mHpaValue.setText("hPa: " + hPa);
//                    // Gyroscope data
//                } else if (uuidStr.contains("8884")) {
//                    byte[] correctGyroData = getReversedBytes(value, 6);
//                    mDeviceView.mGyroData.setText(buildString(correctGyroData));
//                // Accelerometer data
//                } else if (uuidStr.contains("8886")) {
//                    byte[] correctAccelData = getReversedBytes(value, 6);
//                    mDeviceView.mAccelData.setText(buildString(correctAccelData));
//                    if (dos != null) {
//                        try {
//                            dos.write(correctAccelData, 0, 2);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                //  Magnetometer data
//                } else if (uuidStr.contains("8888")) {
//                    byte[] correctMagData = getReversedBytes(value, 6);
//                    mDeviceView.mMagData.setText(buildString(correctMagData));
//                }
//
//                Log.d(TAG,"Got characteristic : " + uuidStr);
//
//            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {    // battery service
//                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
//                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
//                mDeviceView.mBatteryLevel.setText(buildString(value));
//                Log.d(TAG,"Read characteristic : " + uuidStr);
//            }
//        }
//    };

    private final BroadcastReceiver mDataUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, Intent intent) {
            final String action = intent.getAction();

            if (MainActivity.ACTION_ACC.equals(action)) {
                mDeviceView.mAccelData.setText("x: " + ScanView.currentAccelX[mDevicePosition] + "\ny: " +
                        ScanView.currentAccelY[mDevicePosition] + "\nz: " + ScanView.currentAccelZ[mDevicePosition]);
                sendDataToMATLAB(mDevicePosition, "acc");
            } else if (MainActivity.ACTION_MAG.equals(action)) {
                mDeviceView.mMagData.setText("x:"+ ScanView.currentMagX[mDevicePosition] + "\ny: " +
                        ScanView.currentMagY[mDevicePosition] + "\nz: " + ScanView.currentMagZ[mDevicePosition] );
                sendDataToMATLAB(mDevicePosition, "mag");
            } else if (MainActivity.ACTION_GYR.equals(action)) {
                mDeviceView.mGyroData.setText("x: "+ ScanView.currentGyroX[mDevicePosition] + "\ny: " +
                        ScanView.currentGyroY[mDevicePosition] + "\nz: " + ScanView.currentGyroZ[mDevicePosition] );
                sendDataToMATLAB(mDevicePosition, "gyr");
            } else if (MainActivity.ACTION_BAR.equals(action)) {
                mDeviceView.mBaroData.setText(Double.toString(ScanView.currentBaro[mDevicePosition]));
                sendDataToMATLAB(mDevicePosition, "bar");
            } else if (MainActivity.ACTION_BAT.equals(action)) {
                mDeviceView.mBatteryLevel.setText(ScanView.currentBatteryLevel[mDevicePosition]);
            }

        }
    };

    /*
     The firs byte is to identify sensortag or sensornode, 0 = BLE113 Sensornode, 1 = CC2650 Sensortag
     The second byte is used to identify sensor type
     0 = Accelerometer, 1 = Magnetometer, 2 = Gyroscope, 3 = Barometer
    */
    private void sendDataToMATLAB(int position, String sensorType) {
        if (!sensorType.equals(currentStreamingSensorType)) {
            return;
        }
        byte[] newData = new byte[8];
        newData[0] = (byte) ScanView.deviceModel[position];
        switch (sensorType) {
            case "acc":
                newData[1] = 0;
                newData[2] = ScanView.currentAccelXByte[position][0];
                newData[3] = ScanView.currentAccelXByte[position][1];
                newData[4] = ScanView.currentAccelYByte[position][0];
                newData[5] = ScanView.currentAccelYByte[position][1];
                newData[6] = ScanView.currentAccelZByte[position][0];
                newData[7] = ScanView.currentAccelZByte[position][1];
                break;
            case "mag":
                newData[1] = 1;
                newData[2] = ScanView.currentMagXByte[position][0];
                newData[3] = ScanView.currentMagYByte[position][1];
                newData[4] = ScanView.currentMagYByte[position][0];
                newData[5] = ScanView.currentMagYByte[position][1];
                newData[6] = ScanView.currentMagZByte[position][0];
                newData[7] = ScanView.currentMagZByte[position][1];
                break;
            case "gyr":
                newData[1] = 2;
                newData[2] = ScanView.currentGyroXByte[position][0];
                newData[3] = ScanView.currentGyroXByte[position][1];
                newData[4] = ScanView.currentGyroYByte[position][0];
                newData[5] = ScanView.currentGyroYByte[position][1];
                newData[6] = ScanView.currentGyroZByte[position][0];
                newData[7] = ScanView.currentGyroZByte[position][1];
                break;
            case "bar":
                newData[1] = 3;
                newData[2] = ScanView.currentBaroByte[position][0];
                newData[3] = ScanView.currentBaroByte[position][1];
                newData[4] = ScanView.currentBaroByte[position][2];
                break;
        }
        if (dos != null) {
            try {
                dos.write(newData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


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

    public void onMatlabRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.accel_matlab:
                currentStreamingSensorType = "acc";
                break;
            case R.id.mag_matlab:
                currentStreamingSensorType = "mag";
                break;
            case R.id.gyro_matlab:
                currentStreamingSensorType = "gyr";
                break;
            case R.id.baro_matlab:
                currentStreamingSensorType = "bar";
                break;
        }
    }

    public void toggleMatlabStreaming(View view) {
        mIsStreaming = !mIsStreaming;
        if (mIsStreaming) {
            mDeviceView.setMatlabButtonText("Stop streaming");
            Thread matlabWorker = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = new Socket(PostureMonitorApplication.MATLAB_IP, PostureMonitorApplication.MATLAB_PORT);
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
            matlabWorker.start();
        } else {
            mDeviceView.setMatlabButtonText("Start streaming");
            try {
                if (dos != null) {
                    dos.close();
                }
                if (os != null) {
                    os.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

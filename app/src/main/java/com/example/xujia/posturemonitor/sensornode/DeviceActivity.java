package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.common.GattInfo;
import com.example.xujia.posturemonitor.common.GenericBluetoothProfile;
import com.example.xujia.posturemonitor.common.SensorTagGatt;


@SuppressLint("InflateParams") public class DeviceActivity extends ViewPagerActivity {

    // Log
    private static String TAG = "DeviceActivity";

    // Activity
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    private static final int PREF_ACT_REQ = 0;
    private static final int FWUPDATE_ACT_REQ = 1;

    private DeviceView mDeviceView = null;

    // BLE
    private BluetoothLeService mBtLeService;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBtGatt;
    private List<BluetoothGattService> mServiceList;
    private boolean mServicesRdy = false;
    private boolean mIsReceiving = false;

    // GUI
    private List<GenericBluetoothProfile> mProfiles;

    // Temp
    private BluetoothGattCharacteristic mBarometerDataC;
    private BluetoothGattCharacteristic mBarometerConfigC;

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
        mBluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE);
        mServiceList = new ArrayList<BluetoothGattService>();

        // GUI
        mDeviceView = new DeviceView();
        mSectionsPagerAdapter.addSection(mDeviceView, "Sensors");
        mProfiles = new ArrayList<GenericBluetoothProfile>();

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

        for (GenericBluetoothProfile p : mProfiles) {
            p.onPause();
        }

        // View should be started again from scratch
        this.mDeviceView.first = true;
        this.mProfiles = null;
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
        this.mBtLeService.abortTimedDisconnect();
    }

    @Override
    protected void onPause() {
        // Log.d(TAG, "onPause");
        super.onPause();
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        filter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        filter.addAction(BluetoothLeService.ACTION_DATA_READ);
        return filter;
    }

    void onViewInflated(View view) {
        Log.d(TAG, "Gatt view ready");

        // Set title bar to device name
        setTitle(mBluetoothDevice.getName());

        // Create GATT object
        mBtGatt = BluetoothLeService.getBtGatt();

        // Start service discovery
        if (!mServicesRdy && mBtGatt != null) {
            if (mBtLeService.getNumServices() == 0)
                discoverServices();
        }

    }

    private void discoverServices() {
        if (mBtGatt.discoverServices()) {
            mServiceList.clear();
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

    private void makeToast(String txt) {
        Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        List <BluetoothGattService> serviceList;
        List <BluetoothGattCharacteristic> charList = new ArrayList<BluetoothGattCharacteristic>();

        @Override
        public void onReceive(final Context context, Intent intent) {
            final String action = intent.getAction();
            final int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                    BluetoothGatt.GATT_SUCCESS);

            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {

                    mDeviceView.mBarometerValues.setText("Services discovered");

                    serviceList = mBtLeService.getSupportedGattServices();
                    if (serviceList.size() > 0) {
                        for (int ii = 0; ii < serviceList.size(); ii++) {
                            BluetoothGattService s = serviceList.get(ii);
                            List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                            if (c.size() > 0) {
                                for (int jj = 0; jj < c.size(); jj++) {
                                    charList.add(c.get(jj));
                                }
                            }
                        }
                    }
                    Log.d("DeviceActivity","Total characteristics " + charList.size());

//                    for (BluetoothGattService service : serviceList) {
//
//                        // If service is Barometer
//                        // if ((service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
//                        if ((service.getUuid().toString().compareTo("f000aa40-0451-4000-b000-000000000000")) == 0) {
//                            List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
//                            for (BluetoothGattCharacteristic c : barometerChars) {
//                                // if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
//                                if (c.getUuid().toString().equals("f000aa41-0451-4000-b000-000000000000")) {
//                                    mBarometerDataC = c;
//                                }
//                                // if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
//                                if (c.getUuid().toString().equals("f000aa42-0451-4000-b000-000000000000")) {
//                                    mBarometerConfigC = c;
//                                }
//                            }
//                            mBtGatt.setCharacteristicNotification(mBarometerDataC, true);
//                            BluetoothGattDescriptor descriptor =
//                                    mBarometerDataC.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
//                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                            mBtGatt.writeDescriptor(descriptor);
//                            mBtLeService.writeCharacteristic(mBarometerConfigC, (byte)0x01);
//                            break;
//                        }
//                    }

                    Thread worker = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (BluetoothGattService service : serviceList) {

                                // If service is Barometer
                                // if ((service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
                                if ((service.getUuid().toString().compareTo("f000aa40-0451-4000-b000-000000000000")) == 0) {
                                    List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : barometerChars) {
                                        // if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                        if (c.getUuid().toString().equals("f000aa41-0451-4000-b000-000000000000")) {
                                            mBarometerDataC = c;
                                        }
                                        // if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                        if (c.getUuid().toString().equals("f000aa42-0451-4000-b000-000000000000")) {
                                            mBarometerConfigC = c;
                                        }
                                    }
                                    mBtLeService.setCharacteristicNotification(mBarometerDataC, true);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, "Miao! " + mBtLeService.writeCharacteristic(mBarometerConfigC, (byte)0x01));

                                        }
                                    });
                                    break;
                                }
                            }
                        }
                    });
                    worker.start();
                } else {
                    Toast.makeText(getApplication(), "Service discovery failed",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {

                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

                mDeviceView.mBarometerValues.setText(Byte.toString(value[0]));

                Log.d("DeviceActivity","Got Characteristic : " + uuidStr);

            }
        }
    };

}

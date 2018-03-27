package com.example.xujia.posturemonitor.sensornode;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BleDeviceInfo;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.util.CustomToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by xujia on 2018-02-20.
 */

public class ScanView extends Fragment {

    private static String TAG = "ScanView";
    private String giraffe = "00:07:80:2D:9E:F2";

    UUID notifyDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int REQ_DEVICE_ACT = 1;

    private MainActivity mActivity;
    private Context mContext;
    private Intent mDeviceIntent;
    private IntentFilter mFilter;

    // For http client user
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    double currentAccelX = -8888;
    double currentAccelY = -8888;
    double currentAccelZ = -8888;
    double currentMagX = -8888;
    double currentMagY = -8888;
    double currentMagZ = -8888;
    double currentGyroX = -8888;
    double currentGyroY = -8888;
    double currentGyroZ = -8888;
    double currentBaro = -8888;

    // BLE
    private BluetoothLeService mBluetoothLeService;
    public int mNumberOfConfiguredDevices;

    // Handle automatic disconnection
    private boolean working = false;
    private int reconnectPosition = -1;

    // Widgets
    private Button mBtnScan;
    private DeviceListAdapter mDeviceAdapter;
    private ListView mDeviceListView;
    private Button mBtnStream;
    private Button mBtnConnectAll;

    // Temp
    private BluetoothGattCharacteristic mBarometerDataC;
    private BluetoothGattCharacteristic mBarometerConfigC;
    private BluetoothGattCharacteristic mMovDataC;
    private BluetoothGattCharacteristic mMovConfigC;

    private BluetoothGatt mBtGatt;
    private boolean connecetSingleSensor = false;
    private int currentSensorPosition = -1;
    private int currentOnConnectPosition = 0;
    private int currentDiscoverServicePosition = 0;
    private boolean connectAll = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Log.i(TAG, "onCreateView");

        // The last two arguments ensure LayoutParams are inflated properly.
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        mActivity = (MainActivity) getActivity();
        mContext = mActivity.getApplicationContext();

        mBluetoothLeService = BluetoothLeService.getInstance();

        // Initialize widgets and ListView adapter
        mBtnScan = (Button) view.findViewById(R.id.btn_scan);
        mBtnConnectAll = (Button) view.findViewById(R.id.btn_connect_all);
//        mBtnConnectAll.setOnClickListener(v -> {
//            for (int i=0; i<mActivity.getDeviceInfoList().size(); i++) {
//                BluetoothDevice device = mActivity.getDeviceInfoList().get(i).getBluetoothDevice();
//                mDeviceAdapter.onConnect(device, i);
//            }
//        });
        mBtnConnectAll.setOnClickListener(v -> {
            if (working) {
                connecetSingleSensor = false;
                working = false;
            }
            currentOnConnectPosition = 0;
            currentDiscoverServicePosition = 0;
            connectAll = true;
            BluetoothDevice device = mActivity.getDeviceInfoList().get(0).getBluetoothDevice();
            mDeviceAdapter.onConnect(device, 0);
        });
        mBtnStream = (Button) view.findViewById(R.id.btn_stream);
//        mBtnStream.setOnClickListener(v -> {
//            // Create GATT object for each device
//            for (int i = 0; i < mActivity.getDeviceInfoList().size(); i++) {
//                mBtGatt = mBluetoothLeService.getBtGatt(i);
//                if (mBtGatt != null) {
//                    mBtGatt.discoverServices();
//                }
//            }
//        });
        mBtnStream.setOnClickListener(v -> {
            mBtGatt = mBluetoothLeService.getBtGatt(0);
            if (mBtGatt != null) {
                mBtGatt.discoverServices();
            }
        });

        mDeviceListView = (ListView) view.findViewById(R.id.device_list);
        List<BleDeviceInfo> deviceList = mActivity.getDeviceInfoList();
        mDeviceAdapter = new DeviceListAdapter(mActivity, deviceList);
        mDeviceListView.setAdapter(mDeviceAdapter);

        // Register the BroadcastReceiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        mFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        mFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        mFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        mFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        mFilter.addAction(BluetoothLeService.ACTION_DATA_READ);

        mActivity.registerReceiver(mReceiver, mFilter);

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mActivity.registerReceiver(mReceiver, mFilter);
        startTimer();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        stoptimertask();
        super.onPause();
        mActivity.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mActivity.unregisterReceiver(mReceiver);
        stoptimertask();
        super.onDestroy();
    }

    protected void setButtonText(String text) {
        mBtnScan.setText(text);
    }

    protected void showConnectAllButton() {
        mBtnScan.setText("Scan");
        mBtnConnectAll.setVisibility(View.VISIBLE);
    }

    protected BluetoothLeService getBlEService() {
        return mBluetoothLeService;
    }

    void notifyDataSetChanged() {
        mDeviceAdapter.notifyDataSetChanged();
    }

    // Adapter to render device list
    @SuppressLint("InflateParams")
    class DeviceListAdapter extends BaseAdapter {
        private List<BleDeviceInfo> mDevices;
        private LayoutInflater mInflater;

        public DeviceListAdapter(Context context, List<BleDeviceInfo> devices) {
            mInflater = LayoutInflater.from(context);
            mDevices = devices;
        }

        public int getCount() {
            return mDevices.size();
        }

        public Object getItem(int position) {
            return mDevices.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, final View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) mInflater.inflate(R.layout.element_device, null);
            }

            BleDeviceInfo deviceInfo = mDevices.get(position);
            BluetoothDevice device = deviceInfo.getBluetoothDevice();
            int rssi = deviceInfo.getRssi();
            String name = device.getName();

            String descr = name + "\n" + device.getAddress() + "\nRssi: " + rssi + " dBm";
            ((TextView) vg.findViewById(R.id.descr)).setText(descr);

            vg.findViewById(R.id.btnConnect).setOnClickListener(view -> {
                connecetSingleSensor = true;
                currentSensorPosition = position;
                onConnect(device, position);
            });

            vg.findViewById(R.id.btnData).setOnClickListener(view -> {
                startDeviceActivity(device, position);
            });

            return vg;
        }

        void onConnect(BluetoothDevice device, int position) {
            if (mDevices.size() > 0) {
                int connState = mActivity.mBluetoothManager.getConnectionState(device,
                        BluetoothGatt.GATT);

                switch (connState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        mBluetoothLeService.disconnect(device, position);
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        boolean ok = mBluetoothLeService.connect(device.getAddress(), position);
                        if (!ok) {
                            Toast.makeText(mContext, "Connect failed", Toast.LENGTH_LONG).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

    }

    private int findPosition(String address) {
        List<BleDeviceInfo> devices = mActivity.getDeviceInfoList();
        for (int i = 0; i < devices.size(); i++) {
            if (address.equals(devices.get(i).getAddress())) {
                return i;
            }
        }
        return -1;
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    private void showDataButtons() {
        for (int i = 0; i < mNumberOfConfiguredDevices; i++) {
            getViewByPosition(i, mDeviceListView).findViewById(R.id.btnData).setVisibility(View.VISIBLE);
        }
    }

    private void showDataButton(int position) {
        getViewByPosition(position, mDeviceListView).findViewById(R.id.btnData).setVisibility(View.VISIBLE);
    }

    // Broadcasted actions from Bluetooth adapter and BluetoothLeService
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        List<BluetoothGattCharacteristic> charList = new ArrayList<BluetoothGattCharacteristic>();

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                // GATT connect
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_FAILURE);
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    CustomToast.middleBottom(mContext, "Connected. Address " + address + ". Status: " + status);
                    int position = findPosition(address);
                    ((Button) getViewByPosition(position, mDeviceListView).findViewById(R.id.btnConnect)).setText("Disconnect");

                    if (mBluetoothLeService.numConnectedDevices() == mActivity.NUMBER_OF_DEVICES) {
                        mBtnStream.setVisibility(View.VISIBLE);
                        mBtnConnectAll.setText("Disconnect all");
                    }

                    if (reconnectPosition != -1) {
                        mBtGatt = mBluetoothLeService.getBtGatt(reconnectPosition);
                        mBtGatt.discoverServices();
                    } else if (connectAll) {
                        currentOnConnectPosition++;
                        if (currentOnConnectPosition < mActivity.getDeviceInfoList().size()) {
                            BluetoothDevice device = mActivity.getDeviceInfoList().get(currentOnConnectPosition).getBluetoothDevice();
                            mDeviceAdapter.onConnect(device, currentOnConnectPosition);
                        }
                    } else if (connecetSingleSensor) {
                        mBtGatt = mBluetoothLeService.getBtGatt(currentSensorPosition);
                        mBtGatt.discoverServices();
                    }

                } else {
                    CustomToast.middleBottom(mContext, "Connection failed. Status: " + status);
                }

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);

                Log.d(TAG, "Disconnected from " + address);
                CustomToast.middleBottom(mContext, "Disconnected from " + address);
                int position = findPosition(address);
                ((Button) getViewByPosition(position, mDeviceListView).findViewById(R.id.btnConnect)).setText("Connect");
                getViewByPosition(position, mDeviceListView).findViewById(R.id.btnData).setVisibility(View.GONE);

                // Handle unwanted disconnection
                if (working) {
                    reconnectPosition = position;
                    BluetoothDevice device = mActivity.getDeviceInfoList().get(position).getBluetoothDevice();
                    mDeviceAdapter.onConnect(device, position);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (connectAll) {
                    mNumberOfConfiguredDevices--;
                    currentOnConnectPosition++;
                    if (currentOnConnectPosition < mActivity.getDeviceInfoList().size()) {
                        BluetoothDevice device = mActivity.getDeviceInfoList().get(currentOnConnectPosition).getBluetoothDevice();
                        mDeviceAdapter.onConnect(device, currentOnConnectPosition);
                    }
                } else if (connecetSingleSensor) {
                    mNumberOfConfiguredDevices--;
                }

                if (mBluetoothLeService.numConnectedDevices() == 0) {
                    mBtnConnectAll.setText("Connect all");
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_SUCCESS);
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);

                if (reconnectPosition != -1) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        final List<BluetoothGattService> mServiceList = mBluetoothLeService.getSupportedGattServices(reconnectPosition);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        for (BluetoothGattService service : mServiceList) {
                            // If device is BLE113 sensornode
                            if (address.equals(giraffe)) {
                                if (!service.getUuid().toString().contains("1800")) {
                                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : chars) {
                                        enableNotification(c, reconnectPosition);
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {    // If device is a CC2650 sensortags
                                if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString()) == 0) {
                                    List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : barometerChars) {
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                            mBarometerDataC = c;
                                        }
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                            mBarometerConfigC = c;
                                        }
                                    }
                                    mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, reconnectPosition);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, reconnectPosition);
                                        }
                                    });
                                }
                                if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                                    List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : humidityChars) {
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_DATA.toString())) {
                                            mMovDataC = c;
                                        }
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_CONF.toString())) {
                                            mMovConfigC = c;
                                        }
                                    }
                                    mBluetoothLeService.setCharacteristicNotification(mMovDataC, true, reconnectPosition);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            byte[] config = {0x01, 0x7f};
                                            mBluetoothLeService.writeCharacteristic(mMovConfigC, config, reconnectPosition);
                                        }
                                    });
                                }
                            }
                        }
                        showDataButton(reconnectPosition);
                    } else {
                        Toast.makeText(mContext, "Service discovery failed",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } else if (connectAll) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        final List<BluetoothGattService> mServiceList = mBluetoothLeService.getSupportedGattServices(currentDiscoverServicePosition);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        for (BluetoothGattService service : mServiceList) {
                            // If device is BLE113 sensornode
                            if (address.equals(giraffe)) {
                                if (!service.getUuid().toString().contains("1800")) {
                                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : chars) {
                                        enableNotification(c, currentSensorPosition);
                                        try {
                                            Thread.sleep(200);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else {    // If device is a CC2650 sensortags
                                if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString()) == 0) {
                                    List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : barometerChars) {
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                            mBarometerDataC = c;
                                        }
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                            mBarometerConfigC = c;
                                        }
                                    }
                                    mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, currentSensorPosition);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, currentSensorPosition);
                                        }
                                    });
                                }
                                if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                                    List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                    for (BluetoothGattCharacteristic c : humidityChars) {
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_DATA.toString())) {
                                            mMovDataC = c;
                                        }
                                        if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_CONF.toString())) {
                                            mMovConfigC = c;
                                        }
                                    }
                                    mBluetoothLeService.setCharacteristicNotification(mMovDataC, true, currentSensorPosition);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            byte[] config = {0x01, 0x7f};
                                            mBluetoothLeService.writeCharacteristic(mMovConfigC, config, currentSensorPosition);
                                        }
                                    });
                                }
                            }
                        }
                        mNumberOfConfiguredDevices++;
                        showDataButton(currentDiscoverServicePosition);
                        if (mNumberOfConfiguredDevices == mActivity.NUMBER_OF_DEVICES) {
                            mBtnConnectAll.setText("Stop streaming and disconnect devices");
                            mBtnStream.setVisibility(View.GONE);
                            working = true;
                            reconnectPosition = -1;
                        }
                        currentDiscoverServicePosition++;
                        if (currentDiscoverServicePosition < mActivity.getDeviceInfoList().size()) {
                            mBtGatt = mBluetoothLeService.getBtGatt(currentDiscoverServicePosition);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (mBtGatt != null) {
                                mBtGatt.discoverServices();
                            }
                        }
                    } else {
                        Toast.makeText(mContext, "Service discovery failed",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } else if (connecetSingleSensor) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        final List<BluetoothGattService> mServiceList = mBluetoothLeService.getSupportedGattServices(currentSensorPosition);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Thread worker = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (BluetoothGattService service : mServiceList) {
                                    // If device is BLE113 sensornode
                                    if (address.equals(giraffe)) {
                                        if (!service.getUuid().toString().contains("1800")) {
                                            List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                                            for (BluetoothGattCharacteristic c : chars) {
                                                enableNotification(c, currentSensorPosition);
                                                try {
                                                    Thread.sleep(200);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    } else {    // If device is a CC2650 sensortags
                                        if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString()) == 0) {
                                            List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                            for (BluetoothGattCharacteristic c : barometerChars) {
                                                if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                                    mBarometerDataC = c;
                                                }
                                                if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                                    mBarometerConfigC = c;
                                                }
                                            }
                                            mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, currentSensorPosition);
                                            try {
                                                Thread.sleep(100);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, currentSensorPosition);
                                                }
                                            });
                                        }
                                        if (service.getUuid().toString().compareTo(SensorTagGatt.UUID_MOV_SERV.toString()) == 0) {
                                            List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                            for (BluetoothGattCharacteristic c : humidityChars) {
                                                if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_DATA.toString())) {
                                                    mMovDataC = c;
                                                }
                                                if (c.getUuid().toString().equals(SensorTagGatt.UUID_MOV_CONF.toString())) {
                                                    mMovConfigC = c;
                                                }
                                            }
                                            mBluetoothLeService.setCharacteristicNotification(mMovDataC, true, currentSensorPosition);
                                            try {
                                                Thread.sleep(100);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            mActivity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    byte[] config = {0x7f, 0x01};
                                                    mBluetoothLeService.writeCharacteristic(mMovConfigC, config, currentSensorPosition);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        });
                        worker.start();
                        mNumberOfConfiguredDevices++;
                        showDataButton(currentSensorPosition);
                        if (mNumberOfConfiguredDevices == mActivity.NUMBER_OF_DEVICES) {
                            mBtnConnectAll.setText("Stop streaming and disconnect devices");
                            mBtnStream.setVisibility(View.GONE);
                            working = true;
                        }
                    } else {
                        Toast.makeText(mContext, "Service discovery failed",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                Log.d(TAG, "Got Characteristic : " + uuidStr);
                if (uuidStr.contains("aa41")) {
                    currentBaro = getPressureData(value);
                } else if (uuidStr.contains("aa81")) {
                    getGyroData(value);
                    getAccelData(value);
                    getMagData(value);
                }
            } else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                Log.d(TAG, "Written Characteristic : " + uuidStr);
            } else if (BluetoothLeService.ACTION_DATA_READ.equals(action)) {
                // Data read
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.d(TAG, "Read Characteristic : " + value.toString());
            }
        }

    };

    public void startDeviceActivity(BluetoothDevice device, int position) {
        mDeviceIntent = new Intent(mContext, DeviceActivity.class);
        mDeviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE, device);
        mDeviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE_POSITION, position);
        startActivityForResult(mDeviceIntent, REQ_DEVICE_ACT);
    }

    private void enableNotification(BluetoothGattCharacteristic c, int position) {
        mBluetoothLeService.getBtGatt(position).setCharacteristicNotification(c, true);
        BluetoothGattDescriptor descriptor = c.getDescriptor(notifyDescriptorUuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothLeService.getBtGatt(position).writeDescriptor(descriptor);
    }

    // Barometer from CC2650 has 6 bytes; bytes 0-2 is temperature; bytes 3-5 is barometer
    private double getPressureData(byte[] value) {
        Integer rawValue = twentyFourBitUnsignedAtOffset(value, 3);
        return rawValue / 100f;
    }

    private Integer twentyFourBitUnsignedAtOffset(byte[] value, int offset) {
        Integer lowerByte = (int) value[offset] & 0xFF;    // value[3]
        Integer mediumByte = (int) value[offset + 1] & 0xFF;    // value[4]
        Integer upperByte = (int) value[offset + 2] & 0xFF;    // value[5]
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    private void getGyroData(byte[] value) {
        int x = (value[1] << 8) + value[0];
        currentGyroX = (x * 1.0) / (65536 / 500);
        int y = (value[3] << 8) + value[2];
        currentGyroY = (y * 1.0) / (65536 / 500);
        int z = (value[5] << 8) + value[4];
        currentGyroZ = (z * 1.0) / (65536 / 500);
    }

    private void getAccelData(byte[] value) {
        int x = (value[7] << 8) + value[6];
        currentAccelX = (x * 1.0) / 8192;    // 32768/4
        int y = (value[9] << 8) + value[8];
        currentAccelY = (y * 1.0) / 8192;
        int z = (value[11] << 8) + value[10];
        currentAccelZ = (z * 1.0) / 8192;
    }

    private void getMagData(byte[] value) {
        int x = (value[13] << 8) + value[12];
        currentMagX = x * 1.0;
        int y = (value[15] << 8) + value[14];
        currentMagY = y * 1.0;
        int z = (value[17] << 8) + value[16];
        currentMagZ = z * 1.0;
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 50, 100);
    }

    public void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class MyResponseListner implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            Log.i("VOLLEY", response);
        }
    }

    private static class MyErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("VOLLEY", error.toString());
        }
    }

    private static class JsonRequest extends StringRequest {
        private String requestBody;

        public JsonRequest(String requestBody, int method, String url, Response.Listener<String> listener,
                           Response.ErrorListener errorListener) {
            super(method, url, listener, errorListener);
            this.requestBody = requestBody;
        }

        @Override
        public String getBodyContentType() {
            return "application/json; charset=utf-8";
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            try {
                return requestBody == null ? null : requestBody.getBytes("utf-8");
            } catch (UnsupportedEncodingException uee) {
                VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                return null;
            }
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String responseString = "";
            if (response != null) {
                responseString = String.valueOf(response.statusCode);
                // can get more details such as response.headers
            }
            return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        JSONObject jsonBody = new JSONObject();
                        if ((currentBaro == -8888) || (currentMagZ == -8888)) {
                            return;
                        }
                        try {
                            jsonBody.put("Timestamp", timestamp);
                            jsonBody.put("Baro", Double.toString(currentBaro));
                            jsonBody.put("AccelX", currentAccelX);
                            jsonBody.put("AccelY", currentAccelY);
                            jsonBody.put("AccelZ", currentAccelZ);
                            jsonBody.put("MagX", currentMagX);
                            jsonBody.put("MagY", currentMagY);
                            jsonBody.put("MagZ", currentMagZ);
                            jsonBody.put("GyroX", currentGyroX);
                            jsonBody.put("GyroY", currentGyroY);
                            jsonBody.put("GyroZ", currentGyroZ);
                            final String requestBody = jsonBody.toString();
                            StringRequest stringRequest = new JsonRequest(requestBody, Request.Method.POST, MainActivity.URL,
                                    new MyResponseListner(), new MyErrorListener());
                            MainActivity.requestQueue.add(stringRequest);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

}
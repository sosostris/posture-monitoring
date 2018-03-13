package com.example.xujia.posturemonitor.sensornode;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
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

import com.example.xujia.posturemonitor.R;
import com.example.xujia.posturemonitor.common.BleDeviceInfo;
import com.example.xujia.posturemonitor.common.BluetoothLeService;
import com.example.xujia.posturemonitor.util.CustomToast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by xujia on 2018-02-20.
 */

public class ScanView extends Fragment {

    private static String TAG = "ScanView";
    private String baroDevice = "B0:B4:48:BD:0C:84";
    private String humiDevice = "B0:B4:48:BC:53:87";
    private String giraffe = "00:07:80:2D:9E:F2";

    private static final int REQ_DEVICE_ACT = 1;

    private MainActivity mActivity;
    private Context mContext;
    private Intent mDeviceIntent;
    private IntentFilter mFilter;

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
    private BluetoothGattCharacteristic mHumidityDataC;
    private BluetoothGattCharacteristic mHumidityConfigC;
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
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
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
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mServiceList.size() > 0) {
                            for (int j = 0; j < mServiceList.size(); j++) {
                                BluetoothGattService s = mServiceList.get(j);
                                List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                                if (c.size() > 0) {
                                    for (int k = 0; k < c.size(); k++) {
                                        charList.add(c.get(k));
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Total characteristics " + charList.size());

                        for (BluetoothGattService service : mServiceList) {

                            // If service is Barometer
                            if (address.equals(baroDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
                                mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, reconnectPosition);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, reconnectPosition);
                                    }
                                });
                            } else if (address.equals(humiDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_HUM_SERV.toString())) == 0) {
                                mBluetoothLeService.setCharacteristicNotification(mHumidityDataC, true, reconnectPosition);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBluetoothLeService.writeCharacteristic(mHumidityConfigC, (byte) 0x01, reconnectPosition);
                                    }
                                });
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
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mServiceList.size() > 0) {
                            for (int j = 0; j < mServiceList.size(); j++) {
                                BluetoothGattService s = mServiceList.get(j);
                                List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                                if (c.size() > 0) {
                                    for (int k = 0; k < c.size(); k++) {
                                        charList.add(c.get(k));
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Total characteristics " + charList.size());

                        for (BluetoothGattService service : mServiceList) {

                            // If service is Barometer
                            if (address.equals(baroDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
                                List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                for (BluetoothGattCharacteristic c : barometerChars) {
                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                        mBarometerDataC = c;
                                    }
                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                        mBarometerConfigC = c;
                                    }
                                }
                                mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, currentDiscoverServicePosition);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, currentDiscoverServicePosition);
                                    }
                                });
                            } else if (address.equals(humiDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_HUM_SERV.toString())) == 0) {
                                List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                for (BluetoothGattCharacteristic c : humidityChars) {
                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_DATA.toString())) {
                                        mHumidityDataC = c;
                                    }
                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_CONF.toString())) {
                                        mHumidityConfigC = c;
                                    }
                                }
                                mBluetoothLeService.setCharacteristicNotification(mHumidityDataC, true, currentDiscoverServicePosition);
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBluetoothLeService.writeCharacteristic(mHumidityConfigC, (byte) 0x01, currentDiscoverServicePosition);
                                    }
                                });
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
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mServiceList.size() > 0) {
                            for (int j = 0; j < mServiceList.size(); j++) {
                                BluetoothGattService s = mServiceList.get(j);
                                List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                                if (c.size() > 0) {
                                    for (int k = 0; k < c.size(); k++) {
                                        charList.add(c.get(k));
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Total characteristics " + charList.size());

                        Thread worker = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (BluetoothGattService service : mServiceList) {

                                    // If service is Battery service
                                    if (address.equals(giraffe) && (service.getUuid().toString().contains("180f"))) {
                                        List<BluetoothGattCharacteristic> batteryChars = service.getCharacteristics();
                                        for (BluetoothGattCharacteristic c : batteryChars) {
                                            if (c.getUuid().toString().contains("2a19")) {
                                                mActivity.mBatteryC = c;
                                            }
                                        }
                                    }
                                    // If service is Posture service
                                    if (address.equals(giraffe) && (service.getUuid().toString().contains("2222"))) {
                                        List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                        for (BluetoothGattCharacteristic c : barometerChars) {
                                            if (c.getUuid().toString().contains("2aa3")) {
                                                mBluetoothLeService.getBtGatt(currentSensorPosition).setCharacteristicNotification(c, true);
                                                UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                                                BluetoothGattDescriptor descriptor = c.getDescriptor(uuid);
                                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                                mBluetoothLeService.getBtGatt(currentSensorPosition).writeDescriptor(descriptor);
                                            }
                                        }
                                    }
                                    // If service is Barometer
                                    if (address.equals(baroDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
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
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, currentSensorPosition);
                                            }
                                        });
                                        break;
                                    }
                                    if (address.equals(humiDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_HUM_SERV.toString())) == 0) {
                                        List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                        for (BluetoothGattCharacteristic c : humidityChars) {
                                            if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_DATA.toString())) {
                                                mHumidityDataC = c;
                                            }
                                            if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_CONF.toString())) {
                                                mHumidityConfigC = c;
                                            }
                                        }
                                        mBluetoothLeService.setCharacteristicNotification(mHumidityDataC, true, currentSensorPosition);
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mBluetoothLeService.writeCharacteristic(mHumidityConfigC, (byte) 0x01, currentSensorPosition);
                                            }
                                        });
                                        break;
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
                } else {
                    List<BleDeviceInfo> devices = mActivity.getDeviceInfoList();
                    for (int i = 0; i < devices.size(); i++) {
                        if (devices.get(i).getAddress().equals(address)) {
                            int position = i;
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                final List<BluetoothGattService> mServiceList = mBluetoothLeService.getSupportedGattServices(i);
                                if (mServiceList.size() > 0) {
                                    for (int j = 0; j < mServiceList.size(); j++) {
                                        BluetoothGattService s = mServiceList.get(j);
                                        List<BluetoothGattCharacteristic> c = s.getCharacteristics();
                                        if (c.size() > 0) {
                                            for (int k = 0; k < c.size(); k++) {
                                                charList.add(c.get(k));
                                            }
                                        }
                                    }
                                }
                                Log.d("DeviceActivity", "Total characteristics " + charList.size());

                                Thread worker = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (BluetoothGattService service : mServiceList) {

                                            // If service is Barometer
                                            if (address.equals(baroDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_BAR_SERV.toString())) == 0) {
                                                List<BluetoothGattCharacteristic> barometerChars = service.getCharacteristics();
                                                for (BluetoothGattCharacteristic c : barometerChars) {
                                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_DATA.toString())) {
                                                        mBarometerDataC = c;
                                                    }
                                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_BAR_CONF.toString())) {
                                                        mBarometerConfigC = c;
                                                    }
                                                }
                                                mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, position);
                                                mActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, position);
                                                    }
                                                });
                                                break;
                                            }
                                            if (address.equals(humiDevice) && (service.getUuid().toString().compareTo(SensorTagGatt.UUID_HUM_SERV.toString())) == 0) {
                                                List<BluetoothGattCharacteristic> humidityChars = service.getCharacteristics();
                                                for (BluetoothGattCharacteristic c : humidityChars) {
                                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_DATA.toString())) {
                                                        mHumidityDataC = c;
                                                    }
                                                    if (c.getUuid().toString().equals(SensorTagGatt.UUID_HUM_CONF.toString())) {
                                                        mHumidityConfigC = c;
                                                    }
                                                }
                                                mBluetoothLeService.setCharacteristicNotification(mHumidityDataC, true, position);
                                                mActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mBluetoothLeService.writeCharacteristic(mHumidityConfigC, (byte) 0x01, position);
                                                    }
                                                });
                                                break;
                                            }
                                        }
                                    }
                                });
                                worker.start();
                                mNumberOfConfiguredDevices++;
                                if (mNumberOfConfiguredDevices == mActivity.NUMBER_OF_DEVICES) {
                                    mBtnConnectAll.setText("Stop streaming and disconnect devices");
                                    mBtnStream.setVisibility(View.GONE);
                                    showDataButtons();
                                }
                            } else {
                                Toast.makeText(mContext, "Service discovery failed",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

                Log.d(TAG, "Got Characteristic : " + uuidStr);
                return;
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
}
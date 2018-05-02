package com.example.xujia.posturemonitor.sensornode;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
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

    UUID notifyDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int REQ_DEVICE_ACT = 1;

    private MainActivity mActivity;
    private Context mContext;
    private Intent mDeviceIntent;
    private IntentFilter mFilter;

    // For Bigtable
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    boolean streaming;
    public static String[] currentBatteryLevel;
    public static double[] currentAccelX;
    public static double[] currentAccelY;
    public static double[] currentAccelZ;
    public static double[] currentMagX;
    public static double[] currentMagY;
    public static double[] currentMagZ;
    public static double[] currentGyroX;
    public static double[] currentGyroY;
    public static double[] currentGyroZ;
    public static double[] currentBaro;
    public static String currentPosture;

    // TCP connection with Java Bigtable server
    public static Socket btSocket;
    public static PrintWriter btOut;

    // For MATLAB
    public static int[] deviceModel;
    public static byte[][] currentAccelXByte;
    public static byte[][] currentAccelYByte;
    public static byte[][] currentAccelZByte;
    public static byte[][] currentMagXByte;
    public static byte[][] currentMagYByte;
    public static byte[][] currentMagZByte;
    public static byte[][] currentGyroXByte;
    public static byte[][] currentGyroYByte;
    public static byte[][] currentGyroZByte;
    public static byte[][] currentBaroByte;

    // BLE
    private BluetoothLeService mBluetoothLeService;

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
    private boolean connectAll = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Log.i(TAG, "onCreateView");

        // The last two arguments ensure LayoutParams are inflated properly.
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        mActivity = (MainActivity) getActivity();
        mContext = mActivity.getApplicationContext();

        mBluetoothLeService = BluetoothLeService.getInstance();
        streaming = false;

        // Initialize widgets and ListView adapter
        mBtnScan = (Button) view.findViewById(R.id.btn_scan);
        mBtnConnectAll = (Button) view.findViewById(R.id.btn_connect_all);
        mBtnConnectAll.setOnClickListener(v -> {
            if (working) {
                connecetSingleSensor = false;
                reconnectPosition = -1;
                working = false;
            }
            currentOnConnectPosition = 0;
            connectAll = true;
            BluetoothDevice device = mActivity.getDeviceInfoList().get(0).getBluetoothDevice();
            mDeviceAdapter.onConnect(device, 0);
        });
        mBtnStream = (Button) view.findViewById(R.id.btn_stream);
        mBtnStream.setOnClickListener(v -> {
            if (streaming) {
                stopTimer();
                mBtnStream.setText("Start uploading data");
                Log.d(TAG, "Timer stopped");
                streaming = false;
            } else {
                configPosture();
                // startTimer();

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

        initUploadInfo();
        initMATLABData();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Please make sure all sensor nodes are worn properly and they are all powered on!")
                .setTitle("Important");
        AlertDialog reminderDialog = builder.create();
        reminderDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        reminderDialog.show();

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        // mActivity.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        // stopTimer();
        super.onPause();
        // mActivity.unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mActivity.unregisterReceiver(mReceiver);
        stopTimer();
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

    private void toggleDataBtn(int position, boolean show) {
        View dataBtn = getViewByPosition(position, mDeviceListView).findViewById(R.id.btnData);
        dataBtn.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleConnectBtnText(int position, String text) {
        Button connectBtn = getViewByPosition(position, mDeviceListView).findViewById(R.id.btnConnect);
        connectBtn.setText(text);
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
                    toggleConnectBtnText(position, "Disconnect");

                    if (mBluetoothLeService.numConnectedDevices() == PostureMonitorApplication.NUMBER_OF_SENSORNODE) {
                        working = true;
                        mBtnStream.setVisibility(View.VISIBLE);
                        mBtnConnectAll.setText("Disconnect all");
                    }

                    if (reconnectPosition != -1) {
                        mBtGatt = mBluetoothLeService.getBtGatt(reconnectPosition);
                        mBtGatt.discoverServices();
                    } else if (connecetSingleSensor) {
                        mBtGatt = mBluetoothLeService.getBtGatt(currentSensorPosition);
                        mBtGatt.discoverServices();
                    } else if (connectAll) {
                        // Discover service for the current sensornode
                        mBtGatt = mBluetoothLeService.getBtGatt(currentOnConnectPosition);
                        mBtGatt.discoverServices();
                        // Connect the next sensornode
                        currentOnConnectPosition++;
                        if (currentOnConnectPosition < PostureMonitorApplication.NUMBER_OF_SENSORNODE) {
                            BluetoothDevice device = mActivity.getDeviceInfoList().get(currentOnConnectPosition).getBluetoothDevice();
                            mDeviceAdapter.onConnect(device, currentOnConnectPosition);
                        }
                    }

                } else {
                    CustomToast.middleBottom(mContext, "Connection failed. Status: " + status);
                }

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);

                Log.d(TAG, "Disconnected from " + address);
                CustomToast.middleBottom(mContext, "Disconnected from " + address);
                int position = findPosition(address);
                toggleConnectBtnText(position, "Connect");
                toggleDataBtn(position, false);

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
                    currentOnConnectPosition++;
                    if (currentOnConnectPosition < mActivity.getDeviceInfoList().size()) {
                        BluetoothDevice device = mActivity.getDeviceInfoList().get(currentOnConnectPosition).getBluetoothDevice();
                        mDeviceAdapter.onConnect(device, currentOnConnectPosition);
                    }
                }
                if (mBluetoothLeService.numConnectedDevices() == 0) {
                    mBtnStream.setVisibility(View.GONE);
                    mBtnStream.setText("Start uploading data");
                    mBtnConnectAll.setText("Connect all");
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_SUCCESS);
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);
                int position = findPosition(address);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    final List<BluetoothGattService> mServiceList = mBluetoothLeService.getSupportedGattServices(position);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (BluetoothGattService service : mServiceList) {
                        // If device is BLE113 sensornode
                        if (PostureMonitorApplication.ADDRESS_TYPE_MAP.get(address).equals("BLE")) {
                            if (!service.getUuid().toString().contains("1800")) {
                                List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                                for (BluetoothGattCharacteristic c : chars) {
                                    enableNotification(c, position);
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {    // If device is a CC2650 sensortag
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
                                mBluetoothLeService.setCharacteristicNotification(mBarometerDataC, true, position);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mBluetoothLeService.writeCharacteristic(mBarometerConfigC, (byte) 0x01, position);
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
                                mBluetoothLeService.setCharacteristicNotification(mMovDataC, true, position);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        byte[] config = {0x7f, 0x01};
                                        mBluetoothLeService.writeCharacteristic(mMovConfigC, config, position);
                                    }
                                });
                            }
                        }
                    }
                    toggleDataBtn(position, true);
                } else {
                    Toast.makeText(mContext, "Service discovery failed",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
                // Notification
                byte[] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);
                int position = findPosition(address);
                Log.d(TAG, "Got Characteristic : " + uuidStr + " " + address);

                // CC2650 Sensortag
                if (uuidStr.contains("aa41")) {
                    currentBaro[position] = getPressureDataCC2650(value, position);
                } else if (uuidStr.contains("aa81")) {
                    getGyroDataCC2650(value, position);
                    getAccelDataCC2650(value, position);
                    getMagDataCC2650(value, position);

                // BLE113 Sensornode
                } else if (uuidStr.contains("8882")) {
                    currentBaro[position] = getPressureDataBLE113(value, position);
                } else if (uuidStr.contains("8884")) {
                    getGyroDataBLE113(value, position);
                } else if (uuidStr.contains("8886")) {
                    getAccelDataBLE113(value, position);
                } else if (uuidStr.contains("8888")) {
                    getMagDataBLE113(value, position);
                } else if (uuidStr.contains("2a19")) {
                    String battery = buildString(value);
                    currentBatteryLevel[position] = battery;
                    Log.d(TAG,"Got characteristic from " + address + ": " + uuidStr + "battery level: " + buildString(value));
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

    private void configPosture() {
        final AlertDialog.Builder alert= new AlertDialog.Builder(mActivity);
        alert.setMessage("Enter posture:");
        final EditText posture = new EditText(mActivity);
        posture.setHint("E.g. walking, sitting");
        posture.setWidth(280);
        alert.setView(posture);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                if (posture.getText().toString().length() > 0){
                    currentPosture = posture.getText().toString();
                    startTimer();
                    Log.d(TAG, "Timer started");
                    mBtnStream.setText("Stop uploading data (" + currentPosture + ")");
                    streaming = true;
                }else{
                    CustomToast.middleBottom(mActivity, "Posture can't be empty.");
                    return;
                }
            }
        });
        alert.show();

    }

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
    private double getPressureDataCC2650(byte[] value, int position) {
        Integer rawValue = twentyFourBitUnsignedAtOffset(value, 3);
        currentBaroByte[position][0] = value[5];
        currentBaroByte[position][1] = value[4];
        currentBaroByte[position][2] = value[3];
        return rawValue / 100f;
    }

    private double getPressureDataBLE113(byte[] value, int position) {
        // The data in BLE113 is stored in little endian order so we need to reverse order
        byte[] valueInOrder = new byte[4];
        valueInOrder[0] = 0;
        for (int i=1; i<4; i++) {
            valueInOrder[i] = value[3-i];
        }
        writeToMATLABBytesBLE113(valueInOrder, position, "bar");
        int correctData = byteToInt(valueInOrder);
        int hPa = correctData / 4096;
        return hPa;
    }

    private int byteToInt(byte[] bytes) {
        int result = 0;
        for (int i=0; i<4; i++) {
            result = ( result << 8 ) + (int) bytes[i];
        }
        return result;
    }

    private void getGyroDataBLE113(byte[] value, int position) {
        byte[] dataInOrder = getReversedBytes(value, 6);
        writeToMATLABBytesBLE113(dataInOrder, position, "gyr");
        int z = (dataInOrder[0] << 8) | dataInOrder[1];
        int y = (dataInOrder[2] << 8) | dataInOrder[3];
        int x = (dataInOrder[4] << 8) | dataInOrder[5];
        // We use +/- 500 dps
        currentGyroX[position] = (x * 1.0) * 1000 / 65536;
        currentGyroY[position] = (y * 1.0) * 1000 / 65536;
        currentGyroZ[position] = (z * 1.0) * 1000 / 65536;
    }

    private void getAccelDataBLE113(byte[] value, int position) {
        byte[] dataInOrder = getReversedBytes(value, 6);
        writeToMATLABBytesBLE113(dataInOrder, position, "acc");
        int z = (dataInOrder[0] << 8) | dataInOrder[1];
        int y = (dataInOrder[2] << 8) | dataInOrder[3];
        int x = (dataInOrder[4] << 8) | dataInOrder[5];
        // We use +/- 4g
        currentAccelX[position] = (x * 1.0) / 8192;
        currentAccelY[position] = (y * 1.0) / 8192;
        currentAccelZ[position] = (z * 1.0) / 8192;
    }

    /*
    The magnetic field of a certain point is directional and has magnitude.
    The x,y,z returned by the sensor is a vector that describes the strength and direction
    of the magnetic field relative to the device measured in microtesla.
    For example, a value of (x, y, z) = (50, 50, 0) means that there is a magnetic field
    sqrt(50**2+50**2+0**2) ≈ 70.7107 microtesla strong in the direction of 45 degree to X-axis and Y-axis of the device.
     */
    private void getMagDataBLE113(byte[] value, int position) {
        byte[] dataInOrder = getReversedBytes(value, 6);
        writeToMATLABBytesBLE113(dataInOrder, position, "mag");
        int z = (dataInOrder[0] << 8) | dataInOrder[1];
        int y = (dataInOrder[2] << 8) | dataInOrder[3];
        int x = (dataInOrder[4] << 8) | dataInOrder[5];
        // We use +/- 4g
        // 1 Gauss = 100 mT
        currentMagX[position] = x * 1.0 * 100 / 8192;
        currentMagY[position] = y * 1.0 * 100 / 8192;
        currentMagZ[position] = z * 1.0 * 100 / 8192;
    }

    // Returns data in
    private byte[] getReversedBytes(byte[] bytes, int length) {
        byte[] reversedBytes = new byte[length];
        for (int i=0; i<length; i++) {
            reversedBytes[i] = bytes[length-i-1];
        }
        return reversedBytes;
    }

    private String buildString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<bytes.length; i++) {
            builder.append(bytes[i]);
            builder.append(" ");
        }
        return builder.toString();
    }

    private Integer twentyFourBitUnsignedAtOffset(byte[] value, int offset) {
        Integer lowerByte = (int) value[offset] & 0xFF;    // value[3]
        Integer mediumByte = (int) value[offset + 1] & 0xFF;    // value[4]
        Integer upperByte = (int) value[offset + 2] & 0xFF;    // value[5]
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    private void getGyroDataCC2650(byte[] value, int position) {
        int x = (value[1] << 8) | value[0];
        currentGyroX[position] = (x * 1.0) * 500 / 65536;    // (x * 1.0) / (65536 / 500)
        int y = (value[3] << 8) | value[2];
        currentGyroY[position] = (y * 1.0) * 500 / 65536;
        int z = (value[5] << 8) | value[4];
        currentGyroZ[position] = (z * 1.0) * 500 / 65536;
        currentGyroXByte[position][0] = value[1];
        currentGyroXByte[position][1] = value[0];
        currentGyroYByte[position][0] = value[3];
        currentGyroYByte[position][1] = value[2];
        currentGyroZByte[position][0] = value[5];
        currentGyroZByte[position][1] = value[4];
    }

    private void getAccelDataCC2650(byte[] value, int position) {
        int x = (value[7] << 8) | value[6];
        currentAccelX[position] = (x * 1.0) / 8192;    // 32768/4
        int y = (value[9] << 8) | value[8];
        currentAccelY[position] = (y * 1.0) / 8192;
        int z = (value[11] << 8) | value[10];
        currentAccelZ[position] = (z * 1.0) / 8192;
        currentAccelXByte[position][0] = value[7];
        currentAccelXByte[position][1] = value[6];
        currentAccelYByte[position][0] = value[9];
        currentAccelYByte[position][1] = value[8];
        currentAccelZByte[position][0] = value[11];
        currentAccelZByte[position][1] = value[10];
    }

    private void getMagDataCC2650(byte[] value, int position) {
        int x = (value[13] << 8) | value[12];
        currentMagX[position] = x * 1.0;
        int y = (value[15] << 8) | value[14];
        currentMagY[position] = y * 1.0;
        int z = (value[17] << 8) | value[16];
        currentMagZ[position] = z * 1.0;
        currentMagXByte[position][0] = value[13];
        currentMagXByte[position][1] = value[12];
        currentMagYByte[position][0] = value[15];
        currentMagYByte[position][1] = value[14];
        currentMagZByte[position][0] = value[17];
        currentMagZByte[position][1] = value[16];
    }

    private void writeToMATLABBytesBLE113(byte[] data, int position, String sensorType) {
        switch (sensorType) {
            case "acc":
                currentAccelZByte[position][0] = data[0];
                currentAccelZByte[position][1] = data[1];
                currentAccelYByte[position][0] = data[2];
                currentAccelYByte[position][1] = data[3];
                currentAccelXByte[position][0] = data[4];
                currentAccelXByte[position][1] = data[5];
                break;
            case "mag":
                currentMagZByte[position][0] = data[0];
                currentMagZByte[position][1] = data[1];
                currentMagYByte[position][0] = data[2];
                currentMagYByte[position][1] = data[3];
                currentMagXByte[position][0] = data[4];
                currentMagXByte[position][1] = data[5];
                break;
            case "gyr":
                currentGyroZByte[position][0] = data[0];
                currentGyroZByte[position][1] = data[1];
                currentGyroYByte[position][0] = data[2];
                currentGyroYByte[position][1] = data[3];
                currentGyroXByte[position][0] = data[4];
                currentGyroXByte[position][1] = data[5];
                break;
            case "bar":
                currentBaroByte[position][0] = data[1];
                currentBaroByte[position][1] = data[2];
                currentBaroByte[position][2] = data[3];
        }

    }

    private void writeToMATLABBytesCC2650(byte[] data, int position, String sensorType) {
        switch (sensorType) {
            case "acc":
                currentAccelZByte[position][0] = data[0];
                currentAccelZByte[position][1] = data[1];
                currentAccelYByte[position][0] = data[2];
                currentAccelYByte[position][1] = data[3];
                currentAccelXByte[position][0] = data[4];
                currentAccelXByte[position][1] = data[5];
                break;
            case "mag":
                currentMagZByte[position][0] = data[0];
                currentMagZByte[position][1] = data[1];
                currentMagYByte[position][0] = data[2];
                currentMagYByte[position][1] = data[3];
                currentMagXByte[position][0] = data[4];
                currentMagXByte[position][1] = data[5];
                break;
            case "gyr":
                currentGyroZByte[position][0] = data[0];
                currentGyroZByte[position][1] = data[1];
                currentGyroYByte[position][0] = data[2];
                currentGyroYByte[position][1] = data[3];
                currentGyroXByte[position][0] = data[4];
                currentGyroXByte[position][1] = data[5];
                break;
            case "bar":
                currentBaroByte[position][0] = data[1];
                currentBaroByte[position][1] = data[2];
                currentBaroByte[position][2] = data[3];
        }

    }

//    private void startTimer() {
//        timer = new Timer();
//        initializeHttpTimerTask();
//        timer.schedule(timerTask, 1000, 100);
//    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            // timer.purge();
            timer = null;
        }
        try {
            if (btOut != null) {
                btOut.println("ex");
                btOut.close();
            }
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        Thread btWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    btSocket = new Socket(PostureMonitorApplication.JAVA_IP, PostureMonitorApplication.JAVA_PORT_STREAM);
                    if (btSocket != null) {
                        try {
                            btOut = new PrintWriter(btSocket.getOutputStream(), true);
                            Log.d(TAG, "Connected with Bigtable Java TCP server");
                            timer = new Timer();
                            initTCPTimerTask();
                            timer.schedule(timerTask, 1000, 500);    // supposed to be 50
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btWorker.start();
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

    private void initializeHttpTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
                        JSONObject jsonBody = new JSONObject();
                        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
                            if ((currentBaro[i] == -8888) || (currentMagZ[i] == -8888) || (currentBaro[i] == 0) || (currentMagZ[i] == 0)) {
                                continue;
                            }
                            try {
                                jsonBody.put("SensornodeId", MainActivity.mDeviceInfoList.get(i).getName());
                                jsonBody.put("Timestamp", timestamp);
                                jsonBody.put("Baro", currentBaro[i]);
                                jsonBody.put("AccelX", currentAccelX[i]);
                                jsonBody.put("AccelY", currentAccelY[i]);
                                jsonBody.put("AccelZ", currentAccelZ[i]);
                                jsonBody.put("MagX", currentMagX[i]);
                                jsonBody.put("MagY", currentMagY[i]);
                                jsonBody.put("MagZ", currentMagZ[i]);
                                jsonBody.put("GyroX", currentGyroX[i]);
                                jsonBody.put("GyroY", currentGyroY[i]);
                                jsonBody.put("GyroZ", currentGyroZ[i]);
                                currentBaro[i] = -8888;
                                final String requestBody = jsonBody.toString();
                                StringRequest stringRequest = new JsonRequest(requestBody, Request.Method.POST, MainActivity.URL,
                                        new MyResponseListner(), new MyErrorListener());
                                MainActivity.requestQueue.add(stringRequest);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        };
    }

    private void initTCPTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
                            if ((currentBaro[i] == -8888) || (currentMagZ[i] == -8888) || (currentBaro[i] == 0) || (currentMagZ[i] == 0)) {
                                continue;
                            }
                            long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
                            StringBuilder sb = new StringBuilder();
                            sb.append(MainActivity.mDeviceInfoList.get(i).getName() + " ");
                            sb.append(timestamp + " ");
                            sb.append(currentAccelX[i] + " ");
                            sb.append(currentAccelY[i] + " ");
                            sb.append(currentAccelZ[i] + " ");
                            sb.append(currentMagX[i] + " ");
                            sb.append(currentMagY[i] + " ");
                            sb.append(currentMagZ[i] + " ");
                            sb.append(currentGyroX[i] + " ");
                            sb.append(currentGyroY[i] + " ");
                            sb.append(currentGyroZ[i] + " ");
                            sb.append(currentBaro[i] + " ");
                            sb.append(MainActivity.mDeviceInfoList.get(i).getBody() + " ");
                            sb.append(currentPosture);
                            currentBaro[i] = -8888;
                            if (btOut != null) {
                                btOut.println(sb.toString());
                            }
                        }
                    }
                });
            }
        };
    }

    // Initialize all sensornode raw data to -8888
    private void initUploadInfo() {
        currentAccelX = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentAccelY = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentAccelZ = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentMagX = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentMagY = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentMagZ = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentGyroX = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentGyroY = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentGyroZ = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentBaro = new double[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentBatteryLevel = new String[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        for (int i=0; i<PostureMonitorApplication.NUMBER_OF_SENSORNODE; i++) {
            currentAccelX[i] = -8888;
            currentAccelY[i] = -8888;
            currentAccelZ[i] = -8888;
            currentMagX[i] = -8888;
            currentMagY[i] = -8888;
            currentMagZ[i] = -8888;
            currentGyroX[i] = -8888;
            currentGyroY[i] = -8888;
            currentGyroZ[i] = -8888;
            currentBaro[i] = -8888;
            currentBatteryLevel[i] = "Unknown";
        }
    }

    private void initMATLABData() {
        deviceModel = new int[PostureMonitorApplication.NUMBER_OF_SENSORNODE];
        currentAccelXByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentAccelYByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentAccelZByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentMagXByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentMagYByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentMagZByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentGyroXByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentGyroYByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentGyroZByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][2];
        currentBaroByte = new byte[PostureMonitorApplication.NUMBER_OF_SENSORNODE][3];
    }

}
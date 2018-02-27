package com.example.xujia.posturemonitor.sensornode;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import java.util.List;

/**
 * Created by xujia on 2018-02-20.
 */

public class ScanView extends Fragment {

    private static final int REQ_DEVICE_ACT = 1;

    private MainActivity mActivity;
    private Context mContext;
    private Intent mDeviceIntent;
    private IntentFilter mFilter;

    private BluetoothLeService mBluetoothLeService;

    // Widgets
    private Button mBtnScan;
    private DeviceListAdapter mDeviceAdapter;
    private ListView mDeviceListView;

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
        mDeviceListView = (ListView) view.findViewById(R.id.device_list);
        List<BleDeviceInfo> deviceList = mActivity.getDeviceInfoList();
        mDeviceAdapter = new DeviceListAdapter(mActivity,deviceList);
        mDeviceListView.setAdapter(mDeviceAdapter);

        // Register the BroadcastReceiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        mFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        mActivity.registerReceiver(mReceiver, mFilter);

        return view;
    }

    @Override
    public void onDestroy() {
        // Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    protected void setButtonText(String text) {
        mBtnScan.setText(text);
    }

    void notifyDataSetChanged() {
        mDeviceAdapter.notifyDataSetChanged();
    }

    // Adapter to render device list
    @SuppressLint("InflateParams") class DeviceListAdapter extends BaseAdapter {
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
            String name;
            name = device.getName();
            if (name == null) {
                name = new String("Unknown device");
            }

            String descr = name + "\n" + device.getAddress() + "\nRssi: " + rssi + " dBm";
            ((TextView) vg.findViewById(R.id.descr)).setText(descr);

            vg.findViewById(R.id.btnConnect).setOnClickListener(view -> {
                if (((Button) view).getText() == "Disconnect") {
                    ((Button) view).setText("Connect");
                    vg.findViewById(R.id.btnData).setVisibility(View.GONE);
                    onConnect(device);
                } else {
                    ((Button) view).setText("Disconnect");
                    vg.findViewById(R.id.btnData).setVisibility(View.VISIBLE);
                    onConnect(device);
                }
            });

            vg.findViewById(R.id.btnData).setOnClickListener(view -> {
                startDeviceActivity(device);
            });

            return vg;
        }

        void onConnect(BluetoothDevice device) {
            if (mDevices.size() > 0) {
                int connState = mActivity.mBluetoothManager.getConnectionState(device,
                        BluetoothGatt.GATT);

                switch (connState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        mBluetoothLeService.disconnect(null);
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        boolean ok = mBluetoothLeService.connect(device.getAddress());
                        if (!ok) {
                            Toast.makeText(mContext, "Connect failed", Toast.LENGTH_LONG).show();
                        }
                        break;
                    default:
                        Toast.makeText(mContext, "Connecting", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }

    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Broadcasted actions from Bluetooth adapter and BluetoothLeService
    //
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth adapter state change
                switch (mActivity.mBtAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(context, "Exiting application.", Toast.LENGTH_LONG)
                                .show();
                        mActivity.finish();
                        break;
                    default:
                        // Log.w(TAG, "Action STATE CHANGED not processed ");
                        break;
                }
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                // GATT connect
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    CustomToast.middleBottom(mContext, "Connected. Status: " + status);
                } else
                    CustomToast.middleBottom(mContext, "Connection failed. Status: " + status);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // GATT disconnect
                int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
                        BluetoothGatt.GATT_FAILURE);
                stopDeviceActivity();
            } else {
                // Log.w(TAG,"Unknown action: " + action);
            }

        }
    };

    public void startDeviceActivity(BluetoothDevice mBluetoothDevice) {
        mDeviceIntent = new Intent(mContext, DeviceActivity.class);
        mDeviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE, mBluetoothDevice);
        startActivityForResult(mDeviceIntent, REQ_DEVICE_ACT);
    }

    private void stopDeviceActivity() {
        mActivity.finishActivity(REQ_DEVICE_ACT);
    }

}

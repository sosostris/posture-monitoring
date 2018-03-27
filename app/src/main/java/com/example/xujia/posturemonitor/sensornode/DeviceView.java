package com.example.xujia.posturemonitor.sensornode;

/**
 * Created by xujia on 2018-02-25.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;

// Fragment for Device View
public class DeviceView extends Fragment {

    public static DeviceView mInstance;

    // GUI
    private View view;
    public TextView mBaroData;
    public TextView mGyroData;
    public TextView mAccelData;
    public TextView mMagData;
    public TextView mHpaValue;
    public TextView mTextSensorType;
    public TextView mBatteryLevel;
    public boolean first = true;

    // House-keeping
    private DeviceActivity mActivity;

    public DeviceView() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInstance = this;
        mActivity = (DeviceActivity) getActivity();

        view = inflater.inflate(R.layout.fragment_device, container,false);
        mBaroData = (TextView) view.findViewById(R.id.baro_data);
        mGyroData = (TextView) view.findViewById(R.id.gyro_data);
        mAccelData = (TextView) view.findViewById(R.id.accel_data);
        mMagData = (TextView) view.findViewById(R.id.mag_data);
        mHpaValue = (TextView) view.findViewById(R.id.hPa_value);
        mTextSensorType = (TextView) view.findViewById(R.id.sensor_type);
        mBatteryLevel = (TextView) view.findViewById(R.id.battery_level);

        // Notify activity that UI has been inflated
        mActivity.onViewInflated(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}

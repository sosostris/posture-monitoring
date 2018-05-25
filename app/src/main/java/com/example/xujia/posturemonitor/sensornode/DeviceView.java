/**
 * Xujia Zhou. Copyright (c) 2018-02-25.
 */

package com.example.xujia.posturemonitor.sensornode;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;

/**
 * Fragment class for DeviceActivity.
 */
public class DeviceView extends Fragment {

    public static DeviceView mInstance;

    // GUI
    private View view;
    public TextView mBaroData;
    public TextView mGyroData;
    public TextView mAccelData;
    public TextView mMagData;
    public TextView mSensornodeId;
    public TextView mBatteryLevel;
    public RadioButton mAccelRadioBtn;
    public RadioButton mMagRadioBtn;
    public RadioButton mGyroRadioBtn;
    public RadioButton mBaroRadioBtn;
    public Button mToggleMATLABBtn;

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
        mSensornodeId = (TextView) view.findViewById(R.id.sensornode_id);
        mBatteryLevel = (TextView) view.findViewById(R.id.battery_level);

        // For MATLAB
        mAccelRadioBtn = (RadioButton) view.findViewById(R.id.accel_matlab);
        mMagRadioBtn = (RadioButton) view.findViewById(R.id.mag_matlab);
        mGyroRadioBtn = (RadioButton) view.findViewById(R.id.gyro_matlab);
        mBaroRadioBtn = (RadioButton) view.findViewById(R.id.baro_matlab);
        mToggleMATLABBtn = (Button) view.findViewById(R.id.toggle_matlab);

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

    /**
     * Update the text on the "Start/Stop streaming" button.
     */
    public void setMatlabButtonText(String text) {
        mToggleMATLABBtn.setText(text);
    }

}

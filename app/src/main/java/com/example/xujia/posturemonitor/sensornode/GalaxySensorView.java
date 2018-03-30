package com.example.xujia.posturemonitor.sensornode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xujia.posturemonitor.R;

import java.util.Arrays;

/**
 * Created by xujia on 2018-03-05.
 */
public class GalaxySensorView extends Fragment implements SensorEventListener {

    private static final String TAG = "GalaxySensorView";

    private MainActivity mActivity;
    private Context mContext;

    // Phone sensors
    private SensorManager mSensorManager;
    private Sensor mBarometer, mMagnetometer, mAccelerometer, mGyroscope;
    private int mBarometerAccuracy = 0;
    private int mMagnetometerAccuracy = 0;
    private int mAccelerometerAccuracy = 0;
    private int mGyroScopeAccuracy = 0;
    private float mPressure;    // // values[0]: Atmospheric pressure in hPa (millibar)
    private float mMField[] = new float[3];    // All values are in micro-Tesla (uT) and measure the ambient magnetic field in the X, Y and Z axis.
    private float mAcceleration[] = new float[3];    // All values are in SI units (m/s^2)
    private float mGyrorate[] = new float[3];    // All values are in radians/second and measure the rate of rotation around the device's local X, Y and Z axis.

    // UI
    private TextView mBarometerValue, mMagnetometerValue, mAccelValue, mGyroValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Log.i(TAG, "onCreateView");

        // The last two arguments ensure LayoutParams are inflated properly.
        View view = inflater.inflate(R.layout.fragment_galaxy, container, false);
        mBarometerValue = view.findViewById(R.id.barometer_value);
        mMagnetometerValue = view.findViewById(R.id.magnetometer_value);
        mAccelValue = view.findViewById(R.id.accel_value);
        mGyroValue = view.findViewById(R.id.gyro_value);

        mActivity = (MainActivity) getActivity();
        mContext = mActivity.getApplicationContext();

        // Existing sensors on the phone
        mSensorManager  = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        mBarometer = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mBarometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mBarometer) {
            mPressure = event.values[0];
            // if (mBarometerAccuracy == 1) {
                mBarometerValue.setText(Float.toString(mPressure));
            // }
        } else if (event.sensor == mMagnetometer) {
            mMField[0] = event.values[0];
            mMField[1] = event.values[1];
            mMField[2] = event.values[2];
            // if (mMagnetometerAccuracy == 1) {
                mMagnetometerValue.setText(Arrays.toString(mMField));;
             // }
        } else if (event.sensor == mAccelerometer) {
            mAcceleration[0] = event.values[0];
            mAcceleration[1] = event.values[1];
            mAcceleration[2] = event.values[2];
            // if (mAccelerometerAccuracy == 1) {
                mAccelValue.setText(Arrays.toString(mAcceleration));;
            // }
        } else if (event.sensor == mGyroscope) {
            mGyrorate[0] = event.values[0];
            mGyrorate[1] = event.values[1];
            mGyrorate[2] = event.values[2];
            // if (mGyroScopeAccuracy == 1) {
                mGyroValue.setText(Arrays.toString(mGyrorate));;
            // }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor == mBarometer) {
            switch (accuracy) {
                case 0:
                    Log.d(TAG, sensor.getName() + " unreliable");
                    mBarometerAccuracy = 0;
                    break;
                case 1:
                    Log.d(TAG, sensor.getName() + " low Accuracy");
                    mBarometerAccuracy = 0;
                    break;
                case 2:
                    mBarometerAccuracy = 1;
                    break;
                case 3:
                    mBarometerAccuracy = 1;
                    break;
            }
        } else if (sensor == mMagnetometer) {
            switch (accuracy) {
                case 0:
                    Log.d(TAG, sensor.getName() + " unreliable");
                    mMagnetometerAccuracy = 0;
                    break;
                case 1:
                    Log.d(TAG, sensor.getName() + " low Accuracy");
                    mMagnetometerAccuracy = 0;
                    break;
                case 2:
                    mMagnetometerAccuracy = 1;
                    break;
                case 3:
                    mMagnetometerAccuracy = 1;
                    break;
            }
        } else if (sensor == mAccelerometer) {
            switch (accuracy) {
                case 0:
                    Log.d(TAG, sensor.getName() + " unreliable");
                    mAccelerometerAccuracy = 0;
                    break;
                case 1:
                    Log.d(TAG, sensor.getName() + " low Accuracy");
                    mAccelerometerAccuracy = 0;
                    break;
                case 2:
                    mAccelerometerAccuracy = 1;
                    break;
                case 3:
                    mAccelerometerAccuracy = 1;
                    break;
            }
        } else if (sensor == mGyroscope) {
            switch (accuracy) {
                case 0:
                    Log.d(TAG, sensor.getName() + " unreliable");
                    mGyroScopeAccuracy = 0;
                    break;
                case 1:
                    Log.d(TAG, sensor.getName() + " low Accuracy");
                    mGyroScopeAccuracy = 0;
                    break;
                case 2:
                    mGyroScopeAccuracy = 1;
                    break;
                case 3:
                    mGyroScopeAccuracy = 1;
                    break;
            }
        }
    }

}

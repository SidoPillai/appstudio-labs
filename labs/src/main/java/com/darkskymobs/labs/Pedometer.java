/* Copyright 2021 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.darkskymobs.labs;

import android.app.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Pedometer implements SensorEventListener
{
    private static final String TAG = Pedometer.class.getSimpleName();
    private SensorManager mSensorManager;
    private Activity mActivity;
    private Context mContext;
    private Sensor stepDetector;
    private int stepCounterAndroidNative;

    Pedometer(Activity activity)
    {
        mActivity = activity;
    }

    public void init(Context context, long instance)
    {
        mContext = context;

        if (isSupported())
        {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            stepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            return;
        }
    }

    public void start()
    {
        if (!isSupported())
        {
            updateStateChanged(-1);
            return;
        }

        stepCounterAndroidNative = 0;
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_FASTEST);
        updateStateChanged(0);
    }

    public void stop()
    {
        if (!isSupported())
        {
            updateStateChanged(-1);
            return;
        }

        mSensorManager.unregisterListener(this);
        updateStateChanged(1);
    }

    public boolean isSupported()
    {
        PackageManager packageManager = mContext.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if (Sensor.TYPE_STEP_DETECTOR == sensorEvent.sensor.getType())
        {
            if (sensorEvent.values[0] == 1.0f)
            {
                stepCounterAndroidNative++;
                Log.d(TAG, "Received activity update" + stepCounterAndroidNative);
                updateStepsChanged(stepCounterAndroidNative);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private static native void updateStateChanged(int state);
    private static native void updateStepsChanged(int steps);
}

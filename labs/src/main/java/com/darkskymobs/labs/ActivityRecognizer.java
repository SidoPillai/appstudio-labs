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
import android.content.*;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.darkskymobs.labs.DetectedActivityIntentService;

public class ActivityRecognizer
{
    protected static final String TAG = ActivityRecognizer.class.getSimpleName();
    private Context mContext;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private static long g_Instance = 0;
    private Activity mActivity;
    private static final long DETECTION_INTERVAL_IN_MILLISECONDS = 30000;
    protected DetectedActivityReceiver mReceiver;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public interface ACTIVITY_TYPE
    {
        public static final int Unknown = 0;
        public static final int Running = 1;
        public static final int Walking = 2;
        public static final int Stationary = 3;
        public static final int Automotive = 4;
        public static final int Biking = 5;
        public static final int Tilting = 6;
    }

    //----------------------------------------------------------------------------------------------

    public interface CONFIDENCE
    {
        public static final int LOW = 0;
        public static final int MEDIUM = 1;
        public static final int HIGH = 2;
    }

    //----------------------------------------------------------------------------------------------

    ActivityRecognizer(Activity activity)
    {
        mActivity = activity;
        mReceiver = new DetectedActivityReceiver ();
    }

    //----------------------------------------------------------------------------------------------

    public void init(Context context, long instance)
    {
        mContext = context;
        g_Instance = instance;
        mActivityRecognitionClient = new ActivityRecognitionClient(mContext);
    }

    //----------------------------------------------------------------------------------------------

    public void start()
    {
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, new IntentFilter(DetectedActivityIntentService.BROADCAST_ACTION));

        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                DETECTION_INTERVAL_IN_MILLISECONDS,
                getDetectedActivityPendingIntent());

        task.addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void result)
            {
                Toast.makeText(mContext,
                        "Activity Updates Enabled",
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(0);
            }
        });

        task.addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(mContext,
                        "Activity Updates Not Enabled",
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(-1);
            }
        });
    }

    //----------------------------------------------------------------------------------------------

    public void stop()
    {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                getDetectedActivityPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>()
        {
            @Override
            public void onSuccess(Void result)
            {
                Toast.makeText(mContext,
                        "Activity Updates Removed",
                        Toast.LENGTH_SHORT)
                        .show();
                setUpdatesRequestedState(1);
            }
        });

        task.addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Log.w(TAG, "Failed to enable activity recognition.");
                Toast.makeText(mContext,
                        "Activity Updates Not Removed",
                        Toast.LENGTH_SHORT).show();
                setUpdatesRequestedState(-1);
            }
        });
    }

    //----------------------------------------------------------------------------------------------

    public boolean isSupported()
    {
        return isGooglePlayServicesEnabled();
    }

    public boolean isGooglePlayServicesEnabled()
    {
        if (isGooglePlayServicesInstalledAndEnabled(mContext))
        {
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            int result = googleAPI.isGooglePlayServicesAvailable(mContext);

            if (result == ConnectionResult.SUCCESS)
            {
                return true;
            }

            // display the error dialog for google play services
            if (googleAPI.isUserResolvableError(result))
            {
                googleAPI.getErrorDialog(mActivity, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        }

        return false;
    }

    static boolean isGooglePlayServicesInstalledAndEnabled(Context context)
    {
        return packageInstalledAndEnabled(context, GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE);
    }


    private static boolean packageInstalledAndEnabled(Context context, @NonNull String packageName)
    {
        try
        {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return info.applicationInfo.enabled;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    //----------------------------------------------------------------------------------------------

    private PendingIntent getDetectedActivityPendingIntent()
    {
        Intent intent = new Intent(mActivity, DetectedActivityIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //----------------------------------------------------------------------------------------------

    private void setUpdatesRequestedState(int state)
    {
        updateStateChanged(state);
    }

    //----------------------------------------------------------------------------------------------

    private class DetectedActivityReceiver extends BroadcastReceiver
    {
        String DetectedActivityReceiverTAG = "DetectedActivityReceiver";

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(DetectedActivityReceiverTAG, "Received activity update");
            ArrayList<DetectedActivity> updatedActivities = intent.getParcelableArrayListExtra(DetectedActivityIntentService.ACTIVITY_EXTRA);
            int resultsSize = updatedActivities.size();
            int[] activities = new int[resultsSize];
            int[] confidences = new int[resultsSize];
            for ( int i = 0; i < resultsSize; i++ )
            {
                DetectedActivity da = updatedActivities.get( i );
                activities[i] = getActivityType(da.getType());
                confidences[i] = getConfidenceLevel(da.getConfidence());
            }
            updateDetectedActivities(activities, confidences);
        }
    }

    //--------------------------------------------------------------------------

    private int getActivityType(int detectedActivityType)
    {
        switch ( detectedActivityType )
        {
            case DetectedActivity.IN_VEHICLE:
                return ACTIVITY_TYPE.Automotive;

            case DetectedActivity.ON_BICYCLE:
                return ACTIVITY_TYPE.Biking;

            case DetectedActivity.ON_FOOT:

            case DetectedActivity.WALKING:
                return ACTIVITY_TYPE.Walking;

            case DetectedActivity.RUNNING:
                return ACTIVITY_TYPE.Running;

            case DetectedActivity.STILL:
                return ACTIVITY_TYPE.Stationary;

            case DetectedActivity.TILTING:
                return ACTIVITY_TYPE.Tilting;

            default:
                return ACTIVITY_TYPE.Unknown;
        }
    }

    //--------------------------------------------------------------------------

    private int getConfidenceLevel(int confidence)
    {
        if (confidence >= 70)
        {
            return CONFIDENCE.HIGH;
        }
        else if (confidence > 40 && confidence < 70)
        {
            return CONFIDENCE.MEDIUM;
        }
        else
        {
            return CONFIDENCE.LOW;
        }
    }

    //--------------------------------------------------------------------------

    private static native void updateDetectedActivities(int[] activities, int[] confidence);
    private static native void updateStateChanged(int state);

    //--------------------------------------------------------------------------
}

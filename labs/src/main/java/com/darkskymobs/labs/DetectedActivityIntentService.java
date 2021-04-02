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

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class DetectedActivityIntentService extends IntentService
{
    protected static final String TAG = DetectedActivityIntentService.class.getSimpleName();
    protected static final String PACKAGE_NAME = DetectedActivityIntentService.class.getPackage().getName();
    protected static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";
    protected static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    //--------------------------------------------------------------------------

    public DetectedActivityIntentService()
    {
        super( TAG );
    }

    //--------------------------------------------------------------------------

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    //--------------------------------------------------------------------------

    @Override
    protected void onHandleIntent( Intent intent )
    {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult( intent );
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
        Intent localIntent = new Intent(BROADCAST_ACTION);
        localIntent.putExtra( ACTIVITY_EXTRA, detectedActivities );
        LocalBroadcastManager.getInstance(this).sendBroadcast( localIntent );
    }

    //--------------------------------------------------------------------------

}

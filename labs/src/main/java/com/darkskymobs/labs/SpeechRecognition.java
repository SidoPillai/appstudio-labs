/* Copyright 2020 Esri
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

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;

//--------------------------------------------------------------------------

public class SpeechRecognition
{
    private SpeechRecognitionListener listener;
    private static SpeechRecognizer speech = null;
    private String locale = null;
    private static long g_Instance = 0;
    public static Context appContext = null;

    private static final int SPEECH_STARTED = 0;
    private static final int SPEECH_READY = 1;
    private static final int BUFFER_RECEIVED = 2;
    private static final int SPEECH_END = 3;
    private static final int SPEECH_ERROR = 4;
    private static final int SPEECH_RESULTS = 5;
    private static final int SPEECH_RESULTS_DONE = 6;
    private static final int SPEECH_PARTIAL_RESULTS = 7;
    private static final int SPEECH_VOLUME_CHANGED = 8;
    private static ArrayList<String> supportedLanguages;
    public static final String TAG = "SpeechRecognition";

    //--------------------------------------------------------------------------

    public SpeechRecognition() {
        this.listener = null;
    }

    public void setSpeechRecognitionListener(SpeechRecognitionListener listener) {
        this.listener = listener;
    }

    public void initSpeechRecognition(Context context, long instance)
    {
        appContext = context;
        g_Instance = instance;
        Intent detailsIntent =  RecognizerIntent.getVoiceDetailsIntent(appContext);
        appContext.sendOrderedBroadcast(detailsIntent, null, new LanguageDetailsChecker(), null, Activity.RESULT_OK, null, null);
    }

    //--------------------------------------------------------------------------

    RecognitionListener recognitionListener = new RecognitionListener()
    {
        @Override
        public void onReadyForSpeech(Bundle params)
        {
            sendEvent(SPEECH_STARTED, "onReadyForSpeech", 0.0);
        }

        @Override
        public void onBeginningOfSpeech() { }

        @Override
        public void onBufferReceived(byte[] buffer)
        {
            sendEvent(BUFFER_RECEIVED, "onBufferReceived", 0.0);
        }

        @Override
        public void onEndOfSpeech()
        {
            sendEvent(SPEECH_END, "onSpeechEnd", 0.0);
        }

        @Override
        public void onError(int error)
        {
            sendEvent(SPEECH_ERROR, getErrorText(error), 0.0);
        }

        @Override
        public void onResults(Bundle results)
        {
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float [] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

            for ( int i = 0; i < data.size(); i++ )
            {
                sendEvent(SPEECH_RESULTS, data.get(i), confidence[i]);
                listener.speechDetected(data.get(i),confidence[i]);
            }

            sendEvent(SPEECH_RESULTS_DONE, "done", 0.0);
        }

        @Override
        public void onPartialResults(Bundle partialResults) { }

        @Override
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }

        @Override
        public void onRmsChanged(float rms)
        {
            Log.d(TAG, "onSpeechVolumeChanged " + rms);
        }

        public String getErrorText(int errorCode)
        {
            String message;

            switch (errorCode)
            {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;

            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;

            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;

            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;

            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;

            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;

            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;

            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;

            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;

            default:
                message = "Didn't understand, please try again.";
                break;
            }
            return message;
        }
    };

    //--------------------------------------------------------------------------

    public static native void resultChanged(int identifier, long instance, String message, double confidence);

    //--------------------------------------------------------------------------

    public static void sendEvent(int id, String message, double confidence)
    {
        resultChanged(id, g_Instance, message, confidence);
    }

    //--------------------------------------------------------------------------

    public void startSpeech(final String locale, final boolean onDeviceRecognition)
    {
        final Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);
        mainHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    startListening(locale, onDeviceRecognition);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    //--------------------------------------------------------------------------

    public void stopSpeech()
    {
        if (speech != null)
        {
            speech.destroy();
        }
    }

    //--------------------------------------------------------------------------

    public void cancelSpeech()
    {
        if (speech != null)
        {
            speech.cancel();
        }
    }

    //--------------------------------------------------------------------------

    public boolean isRecognitionAvailable()
    {
        return SpeechRecognizer.isRecognitionAvailable(appContext);
    }

    //--------------------------------------------------------------------------

    public String[] getAvailableLocales()
    {
        if (supportedLanguages != null)
        {
            ArrayList<String> supportedLangs = supportedLanguages;
            Object[] objArr = supportedLangs.toArray();
            String[] str = Arrays.copyOf(objArr, objArr.length, String[].class);
            return str;
         }

        return new String[] {};
    }

    //--------------------------------------------------------------------------

    public boolean isLanguageAvailable(String language)
    {
        if (language == null || language.equals(""))
            return false;

        String[] supportedLanguages = getAvailableLocales();

        for (int i = 0; i < supportedLanguages.length; i++)
        {
            if (supportedLanguages[i] == language)
                return true;
        }

        return false;
    }

    //--------------------------------------------------------------------------

    private String getLocale(String locale)
    {
        if (locale != null && !locale.equals(""))
        {
            return locale;
        }

        return Locale.getDefault().toString();
    }

    //--------------------------------------------------------------------------

    private void startListening(String locale, boolean onDeviceRecognition)
    {
        if (speech != null)
        {
            speech.destroy();
        }

        speech = SpeechRecognizer.createSpeechRecognizer(appContext);
        speech.setRecognitionListener(recognitionListener);

        final Intent mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, getLocale(locale));
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocale(locale));
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, onDeviceRecognition);
        speech.startListening(mSpeechRecognizerIntent);
    }

    //--------------------------------------------------------------------------

    public class LanguageDetailsChecker extends BroadcastReceiver
    {
        private String languagePreference;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle results = getResultExtras(true);
            if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))
            {
                languagePreference = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
            }
            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES))
            {
                supportedLanguages = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
            }
            sendEvent(SPEECH_READY, "onReadyForSpeech", 0.0);
        }
    }

    //--------------------------------------------------------------------------

    public interface SpeechRecognitionListener
    {
        void speechDetected(String transcription, double confidence);
    }

}
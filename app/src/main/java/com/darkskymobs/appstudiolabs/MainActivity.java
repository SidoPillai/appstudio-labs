package com.darkskymobs.appstudiolabs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.darkskymobs.labs.SpeechRecognition;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognition speechRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speechRecognition = new SpeechRecognition();
        speechRecognition.initSpeechRecognition(getApplicationContext(), 1234);
        speechRecognition.setSpeechRecognitionListener(new SpeechRecognition.SpeechRecognitionListener() {
            @Override
            public void speechDetected(String transcription, double confidence) {
                System.out.println("transcription" + transcription);
                System.out.println("confidence" + confidence);
            }
        });
    }
}
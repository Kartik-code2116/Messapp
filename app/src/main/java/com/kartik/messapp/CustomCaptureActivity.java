package com.kartik.messapp;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Custom capture activity to force portrait mode for the QR Scanner.
 * The default CaptureActivity from zxing-android-embedded is locked to landscape.
 */
public class CustomCaptureActivity extends CaptureActivity {
    // No code needed. We just need this class to reference in AndroidManifest.xml
}

/*
 *
 */
package com.indooratlas.android.sdk.examples.sharelocation;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;

import com.indooratlas.android.sdk.examples.R;

import java.util.Random;

/**
 *
 */
public class SharingUtils {

    public static final String TAG = "SharingExample";

    private static Random sRandom = new Random(SystemClock.elapsedRealtime());


    public static String defaultIdentity() {
        return Build.BRAND + " " + Build.DEVICE;
    }


    public static int randomColor(Context context) {
        String[] colors = context.getResources().getStringArray(R.array.dot_colors);
        return Color.parseColor(colors[sRandom.nextInt(colors.length)]);
    }

}

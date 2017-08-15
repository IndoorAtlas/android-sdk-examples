package com.indooratlas.android.sdk.examples.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.indooratlas.android.sdk.IALocationManager;

public class ExampleUtils {

    public static void shareTraceId(View view, final Context context,
                                    final IALocationManager manager) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                shareText(context, manager.getExtraInfo().traceId, "traceId");
                return true;
            }
        });
    }

    public static void shareText(Context context, String text, String title) {

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        context.startActivity(Intent.createChooser(sendIntent, title));
    }

}

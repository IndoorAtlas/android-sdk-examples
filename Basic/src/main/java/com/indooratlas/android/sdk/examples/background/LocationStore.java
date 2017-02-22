package com.indooratlas.android.sdk.examples.background;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IARegion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Abstraction for storing/sharing location events.
 */
public class LocationStore {

    private Context mContext;

    public static LocationStore obtain(Context context) {
        return new LocationStore(context);
    }

    private LocationStore(Context context) {
        mContext = context.getApplicationContext();
    }

    public synchronized void store(IALocation location) {
        try {
            File file = getStorageFile();
            FileWriter writer = new FileWriter(file, true); // true to append
            writer.write(locationToCsvLine(location));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void share() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, getStorageFileSharableUri());
        shareIntent.setType("text/plain");
        Intent chooserIntent = Intent.createChooser(shareIntent, null);
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(chooserIntent);
    }

    public synchronized void reset() {
        try {
            File file = getStorageFile();
            FileWriter writer = new FileWriter(file);
            writer.write("");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getStorageFile() throws IOException {
        File privateRootDir = mContext.getFilesDir();
        File sharedDir = new File(privateRootDir, "shared");
        if (!sharedDir.exists()) {
            sharedDir.mkdir();
        }
        File storeFile = new File(sharedDir, "events.csv");
        if (!storeFile.exists()) {
            storeFile.createNewFile();
        }
        return storeFile;
    }

    private Uri getStorageFileSharableUri() {
        try {
            File file = getStorageFile();
            return FileProvider.getUriForFile(mContext,
                    "com.indooratlas.android.sdk.examples.fileprovider", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String locationToCsvLine(IALocation location) {
        long ts = location.getTime();
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float accuracy = location.getAccuracy();
        int floor = location.getFloorLevel();
        float floorCertainty = location.getFloorCertainty();
        float bearing = location.getBearing();
        int regionType = 0;
        String regionId = null;
        IARegion region = location.getRegion();
        if (region != null) {
            regionType = region.getType();
            regionId = region.getId();
        }
        return String.format(Locale.US, "%d,%f,%f,%f,%d,%f,%f,%d,%s\n",
                ts, lat, lon, accuracy, floor, floorCertainty, bearing, regionType, regionId);
    }

}

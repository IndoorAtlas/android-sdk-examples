/*
 *
 */
package com.indooratlas.android.examples.sharelocation.sharing;

import com.indooratlas.android.sdk.IALocation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 */
public class ConversionUtils {

    private static final String KEY_SOURCE = "source";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";

    public static JSONObject toJSON(LocationChannelEvent event) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_SOURCE, event.source.getName());
        jsonObject.put(KEY_LOCATION, new JSONObject()
                .put(KEY_LAT, event.location.getLatitude())
                .put(KEY_LON, event.location.getLongitude()));
        return jsonObject;
    }


    public static LocationChannelEvent parseJSON(JSONObject jsonObject) throws JSONException {

        JSONObject location = jsonObject.getJSONObject(KEY_LOCATION);

        String name = jsonObject.getString(KEY_SOURCE);
        long latitude = location.getLong(KEY_LAT);
        long longitude = location.getLong(KEY_LON);

        return new LocationChannelEvent(new LocationSource(name), new IALocation.Builder()
                .withLatitude(latitude)
                .withLongitude(longitude)
                .build());

    }

}

/*
 *
 */
package com.indooratlas.android.sdk.examples.sharelocation.channel;

import com.indooratlas.android.sdk.IALocation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to convert objects to JSON and vice versa.
 */
public class ConversionUtils {

    private static final String KEY_SOURCE = "source";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_NAME = "name";
    private static final String KEY_COLOR = "color";
    private static final String KEY_ID = "id";

    public static JSONObject toJSON(LocationEvent event) throws JSONException {
        return toJSON(event.source, event.location);
    }

    public static JSONObject toJSON(LocationSource source, IALocation location)
            throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_SOURCE, new JSONObject()
                .put(KEY_ID, source.id)
                .put(KEY_NAME, source.name)
                .put(KEY_COLOR, source.color));
        jsonObject.put(KEY_LOCATION, new JSONObject()
                .put(KEY_LAT, location.getLatitude())
                .put(KEY_LON, location.getLongitude()));
        return jsonObject;
    }


    public static LocationEvent parseJSON(JSONObject jsonObject) throws JSONException {

        JSONObject location = jsonObject.getJSONObject(KEY_LOCATION);
        JSONObject source = jsonObject.getJSONObject(KEY_SOURCE);

        final double latitude = location.getDouble(KEY_LAT);
        final double longitude = location.getDouble(KEY_LON);

        return new LocationEvent(
                new LocationSource(source.getString(KEY_ID), source.getString(KEY_NAME),
                        source.getInt(KEY_COLOR)),
                new IALocation.Builder()
                        .withLatitude(latitude)
                        .withLongitude(longitude)
                        .build());

    }


    public static LocationEvent parseJSONOpt(JSONObject jsonObject) {
        try {
            return parseJSON(jsonObject);
        } catch (JSONException e) {
            return null;
        }
    }
}

package com.indooratlas.android.sdk.examples.ar;

import android.os.Bundle;

import com.indooratlas.android.sdk.IAARSession;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IAPOI;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.IAARObject;
import com.indooratlas.android.sdk.IAWayfindingListener;
import com.indooratlas.android.sdk.IAWayfindingRequest;
import com.indooratlas.android.sdk.examples.ar.wrapper.Api;
import com.indooratlas.android.sdk.resources.IAVenue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class WayfindingSession implements IALocationListener, IARegion.Listener {
    private final IALocationManager locationManager;
    private final IAARSession arSdk;
    private final Callbacks callbacks;

    interface Callbacks {
        void onArPoisUpdated(List<ArPOI> arPois);
    }

    public static class ArPOI {
        IAPOI iaPoi;
        double heading;
        double elevation;
        String textureName;
        float scale = 1f;
    }

    WayfindingSession(IALocationManager manager, Callbacks cbs) {
        this.callbacks = cbs;

        locationManager = manager;
        arSdk = locationManager.requestArUpdates();

        locationManager.requestLocationUpdates(IALocationRequest.create(), this);
        locationManager.registerRegionListener(this);
    }

    void destroy() {
        arSdk.destroy();
        locationManager.removeWayfindingUpdates();
        locationManager.unregisterRegionListener(this);
        locationManager.removeLocationUpdates(this);
    }

    boolean converged() {
        return arSdk.converged();
    }

    void onArFrame(float[] imuToWorld, float[] invViewMat, List<Api.HorizontalPlane> planes) {
        arSdk.setPoseMatrix(imuToWorld);
        arSdk.setCameraToWorldMatrix(invViewMat);
        for (Api.HorizontalPlane plane : planes) {
            arSdk.addArPlane(
                    plane.xyz,
                    plane.extentX,
                    plane.extentZ);
        }
    }

    IAARSession getArSdk() {
        return arSdk;
    }

    IALocationManager getLocationManager() { return locationManager; }

    List<IAARObject> getWaypoints() {
        return arSdk.getWayfindingTurnArrows();
    }

    public void setWayfindingTarget(IAWayfindingRequest wayfindingRequest) {
        if (wayfindingRequest != null) {
            Timber.i("Setting AR wayfinding target to %f,%f,%d",
                    wayfindingRequest.getLatitude(),
                    wayfindingRequest.getLongitude(),
                    wayfindingRequest.getFloor());
            arSdk.startWayfinding(wayfindingRequest);
        }
    }

    @Override
    public void onEnterRegion(IARegion region) {
        Timber.d("ENTER region %s", region.getName());
        IAVenue venue = region.getVenue();
        if (venue != null) {
            Timber.i("ENTER venue %s", venue.getName());
            // update POIs
            List<ArPOI> newArPois = new ArrayList<>();
            int i = 0;
            for (IAPOI poi : venue.getPOIs()) {
                ArPOI arPoi = new ArPOI();
                double elevation = 0.5;
                // use one FP by random to simplify -- usually they are the same for other
                // could perhaps match a bit more accurately with POI.floornumber --> fp.bearing
                double heading = venue.getFloorPlans().get(0).getBearing();
                Timber.d("POI " + poi.getId() + ", heading: "+heading);
                elevation = 1;
                // use a default image for all POIS in Camera view -- useful for quick demos
                arPoi.textureName = "IA_AR_ad_framed.png";
                arPoi.scale = 1;

                if (poi.getPayload() != null) {
                    try {
                        Timber.d(poi.getPayload().toString());
                        JSONObject arPayload = poi.getPayload().getJSONObject("");
                        if (arPayload == null) {
                            Timber.d("POI " + poi.getId() + " does not have AR payload, " +
                                    "using default IA image");
                        } else {
                            elevation = arPayload.getDouble("elevation");
                            heading = arPayload.getDouble("heading");
                            // NOTE: a potential security issue here. Should validate that the asset
                            // exists and is not pointing to an illegal path here
                            arPoi.textureName = arPayload.getString("assetName");
                            arPoi.scale = (float) arPayload.getDouble("scale");
                        }
                    } catch (JSONException ex) {
                        Timber.e("failed to parse AR payload, use default IA image");
                    }
                }

                arPoi.iaPoi = poi;
                arPoi.heading = heading;
                arPoi.elevation = elevation;

                newArPois.add(arPoi);
            }

            Timber.d("%s AR POI(s)", newArPois.size());
            callbacks.onArPoisUpdated(newArPois);
        }
    }

    @Override
    public void onExitRegion(IARegion region) {
    }

    // TODO: should not be needed
    @Override
    public void onLocationChanged(IALocation location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}

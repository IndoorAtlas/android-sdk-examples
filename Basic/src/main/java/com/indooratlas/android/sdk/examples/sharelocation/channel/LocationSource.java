/*
 *
 */
package com.indooratlas.android.sdk.examples.sharelocation.channel;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.UUID;

/**
 * Data object that holds source information used with in this example.
 */
public class LocationSource implements Parcelable {

    /** Display name of the source. */
    public final String name;

    /** Color used but the source. */
    public int color;

    /** Identifier of the source. */
    public String id;


    public LocationSource(String name, int color) {
        this(UUID.randomUUID().toString(), name, color);
    }

    public LocationSource(String id, String name, int color) {
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("both id and name must be non empty");
        }
        this.id = id;
        this.name = name;
        this.color = color;
    }

    protected LocationSource(Parcel in) {
        name = in.readString();
        color = in.readInt();
        id = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(color);
        dest.writeString(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationSource> CREATOR = new Creator<LocationSource>() {
        @Override
        public LocationSource createFromParcel(Parcel in) {
            return new LocationSource(in);
        }

        @Override
        public LocationSource[] newArray(int size) {
            return new LocationSource[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof LocationSource)) {
            return false;
        }
        return ((LocationSource) o).id.equals(this.id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LocationSource{");
        sb.append("name='").append(name).append('\'');
        sb.append(", color=").append(color);
        sb.append(", id='").append(id).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

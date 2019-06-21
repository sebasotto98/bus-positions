package ds.android.ds_project.Classes;

import java.io.Serializable;

/**
 * The Value class is used to store the bus which is of type Bus, the latitude,
 * the longitude and the timestamp.
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */

public class Value implements Serializable {

    public static final long serialVersionUID= 6621410190597288110L;
    private Bus bus;
    private double latitude;
    private double longitude;
    private String timestamp;

    public Value(Bus bus, double latitude, double longitude, String timestamp) {
        this.bus = bus;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public Bus getBus() {
        return bus;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() { return timestamp; }

}

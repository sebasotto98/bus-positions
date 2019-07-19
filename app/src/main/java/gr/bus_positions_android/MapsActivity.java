package gr.bus_positions_android;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import gr.bus_positions.Value;
import gr.bus_positions.Interfaces.Subscriber;
import gr.DS_Android.R;
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private String request;
    private Subscriber subscriber;
    private ReadValues readValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        request = Objects.requireNonNull(getIntent().getExtras()).getString("topic");
        readValues = new ReadValues();
        subscriber = new SubscriberImpl(readValues);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        readValues.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class ReadValues extends AsyncTask {

        private LatLng busPosition;
        private MarkerOptions markerOptions;
        private String vehicleID;
        private boolean flag1;
        private boolean flag2;
        private boolean flag3;
        private int lastDay;

        private List<String> vehicleIDs;
        private Hashtable<String, Marker> markers;

        public ReadValues() {
            markerOptions = new MarkerOptions();
            flag1 = true;
            flag2 = false;
            flag3 = true;
            vehicleIDs = new ArrayList<>();
            markers = new Hashtable<>();
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.bus);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            subscriber.getValue(request);
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            if (flag2) {
                List<String> vehicles = new ArrayList<>(markers.keySet());
                for (String vehicle : vehicles) {
                    markers.get(vehicle).remove();
                }
                markers = new Hashtable<>();
                flag2 = false;
            }
            if (markers.containsKey(vehicleID)) {
                markers.get(vehicleID).remove();
            }
            markerOptions.position(busPosition);
            markerOptions.title(vehicleID);
            markers.put(vehicleID, mMap.addMarker(markerOptions));
            if (flag3) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(busPosition));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);
                flag3 = false;
            }
        }

        public void visualizeData(Value value) {
            vehicleID = value.getBus().getVehicleID();
            int day = Integer.valueOf(value.getTimestamp().substring(5, 6));
            if (flag1) {
                lastDay = day;
                flag1 = false;
            }
            if (lastDay != day) {
                vehicleIDs = new ArrayList<>();
                flag2 = true;
            }
            if (!vehicleIDs.contains(vehicleID)) {
                vehicleIDs.add(vehicleID);
            }
            busPosition = new LatLng(value.getLatitude(), value.getLongitude());
            publishProgress(null);
        }
    }
}
package com.example.a1.emergencyapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Constants {

    public static final int ZOOM = 15;

    private GoogleMap mMap;
    private GoogleApiClient client;
    /**
     * previous user location
     */
    private LatLng oldPoint;
    private Marker currentMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void mapLocation() {
        Log.e("CONNECT", "mapLocation");
        client = new GoogleApiClient.Builder(MapsActivity.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.e("CONNECT", "onConnected");
                        LocationRequest request = new LocationRequest();
                        request.setInterval(MIN_GPS_REQUEST_INTERVAL);

                        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        request.setFastestInterval(MIN_GPS_REQUEST_INTERVAL);

                        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            showPermissionErrorDialog();
                            return;
                        }

                        com.google.android.gms.location.LocationListener listener = new com.google.android.gms.location.LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                Log.e("CONNECT", "onLocationChanged " + location.getLongitude() + " lat " + location.getLatitude());
                                double distanceLastGeoLocation = 0;
                                LatLng newPoint = new LatLng(location.getLatitude(),
                                        location.getLongitude());

                                if(oldPoint!=null){
                                    distanceLastGeoLocation = SphericalUtil.computeDistanceBetween(newPoint, oldPoint);
                                }

                                if(oldPoint==null){
                                    MarkerOptions marker = new MarkerOptions()
                                            .position(newPoint);
                                    currentMarker = mMap.addMarker(marker);
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPoint, ZOOM));

                                }
                                if(distanceLastGeoLocation>MIN_USER_PROXIMITY || oldPoint==null) {
                                    Log.e("CONNECT", "Change marker " + newPoint.toString());
                                    currentMarker.setPosition(newPoint);
                                    currentMarker.setTitle("lat " + newPoint.latitude
                                                                        + " lng " + newPoint.longitude);
                                    mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(newPoint, ZOOM));
                                }

                                oldPoint = newPoint;
                            }
                        };

                        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, listener);

                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        showConnectionSuspendDialog();
                    }
                }).build();

        client.connect();

    }

    /**
     * Dialog for warn connetion suspend
     */
    private void showConnectionSuspendDialog() {
        AlertDialog alert = new AlertDialog.Builder(MapsActivity.this)
                .create();
        alert.setTitle("Error");
        alert.setMessage("GPS connection is suspended!");
        alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // closing the application
                finish();
            }
        });
        alert.show();
    }

    /**
     * Dialog for warn application's lack of location access permission
     */
    private void showPermissionErrorDialog() {
        AlertDialog alert = new AlertDialog.Builder(MapsActivity.this)
                .create();
        alert.setTitle("Error");
        alert.setMessage("Sorry, application has not GPS access permissions!");
        alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // closing the application
                finish();
            }
        });
        alert.show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapLocation();
    }

}

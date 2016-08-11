package com.example.a1.emergencyapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import permission.auron.com.marshmallowpermissionhelper.ActivityManagePermission;
import permission.auron.com.marshmallowpermissionhelper.PermissionResult;
import permission.auron.com.marshmallowpermissionhelper.PermissionUtils;

public class MainActivity extends ActivityManagePermission implements Constants{

    /**
     * width of bitmap with qr code
     */
    public final static int WIDTH=200;
    public static final String QR_MESSAGE_TEMPLATE = "http://www.openstreetmap.org/?";


    ImageView qrCodeImageView;
    StringBuilder qrCodeString;
    CustomButton markButton;
    CustomButton sosButton;
    CustomButton shareButton;
    TextView latValueTV;
    TextView lngValueTV;
    RelativeLayout qrLayout;
    TextView dateTv;

    private Camera camera;
    private double lngValue;
    private double latValue;
    private boolean hasFlashlight;
    Camera.Parameters params;
    private boolean isFlashOn;
    private MorseSOSTask morseSOSTask;
    private QRLoadTask locationLoadTask;
    private Bitmap qrBitmap;
    private ProgressBar qrLoadProgress;
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Log.e("SOS", "onCreate start");
        setContentView(R.layout.activity_sos);

        askCompactPermissions(new String[]{PermissionUtils.Manifest_CAMERA}, new PermissionResult() {
            @Override
            public void permissionGranted() {
                getCamera();
                checkFlashlight();
            }

            @Override
            public void permissionDenied() {
            }
        });

        initialize();
        setValues();
        setListeners();

        Log.e("SOS", "onCreate end");

    }

    /**
     * if activity is not active - stop the light
     */
    @Override
    protected void onPause() {
        super.onPause();
        turnOffFlash();
    }

    /**
     * while application is stopping - stop all async tasks
     * and close camera connection
     */
    @Override
    protected void onStop() {
        super.onStop();

        if (camera != null) {
            camera.release();
            camera = null;
        }

        if(locationLoadTask!=null){
            locationLoadTask.cancel(true);
        }

        if(morseSOSTask!=null){
            morseSOSTask.cancel(true);
        }


    }

    /**
     * extract UI elements from xml file and settings them to appropriate java objects
     */
    public void initialize(){
        markButton = (CustomButton) findViewById(R.id.mark_button);
        shareButton = (CustomButton) findViewById(R.id.share_button);
        sosButton = (CustomButton) findViewById(R.id.sos_button);
        latValueTV = (TextView) findViewById(R.id.lat_value_tv);
        lngValueTV = (TextView) findViewById(R.id.lng_value_tv);
        qrLayout = (RelativeLayout) findViewById(R.id.qr_code_panel);
        dateTv = (TextView) findViewById(R.id.date_tv);
        qrCodeImageView = (ImageView) findViewById(R.id.img_qr_code);
        qrLoadProgress = (ProgressBar) findViewById(R.id.qr_load_progress);
    }

    /**
     * set values for UI objects if necessary
     */
    public void setValues(){
        markButton.setValues(R.string.mark, R.drawable.selector_mark);
        shareButton.setValues(R.string.share, R.drawable.selector_share);
        sosButton.setValues(R.string.help, R.drawable.sos_white);

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.US);
        dateTv.setText(dateFormat.format(new Date()));

        mapLocation();
    }

    /**
     * update all UI elements' data, which depend on user location
     * @param newLocation  - last known user location
     */
    public void updateGeoData(LatLng newLocation){
        lngValue = newLocation.longitude;
        latValue = newLocation.latitude;

        qrCodeString = new StringBuilder().append(QR_MESSAGE_TEMPLATE)
                .append("mlat=")
                .append(latValue)
                .append("&mlon=").append(lngValue)
                .append("&zoom=16#map=16/")
                .append(latValue).append("/").append(lngValue);

        latValueTV.setText(String.valueOf(latValue));
        lngValueTV.setText(String.valueOf(lngValue));

        qrLoadProgress.setVisibility(View.VISIBLE);
        loadQRBitmap();
    }

    /**
     * set listeners for UI object if necessary
     */
    public void setListeners(){
        markButton.setClick(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
        shareButton.setClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, qrCodeString.toString());
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.qr_share)));
            }
        });
        sosButton.setClick(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "SOS", Toast.LENGTH_SHORT).show();
                makeSOSSignal();
            }
        });
    }

    /**
     * executing QRLoadTask object for getting last known user's location
     */
    public void loadQRBitmap(){
        locationLoadTask = new QRLoadTask();
        locationLoadTask.execute();
    }

    /**
     * generate qr code and convert it to bitmap object with appropriate WIDTH
     * from string str, which is sent as parameter
     * @param str
     * @return
     * @throws WriterException
     */
    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
        return bitmap;
    }

    /**
     * check whether flashlight exists, set variable hasFlashlight = true if supports flashlight,
     * false - otherwise
     */
    public void checkFlashlight(){
        hasFlashlight = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * get system camera instance
     */
    private void getCamera(){
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * check if camera and params available and
     * turn on flashlight
     */
    private void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();

            isFlashOn = true;
        }
    }

    /**
     * check if camera and params available and
     * turn off flashlight
     */
    private void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;
        }
    }

    /**
     * display morse flashlight signal
     * check if flashlight available, if yes - calling async task MorseSOSTask
     * for activating flashlight morse signal, otherwise display dialog with message,
     * that flashlight isn't supporting by device
     */
    private void makeSOSSignal(){

        if(!hasFlashlight){
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .create();
            alert.setTitle("Error");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }else{
            morseSOSTask = new MorseSOSTask();
            morseSOSTask.execute();
        }
    }

    /**
     * Async task for dislplaying morse flashlight signal
     */
    class MorseSOSTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.e("MORSE", "START");
            for(int i = 0;i<3;i++){
                makeSignals(SHORT_MORSE_DURATION);
                makeSignals(LONG_MORSE_DURATION);
                makeSignals(SHORT_MORSE_DURATION);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.e("MORSE", "END");
            return null;
        }

        /**
         * Make series of 3 signals with duration = @param(duration)
         * @param duration
         */
        void makeSignals(int duration){
            for(int j = 0; j < 3; j++){
                turnOnFlash();
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                turnOffFlash();
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Async task for loading last location object and setting values for lan, lng variables
     */
    class QRLoadTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.e("QR LOAD", "START");
            try {
                Log.e("QR MESSAGE", qrCodeString.toString());
                try {
                    qrBitmap = encodeAsBitmap(qrCodeString.toString());
                } catch (WriterException e) {
                    e.printStackTrace();
                }

                Thread.sleep(1);

            }catch (InterruptedException ex){
                Log.e("QR LOAD", "INTERRUPTED");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            qrCodeImageView.setImageBitmap(qrBitmap);
            qrLoadProgress.setVisibility(View.GONE);
            Log.e("QR LOAD", "END");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.e("QR LOAD", "ON CANCELLED");
        }
    }

    /**
     * method for getting current user's location and
     * track for it changes every 25 meters, call method for data update
     */
    public void mapLocation() {


        Log.e("CONNECT", "mapLocation");
        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

                    LatLng oldPoint;
                    double distanceLastGeoLocation;

                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.e("CONNECT", "onConnected");
                        LocationRequest request = new LocationRequest();
                        request.setInterval(MIN_GPS_REQUEST_INTERVAL);

                        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        request.setFastestInterval(MIN_GPS_REQUEST_INTERVAL);

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        com.google.android.gms.location.LocationListener listener = new com.google.android.gms.location.LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                Log.e("CONNECT", "onLocationChanged " + location.getLongitude() + " lat " + location.getLatitude());
                                LatLng newPoint = new LatLng(location.getLatitude(),
                                        location.getLongitude());

                                if(oldPoint!=null){
                                    distanceLastGeoLocation = SphericalUtil.computeDistanceBetween(newPoint, oldPoint);
                                }

                                if(distanceLastGeoLocation>MIN_USER_PROXIMITY || oldPoint==null) {
                                    Log.e("CONNECT", "Change marker " + newPoint.toString());
                                    updateGeoData(newPoint);
                                    oldPoint = newPoint;
                                }


                            }
                        };

                        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, listener);

                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.e("CONNECT", "onConnectionSuspended");
                    }
                }).build();

        client.connect();

    }
}

package parkassist.cloud.com.parkassist;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.UrlQuerySanitizer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.api.CloudLogicAPI;
import com.amazonaws.mobile.api.CloudLogicAPIConfiguration;
import com.amazonaws.mobile.push.PushManager;
import com.amazonaws.mobile.util.ThreadUtils;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.util.IOUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.security.AccessController.getContext;

public class ParkSearch extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, OnMapLoadedCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private static final String TAG = "MainActivity";
    public static ParkSearch p;
    private IdentityManager identityManager;

    static boolean FINE_LOCATION_PERMISSION;
    static boolean COARSE_LOCATION_PERMISSION;


    private static GoogleMap gMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation,apiRequestLocation;

    private LocationRequest mLocationRequest;

    private final LatLngBounds NEW_YORK_CITY = new LatLngBounds(
            new LatLng(40.496151, -74.255555), new LatLng(40.914401, -73.701626));

    private final LatLng NEW_YORK_CITY_CENTRE = new LatLng(40.7745457, -73.9718052);
    private static CameraUpdateFactory cameraUpdateFactory;

    double Latitude, Longitude;

    MarkerOptions markerOptions;
    Marker currLocationMarker;

    static CircleOptions circleOptions = new CircleOptions();
    static Circle circle;

    private final double QUARTER_MILE_IN_METERS = 402.336;
    private final double API_REQUEST_MIN_DIST = 1000;

    Bitmap bitmap;

    SharedPreferences sharedPrefs ;
    SharedPreferences.Editor ed ;

    FloatingActionButton fab_carPark;
    ArrayList<Polyline> polyLines = new ArrayList<Polyline>();

    public String endpoint;

    private static Context context;

    Marker carMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p = this;
        AWSMobileClient.initializeMobileClientIfNecessary(this);

        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        identityManager = awsMobileClient.getIdentityManager();

        setupSNS();

        setContentView(R.layout.activity_park_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        sharedPrefs = getSharedPreferences("ParkAssist", MODE_PRIVATE);
        context = this;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Setting Your Parking Spot", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                FragmentManager fm = getFragmentManager();
                TimerDialogFragment dialogFragment = new TimerDialogFragment ();
                dialogFragment.show(fm, "Sample Fragment");
            }
        });

        fab_carPark = (FloatingActionButton) findViewById(R.id.fab_carPark);
        fab_carPark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ed = sharedPrefs.edit();
                if (sharedPrefs.contains("CarLatitude") || sharedPrefs.contains("CarLongitude")) {

                    try{
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                        alertDialogBuilder.setTitle("Leave Parking Spot ?");
                        alertDialogBuilder.setIcon(R.drawable.app_logo);
                        alertDialogBuilder.setCancelable(true);
                        alertDialogBuilder
                                .setMessage("Your Car is Parked, Would you like to leave the spot ?")
                                .setCancelable(true)
                                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // if this button is clicked, close
                                        // current activity
                                        carMarker.remove();
                                        Toast.makeText(getApplicationContext(),"Parking Spot Left",Toast.LENGTH_SHORT).show();
                                        final Map<String, String> parameters = new HashMap<>();

                                        parameters.put("lat", sharedPrefs.getString("CarLatitude",""));
                                        parameters.put("lon", sharedPrefs.getString("CarLongitude",""));

                                        sharedPrefs.edit().remove("CarLatitude").commit();
                                        sharedPrefs.edit().remove("CarLongitude").commit();
                                        fab_carPark.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCarFind)));

                                        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
                                        sanitizer.setAllowUnregisteredParamaters(true);
                                        sanitizer.parseQuery(endpoint);

                                        parameters.put("endpoint", endpoint);

                                        final CloudLogicAPIConfiguration apiConfiguration = new CloudLogicAPIConfiguration("parking",
                                                "",
                                                "https://4grcls3sp6.execute-api.us-east-1.amazonaws.com/Development",
                                                new String[] { "/leave"
                                                },
                                                com.amazonaws.mobile.api.id4grcls3sp6.ParkingMobileHubClient.class);
                                        final CloudLogicAPI client =
                                                AWSMobileClient.defaultMobileClient().createAPIClient(apiConfiguration.getClientClass());
                                        ApiRequest tmpRequest = new ApiRequest(client.getClass().getSimpleName())
                                                .withPath("/leave")
                                                .withHttpMethod(HttpMethodName.GET)
                                                .withParameters(parameters);

                                        final ApiRequest request;
                                        request = tmpRequest;

                                        // Make network call on background thread
                                        new Thread(new Runnable() {
                                            Exception exception = null;

                                            @Override
                                            public void run() {
                                                try {
                                                    final ApiResponse response = client.execute(request);
                                                    final String responseData = IOUtils.toString(response.getContent());
                                                    Log.d(TAG,"Response: " + responseData);

                                                } catch (final Exception exception) {
                                                    exception.printStackTrace();
                                                }
                                            }
                                        }).start();
                                    }
                                });


                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show it
                        alertDialog.show();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else{
                    LatLng carParkLatLng = markerOptions.getPosition();
                    ed.putString("CarLatitude", Double.toString(carParkLatLng.latitude));
                    ed.putString("CarLongitude", Double.toString(carParkLatLng.longitude));
                    ed.commit();

                    MarkerOptions carMarkerOptions = new MarkerOptions()
                            .position(carParkLatLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(getApplicationContext(), R.drawable.ic_parksign)))
                            .title("Your Car's Parking Spot");

                    carMarker = gMap.addMarker(carMarkerOptions);

                    view.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCarParked)));
                    Snackbar.make(view, "Parking Spot Saved", Snackbar.LENGTH_LONG)
                            .setAction("Dismiss", null).show();
                }



            }
        });

        final AppCompatActivity activity = this;
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            @Override
            public void syncState() {
                super.syncState();
                //updateUserName(activity);
                //updateUserImage(activity);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateUserName(activity);
                updateUserImage(activity);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        toggle.setHomeAsUpIndicator(R.drawable.ic_audiotrack);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            if (!FINE_LOCATION_PERMISSION && !COARSE_LOCATION_PERMISSION)
                return;
        } else {
            setUpAPIClient();
        }

        bitmap = getBitmapFromVectorDrawable(this,R.drawable.ic_navigation);

        /*
        String token = FirebaseInstanceId.getInstance().getToken();

        // Log and toast
        String msg = getString(R.string.msg_token_fmt, token);
        Log.d(TAG, msg);
        Toast.makeText(ParkSearch.this, msg, Toast.LENGTH_SHORT).show();
        */

    }

    private void updateUserName(final AppCompatActivity activity)
    {
        final IdentityProvider identityProvider =
                identityManager.getCurrentIdentityProvider();

        final TextView userNameView = (TextView) activity.findViewById(R.id.CurrentUserName);

        if (identityProvider == null) {
            // Not signed in
            userNameView.setText(activity.getString(R.string.main_nav_menu_default_user_text));
            //userNameView.setBackgroundColor(activity.getResources().getColor(R.color.nav_drawer_no_user_background));
            return;
        }

        final String userName =
                identityProvider.getUserName();

        if (userName != null) {
            userNameView.setText(userName);
            userNameView.setBackgroundColor(
                    activity.getResources().getColor(R.color.nav_drawer_top_background));
        }
    }

    private void updateUserImage(final AppCompatActivity activity)
    {
        final IdentityProvider identityProvider =
                identityManager.getCurrentIdentityProvider();

        final ImageView userImageView = (ImageView) activity.findViewById(R.id.imageView);

        if (identityProvider == null) {
            // Not signed in
            //userNameView.setText(activity.getString(R.string.main_nav_menu_default_user_text));
            //userNameView.setBackgroundColor(activity.getResources().getColor(R.color.nav_drawer_no_user_background));
            if(Build.VERSION.SDK_INT < 22)
                userImageView.setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.user));
            else
                userImageView.setImageDrawable(activity.getDrawable(R.mipmap.user));
            return;
        }

        final Bitmap userImage =
                identityManager.getUserImage();

        if (userImage != null) {
            userImageView.setImageBitmap(userImage);
    //            userNameView.setBackgroundColor(
    //            activity.getResources().getColor(R.color.nav_drawer_top_background));
        }
    }

    public void setUpAPIClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        Toast.makeText(this, "Google API Client Setup", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.moveCamera(CameraUpdateFactory.newLatLng(NEW_YORK_CITY_CENTRE));
        gMap.setMinZoomPreference(13);
        gMap.setMaxZoomPreference(18);
        gMap.setOnMapLoadedCallback(this);
        isCarParked();

        circleOptions.radius(QUARTER_MILE_IN_METERS).fillColor(0x55000000).strokeColor(Color.BLACK).strokeWidth(2);

    }

    public void UpdateMap(double lat, double lon)
    {
        if(endpoint == null){
        return;
        }
        //Toast.makeText(this,"Requested Map Update",Toast.LENGTH_LONG).show();

        final Map<String, String> parameters = new HashMap<>();

        parameters.put("lat", Double.toString(lat));
        parameters.put("lon", Double.toString(lon));

        final UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
        sanitizer.setAllowUnregisteredParamaters(true);
        sanitizer.parseQuery(endpoint);

        parameters.put("endpoint", endpoint);

        final CloudLogicAPIConfiguration apiConfiguration = new CloudLogicAPIConfiguration("parking",
                "",
                "https://4grcls3sp6.execute-api.us-east-1.amazonaws.com/Development",
                new String[] { "/items"
                },
                com.amazonaws.mobile.api.id4grcls3sp6.ParkingMobileHubClient.class);
        final CloudLogicAPI client =
                AWSMobileClient.defaultMobileClient().createAPIClient(apiConfiguration.getClientClass());
        ApiRequest tmpRequest = new ApiRequest(client.getClass().getSimpleName())
                .withPath("/items")
                .withHttpMethod(HttpMethodName.GET)
                .withParameters(parameters);

        final ApiRequest request;
        request = tmpRequest;

        // Make network call on background thread
        new Thread(new Runnable() {
            Exception exception = null;

            @Override
            public void run() {
                try {
                    final ApiResponse response = client.execute(request);
                    final String responseData = IOUtils.toString(response.getContent());
                    Log.d(TAG,"Response: " + responseData);
                    plotStreetLines(responseData);
                } catch (final Exception exception) {
                    exception.printStackTrace();
                }
            }
        }).start();
    }

    public void isCarParked(){
        if (sharedPrefs.contains("CarLatitude") || sharedPrefs.contains("CarLongitude")){
            fab_carPark.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCarParked)));
            String lat = sharedPrefs.getString("CarLatitude","");
            String lng = sharedPrefs.getString("CarLongitude","");
            LatLng carParkLatLng = new LatLng(Double.parseDouble(lat),Double.parseDouble(lng));

            MarkerOptions carMarkerOptions = new MarkerOptions()
                    .position(carParkLatLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(this,R.drawable.ic_parksign)))
                    .title("Your Car's Parking Spot");

            carMarker = gMap.addMarker(carMarkerOptions);

        }else{
            fab_carPark.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCarFind)));
        }
    }

    public void setMapLocation(double lat, double lng) {

        LatLng newLoc = new LatLng(lat, lng);

        Location newLocation = new Location("");
        newLocation.setLatitude(lat);
        newLocation.setLongitude(lng);

        float distanceBetween = mLastLocation.distanceTo(newLocation);

        if(distanceBetween >= API_REQUEST_MIN_DIST ){
            for(Polyline line : polyLines)
            {
                line.remove();
            }

            polyLines.clear();

            UpdateMap(lat,lng);
        }

        if (currLocationMarker == null){
            markerOptions = new MarkerOptions()
                    .position(newLoc)
                    .title("Current Location")
                    .flat(TRUE)
                    .anchor(0.5f,0.5f)
                    .flat(TRUE)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap));
            currLocationMarker = gMap.addMarker(markerOptions);

        }else {
            //currLocationMarker.setPosition(newLoc);

        }
        animateMarker(newLoc,FALSE);
        currLocationMarker.setRotation(mLastLocation.bearingTo(newLocation));
        mLastLocation = newLocation;

        circleOptions.center(newLoc);
        if (circle == null)
            circle = gMap.addCircle(circleOptions);
        else
            circle.setCenter(newLoc);


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(newLoc)
                .bearing(mLastLocation.bearingTo(newLocation))
                .tilt(30)
                .zoom(gMap.getCameraPosition().zoom)
                .build();
        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),2000,null);
        // gMap.animateCamera(cameraUpdateFactory.newLatLng(newLoc));

    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public void animateMarker(final LatLng toPosition,final boolean hideMarke) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = gMap.getProjection();
        Point startPoint = proj.toScreenLocation(currLocationMarker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 2500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                currLocationMarker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarke) {
                        currLocationMarker.setVisible(false);
                    } else {
                        currLocationMarker.setVisible(true);
                    }
                }
            }
        });
    }

    @Override
    public void onMapLoaded() {
        gMap.setLatLngBoundsForCameraTarget(NEW_YORK_CITY);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.park_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    FINE_LOCATION_PERMISSION = TRUE;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(ParkSearch.this, "Permission denied to access your Fine Location", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case 11: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    COARSE_LOCATION_PERMISSION = TRUE;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(ParkSearch.this, "Permission denied to access your Coarse Location", Toast.LENGTH_SHORT).show();
                }
                break;

            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
        if (FINE_LOCATION_PERMISSION && COARSE_LOCATION_PERMISSION)
            setUpAPIClient();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();

        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Latitude = mLastLocation.getLatitude();
            Longitude = mLastLocation.getLongitude();

            apiRequestLocation = mLastLocation;
            Toast.makeText(this, Latitude + " : " + Longitude, Toast.LENGTH_LONG).show();
            UpdateMap(Latitude,Longitude);

            setMapLocation(Latitude, Longitude);

            startLocationUpdates();
        }


    }


    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }


    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);
                //.setSmallestDisplacement(3);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    @Override
    public void onLocationChanged(Location location) {
        Latitude = location.getLatitude();
        Longitude = location.getLongitude();
        Toast.makeText(this,"Location Changed",Toast.LENGTH_SHORT).show();
        setMapLocation(Latitude,Longitude);
    }


    public void requestPermissions(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                10);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                11);
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        //savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,mRequestingLocationUpdates);
        //savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        //savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);

        super.onSaveInstanceState(savedInstanceState);
    }


    public void plotStreetLines(String jsonResponse){
        try{
            PolylineOptions polyOptions = new PolylineOptions();
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i <  jsonArray.length();i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                JSONArray jsonCoordinates = jsonObject.getJSONArray("points");
                int probability = jsonObject.getInt("prob");
                int color ;
                if (probability == 0)
                    color = Color.RED;
                else if (probability == 0.4)
                    color = Color.GREEN;
                else if (probability == -1)
                    color = Color.GREEN;
                else
                    color = Color.YELLOW;
                LatLng streetPoint1 =null;
                LatLng streetPoint2 = null;
                for(int j =0; j < jsonCoordinates.length();j++){
                    JSONObject coordinate = jsonCoordinates.getJSONObject(j);
                    if (streetPoint1 == null)
                        streetPoint1  = new LatLng(coordinate.getDouble("lat"),coordinate.getDouble("lon"));
                    else
                        streetPoint2 = new LatLng(coordinate.getDouble("lat"),coordinate.getDouble("lon"));
                }
                runOnUiThread(new DrawPoints(streetPoint1, streetPoint2, gMap,color));


                //Log.d("Point",jsonObject.get("points").toString());
            }

        }catch (JSONException e){
            ParkSearch parkSearch = (ParkSearch) getApplicationContext();
            Snackbar.make(parkSearch.findViewById(R.id.fab),"We Encountered Some Error Please Try Later",Snackbar.LENGTH_LONG).show();

        }catch (Exception e){
            ParkSearch parkSearch = (ParkSearch) getApplicationContext();
            Snackbar.make(parkSearch.findViewById(R.id.fab),"We Encountered Some Error Please Try Later",Snackbar.LENGTH_LONG).show();

        }

    }

    class DrawPoints implements Runnable {

        private LatLng sp1, sp2;
        private GoogleMap gMap;
        private int color;

        public DrawPoints(LatLng p1, LatLng p2, GoogleMap m, int col) {
            sp1 = p1;
            sp2 = p2;
            gMap = m;
            color = col;
        }

        public void run() {
            polyLines.add(gMap.addPolyline(new PolylineOptions()
                    .add(sp1, sp2)
                    .color(color)));

        }

    }

    public void clearPolyLines(){
        for(Polyline line : polyLines)
        {
            line.remove();
        }

        polyLines.clear();

    }

    public void setupSNS()  {
        PushManager pushManager = AWSMobileClient.defaultMobileClient().getPushManager();
        final GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        final int code = api.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS != code) {
            final String errorString = api.getErrorString(code);
            Log.e(TAG, "Google Services Availability Error: " + errorString + " (" + code + ")");

            if (api.isUserResolvableError(code)) {
                Log.e(TAG, "Google Services Error is user resolvable.");
                //api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES);
                return;
            } else {
                Log.e(TAG, "Google Services Error is NOT user resolvable.");
                //showErrorMessage(R.string.push_demo_error_message_google_play_services_unavailable);
                return;
            }
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                // register device first to ensure we have a push endpoint.

                PushManager pushManager = AWSMobileClient.defaultMobileClient().getPushManager();

                pushManager.registerDevice();

                // if registration succeeded.
                if (pushManager.isRegistered()) {
                    try {
                        pushManager.setPushEnabled(true);
                        pushManager.subscribeToTopic(pushManager.getDefaultTopic());
                        endpoint = pushManager.getEndpointArn();
                        return null;
                    } catch (final AmazonClientException ace) {
                        Log.e(TAG, "Failed to change push notification status", ace);
                        return ace.getMessage();
                    }
                }
                return "Failed to register for push notifications.";
            }
        }.execute();
    }

    public void addNewSpot(LatLng newSpot){
       try{
           runOnUiThread(new addNewMarker(newSpot));


       }catch (Exception e){
           e.printStackTrace();
       }

    }

    class addNewMarker implements Runnable {

        private LatLng sp1, sp2;

        public addNewMarker(LatLng p1) {
            sp1 = p1;
             }

        public void run() {
            MarkerOptions newMarkerOptions = new MarkerOptions()
                    .position(sp1)
                    //.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(context
                    //        , R.drawable.ic_parksign)))
                    .title("Available Spot");

            Marker newMarkerSpot = gMap.addMarker(newMarkerOptions);
        }

    }

}

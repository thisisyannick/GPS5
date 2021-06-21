package com.example.gps5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    //Referenz zu UI elements
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;

    Switch sw_locationupdates;
    Switch sw_gps;

    Button btn_newWaypoint, btn_showWayPointList, btn_showMap;

    //variabel die prüft, ob wir standort tracken oder nicht
    boolean updateON = false;

    //derzeiter Standort
    Location currentLocation;

    //liste der gespeicherten Standorte
    List<Location> savedLocations;


    //API von Google für die Standortbestimmung - Herz der Applikation
    FusedLocationProviderClient fusedLocationProviderClient;

    // Standort anfragen - ist ein config file
    LocationRequest locationRequest;

    LocationCallback locationCallBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

// UI Variablen Werte geben

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);

        btn_newWaypoint = findViewById(R.id.btn_newWayPoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        btn_showMap = findViewById(R.id.btn_showMap);

        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);

        // Eigenschaften der LocationRequest

        locationRequest = new LocationRequest();

        //how often dies default location check occur?
        locationRequest.setInterval(1000 * 30);

        //how often dies the lcoation check occur when set to the most frequent update?
        locationRequest.setFastestInterval(1000 * 5);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // wird ausgelöst wenn intervall eintritt (5secs)
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull @NotNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //Standort sichern
                updateUIValues(locationResult.getLastLocation());
            }
        };

        btn_newWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get GPS Standort

                //add neuen GPS Standort zur Liste
                MyApplication myApplication = (MyApplication)getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);

            }
        });

        btn_showWayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ShowSavedLocationsList.class);
                startActivity(i);
            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);
            }
        });



        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()){
                    // most accurate - use GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                }else{
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()){
                    // tracking anschalten
                    startLocationUpdates();
                }
                else{
                    //tracking ausschalten
                    stopLocationUpdates();
                }
            }
        });


        updateGPS();


    } //ende onCreate method

    private void startLocationUpdates(){
        tv_updates.setText("Standort wird abgefragt");
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null );
        updateGPS();
    }

    private void stopLocationUpdates(){
        tv_updates.setText("Standort wird nicht abgefragt");
        tv_lat.setText("Kein Tracking");
        tv_lon.setText("Kein Tracking");
        tv_speed.setText("Kein Tracking");
        tv_address.setText("Kein Tracking");
        tv_accuracy.setText("Kein Tracking");
        tv_altitude.setText("Kein Tracking");
        tv_sensor.setText("Kein Tracking");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }
                else{
                    Toast.makeText( this,"This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }

    }

    private void updateGPS() {

        //Einverständnis von Nutzer um tracken zu dürfen
        //aktuellen Standort von user abfragen
        //updateUI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //einverständnis wurde erteilt
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //wir haben die einverstädnis

                    updateUIValues(location);
                    currentLocation = location;

                }
            });
        }
        else{
            //einverständnis wurde NICHT erteilt

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION} , PERMISSIONS_FINE_LOCATION);
            }

        }
    }

    private void updateUIValues(Location location) {

        // all textview objects mit neuem standort aktualisieren
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if (location.hasAltitude()) {
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        }
        else {
            tv_altitude.setText("Höhe kann nicht erkannt werden");
        }

        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getAltitude()));
        }
        else {
            tv_speed.setText("Es wird keine Bewegung festgestellt");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this );

        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addresses.get(0).getAddressLine(0));

        }
        catch (Exception e){
            tv_address.setText("Die Adresse kann derzeit nicht angezeigt werden");
        }

        MyApplication myApplication = (MyApplication)getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        // zeige Anzahl der Wegpunkte, die sich in Liste befinden
        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));


    }


}
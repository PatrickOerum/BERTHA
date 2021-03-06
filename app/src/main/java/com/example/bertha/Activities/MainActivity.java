package com.example.bertha.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bertha.HelperClasses.OnSwipeTouchListener;
import com.example.bertha.Model.CombinedSendData;
import com.example.bertha.R;
import com.example.bertha.REST.PostHttpTask;
import com.example.bertha.REST.ReadHttpTask;
import com.example.bertha.SendDataToDb;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //Toolbar
    private Toolbar toolbar;

    //Logging
    public static final String MINE = "MINE";

    //HTTP
    public static final String urlWristbandData = "https://berthawristbandrestprovider.azurewebsites.net/api/wristbanddata/";
    public static final String postToDbUrl = "https://berthabackendrestprovider.azurewebsites.net/api/data/";

    private TextView mainMessageTv;

    private CombinedSendData combinedDataNew;

    private int deviceId;
    private double pm25;
    private double pm10;
    private int co2;
    private int o3;
    private double pressure;
    private double temperature;
    private int humidity;
    private double latitude;
    private double longitude;
    private Timer timer;

    private LinearLayout layout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //String myString = savedInstanceState.getString("MyString")

        setContentView(R.layout.activity_main);

        //Timer
        timer = new Timer();

        //Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainMessageTv = findViewById(R.id.mainMessageTv);
        layout = findViewById(R.id.mainActivityLayout);

        //Swipe
        layout.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
                Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeRight() {
                Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeLeft() {
                Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeBottom() {
                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //outState.putString("MyString", "Denne string bliver gemt")
    }

    //Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.actionBarUserSettings) {
            Intent intent = new Intent(this, UserSettings.class);
            startActivity(intent);
        }
        else if(id == R.id.actionBarLogOut){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else if(id == R.id.actionBarSettings){

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void GoToShowAllData(View view) {
        Intent intent = new Intent(this, ShowAllDataActivity.class);
        startActivity(intent);
        Log.d(MINE, "GoToShowAllData: ");

    }

    public void StartSendLoop(View view) {
        doEvery10Seconds();
        Toast.makeText(this, "Startet", Toast.LENGTH_LONG).show();
    }

    public void stopSendLoop(View view) {
        timer.cancel();
        Toast.makeText(this, "Stoppet", Toast.LENGTH_SHORT).show() ;
    }

    private class ReadFromWristbandTask extends ReadHttpTask {
        @Override
        protected void onPostExecute(CharSequence jsonString) {
            Log.d(MINE, "Read from wristband: called");

            try {
                JSONObject jsonObject = new JSONObject(jsonString.toString());

                deviceId = jsonObject.getInt("deviceId");
                pm25 = jsonObject.getDouble("pm25");
                pm10 = jsonObject.getDouble("pm10");
                co2 = jsonObject.getInt("co2");
                o3 = jsonObject.getInt("o3");
                pressure = jsonObject.getDouble("pressure");
                temperature = jsonObject.getDouble("temperature");
                humidity = jsonObject.getInt("humidity");

                getLatLongMethod();

                Log.d(MINE, "Retrieved data: success");

            } catch (JSONException ex) {
                mainMessageTv.setText(ex.getMessage());
                Log.d(MINE, "ReadFromWristBandTask" + ex.getMessage());
            }
        }
    }

    public void postDataToDb() {
        String userId = "pady";
        int noise = 2;

        combinedDataNew = new CombinedSendData(deviceId, co2, o3, humidity, pm25, pm10, pressure, temperature, new Date().getTime(), latitude, longitude, noise, userId);
        //if you are using the emulator, please change object to testobject from onCreate
        //Converts object to json with gson
        Gson gson = new Gson();
        Log.d(MINE, "postDataToDb: " + combinedDataNew.toString());
        String jsonDoc = gson.toJson(combinedDataNew);
        PostHttpTask task = new PostHttpTask();
        task.execute(postToDbUrl, jsonDoc);
        Log.d(MINE, "postDataToDb: " + jsonDoc);
    }


    public void getLatLongMethod() {
        Log.d(MINE, "getLatLongMethod: called");
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        showLocation(location);
        //TODO uncomment theese
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        Log.d(MINE, "Location found: Longitude: " + longitude +" latitude: " + latitude);
    }

    private void showLocation(Location location){
        if(location == null){
            Toast.makeText(this, "Location is null", Toast.LENGTH_LONG).show();
            return;
        }

    }


    public void doEvery10Seconds(){
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ReadFromWristbandTask getData = new ReadFromWristbandTask();
                getData.execute(urlWristbandData);
                postDataToDb();
            }
        },0, 10000);

    }
}

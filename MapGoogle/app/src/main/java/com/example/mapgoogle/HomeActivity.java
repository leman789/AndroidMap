package com.example.mapgoogle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private GoogleMap mMap;
    public String [] Photos = new String[1];
    public ImageView iv;

    public ListView list ;
    private ArrayAdapter<String> adapter ;

    public String sendTypeOfProblem = ":c",sendLocation  = ":c",sendPhoto  = ":c";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_home);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        String cars[] = {"Usterka nr 1", "Usterka nr 2", "Usterka nr 3", "Usterka nr 4", "Usterka nr 5"};
        list = (ListView) findViewById(R.id.listView1);
        ArrayList<String> carL = new ArrayList<String>();
        carL.addAll( Arrays.asList(cars) );

        adapter = new ArrayAdapter<String>(this, R.layout.row, carL);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                sendTypeOfProblem = cars[position];
            }
        });


        Button button = findViewById(R.id.btn_camera);
        Button button2 = findViewById(R.id.btn_send);
        Button button3 = findViewById(R.id.btn_map);
        iv = findViewById(R.id.iv_image);
        Intent myIntent = new Intent(this, MapsActivity.class);

        //check permissions
        if (ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }else{
            LocationManager locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new HomeActivity.MyLocationListener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }
        //Camera button
        button.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View view) {
                                          if(ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                                              Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                              startActivityForResult(intent, 2);
                                          }
                                          else{
                                              Photos[0] = Manifest.permission.CAMERA;
                                              ActivityCompat.requestPermissions(HomeActivity.this,Photos,2);
                                          }
                                      }
                                  }
        );

        //Send button
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPost();
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(myIntent);
            }
        });
    }

    //get current location
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            TextView editLocation = (TextView)findViewById(R.id.textView);

            Toast.makeText(getBaseContext(),
                    "Location changed Lat " + loc.getLatitude()
                            +" Lng  " + loc.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            String longitude = "Longitude " + loc.getLongitude();

            String latitude = "Latitude " + loc.getLatitude();

            /*------- To get city name from coordinates -------- */

            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String stringLocation = longitude + "\n" + latitude + "\n";
            sendLocation=stringLocation;
            editLocation.setText(stringLocation);
        }
    }


    //Make photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2 && resultCode == RESULT_OK && data != null){
            Bundle bundle = data.getExtras();
            Bitmap finalPhoto = (Bitmap) bundle.get("data");
            iv.setImageBitmap(finalPhoto);
            String stringBitmap = "";
            stringBitmap = getStringFromBitmap(finalPhoto);
            sendPhoto = stringBitmap;
        }
    }

    //parse Photo to String Bitmap
    private String getStringFromBitmap(Bitmap bitmapPicture) {
        final int COMPRESSION_QUALITY = 100;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    //sending JSON to AWS server
    public void sendPost() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://6ti4xfuan6.execute-api.us-east-1.amazonaws.com/Prod/q");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("Type", sendTypeOfProblem);
                    jsonParam.put("Photo", sendPhoto);
                    jsonParam.put("Location", sendLocation);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
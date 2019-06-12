package com.coders.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient mFusedLocationClient;

    private double wayLatitude = 32.0, wayLongitude = 32.0;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lmAcc;

    private TextView txtContinueLocation;
    private TextView textViewRadiusShower;
    private TextView textViewAccuracyShower;
    private StringBuilder stringBuilder;

    private boolean isContinue = false;
    private boolean isGPS = false;

    private GoogleMap mMap;
    private Circle circle;
    private Circle circle_manual;

    private final static int REQUEST_lOCATION = 90;
    private static final int SMS_PERMISSION_CODE = 1003;

    public BackgroundService gpsService;

    private MediaPlayer mp;

    private Button btnLetLocTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Kullanıcı Ayarlarını Yukluyoruz.
        MainActivitySettings.get_settings(getApplicationContext());

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Uygulamayı portrait'de sabit tutuyoruz.
        setContentView(R.layout.activity_main);

        mp = MediaPlayer.create(MainActivity.this, R.raw.submarine2);

        // Background service ile alakalı baslatma
        final Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
        MainActivity.this.getApplication().startService(intent);
        //this.getApplication().startForegroundService(intent);
        MainActivity.this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        // Background service ile alakalı baslatma


        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);
        }
        else{
            birkereyemahsusbaslangic();
            AppConstants.run_once =1;
        }


        // Alarm Kurma
        final Button btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnSetAlarm.setOnClickListener(v -> {
            if(AppConstants.ALARM == 0){
                // Alarm Kurulucak
                AppConstants.ALARM = 1;
                btnSetAlarm.setText("Alarm Kapa");
                AppConstants.Alarm_Latitude = wayLatitude;
                AppConstants.Alarm_Longitude = wayLongitude;

                if(circle!=null)
                {
                    AppConstants.Alarm_Latitude = circle.getCenter().latitude;
                    AppConstants.Alarm_Longitude = circle.getCenter().longitude;
                }
                if(circle_manual!=null)
                {
                    AppConstants.Alarm_Latitude = circle_manual.getCenter().latitude;
                    AppConstants.Alarm_Longitude = circle_manual.getCenter().longitude;
                }
                btnSetAlarm.setBackgroundColor(Color.RED);
                MarkerOptions marker = new MarkerOptions().position(new LatLng(AppConstants.Alarm_Latitude, AppConstants.Alarm_Longitude)).title("New Marker");
                mMap.addMarker(marker).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.anchor_small));

            }else {
                // Alarm Devre Dışı Kalacak
                AppConstants.ALARM = 0;
                btnSetAlarm.setText("Alarm Kur");
                btnSetAlarm.setBackgroundColor(Color.BLUE);
                // Alarm Sesini Kapatmak icin AlarmKontrol u cagiriyoruz
                AlarmKontrol();
                if(circle!=null)
                {
                mMap.clear();
                circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(wayLatitude, wayLongitude))
                        .radius(AppConstants.RADIUS)
                        .strokeWidth(8f)
                        .strokeColor(Color.RED)
                        .fillColor(0x2233FFBB));
                }
                if(circle_manual!=null)
                {
                    mMap.clear();
                    circle_manual = mMap.addCircle(new CircleOptions()
                            .center(new LatLng(circle_manual.getCenter().latitude, circle_manual.getCenter().longitude))
                            .radius(AppConstants.RADIUS)
                            .strokeWidth(8f)
                            .strokeColor(Color.RED)
                            .fillColor(0x2233FFBB));
                }
            }

        });

        // Ayarlara Geçiş
        final Button btn_settings = findViewById(R.id.btnSettings);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent activity2Intent = new Intent(getApplicationContext(), MainActivitySettings.class);
                startActivity(activity2Intent);
            }
        });

        // Konum Takibini Bırakma
        btnLetLocTrack = findViewById(R.id.btnLetLocTrack);
        btnLetLocTrack.setOnClickListener(v -> {
            if(AppConstants.LOCATION_TRACKING_STATUS == 1){
                // Mavi nokta hareket edecek fakat circle takibi bırakacak
                AppConstants.LOCATION_TRACKING_STATUS = 0;
                btnLetLocTrack.setText("KONUM TAKİBİNE DEVAM ET");
                Toast.makeText(getBaseContext(), "Halka takibi bıraktı.", Toast.LENGTH_LONG).show();

            }else{
                // Circle mavi noktayı takibe devam edecek
                AppConstants.LOCATION_TRACKING_STATUS = 1;
                btnLetLocTrack.setText("KONUM TAKİBİNİ BIRAK");
                if(AppConstants.ALARM == 1)
                {
                    AppConstants.ALARM=1;
                }else {
                    Toast.makeText(getBaseContext(), "Halka konumuz ile birlikte güncellenecek.", Toast.LENGTH_LONG).show();
                    if (circle_manual != null) {
                        mMap.clear(); // Her tıklamada haritayı temizliyoruz.
                        //circle_manual.remove(); // Kullanıcının elle koydugu daireyi kaldırıyoruz
                        circle_manual = null;
                    }
                }
                birkereyemahsusbaslangic();
            }
        });

        // Ekranın sağ altında çember çapını gösteriyoruz.
        textViewRadiusShower = findViewById(R.id.textViewRadiusShower);
        textViewRadiusShower.setText("Alarm Çapı: " + AppConstants.RADIUS +" METRE");
    }



    @Override
    public void onDestroy()
    {
        super.onDestroy();
        gpsService.stopTracking();
    }

    // alarm Kontrol Etme

    public void AlarmKontrol() {
        // Alarm Sesi
        if(AppConstants.ALARM==0){
            // Alarm Kapalı
            mp.pause();
        }
        else {
            float[] distance = new float[2];
/*
        Toast.makeText(getBaseContext(), "A latitude: " + AppConstants.Alarm_Latitude, Toast.LENGTH_SHORT).show();
        Toast.makeText(getBaseContext(), "A lonitude: " + AppConstants.Alarm_Longitude, Toast.LENGTH_SHORT).show();

        Toast.makeText(getBaseContext(), "B latitude: " + wayLatitude, Toast.LENGTH_SHORT).show();
        Toast.makeText(getBaseContext(), "B longtitude: " + wayLongitude, Toast.LENGTH_SHORT).show();
        */
            Location.distanceBetween(AppConstants.Alarm_Latitude, AppConstants.Alarm_Longitude, wayLatitude, wayLongitude, distance);

            if (distance[0] > AppConstants.RADIUS) {
                // ALARM ÇAL
                Toast.makeText(getBaseContext(), "Outside, distance from center: " + distance[0] + " radius: " + AppConstants.RADIUS, Toast.LENGTH_LONG).show();

                if (mp.isPlaying()) {
                    //stop or pause your media player mediaPlayer.stop(); or mediaPlayer.pause();
                    //mPlayer.stop();
                } else {
                    //mPlayer.start();
                    mp.start();
                    mp.setLooping(true);
                }
                // 1 Kereliğine SMS at
                if(AppConstants.SmsAlarmSend != 1){
                    if(AppConstants.SmsAlarm) {
                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(AppConstants.SMSCepNo, null, "Tekne Demir Tarıyor.", null, null);
                            Toast.makeText(getApplicationContext(), "SMS Alarm yollandı.", Toast.LENGTH_LONG).show();
                            AppConstants.SmsAlarmSend=1;
                    }
                }
            }
            else
            {
                // Halka Capına geri dondu alarmı kapa Düzeldi diye sms at

            }
        }
    }

    // Harita Ortalama Devamlı Çalışıyor
    private void HaritaOrtala() {
        //Takip Eden Daire
        if (AppConstants.ALARM == 1 && AppConstants.LOCATION_TRACKING_STATUS == 1){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(wayLatitude, wayLongitude)));
        } else if (AppConstants.ALARM == 0 && AppConstants.LOCATION_TRACKING_STATUS == 1){
            if (circle != null) {
                circle.setCenter(new LatLng(wayLatitude, wayLongitude));
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(wayLatitude, wayLongitude)));
        }
        // Zoom cok yuksekse kucult
        float zoom = mMap.getCameraPosition().zoom;
        if(Math.round(zoom) < 17)
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(wayLatitude, wayLongitude)));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        }
    }


    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);

        } else {
            if (isContinue) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();

                    } else {
                        wayLatitude= 32.0;
                        wayLongitude=32.0;
                    }
                });
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();

                    } else {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(AppConstants.run_once==0) {
            birkereyemahsusbaslangic();
        }

        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (isContinue) {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        System.out.println("girdimm22");
                    } else {

                        System.out.println("girdimm33");
                        /*
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                            if (location != null) {
                                wayLatitude = location.getLatitude();
                                wayLongitude = location.getLongitude();

                            } else {
                                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        });
                        */
                    }
                } else {
                    Toast.makeText(this, "İzin Verilmedi", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case 1001: {

            }
        }
    }

    private void birkereyemahsusbaslangic() {
        this.txtContinueLocation = findViewById(R.id.txtContinueLocation);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds




        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {

                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        if (isContinue) {
                            //txtContinueLocation.setText(wayLatitude+"-"+wayLongitude);
                            txtContinueLocation.setText(DMS_Cevir(wayLatitude, wayLongitude));
                            // Gps Hata Payı
                            AppConstants.DEFAULT_ACCURACY = location.getAccuracy();
                            textViewAccuracyShower = findViewById(R.id.textViewAccuracyShower);
                            if(textViewAccuracyShower != null) {
                                textViewAccuracyShower.setText("Gps Hata Payı:" + AppConstants.DEFAULT_ACCURACY + " m");
                            }

                            System.out.println("location_update");

                            HaritaOrtala();
                            if (AppConstants.ALARM == 1) {
                                AlarmKontrol();
                            }
                        }
                        if (!isContinue && mFusedLocationClient != null) {
                            // mFusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };

        // Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Otomatik Baslangic
        isContinue = true;
        getLocation();

        // Uydu,Harita geçiş kısmı.
        final Button btn_MapType = findViewById(R.id.btnMapViewSat);
        btn_MapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    btn_MapType.setText("NORMAL");
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    btn_MapType.setText("UYDU");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }

    // Google Maps
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap = googleMap;
            mMap.setMyLocationEnabled(true);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_lOCATION);
            }
        }
        // Mevcut Konum
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Add a marker in Sydney and move the camera
            // LatLng sydney = new LatLng(-34, 151);
            // mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
            // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


            //Kamerayı şuanki konuma kaydırıyor ilk açılışta.
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

            System.out.println("daire yaratirim");
            if (AppConstants.ALARM == 0) {
                if(circle == null){
                circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(AppConstants.RADIUS)
                        .strokeWidth(8f)
                        .strokeColor(Color.RED)
                        .fillColor(0x2233FFBB));}
            }
        }
        else{
            // Location request açık değil
            wayLongitude=32.0 ;
            wayLongitude=33.0;
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            // Haritada dokunulan yere marker ekleyip, etrafına standart çemberi ekliyoruz.
            @Override
            public void onMapClick(LatLng point) {
                if(AppConstants.ALARM == 0) { // Alarm Kurulmamıs ise Daire Eklenebilir
                    LatLng latLng = new LatLng(point.latitude, point.longitude);
                    if (AppConstants.LOCATION_TRACKING_STATUS == 0) {
                        mMap.clear(); //Önce haritayı temizleyip ardından marker ile circle ekliyoruz, kullanıcının haritada bastığı yere.
                        if (circle != null) {
                            circle.remove(); // Otomotik olusturulan daireyi kaldırıyoruz
                            circle = null;
                        }
                        if (circle_manual != null) {
                            circle_manual.remove(); // Bir onceki Manual Daireyi Kaldırıyoruz
                            circle_manual = null;
                        }
                        circle_manual = mMap.addCircle(new CircleOptions()
                                .center(latLng)
                                .radius(AppConstants.RADIUS)
                                .strokeWidth(8f)
                                .strokeColor(Color.RED)
                                .fillColor(0x2233FFBB));

                    }
                } else {
                    Toast.makeText(getBaseContext(), "Alarm kuruluyken halkanın yeri değiştilemez. Lütfen Alarmı kapatın." , Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    private String DMS_Cevir(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();

        if (latitude < 0) {
            builder.append("S ");
        } else {
            builder.append("N ");
        }

        String latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");
        builder.append(latitudeSplit[1]);
        builder.append("'");
        builder.append(latitudeSplit[2]);
        builder.append("\"");

        builder.append(" ");

        if (longitude < 0) {
            builder.append("W ");
        } else {
            builder.append("E ");
        }

        String longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
        String[] longitudeSplit = longitudeDegrees.split(":");
        builder.append(longitudeSplit[0]);
        builder.append("°");
        builder.append(longitudeSplit[1]);
        builder.append("'");
        builder.append(longitudeSplit[2]);
        builder.append("\"");

        return builder.toString();


    }


    // Kod baska ekrana geldi
    @Override
    protected void onPause() {
        super.onPause();
        isContinue = false;

        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            //System.out.println("location_update stop");
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        isContinue = true;
        if(mFusedLocationClient != null) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }

        // ayarlardan geri donunce reset ediliyor takip bug olusmasın diye
        AppConstants.LOCATION_TRACKING_STATUS = 1;
        btnLetLocTrack.setText("KONUM TAKİBİNİ BIRAK");

/*
        if(mMap.getCameraPosition().zoom > 17) {
            final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            final Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        }*/
    }


    private Notification getNotification() {
        NotificationChannel channel = new NotificationChannel(
                "channel_01",
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01");
        return builder.build();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundService")) {
                gpsService = ((BackgroundService.LocationServiceBinder) service).getService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundService")) {
                gpsService = null;
            }
        }
    };
}



package com.coders.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Random;

public class MainActivitySettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(ActivityCompat.checkSelfPermission(MainActivitySettings.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivitySettings.this, new String[]{Manifest.permission.SEND_SMS}, 1003);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_settings);

        // Çapı Texte yazdır
        final EditText alarm_capi = findViewById(R.id.editTextAlarmCap);
        //System.out.println("abcdc"+AppConstants.RADIUS);
        alarm_capi.setText(Integer.toString(AppConstants.RADIUS));

        // Sms Checkbox
        CheckBox checkSms = findViewById(R.id.checkBoxAlarm);
        checkSms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
               @Override
               public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                   if(isChecked){
                       if(AppConstants.SMSCepNo != "")
                       {
                           AppConstants.SmsAlarm=true;
                       }
                       else
                       {
                           // Sms Tanımlanmamıs
                           checkSms.setChecked(false);
                       }
                   }
                   if(!isChecked){
                       AppConstants.SmsAlarm=false;
                   }
               }
           }
        );

        // Sms Doğrulama PopUP
        Button smsPopUp = findViewById(R.id.btnDogrulaPopUp);
        smsPopUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // Telefon Validasyon
                final EditText telefon = findViewById(R.id.editTextSmsNo);
                if(telefon.getText().length()>10)
                {
                    String cep_tel = telefon.getText().toString();
                    final int min = 1000;
                    final int max = 9999;
                    AppConstants.random_number = new Random().nextInt((max - min) + 1) + min;

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(cep_tel, null, "Sms uyarilarini aktifleştirmek için "+AppConstants.random_number+" giriniz", null, null);
                        Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(),"SMS faild, please try again.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    AppConstants.tempSMSCepNo = cep_tel;
                    startActivity(new Intent(MainActivitySettings.this, MainActivitySettingsSmsPopUp.class));


                }
                else {
                    telefon.setError( "Telefon Numarası Kontrol Ediniz" );
                }
            }
        });

        // Çapı Texten guncelle
        alarm_capi.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                System.out.println("abcdc1");
                String sUsername = alarm_capi.getText().toString();
                if(sUsername.matches("")){
                    alarm_capi.setError( "Alarm Çapi Min. 5 Metredir" );
                }else {

                    if (Integer.parseInt(alarm_capi.getText().toString()) > 4) {
                        AppConstants.RADIUS = Integer.parseInt(alarm_capi.getText().toString());
                    }
                    else{
                        alarm_capi.setError( "Alarm Çapi Min. 5 Metredir" );
                    }
                }
            }

        });
        alarm_capi.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // code to execute when EditText loses focus
                    System.out.println("abcdc2");
                    String sUsername = alarm_capi.getText().toString();
                    if(sUsername.matches("")){
                        alarm_capi.setError( "Alarm Çapi Min. 5 Metredir" );
                        AppConstants.RADIUS = 30;
                        alarm_capi.setText("30");
                    }else {
                        if (Integer.parseInt(alarm_capi.getText().toString()) < 5) {
                            AppConstants.RADIUS = 30;
                            alarm_capi.setText("30");

                            Toast.makeText(getBaseContext(), "Alarm Çapı Minimum 5 Metredir", Toast.LENGTH_SHORT).show();
                            alarm_capi.setError("Alarm >api Min. 5 Metredir");
                        }
                    }
                }
            }
        });


        //Anasayfaya Geri Don
        final Button btn_settings = findViewById(R.id.btnAnasayfa);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent activity2Intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(activity2Intent);
            }
        });


    }


    @Override
    protected void onPause() {
        super.onPause();
        save_settings();
    }

    // Forma Geri Donus Oldugunda
    @Override
    public void onResume() {
        super.onResume();
        final EditText telefon = findViewById(R.id.editTextSmsNo);
        telefon.setText(AppConstants.SMSCepNo);
    }

    // Kullanıcı Ayarları Kaydetme
    private void save_settings() {

        SharedPreferences settings = getSharedPreferences("UltraAnchor", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("RADIUS",AppConstants.RADIUS);

        editor.apply();
    }

    // Kullanıcı Ayarlari Yukleme
    public static void get_settings(Context c) {
        SharedPreferences settings = c.getSharedPreferences("UltraAnchor", 0);
        AppConstants.RADIUS = settings.getInt("RADIUS",30);
    }
}

package com.coders.location;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivitySettingsSmsPopUp extends MainActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings_sms_pop_up);
        // Pencere Ölçülerini Alıyoruz
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;
        // Pencereyi Orantısal Küçültüyoruz
        getWindow().setLayout((int)(width*.8),(int)(height*.6));



        // COUNTDOWN TIMER
        TextView mTextField = (TextView) findViewById(R.id.textViewTimer);

        new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTextField.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                mTextField.setText("done!");
                // Popup i kapa
                finish();
            }
        }.start();

        // Sms Doğrulama PopUP
        Button smsPopUp = (Button) findViewById(R.id.btnDogrula);
        smsPopUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // Text deki veriyi al
                final EditText dogrulama = findViewById(R.id.editTextDogrulamaKodu);
                String dogrulama_kodu = dogrulama.getText().toString();
                if(Integer.parseInt(dogrulama_kodu) == AppConstants.random_number){
                    // Dogrulandi
                    AppConstants.SMSCepNo = AppConstants.tempSMSCepNo;
                    finish();
                }else{
                    //Dogrulanmadi Hata Ver
                    // System.out.println("giriyorum3");
                    dogrulama.setError( "Yanlış Kod" );
                }

            }
        });




    }
}

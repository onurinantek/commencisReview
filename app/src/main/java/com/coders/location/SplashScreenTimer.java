package com.coders.location;

import android.app.Application;
import android.os.SystemClock;

public class SplashScreenTimer extends Application {

    // İlk açılışta yaşanan crash sorununu çözme ihtimaline karşı bu class'ı da ekledim. Splash screen ne kadar süre ekranda gözükecek onu ayarlıyor.
    // AndroidManifes'te de android:name=".SplashScreenTimer"> olarak eklemesi var.
    @Override
    public void onCreate() {
        super.onCreate();
        SystemClock.sleep(2000);
    }
}

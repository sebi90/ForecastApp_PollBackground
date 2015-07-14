package ema.lecture.forecast.autopollbackground;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Sebi on 14.07.15.
 */
public class MyReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, FetchAndBroadcastWeatherService.class));


    }

}

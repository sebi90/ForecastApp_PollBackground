package ema.lecture.forecast.autopollbackground;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class FetchAndBroadcastWeatherService extends Service {
  //private static final InetSocketAddress SERVER = new InetSocketAddress("192.168.56.1", 4711);
  private static final InetSocketAddress SERVER = new InetSocketAddress("10.0.2.2", 4711);

  public static final String BC_ACT_FETCH_STARTED = "ema.lecture.forecast.bc.FETCH_STARTED";

  public static final String BC_ACT_FETCH_FINISHED = "ema.lecture.forecast.bc.FETCH_FINISHED";
  public static final String BC_EXTRA_SUCCESSFUL = "ema.lecture.forecast.bcextr.SUCCESSFUL";
  public static final String BC_EXTRA_CONTENT = "ema.lecture.forecast.bcextr.CONTENT";

  private ScheduledExecutorService execSrv;

  private final BroadcastReceiver button_clicked = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {

      new Thread(new Runnable() {
        @Override
        public void run() {
         FetchAndBroadcastWeatherService.this.fetchAndBroadcastWeather();
        }
      });
    }
  };

  private int intervall = 10;

  public void setIntervall(int intervall)
  {
    this.intervall = intervall;
  }

  //private IBinder mBinder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return null;
    //return mBinder;
  }

  /*
  public class LocalBinder extends Binder
  {
    public FetchAndBroadcastWeatherService getService()
    {
      return FetchAndBroadcastWeatherService.this;
    }
  }

*/
  @Override public void onCreate() {
    Log.d("myTag", "Weather Service onCreate");
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ShowForecastActivity.BUTTON_CLICKED);

    registerReceiver(button_clicked, filter);
    init();
  }

  public void init()
  {
    if(intervall != -1) {
      if(this.execSrv != null)
      {
        execSrv.shutdown();
        execSrv = null;
      }
      this.execSrv = Executors.newSingleThreadScheduledExecutor();
      this.execSrv.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          Log.d("myTag", "Intervall = " + intervall);
          FetchAndBroadcastWeatherService.this.fetchAndBroadcastWeather();
        }
      }, 0, intervall, TimeUnit.SECONDS);
    }
    else
    {
      Log.d("myTag" ,"execSrv shutdown");
      execSrv.shutdownNow();
    }

  }

  @Override public void onDestroy() {
    this.execSrv.shutdownNow();
  }

  @Override public int onStartCommand(final Intent intent, final int flags, final int startId) {
    return Service.START_STICKY;
  }

  //
  //

  public void fetchAndBroadcastWeather() {
    this.sendBroadcast(new Intent(FetchAndBroadcastWeatherService.BC_ACT_FETCH_STARTED));

    final Intent bcIntent = new Intent(FetchAndBroadcastWeatherService.BC_ACT_FETCH_FINISHED);
    try {
      final String newWeather = this.getCurrentWeatherFromServer();
      bcIntent.putExtra(FetchAndBroadcastWeatherService.BC_EXTRA_SUCCESSFUL, true);
      bcIntent.putExtra(FetchAndBroadcastWeatherService.BC_EXTRA_CONTENT, newWeather);

    } catch (final IOException ioe) {
      bcIntent.putExtra(FetchAndBroadcastWeatherService.BC_EXTRA_SUCCESSFUL, false);
      bcIntent.putExtra(FetchAndBroadcastWeatherService.BC_EXTRA_CONTENT, ioe.toString());
    }

    this.sendStickyBroadcast(bcIntent);
  }

  private String getCurrentWeatherFromServer() throws IOException {
    final InetAddress serverAddress = FetchAndBroadcastWeatherService.SERVER.getAddress();
    final int serverPort = FetchAndBroadcastWeatherService.SERVER.getPort();

    try (Socket socketToServer = new Socket(serverAddress, serverPort)) {
      final BufferedReader r = new BufferedReader(new InputStreamReader(socketToServer.getInputStream(), "UTF-8"));
      return r.readLine();
    }
  }
}

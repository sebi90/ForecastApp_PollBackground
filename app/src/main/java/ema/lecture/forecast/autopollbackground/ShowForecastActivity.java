package ema.lecture.forecast.autopollbackground;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ShowForecastActivity extends Activity {
  private ImageView ivWeather;
  private TextView tvWeather;
  private Button buttonRefresh;

  private boolean mBounded;
  private FetchAndBroadcastWeatherService mService;

  private int intervall = 10;

  @Override
  protected void onStart() {
    super.onStart();
    Intent mIntent = new Intent(this, FetchAndBroadcastWeatherService.class);
    bindService(mIntent, mConnection, BIND_AUTO_CREATE);
  };

  ServiceConnection mConnection = new ServiceConnection()
  {
    public void onServiceConnected(ComponentName name, IBinder service) {
      mBounded = true;
      FetchAndBroadcastWeatherService.LocalBinder mLocalBinder = (FetchAndBroadcastWeatherService.LocalBinder)service;
      mService = mLocalBinder.getService();
    }

    public void onServiceDisconnected(ComponentName name) {
      mBounded = false;
      mService = null;
    }
  };

  @Override
  protected void onStop()
  {
    super.onStop();
    if(mBounded)
    {
      unbindService(mConnection);
      mBounded = false;
    }
  };



  private final BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
    @Override public void onReceive(final Context context, final Intent intent) {
      ShowForecastActivity.this.onWeatherServiceBroadcastReceived(intent);
    }
  };

  private final BroadcastReceiver internetAndPowerReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      ShowForecastActivity.this.onInternetAndPowerReceived(intent);
    }
  };

  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    this.setContentView(R.layout.activity_show_forecast);

    this.ivWeather = (ImageView) this.findViewById(R.id.ivWeather);
    this.tvWeather = (TextView) this.findViewById(R.id.tvWeather);
    this.buttonRefresh = (Button) this.findViewById(R.id.buttonRefresh);

  }

  @Override protected void onResume() {
    super.onResume();

    final IntentFilter iFilter = new IntentFilter();
    iFilter.addAction(FetchAndBroadcastWeatherService.BC_ACT_FETCH_STARTED);
    iFilter.addAction(FetchAndBroadcastWeatherService.BC_ACT_FETCH_FINISHED);
    this.registerReceiver(this.weatherReceiver, iFilter);

    final IntentFilter ipFilter = new IntentFilter();
    ipFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    ipFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

    this.registerReceiver(this.internetAndPowerReceiver, ipFilter);



  }

  //
  //

  @Override protected void onPause() {
    super.onPause();
    this.unregisterReceiver(this.weatherReceiver);
    this.unregisterReceiver(this.internetAndPowerReceiver);
  }

  //plugged 0 = kein Ladegerät
  //level = Akkuprozentzahl
  //./adb shell am broadcast -a android.intent.action.BATTERY_CHANGED --ei plugged 0 --ei level 9

  // WIFI aktivieren
  //./adb shell am broadcast -a android.net.conn.CONNECTIVITY_CHANGE --ei networkType 1

  // noConnectivity aktivieren
  //./adb shell am broadcast -a android.net.conn.CONNECTIVITY_CHANGE --ei noConnectivity 0


  private void onInternetAndPowerReceived(final Intent ipIntent)
  {
    if ((ipIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0) && (ipIntent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE,-1) == 1))
    {
      intervall = 5;
    }
    else if((ipIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) <= 10) || (ipIntent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)))
    {
      intervall = -1;
    }
    else
    {
      intervall = 10;
    }

    if (mService != null)
    {
      myInternetPowerAsyncTask t = new myInternetPowerAsyncTask();
      t.execute();
    }


  }

  private void onWeatherServiceBroadcastReceived(final Intent bcIntent) {
    final String intentAction = bcIntent.getAction();
    if (intentAction.equals(FetchAndBroadcastWeatherService.BC_ACT_FETCH_STARTED)) {
      this.setProgressBarIndeterminateVisibility(true);
      this.buttonRefresh.setEnabled(false);

    } else if (intentAction.equals(FetchAndBroadcastWeatherService.BC_ACT_FETCH_FINISHED)) {
      final String conent = bcIntent.getStringExtra(FetchAndBroadcastWeatherService.BC_EXTRA_CONTENT);
      if (bcIntent.getBooleanExtra(FetchAndBroadcastWeatherService.BC_EXTRA_SUCCESSFUL, false)) {
        this.onRefreshWeatherSucceeded(conent);

      } else {
        this.onRefreshWeatherFailed(conent);
      }
    }
  }

  //
  //

  public class myAsyncTask extends AsyncTask
  {
    @Override
    protected Object doInBackground(Object[] params)
    {
      mService.fetchAndBroadcastWeather();
      return null;
    }
  }

  public class myInternetPowerAsyncTask extends AsyncTask
  {
    @Override
    protected Object doInBackground(Object[] params)
    {
      mService.setIntervall(intervall);
      mService.init();
      return null;
    }
  }

  public void onButtonRefreshClicked(final View buttonRefresh) {
    this.setProgressBarIndeterminateVisibility(true);
    this.buttonRefresh.setEnabled(false);

    if (mService != null)
    {
      myAsyncTask t = new myAsyncTask();
      t.execute();
    }
  }

  //
  //

  private void onRefreshWeatherSucceeded(final String newWeather) {
    this.setProgressBarIndeterminateVisibility(false);
    this.buttonRefresh.setEnabled(true);

    final int imageResourceId = this.getResources().getIdentifier(newWeather, "drawable", this.getPackageName());
    this.ivWeather.setImageResource(imageResourceId);
    this.tvWeather.setText(newWeather);
    Log.d("myTag", "Succeeded");
  }

  private void onRefreshWeatherFailed(final String failCause) {
    this.setProgressBarIndeterminateVisibility(false);
    this.buttonRefresh.setEnabled(true);

    final AlertDialog dia = new AlertDialog.Builder(this).create();
    dia.setTitle(this.getText(R.string.refreshWeatherFailedTitle));
    dia.setMessage(failCause);
    dia.show();
  }
}

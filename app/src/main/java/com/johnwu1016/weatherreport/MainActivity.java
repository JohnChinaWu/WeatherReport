package com.johnwu1016.weatherreport;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private CurrentWeather currentWeather;

    @InjectView(R.id.timeLabel)
    TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel)
    TextView temperatureLabel;
    @InjectView(R.id.humidityValue)
    TextView humidityValue;
    @InjectView(R.id.precipValue)
    TextView precipValue;
    @InjectView(R.id.summaryLabel)
    TextView summaryLabel;
    @InjectView(R.id.iconImageView)
    ImageView iconImageView;
    @InjectView(R.id.refreshImageView)
    ImageView refresh;
    @InjectView(R.id.progressBar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        progressBar.setVisibility(View.INVISIBLE);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forecast(34.0201613, -118.6919178);
            }
        });

        forecast(34.0201613, -118.6919178);
        Log.v(TAG, "Main Activity is running.");

    }

    private void forecast(double latitude, double longitude) {
        String apiKey = "e17e17602b10d445c854bc3d1f9a4780";
        String forecastUrl = "https://api.darksky.net/forecast/" + apiKey +
                "/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
//            Log.v(TAG, "1st");
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(forecastUrl).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);

                        if (response.isSuccessful()) {
                            currentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if (progressBar.getVisibility() == View.VISIBLE)
            progressBar.setVisibility(View.INVISIBLE);
        else
            progressBar.setVisibility(View.VISIBLE);

        if (refresh.getVisibility() == View.VISIBLE)
            refresh.setVisibility(View.INVISIBLE);
        else
            refresh.setVisibility(View.VISIBLE);
    }

    private void updateDisplay() {
        temperatureLabel.setText(currentWeather.getTemperature() + "");
        mTimeLabel.setText("At " + currentWeather.getFormattedTime() + " it will be");
        humidityValue.setText(currentWeather.getHumidity() + "");
        precipValue.setText(currentWeather.getPrecipChance() + "%");
        summaryLabel.setText(currentWeather.getSummary());

        Drawable drawable = getResources().getDrawable(currentWeather.getIconId());
//        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.clear_day);
        Log.d(TAG, currentWeather.getIconId() + "icon");
        iconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.v(TAG, "From json: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setTimeZone(timezone);

        Log.d(TAG, currentWeather.getFormattedTime());
        return currentWeather;
    }

    private boolean isNetworkAvailable() {
//        Log.v(TAG, "2nd");
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isAvailable())
            isAvailable = true;
        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }
}

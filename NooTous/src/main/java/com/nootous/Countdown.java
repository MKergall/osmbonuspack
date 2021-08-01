package com.nootous;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nootous.databinding.CountdownBinding;

import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Countdown extends Fragment implements LocationListener {

    private CountdownBinding mBinding;
    private boolean mIsSharing;
    private int mCountdown;
    private float mAzimuthAngleSpeed = 0.0f;
    private Activity mActivity;
    protected LocationManager mLocationManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        mActivity = getActivity();
        checkPermissions();

        //Get initial location:
        mLocationManager = (LocationManager)mActivity.getSystemService(mActivity.LOCATION_SERVICE);
        Location location = null;
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null)
                location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (location != null) {
            //location known:
            onLocationChanged(location);
        }

        mBinding = CountdownBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String nickname = "NooTous";
        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "");
        String message = "";
        new StartSharingTask().execute(nickname, groupName, message);

        mBinding.textviewGroupname.setText(groupName);
    }

    @Override public void onDestroyView() {
        new StopSharingTask().execute();
        super.onDestroyView();
        mBinding = null;
    }

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissions.isEmpty()) {
            String[] params = permissions.toArray(new String[permissions.size()]);
            ActivityCompat.requestPermissions(mActivity, params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }

    //--------------------------------------------

    protected static final String NAV_SERVER_URL = "http://comob.free.fr/sharing/";

    public String getUniqueId() {
        return Settings.Secure.getString(mActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    //Start Sharing
    String callStartSharing(String nickname, String group, String message) {
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstart.php?"
                    + "nickname=" + URLEncoder.encode(nickname, "UTF-8")
                    + "&group_id=" + URLEncoder.encode(group, "UTF-8")
                    + "&user_id=" + URLEncoder.encode(getUniqueId(), "UTF-8")
                    + "&message=" + URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        String result = BonusPackHelper.requestStringFromUrl(url);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    private class StartSharingTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return callStartSharing(params[0], params[1], params[2]);
        }

        @Override
        protected void onPostExecute(String error) {
            if (error == null) {
                startSharingTimer();
                mIsSharing = true;
            } else {
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Update Sharing
    private static final int TICK_INTERVAL = 1000; //one tick every second
    private static final int SHARING_INTERVAL = 30; //update every 30 sec
    private Handler mSharingHandler;
    private Runnable mSharingRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateSharingTask().execute();
            mSharingHandler.postDelayed(this, TICK_INTERVAL);
        }
    };

    void startSharingTimer() {
        mSharingHandler = new Handler();
        mSharingHandler.postDelayed(mSharingRunnable, 0);
    }

    void stopSharingTimer() {
        if (mSharingHandler != null) {
            mSharingHandler.removeCallbacks(mSharingRunnable);
        }
    }

    String callUpdateSharing() {
        GeoPoint myPosition = mCurrentLocation;
        int hasLocation = (myPosition != null ? 1 : 0);
        if (myPosition == null)
            myPosition = new GeoPoint(0.0, 0.0);
        String url = null;
        try {
            url = NAV_SERVER_URL + "nupdate.php?"
                    + "user_id=" + URLEncoder.encode(getUniqueId(), "UTF-8")
                    + "&has_location=" + hasLocation
                    + "&lat=" + myPosition.getLatitude()
                    + "&lon=" + myPosition.getLongitude()
                    + "&bearing=" + mAzimuthAngleSpeed;
        } catch (UnsupportedEncodingException e) {
            return "Technical error with the server";
        }
        Log.d(BonusPackHelper.LOG_TAG, "callUpdateSharing:" + url);
        String result = BonusPackHelper.requestStringFromUrl(url);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            mCountdown = jResult.get("countdown").getAsInt();
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    int mTick = 0;

    private class UpdateSharingTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if (mTick == 0) {
                mTick = SHARING_INTERVAL;
                return callUpdateSharing();
            } else {
                mTick--;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String error) {
            mBinding.timer.setText(String.valueOf(mTick));
            if (error == null) {
                mBinding.textviewCountdown.setText(String.valueOf(mCountdown));
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    //Stop Sharing
    String callStopSharing() {
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstop.php?"
                    + "user_id=" + URLEncoder.encode(getUniqueId(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "Technical error with the server";
        }
        String result = BonusPackHelper.requestStringFromUrl(url);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    private class StopSharingTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            //return callStopSharing();
            return null;
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                stopSharingTimer();
                mIsSharing = false;
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    //------------ LocationListener implementation
    private final NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();
    long mLastTime = 0; // milliseconds
    //double mSpeed = 0.0; // km/h
    GeoPoint mCurrentLocation = null;

    @Override public void onLocationChanged(final Location pLoc) {
        long currentTime = System.currentTimeMillis();
        if (mIgnorer.shouldIgnore(pLoc.getProvider(), currentTime))
            return;
        double dT = currentTime - mLastTime;
        if (dT < 100.0) {
            return;
        }
        mLastTime = currentTime;

        mCurrentLocation = new GeoPoint(pLoc);

        if (pLoc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            //mSpeed = pLoc.getSpeed() * 3.6;
            mAzimuthAngleSpeed = pLoc.getBearing();
        }
    }

    @Override public void onProviderDisabled(String provider) {}

    @Override public void onProviderEnabled(String provider) {}

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

}
package com.nootous;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nootous.databinding.CountdownBinding;

import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Countdown extends Fragment {

    private CountdownBinding binding;
    private boolean mIsSharing;
    private int mCountdown;
    private float mAzimuthAngleSpeed = 0.0f;
    private Activity mActivity;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        mActivity = getActivity();
        binding = CountdownBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String nickname = "NooTous";
        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "");
        String message = "";
        new StartSharingTask().execute(nickname, groupName, message);
    }

    @Override public void onDestroyView() {
        new StopSharingTask().execute();
        super.onDestroyView();
        binding = null;
    }

    //--------------------------------------------

    protected static final String NAV_SERVER_URL = "http://comob.free.fr/sharing/";

    public String getUniqueId() {
        return Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

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
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
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
        @Override protected String doInBackground(String... params) {
            return callStartSharing(params[0], params[1], params[2]);
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                startSharingTimer();
                mIsSharing = true;
            } else {
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static final int SHARING_INTERVAL = 30 * 1000; //every 30 sec
    private Handler mSharingHandler;
    private Runnable mSharingRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateSharingTask().execute();
            mSharingHandler.postDelayed(this, SHARING_INTERVAL);
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
        GeoPoint myPosition = null; //mActivity.myLocationOverlay.getLocation();
        int hasLocation = (myPosition != null ? 1 : 0);
        if (myPosition == null)
            myPosition = new GeoPoint(0.0, 0.0);
        String url = null;
        try {
            url = NAV_SERVER_URL + "jupdate.php?"
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
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            JsonArray jFriends = jResult.get("people").getAsJsonArray();
            mCountdown = jFriends.size()+1;//+1 because myself is not listed in friends
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    private class UpdateSharingTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            return callUpdateSharing();
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                binding.textviewCountdown.setText(String.valueOf(mCountdown));
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

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
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
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
            return callStopSharing();
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                stopSharingTimer();
                mIsSharing = false;
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }
}

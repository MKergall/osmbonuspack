package com.nootous;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nootous.databinding.CountdownBinding;

import org.osmdroid.bonuspack.sharing.Friends;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class Countdown extends Fragment implements LocationListener {

    private CountdownBinding mBinding;
    private int mCountdown;
    private String mMessage;
    private MainActivity mActivity;
    protected LocationManager mLocationManager;
    protected Friends mFriends;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        mActivity = (MainActivity)getActivity();
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

        mFriends = new Friends();

        mBinding = CountdownBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String nickname = "NooTous";
        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "");
        String message = "";
        new StartSharingTask().execute(nickname, groupName, message);

        mBinding.groupname.setText(groupName);

        mBinding.partner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (mFriends.partners.size()>0) {
                    String url = mFriends.partners.get(0).url;
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            };
        });

        mBinding.buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                NavHostFragment.findNavController(Countdown.this)
                        .navigate(R.id.action_CountFragment_to_MapFragment);
            }
        });

    }

    @Override public void onDestroyView() {
        stopSharingTimer();
        super.onDestroyView();
        mBinding = null;
        mActivity = null;
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

    private class StartSharingTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return mFriends.callStartSharing(mActivity.getUniqueId(), params[0], params[1], params[2]);
        }

        @Override
        protected void onPostExecute(String error) {
            if (mBinding == null) return;
            if (error == null) {
                if (mFriends.partners.size()>0) {
                    SpannableString content = new SpannableString(mFriends.partners.get(0).name);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    mBinding.partner.setText(content);
                }
                startSharingTimer();
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
	int hasLocation = (mActivity.mCurrentLocation == null ? 0 : 1);
	GeoPoint myBlurredPosition = mActivity.getBlurredLocation();
        String url = null;
        try {
            url = Friends.NAV_SERVER_URL + "nupdate.php?"
                    + "user_id=" + URLEncoder.encode(mActivity.getUniqueId(), "UTF-8")
                    + "&has_location=" + hasLocation
                    + "&lat=" + myBlurredPosition.getLatitude()
                    + "&lon=" + myBlurredPosition.getLongitude()
                    + "&bearing=" + mActivity.mAzimuthAngleSpeed;
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
            mMessage = jResult.get("message").getAsString();
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
            if (mBinding == null)
                //too late, not on this fragment anymore
                return;
            mBinding.timer.setText(String.valueOf(mTick));
            if (error == null) {
                DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
                DecimalFormat formatter = new DecimalFormat("###,###,###", symbols);
                String countdown = formatter.format(mCountdown);
                mBinding.countdown.setText(countdown);
                mBinding.message.setText(mMessage);
                if (mMessage.isEmpty())
                    mBinding.pancarte.setVisibility(View.INVISIBLE);
                else
                    mBinding.pancarte.setVisibility(View.VISIBLE);
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    //------------ LocationListener implementation
    private final NetworkLocationIgnorer mIgnorer = new NetworkLocationIgnorer();
    long mLastTime = 0; // milliseconds
    //double mSpeed = 0.0; // km/h

    @Override public void onLocationChanged(final Location pLoc) {
        long currentTime = System.currentTimeMillis();
        if (mIgnorer.shouldIgnore(pLoc.getProvider(), currentTime))
            return;
        double dT = currentTime - mLastTime;
        if (dT < 100.0) {
            return;
        }
        mLastTime = currentTime;

        mActivity.mCurrentLocation = new GeoPoint(pLoc);

        if (pLoc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            //mSpeed = pLoc.getSpeed() * 3.6;
            mActivity.mAzimuthAngleSpeed = pLoc.getBearing();
        }
    }

    @Override public void onProviderDisabled(String provider) {}

    @Override public void onProviderEnabled(String provider) {}

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

}

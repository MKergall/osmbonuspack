package com.osmnavigator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.bonuspack.utils.StatusException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author M.Kergall
 */
public class FriendsManager {

    MapActivity mActivity;
    MapView mMap;

    protected static final int START_SHARING_REQUEST = 3;
    protected static final int FRIENDS_REQUEST = 4;

    Button mFriendsButton;
    protected static ArrayList<Friend> mFriends; //
    protected boolean mIsSharing;
    protected FolderOverlay mFriendsMarkers; //

    FriendsManager(MapActivity activity, MapView map) {
        mActivity = activity;
        mMap = map;
    }

    void onCreate(Bundle savedInstanceState) {
        mFriendsMarkers = new FolderOverlay(mActivity);
        mMap.getOverlays().add(mFriendsMarkers);
        if (savedInstanceState != null) {
            //STATIC mFriends = savedInstanceState.getParcelable("friends");
            mIsSharing = savedInstanceState.getBoolean("is_sharing");
            updateUIWithFriendsMarkers();
        } else {
            mFriends = null;
            mIsSharing = false;
        }
        mFriendsButton = (Button) mActivity.findViewById(R.id.buttonFriends);
        mFriendsButton.setVisibility(mIsSharing ? View.VISIBLE : View.GONE);
        mFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mIsSharing) {
                    Intent myIntent = new Intent(view.getContext(), FriendsActivity.class);
                    myIntent.putExtra("ID", MapActivity.getIndexOfBubbledMarker(mFriendsMarkers.getItems()));
                    mActivity.startActivityForResult(myIntent, FRIENDS_REQUEST);
                }
            }
        });
    }

    void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("is_sharing", mIsSharing);
    }

    void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case START_SHARING_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    String login = intent.getStringExtra("NICKNAME");
                    String group = intent.getStringExtra("GROUP");
                    String message = intent.getStringExtra("MESSAGE");
                    new StartSharingTask().execute(login, group, message);
                }
                break;
            case FRIENDS_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    int id = intent.getIntExtra("ID", 0);
                    Friend selected = mFriends.get(id);
                    if (selected.mHasLocation) {
                        mMap.getController().setCenter(selected.mPosition);
                        Marker friendMarker = (Marker) mFriendsMarkers.getItems().get(id);
                        friendMarker.showInfoWindow();
                    }
                }
                break;
        }
    }

    void onResume() {
        if (mIsSharing)
            startSharingTimer();
    }

    void onPause() {
        stopSharingTimer();
    }

    void onPrepareOptionsMenu(Menu menu) {
        if (mIsSharing)
            menu.findItem(R.id.menu_sharing).setTitle(R.string.menu_stop_sharing);
        else
            menu.findItem(R.id.menu_sharing).setTitle(R.string.menu_start_sharing);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent;
        switch (item.getItemId()) {
            case R.id.menu_sharing:
                if (!mIsSharing) {
                    myIntent = new Intent(mActivity, StartSharingActivity.class);
                    mActivity.startActivityForResult(myIntent, START_SHARING_REQUEST);
                } else {
                    new StopSharingTask().execute();
                }
        }
        return true;
    }

    //--------------------------------------------

    static final String NAV_SERVER_URL = "http://comob.free.fr/sharing/";

    public String getUniqueId() {
        return Settings.Secure.getString(mActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    String callStartSharing(String nickname, String group, String message) {
        //List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstart.php?"
                    + "nickname=" + URLEncoder.encode(nickname, "UTF-8")
                    + "&group_id=" + URLEncoder.encode(group, "UTF-8")
                    + "&user_id=" + URLEncoder.encode(getUniqueId(), "UTF-8")
                    + "&message=" + URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        /*
		nameValuePairs.add(new BasicNameValuePair("nickname", nickname));
		nameValuePairs.add(new BasicNameValuePair("group_id", group));
		nameValuePairs.add(new BasicNameValuePair("user_id", getUniqueId()));
		nameValuePairs.add(new BasicNameValuePair("message", message));
		String result = BonusPackHelper.requestStringFromPost(url, nameValuePairs);
		*/
        String result;
        try {
            result = BonusPackHelper.requestStringFromUrl(url);
        } catch (IOException e) {
            return "Problem with server communication, check internet connection";
        } catch (StatusException e) {
            return "Problem with server communication httpStatusCode:"+e.getHttpStatusCode();
        }
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                String error = jResult.get("error").getAsString();
                return error;
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
                mFriendsButton.setVisibility(mIsSharing ? View.VISIBLE : View.GONE);
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    static final int SHARING_INTERVAL = 20 * 1000; //every 20 sec
    protected Handler mSharingHandler;
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
        mFriends = null;
        GeoPoint myPosition = mActivity.myLocationOverlay.getLocation();
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
                    + "&bearing=" + mActivity.mAzimuthAngleSpeed;
        } catch (UnsupportedEncodingException e) {
        }
        Log.d(BonusPackHelper.LOG_TAG, "callUpdateSharing:" + url);
        String result;
        try {
            result = BonusPackHelper.requestStringFromUrl(url);
        } catch (IOException e) {
            return "Problem with server communication, check internet connection";
        } catch (StatusException e) {
            return "Problem with server communication httpStatusCode:"+e.getHttpStatusCode();
        }
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                String error = jResult.get("error").getAsString();
                return error;
            }
            JsonArray jFriends = jResult.get("people").getAsJsonArray();
            mFriends = new ArrayList<Friend>(jFriends.size());
            for (JsonElement jFriend : jFriends) {
                JsonObject joFriend = (JsonObject) jFriend;
                Friend friend = new Friend(joFriend);
                mFriends.add(friend);
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    int getOpenedInfoWindow(FolderOverlay folder) {
        List<Overlay> items = mFriendsMarkers.getItems();
        for (int i = 0; i < items.size(); i++) {
            Overlay overlay = items.get(i);
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                if (m.isInfoWindowShown())
                    return i;
            }
        }
        return -1;
    }

    void updateUIWithFriendsMarkers() {
        int opened = getOpenedInfoWindow(mFriendsMarkers);
        mFriendsMarkers.closeAllInfoWindows();
        mFriendsMarkers.getItems().clear();
        if (mFriends == null) {
            mMap.invalidate();
            return;
        }
        Drawable iconOnline = mActivity.getResources().getDrawable(R.drawable.marker_car_on);
        Drawable iconOffline = mActivity.getResources().getDrawable(R.drawable.marker_friend_off);
        for (Friend friend : mFriends) {
            //MarkerLabeled marker = new MarkerLabeled(map);
            Marker marker = new Marker(mMap);
            marker.setPosition(friend.mPosition);
            marker.setTitle(friend.mNickName);
            marker.setSnippet(friend.mMessage);
            if (friend.mOnline) {
                marker.setIcon(iconOnline); //((BitmapDrawable) iconOnline).getBitmap());
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setRotation(friend.mBearing);
            } else {
                marker.setIcon(iconOffline); //((BitmapDrawable)iconOffline).getBitmap());
            }
            if (!friend.mHasLocation)
                marker.setEnabled(false);
            mFriendsMarkers.add(marker);
        }
        mMap.invalidate();
        if (opened != -1 && opened < mFriends.size()) {
            //TODO - completely insufficient, as index may have changed.
            Marker markerToOpen = (Marker) mFriendsMarkers.getItems().get(opened);
            markerToOpen.showInfoWindow();
        }
    }

    private class UpdateSharingTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return callUpdateSharing();
        }

        @Override
        protected void onPostExecute(String error) {
            if (error == null) {
                updateUIWithFriendsMarkers();
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    String callStopSharing() {
        mFriends = null;
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstop.php?"
                    + "user_id=" + URLEncoder.encode(getUniqueId(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        String result;
        try {
            result = BonusPackHelper.requestStringFromUrl(url);
        } catch (IOException e) {
            return "Problem with server communication, check internet connection";
        } catch (StatusException e) {
            return "Problem with server communication httpStatusCode:"+e.getHttpStatusCode();
        }
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                String error = jResult.get("error").getAsString();
                return error;
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    private class StopSharingTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return callStopSharing();
        }

        @Override
        protected void onPostExecute(String error) {
            if (error == null) {
                updateUIWithFriendsMarkers();
                stopSharingTimer();
                mIsSharing = false;
                mFriendsButton.setVisibility(mIsSharing ? View.VISIBLE : View.GONE);
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }
}

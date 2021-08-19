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

import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.sharing.Friend;
import org.osmdroid.bonuspack.sharing.Friends;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * @author M.Kergall
 */
public class FriendsManager {

    private MapActivity mActivity;
    private MapView mMap;

    public static final int START_SHARING_REQUEST = 3;
    public static final int FRIENDS_REQUEST = 4;

    private Button mFriendsButton;
    private static Friends mFriends; //static to keep it between activities
    private boolean mIsSharing;
    private RadiusMarkerClusterer mFriendsMarkers; //
    private boolean mRecordTracks;

    public FriendsManager(MapActivity activity, MapView map) {
        mActivity = activity;
        mMap = map;
        mRecordTracks = false;
        mFriends = new Friends(BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME);
    }

    public static ArrayList<Friend> getFriends(){
        return mFriends.friendsList;
    }

    public void onCreate(Bundle savedInstanceState) {
        mFriendsMarkers = new RadiusMarkerClusterer(mActivity);
        mMap.getOverlays().add(mFriendsMarkers);
        if (savedInstanceState != null) {
            //STATIC mFriends = savedInstanceState.getParcelable("friends");
            mIsSharing = savedInstanceState.getBoolean("is_sharing");
            updateUIWithFriendsMarkers();
        } else {
            mFriends = new Friends(BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME);
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

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("is_sharing", mIsSharing);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
                    int index = intent.getIntExtra("ID", 0);
                    Friend selected = mFriends.get(index);
                    if (selected.mHasLocation) {
                        mMap.getController().setCenter(selected.mPosition);
                        Marker friendMarker = (Marker) mFriendsMarkers.getItems().get(index);
                        friendMarker.showInfoWindow();
                    }
                }
                break;
        }
    }

    public void onResume() {
        if (mIsSharing)
            startSharingTimer();
    }

    public void onPause() {
        stopSharingTimer();
    }

    public void onPrepareOptionsMenu(Menu menu) {
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

    public String getUniqueId(Activity activity) {
        return Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    //--------------------------------------------

    private class StartSharingTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            return mFriends.callStartSharing(getUniqueId(mActivity), params[0], params[1], params[2]);
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                startSharingTimer();
                mIsSharing = true;
                mFriendsButton.setVisibility(View.VISIBLE);
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }

    private static final long SHARING_INTERVAL_SHORT = 20 * 1000; //every 20 sec
    private static final long SHARING_INTERVAL_LONG = 60 * 1000; //every minute
    private Handler mSharingHandler;

    private Runnable mSharingRunnable = new Runnable() {
        @Override
        public void run() {
            new UpdateSharingTask().execute();
            //adjust update frequency to the size of the group:
            int n = (mFriends.friendsList == null ? 0 : mFriends.size());
            long sharingInterval = (n<100 ? SHARING_INTERVAL_SHORT : SHARING_INTERVAL_LONG);
            //request next update:
            mSharingHandler.postDelayed(this, sharingInterval);
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

    int getOpenedInfoWindow(RadiusMarkerClusterer folder) {
        ArrayList<Marker> items = mFriendsMarkers.getItems();
        for (int i = 0; i < items.size(); i++) {
            Marker m = items.get(i);
            if (m.isInfoWindowShown())
                    return i;
        }
        return -1;
    }

    public void setTracksRecording(boolean recordTracks){
        mRecordTracks = recordTracks;
    }

    public void recordTracks(){
        for (Friend friend : mFriends.friendsList) {
            if (friend.mHasLocation)
                mActivity.recordCurrentLocationInTrack(friend.mId, friend.mNickName, friend.mPosition);
        }
    }

    void updateUIWithFriendsMarkers() {
        //retrieve the id of the "opened" friend (friend with opened bubble):
        int opened = getOpenedInfoWindow(mFriendsMarkers);
        String openedFriendId = null;
        if (opened != -1) {
            Marker m = (Marker)mFriendsMarkers.getItems().get(opened);
            Friend openedFriend = (Friend)m.getRelatedObject();
            openedFriendId = openedFriend.mId;
        }

        //mFriendsMarkers.closeAllInfoWindows(); TODO
        mFriendsMarkers.getItems().clear();
        if (mFriends.friendsList == null) {
            mMap.invalidate();
            return;
        }
        Drawable iconOnline = mActivity.getResources().getDrawable(R.drawable.marker_car_on);
        Drawable iconOffline = mActivity.getResources().getDrawable(R.drawable.marker_friend_off);
        for (Friend friend : mFriends.friendsList) {
            //MarkerLabeled marker = new MarkerLabeled(map);
            Marker marker = new Marker(mMap);
            try {
                marker.setPosition(friend.mPosition);
            } catch (Exception IllegalArgumentException) {
                marker.setPosition(new GeoPoint(0.0,0.0,0.0));
            }
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
            marker.setRelatedObject(friend);
            mFriendsMarkers.add(marker);
        }
        mMap.invalidate();

        //reopen the bubble on the "same" (but new) Friend marker:
        opened = mFriends.getFriendWithId(openedFriendId);
        if (opened != -1) {
            Marker markerToOpen = (Marker) mFriendsMarkers.getItems().get(opened);
            markerToOpen.showInfoWindow();
        }
    }

    private class UpdateSharingTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            GeoPoint myPosition = mActivity.myLocationOverlay.getLocation();
            return mFriends.callUpdateSharing(getUniqueId(mActivity), myPosition, mActivity.mAzimuthAngleSpeed);
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                updateUIWithFriendsMarkers();
                if (mRecordTracks)
                    recordTracks();
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }


    private class StopSharingTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            return mFriends.callStopSharing(getUniqueId(mActivity));
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                updateUIWithFriendsMarkers();
                stopSharingTimer();
                mIsSharing = false;
                mFriendsButton.setVisibility(View.GONE);
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }
}

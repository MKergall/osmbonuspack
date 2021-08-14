package com.nootous;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.nootous.databinding.MapBinding;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.sharing.Friend;
import org.osmdroid.bonuspack.sharing.Friends;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;


public class Map extends Fragment {

    private MapBinding mBinding;
    private MainActivity mActivity;
    MapView mMap;
    Friends mFriends;
    private RadiusMarkerClusterer mFriendsMarkers;
    protected DirectedLocationOverlay myLocationOverlay;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = (MainActivity)getActivity();
        mBinding = MapBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID+"/"+BuildConfig.VERSION_NAME);
        mMap = (MapView) mActivity.findViewById(R.id.map);
        mMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mMap.setMultiTouchControls(true);
        GeoPoint startPoint;
        if (mActivity.mCurrentLocation != null)
            startPoint = mActivity.mCurrentLocation;
        else
            startPoint = new GeoPoint(48.13, 1.2);
        IMapController mapController = mMap.getController();
        mapController.setZoom(8.0);
        mapController.setCenter(startPoint);

        mFriendsMarkers = new RadiusMarkerClusterer(mActivity);
        mMap.getOverlays().add(mFriendsMarkers);

        myLocationOverlay = new DirectedLocationOverlay(mActivity);
        if (mActivity.mCurrentLocation != null){
            myLocationOverlay.setLocation(mActivity.mCurrentLocation);
            myLocationOverlay.setBearing(mActivity.mAzimuthAngleSpeed);
        } else {
            myLocationOverlay.setEnabled(false);
        }
        mMap.getOverlays().add(myLocationOverlay);

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mMap);
        mMap.getOverlays().add(scaleBarOverlay);

        mFriends = new Friends();
        new UpdateSharingTask().execute();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
        mActivity = null;
    }

    //------------

    void updateUIWithFriendsMarkers() {
        mFriendsMarkers.getItems().clear();
        if (mFriends.friendsList == null) {
            mMap.invalidate();
            return;
        }
        Drawable icon = mActivity.getResources().getDrawable(R.drawable.marker_friend_off);
        for (Friend friend : mFriends.friendsList) {
            if (!friend.mHasLocation ||
                    (friend.mPosition.getLatitude() == 0.0 && friend.mPosition.getLongitude() == 0.0))
                //some filtering
                continue;
            Marker marker = new Marker(mMap);
            try {
                marker.setPosition(friend.mPosition);
            } catch (Exception IllegalArgumentException) {
                marker.setPosition(new GeoPoint(0.0, 0.0, 0.0));
            }
            marker.setTitle(friend.mNickName);
            marker.setSnippet(friend.mMessage);
            marker.setIcon(icon); //((BitmapDrawable) iconOnline).getBitmap());
            //marker.setRelatedObject(friend);
            mFriendsMarkers.add(marker);
        }

        BoundingBox bb = mFriendsMarkers.getBounds();
        if (bb != null)
            mMap.zoomToBoundingBox(bb, true);
    }

    private class UpdateSharingTask extends AsyncTask<Void, Void, String> {
        @Override protected String doInBackground(Void... params) {
            GeoPoint myPosition = mActivity.mCurrentLocation;
            return mFriends.callUpdateSharing(mActivity.getUniqueId(), myPosition, mActivity.mAzimuthAngleSpeed);
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                updateUIWithFriendsMarkers();
            } else
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
        }
    }
}
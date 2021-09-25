package com.nootous;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nootous.databinding.ActivityGroupBinding;
import org.osmdroid.bonuspack.sharing.Friends;
import org.osmdroid.bonuspack.sharing.Partner;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityGroupBinding mBinding;

    //data common to all fragments:
    public List<Trend> mTrends = new ArrayList<>(0);
    public Partner mPartner;
    public GeoPoint mCurrentLocation = null;
    public float mAzimuthAngleSpeed = 0.0f;
    protected double mBlurredDistance, mBlurredBearing; //to blur a little bit my position

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_group);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        initBlurring(100.0);
    }

    @Override public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_group);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_group, menu);
        return true;
    }

    private static String eventManagementUrl = "https://comob.org/NooTous/partner.html";

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_event_management:
                String groupName = getSharedPreferences("NOOTOUS", Context.MODE_PRIVATE).getString("GROUP_NAME", "#");
                String url = eventManagementUrl + "?group_id=" + groupName;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //----------------------

    @Override protected void onDestroy() {
        super.onDestroy();
        mTrends = null;
    }

    /**
     * @param distanceRange offset by distanceRange to 2*distanceRange (in meters)
     */
    public void initBlurring(double distanceRange){
        mBlurredDistance = distanceRange + Math.random()*distanceRange;
        mBlurredBearing = Math.random()*360.0; //in any direction
    }

    public GeoPoint getBlurredLocation(){
	  if (mCurrentLocation == null)
		return new GeoPoint(0.0, 0.0);
	  else
		return mCurrentLocation.destinationPoint(mBlurredDistance, mBlurredBearing);
    }

    public String getUniqueId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}

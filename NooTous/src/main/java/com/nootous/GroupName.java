package com.nootous;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nootous.databinding.GroupNameBinding;

import org.osmdroid.bonuspack.utils.BonusPackHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import static android.widget.Toast.makeText;

public class GroupName extends Fragment {

    private GroupNameBinding mBinding;
    private MainActivity mActivity;
    GetTrendTask mGetTrendTask;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = (MainActivity)getActivity();
        mBinding = GroupNameBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "#");
        mBinding.groupName.setText(groupName);
        mBinding.groupName.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (mActivity.mTrends != null && mActivity.mTrends.length > 0)
                    mBinding.groupName.showDropDown();
            }
        });

        mBinding.buttonNext.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                String groupName = mBinding.groupName.getText().toString();
                SharedPreferences prefs = getActivity().getSharedPreferences("NOOTOUS", getActivity().MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString("GROUP_NAME", groupName);
                ed.apply();
                NavHostFragment.findNavController(GroupName.this)
                        .navigate(R.id.action_GroupFragment_to_CountFragment);
            }
        });

        if (mActivity.mTrends == null) {
            mGetTrendTask = new GetTrendTask();
            mGetTrendTask.execute();
        }
    }

    @Override public void onDestroyView() {
        mGetTrendTask.cancel(true);
        super.onDestroyView();
        mBinding = null;
        mActivity = null;
    }

    //------------- Trends
    protected static final String NAV_SERVER_URL = "https://comob.org/sharing/";

    String getTrends() {
        String url = NAV_SERVER_URL + "jtrends.php";
        String result = BonusPackHelper.requestStringFromUrl(url);
        if (result == null) {
            return "Technical error with the server";
        }
        Log.d(BonusPackHelper.LOG_TAG, "getTrends:" + url);
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            JsonArray jTrends = jResult.get("trends").getAsJsonArray();
            mActivity.mTrends = new String[jTrends.size()];
            int i = 0;
            for (JsonElement jPartner:jTrends){
                JsonObject jPO = jPartner.getAsJsonObject();
                String trend = jPO.get("group_id").getAsString();
                mActivity.mTrends[i] = trend;
                i++;
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    private class GetTrendTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            return getTrends();
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                if (mActivity.mTrends.length > 0) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>
                            (mActivity, android.R.layout.simple_list_item_1, mActivity.mTrends);
                    mBinding.groupName.setAdapter(adapter);
                }
            } else {
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
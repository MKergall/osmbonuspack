package com.nootous;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.nootous.databinding.GroupNameBinding;

import java.util.ArrayList;

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
        mBinding.groupName.setThreshold(1);

        mBinding.trends.setOnClickListener(new OnClickListener() {
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
    /*
    public class Trend {
        public String name;
        public int count;
    }
    */

    private class GetTrendTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            return mActivity.getTrends();
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
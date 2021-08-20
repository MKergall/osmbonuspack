package com.nootous;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.nootous.databinding.GroupNameBinding;

import java.util.ArrayList;
import java.util.List;

public class GroupName extends Fragment {

    private GroupNameBinding mBinding;
    private MainActivity mActivity;
    GetTrendTask mGetTrendTask;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = (MainActivity)getActivity();
        mBinding = GroupNameBinding.inflate(inflater, container, false);

        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "#");
        mBinding.groupName.setText(groupName);

        TrendAdapter adapter = new TrendAdapter(
                getContext(), R.layout.group_name, R.id.lbl_name, mActivity.mTrends);
        mBinding.groupName.setAdapter(adapter);

        mBinding.trends.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (mActivity.mTrends.size() > 0)
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

        if (mActivity.mTrends.size()==0) {
            mBinding.trends.setVisibility(View.INVISIBLE);
            //load only if not already loaded - test is not perfect...
            mGetTrendTask = new GetTrendTask();
            mGetTrendTask.execute();
        } else
            mBinding.trends.setVisibility(View.VISIBLE);

        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override public void onDestroyView() {
        mGetTrendTask.cancel(true);
        super.onDestroyView();
        mBinding = null;
        mActivity = null;
    }

    //------------- TrendAdapter

    public class TrendAdapter extends ArrayAdapter<Trend> {
        Context context;
        int resource, textViewResourceId;
        List<Trend> items, tempItems, suggestions;

        public TrendAdapter(Context context, int resource, int textViewResourceId, List<Trend> items) {
            super(context, resource, textViewResourceId, items);
            this.context = context;
            this.items = items;
            this.resource = resource;
            this.textViewResourceId = textViewResourceId;
            tempItems = new ArrayList<Trend>(items);
            suggestions = new ArrayList<Trend>();
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.trend_row, parent, false);
            }
            Trend trend = items.get(position);
            if (trend != null) {
                TextView lblName = (TextView) view.findViewById(R.id.lbl_name);
                lblName.setText(trend.name);
                TextView lblCount = (TextView) view.findViewById(R.id.lbl_count);
                lblCount.setText(trend.displayedCount());
            }
            return view;
        }

        @Override public Filter getFilter() {
            return nameFilter;
        }

        Filter nameFilter = new Filter() {
            @Override public CharSequence convertResultToString(Object resultValue) {
                String str = ((Trend) resultValue).name;
                return str;
            }

            @Override protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    suggestions.clear();
                    for (Trend trend : tempItems) {
                        if (trend.name.contains(constraint.toString())) {
                            suggestions.add(trend);
                        }
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();
                    return filterResults;
                } else {
                    return new FilterResults();
                }
            }

            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                List<Trend> filterList = (ArrayList<Trend>) results.values;
                if (results != null && results.count > 0) {
                    clear();
                    for (Trend trend : filterList) {
                        add(trend);
                        notifyDataSetChanged();
                    }
                }
            }
        };
    }

    //---------------

    private class GetTrendTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            return Trend.getTrends(mActivity.mTrends);
        }

        @Override protected void onPostExecute(String error) {
            if (error == null) {
                //ready to use:
                mBinding.trends.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(mActivity.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
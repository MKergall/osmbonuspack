package com.nootous;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.nootous.databinding.GroupNameBinding;

import static android.widget.Toast.makeText;

public class GroupName extends Fragment {

    private GroupNameBinding binding;
    private Activity mActivity;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        mActivity = getActivity();
        binding = GroupNameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String groupName = mActivity.getSharedPreferences("NOOTOUS", mActivity.MODE_PRIVATE).getString("GROUP_NAME", "");
        binding.groupName.setText(groupName);

        binding.buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String groupName = binding.groupName.getText().toString();
                SharedPreferences prefs = getActivity().getSharedPreferences("NOOTOUS", getActivity().MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString("GROUP_NAME", groupName);
                ed.apply();
                NavHostFragment.findNavController(GroupName.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
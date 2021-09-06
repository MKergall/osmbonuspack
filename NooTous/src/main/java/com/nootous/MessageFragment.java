package com.nootous;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.nootous.databinding.MessageBinding;

public class MessageFragment extends Fragment {
    private MessageBinding mBinding;

    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mBinding = MessageBinding.inflate(inflater, container, false);

        mBinding.buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                NavHostFragment.findNavController(MessageFragment.this)
                        .navigate(R.id.action_MessageFragment_to_GroupFragment);
            }
        });

        return mBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}

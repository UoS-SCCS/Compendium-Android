package com.castellate.compendium.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.transition.TransitionInflater;

import com.castellate.compendium.R;
import com.castellate.compendium.databinding.HomeScreenBinding;

public class HomeScreen extends Fragment {

    private HomeScreenBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = HomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonEnrol.setOnClickListener(enrolView -> NavHostFragment.findNavController(HomeScreen.this)
                .navigate(R.id.action_HomeFragment_to_QRCodeFragment));
        binding.buttonNotifications.setOnClickListener(notificationView -> NavHostFragment.findNavController(HomeScreen.this)
                .navigate(R.id.action_HomeFragment_to_itemFragment));
        binding.buttonKeymanager.setOnClickListener(keyManagerView -> NavHostFragment.findNavController(HomeScreen.this)
                .navigate(R.id.action_HomeFragment_to_itemFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
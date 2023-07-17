package com.cads.projectplanmyday.Fragments;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cads.projectplanmyday.ActivityViewModel;
import com.cads.projectplanmyday.databinding.FragmentCalendarDisplayBinding;
import com.cads.projectplanmyday.databinding.FragmentGenerationBinding;
import com.cads.projectplanmyday.databinding.FragmentOpenaiResponseBinding;

public class GenerationFragment extends Fragment {
    private FragmentGenerationBinding binding;
    ActivityViewModel viewModel;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding= FragmentGenerationBinding.inflate(inflater,container,false);
        getActivity().setTitle("Error");
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);
        // Inflate the layout for this fragment
        viewModel.userMessage.observe(getViewLifecycleOwner(), userMessage -> {
            binding.generationTv.setText(userMessage);
        });
        return binding.getRoot();
    }
}
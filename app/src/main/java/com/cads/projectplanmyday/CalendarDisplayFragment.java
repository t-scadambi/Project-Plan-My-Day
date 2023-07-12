package com.cads.projectplanmyday;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cads.projectplanmyday.databinding.FragmentCalendarDisplayBinding;

import org.json.JSONObject;

import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.O)
public class CalendarDisplayFragment extends Fragment {

    String TAG = CalendarDisplayFragment.class.getSimpleName();

    RecyclerView recyclerView;
    CalendarResponseAdapter adapter;
    Button nextbtn2;
    ActivityViewModel viewModel;
    FragmentCalendarDisplayBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding= FragmentCalendarDisplayBinding.inflate(inflater,container,false);
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);
        adapter = new CalendarResponseAdapter(viewModel.getEvents());
        initializeUI(binding);
        return binding.getRoot();
    }

    private void initializeUI(@NonNull final FragmentCalendarDisplayBinding binding) {
        nextbtn2 = binding.nextbtn2;
        recyclerView = binding.rvCalendar;
        recyclerView.setAdapter(adapter);
        nextbtn2.setOnClickListener(v->{
            feedOpenAIContent();
        });
    }
    private void feedOpenAIContent() {
        OpenAIHelper openAIHelper = new OpenAIHelper();
        List<TaskPlanner> tasksList = viewModel.getTasks();
        List<CalendarEvent> eventsList =viewModel.getEvents();
        String userContent = openAIHelper.formatUserContent(tasksList,eventsList);
        openAIHelper.getResponse(requireActivity().getApplicationContext(),
                response -> {
                    /* Successfully called graph, process data and send to UI */
                    Log.d(TAG, "Response: " + response.toString());
                    displayResult(response);
                },
                error -> {
                    Log.d(TAG,"error: "+error.toString());
                    displayError(error);
                },userContent);
    }
    private void displayResult(JSONObject response) {
        String responseString ;
        try{
            responseString = response.getJSONArray("choices")
                    .getJSONObject(0).getString("text");
            try {
                // Remove the trailing characters
                int endIndex = responseString.lastIndexOf("```");
                String jsonStr = responseString.substring(0, endIndex);

                // Extract the JSON object from the response string
                JSONObject jsonObject = new JSONObject(jsonStr);

            }catch(Exception e){
                responseString = "error in parsing invalid json object";
            }
        }catch (Exception e){
            responseString = e.toString();
        }
        viewModel.setOpenAIData(responseString);

    }

    private void displayError(Exception error) {
        viewModel.setOpenAIData(error.toString());
    }

}
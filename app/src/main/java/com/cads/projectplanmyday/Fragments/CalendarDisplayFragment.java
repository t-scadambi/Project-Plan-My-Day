package com.cads.projectplanmyday.Fragments;

import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.cads.projectplanmyday.ActivityViewModel;
import com.cads.projectplanmyday.CalendarEvent;
import com.cads.projectplanmyday.CalendarResponseAdapter;
import com.cads.projectplanmyday.timelineview.Event;
import com.cads.projectplanmyday.OpenAIHelper;
import com.cads.projectplanmyday.R;
import com.cads.projectplanmyday.TaskPlanner;
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
    OpenAIHelper openAIHelper;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding= FragmentCalendarDisplayBinding.inflate(inflater,container,false);
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);
        adapter = new CalendarResponseAdapter(viewModel.getEvents());
        openAIHelper = new OpenAIHelper();
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
        ProgressDialog progress = new ProgressDialog(requireContext());
        progress.setTitle("Generating Calendar");
        progress.setMessage("Please Wait...");
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();
        List<TaskPlanner> tasksList = viewModel.getTasks();
        List<CalendarEvent> eventsList =viewModel.getEvents();
        String userContent = openAIHelper.formatUserContent(tasksList,eventsList);
        openAIHelper.getResponse(requireActivity().getApplicationContext(),
                response -> {
                    /* Successfully called graph, process data and send to UI */
                    Log.d(TAG, "Response: " + response.toString());
                    progress.dismiss();

                    displayResult(response);
                },
                error -> {
                    progress.dismiss();
                    viewModel.setUserMessage(error.toString());
                    Log.d(TAG,"error: "+error.toString());
                    displayError(error);
                },userContent);

    }
    private void displayResult(JSONObject response) {
        String responseString ;
        List<Event> eventResponse ;
        try{
            responseString = response.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");
            try {

                // Extract the JSON object from the response string
                JSONObject jsonObject = new JSONObject(responseString);
                Log.d("Response" , jsonObject.toString());
                eventResponse = openAIHelper.formatEventList(jsonObject);

            }catch(Exception e){
                displayError(e);
                return;
            }
        }catch (Exception e){
            displayError(e);
            return;
        }
        Log.d(TAG , eventResponse.toString());
        viewModel.setOpenAIData(eventResponse);
        Navigation.findNavController(getView()).navigate(R.id.action_calendarDisplayFragment_to_openaiResponseFragment);

    }

    private void displayError(Exception error) {
        Log.d(TAG , error.toString());
        Navigation.findNavController(getView()).navigate(R.id.action_calendarDisplayFragment_to_generationFragment);
    }

}
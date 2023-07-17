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

import com.android.volley.Response;
import com.cads.projectplanmyday.ActivityViewModel;
import com.cads.projectplanmyday.CalendarEvent;
import com.cads.projectplanmyday.Event;
import com.cads.projectplanmyday.MSGraphRepository;
import com.cads.projectplanmyday.MainActivity;
import com.cads.projectplanmyday.OpenAIHelper;
import com.cads.projectplanmyday.R;
import com.cads.projectplanmyday.RenderData;
import com.cads.projectplanmyday.TaskPlanner;
import com.cads.projectplanmyday.TaskResponseAdapter;
import com.cads.projectplanmyday.databinding.FragmentTaskDisplayBinding;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;



@RequiresApi(api = Build.VERSION_CODES.O)
public class TaskDisplayFragment extends Fragment {
    private static final String TAG = TaskDisplayFragment.class.getSimpleName();

    RecyclerView recyclerView;
    TaskResponseAdapter adapter;
    Button nxtbtn;
    ActivityViewModel viewModel;

    MSGraphRepository msGraphRepository;
    OpenAIHelper openAIHelper;

    String calendarUrl = "https://graph.microsoft.com/v1.0/me/calendarview";
    String filters = "&$select=id,subject,start,end";
    FragmentTaskDisplayBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentTaskDisplayBinding.inflate(inflater,container,false);
        getActivity().setTitle("Tasks to be done");
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);
        openAIHelper = new OpenAIHelper();
        msGraphRepository = new MSGraphRepository();
        adapter = new TaskResponseAdapter(viewModel.getTasks() , getActivity().getApplicationContext());
        initializeUI(binding);
        return binding.getRoot();
    }

    private void initializeUI(@NonNull final FragmentTaskDisplayBinding binding) {
        nxtbtn = binding.nextbtn;
        recyclerView = binding.recyclerView;
        ISingleAccountPublicClientApplication mSingleAccountApp = viewModel.getSingleAccountAppLiveData().getValue();
        IAccount mAccount = viewModel.getAccountLiveData().getValue();
        recyclerView.setAdapter(adapter);
        nxtbtn.setOnClickListener(v -> {
            if(mSingleAccountApp==null)return;
            List<TaskPlanner> tasksSelection = viewModel.getTasks();
            List<MainActivity.TodoTask> todoTasks = new ArrayList<>();
            for(int i=0;i<tasksSelection.size();i++){
                int isSelected = tasksSelection.get(i).getIsSelected()? 1:0;
                String title = tasksSelection.get(i).getTitle();
                todoTasks.add(new MainActivity.TodoTask(title,isSelected ));
            }
            MainActivity.Companion.writeData(getActivity().getApplicationContext(), todoTasks,"usertaskinfo.txt");
            Log.d("SelectTaskFragment" , viewModel.getTasks().toString());
//            mSingleAccountApp.acquireTokenSilentAsync(getScopes(),mAccount.getAuthority(),getAuthSilentCallback());
            feedOpenAIContent();

        });

    }
    private SilentAuthenticationCallback getAuthSilentCallback(){
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                callGraphAPI(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());
                Navigation.findNavController(getView()).navigate(R.id.action_taskDisplayFragment_to_homepageFragment);
            }
        };
    }
    private void callGraphAPI(final IAuthenticationResult authenticationResult) {
        try {
            // Get current date and time
            LocalDateTime currentDateTime = LocalDateTime.now();

//            LocalTime specificTime = LocalTime.of(20, 30);
//            LocalDateTime endOfDayDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), specificTime);

            // Get end of the day date and time
            LocalDateTime endOfDayDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), LocalTime.from(LocalDateTime.MAX));

            // Define ISO date-time formatter
            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

            // Convert current date and time to ISO format
            String currentDateTimeIso = currentDateTime.format(isoFormatter);

            // Convert end of the day date and time to ISO format
            String endOfDayDateTimeIso = endOfDayDateTime.format(isoFormatter);
            String dateInfo = "?startdatetime="+currentDateTimeIso+"&enddatetime="+endOfDayDateTimeIso;
            Log.d(TAG,"DATE INFO"+dateInfo);
            String requestUrl = calendarUrl + dateInfo + filters;
            msGraphRepository.callGraphAPIUsingVolley(
                    getContext(),
                    requestUrl,
                    authenticationResult.getAccessToken(),
                    (Response.Listener<JSONObject>) response -> {
                        /* Successfully called graph, process data and send to UI */
                        Log.d(TAG, "Response: " + response.toString());
                        displayGraphResult(response);
                    },
                    error -> {
                        Log.d(TAG, "Error: " + error.toString());
                        displayError(error);
                    }
            );
        }catch (Exception e){
            Log.d(TAG,e.toString());
            displayError(e);
        }
    }
    private String[] getScopes() {
        String scope = "User.Read Calendars.Read Calendars.Read.Shared Tasks.Read";
        return scope.toLowerCase().split(" ");
    }
    private void displayGraphResult(@NonNull final JSONObject response) {
        //TODO
        RenderData graphData = new RenderData(response,requireActivity().getApplicationContext());
        List<CalendarEvent> datalist = graphData.renderCalendarInfo();
        viewModel.setEventResponses(datalist);
        Log.d(TAG, datalist.toString());
        Navigation.findNavController(getView()).navigate(R.id.action_taskDisplayFragment_to_calendarDisplayFragment);
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
        Navigation.findNavController(getView()).navigate(R.id.action_taskDisplayFragment_to_openaiResponseFragment);

    }

    private void displayError(@NonNull final Exception exception) {
        //TODO
    }

}
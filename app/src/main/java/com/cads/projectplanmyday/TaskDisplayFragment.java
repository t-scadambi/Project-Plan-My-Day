package com.cads.projectplanmyday;

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
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.O)
public class TaskDisplayFragment extends Fragment {
    private static final String TAG = TaskDisplayFragment.class.getSimpleName();

    RecyclerView recyclerView;
    TaskResponseAdapter adapter;
    Button nxtbtn;
    ActivityViewModel viewModel;

    MSGraphRepository msGraphRepository;

    String calendarUrl = "https://graph.microsoft.com/v1.0/me/calendarview";
    String filters = "&$select=id,subject,start,end";
    FragmentTaskDisplayBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentTaskDisplayBinding.inflate(inflater,container,false);
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);
        msGraphRepository = new MSGraphRepository();
        adapter = new TaskResponseAdapter(viewModel.getTasks());
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
            Log.d("SelectTaskFragment" , viewModel.getTasks().toString());
            mSingleAccountApp.acquireTokenSilentAsync(getScopes(),mAccount.getAuthority(),getAuthSilentCallback());
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
    private void displayError(@NonNull final Exception exception) {
        //TODO
    }

}
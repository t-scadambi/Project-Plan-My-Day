package com.cads.projectplanmyday.Fragments;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.cads.projectplanmyday.ActivityViewModel;
import com.cads.projectplanmyday.CalendarEvent;
import com.cads.projectplanmyday.MSGraphRepository;
import com.cads.projectplanmyday.R;
import com.cads.projectplanmyday.RenderData;
import com.cads.projectplanmyday.TaskPlanner;
import com.cads.projectplanmyday.databinding.FragmentHomepageBinding;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class HomepageFragment extends Fragment {


    private static final String TAG = HomepageFragment.class.getSimpleName();

    FragmentHomepageBinding binding;
    String taskUrl = "https://graph.microsoft.com/v1.0/me/planner/tasks";
    String calendarUrl = "https://graph.microsoft.com/v1.0/me/calendarview";
    String filters = "&$select=id,subject,start,end";

    private ISingleAccountPublicClientApplication mSingleAccountApp;
    private IAccount mAccount;
    private MSGraphRepository msGraphRepository;
    ActivityViewModel viewModel;
    Button signInButton;
    Button signOutButton;
    Button planDayButton;
    ImageButton viewCalendarBtn;
    TextView currentUserTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomepageBinding.inflate(inflater,container,false);
        initializeUI(binding);
        getActivity().setTitle("Home");
        msGraphRepository = new MSGraphRepository();
        PublicClientApplication.createSingleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_single_account,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mSingleAccountApp = application;
                        viewModel.setSingleAccountApp(mSingleAccountApp);
                        loadAccount();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        displayError(exception);
                    }
                }

        );
        return binding.getRoot();
    }
    private void initializeUI(@NonNull final FragmentHomepageBinding binding) {
        signInButton = binding.signInBtn;
        signOutButton = binding.signOutBtn;
        planDayButton = binding.planBtn;
//        currentUserTextView = binding.currentUser;
        viewCalendarBtn = binding.viewCalendarBtn;
//        final String defaultGraphResourceUrl = MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me";

        //Getting an Activity-scoped ViewModel from a Fragment
        viewModel = new ViewModelProvider(requireActivity())
                .get(ActivityViewModel.class);

        signInButton.setOnClickListener(v -> {
            if (mSingleAccountApp == null) {
                return;
            }

            mSingleAccountApp.signIn(getActivity(), null, getScopes(), getAuthInteractiveCallback());
            viewModel.setSingleAccountApp(mSingleAccountApp);
        });

        signOutButton.setOnClickListener(v -> {
            if (mSingleAccountApp == null) {
                return;
            }

            /**
             * Removes the signed-in account and cached tokens from this app (or device, if the device is in shared mode).
             */
            mSingleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                @Override
                public void onSignOut() {
                    mAccount = null;
                    viewModel.setAccount(mAccount);
                    updateUI();
                    showToastOnSignOut();
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    displayError(exception);
                }
            });
            viewModel.setSingleAccountApp(mSingleAccountApp);

        });

        planDayButton.setOnClickListener(v -> {
            if(mSingleAccountApp==null){
                return;
            }
            planDayButton.setEnabled(false);
            Toast.makeText(getContext(),"Getting tasks!",Toast.LENGTH_SHORT).show();
            mSingleAccountApp.acquireTokenSilentAsync(getScopes(),mAccount.getAuthority(),getAuthSilentCallback());
            viewModel.setSingleAccountApp(mSingleAccountApp);
        });

        viewCalendarBtn.setOnClickListener(v-> {
            Navigation.findNavController(getView()).navigate(R.id.action_homepageFragment_to_calendarFragment);
        });
    }
    @Override
    public  void onResume(){
        super.onResume();
        loadAccount();
    }

    private void loadAccount() {
        if (mSingleAccountApp == null) {
            return;
        }
        mSingleAccountApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                mAccount = activeAccount;
                viewModel.setAccount(mAccount);
                updateUI();
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                if (currentAccount == null) {
                    showToastOnSignOut();
                }
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                displayError(exception);
            }
        });

    }

    private AuthenticationCallback getAuthInteractiveCallback(){
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                Log.d(TAG, "User cancelled login.");
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));
                mAccount = authenticationResult.getAccount();
                viewModel.setAccount(mAccount);
                updateUI();

            }

            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());
                displayError(exception);

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }
        };
    }

    private SilentAuthenticationCallback getAuthSilentCallback(){
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                callGraphAPI(authenticationResult);
                callCalendarAPI(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());
                displayError(exception);

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                    showToastOnSignOut();
                }
            }
        };
    }

    private void callGraphAPI(final IAuthenticationResult authenticationResult) {
        msGraphRepository.callGraphAPIUsingVolley(
                getContext(),
                taskUrl,
                authenticationResult.getAccessToken(),
                (Response.Listener<JSONObject>) response -> {
                    /* Successfully called graph, process data and send to UI */
                    Log.d(TAG, "Response: " + response.toString());
                    displayGraphResult(response);
                },
                error ->{
                    Log.d(TAG, "Error: " + error.toString());
                    displayError(error);
                }
        );
    }
    private void callCalendarAPI(final IAuthenticationResult authenticationResult){
        try {
            // Get current date and time
            LocalDateTime currentDateTime = LocalDateTime.now();

//            LocalTime specificTime = LocalTime.of(20, 30);
//            LocalDateTime endOfDayDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), specificTime);

            // Get end of the day date and time
            LocalDateTime endOfDayDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), LocalTime.from(LocalDateTime.MAX));
            LocalDateTime startofTheDay = LocalDateTime.of(currentDateTime.toLocalDate(),LocalTime.from(LocalDateTime.MIN));
            // Define ISO date-time formatter
            DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

            // Convert current date and time to ISO format
            String currentDateTimeIso = currentDateTime.format(isoFormatter);
            String startofTheDayIso = startofTheDay.format(isoFormatter);

            // Convert end of the day date and time to ISO format
            String endOfDayDateTimeIso = endOfDayDateTime.format(isoFormatter);
            String dateInfo = "?startdatetime="+startofTheDayIso+"&enddatetime="+endOfDayDateTimeIso;
            Log.d(TAG,"DATE INFO"+dateInfo);
            String requestUrl = calendarUrl + dateInfo + filters;
            msGraphRepository.callGraphAPIUsingVolley(
                    getContext(),
                    requestUrl,
                    authenticationResult.getAccessToken(),
                    (Response.Listener<JSONObject>) response -> {
                        /* Successfully called graph, process data and send to UI */
                        Log.d(TAG, "Response: " + response.toString());
                        RenderData graphData = new RenderData(response,requireActivity().getApplicationContext());
                        List<CalendarEvent> datalist = graphData.renderCalendarInfo();
                        viewModel.setEventResponses(datalist);
                        Log.d(TAG, datalist.toString());
                    },
                    error -> {
                        Log.d(TAG, "Error: " + error.toString());
//                        displayError(error);
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
        RenderData graphData = new RenderData(response , getActivity().getApplicationContext());
        List<TaskPlanner> datalist =graphData.renderTaskInfo();
        viewModel.setTaskResponses(datalist);
        // TODO navigate to calendar events
        planDayButton.setEnabled(true);
        Navigation.findNavController(getView()).navigate(R.id.action_homepageFragment_to_taskDisplayFragment);
    }

    private void displayError(@NonNull final Exception exception) {
        //TODO
        planDayButton.setEnabled(true);
    }

    private void updateUI() {
        if(mAccount!=null){
            signInButton.setEnabled(false);
            signOutButton.setEnabled(true);
            planDayButton.setEnabled(true);
            viewCalendarBtn.setEnabled(true);
            signInButton.setVisibility(View.INVISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            planDayButton.setVisibility(View.VISIBLE);
            viewCalendarBtn.setVisibility(View.VISIBLE);
//            currentUserTextView.setText(mAccount.getUsername());
        }else{
            signInButton.setEnabled(true);
            signOutButton.setEnabled(false);
            planDayButton.setEnabled(false);
//            currentUserTextView.setText("None");
            viewCalendarBtn.setEnabled(false);

            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.INVISIBLE);
            planDayButton.setVisibility(View.INVISIBLE);
            viewCalendarBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void showToastOnSignOut() {
        final String signOutText = "Signed out";
//        currentUserTextView.setText("");
        Toast.makeText(getContext(),signOutText,Toast.LENGTH_SHORT).show();
    }

}


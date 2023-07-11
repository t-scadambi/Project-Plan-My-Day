package com.cads.projectplanmyday;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;

import java.util.List;

public class ActivityViewModel extends ViewModel {
    private MutableLiveData<ISingleAccountPublicClientApplication> singleAccountAppLiveData = new MutableLiveData<>();
    private MutableLiveData<IAccount> accountLiveData = new MutableLiveData<>();

    private MutableLiveData<String> _openAIData = new MutableLiveData<>("Generating...");

    private MutableLiveData<List<TaskPlanner>> _tasks = new MutableLiveData<>();
    private MutableLiveData<List<CalendarEvent>> _events = new MutableLiveData<>();
    public List<TaskPlanner> getTasks() {
        return _tasks.getValue();
    }
    public List<CalendarEvent> getEvents(){return _events.getValue();}
    public LiveData<String> get_openAIData(){return _openAIData;}
    public void setOpenAIData(String openAIData){
        _openAIData.setValue(openAIData);
    }
    public void setTaskResponses(List<TaskPlanner> tasks){
        _tasks.setValue(tasks);
    }
    public void setEventResponses(List<CalendarEvent> events){_events.setValue(events);}
    public void setSingleAccountApp(ISingleAccountPublicClientApplication singleAccountApp) {
        singleAccountAppLiveData.setValue(singleAccountApp);
    }

    public LiveData<ISingleAccountPublicClientApplication> getSingleAccountAppLiveData() {
        return singleAccountAppLiveData;
    }

    public void setAccount(IAccount account) {
        accountLiveData.setValue(account);
    }

    public LiveData<IAccount> getAccountLiveData() {
        return accountLiveData;
    }

}

package com.cads.projectplanmyday;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)

public class RenderData implements TextClassifierHelper.ClassifierListener {
    private static final String TAG = RenderData.class.getSimpleName();
    private JSONObject data;
    private Context context;
    private TextClassifierHelper textClassifierHelper;
    private final String plannerTask = "https://graph.microsoft.com/v1.0/$metadata#Collection(microsoft.graph.plannerTask)";
    private final String events = "https://graph.microsoft.com/v1.0/$metadata#users('b12afbea-f260-4f4f-b08a-103bb613bc76')/calendarView(id,subject,start,end)";
    private boolean predicitonFinished = false;
    private List<Boolean> taskLabels = new ArrayList<>();
    public RenderData(JSONObject data, Context context){
        this.data = data;
        this.context = context;
        this.textClassifierHelper = new TextClassifierHelper(
                context,this
        );
    }
    public List<TaskPlanner> renderTaskInfo(){
        List<TaskPlanner> datalist = new ArrayList<>();
        try{
            String dataType = data.getString("@odata.context");
            Log.d(TAG, String.valueOf(dataType.equals(events)));
            JSONArray jsonArray = data.getJSONArray("value");
            if(dataType.equals(plannerTask)){

                synchronized (this){
                    predicitonFinished = false;
//                    textClassifierHelper.classify();

                    while(!predicitonFinished){
                        try{
                            wait();
                        }catch (InterruptedException e){
                            Log.d("RenderData", "Error in the prediction process");
                        }
                     }
                }

                for(int i=0;i<jsonArray.length();i++){
                    JSONObject dataObj = jsonArray.getJSONObject(i);
                    TaskPlanner taskData = new TaskPlanner(dataObj.getString("id") , dataObj.getString("title")
                            ,false,dataObj.getString("dueDateTime"), dataObj.getInt("priority"));
                    datalist.add(taskData);
                }

            }

        }catch (Exception e){
            Log.d(TAG, "displayGraphResult: No value");
        }
        return datalist;
    }

    public List<CalendarEvent> renderCalendarInfo(){
        List<CalendarEvent> datalist = new ArrayList<>();
        try{
            String dataType = data.getString("@odata.context");
            Log.d(TAG, String.valueOf(dataType.equals(events)));
            JSONArray jsonArray = data.getJSONArray("value");
            if (dataType.equals(events)){
                for(int i=0;i<jsonArray.length();i++){
                    JSONObject dataObj = jsonArray.getJSONObject(i);
                    CalendarEvent eventData = new CalendarEvent(dataObj.getString("id"), dataObj.getString("subject")
                            ,dataObj.getJSONObject("start").getString("dateTime"),dataObj.getJSONObject("end").getString("dateTime")) ;

                    datalist.add(eventData);
                }
            }

        }catch (Exception e){
            Log.d(TAG, "displayGraphResult: No value");
        }
        return  datalist;
    }


    @Override
    public void onError(@NonNull String error) {

    }

    @Override
    public void onResults(@Nullable List<Float> results, long inferenceTime) {

    }

    @Override
    public void onLossResults(float lossNumber) {

    }
}

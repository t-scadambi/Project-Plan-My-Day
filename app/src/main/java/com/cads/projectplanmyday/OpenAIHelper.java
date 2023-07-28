package com.cads.projectplanmyday;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cads.projectplanmyday.timelineview.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RequiresApi(api = Build.VERSION_CODES.O)


public class OpenAIHelper {
    String TAG = OpenAIHelper.class.getSimpleName();
        private final String openAIResourceUrl = "https://planmydayopenai.openai.azure.com/openai/deployments/ChatBot-turbo35/chat/completions?api-version=2023-03-15-preview";
//    private final String openAIResourceUrl = "https://planmydayopenai.openai.azure.com/openai/deployments/Davinci03/completions?api-version=2022-12-01";

    private final String accessToken = "OPENAI_KEY";

    public void getResponse(@NonNull final Context context,
                            @NonNull final Response.Listener<JSONObject> responseListener,
                            @NonNull final Response.ErrorListener errorListener,
                            @NonNull final String userContent) {
        RequestQueue queue = Volley.newRequestQueue(context);

        JSONObject jsonObject = new JSONObject();

        try {
            String prompt = "You are an assistent for a human who wants to plan their day optimally. The human will give you list of tasks and calendar events." +
                    " Based on the title and priority of the task, it is your job to estimate the approximate time the task could take allot the time slot with start and end timings mentioned (should be less than 1 hour)." +
                    " The calendar events are already scheduled, so you will be given start and end time for it. Make sure that tasks doesn't clash with any other events. The remaining times apart from tasks and calendar events are the \"free hours\". " +
                    "You will also need to consider lunch hours, breaks and other interruptions that might occur. " +
                    "Suggest a detailed timetable for the day to maximize productivity. The output has to be in JSON format only.\n";
            prompt+= "\nHere's an example of json structure:\n" +
                    "{\n" +
                    "\"date\":\"____\",\n" +
                    "\"tasks\":[\n" +
                    "{\"title\":\"____\",\n" +
                    "\"start time\": \"____\",\n" +
                    "\"end time\":\"____\"\n" +
                    "},\n" +
                    "]\n" +
                    ",\"Events\":[\n" +
                    "{\n" +
                    "\"event title\":\"____\",\n" +
                    "\"start time\": \"____\",\n" +
                    "\"end time\":\"____\"\n" +
                    "},\n" +
                    "],\n" +
                    "\"Free hours\":[\n" +
                    "{\n" +
                    "\"start time\": \"____\",\n" +
                    "\"end time\":\"____\"\n" +
                    "}\n" +
                    "]\n" +
                    "}\n" +
                    "\n" +
                    "\\n```json";
//             Create the "messages" array
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", prompt);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userContent);

            JSONArray messages = new JSONArray();
            messages.put(systemMessage);
            messages.put(userMessage);
            jsonObject.put("messages", messages);
//            jsonObject.put("prompt" , prompt);
            // Add other properties
            jsonObject.put("temperature", 1);
            jsonObject.put("max_tokens", 1500);
            jsonObject.put("top_p", 0.5);
            jsonObject.put("frequency_penalty", 0);
            jsonObject.put("presence_penalty", 0);
            jsonObject.put("stop", JSONObject.NULL);

            Log.d(TAG,prompt);
        } catch (JSONException e) {
            Log.d(TAG, e.toString());
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, openAIResourceUrl,
                jsonObject, responseListener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("api-key", accessToken);
                return headers;
            }
        };
        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }
    public String formatUserContent(@NonNull List<TaskPlanner> tasks, @NonNull List<CalendarEvent> events){
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String today = df.format(new Date());
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
        String timenow = df.format(new Date());
        String op= "Date today: " + today+", Time now: "+ timenow +"\n";
        int k =0;

        for (int i=0;i<tasks.size();i++){
            if(tasks.get(i).getIsSelected()){
                if(k==0){
                    op+= "Today's Tasks:\n";
                    k++;
                }
                op += (i+1)+". Task title: " + tasks.get(i).getTitle()+ ", Priority: "+tasks.get(i).getPriority() + ", Due date: " + tasks.get(i).getDue_date()  +"\n";
            }
        }
        op+="\nEvents:\n";
        for(int i=0;i<events.size();i++){
            op+= "Event: "+ events.get(i).getSubject() +", Start time: "+ events.get(i).getStart_time() +", End time: "+events.get(i).getEnd_time() +"\n";
        }
        op+="\nGive only the json response and nothing else. Include all the tasks and events that is given and make sure timings for tasks and events dont overlap.";
        return op;
    }

    public List<com.cads.projectplanmyday.timelineview.Event> formatEventList(JSONObject jsonObject){
        List<com.cads.projectplanmyday.timelineview.Event> eventResponse = new ArrayList<>();
        try{
            String dateToday = jsonObject.getString("date");
            JSONArray taskObjects = jsonObject.getJSONArray("tasks");
            JSONArray eventObjects = jsonObject.getJSONArray("Events");
            for (int i = 0; i < taskObjects.length(); i++) {
                JSONObject task = taskObjects.getJSONObject(i);
                long startTime = convertTimeStringToTimestamp(dateToday, task.getString("start time"));
                long endTime = convertTimeStringToTimestamp(dateToday, task.getString("end time"));
                com.cads.projectplanmyday.timelineview.Event ev = new com.cads.projectplanmyday.timelineview.Event(task.getString("title"), startTime,endTime,1 );
                eventResponse.add(ev);
            }
            for (int i = 0; i < eventObjects.length(); i++) {
                JSONObject event = eventObjects.getJSONObject(i);

                long startTime = convertTimeStringToTimestamp(dateToday, event.getString("start time"));
                long endTime = convertTimeStringToTimestamp(dateToday, event.getString("end time"));
                com.cads.projectplanmyday.timelineview.Event ev = new Event(event.getString("event title"), startTime, endTime,2);
                eventResponse.add(ev);
            }
        }catch (Exception e){
            Log.d(TAG,"JSON error");
        }
        return eventResponse;
    }
    private static long convertTimeStringToTimestamp(String dateStr, String timeStr) {
        String dateTimeStr = dateStr + " " + timeStr;
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        format.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        try {
            Date date = format.parse(dateTimeStr);
            return date.getTime()/1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}


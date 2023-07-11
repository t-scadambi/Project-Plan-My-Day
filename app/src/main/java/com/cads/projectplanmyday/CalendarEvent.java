package com.cads.projectplanmyday;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.O)

public class CalendarEvent implements Serializable {
    // Define IST date-time formatter
    DateTimeFormatter istFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String subject;
    private String id;
    private String start_time;
    private  String end_time;

    public CalendarEvent(String id,String subject, String start_time, String end_time){
        this.id = id;
        this.subject = subject;
        this.start_time = start_time;
        this.end_time = end_time;
    }

    public String getSubject() {
        return subject;
    }

    public String getStart_time() {
        try{// Parse the ISO DateTime string to LocalDateTime object
            LocalDateTime dateTime = LocalDateTime.parse(start_time);
            LocalDateTime istStart = dateTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();


            // Format the LocalDateTime object to minutes:seconds format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String minutesSeconds = istStart.format(formatter);
            return minutesSeconds;
        }catch (Exception e){}
        return start_time;
    }
    public String getEnd_time() {
        try{
            // Parse the ISO DateTime string to LocalDateTime object
            LocalDateTime dateTime = LocalDateTime.parse(end_time);
            LocalDateTime istEnd = dateTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();

            // Format the LocalDateTime object to minutes:seconds format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String minutesSeconds = istEnd.format(formatter);
            return minutesSeconds;
        }catch(Exception e){}
        return end_time;

    }

    public String getTotInfo(){
        LocalDateTime startTime = LocalDateTime.parse(start_time);
        LocalDateTime endTime = LocalDateTime.parse(end_time);

        // Convert to IST (Indian Standard Time) by adding the offset
        LocalDateTime istStart = startTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        LocalDateTime istEnd = endTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();

        // Format LocalDateTime object to IST format string using istStart.format(istFormatter)

        String info = "Event: "+ getSubject()+"\n";
        info += "Start time: " + istStart.format(istFormatter) +"\n";
        info+="End time: "+istEnd.format(istFormatter);
        return info;
    }
}


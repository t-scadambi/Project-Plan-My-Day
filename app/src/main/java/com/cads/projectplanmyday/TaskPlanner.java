package com.cads.projectplanmyday;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.O)

public class TaskPlanner implements Serializable {
    private String title ;
    private String id;
    private String due_date;
    private Integer priority;
    private Boolean isSelected ;
    public TaskPlanner(String id,String title, Boolean selected,String due_date, Integer priority){
        this.id =id;
        this.title = title;
        this.isSelected = selected;
        if(!Objects.equals(due_date, "null")) {
            this.due_date = due_date.substring(0, due_date.length() - 1);
        }
        else{
            this.due_date = due_date;
        }
        this.priority = priority;
    }

    public String getTitle(){
        return this.title;
    }

    public String getDue_date() {
        if(due_date=="null") return "No due date";
        LocalDateTime dateTime = LocalDateTime.parse(due_date);
        LocalDateTime istDate = dateTime.atZone(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        // Format the LocalDateTime object to minutes:seconds format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String taskDue = istDate.format(formatter);
        return taskDue;
    }

    public Boolean getIsSelected(){
        return this.isSelected;
    }

    public void setIsSelected(Boolean isSelected){
        this.isSelected = isSelected;
    }

    public int getPriority() {
        return  priority;
    }
}


package com.cads.projectplanmyday;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)


public class CalendarResponseAdapter extends RecyclerView.Adapter<CalendarResponseAdapter.ViewHolder> {
    private List<CalendarEvent> datalist;
    public CalendarResponseAdapter(List<CalendarEvent> datalist){
        this.datalist = datalist;
    }

    public void setEventsList(List<CalendarEvent> datalist){
        this.datalist = datalist;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvEvent;
        public ViewHolder(@NonNull View itemView){
            super(itemView);
            tvEvent = itemView.findViewById(R.id.tv_event);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String data = datalist.get(position).getTotInfo();
        holder.tvEvent.setText(data);
    }

    @Override
    public int getItemCount() {
        return datalist.size();
    }



}

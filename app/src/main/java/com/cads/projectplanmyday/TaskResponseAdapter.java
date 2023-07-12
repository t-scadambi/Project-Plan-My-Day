package com.cads.projectplanmyday;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)


public class TaskResponseAdapter extends RecyclerView.Adapter<TaskResponseAdapter.ViewHolder>{

    private List<TaskPlanner> dataList;
    public TaskResponseAdapter(List<TaskPlanner> dataList) {
        this.dataList = dataList;
    }
    //to submit data from the UI (Binding Adapters)
    public void setDataList(List<TaskPlanner> dataList){
        this.dataList = dataList;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem;
        CheckBox checkbox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tv_item); // Replace with the appropriate view ID
            checkbox =itemView.findViewById(R.id.checkBox);
        }
    }
    @NonNull
    @Override
    public TaskResponseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskResponseAdapter.ViewHolder holder, int position) {
        String data = dataList.get(position).getTitle();
        holder.tvItem.setText(data);
        //in some cases, it will prevent unwanted situations
        holder.checkbox.setOnCheckedChangeListener(null);

        //if true, your checkbox will be selected, else unselected
        holder.checkbox.setChecked(dataList.get(position).getIsSelected());

        holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dataList.get(holder.getAdapterPosition()).setIsSelected(isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

}
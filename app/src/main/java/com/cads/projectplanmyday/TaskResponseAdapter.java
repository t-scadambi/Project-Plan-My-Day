package com.cads.projectplanmyday;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)


public class TaskResponseAdapter extends RecyclerView.Adapter<TaskResponseAdapter.ViewHolder>{

    private List<TaskPlanner> dataList;
    private Context context;
    public TaskResponseAdapter(List<TaskPlanner> dataList, Context context) {
        this.dataList = dataList;
        this.context = context;
    }
    //to submit data from the UI (Binding Adapters)
    public void setDataList(List<TaskPlanner> dataList){
        this.dataList = dataList;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem;
        CheckBox checkbox;
        ConstraintLayout task_holder;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tv_item); // Replace with the appropriate view ID
            checkbox =itemView.findViewById(R.id.checkBox);
            task_holder = itemView.findViewById(R.id.task_holder);
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
        setBg(holder, dataList.get(position).getIsSelected());
        holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dataList.get(holder.getAdapterPosition()).setIsSelected(isChecked);
                setBg(holder, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }
    void setBg(@NonNull TaskResponseAdapter.ViewHolder holder,Boolean isChecked){
        if(isChecked) {
            holder.task_holder.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_cardv));
        }
        else{
            holder.task_holder.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_card_white));
        }
    }
}
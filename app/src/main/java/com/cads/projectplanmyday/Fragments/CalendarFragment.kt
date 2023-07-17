package com.cads.projectplanmyday.Fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.akribase.timelineview.Event
import com.cads.projectplanmyday.R
import com.cads.projectplanmyday.ReadWriteHandler
import com.cads.projectplanmyday.databinding.FragmentCalendarBinding
import com.cads.projectplanmyday.databinding.FragmentOpenaiResponseBinding
import com.google.android.material.snackbar.Snackbar

@RequiresApi(Build.VERSION_CODES.O)
class CalendarFragment : Fragment() {

    private  lateinit var binding: FragmentCalendarBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalendarBinding.inflate(inflater,container,false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val eventsList :List<Event>? = ReadWriteHandler.loadDataFromFile(requireContext())
        if(eventsList==null){
            binding.calendarIid.timelineView.timelineEvents = listOf()
        }
        else{
            binding.calendarIid.timelineView.timelineEvents = eventsList
        }
    }


}
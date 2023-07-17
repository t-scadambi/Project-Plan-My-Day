package com.cads.projectplanmyday.Fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.cads.projectplanmyday.ActivityViewModel
import com.cads.projectplanmyday.timelineview.Event
import com.cads.projectplanmyday.R
import com.cads.projectplanmyday.ReadWriteHandler
import com.cads.projectplanmyday.databinding.FragmentOpenaiResponseBinding

@RequiresApi(Build.VERSION_CODES.O)
class OpenaiResponseFragment : Fragment() {


    private lateinit var binding: FragmentOpenaiResponseBinding
    val viewModel: ActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpenaiResponseBinding.inflate(inflater,container,false);

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val eventsList :List<Event> = viewModel.openAIData
        binding.calendarIid.timelineView.timelineEvents = eventsList
        binding.fab.setOnClickListener { v ->
            ReadWriteHandler.saveDataToFile(requireContext(),eventsList)
            Toast.makeText(context,"Calendar saved" , Toast.LENGTH_SHORT).show()
            Navigation.findNavController(v).navigate(R.id.action_openaiResponseFragment_to_homepageFragment)
        }
    }
}

package com.example.statusbarshow.ui.home

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.statusbarshow.cpuusage
import com.example.statusbarshow.databinding.FragmentHomeBinding
import com.example.statusbarshow.isInForeground
import com.example.statusbarshow.memusage
import com.example.statusbarshow.samplingtime
import com.example.statusbarshow.totalcpuusage

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val memView = binding.realtimeviewMEM
        val cpuView = binding.realtimeviewCPU
        val cpuiView = arrayOf(
            binding.realtimeviewCPU0,
            binding.realtimeviewCPU1,
            binding.realtimeviewCPU2,
            binding.realtimeviewCPU3,
            binding.realtimeviewCPU4,
            binding.realtimeviewCPU5,
            binding.realtimeviewCPU6,
            binding.realtimeviewCPU7
        )


        val handlercpu = Handler(Looper.getMainLooper())
        val handlermem = Handler(Looper.getMainLooper())

        if(prefs.getBoolean("CPUState", false)) {
            handlercpu.post(object : Runnable {
                override fun run() {
                    if(isInForeground){
                        cpuView.addPoint(totalcpuusage[1].toFloat())    //总利用率
                        cpuiView.indices.forEach { i ->
                            cpuiView[i].addPoint(cpuusage[i][1].toFloat())
                        } //各核心CPU利用率
                        handlercpu.postDelayed(this, samplingtime)
                    }
                    }
            })
        }

        if(prefs.getBoolean("MEMState", false)) {
            handlermem.post(object : Runnable {
                override fun run() {
                    if(isInForeground){
                        memView.addPoint(memusage.toFloat())
                        handlercpu.postDelayed(this, samplingtime)}
                }

            })
        }
        cpuView.setOnLongClickListener {
            cpuView.toggleHistory()
            true
        }
        memView.setOnLongClickListener {
            memView.toggleHistory()
            true
        }
        for (v in cpuiView) {
            v.setOnLongClickListener {
                memView.toggleHistory()
                true
            }
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
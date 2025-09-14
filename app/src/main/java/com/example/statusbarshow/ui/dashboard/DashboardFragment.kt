package com.example.statusbarshow.ui.dashboard

import com.example.statusbarshow.R  //导入自定义布局文件
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.statusbarshow.MyFunction
import com.example.statusbarshow.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val cpuinfoArray = mutableListOf<MutableMap<String, String>>()


        for(i in 0 .. prefs.getInt("CPUCoreNumber",0)){
            val freq = "%.2f".format(prefs.getLong("CPU${i}MaxFreq",0).toFloat()/1000f/1000f)
            cpuinfoArray.add(mutableMapOf("CPU" to "CPU$i","CPUInfo" to (freq + "GHz " + MyFunction.getCPUArch(prefs.getString("CPU$i","").toString() ))))
        }

        val cpuinfoView : ListView = binding.listviewCpuinfo   //binding引用时下划线转为大写
        val cpuadapter = SimpleAdapter(
            requireContext(),
            cpuinfoArray,
            R.layout.listview_item, //自定义布局文件
            arrayOf("CPU", "CPUInfo"),
            intArrayOf(R.id.title, R.id.info)
        )

        cpuinfoView.adapter = cpuadapter

        val stoinfoView : ListView = binding.listviewStoinfo   //binding引用时下划线转为大写


        val stoinfoArray = mutableListOf<MutableMap<String, String>>()
        val totalmem = MyFunction.getCPUArch(prefs.getString("TotalMemory","").toString()).toLong()/1000
        stoinfoArray.add(mutableMapOf("Term" to "Total Memory","Info" to "$totalmem MB" ))

        val stoadapter = SimpleAdapter(
            requireContext(),
            stoinfoArray,
            R.layout.listview_item, //自定义布局文件
            arrayOf("Term", "Info"),
            intArrayOf(R.id.title, R.id.info)
        )
        stoinfoView.adapter = stoadapter
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
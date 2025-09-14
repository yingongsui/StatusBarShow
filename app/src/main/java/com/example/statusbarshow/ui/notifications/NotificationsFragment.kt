package com.example.statusbarshow.ui.notifications


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.statusbarshow.CPUNotiService
import com.example.statusbarshow.CPUService
import com.example.statusbarshow.MEMNotiService
import com.example.statusbarshow.MEMService
import com.example.statusbarshow.databinding.FragmentNotificationsBinding
import androidx.core.content.edit
import com.example.statusbarshow.CPUMEMNotiService
import com.example.statusbarshow.NetService
import com.example.statusbarshow.R

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val memSwitch = binding.MEMswitch
        val cpuSwitch = binding.CPUswitch
        val memnoSwitch = binding.MEMnotiswitch
        val cpunoSwitch = binding.CPUnotiswitch
        val netSwitch = binding.NetSpeedswitch
        val cmnoSwitch = binding.CPUMEMNoswitch

        val aboutTextView = binding.abouttitle

        //回复控件状态
        memSwitch.isChecked = prefs.getBoolean("MEMState", false)
        cpuSwitch.isChecked = prefs.getBoolean("CPUState", false)
        memnoSwitch.isChecked = prefs.getBoolean("MEMNoState", false)
        cpunoSwitch.isChecked = prefs.getBoolean("CPUNoState", false)
        netSwitch.isChecked = prefs.getBoolean("NETSpState", false)
        cmnoSwitch.isChecked = prefs.getBoolean("CMNoState", false)

        //->:Lambda函数 参数->函数
        cpuSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("CPUState", true) }   //记录控件状态到实体文件
                requireActivity().startService(Intent(requireActivity(), CPUService::class.java))
            } else {
                prefs.edit { putBoolean("CPUState", false) }
                requireActivity().stopService(Intent(requireActivity(), CPUService::class.java))
            }
        }

        cpunoSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("CPUNoState", true) }
                requireActivity().startService(Intent(requireActivity(), CPUNotiService::class.java))
            } else {
                prefs.edit { putBoolean("CPUNoState", false) }
                requireActivity().stopService(Intent(requireActivity(), CPUNotiService::class.java))
            }

        }

        memSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("MEMState", true) }
                requireActivity().startService(Intent(requireActivity(), MEMService::class.java))
            } else {
                prefs.edit { putBoolean("MEMState", false) }
                requireActivity().stopService(Intent(requireActivity(), MEMService::class.java))
            }
        }

        memnoSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("MEMNoState", true) }
                requireActivity().startService(Intent(requireActivity(), MEMNotiService::class.java))

            } else {
                prefs.edit { putBoolean("MEMNoState", false) }
                requireActivity().stopService(Intent(requireActivity(), MEMNotiService::class.java))
            }
        }

        netSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("NETSpState", true) }   //记录控件状态到实体文件
                requireActivity().startService(Intent(requireActivity(), NetService::class.java))
            } else {
                prefs.edit { putBoolean("NETSpState", false) }
                requireActivity().stopService(Intent(requireActivity(), NetService::class.java))
            }
        }

        cmnoSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit { putBoolean("CMNoState", true) }   //记录控件状态到实体文件
                requireActivity().startService(Intent(requireActivity(), CPUMEMNotiService::class.java))
            } else {
                prefs.edit { putBoolean("CMNoState", false) }
                requireActivity().stopService(Intent(requireActivity(), CPUMEMNotiService::class.java))
            }
        }

        aboutTextView.setOnClickListener {
            AlertDialog.Builder(requireActivity())
                .setTitle("StatusBarShow")
                .setMessage("Version:" + getString(R.string.appversion) + "\n© 2025")
                .setPositiveButton("OK", null)
                .show()

        }


        return root


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}
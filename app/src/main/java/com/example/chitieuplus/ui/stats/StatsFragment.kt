package com.example.chitieuplus.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.chitieuplus.databinding.FragmentStatsBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.*

class StatsFragment : Fragment() {
    private var _vb: FragmentStatsBinding? = null
    private val vb get() = _vb!!
    private val vm: TransactionViewModel by viewModels()
    private val nf = NumberFormat.getInstance(Locale("vi","VN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _vb = FragmentStatsBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.totalIncome.observe(viewLifecycleOwner) { income ->
            vb.tvIncome.text = "Tổng thu: ${nf.format(income ?: 0)}₫"
        }
        vm.totalExpense.observe(viewLifecycleOwner) { expense ->
            vb.tvExpense.text = "Tổng chi: ${nf.format(kotlin.math.abs(expense ?: 0))}₫"
        }
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}

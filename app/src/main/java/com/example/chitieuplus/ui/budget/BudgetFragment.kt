package com.example.chitieuplus.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.chitieuplus.R
import com.example.chitieuplus.viewmodel.BudgetViewModel
import com.example.chitieuplus.viewmodel.BudgetViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val edtLimit = view.findViewById<TextInputEditText>(R.id.edtLimit)
        val btnSave = view.findViewById<Button>(R.id.btnSaveBudget)
        val txtSpent = view.findViewById<TextView>(R.id.txtSpent)
        val txtStatus = view.findViewById<TextView>(R.id.txtStatus)

        // Bấm nút -> lưu ngân sách
        btnSave.setOnClickListener {
            val limitText = edtLimit.text?.toString()?.trim().orEmpty()
            if (limitText.isNotEmpty()) {
                val limit = limitText.toLong()
                viewModel.saveBudget(limit)
                Toast.makeText(requireContext(), "Đã lưu hạn mức", Toast.LENGTH_SHORT).show()
            } else {
                edtLimit.error = "Vui lòng nhập hạn mức"
            }
        }

        // Quan sát state từ ViewModel -> CHỈ CẬP NHẬT UI, KHÔNG GỌI SERVICE
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                txtSpent.text = "Đã chi: ${state.spentAmount} đ"

                if (state.limitAmount > 0 && edtLimit.text.isNullOrEmpty()) {
                    edtLimit.setText(state.limitAmount.toString())
                }

                if (state.isOverBudget) {
                    txtStatus.text = "Trạng thái: ĐÃ VƯỢT NGÂN SÁCH!"
                } else {
                    txtStatus.text = "Trạng thái: Trong giới hạn"
                }
            }
        }

        // Lần đầu vào màn hình -> load ngân sách + chi tiêu hiện tại
        viewModel.loadBudget()
    }
}

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
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels {
        BudgetViewModelFactory(requireContext())
    }

    private val moneyFmt: NumberFormat =
        NumberFormat.getInstance(Locale("vi", "VN"))

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

        val txtLimitValue = view.findViewById<TextView>(R.id.txtLimitValue)
        val txtRemaining = view.findViewById<TextView>(R.id.txtRemaining)
        val progressBudget =
            view.findViewById<LinearProgressIndicator>(R.id.progressBudget)

        // Bấm nút -> lưu ngân sách
        btnSave.setOnClickListener {
            val limitText = edtLimit.text?.toString()?.trim().orEmpty()
            if (limitText.isNotEmpty()) {
                try {
                    val limit = limitText.toLong()
                    viewModel.saveBudget(limit)
                    Toast.makeText(
                        requireContext(),
                        "Đã lưu hạn mức",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: NumberFormatException) {
                    edtLimit.error = "Số không hợp lệ"
                }
            } else {
                edtLimit.error = "Vui lòng nhập hạn mức"
            }
        }

        // Quan sát state từ ViewModel -> cập nhật UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->

                // Hạn mức + đã chi + còn lại
                txtLimitValue.text =
                    "Hạn mức: ${moneyFmt.format(state.limitAmount)} đ"
                txtSpent.text =
                    "Đã chi: ${moneyFmt.format(state.spentAmount)} đ"

                if (state.limitAmount > 0) {
                    val remaining = (state.limitAmount - state.spentAmount)
                        .coerceAtLeast(0)
                    txtRemaining.text =
                        "Còn lại: ${moneyFmt.format(remaining)} đ"

                    // progress = % đã chi so với hạn mức
                    val percent = (state.spentAmount * 100 / state.limitAmount)
                        .coerceIn(0, 100)
                    progressBudget.max = 100
                    progressBudget.progress = percent.toInt()
                    progressBudget.visibility = View.VISIBLE
                } else {
                    txtRemaining.text = "Còn lại: -"
                    progressBudget.progress = 0
                    progressBudget.visibility = View.GONE
                }

                // Nếu có limit mà ô nhập đang trống -> fill sẵn
                if (state.limitAmount > 0 && edtLimit.text.isNullOrEmpty()) {
                    edtLimit.setText(state.limitAmount.toString())
                }

                // Trạng thái
                txtStatus.text =
                    if (state.isOverBudget) {
                        "Trạng thái: ĐÃ VƯỢT NGÂN SÁCH!"
                    } else {
                        "Trạng thái: Trong giới hạn"
                    }
            }
        }

        // Lần đầu vào màn hình -> load ngân sách + chi tiêu hiện tại
        viewModel.loadBudget()
    }
}

package com.example.chitieuplus.ui.stats

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.chitieuplus.R
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.databinding.FragmentStatsBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import kotlin.math.abs
import java.util.Locale

class StatsFragment : Fragment() {

    private var _vb: FragmentStatsBinding? = null
    private val vb get() = _vb!!

    private val vm: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // enable menu share
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentStatsBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.totalIncome.observe(viewLifecycleOwner) {
            // chỉ hiển thị + tính toán lại ở updateBalanceAndBars()
            vb.tvIncome.text = "Tổng thu: ${formatVnd((it ?: 0))}"
            updateBalanceAndBars()
        }
        vm.totalExpense.observe(viewLifecycleOwner) {
            // hiển thị có dấu âm (nếu muốn), nhưng tính toán dùng |chi|
            val expenseAbs = abs(it ?: 0)
            vb.tvExpense.text = "Tổng chi: ${formatVnd(-expenseAbs)}"
            updateBalanceAndBars()
        }

        vm.items.observe(viewLifecycleOwner) { list ->
            renderTopExpenseCategories(list)
        }
    }

    /** Tính số dư + cập nhật % Thu/Chi, ProgressBar, PieView */
    private fun updateBalanceAndBars() {
        val income = (vm.totalIncome.value ?: 0).coerceAtLeast(0)    // đảm bảo không âm
        val expenseAbs = abs(vm.totalExpense.value ?: 0)              // dùng trị tuyệt đối

        val total = income + expenseAbs
        val balance = income - expenseAbs

        vb.tvBalance.text = "Còn lại: ${formatVnd(balance)}"

        // cập nhật biểu đồ tròn
        vb.pieView.setData(income, expenseAbs)

        if (total <= 0) {
            vb.progressIncome.progress = 0
            vb.progressExpense.progress = 0
            vb.tvIncomePct.text = "Thu: 0%"
            vb.tvExpensePct.text = "Chi: 0%"
            vb.progressIncome.isVisible = false
            vb.progressExpense.isVisible = false
            return
        }

        vb.progressIncome.isVisible = true
        vb.progressExpense.isVisible = true

        val incomePct = ((income.toDouble() / total.toDouble()) * 100).toInt()
        val expensePct = 100 - incomePct

        vb.progressIncome.setProgress(incomePct, true)
        vb.progressExpense.setProgress(expensePct, true)
        vb.tvIncomePct.text = "Thu: $incomePct%"
        vb.tvExpensePct.text = "Chi: $expensePct%"
    }

    /** Gom nhóm top danh mục CHI và render list; bấm giữ để share nhanh */
    private fun renderTopExpenseCategories(list: List<TransactionEntity>) {
        val container = vb.chipsContainer
        container.removeAllViews()

        val grouped = list.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .mapValues { (_, sum) -> abs(sum) } // đảm bảo dương để hiển thị
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        if (grouped.isEmpty()) {
            addChip("Chưa có dữ liệu", 0)
            return
        }
        grouped.forEach { (cat, total) -> addChip(cat, total) }
    }

    private fun addChip(title: String, amount: Long) {
        val tv = TextView(requireContext()).apply {
            text = "• $title: ${formatVnd(-amount)}" // hiển thị có dấu âm cho danh mục chi
            textSize = 14f
            setPadding(0, 6, 0, 6)
            setOnLongClickListener {
                shareText("Danh mục: $title — ${formatVnd(-amount)}")
                true
            }
        }
        vb.chipsContainer.addView(tv)
    }

    private fun formatVnd(value: Long): String {
        // hiển thị theo vi-VN, giữ dấu âm nếu value < 0
        return String.format(Locale("vi", "VN"), "%,d ₫", value)
    }

    /** MENU (Share) **/
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_stats, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                val income = (vm.totalIncome.value ?: 0).coerceAtLeast(0)
                val expenseAbs = abs(vm.totalExpense.value ?: 0)
                val balance = income - expenseAbs
                val total = income + expenseAbs
                val incomePct = if (total > 0) (income * 100 / total) else 0
                val expensePct = 100 - incomePct

                val msg = buildString {
                    appendLine("ChiTieuPlus – Tổng quan")
                    appendLine("Tổng thu: ${formatVnd(income)} ($incomePct%)")
                    appendLine("Tổng chi: ${formatVnd(-expenseAbs)} ($expensePct%)")
                    appendLine("Còn lại: ${formatVnd(balance)}")
                }
                shareText(msg)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Thống kê ChiTieuPlus")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Chia sẻ qua…"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}

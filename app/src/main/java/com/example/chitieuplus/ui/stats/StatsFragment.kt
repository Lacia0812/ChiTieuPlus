package com.example.chitieuplus.ui.stats

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.chitieuplus.R
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.databinding.FragmentStatsBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import kotlin.math.abs
import java.util.Calendar
import java.util.Locale

class StatsFragment : Fragment() {

    private var _vb: FragmentStatsBinding? = null
    private val vb get() = _vb!!

    private val vm: TransactionViewModel by viewModels()

    // state lọc ngày
    private var fromDay: Long? = null
    private var toDay: Long? = null

    // dữ liệu theo khoảng ngày (nếu có)
    private var filteredItems: List<TransactionEntity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // enable menu share + clear filter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentStatsBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Quan sát tổng hệ thống (khi KHÔNG lọc ngày)
        vm.totalIncome.observe(viewLifecycleOwner) {
            if (!isFiltering()) {
                vb.tvIncome.text = "Tổng thu: ${formatVnd((it ?: 0))}"
                updateUI()
            }
        }
        vm.totalExpense.observe(viewLifecycleOwner) {
            if (!isFiltering()) {
                val expenseAbs = abs(it ?: 0)
                vb.tvExpense.text = "Tổng chi: ${formatVnd(-expenseAbs)}"
                updateUI()
            }
        }

        // Danh mục chi nổi bật + biểu đồ cột 3 tháng từ danh sách hiện hành
        // - nếu lọc: dùng filteredItems
        // - nếu không lọc: dùng vm.items
        vm.items.observe(viewLifecycleOwner) { list ->
            if (!isFiltering()) {
                renderTopExpenseCategories(list)
                updateBarChartFromList(list)
            }
        }

        // Nút chọn ngày
        vb.btnFrom.setOnClickListener { pickDate(isFrom = true) }
        vb.btnTo.setOnClickListener { pickDate(isFrom = false) }

        // Hiển thị trạng thái ban đầu
        showRangeLabel()
    }

    /** Hiện DatePicker và áp dụng lọc */
    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        val dlg = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val millis = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (isFrom) {
                    fromDay = millis
                    // Nếu chưa có toDay -> mặc định cuối ngày fromDay
                    if (toDay == null) {
                        toDay = endOfDayMillis(millis)
                    }
                } else {
                    toDay = endOfDayMillis(millis)
                    if (fromDay == null) {
                        fromDay = startOfDayMillis(millis)
                    }
                }
                applyFilterIfReady()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dlg.show()
    }

    /** Khi đã có từ & đến -> gọi VM getByDateRange và tính tổng từ list */
    private fun applyFilterIfReady() {
        val f = fromDay
        val t = toDay
        if (f == null || t == null) {
            showRangeLabel()
            return
        }
        // đảm bảo f <= t
        val from = minOf(f, t)
        val to = maxOf(f, t)

        // observe tạm thời dữ liệu theo khoảng
        vm.getByDateRange(from, to).observe(viewLifecycleOwner) { list ->
            filteredItems = list
            // tự tính tổng theo list lọc
            val income = list.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }.coerceAtLeast(0)
            val expenseAbs = abs(
                list.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
            )
            vb.tvIncome.text = "Tổng thu: ${formatVnd(income)}"
            vb.tvExpense.text = "Tổng chi: ${formatVnd(-expenseAbs)}"
            renderTopExpenseCategories(list)
            updateBarChartFromList(list)
            showRangeLabel(from, to)
            updateUI(incomeOverride = income, expenseAbsOverride = expenseAbs)
        }
    }

    /** Cập nhật số dư + % + biểu đồ tròn. */
    private fun updateUI(
        incomeOverride: Long? = null,
        expenseAbsOverride: Long? = null
    ) {
        val income = incomeOverride ?: (vm.totalIncome.value ?: 0).coerceAtLeast(0)
        val expenseAbs = expenseAbsOverride ?: abs(vm.totalExpense.value ?: 0)
        val total = income + expenseAbs
        val balance = income - expenseAbs

        vb.tvBalance.text = "Còn lại: ${formatVnd(balance)}"
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

    /** Cập nhật biểu đồ cột 3 tháng gần nhất */
    private fun updateBarChartFromList(list: List<TransactionEntity>) {
        if (!isAdded) return

        if (list.isEmpty()) {
            vb.bar3Months.setData(emptyList(), emptyList(), emptyList())
            return
        }

        val now = Calendar.getInstance()
        val months = mutableListOf<Pair<Int, Int>>() // (year, monthIndex)
        repeat(3) {
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH)
            // thêm vào đầu để thứ tự: 2 tháng trước - 1 tháng trước - hiện tại
            months.add(0, year to month)
            now.add(Calendar.MONTH, -1)
        }

        val grouped = list.groupBy { txn ->
            val c = Calendar.getInstance()
            c.timeInMillis = txn.date
            c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
        }

        val labels = mutableListOf<String>()
        val incomes = mutableListOf<Long>()
        val expenses = mutableListOf<Long>()

        months.forEach { (year, month) ->
            val items = grouped[year to month] ?: emptyList()
            val income = items
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
                .coerceAtLeast(0)
            val expenseAbs = abs(
                items.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
            )

            val label = String.format(
                Locale("vi", "VN"),
                "%02d/%02d",
                month + 1,
                year % 100
            )

            labels.add(label)
            incomes.add(income)
            expenses.add(expenseAbs)
        }

        vb.bar3Months.setData(labels, incomes, expenses)
    }

    /** Render top danh mục CHI từ list hiện hành */
    private fun renderTopExpenseCategories(list: List<TransactionEntity>) {
        val container = vb.chipsContainer
        container.removeAllViews()

        val grouped = list.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, items) -> abs(items.sumOf { it.amount }) } // dương để so sánh
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        if (grouped.isEmpty()) {
            addChip("Chưa có dữ liệu", 0)
            return
        }
        grouped.forEach { (cat, total) -> addChip(cat, total) }
    }

    private fun addChip(title: String, amountAbs: Long) {
        val tv = TextView(requireContext()).apply {
            text = "• $title: ${formatVnd(-amountAbs)}" // chi hiển thị âm
            textSize = 14f
            setPadding(0, 6, 0, 6)
            setOnLongClickListener {
                shareText("Danh mục: $title — ${formatVnd(-amountAbs)}")
                true
            }
        }
        vb.chipsContainer.addView(tv)
    }

    private fun formatVnd(value: Long): String {
        return String.format(Locale("vi", "VN"), "%,d ₫", value)
    }

    /** MENU (Share + Clear filter + Budget) */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_stats, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                val (income, expenseAbs) = currentTotals()
                val balance = income - expenseAbs
                val total = income + expenseAbs
                val incomePct = if (total > 0) (income * 100 / total) else 0
                val expensePct = 100 - incomePct

                val msg = buildString {
                    appendLine("ChiTieuPlus – Tổng quan" + rangeSuffixForShare())
                    appendLine("Tổng thu: ${formatVnd(income)} ($incomePct%)")
                    appendLine("Tổng chi: ${formatVnd(-expenseAbs)} ($expensePct%)")
                    appendLine("Còn lại: ${formatVnd(balance)}")
                }
                shareText(msg)
                true
            }
            R.id.action_clear_filter -> {
                // Xoá lọc
                fromDay = null
                toDay = null
                filteredItems = null
                showRangeLabel()
                // reset hiển thị theo “tổng hệ thống”
                vm.items.value?.let {
                    renderTopExpenseCategories(it)
                    updateBarChartFromList(it)
                }
                updateUI()
                true
            }
            R.id.action_budget -> {
                // mở màn Ngân sách
                findNavController().navigate(R.id.action_stats_to_budget)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun currentTotals(): Pair<Long, Long> {
        return if (isFiltering() && filteredItems != null) {
            val inc = filteredItems!!.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }.coerceAtLeast(0)
            val expAbs = abs(
                filteredItems!!.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
            )
            inc to expAbs
        } else {
            val inc = (vm.totalIncome.value ?: 0).coerceAtLeast(0)
            val expAbs = abs(vm.totalExpense.value ?: 0)
            inc to expAbs
        }
    }

    private fun isFiltering() = (fromDay != null && toDay != null)

    private fun showRangeLabel(from: Long? = fromDay, to: Long? = toDay) {
        vb.tvRange.text = if (from != null && to != null) {
            "Khoảng ngày: ${formatDate(from)} — ${formatDate(to)}"
        } else {
            "(Không lọc theo ngày)"
        }
    }

    private fun formatDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        return "%02d/%02d/%04d".format(d, m, y)
    }

    private fun startOfDayMillis(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun endOfDayMillis(millis: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Thống kê ChiTieuPlus")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Chia sẻ qua…"))
    }

    private fun rangeSuffixForShare(): String {
        val f = fromDay; val t = toDay
        return if (f != null && t != null) " (${formatDate(f)}–${formatDate(t)})" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}

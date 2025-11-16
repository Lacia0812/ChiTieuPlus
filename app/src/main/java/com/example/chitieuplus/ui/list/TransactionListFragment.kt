package com.example.chitieuplus.ui.list

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chitieuplus.R
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.databinding.FragmentTransactionListBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class TransactionListFragment : Fragment() {

    private var _vb: FragmentTransactionListBinding? = null
    private val vb get() = _vb!!

    private val vm: TransactionViewModel by activityViewModels()

    private val adapter = TransactionAdapter { item ->
        findNavController().navigate(
            R.id.action_list_to_edit,
            Bundle().apply { putInt("id", item.id) }
        )
    }

    // ====== Date filter ======
    private var fromDay: Long? = null
    private var toDay: Long? = null
    private var currentKeyword: String = ""
    private var baseList: List<TransactionEntity> = emptyList()

    private val ID_FILTER_DATE = 0x1001
    private val ID_CLEAR_FILTER = 0x1002

    // ====== EXPORT CSV LAUNCHER ======
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            exportCsvToUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _vb = FragmentTransactionListBinding.inflate(inflater, container, false)

        // RecyclerView
        vb.rv.layoutManager = LinearLayoutManager(requireContext())
        vb.rv.adapter = adapter

        // Swipe delete
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = adapter.currentAt(pos) ?: return
                    viewLifecycleOwner.lifecycleScope.launch { vm.delete(item) }
                }
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(vb.rv)

        // FAB
        vb.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_edit)
        }

        vb.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 6) vb.fabAdd.hide() else if (dy < -6) vb.fabAdd.show()
            }
        })

        // Observe list
        vm.items.observe(viewLifecycleOwner) { list ->
            if (!isFiltering()) {
                baseList = list
                if (currentKeyword.isBlank()) adapter.submit(list)
                else renderFilteredAndSearched()
            }
        }

        // MENU
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

                // Menu gốc
                menuInflater.inflate(R.menu.menu_list, menu)

                // ⭐ Thêm menu xuất CSV
                menuInflater.inflate(R.menu.menu_transaction_list, menu)

                // Lọc ngày
                menu.add(0, ID_FILTER_DATE, 0, "Lọc theo ngày")
                    .setIcon(android.R.drawable.ic_menu_month)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

                menu.add(0, ID_CLEAR_FILTER, 1, "Xoá lọc")
                    .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

                // Search
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search_hint)
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(q: String?) = true
                    override fun onQueryTextChange(q: String?): Boolean {
                        currentKeyword = q.orEmpty()
                        applySearch(currentKeyword)
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {

                    // Xuất CSV
                    R.id.action_export_csv -> {
                        startExportCsv()
                        true
                    }

                    R.id.action_stats -> {
                        findNavController().navigate(R.id.action_list_to_stats); true
                    }
                    R.id.action_manage_categories -> {
                        findNavController().navigate(R.id.categoryManagerFragment); true
                    }
                    R.id.action_budget -> {
                        findNavController().navigate(R.id.action_list_to_budget); true
                    }
                    ID_FILTER_DATE -> { pickDateRange(); true }
                    ID_CLEAR_FILTER -> { clearDateFilter(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return vb.root
    }

    // ====================== SEARCH ======================
    private fun applySearch(q: String) {
        if (!isFiltering()) {
            if (q.isBlank()) vm.items.observe(viewLifecycleOwner) { adapter.submit(it) }
            else vm.search("%$q%").observe(viewLifecycleOwner) { adapter.submit(it) }
        } else {
            renderFilteredAndSearched()
        }
    }

    private fun renderFilteredAndSearched() {
        val kw = currentKeyword.lowercase(Locale.ROOT)
        val filtered = baseList.filter {
            it.title.lowercase(Locale.ROOT).contains(kw) ||
                    it.category.lowercase(Locale.ROOT).contains(kw)
        }
        adapter.submit(filtered)
    }

    // ====================== DATE FILTER ======================
    private fun pickDateRange() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            fromDay = startOfDayMillis(y, m, d)

            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                toDay = endOfDayMillis(y2, m2, d2)
                applyDateFilter()
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()

        }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
    }

    private fun applyDateFilter() {
        val f = fromDay ?: return
        val t = toDay ?: return
        val from = min(f, t)
        val to = max(f, t)

        vm.items.observe(viewLifecycleOwner) { full ->
            baseList = full.filter { it.date in from..to }
            renderFilteredAndSearched()
        }

        Toast.makeText(requireContext(), "Lọc: ${fmtDate(from)} – ${fmtDate(to)}", Toast.LENGTH_SHORT).show()
    }

    private fun clearDateFilter() {
        fromDay = null
        toDay = null
        vm.items.observe(viewLifecycleOwner) {
            baseList = it
            renderFilteredAndSearched()
        }
    }

    private fun isFiltering() = (fromDay != null && toDay != null)

    private fun startOfDayMillis(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }.timeInMillis

    private fun endOfDayMillis(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }.timeInMillis

    private fun fmtDate(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%02d/%02d/%04d".format(
            c[Calendar.DAY_OF_MONTH],
            c[Calendar.MONTH] + 1,
            c[Calendar.YEAR]
        )
    }

    // ====================== EXPORT CSV ======================
    private fun startExportCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/csv"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "chitieu_${System.currentTimeMillis()}.csv")
        }
        exportLauncher.launch(intent)
    }

    private fun exportCsvToUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val db = AppDatabase.get(requireContext())
            val dao = db.transactionDao()
            val list = dao.getAllOnce()

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))

            val builder = StringBuilder()
            builder.appendLine("ID;Tiêu đề;Số tiền;Loại;Danh mục;Ngày;Ghi chú")

            for (t in list) {
                val dateStr = sdf.format(Date(t.date))
                val typeVi = if (t.type.name == "INCOME") "Thu" else "Chi"

                builder.appendLine(
                    "${t.id};" +
                            "\"${t.title.replace("\"", "\"\"")}\";" +
                            "${t.amount};" +
                            "$typeVi;" +
                            "\"${t.category.replace("\"", "\"\"")}\";" +
                            "\"$dateStr\";" +
                            "\"${t.note?.replace("\"", "\"\"") ?: ""}\""
                )
            }

            val csvContent = builder.toString()

            requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                os.write("\uFEFF".toByteArray(Charsets.UTF_8))
                os.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Xuất CSV thành công!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}

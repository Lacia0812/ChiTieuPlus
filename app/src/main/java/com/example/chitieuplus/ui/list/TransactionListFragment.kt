package com.example.chitieuplus.ui.list

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
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
import com.example.chitieuplus.data.TransactionEntity
import com.example.chitieuplus.databinding.FragmentTransactionListBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
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

    // ====== Date range filter state & IDs for dynamic menu ======
    private var fromDay: Long? = null
    private var toDay: Long? = null
    private var currentKeyword: String = ""

    private var baseList: List<TransactionEntity> = emptyList() // nguồn hiện hành để render (khi có lọc ngày)
    private val ID_FILTER_DATE = 0x1001
    private val ID_CLEAR_FILTER = 0x1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentTransactionListBinding.inflate(inflater, container, false)

        // RecyclerView
        vb.rv.layoutManager = LinearLayoutManager(requireContext())
        vb.rv.adapter = adapter

        // Swipe-to-delete
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = adapter.currentAt(pos) ?: return
                    viewLifecycleOwner.lifecycleScope.launch { vm.delete(item) }
                }
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(vb.rv)

        // FAB -> mở màn thêm/sửa
        vb.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_edit)
        }
        // Ẩn/hiện FAB khi cuộn
        vb.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 6) vb.fabAdd.hide() else if (dy < -6) vb.fabAdd.show()
            }
        })

        // Quan sát danh sách mặc định (không lọc ngày) để có dữ liệu nền
        vm.items.observe(viewLifecycleOwner) { list ->
            if (!isFiltering()) {
                baseList = list
                // nếu không có keyword -> render toàn bộ; nếu có -> áp dụng search cục bộ cho đồng nhất
                if (currentKeyword.isBlank()) {
                    adapter.submit(list)
                } else {
                    renderFilteredAndSearched()
                }
            }
        }

        // Menu Toolbar (Search + Stats + Manage Categories + dynamic Date Filter)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // menu gốc (search/stats/manage) từ XML
                menuInflater.inflate(R.menu.menu_list, menu)

                // ---- Thêm 2 mục menu động cho lọc ngày ----
                menu.add(0, ID_FILTER_DATE, 0, "Lọc theo ngày")
                    .setIcon(android.R.drawable.ic_menu_month)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

                menu.add(0, ID_CLEAR_FILTER, 1, "Xoá lọc")
                    .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

                // ---- Search giữ nguyên hành vi cũ khi KHÔNG lọc ngày ----
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search_hint)
                // khôi phục text nếu có
                if (currentKeyword.isNotBlank()) {
                    searchItem.expandActionView()
                    searchView?.setQuery(currentKeyword, false)
                    searchView?.clearFocus()
                }
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        currentKeyword = query.orEmpty()
                        applySearch(currentKeyword)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        currentKeyword = newText.orEmpty()
                        applySearch(currentKeyword)
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_stats -> {
                        findNavController().navigate(R.id.action_list_to_stats); true
                    }
                    R.id.action_manage_categories -> {
                        findNavController().navigate(R.id.categoryManagerFragment); true
                    }
                    ID_FILTER_DATE -> {
                        pickDateRange(); true
                    }
                    ID_CLEAR_FILTER -> {
                        clearDateFilter(); true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return vb.root
    }

    // ====== SEARCH ======
    private fun applySearch(q: String) {
        val key = q.trim()
        if (!isFiltering()) {
            // hành vi cũ: dùng DAO khi KHÔNG lọc ngày
            if (key.isEmpty()) {
                vm.items.observe(viewLifecycleOwner) { adapter.submit(it) }
            } else {
                vm.search(key).observe(viewLifecycleOwner) { adapter.submit(it) }
            }
        } else {
            // đang lọc ngày: search CỤC BỘ trên baseList
            renderFilteredAndSearched()
        }
    }

    private fun renderFilteredAndSearched() {
        val kw = currentKeyword.trim().lowercase(Locale.ROOT)
        val list = if (kw.isEmpty()) baseList
        else baseList.filter { it.title.lowercase(Locale.ROOT).contains(kw) || it.category.lowercase(Locale.ROOT).contains(kw) }
        adapter.submit(list)
    }

    // ====== DATE RANGE FILTER ======
    private fun pickDateRange() {
        val cal = Calendar.getInstance()
        // step 1: from
        DatePickerDialog(requireContext(), { _, y, m, d ->
            fromDay = startOfDayMillis(y, m, d)
            // step 2: to
            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                toDay = endOfDayMillis(y2, m2, d2)
                applyDateFilter()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun applyDateFilter() {
        val f = fromDay; val t = toDay
        if (f == null || t == null) return
        val from = min(f, t)
        val to = max(f, t)

        // lấy danh sách nền từ vm.items và lọc theo date tại chỗ (không cần sửa ViewModel/DAO)
        vm.items.observe(viewLifecycleOwner) { full ->
            baseList = full.filter { it.date in from..to }
            renderFilteredAndSearched()
        }

        Toast.makeText(requireContext(), "Lọc: ${fmtDate(from)} – ${fmtDate(to)}", Toast.LENGTH_SHORT).show()
    }

    private fun clearDateFilter() {
        fromDay = null; toDay = null
        // quay lại nguồn mặc định
        vm.items.observe(viewLifecycleOwner) { list ->
            baseList = list
            renderFilteredAndSearched() // sẽ dùng keyword hiện tại (nếu rỗng thì ra full)
        }
        Toast.makeText(requireContext(), "Đã xoá lọc ngày", Toast.LENGTH_SHORT).show()
    }

    private fun isFiltering() = (fromDay != null && toDay != null)

    // ====== DATE HELPERS ======
    private fun startOfDayMillis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance().apply {
            set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
    private fun endOfDayMillis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance().apply {
            set(y, m, d, 23, 59, 59); set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }
    private fun fmtDate(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val dd = c.get(Calendar.DAY_OF_MONTH)
        val mm = c.get(Calendar.MONTH) + 1
        val yy = c.get(Calendar.YEAR)
        return "%02d/%02d/%04d".format(dd, mm, yy)
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}

package com.example.chitieuplus.ui.category

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chitieuplus.R
import com.example.chitieuplus.data.AppDatabase
import com.example.chitieuplus.data.CategoryEntity
import com.example.chitieuplus.data.TransactionType
import com.example.chitieuplus.databinding.FragmentCategoryManagerBinding
import com.example.chitieuplus.viewmodel.CategoryVMFactory
import com.example.chitieuplus.viewmodel.CategoryViewModel
import com.google.android.material.tabs.TabLayout

class CategoryManagerFragment : Fragment() {

    private var _vb: FragmentCategoryManagerBinding? = null
    private val vb get() = _vb!!

    private lateinit var adapter: CategoryAdapter

    private val vm: CategoryViewModel by viewModels {
        val dao = AppDatabase.get(requireContext()).categoryDao()
        CategoryVMFactory(requireActivity().application, dao)
    }

    private var currentType: TransactionType = TransactionType.EXPENSE

    // Giữ LiveData hiện tại để tháo/gắn khi đổi tab
    private var currentLiveData: LiveData<List<CategoryEntity>>? = null
    private val listObserver = Observer<List<CategoryEntity>> { list ->
        adapter.submitList(list)           // ✅ Sửa ở đây
        vb.tvEmpty.isVisible = list.isEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentCategoryManagerBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = CategoryAdapter(
            onEdit = { item -> promptRename(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        vb.rv.layoutManager = LinearLayoutManager(requireContext())
        vb.rv.adapter = adapter

        // TabLayout: 0 = Chi, 1 = Thu
        setupTabs()

        // Mặc định mở tab Chi
        switchType(TransactionType.EXPENSE)

        // FAB: thêm danh mục theo tab hiện tại
        vb.fabAdd.setOnClickListener { promptAdd() }
    }

    private fun setupTabs() {
        // Nếu bạn đã khai báo TabItem trong XML, có thể bỏ phần addTab thủ công
        if (vb.tabType.tabCount == 0) {
            vb.tabType.addTab(vb.tabType.newTab().setText(getString(R.string.expense)))
            vb.tabType.addTab(vb.tabType.newTab().setText(getString(R.string.income)))
        }

        vb.tabType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val type =
                    if (tab.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
                switchType(type)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun switchType(type: TransactionType) {
        if (type == currentType && currentLiveData != null) return
        currentType = type

        // Tháo observer cũ
        currentLiveData?.removeObserver(listObserver)

        // Gắn observer mới
        val live = vm.observeByType(type)
        currentLiveData = live
        live.observe(viewLifecycleOwner, listObserver)

        // Đồng bộ chọn tab
        val idx = if (type == TransactionType.EXPENSE) 0 else 1
        if (vb.tabType.selectedTabPosition != idx) {
            vb.tabType.getTabAt(idx)?.select()
        }
    }

    private fun promptAdd() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.add_category)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_category))
            .setView(input)
            .setPositiveButton(R.string.add) { dlg, _ ->
                val name = input.text?.toString().orEmpty()
                vm.add(name, currentType)
                dlg.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun promptRename(item: CategoryEntity) {
        val input = EditText(requireContext()).apply {
            setText(item.name)
            setSelection(item.name.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cd_edit_category))
            .setView(input)
            .setPositiveButton(R.string.save) { dlg, _ ->
                val newName = input.text?.toString().orEmpty()
                vm.rename(item.id, newName)
                dlg.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: CategoryEntity) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.cd_delete_category))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                vm.deleteHard(item.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        currentLiveData?.removeObserver(listObserver)
        _vb = null
        super.onDestroyView()
    }
}

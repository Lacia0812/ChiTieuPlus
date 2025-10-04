package com.example.chitieuplus.ui.list

import android.os.Bundle
import android.view.*
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
import com.example.chitieuplus.databinding.FragmentTransactionListBinding
import com.example.chitieuplus.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentTransactionListBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Recycler
        vb.rv.layoutManager = LinearLayoutManager(requireContext())
        vb.rv.adapter = adapter

        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
        }

        // Vuốt để xoá
        val swipe = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
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

        // Menu Toolbar (Search + Stats + Manage Categories)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_list, menu)

                // Search
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search_hint)
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        applySearch(query.orEmpty()); return true
                    }
                    override fun onQueryTextChange(newText: String?): Boolean {
                        applySearch(newText.orEmpty()); return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_stats -> {
                        findNavController().navigate(R.id.action_list_to_stats); true
                    }
                    R.id.action_manage_categories -> {
                        // điều hướng tới màn quản lý danh mục
                        findNavController().navigate(R.id.categoryManagerFragment); true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun applySearch(q: String) {
        val key = q.trim()
        if (key.isEmpty()) {
            vm.items.observe(viewLifecycleOwner) { adapter.submit(it) }
        } else {
            vm.search(key).observe(viewLifecycleOwner) { adapter.submit(it) }
        }
    }

    override fun onDestroyView() {
        _vb = null
        super.onDestroyView()
    }
}

package com.bonhams.expensemanagement.ui.FinanceManger

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonhams.expensemanagement.R
import com.bonhams.expensemanagement.adapters.ClaimsAdapter
import com.bonhams.expensemanagement.adapters.ClaimsLoadStateAdapter
import com.bonhams.expensemanagement.adapters.FinanceRequestListAdapter
import com.bonhams.expensemanagement.adapters.FinanceRequestLoadStateAdapter
import com.bonhams.expensemanagement.data.model.ClaimDetail
import com.bonhams.expensemanagement.data.services.ApiHelper
import com.bonhams.expensemanagement.data.services.RetrofitBuilder
import com.bonhams.expensemanagement.databinding.FragmentClaimsBinding
import com.bonhams.expensemanagement.databinding.FragmentFinanceMangerRequestBinding
import com.bonhams.expensemanagement.ui.BaseActivity
import com.bonhams.expensemanagement.ui.claims.claimDetail.ClaimDetailFragment
import com.bonhams.expensemanagement.ui.claims.newClaim.NewClaimFragment
import com.bonhams.expensemanagement.ui.home.HomeViewModel
import com.bonhams.expensemanagement.ui.home.HomeViewModelFactory
import com.bonhams.expensemanagement.ui.main.MainActivity
import com.bonhams.expensemanagement.ui.main.MainViewModel
import com.bonhams.expensemanagement.utils.RefreshPageListener
import com.bonhams.expensemanagement.utils.Utils.Companion.showKeyboard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

class FinanceManegerRequestFragment : Fragment(), FinanceRequestListAdapter.OnClaimClickListener, RefreshPageListener {

    private val TAG = javaClass.simpleName
    private var contextActivity: BaseActivity? = null
    private lateinit var claimsAdapter: FinanceRequestListAdapter
    private lateinit var viewModel: FinaanceMangerViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentFinanceMangerRequestBinding
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_finance_manger_request, container, false)
        val view = binding.root
        binding.lifecycleOwner = this
        contextActivity = activity as? BaseActivity

        setupViewModel()
        initSwipeToRefresh()
        initSearch()


        return view
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            FinancesViewModelFactory(this, ApiHelper(RetrofitBuilder.apiService))
        ).get(FinaanceMangerViewModel::class.java)

        homeViewModel = ViewModelProvider(requireActivity(),
            HomeViewModelFactory(ApiHelper(RetrofitBuilder.apiService))
        ).get(HomeViewModel::class.java)

        homeViewModel.datePicker.observe(viewLifecycleOwner, {
            Log.d(TAG, "setupViewModel: datePicker: $it")
            updatedClaimsFromStatus(null, it)
        })

        homeViewModel.statusPicker.observe(viewLifecycleOwner, {
            Log.d(TAG, "setupViewModel: statusPicker: $it")
            updatedClaimsFromStatus(it as String?, null)
        })


        mainViewModel.appbarSearchClick?.observe(viewLifecycleOwner, {
            Log.d(TAG, "setupViewModel: appbarSearchClick: $it")
            if(it){
                binding.tilSearchClaim.visibility = View.VISIBLE
                binding.edtSearchClaim.showKeyboard(contextActivity, true)
            }
            else{
                binding.tilSearchClaim.visibility = View.GONE
                binding.edtSearchClaim.setText("")
                binding.edtSearchClaim.showKeyboard(contextActivity, false)
            }
        })
    }


    override fun onResume() {
        super.onResume()
        initAdapter()
    }



    private fun initAdapter() {
        claimsAdapter = FinanceRequestListAdapter(requireActivity())
        claimsAdapter.setupClaimListener(this)
        binding.recyclerView.adapter = claimsAdapter.withLoadStateHeaderAndFooter(
            header = FinanceRequestLoadStateAdapter(claimsAdapter),
            footer = FinanceRequestLoadStateAdapter(claimsAdapter)
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(contextActivity)
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        claimsAdapter.addLoadStateListener { loadState ->
            if (loadState.source.refresh is LoadState.NotLoading
                && loadState.append.endOfPaginationReached && claimsAdapter.itemCount < 1) {
                binding.mNoResult.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.mNoResult.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        lifecycleScope.launchWhenCreated {
            claimsAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.swipeRefresh.isRefreshing = loadStates.mediator?.refresh is LoadState.Loading
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.claims.collectLatest {
                claimsAdapter.submitData(it)
                claimsAdapter.notifyDataSetChanged()
            }
        }

        lifecycleScope.launchWhenCreated {
            claimsAdapter.loadStateFlow
                // Use a state-machine to track LoadStates such that we only transition to
                // NotLoading from a RemoteMediator load if it was also presented to UI.
//                .asMergedLoadStates()
                // Only emit when REFRESH changes, as we only want to react on loads replacing the
                // list.
                .distinctUntilChangedBy { it.refresh }
                // Only react to cases where REFRESH completes i.e., NotLoading.
                .filter { it.refresh is LoadState.NotLoading }
                // Scroll to top is synchronous with UI updates, even if remote load was triggered.
//                .collect { binding.recyclerView.scrollToPosition(0) }
        }
    }

    private fun initSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.resetFilters()
            homeViewModel.resetFilters()
            claimsAdapter.refresh()
        }
    }

    private fun initSearch() {
        binding.edtSearchClaim.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                updatedClaimsFromInput()
                true
            } else {
                false
            }
        }
        binding.edtSearchClaim.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updatedClaimsFromInput()
                true
            } else {
                false
            }
        }
    }

    private fun updatedClaimsFromInput() {
        binding.edtSearchClaim.text!!.trim().toString().let {
            if (/*it.isNotBlank() &&*/ viewModel.shouldShowClaimList(it)) {
                viewModel.showClaimsList(it)
                binding.edtSearchClaim.showKeyboard(contextActivity, false)
            }
        }
    }

    private fun updatedClaimsFromStatus(status: String?, date: Any?) {
        binding.edtSearchClaim.text!!.trim().toString().let {
            viewModel.showClaimsList(it, status, date)
        }
    }

    override fun onClaimItemClicked(claim: ClaimDetail?, position: Int) {
        Log.d(TAG, "onClaimItemClicked: $position claim: $claim")
        val fragment = ClaimDetailFragment()
        fragment.setClaimDetails(claim)
        (contextActivity as? MainActivity)?.addFragment(fragment)
    }

    override fun onClaimCreateCopyClicked(claim: ClaimDetail?, position: Int) {
        Log.d(TAG, "onClaimCreateCopyClicked: $position")
        val fragment = NewClaimFragment()
        fragment.setClaimDetails(claim)
        fragment.setRefreshPageListener(this)
        (contextActivity as? MainActivity)?.addFragment(fragment)
    }

    override fun refreshPage() {
        if(this::claimsAdapter.isInitialized){
            claimsAdapter.refresh()
        }
    }
}

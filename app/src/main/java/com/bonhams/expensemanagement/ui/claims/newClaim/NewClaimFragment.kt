package com.bonhams.expensemanagement.ui.claims.newClaim

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonhams.expensemanagement.R
import com.bonhams.expensemanagement.adapters.AttachmentsAdapter
import com.bonhams.expensemanagement.adapters.CustomSpinnerAdapter
import com.bonhams.expensemanagement.data.model.*
import com.bonhams.expensemanagement.data.model.Currency
import com.bonhams.expensemanagement.data.services.ApiHelper
import com.bonhams.expensemanagement.data.services.RetrofitBuilder
import com.bonhams.expensemanagement.data.services.requests.NewClaimRequest
import com.bonhams.expensemanagement.data.services.responses.CommonResponse
import com.bonhams.expensemanagement.data.services.responses.DropdownResponse
import com.bonhams.expensemanagement.databinding.FragmentNewClaimBinding
import com.bonhams.expensemanagement.ui.BaseActivity
import com.bonhams.expensemanagement.ui.claims.splitClaim.SplitClaimFragment
import com.bonhams.expensemanagement.ui.main.MainActivity
import com.bonhams.expensemanagement.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.lassi.common.utils.KeyUtils
import com.lassi.data.media.MiMedia
import com.lassi.domain.media.LassiOption
import com.lassi.domain.media.MediaType
import com.lassi.presentation.builder.Lassi
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.imaginativeworld.oopsnointernet.utils.NoInternetUtils
import java.io.*
import java.util.*
class NewClaimFragment() : Fragment() ,RecylerCallback{

    private val TAG = javaClass.simpleName
    private var contextActivity: BaseActivity? = null
    private lateinit var claimDetail: ClaimDetail
    private lateinit var viewModel: NewClaimViewModel
    private lateinit var binding: FragmentNewClaimBinding

    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private lateinit var refreshPageListener: RefreshPageListener
    private var shouldRefreshPage: Boolean = false
    private var expenseCode: String = ""
    private var mtaxcodeId: String = ""
    private var taxcodeId: String = ""
    private var compnyId: Int = 0
    private var currencyCode: String = ""
    private var currencySymbol: String = ""
    private var companyDateFormate: String = ""
    private var companyLocation: String = ""
    private var isCreateCopy: Boolean = false
    var groupname="n/a"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_new_claim, container, false)
        val view = binding.root
        binding.lifecycleOwner = this
        contextActivity = activity as? BaseActivity

        setupViewModel()
        setClickListeners()
        setupAttachmentRecyclerView()
        setDropdownDataObserver()
        setupView()
        setupTextWatcher()
        return view
    }

    fun setClaimDetails(detail: ClaimDetail?){
        detail?.let {
            claimDetail = it
        }
    }

    fun setRefreshPageListener(refreshListener: RefreshPageListener){
        refreshPageListener = refreshListener
    }

    private fun setupView(){

        try {
            if (this::claimDetail.isInitialized) {
                isCreateCopy=true

                binding.edtMerchantName.setText(
                    claimDetail.merchant.replaceFirstChar(Char::uppercase) ?: claimDetail.merchant
                )
                binding.tvDateOfSubmission.text = if (!claimDetail.createdOn.trim().isNullOrEmpty())
                    Utils.getFormattedDate(claimDetail.createdOn, Constants.YYYY_MM_DD_SERVER_RESPONSE_FORMAT,companyDateFormate
                    ) else ""

                binding.edtDescription.setText(claimDetail.description)
                if (!claimDetail.attachments.isNullOrEmpty() && claimDetail.attachments.trim()
                        .isNotEmpty()
                ) {
                    val attachment=claimDetail.attachments.split(",")
                    viewModel.attachmentsList.clear()
                }

            }else{
                binding.edtMerchantName.setText(AppPreferences.ledgerId)
                viewModel.attachmentsList.clear()
            }
            refreshAttachments()
        }
        catch (error: Exception){
            Log.e(TAG, "setupView: ${error.message}")
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity(),
            NewClaimViewModelFactory(ApiHelper(RetrofitBuilder.apiService))
        ).get(NewClaimViewModel::class.java)
    }

    private fun setClickListeners(){
        binding.edtGroupValue.setOnClickListener {
            binding.spnExpenseGroup.performClick()
        }
        binding.edtExpenceTypeValue.setOnClickListener {
            binding.spnExpenseType.performClick()
        }

        binding.tvUploadPic.setOnClickListener(View.OnClickListener {
            showBottomSheet()
        })
        binding.ivPicUpload.setOnClickListener(View.OnClickListener {
            showBottomSheet()
        })

        binding.tvDateOfSubmission.setOnClickListener(View.OnClickListener {
            showCalenderDialog()
        })

        binding.btnSplit.setOnClickListener(View.OnClickListener {
            splitNewClaim()
        })

        binding.btnSubmit.setOnClickListener(View.OnClickListener {
            contextActivity?.let {
                if(NoInternetUtils.isConnectedToInternet(it))
                    createNewClaim()
                else
                    Toast.makeText(it, getString(R.string.check_internet_msg), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSpinners(){
       // Tax Adapter
        val taxAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.taxList
        )
        binding.spntaxcode.adapter = taxAdapter
        binding.spntaxcode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }

        // Company List Adapter
        when (AppPreferences.userType) {
            "Reporting Manager" -> { }
            "Finance Department" -> { }
            "Admin" -> { }
            "Final Approver" -> { }
            else->{
                binding.edtTax.isEnabled=false
                viewModel.companyList.forEach {
            if(AppPreferences.company == it.name){
                viewModel.companyList= listOf(it)
                return@forEach
            }
        }
            }
        }



        val companyAdapter = CustomSpinnerAdapter(requireContext(), R.layout.item_spinner, viewModel.companyList)
        binding.spnCompanyNumber.adapter = companyAdapter
        var compnypostion=0
        viewModel.companyList.forEachIndexed { index, element ->

            if(AppPreferences.company == element.name){
                compnypostion=index
                return@forEachIndexed
            }
        }
        binding.spnCompanyNumber.setSelection(compnypostion)

        binding.spnCompanyNumber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                compnyId=viewModel.companyList[position].id.toInt()
                companyDateFormate=viewModel.companyList[position].dateFormat
                companyLocation=viewModel.companyList[position].location
                binding.spnExpenseGroup.adapter=null

                if(isCreateCopy){
                    setupExpenceGroupTypeCopy(claimDetail.expenseGroupID)

                    binding.edtTotalAmount.setText(claimDetail.totalAmount)
                    binding.edtTax.setText(claimDetail.tax)
                    binding.tvNetAmount.setText(claimDetail.netAmount)
                    binding.tvDateOfSubmission.setText("")
                    binding.edtGroupValue.setText(claimDetail.expenseGroupName)
                    binding.edtExpenceTypeValue.setText(claimDetail.expenseTypeName)

                }else {
                    setupExpenceGroupType(true)

                    binding.edtTotalAmount.setText("")
                    binding.edtTax.setText("")
                    binding.tvNetAmount.setText("")
                    binding.tvDateOfSubmission.text = ""
                    binding.edtGroupValue.setText("")

                }

                viewModel.departmentList.clear()
                viewModel.departmentListCompany.forEach {
                    if(it.company_id == compnyId.toString()){
                        viewModel.departmentList.add(it)
                    }
                }
                setupDeparmentType()
                viewModel.currencyList.forEach {
                    if(it.id.toInt()==viewModel.companyList[position].currency_type_id){
                        val symbol=it.symbol
                        val code=it.code
                        currencyCode=code
                        currencySymbol=symbol
                        binding.tvTotalAmountCurrency.text = symbol
                        binding.tvTaxAmountCurrency.text = symbol
                        binding.tvNetAmountCurrency.text = symbol
                        if(isCreateCopy){
                            val currency: Currency? = viewModel.currencyList.find { it.id == claimDetail.currencyTypeID }
                            val currencyPos = viewModel.currencyList.indexOf(currency)
                            if (currencyPos >= 0) {
                                binding.spnCurrency.setSelection(currencyPos)
                            }
                        }else {
                            val currency: Currency? =
                                viewModel.currencyList.find { it.id.toInt() == viewModel.companyList[position].currency_type_id }
                            val currencyPos = viewModel.currencyList.indexOf(currency)
                            if (currencyPos >= 0) {
                                binding.spnCurrency.setSelection(currencyPos)
                            }
                        }
                    }
                }
                binding.edtTitle.setText(viewModel.companyList[position].code)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Nothing selected", Toast.LENGTH_SHORT).show();

            }
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }



        // Currency Adapter
        val currencyAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.currencyList
        )
        binding.spnCurrency.adapter = currencyAdapter

        binding.spnCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val code = viewModel.currencyList[position].code
                val symbol = viewModel.currencyList[position].symbol
                currencyCode=code
                currencySymbol=symbol
                binding.tvTotalAmountCurrency.text = symbol
                binding.tvTaxAmountCurrency.text = symbol
                binding.tvNetAmountCurrency.text = symbol

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Nothing selected", Toast.LENGTH_SHORT).show();

            }
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }

        if(this::claimDetail.isInitialized){
            try {
                val company: Company? =
                    viewModel.companyList.find { it.name == claimDetail.companyName }
                val companyPos = viewModel.companyList.indexOf(company)
                if (companyPos >= 0) {
                    binding.spnCompanyNumber.setSelection(companyPos)
                }

                val department: Department? =
                    viewModel.departmentList.find { it.name == claimDetail.department }
                val departmentPos = viewModel.departmentList.indexOf(department)
                if (departmentPos >= 0) {
                    binding.spnDepartment.setSelection(departmentPos)
                }

                val currency: Currency? = viewModel.currencyList.find { it.id == claimDetail.currencyTypeID }
                val currencyPos = viewModel.currencyList.indexOf(currency)
                if (currencyPos >= 0) {
                    binding.spnCurrency.setSelection(currencyPos)
                }

                binding.edtTotalAmount.setText(String.format("%.2f", claimDetail.totalAmount.toDouble()))
                binding.edtTax.setText(String.format("%.2f", claimDetail.tax.toDouble()))
                binding.tvNetAmount.setText(String.format("%.2f", claimDetail.netAmount.toDouble()))
                binding.edtAutionValue.setText(claimDetail.auction.toString())
                binding.tvAuctionExpCode.text = claimDetail.expenseCode

            }
            catch (e: Exception){
                Log.e(TAG, "setupSpinners: ${e.message}")
            }
        }
    }

    private fun setupExpenceGroupType(isShowDefault:Boolean){
        var isDefaultshow=isShowDefault
        // Expense Group Adapter
        val expenseGroupAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.expenseGroupList
        )
        binding.spnExpenseGroup.adapter = expenseGroupAdapter
        binding.spnExpenseGroup.isSelected=false
        binding.spnExpenseGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if(!isDefaultshow) {
                    binding.edtGroupValue.setText(viewModel.expenseGroupList[position].name)
                    val groupid = viewModel.expenseGroupList[position].id
                     groupname = viewModel.expenseGroupList[position].name
                    if(groupname == "Capital Asset"){
                        viewModel.departmentList.clear()
                        viewModel.departmentListCompany.forEach {
                            if(it.name == "Tangible Assets"&&it.company_id==compnyId.toString() ){
                                viewModel.departmentList.add(it)
                                return@forEach
                            }
                        }
                        binding.spnDepartment.setBackgroundResource(R.drawable.spinner_bg)

                        setupDeparmentType()
                    }else{
                         viewModel.departmentList.clear()
                    viewModel.departmentListCompany.forEach {
                    if(it.company_id == compnyId.toString()){
                        viewModel.departmentList.add(it)
                       }
                       }
                        binding.spnDepartment.setBackgroundResource(R.drawable.spinner_purple_bg)

                      setupDeparmentType()
                    }
                    viewModel.expenseTypeList.clear()
                    binding.edtExpenceTypeValue.setText(" ")
                    viewModel.expenseTypeList.add(ExpenseType("0","Select Expense Type",""))
                    viewModel.expenseTypeListExpenseGroup.forEach {
                            if (it.expenseGroupID == groupid) {
                            viewModel.expenseTypeList.add(it)
                        } else if (it.companyID.isNullOrEmpty()) {


                        }

                    }
                    setupExpenceType(true)
                }else{
                    isDefaultshow=false
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Nothing selected", Toast.LENGTH_SHORT).show();

            }
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }
    }

    private fun setupExpenceGroupTypeCopy(groupId:String){
        // Expense Group Adapter
        val expenseGroupAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.expenseGroupList
        )
        binding.spnExpenseGroup.adapter = expenseGroupAdapter
        val expenseGroup: ExpenseGroup? =
            viewModel.expenseGroupList.find { it.id == groupId }
        val expenseGroupPos = viewModel.expenseGroupList.indexOf(expenseGroup)
        if (expenseGroupPos >= 0) {
            binding.spnExpenseGroup.setSelection(expenseGroupPos)
        }
        binding.spnExpenseGroup.isSelected=false
        binding.spnExpenseGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    binding.edtGroupValue.setText(viewModel.expenseGroupList[position].name)
                    val groupid = viewModel.expenseGroupList[position].id
                val groupname = viewModel.expenseGroupList[position].name
                if(groupname == "Capital Asset"){
                    viewModel.departmentList.clear()
                    viewModel.departmentListCompany.forEach {
                        if(it.name == "Tangible Assets"&&it.company_id==compnyId.toString() ){
                            viewModel.departmentList.add(it)
                            return@forEach
                        }
                    }
                    binding.spnDepartment.setBackgroundResource(R.drawable.spinner_bg)

                    setupDeparmentType()
                }else{
                    viewModel.departmentList.clear()
                    viewModel.departmentListCompany.forEach {
                        if(it.company_id == compnyId.toString()){
                            viewModel.departmentList.add(it)
                        }
                    }
                    binding.spnDepartment.setBackgroundResource(R.drawable.spinner_purple_bg)

                    setupDeparmentType()
                }


                    viewModel.expenseTypeList.clear()
                    binding.edtExpenceTypeValue.setText(" ")
                    viewModel.expenseTypeList.add(ExpenseType("0","Select Expense Type",""))
                    viewModel.expenseTypeListExpenseGroup.forEach {
                        if (it.expenseGroupID == groupid) {
                            viewModel.expenseTypeList.add(it)
                        } else if (it.companyID.isNullOrEmpty()) {
                        }

                    }
                    setupExpenceType(false)

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(requireContext(), "Nothing selected", Toast.LENGTH_SHORT).show();

            }
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }
    }


    private fun setupExpenceType(isShowDefault:Boolean){
        var isDefaultShow=isShowDefault
        // Expense Type Adapter
        println("call expence type method ")

        val expenseTypeAdapter = CustomSpinnerAdapter(
                requireContext(),
                R.layout.item_spinner,
                viewModel.expenseTypeList
            )
            binding.spnExpenseType.adapter = expenseTypeAdapter
        binding.spnExpenseType.isSelected=false

        if(isCreateCopy){
             var expenseTypePos = 0
            viewModel.expenseTypeList.forEachIndexed { index, expenseType ->

                if(expenseType.name==claimDetail.expenseTypeName){
                    expenseTypePos=index
                }
                }
                if (expenseTypePos >= 0) {
                    binding.spnExpenseType.setSelection(expenseTypePos)
                }
             }


        binding.spnExpenseType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if(!isDefaultShow) {
                    binding.edtExpenceTypeValue.setText(viewModel.expenseTypeList[position].name)

                    expenseCode = viewModel.expenseTypeList[position].activityCode
                    mtaxcodeId = viewModel.expenseTypeList[position].taxCodeID

                    setupTax()
                    if (binding.edtAutionValue.text.toString().isNotEmpty()) {
                        binding.tvAuctionExpCode.text = expenseCode

                    } else {
                        binding.tvAuctionExpCode.text = ""
                    }
                }else{
                    isDefaultShow=false
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }
    }
    private fun setupDeparmentType(){
        // Department Adapter
        val departmentAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.departmentList
        )
        binding.spnDepartment.adapter = departmentAdapter
       var postion=0
        viewModel.departmentList.forEachIndexed { index, element ->

            if(AppPreferences.departmentID == element.id){
                postion=index
                return@forEachIndexed
            }
        }
        binding.spnDepartment.setSelection(postion)

    }

    private fun setupTax(){

        when (AppPreferences.userType) {
            "Reporting Manager" -> { }
            "Finance Department" -> { }
            "Admin" -> { }
            "Final Approver" -> { }
            else->{
                viewModel.taxList.forEach {
                    if(it.id.toString() == mtaxcodeId){
                        viewModel.taxList= listOf(it)
                        return@forEach
                    }
                }
            }
        }
        viewModel.taxList.forEach {

            if(it.id.toString() == mtaxcodeId) {


                binding.edtTaxcode.setText(it.tax_code)

            }
        }

        val taxAdapter = CustomSpinnerAdapter(
            requireContext(),
            R.layout.item_spinner,
            viewModel.taxList
        )
        binding.spntaxcode.adapter = taxAdapter

        var postion=0
        viewModel.taxList.forEachIndexed { index, element ->

            if(element.id.toString() == mtaxcodeId) {

                postion=index
                return@forEachIndexed
                //  binding.edtTaxcode.setText(it.tax_code)

            }
        }
        binding.spntaxcode.setSelection(postion)

        binding.spntaxcode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener,
            View.OnFocusChangeListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                taxcodeId= viewModel.taxList[position].id.toString()

            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
            override fun onFocusChange(v: View?, hasFocus: Boolean) {}
        }
    }
    private fun setupTextWatcher(){

     //   binding.edtTotalAmount.addTextChangedListener(NumberTextWatcher(binding.edtTotalAmount, "#,###"))
        binding.edtTotalAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(binding.edtTotalAmount.text.isNotEmpty()){
                    if(binding.edtTax.text.isEmpty()){
                        updateNetAmount(binding.edtTotalAmount.text.toString(), "0")

                    }else{
                        updateNetAmount(binding.edtTotalAmount.text.toString(), binding.edtTax.text.toString())

                    }
                }

            }
        })

        binding.edtTax.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(binding.edtTotalAmount.text.isNotEmpty()&&binding.edtTax.text.isNotEmpty())
                updateNetAmount(binding.edtTotalAmount.text.toString(), binding.edtTax.text.toString())
            }
        })
        binding.edtAutionValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
              if(binding.edtAutionValue.text.toString().isNotEmpty()){
                  binding.tvAuctionExpCode.text = expenseCode

              }else{
                  binding.tvAuctionExpCode.text = ""
              }
            }
        })
    }

    private fun setupAttachmentRecyclerView(){
        val linearLayoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvAttachments.layoutManager = linearLayoutManager
        attachmentsAdapter = AttachmentsAdapter(viewModel.attachmentsList,"claim",this)
        binding.rvAttachments.adapter = attachmentsAdapter
    }

    private fun refreshAttachments(){
        if(viewModel.attachmentsList.size > 0){
            binding.tvNoFileSelected.visibility = View.GONE
            binding.rvAttachments.visibility = View.VISIBLE
            attachmentsAdapter.notifyDataSetChanged()
        }
        else{
            binding.rvAttachments.visibility = View.GONE
            binding.tvNoFileSelected.visibility = View.VISIBLE
        }
    }

    private fun updateNetAmount(total: String, tax: String){
        try {
            var totalAmount = 0.0
            var taxAmount = 0.0

            if (!total.isNullOrEmpty()) {
                totalAmount = total.toDouble()
            }
            if (!tax.isNullOrEmpty()) {
                taxAmount = tax.toDouble()
            }

            if(taxAmount>totalAmount){
                Toast.makeText(contextActivity, "The tax amount must not be greater than the total amount", Toast.LENGTH_SHORT).show()

                binding.tvNetAmount.setText("")

            }else {
                val netAmount = totalAmount - taxAmount

                if(netAmount<0){
                    Toast.makeText(contextActivity, "Net amount should be greater than 0", Toast.LENGTH_SHORT).show()
                    binding.tvNetAmount.setText("")

                }else{
                    binding.tvNetAmount.setText(String.format("%.2f", netAmount))

                }

                // binding.tvNetAmount.text = "$netAmount"
                //binding.tvNetAmount.text =
            }
        }
        catch (error: Exception){
            Log.e(TAG, "updateNetAmount: ${error.message}")
        }
    }

    private fun setDropdownDataObserver() {
        viewModel.getDropDownData().observe(viewLifecycleOwner, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            try {
                                Log.d(TAG, "setChangePasswordObserver: ${resource.status}")
                                initializeSpinnerData(response)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Status.ERROR -> {
                        Log.e(TAG, "setChangePasswordObserver: ${it.message}")
                        it.message?.let { it1 -> Toast.makeText(contextActivity, it1, Toast.LENGTH_SHORT).show() }
                    }
                    Status.LOADING -> {
//                        binding.mProgressBars.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun initializeSpinnerData(dropdownResponse: DropdownResponse){
        viewModel.expenseGroupList.clear()
       viewModel.expenseGroupList.add(ExpenseGroup("0","Select Expense Group","0","active"))

        ( dropdownResponse.expenseGroup as MutableList<ExpenseGroup>).forEachIndexed { index, expenseGroup ->

            if(!expenseGroup.name.contains("Mileage")){
                viewModel.expenseGroupList.add(expenseGroup)
            }
        }
        viewModel.expenseTypeListExpenseGroup = dropdownResponse.expenseType
        viewModel.departmentListCompany = dropdownResponse.departmentList
        viewModel.currencyList  = dropdownResponse.currencyType as MutableList<Currency>
        viewModel.carTypeList  = dropdownResponse.carType
        viewModel.statusTypeList  = dropdownResponse.statusType
        viewModel.mileageTypeList  = dropdownResponse.mileageType
        viewModel.companyList  = dropdownResponse.companyList
        viewModel.taxList  = dropdownResponse.tax

        setupSpinners()
    }

    private fun setCreateClaimObserver(newClaimRequest: NewClaimRequest) {

        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("claim_type", "E")

        for (photoPath in viewModel.attachmentsList) {
            if (photoPath != null) {
                val images = File(photoPath)
                if (images.exists()) {
                    //val bitmap = BitmapFactory.decodeFile(photoPath)
                  // val imgFile= bitmapToFile(bitmap,images.name)
                    //builder.addFormDataPart("claimImage", images.name, RequestBody.create(MultipartBody.FORM,imgFile))
                   builder.addFormDataPart("claimImage", images.name, RequestBody.create(MultipartBody.FORM, images))
                }
            }
        }
        val mrequestBody: RequestBody = builder.build()

        viewModel.uploadClaimAttachement(mrequestBody).observe(viewLifecycleOwner, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            try {
                                newClaimRequest.attachments=response.images
                                callApiCreateClaim(newClaimRequest)


                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Status.ERROR -> {
                        binding.mProgressBars.visibility = View.GONE
                        binding.btnSubmit.visibility = View.VISIBLE
                        Log.e(TAG, "setChangePasswordObserver: ${it.message}")
                        it.message?.let { it1 -> Toast.makeText(contextActivity, it1, Toast.LENGTH_SHORT).show() }
                    }
                    Status.LOADING -> {
                        binding.mProgressBars.visibility = View.VISIBLE
                    }
                }
            }
        })


    }
    fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? { // File name like "image.png"
        //create a file to write bitmap data
        var file: File? = null
        return try {
            file = File(Environment.getExternalStorageDirectory().toString() + File.separator + fileNameToSave)
            file?.createNewFile()

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // YOU can also save it in JPEG
            val bitmapdata = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }
    private fun callApiCreateClaim(newClaimRequest: NewClaimRequest){
         val splitOne = SplitClaimDetail(newClaimRequest?.companyNumber!!, newClaimRequest?.department!!, newClaimRequest.expenseType!!,
                    newClaimRequest.totalAmount?:"0", newClaimRequest.tax?.toDouble()?:0.0,newClaimRequest.taxCode?.toInt()?:0,newClaimRequest.auction,newClaimRequest.expenseCode)
                newClaimRequest.split.add(splitOne)

        viewModel.createNewClaim(newClaimRequest).observe(viewLifecycleOwner, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            try {
                                Log.d(TAG, "setChangePasswordObserver: ${resource.status}")
                                //Toast.makeText(contextActivity, "Claim added successfully/submitted successfully", Toast.LENGTH_SHORT).show()

                                setResponse(response)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Status.ERROR -> {
                        binding.mProgressBars.visibility = View.GONE
                        binding.btnSubmit.visibility = View.VISIBLE
                        Log.e(TAG, "setChangePasswordObserver: ${it.message}")
                        it.message?.let { it1 -> Toast.makeText(contextActivity, it1, Toast.LENGTH_SHORT).show() }
                    }
                    Status.LOADING -> {
                        binding.mProgressBars.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun createNewClaim() {
        try {
            val newClaimRequest = getClaimRequest()

            if (!validateCreateClaim(newClaimRequest)) {
                onCreateClaimFailed()
                return
            }
            if (binding.tvDateOfSubmission.text.isNullOrEmpty()) {
                Toast.makeText(contextActivity, "Please select Date", Toast.LENGTH_LONG).show()
                onCreateClaimFailed()
                return
            }
            if (binding.edtTotalAmount.text.isNullOrEmpty()) {
                Toast.makeText(contextActivity, "Please Enter Amount", Toast.LENGTH_LONG).show()

                onCreateClaimFailed()
                return
            }

            if(viewModel.attachmentsList.size > 0){
                binding.btnSubmit.visibility = View.GONE
                setCreateClaimObserver(newClaimRequest)
            } else{
                Toast.makeText(contextActivity, "Please select receipt image to upload", Toast.LENGTH_LONG).show()
                return
            }


        }
        catch (e: Exception){
            Log.e(TAG, "createNewClaim: ${e.message}")
        }
    }

    private fun splitNewClaim() {
        try {
            val newClaimRequest = getClaimRequest()

            if (!validateCreateClaim(newClaimRequest)) {
                onCreateClaimFailed()
                return
            }
            if (binding.tvDateOfSubmission.text.isNullOrEmpty()) {
                Toast.makeText(contextActivity, "Please select Date", Toast.LENGTH_LONG).show()
                onCreateClaimFailed()
                return
            }
            if (binding.edtTotalAmount.text.isNullOrEmpty()) {
                Toast.makeText(contextActivity, "Please Enter Amount", Toast.LENGTH_LONG).show()

                onCreateClaimFailed()
                return
            }

            if(viewModel.attachmentsList.size > 0){
                val splitOne = SplitClaimItem(
                    viewModel.companyList[binding.spnCompanyNumber.selectedItemPosition].id,
                    viewModel.companyList[binding.spnCompanyNumber.selectedItemPosition].code,
                    viewModel.departmentList[binding.spnDepartment.selectedItemPosition].id,
                    viewModel.expenseTypeList[binding.spnExpenseType.selectedItemPosition].id,
                    newClaimRequest.netAmount?:"0",
                    newClaimRequest.taxCode?:"",
                    newClaimRequest.tax?.toDouble()?:0.0,
                    viewModel.companyList[binding.spnCompanyNumber.selectedItemPosition].name,
                    viewModel.departmentList[binding.spnDepartment.selectedItemPosition].cost_code,
                    viewModel.expenseTypeList[binding.spnExpenseType.selectedItemPosition].name?:"",
                    newClaimRequest.auction?:"0",
                    expenseCode,
                    newClaimRequest.expenseCode?:"",
                    taxcodeId,""
                )
                println("splitOne :$splitOne")


                val fragment = SplitClaimFragment()
                fragment.setClaimRequestDetail(newClaimRequest)
                fragment.setSplitRequestDetail(splitOne)
                fragment.setGroupName(groupname)
                fragment.setCurrency(currencyCode,currencySymbol)
                (contextActivity as? MainActivity)?.addFragment(fragment)
            } else{
                Toast.makeText(contextActivity, "Please select receipt image to upload", Toast.LENGTH_LONG).show()
                return
            }

        }
        catch (e: Exception){
            Log.e(TAG, "createNewClaim: ${e.message}")
        }
    }

    private fun getClaimRequest() : NewClaimRequest{

        var dateFormate = if(companyDateFormate=="USA") {
            Constants.MMM_DD_YYYY_FORMAT
        }else{
            Constants.DD_MM_YYYY_FORMAT

        }
        return viewModel.getNewClaimRequest(
            binding.edtTitle.text.toString().trim(),
            binding.edtMerchantName.text.toString().trim(),
            if(binding.edtGroupValue.text.isEmpty())
            {
                ""
            }else{
                if (!viewModel.expenseGroupList.isNullOrEmpty()) viewModel.expenseGroupList[binding.spnExpenseGroup.selectedItemPosition].id else ""

            },
            if(binding.edtExpenceTypeValue.text.isEmpty())
            {
                ""
            }else{
                if (!viewModel.expenseTypeList.isNullOrEmpty()) viewModel.expenseTypeList[binding.spnExpenseType.selectedItemPosition].id else ""

            },
            if (!viewModel.companyList.isNullOrEmpty()) viewModel.companyList[binding.spnCompanyNumber.selectedItemPosition].id else "",
//            binding.edtCompanyNumber.text.toString().trim(),
            if (!viewModel.departmentList.isNullOrEmpty()) viewModel.departmentList[binding.spnDepartment.selectedItemPosition].id else "",
            Utils.getDateInServerRequestFormat(
                binding.tvDateOfSubmission.text.toString().trim(),
                dateFormate
            ),
            if (!viewModel.currencyList.isNullOrEmpty()) viewModel.currencyList[binding.spnCurrency.selectedItemPosition].id else "",
            binding.edtTotalAmount.text.toString(),
            if(binding.edtTax.text.isNotEmpty())binding.edtTax.text.toString()else "0",
            binding.tvNetAmount.text.toString(),
            binding.edtDescription.text.toString().trim(),
            if (!taxcodeId.isNullOrEmpty()) taxcodeId else "0",
            binding.edtAutionValue.text.toString().trim(),
            if (!viewModel.expenseTypeList.isNullOrEmpty()) viewModel.expenseTypeList[binding.spnExpenseType.selectedItemPosition].expenseCodeID else "",

            viewModel.claimImageList as List<String>,
            viewModel.attachmentsList as List<String>
        )
    }

    private fun validateCreateClaim(newClaimRequest: NewClaimRequest): Boolean {
        val isValid = viewModel.validateNewClaimRequest(newClaimRequest)
        if(!isValid.first){
            Toast.makeText(contextActivity, isValid.second, Toast.LENGTH_SHORT).show()
        }
        return isValid.first
    }

    private fun setResponse(commonResponse: CommonResponse) {
        binding.mProgressBars.visibility = View.GONE
        binding.btnSubmit.visibility = View.VISIBLE
        Toast.makeText(contextActivity, commonResponse.message, Toast.LENGTH_SHORT).show()
        if(commonResponse.success) {
            shouldRefreshPage = true
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity(). finish()
        }
    }

    private fun onCreateClaimFailed() {
        binding.btnSubmit.isEnabled = true
    }

    private fun showCalenderDialog(){
        val calendar = Calendar.getInstance()
        val calendarStart: Calendar = Calendar.getInstance()

        val constraintsBuilder =
            CalendarConstraints.Builder()
                //.setStart(calendarStart.timeInMillis)
                .setEnd(calendar.timeInMillis)
                .setValidator(DateValidatorPointBackward.now())


        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.Widget_AppTheme_MaterialDatePicker)
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        activity?.supportFragmentManager?.let { picker.show(it, picker.toString()) }
        picker.addOnPositiveButtonClickListener {
            val date = Utils.getDateInDisplayFormatWithCountry(it,companyDateFormate)
            Log.d("DatePicker Activity", "Date String = ${date}:: Date epoch value = ${it}")
            binding.tvDateOfSubmission.text = date
        }
    }

    private fun showBottomSheet(){
        contextActivity?.let {
            val dialog = BottomSheetDialog(contextActivity!!, R.style.CustomBottomSheetDialogTheme)
            val view = layoutInflater.inflate(R.layout.item_bottom_sheet, null)
            dialog.setCancelable(true)
            dialog.setContentView(view)
            val bottomOptionOne = view.findViewById<TextView>(R.id.bottomOptionOne)
//            val dividerOne = view.findViewById<View>(R.id.dividerOne)
            val bottomOptionTwo = view.findViewById<TextView>(R.id.bottomOptionTwo)
            val dividerTwo = view.findViewById<View>(R.id.dividerTwo)
            val bottomOptionThree = view.findViewById<TextView>(R.id.bottomOptionThree)
            val bottomOptionCancel = view.findViewById<TextView>(R.id.bottomOptionCancel)

            bottomOptionOne.text = resources.getString(R.string.upload_file)
            bottomOptionTwo.text = resources.getString(R.string.take_photo)
            dividerTwo.visibility = View.GONE
            bottomOptionThree.visibility = View.GONE

            bottomOptionOne.setOnClickListener {
                dialog.dismiss()
                choosePhotoFromGallery()
            }
            bottomOptionTwo.setOnClickListener {
                dialog.dismiss()
                takePhotoFromCamera()
            }
            bottomOptionCancel.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    private fun choosePhotoFromGallery() {
        contextActivity?. let {
            val intent = Lassi(contextActivity!!)
                .with(LassiOption.CAMERA_AND_GALLERY) // choose Option CAMERA, GALLERY or CAMERA_AND_GALLERY
                .setMaxCount(1)
                .setGridSize(3)
                .setMediaType(MediaType.IMAGE) // MediaType : VIDEO IMAGE, AUDIO OR DOC
                .setCompressionRation(50) // compress image for single item selection (can be 0 to 100)
               // .setMinFileSize(50) // Restrict by minimum file size
               // .setMaxFileSize(100) //  Restrict by maximum file size
                .disableCrop() // to remove crop from the single image selection (crop is enabled by default for single image)
                .setStatusBarColor(R.color.secondary)
                .setToolbarResourceColor(R.color.white)
                .setProgressBarColor(R.color.secondary)
                .setToolbarColor(R.color.secondary)
                .setPlaceHolder(R.drawable.ic_image_placeholder)
                .setErrorDrawable(R.drawable.ic_image_placeholder)
                .build()
            startActivityForResult(intent, 100)
        }
    }

    private fun takePhotoFromCamera(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ImagePicker.with(this)
                .crop()
                .cameraOnly()//Crop image(Optional), Check Customization for more option
                .compress(1024)            //Final image size will be less than 1 MB(Optional)
                .maxResultSize(
                    1080,
                    1080
                )    //Final image resolution will be less than 1080 x 1080(Optional)
                .start()
        }else {
            contextActivity?.let {
                val intent = Lassi(contextActivity!!)
                    .with(LassiOption.CAMERA) // choose Option CAMERA, GALLERY or CAMERA_AND_GALLERY
                    .setMaxCount(1)
                    .setGridSize(3)
                    .setMediaType(MediaType.IMAGE) // MediaType : VIDEO IMAGE, AUDIO OR DOC
                    .setCompressionRation(50) // compress image for single item selection (can be 0 to 100)
                    .setMinFileSize(50) // Restrict by minimum file size
                    .setMaxFileSize(100) //  Restrict by maximum file size
                    .disableCrop() // to remove crop from the single image selection (crop is enabled by default for single image)
                    .setStatusBarColor(R.color.secondary)
                    .setToolbarResourceColor(R.color.white)
                    .setProgressBarColor(R.color.secondary)
                    .setToolbarColor(R.color.secondary)
                    .setPlaceHolder(R.drawable.ic_image_placeholder)
                    .setErrorDrawable(R.drawable.ic_image_placeholder)
                    .build()
                startActivityForResult(intent, 101)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                100 -> {
                    val selectedMedia = data.getSerializableExtra(KeyUtils.SELECTED_MEDIA) as ArrayList<MiMedia>
                    Log.d(TAG, "onActivityResult: ${selectedMedia.size}")
                    if(selectedMedia.size > 0) {
                        val file= File( selectedMedia[0].path!!)
                        val filePath: String = file.path
                        val bitmap = BitmapFactory.decodeFile(filePath)
                        viewModel.claimImageList.add(bitmap)

                        viewModel.attachmentsList.add(selectedMedia[0].path!!)
                        refreshAttachments()
                        Log.d(TAG, "onActivityResult:  attachmentsList: ${viewModel.attachmentsList.size}")
                    }
                }
                101 -> {
                    val selectedMedia = data.getSerializableExtra(KeyUtils.SELECTED_MEDIA) as ArrayList<MiMedia>
                    Log.d(TAG, "onActivityResult: ${selectedMedia.size}")
                    if(selectedMedia.size > 0) {
                        val file= File( selectedMedia[0].path!!)
                        val filePath: String = file.path
                        val bitmap = BitmapFactory.decodeFile(filePath)
                        viewModel.claimImageList.add(bitmap)
                        viewModel.attachmentsList.add(selectedMedia[0].path!!)

                        refreshAttachments()
                        Log.d(TAG, "onActivityResult:  attachmentsList: ${viewModel.attachmentsList.size}")
                    }
                }else->{
                if (resultCode == Activity.RESULT_OK){
                    val uri: Uri = data?.data!!
                    val file= File( uri.path)
                    val filePath: String = file.path
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    viewModel.claimImageList.add(bitmap)
                    viewModel.attachmentsList.add(uri.path)
                    refreshAttachments()
                }

            }
            }
        }
    }

    @Throws(IOException::class)
    fun getBytes(`is`: InputStream): ByteArray? {
        val byteBuff = ByteArrayOutputStream()
        val buffSize = 1024
        val buff = ByteArray(buffSize)
        var len = 0
        while (`is`.read(buff).also { len = it } != -1) {
            byteBuff.write(buff, 0, len)
        }
        return byteBuff.toByteArray()
    }
    override fun onDestroy() {
        super.onDestroy()
        if(shouldRefreshPage && this::refreshPageListener.isInitialized){
            refreshPageListener.refreshPage()
        }
    }
    private fun showImagePopup(imageUrl:String) {
        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.image_popup_dialog)

        val image = dialog.findViewById(R.id.itemImage) as ImageView
        Glide.with(requireContext())
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.mountains)
                    .error(R.drawable.mountains)
            )
            .placeholder(R.drawable.mountains)
            .error(R.drawable.mountains)
            .into(image)


        dialog.show()
        val noBtn = dialog.findViewById(R.id.lnClose) as LinearLayout
        noBtn.setOnClickListener {
            dialog.dismiss()
        }
    }
    override fun callback(action: String, data: Any, postion: Int) {
        if (action == "show") {
            showImagePopup(data as String)
        }
        if (action == "remove") {
           val size =data as Int
            if(size<=0){
                refreshAttachments()
            }
        }
    }





}
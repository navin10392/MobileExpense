package com.bonhams.expensemanagement.ui.batch


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bonhams.expensemanagement.R
import com.bonhams.expensemanagement.data.model.BatchData
import com.bonhams.expensemanagement.databinding.ItemBatchBinding
import com.bonhams.expensemanagement.databinding.ItemBatchsssssssBinding
import com.bonhams.expensemanagement.utils.RecylerCallback
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class BatchListAdapter(
    var listOrders: List<BatchData>, var context:Context,var recylerCallback: RecylerCallback
) : RecyclerView.Adapter<BatchListAdapter.ViewHolder>() {

    init {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       // val attachmentItem = expncelist?.get(position)
       holder.bindItems(listOrders[position],context,listOrders,recylerCallback,position)


    }
    override fun getItemCount(): Int {
        listOrders.let {
            return listOrders.size
        }

    }

    class ViewHolder(itemBinding: ItemBatchsssssssBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        private val binding: ItemBatchsssssssBinding= itemBinding
        fun bindItems(
            item: BatchData,
            context: Context,
            expncelist: List<BatchData>,
            recylerCallback: RecylerCallback,
            postion: Int
        ) {
            binding.tvBatchNo.text = item.batch_allotted.toString()
            binding.tvCurrency.text = item.currency_type
            binding.tvCompany.text = item.company_code
            binding.tvLedger.text = item.ledger_id

            binding.tvTotalAmount.text = item.currency_symbol+" "+String.format("%.2f",item.totalAmount.toString().toDouble())
           binding.tvDate.text =parseDateFormat(item.lastestSubmissionDate, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "dd MMM")

            if(item.e_claims>0){
                binding.claimView.visibility=View.VISIBLE
                binding.tvNoClaims.visibility=View.VISIBLE
                binding.tvNoClaims.text = item.e_claims.toString()+" "+"Claims"
            }else{
                binding.claimView.visibility=View.GONE
                binding.tvNoClaims.visibility=View.GONE

            }
            if(item.m_claims>0){
                binding.mileageView.visibility=View.VISIBLE
                binding.tvNoMileage.visibility=View.VISIBLE
                binding.tvNoMileage.text = item.m_claims.toString()+" "+"Mileage"

            }else{
                binding.mileageView.visibility=View.GONE
                binding.tvNoMileage.visibility=View.GONE
            }
            binding.batchCardView.setOnClickListener {

                recylerCallback.callback("details",item,postion)
            }
            binding.lnMore.setOnClickListener {

                recylerCallback.callback("batch",item,postion)
            }

            if(item.batch_submit_check==0){

                binding.lnMore.visibility=View.GONE
            }else{
                binding.lnMore.visibility=View.VISIBLE

            }

        }

        fun parseDateFormat(
            dateToFormat: String?,
            inputFormat: String?,
            outputFormat: String?
        ): String? {
            val inputFormat = SimpleDateFormat(inputFormat)
            val outputFormat = SimpleDateFormat(outputFormat)
            var date: Date? = null
            var str: String? = null
            try {
                date = inputFormat.parse(dateToFormat)
                str = outputFormat.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return str
        }


        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                return ViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_batchsssssss, parent, false
                    )
                )
            }
        }
    }


}
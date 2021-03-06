package com.bonhams.expensemanagement.data.services.requests

import android.graphics.Bitmap
import com.bonhams.expensemanagement.data.model.SplitClaimDetail
import java.io.File

class NewClaimRequest {
    var title: String? = ""
    var merchantName: String? = ""
    var expenseGroup: String? = ""
    var expenseType: String? = ""
    var companyNumber: String? = ""
    var department: String? = ""
    var dateSubmitted: String? = ""
    var currency:  String? = ""
    var totalAmount: String? = ""
    var tax: String? = ""
    var netAmount: String? = ""
    var description: String? = ""
    var taxCode: String? = ""
    var auction: String? = ""
    var expenseCode: String? = ""
    var attachments: List<String> = emptyList()
    var split: MutableList<SplitClaimDetail> = mutableListOf()
}
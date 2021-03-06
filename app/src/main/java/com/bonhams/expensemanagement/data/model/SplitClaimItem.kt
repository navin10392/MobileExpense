package com.bonhams.expensemanagement.data.model

import java.io.Serializable

data class SplitClaimItem (
    val companyNumber: String = "",
    val companyCode: String = "",
    val department: String = "",
    val expenseType: String = "",
    var totalAmount: String = "",
    val taxcode: String = "",
    var tax: Double = 0.0,
    val compnyName: String = "",
    val departmentName: String = "",
    val expenceTypeName: String = "",
    val auctionSales: String = "0",
    val expenceCode: String = "",
    val expenseCodeID: String = "0",
    val taxCodeValue: String = "0",
    val split_id: String = "",
    val expense_group_id: String = "",

    ): Serializable
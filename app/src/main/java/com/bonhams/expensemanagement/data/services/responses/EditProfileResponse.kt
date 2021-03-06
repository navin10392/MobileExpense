package com.bonhams.expensemanagement.data.services.responses

import com.bonhams.expensemanagement.data.model.ToBeAcceptedData
import com.google.gson.annotations.SerializedName

class EditProfileResponse {
    @SerializedName("success")
    var success: Boolean = false
    @SerializedName("message")
    var message: String? = ""
    @SerializedName("data")
    val profileDetail: List<ProfileDetails?> = emptyList()
}

class EditProfileDetails() {
    val id: String = ""
    val name: String = ""
    val profileImage: String = ""
    val fname: String = ""
    val lname: String = ""
    val email: String = ""
    @SerializedName("contact_no")
    val contactNo: String = ""
    val companyName: String = ""
    val departmentName: String = ""
    @SerializedName("employId")
    val employID: String = ""
    val approver: String = ""
    val countryCode: String = ""
}

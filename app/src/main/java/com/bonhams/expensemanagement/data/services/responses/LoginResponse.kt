package com.bonhams.expensemanagement.data.services.responses

import com.google.gson.annotations.SerializedName

class LoginResponse {
    @SerializedName("success")
    var success: Boolean = false
    @SerializedName("message")
    var message: String? = ""
    @SerializedName("userDetails")
    var userDetails: UserDetails? = null
}

class UserDetails() {
    val id: String = ""
    val name: String = ""
    val profileImage: String = ""
    val fname: String = ""
    val lname: String = ""
    val email: String = ""
    @SerializedName("contact_no")
    val contactNo: String = ""
    @SerializedName("user_type")
    val userType: String = ""
    @SerializedName("companyName")
    val companyName: String = ""
    @SerializedName("companyId")
    val companyId: String = ""
    @SerializedName("carType_id")
    val carType_id: String = ""
    @SerializedName("departmentName")
    val departmentName: String = ""
    @SerializedName("departmentId")
    val departmentId: String = ""
    val carType: String = ""
    val mileageType: String = ""
    @SerializedName("employId")
    val employID: String = ""
    val isReset: Long = 0
    val status: String = ""
    @SerializedName("last_login")
    val lastLogin: String = ""
    val approver: String = ""
    val countryCode: String = ""
    val token: String = ""
    @SerializedName("refresh_token")
    val refreshToken: String = ""
    @SerializedName("ledger_id")
    val ledger_id: String = ""

}

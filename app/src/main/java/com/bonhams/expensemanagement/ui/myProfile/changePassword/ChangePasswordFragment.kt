package com.bonhams.expensemanagement.ui.myProfile.changePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bonhams.expensemanagement.R
import com.bonhams.expensemanagement.ui.BaseActivity

private const val TAG = "NotificationFragment"

class ChangePasswordFragment() : Fragment() {

    companion object {
        fun newInstance() = ChangePasswordFragment()
    }

    private var contextActivity: BaseActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        contextActivity = activity as? BaseActivity

        return view
    }
}
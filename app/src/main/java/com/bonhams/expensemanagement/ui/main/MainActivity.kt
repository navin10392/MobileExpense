package com.bonhams.expensemanagement.ui.main

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bonhams.expensemanagement.BuildConfig
import com.bonhams.expensemanagement.R
import com.bonhams.expensemanagement.adapters.NavDrawerAdapter
import com.bonhams.expensemanagement.data.model.NavDrawerItem
import com.bonhams.expensemanagement.data.services.ApiHelper
import com.bonhams.expensemanagement.data.services.RetrofitBuilder
import com.bonhams.expensemanagement.ui.BaseActivity
import com.bonhams.expensemanagement.ui.BlankFragment
import com.bonhams.expensemanagement.ui.claims.newClaim.NewClaimFragment
import com.bonhams.expensemanagement.ui.gpsTracking.GPSTrackingFragment
import com.bonhams.expensemanagement.ui.home.HomeFragment
import com.bonhams.expensemanagement.ui.login.LoginActivity
import com.bonhams.expensemanagement.ui.mileageExpenses.newMileageClaim.NewMileageClaimFragment
import com.bonhams.expensemanagement.ui.myProfile.MyProfileFragment
import com.bonhams.expensemanagement.ui.notification.NotificationFragment
import com.bonhams.expensemanagement.utils.*
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.app_bar_main.view.*
import org.imaginativeworld.oopsnointernet.callbacks.ConnectionCallback
import org.imaginativeworld.oopsnointernet.snackbars.fire.NoInternetSnackbarFire


class MainActivity : BaseActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var navDrawerAdapter: NavDrawerAdapter
    private val TAG = javaClass.simpleName
    private var navDrawerItems = arrayListOf(
        NavDrawerItem(R.drawable.ic_home, "Expense", -1),
        NavDrawerItem(R.drawable.ic_nav_expense_plus, "Manually Create", 1),
        NavDrawerItem(R.drawable.ic_nav_scan, "Scan Receipt", 2),
        NavDrawerItem(R.drawable.ic_home, "Mileage", -1),
        NavDrawerItem(R.drawable.ic_nav_car, "Manually Create", 3),
        NavDrawerItem(R.drawable.ic_nav_gps, "Start GPS", 4),
        NavDrawerItem(R.drawable.ic_profile, "Others", -1),
        NavDrawerItem(R.drawable.ic_nav_my_profile, "My Account", 5),
        NavDrawerItem(R.drawable.ic_nav_logout, "Logout", 6),
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        setupNavDrawer()
        setupViewModel()
        setupClickListeners()
        setupAppbar()
        setNoInternetSnackbar()
        fragmentBackstackListener()

        if (savedInstanceState == null) {
            val fragment = HomeFragment()
            changeFragment(fragment)
        }
    }

    override fun onBackPressed() {
        if (navDrawer.isDrawerOpen(GravityCompat.START)) {
            navDrawer.closeDrawer(GravityCompat.START)
        } else {
            // Checking for fragment count on back stack
            if (supportFragmentManager.backStackEntryCount > 0) {
                backButtonPressed()
            } else {
                // Exit the app
                super.onBackPressed()
            }
        }
    }

    private fun backButtonPressed(){
        // Go to the previous fragment
        supportFragmentManager.popBackStack()
        // Reset app bar
        if (supportFragmentManager.backStackEntryCount == 1) {
            setupAppbar()
            bottomNavigationView.selectedItemId = R.id.bottom_nav_home
        }
    }

    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            bottomNavigationView.menu.setGroupCheckable(0, true, true)
            when (menuItem.itemId) {
                R.id.bottom_nav_home -> {
                    setupAppbar()
                    showBottomNavbar(true)
                    removeAnyOtherFragVisible()
                    val fragment = HomeFragment()
                    changeFragment(fragment)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bottom_nav_camera -> {
                    setAppbarTitle(getString(R.string.scan_receipt))
                    showBottomNavbar(true)
                    val fragment = BlankFragment()
                    changeFragment(fragment)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bottom_nav_notifications -> {
                    setAppbarTitle(getString(R.string.notifications))
                    showBottomNavbar(true)
                    val fragment = NotificationFragment()
                    changeFragment(fragment)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bottom_nav_my_profile -> {
                    setAppbarTitle(getString(R.string.profile))
                    showBottomNavbar(true)
                    val fragment = MyProfileFragment()
                    changeFragment(fragment)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    private fun setupClickListeners(){
        layoutBack.setOnClickListener(View.OnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                backButtonPressed()
            }
        })
        ivMenu.setOnClickListener(View.OnClickListener {
            if (!navDrawer.isDrawerOpen(GravityCompat.START)) {
                navDrawer.openDrawer(GravityCompat.START)
            }
        })
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(ApiHelper(RetrofitBuilder.apiService))
        ).get(MainViewModel::class.java)
        
        viewModel.responseLogout?.observe(this, Observer {
            Log.d(TAG, "setupViewModel: ${it.message}")
        })
    }
    
    private fun setupAppbar(){
        appbarTitle.visibility = View.GONE
        layoutAppBarMenu.visibility = View.VISIBLE
        layoutGreeting.visibility = View.VISIBLE
        ivMenu.visibility = View.VISIBLE
        layoutBack.visibility = View.GONE
        layoutAppBarSearch.visibility = View.VISIBLE
        ivSearch.visibility = View.VISIBLE
    }

    private fun setupNavDrawer(){
        navDrawerTitle.text = AppPreferences.fullName
        navDrawerDescription.text = AppPreferences.email
        tvNavDrawerAppVersion.text = "Version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        Glide.with(this)
            .load(AppPreferences.profilePic)
            .error(R.drawable.ic_default_user)
            .fallback(R.drawable.ic_default_user)
            .circleCrop()
            .into(navDrawerProfilePic);

        // Setup Recyclerview's Layout
        navDrawerRv.layoutManager = LinearLayoutManager(this)
        navDrawerRv.setHasFixedSize(true)

        // Add Item Touch Listener
        navDrawerRv.addOnItemTouchListener(RecyclerTouchListener(this, object : ClickListener {
            override fun onClick(view: View, position: Int) {
                Log.d(TAG, "navDrawerRv onClick: $position")
                Log.d(TAG, "navDrawerRv onClick code: ${navDrawerItems[position].code}")
                when (navDrawerItems[position].code) {
                    1 -> { // Manually Create
                        setAppbarTitle(getString(R.string.create_new_claim))
                        showBottomNavbar(false)
                        showAppbarBackButton(true)
                        val fragment = NewClaimFragment()
                        addFragment(fragment)
                    }
                    2 -> { // Scan Receipt
                        setAppbarTitle(getString(R.string.scan_receipt))
                        showBottomNavbar(false)
                        showAppbarBackButton(true)
                        val fragment = BlankFragment()
                        addFragment(fragment)
                    }
                    3 -> { // Manually Create
                        setAppbarTitle(getString(R.string.create_mileage_claim))
                        showBottomNavbar(false)
                        showAppbarBackButton(true)
                        val fragment = NewMileageClaimFragment()
                        addFragment(fragment)
                    }
                    4 -> { // Start GPS
                        setAppbarTitle(getString(R.string.start_gps))
                        showBottomNavbar(false)
                        showAppbarBackButton(true)
                        val fragment = GPSTrackingFragment()
                        addFragment(fragment)
                    }
                    5 -> { // My Account
                        setAppbarTitle(getString(R.string.profile))
                        setupAppbar()
                        bottomNavigationView.selectedItemId = R.id.bottom_nav_my_profile
                    }
                    6 -> { // Logout
                        showLogoutAlert()
                    }
                    else -> {
                        setupAppbar()
                        showBottomNavbar(true)
                        val fragment = HomeFragment()
                        changeFragment(fragment)
                    }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    navDrawer.closeDrawer(GravityCompat.START)
                }, 200)
            }
        }))

//        updateAdapter(0)
        navDrawerAdapter = NavDrawerAdapter(navDrawerItems, -1)
        navDrawerRv.adapter = navDrawerAdapter
        navDrawerAdapter.notifyDataSetChanged()
    }

    private fun showLogoutAlert() {
        val dialog = Dialog(this)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_alert_dialog)
        val title = dialog.findViewById(R.id.txtTitle) as TextView
        val body = dialog.findViewById(R.id.txtDescription) as TextView
        val input = dialog.findViewById(R.id.edtDescription) as EditText
        val yesBtn = dialog.findViewById(R.id.btnPositive) as Button
        val noBtn = dialog.findViewById(R.id.btnNegative) as TextView

        input.visibility = View.GONE
        title.text = resources.getString(R.string.logout)
        body.text = resources.getString(R.string.are_you_sure_you_want_to_logout)
        yesBtn.text = resources.getString(R.string.logout)
        noBtn.text = resources.getString(R.string.cancel)

        yesBtn.setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun logoutUser(){
        viewModel.logoutUser().observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let { response ->
                            Log.d(TAG, "logoutUser: ${response.message}")
                        }
                    }
                    Status.ERROR -> {
                        it.message?.let { it1 -> Log.d(TAG, "logoutUser: $it1")}
                    }
                    Status.LOADING -> {
                        Log.d(TAG, "Loading.......")
                    }
                }
            }
        })

        AppPreferences.clearPrefs()
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }


    /*
    * Location functions
    * */
    private fun hasLocationForegroundPermission() = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationBackgroundPermission() = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasFineLocationPermission() = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun requestPermission() {
        var permissionToRequest = mutableListOf<String>()
        if (!hasLocationForegroundPermission())
            permissionToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (!hasLocationBackgroundPermission())
            permissionToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        if (!hasFineLocationPermission())
            permissionToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                    viewModel.locationGranted = true
                    Log.d("MainActivity", "onRequestPermissionsResult: ${permissions[i]}")
                }
            }
        }
    }

    /*
    * App bar functions
    * */
    fun setAppbarTitle(title: String){
        appbarTitle.visibility = View.VISIBLE
        appbarTitle.text = title
        layoutGreeting.visibility = View.GONE
        ivMenu.visibility = View.VISIBLE
        layoutBack.visibility = View.GONE
        layoutAppBarSearch.visibility = View.VISIBLE
    }

    fun showAppbarBackButton(show: Boolean){
        appbarTitle.visibility = View.VISIBLE//if(show) View.VISIBLE else View.INVISIBLE
        layoutGreeting.visibility = View.GONE
        ivMenu.visibility = View.GONE
        layoutBack.visibility = if(show) View.VISIBLE else View.INVISIBLE
        showAppbarSearch(false)
    }

    fun showAppbarSearch(show: Boolean){
        layoutAppBarSearch.visibility = if(show) View.VISIBLE else View.INVISIBLE
        ivSearch.visibility = if(show) View.VISIBLE else View.INVISIBLE
        appbarEdit.visibility = View.GONE
    }

    fun showAppbarEdit(show: Boolean){
        layoutAppBarSearch.visibility = if(show) View.VISIBLE else View.INVISIBLE
        appbarEdit.visibility = if(show) View.VISIBLE else View.GONE
        ivSearch.visibility = View.GONE
    }

    fun showBottomNavbar(show: Boolean){
        bottomNavigationView.visibility = if(show) View.VISIBLE else View.GONE
    }

    /*
    * Fragment backstack functions
    * */
    private fun removeAnyOtherFragVisible(){
        supportFragmentManager.fragments.forEach {
            if (it !is HomeFragment) {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(
            R.id.container,
            fragment,
            fragment.javaClass.simpleName
        ).commit()
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            fragment,
            fragment.javaClass.simpleName
        ).addToBackStack(fragment.javaClass.simpleName).commit()
    }

    private fun fragmentBackstackListener(){
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.container)
            if (fragment != null) {
                val fragName = fragment.javaClass.simpleName
                if(fragName.equals(HomeFragment::class.java.name, true)){
                    setupAppbar()
                }
                else if(fragName.equals(NewClaimFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.create_new_claim))
                    showAppbarBackButton(true)
                    showBottomNavbar(false)
                }
                else if(fragName.equals(BlankFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.bonhams))
                    setupAppbar()
                    showBottomNavbar(true)
                }
                else if(fragName.equals(NewMileageClaimFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.create_mileage_claim))
                    showAppbarBackButton(true)
                    showBottomNavbar(false)
                }
                else if(fragName.equals(GPSTrackingFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.start_gps))
                    showAppbarBackButton(true)
                    showBottomNavbar(false)
                }
                else if(fragName.equals(NotificationFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.notifications))
                    showAppbarBackButton(false)
                    showBottomNavbar(true)
                }
                else if(fragName.equals(MyProfileFragment::class.java.simpleName, true)){
                    setAppbarTitle(getString(R.string.profile))
                    showAppbarBackButton(false)
                    showBottomNavbar(true)
                }
            }
        }
    }

    /*
    * No internet connection check
    * */
    private fun setNoInternetSnackbar(){
        // No Internet Snackbar: Fire
        NoInternetSnackbarFire.Builder(
            navDrawer,
            lifecycle
        ).apply {
            snackbarProperties.apply {
                connectionCallback = object : ConnectionCallback { // Optional
                    override fun hasActiveConnection(hasActiveConnection: Boolean) {

                    }
                }

                duration = Snackbar.LENGTH_INDEFINITE // Optional
                noInternetConnectionMessage = "No active Internet connection!" // Optional
                onAirplaneModeMessage = "You have turned on the airplane mode!" // Optional
                snackbarActionText = "Settings" // Optional
                showActionToDismiss = false // Optional
                snackbarDismissActionText = "OK" // Optional
            }
        }.build()
    }
}
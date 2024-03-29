/*
 * Copyright © 2021 By Geeks Empire.
 *
 * Created by Elias Fazel
 * Last modified 4/22/21 10:06 AM
 *
 * Licensed Under MIT License.
 * https://opensource.org/licenses/MIT
 */

package net.geekstools.supershortcuts.PRO.ApplicationsShortcuts

import android.app.ActivityOptions
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import net.geekstools.supershortcuts.PRO.ApplicationsShortcuts.Adapters.SavedAppsListPopupAdapter
import net.geekstools.supershortcuts.PRO.ApplicationsShortcuts.Adapters.SelectionListAdapter
import net.geekstools.supershortcuts.PRO.ApplicationsShortcuts.Extensions.*
import net.geekstools.supershortcuts.PRO.ApplicationsShortcuts.UI.AppsConfirmButtonPhone
import net.geekstools.supershortcuts.PRO.BuildConfig
import net.geekstools.supershortcuts.PRO.FoldersShortcuts.FolderShortcuts
import net.geekstools.supershortcuts.PRO.MixShortcuts.MixShortcutsProcess
import net.geekstools.supershortcuts.PRO.Preferences.PreferencesUI
import net.geekstools.supershortcuts.PRO.R
import net.geekstools.supershortcuts.PRO.SecurityServices.SecurityServicesProcess
import net.geekstools.supershortcuts.PRO.SplitShortcuts.SplitShortcuts
import net.geekstools.supershortcuts.PRO.Utils.AdapterItemsData.AdapterItemsData
import net.geekstools.supershortcuts.PRO.Utils.Functions.FunctionsClass
import net.geekstools.supershortcuts.PRO.Utils.Functions.FunctionsClassDialogues
import net.geekstools.supershortcuts.PRO.Utils.InAppStore.DigitalAssets.Utils.PurchasesCheckpoint
import net.geekstools.supershortcuts.PRO.Utils.InAppUpdate.InAppUpdateProcess
import net.geekstools.supershortcuts.PRO.Utils.RemoteProcess.LicenseValidator
import net.geekstools.supershortcuts.PRO.Utils.UI.ConfirmButtonInterface.ConfirmButtonProcessInterface
import net.geekstools.supershortcuts.PRO.Utils.UI.CustomIconManager.LoadCustomIcons
import net.geekstools.supershortcuts.PRO.Utils.UI.Gesture.GestureConstants
import net.geekstools.supershortcuts.PRO.Utils.UI.Gesture.GestureListenerConstants
import net.geekstools.supershortcuts.PRO.Utils.UI.Gesture.GestureListenerInterface
import net.geekstools.supershortcuts.PRO.Utils.UI.Gesture.SwipeGestureListener
import net.geekstools.supershortcuts.PRO.databinding.NormalAppSelectionBinding
import java.lang.String
import java.util.*
import kotlin.Boolean
import kotlin.Float
import kotlin.apply
import kotlin.getValue
import kotlin.lazy
import kotlin.let

class NormalAppShortcutsSelectionListPhone : AppCompatActivity(),
        GestureListenerInterface,
        ConfirmButtonProcessInterface {

    val functionsClass: FunctionsClass by lazy {
        FunctionsClass(applicationContext)
    }

    private val functionsClassDialogues: FunctionsClassDialogues by lazy {
        FunctionsClassDialogues(this@NormalAppShortcutsSelectionListPhone, functionsClass)
    }

    private val listPopupWindow: ListPopupWindow by lazy {
        ListPopupWindow(applicationContext)
    }

    lateinit var recyclerViewLayoutManager: LinearLayoutManager
    lateinit var appSelectionListAdapter: RecyclerView.Adapter<SelectionListAdapter.ViewHolder>
    val installedAppsListItem: ArrayList<AdapterItemsData> = ArrayList<AdapterItemsData>()

    private val selectedAppsListItem: ArrayList<AdapterItemsData> = ArrayList<AdapterItemsData>()

    var appsConfirmButtonPhone: AppsConfirmButtonPhone? = null

    var appShortcutLimitCounter = 0

    var resetAdapter: Boolean = false
    var updateAvailable: Boolean = false

    private val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    val loadCustomIcons: LoadCustomIcons by lazy {
        LoadCustomIcons(applicationContext, functionsClass.customIconPackageName())
    }

    private val swipeGestureListener: SwipeGestureListener by lazy {
        SwipeGestureListener(applicationContext, this@NormalAppShortcutsSelectionListPhone)
    }

    companion object {
        const val NormalApplicationsShortcutsFile = ".autoSuper"
    }

    lateinit var normalAppSelectionBinding: NormalAppSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        normalAppSelectionBinding = NormalAppSelectionBinding.inflate(layoutInflater)
        setContentView(normalAppSelectionBinding.root)

        SecurityServicesProcess(this@NormalAppShortcutsSelectionListPhone).switchSecurityServices(normalAppSelectionBinding.securityServicesSwitchView)

        /* Check Shortcuts Information */
        evaluateShortcutsInfo()
        /* Check Shortcuts Information */

        appsConfirmButtonPhone = setupConfirmButtonUI(this@NormalAppShortcutsSelectionListPhone)

        /* Setup UI*/
        setupUI()
        /* Setup UI*/

        initializeLoadingProcess()

        functionsClassDialogues.changeLog(!functionsClass.isFirstToCheckTutorial, false)

        //In-App Billing
        PurchasesCheckpoint(this@NormalAppShortcutsSelectionListPhone).trigger()
    }

    override fun onStart() {
        super.onStart()

        if (!getFileStreamPath(".License").exists() && functionsClass.networkConnection()) {
            startService(Intent(applicationContext, LicenseValidator::class.java))

            val intentFilter = IntentFilter()
            intentFilter.addAction(getString(R.string.license))
            val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == getString(R.string.license)) {
                        functionsClass.dialogueLicense(this@NormalAppShortcutsSelectionListPhone)

                        Handler().postDelayed({
                            stopService(Intent(applicationContext, LicenseValidator::class.java))
                        }, 1000)

                        unregisterReceiver(this)
                    }
                }
            }
            registerReceiver(broadcastReceiver, intentFilter)
        }

        normalAppSelectionBinding.autoCategories.setOnClickListener {

            functionsClass.overrideBackPress(this@NormalAppShortcutsSelectionListPhone, FolderShortcuts::class.java,
                    ActivityOptions.makeCustomAnimation(applicationContext, R.anim.slide_from_right, R.anim.slide_to_left))
        }

        normalAppSelectionBinding.autoSplit.setOnClickListener {

            functionsClass.overrideBackPress(this@NormalAppShortcutsSelectionListPhone, SplitShortcuts::class.java,
                    ActivityOptions.makeCustomAnimation(applicationContext, R.anim.slide_from_right, R.anim.slide_to_left))
        }

        normalAppSelectionBinding.preferencesView.setOnClickListener {

            if (updateAvailable) {

                FunctionsClassDialogues(this@NormalAppShortcutsSelectionListPhone, functionsClass).changeLogPreference(
                        firebaseRemoteConfig.getString(functionsClass.upcomingChangeLogRemoteConfigKey()),
                        String.valueOf(firebaseRemoteConfig.getLong(functionsClass.versionCodeRemoteConfigKey()))
                )

            } else {

                startActivity(Intent(applicationContext, PreferencesUI::class.java),
                        ActivityOptions.makeCustomAnimation(applicationContext, R.anim.up_down, android.R.anim.fade_out).toBundle())

                this@NormalAppShortcutsSelectionListPhone.finish()
            }
        }

        MixShortcutsProcess(applicationContext, normalAppSelectionBinding.mixShortcutsSwitchView).initialize()
    }

    override fun onResume() {
        super.onResume()

        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_default)
        firebaseRemoteConfig.fetch(0)
                .addOnSuccessListener {

                    firebaseRemoteConfig.activate().addOnSuccessListener {

                        if (firebaseRemoteConfig.getLong(functionsClass.versionCodeRemoteConfigKey()) > BuildConfig.VERSION_CODE) {

                            val layerDrawableNewUpdate = getDrawable(R.drawable.ic_update) as LayerDrawable?
                            val gradientDrawableNewUpdate = layerDrawableNewUpdate!!.findDrawableByLayerId(R.id.temporaryBackground) as BitmapDrawable
                            gradientDrawableNewUpdate.setTint(getColor(R.color.default_color_game))

                            val temporaryBitmap = functionsClass.drawableToBitmap(layerDrawableNewUpdate)
                            val scaleBitmap = Bitmap.createScaledBitmap(temporaryBitmap, temporaryBitmap.width / 4, temporaryBitmap.height / 4, false)
                            val logoDrawable: Drawable = BitmapDrawable(resources, scaleBitmap)

                            normalAppSelectionBinding.preferencesView.setImageDrawable(logoDrawable)

                            functionsClass.notificationCreator(
                                    getString(R.string.updateAvailable),
                                    firebaseRemoteConfig.getString(functionsClass.upcomingChangeLogSummaryConfigKey()),
                                    firebaseRemoteConfig.getLong(functionsClass.versionCodeRemoteConfigKey()).toInt()
                            )

                            val inAppUpdateTriggeredTime =
                                    (Calendar.getInstance()[Calendar.YEAR].toString() + Calendar.getInstance()[Calendar.MONTH].toString() + Calendar.getInstance()[Calendar.DATE].toString())
                                            .toInt()

                            if (FirebaseAuth.getInstance().currentUser != null
                                    && functionsClass.readPreference("InAppUpdate", "TriggeredDate", 0) < inAppUpdateTriggeredTime) {

                                startActivity(Intent(applicationContext, InAppUpdateProcess::class.java)
                                        .putExtra("UPDATE_CHANGE_LOG", firebaseRemoteConfig.getString(functionsClass.upcomingChangeLogRemoteConfigKey()))
                                        .putExtra("UPDATE_VERSION", firebaseRemoteConfig.getLong(functionsClass.versionCodeRemoteConfigKey()).toString())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        ActivityOptions.makeCustomAnimation(applicationContext, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
                            }

                            updateAvailable = true
                        }
                    }
                }
    }

    override fun onPause() {
        super.onPause()

        getSharedPreferences("ShortcutsModeView", Context.MODE_PRIVATE).edit().apply {
            putString("TabsView", NormalAppShortcutsSelectionListPhone::class.java.simpleName)
            apply()
        }
    }

    override fun onBackPressed() {
        if (functionsClass.UsageAccessEnabled()) {
            this@NormalAppShortcutsSelectionListPhone.finish()
        } else {
            val homeScreen = Intent(Intent.ACTION_MAIN).apply {
                this.addCategory(Intent.CATEGORY_HOME)
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeScreen,
                    ActivityOptions.makeCustomAnimation(applicationContext, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        }
    }

    override fun onSwipeGesture(gestureConstants: GestureConstants, downMotionEvent: MotionEvent, moveMotionEvent: MotionEvent, initVelocityX: Float, initVelocityY: Float) {
        super.onSwipeGesture(gestureConstants, downMotionEvent, moveMotionEvent, initVelocityX, initVelocityY)

        when (gestureConstants) {
            is GestureConstants.SwipeHorizontal -> {
                when (gestureConstants.horizontalDirection) {
                    GestureListenerConstants.SWIPE_RIGHT -> {

                    }
                    GestureListenerConstants.SWIPE_LEFT -> {
                        functionsClass.navigateToClass(this@NormalAppShortcutsSelectionListPhone, SplitShortcuts::class.java,
                                ActivityOptions.makeCustomAnimation(applicationContext, R.anim.slide_from_right, R.anim.slide_to_left))
                    }
                }
            }
            else -> {}
        }
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent?): Boolean {
        motionEvent?.let { swipeGestureListener.onTouchEvent(it) }

        return super.dispatchTouchEvent(motionEvent)
    }

    /*ConfirmButtonProcess*/
    override fun savedShortcutCounter() {

        normalAppSelectionBinding.selectedShortcutCounterView.text = functionsClass.countLineInnerFile(NormalAppShortcutsSelectionListPhone.NormalApplicationsShortcutsFile).toString()
    }

    override fun showSavedShortcutList() {

        if (getFileStreamPath(NormalAppShortcutsSelectionListPhone.NormalApplicationsShortcutsFile).exists()
                && functionsClass.countLineInnerFile(NormalAppShortcutsSelectionListPhone.NormalApplicationsShortcutsFile) > 0) {

            selectedAppsListItem.clear()

            functionsClass.readFileLine(NormalAppShortcutsSelectionListPhone.NormalApplicationsShortcutsFile)?.let {

                for (aSavedLine in it) {

                    try {
                        val aLineSplit = aSavedLine.split("|")

                        val packageName = aLineSplit[0]
                        val className = aLineSplit[1]

                        val activityInfo = packageManager.getActivityInfo(ComponentName(packageName, className), 0)

                        selectedAppsListItem.add(AdapterItemsData(
                                functionsClass.activityLabel(activityInfo),
                                packageName,
                                className,
                                if (functionsClass.customIconsEnable()) {
                                    loadCustomIcons.getDrawableIconForPackage(packageName, functionsClass.activityIcon(activityInfo))
                                } else {
                                    functionsClass.activityIcon(activityInfo)
                                }
                        ))

                    } catch (e: PackageManager.NameNotFoundException) {
                        e.printStackTrace()
                    }
                }

                val savedAppsListPopupAdapter = SavedAppsListPopupAdapter(
                        applicationContext,
                        functionsClass,
                        selectedAppsListItem,
                        this@NormalAppShortcutsSelectionListPhone
                )

                listPopupWindow.apply {
                    anchorView = normalAppSelectionBinding.confirmLayout
                    width = functionsClass.DpToInteger(300)
                    height = ListPopupWindow.WRAP_CONTENT
                    promptPosition = ListPopupWindow.POSITION_PROMPT_ABOVE
                    isModal = true
                    setDropDownGravity(Gravity.CENTER)
                    setBackgroundDrawable(null)
                }

                listPopupWindow.setOnDismissListener {

                    appsConfirmButtonPhone?.makeItVisible()
                }

                listPopupWindow.setAdapter(savedAppsListPopupAdapter)
                listPopupWindow.show()
            }
        }
    }

    override fun hideSavedShortcutList() {

        listPopupWindow.dismiss()
    }

    override fun shortcutDeleted() {

        resetAdapter = true

        loadInstalledAppsData()

        listPopupWindow.dismiss()
    }

    override fun reevaluateShortcutsInfo() {

        evaluateShortcutsInfo()
    }
    /*ConfirmButtonProcess*/

    private fun initializeLoadingProcess() {

        if (functionsClass.customIconsEnable()) {
            loadCustomIcons.load()
        }

        if (functionsClass.UsageAccessEnabled()) {

            smartPickProcess()

        } else {

            if (!functionsClass.mixShortcuts()) {

                if (applicationContext.getFileStreamPath(".mixShortcuts").exists()) {

                    functionsClass.readFileLine(".mixShortcuts")?.let {

                        for (mixShortcutLine in it) {
                            when {
                                mixShortcutLine.contains(".CategorySelected") -> {
                                    applicationContext.deleteFile(functionsClass.categoryNameSelected(mixShortcutLine))
                                }
                                mixShortcutLine.contains(".SplitSelected") -> {
                                    applicationContext.deleteFile(functionsClass.splitNameSelected(mixShortcutLine))
                                }
                                else -> {
                                    applicationContext.deleteFile(functionsClass.packageNameSelected(mixShortcutLine))
                                }
                            }

                        }
                    }

                    applicationContext.deleteFile(".mixShortcuts")
                }
            }

            if (applicationContext.getFileStreamPath(".superFreq").exists()) {
                applicationContext.deleteFile(".superFreq")
            }

            /* Load Installed Applications */
            loadInstalledAppsData()
            /* Load Installed Applications */
        }
    }
}
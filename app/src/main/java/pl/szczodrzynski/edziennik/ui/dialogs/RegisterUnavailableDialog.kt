/*
 * Copyright (c) Kuba Szczodrzyński 2020-9-3.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.api.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.BuildConfig
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus
import pl.szczodrzynski.edziennik.databinding.DialogRegisterUnavailableBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.utils.Utils
import kotlin.coroutines.CoroutineContext

class RegisterUnavailableDialog(
        val activity: AppCompatActivity,
        val status: RegisterAvailabilityStatus,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "RegisterUnavailableDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init { run {
        if (activity.isFinishing)
            return@run
        if (status.available && status.minVersionCode <= BuildConfig.VERSION_CODE)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        if (!status.available && status.message != null) {
            val b = DialogRegisterUnavailableBinding.inflate(LayoutInflater.from(activity), null, false)
            b.message = status.message
            if (status.message.image != null)
                b.image.load(status.message.image)
            if (status.message.url != null) {
                b.readMore.onClick {
                    Utils.openUrl(activity, status.message.url)
                }
            }
            dialog = MaterialAlertDialogBuilder(activity)
                    .setView(b.root)
                    .setPositiveButton(R.string.close) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setOnDismissListener {
                        onDismissListener?.invoke(TAG)
                    }
                    .show()
        }

        val update = app.config.update
        if (status.minVersionCode > BuildConfig.VERSION_CODE) {
            if (update != null && update.versionCode >= status.minVersionCode) {
                UpdateAvailableDialog(activity, update, true, onShowListener, onDismissListener)
            }
            else {
                // this *should* never happen
                dialog = MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.update_available_title)
                        .setMessage(R.string.update_available_fallback)
                        .setPositiveButton(R.string.update_available_button) { dialog, _ ->
                            Utils.openGooglePlay(activity)
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .setOnDismissListener {
                            onDismissListener?.invoke(TAG)
                        }
                        .show()
            }
            return@run
        }
    }}
}

/*
 * Copyright © 2020 By Geeks Empire.
 *
 * Created by Elias Fazel
 * Last modified 5/10/20 3:39 PM
 *
 * Licensed Under MIT License.
 * https://opensource.org/licenses/MIT
 */

package net.geekstools.supershortcuts.PRO.ApplicationsShortcuts.Adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import java.lang.Float.min
import kotlin.math.absoluteValue

class CurveWearLayoutManager(var maxIconProgress: Float = 0.65f) : WearableLinearLayoutManager.LayoutCallback() {

    private var progressToCenter: Float = 0f

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        child.apply {
            // Figure out % progress from top to bottom
            val centerOffset = height.toFloat() / 2.0f / parent.height.toFloat()
            val yRelativeToCenterOffset = y / parent.height + centerOffset

            // Normalize for center
            progressToCenter = (0.5f - yRelativeToCenterOffset).absoluteValue
            // Adjust to the maximum scale
            progressToCenter = min(progressToCenter, maxIconProgress)

            scaleX = 1 - progressToCenter
            scaleY = 1 - progressToCenter
        }
    }
}
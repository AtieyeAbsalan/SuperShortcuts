package net.geekstools.supershortcuts.PRO.Util.NavAdapter;

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

public class RecycleViewSmoothLayout extends LinearLayoutManager {

    private static final float MILLISECONDS_PER_INCH = 27f;
    private Context context;

    public RecycleViewSmoothLayout(Context context, int Orientation, boolean reverseLayout) {
        super(context, Orientation, reverseLayout);
        this.context = context;
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(context) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return RecycleViewSmoothLayout.this.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}

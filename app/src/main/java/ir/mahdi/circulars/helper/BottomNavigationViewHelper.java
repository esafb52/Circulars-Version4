package ir.mahdi.circulars.helper;

import android.content.Context;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

public class BottomNavigationViewHelper extends CoordinatorLayout.Behavior<BottomNavigationView>{
    public static void disableShiftMode(BottomNavigationView view) {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                //noinspection RestrictedApi
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                //noinspection RestrictedApi
                item.setChecked(item.getItemData().isChecked());
            }
        } catch (NoSuchFieldException e) {
            Log.e("BNVHelper", "Unable to get shift mode field", e);
        } catch (IllegalAccessException e) {
            Log.e("BNVHelper", "Unable to change value of shift mode", e);
        }
    }


    public BottomNavigationViewHelper() {
        super();
    }

    public BottomNavigationViewHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, BottomNavigationView child, View dependency) {
        boolean dependsOn = dependency instanceof FrameLayout;
        return dependsOn;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, BottomNavigationView child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, BottomNavigationView child, View target, int dx, int dy, int[] consumed) {
        if (dy < 0) {
            showBottomNavigationView(child);
        } else if (dy > 0) {
            hideBottomNavigationView(child);
        }
    }

    private void hideBottomNavigationView(BottomNavigationView view) {
        view.animate().translationY(view.getHeight());
    }

    private void showBottomNavigationView(BottomNavigationView view) {
        view.animate().translationY(0);
    }
}
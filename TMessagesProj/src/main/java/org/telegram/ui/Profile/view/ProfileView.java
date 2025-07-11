package org.telegram.ui.Profile.view;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Profile.model.ProfileState;

public class ProfileView extends ScrollView {

    RecyclerListView parentListView;
    GestureDetector gestureDetector;

    public ProfileView(Context context) {
        super(context);

        parentListView = new RecyclerListView(context);
        addView(parentListView);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public RecyclerListView getParentListView() {
        return parentListView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        return true;
    }

    public void bind(ProfileState state) {
    }
}

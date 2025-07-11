package org.telegram.ui.Profile.view;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Observer;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Profile.viewmodel.ProfileViewModel;

import lombok.Getter;

public class ProfileActivity extends BaseFragment implements LifecycleOwner {

    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    @Getter private ProfileViewModel viewModel;
    private ProfileView profileView;
    private ProfileHeaderView profileHeaderView;

    SharedMediaLayout.SharedMediaPreloader preloader;

    public ProfileActivity(Bundle args) {
        super(args);
    }

    public ProfileActivity(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        super(args);
        this.preloader = preloader;
    }

    public static ProfileActivity of(long dialogId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }

        return new ProfileActivity(bundle);
    }

    @Override
    public View createView(Context context) {
        FrameLayout container = new FrameLayout(context);

        profileView = new ProfileView(context);
        profileHeaderView = new ProfileHeaderView(context, resourceProvider, profileView.getParentListView());

        profileView.setGestureDetector(profileHeaderView.getGestureDetector());

        container.addView(profileView);
        container.addView(profileHeaderView);

        // Set up header manually
        profileHeaderView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                Gravity.TOP
        ));

        actionBar = null;
        fragmentView = container;
        initViewModel();
        return fragmentView;
    }

    private void initViewModel() {
        // Parse arguments
        Bundle args = getArguments();
        int classGuid = this.classGuid; // assuming inherited from BaseFragment
        viewModel = new ProfileViewModel(args, classGuid, resourceProvider);

        observeState();

        viewModel.loadArgs();
    }

    private void observeState() {
        viewModel.getState().observe(this, newState -> {
            profileView.bind(newState);
            profileHeaderView.bind(newState);
        });
    }

    @Override
    public boolean onFragmentCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        if (viewModel != null) {
            //viewModel.cleanup();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Override
    public void onPause() {
        super.onPause();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}

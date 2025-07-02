package org.telegram.ui.Profile.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Profile.model.ProfileState;

public class ProfileHeaderView extends FrameLayout {

    TextView nameTextView;
    TextView statusTextView;
    LinearLayout quickActions;

    public ProfileHeaderView(@NonNull Context context) {
        super(context);

        nameTextView = new TextView(context);
        nameTextView.setTextSize(SharedConfig.fontSize);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        nameTextView.setSingleLine(true);
        nameTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(nameTextView);

        statusTextView = new TextView(context);
        statusTextView.setTextSize(SharedConfig.fontSize);
        statusTextView.setEllipsize(TextUtils.TruncateAt.END);
        statusTextView.setSingleLine(true);
        statusTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(statusTextView);

        quickActions = new LinearLayout(context);
        quickActions.setOrientation(LinearLayout.HORIZONTAL);
        quickActions.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(quickActions);
    }

    public void bind(ProfileState state) {
        nameTextView.setText(String.join(" ", state.getUser().last_name, state.getUser().first_name));
        String status = LocaleController.formatUserStatus(UserConfig.selectedAccount, state.getUser());
        statusTextView.setText(status);
    }
}

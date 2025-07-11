package org.telegram.ui.Profile.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;

public class ProfileHeaderQuickActionsButton extends LinearLayout {
    private final ImageView iconView;
    private final TextView textView;

    public ProfileHeaderQuickActionsButton(Context context, int iconResId, String text) {
        super(context);

        // Layout config
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int padding = AndroidUtilities.dp(4);
        setPadding(padding, padding, padding, padding);

        // Set background: semi-transparent black with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x30000000); // semi-transparent black
        background.setCornerRadius(AndroidUtilities.dp(12));
        setBackground(background);

        // Enable click effects
        setClickable(true);
        setFocusable(true);

        // Icon
        iconView = new ImageView(context);
        LayoutParams iconParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        iconView.setImageDrawable(ContextCompat.getDrawable(context, iconResId));
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setLayoutParams(iconParams);
        addView(iconView);

        // Text
        textView = new TextView(context);
        LayoutParams textParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        textParams.topMargin = AndroidUtilities.dp(4);
        textView.setLayoutParams(textParams);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setTextColor(0xFFFFFFFF);
        textView.setGravity(Gravity.CENTER);
        addView(textView);
    }

    public void setIcon(int resId) {
        iconView.setImageResource(resId);
    }

    public void setText(String text) {
        textView.setText(text);
    }
}
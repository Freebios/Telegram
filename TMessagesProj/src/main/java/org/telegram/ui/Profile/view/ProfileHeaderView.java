package org.telegram.ui.Profile.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.AvatarPreviewPagerIndicator;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CrossfadeDrawable;
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScamDrawable;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.Profile.model.ProfileState;

import java.util.ArrayList;
import java.util.List;

public class ProfileHeaderView extends FrameLayout {

    private float mediaHeaderAnimationProgress = 0;

    public enum ProfileHeaderMode {
        FULL,
        CIRCLE_CENTERED,
        COLLAPSIBLE,
        MINIMAL
    }

    private int marginTop = AndroidUtilities.statusBarHeight;

    private int QUICK_ACTIONS_HEIGHT = AndroidUtilities.dp(90);
    private int HEADER_FULL_HEIGHT = AndroidUtilities.displayMetrics.widthPixels + QUICK_ACTIONS_HEIGHT;
    private int HEADER_INTERMEDIATE_HEIGHT = AndroidUtilities.dp(150);
    private int HEADER_MINIMAL_HEIGHT;

    Theme.ResourcesProvider resourcesProvider;

    ProfileGalleryView profileGalleryView;
    AvatarPreviewPagerIndicator avatarPreviewPagerIndicator;
    GradientDrawable gradientDrawable;
    SimpleTextView nameTextView;
    TextView statusTextView;
    ProfileHeaderQuickActions quickActions;

    private DrawerProfileCell.AnimatedStatusView animatedStatusView;


    private Drawable lockIconDrawable = null;
    private final Drawable[] verifiedDrawable = new Drawable[2];
    private final Drawable[] premiumStarDrawable = new Drawable[2];
    private Long emojiStatusGiftId;
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] emojiStatusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] botVerificationDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final Drawable[] verifiedCheckDrawable = new Drawable[2];
    private final CrossfadeDrawable[] verifiedCrossfadeDrawable = new CrossfadeDrawable[2];
    private final CrossfadeDrawable[] premiumCrossfadeDrawable = new CrossfadeDrawable[2];
    private ScamDrawable scamDrawable;

    private MessagesController.PeerColor peerColor = null;
    private int color1, color2, emojiColor;

    private int currentHeight;

    private GestureDetector gestureDetector;
    private float scrollProgress;

    public ProfileHeaderView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, RecyclerListView parentListView) {
        super(context);

        this.resourcesProvider = resourcesProvider;
        Theme.createProfileResources(context);

        setupGestureDetector();

        WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(this);
        if (rootWindowInsets != null) {
            Insets insets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            marginTop = insets.top;
        }

        // Create a vertical gradient background
        gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        Theme.getColor(Theme.key_actionBarDefault, resourcesProvider),
                        Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)
                }
        );

        avatarPreviewPagerIndicator = new AvatarPreviewPagerIndicator(context);
        profileGalleryView = new ProfileGalleryView(context, null, parentListView, avatarPreviewPagerIndicator);
        profileGalleryView.setBlur(true);
        avatarPreviewPagerIndicator.setProfileGalleryView(profileGalleryView);

        addView(profileGalleryView);
        addView(avatarPreviewPagerIndicator);

        nameTextView = new SimpleTextView(context);
        nameTextView.setTextSize(24);
        nameTextView.setEllipsizeByGradient(true);
        nameTextView.setMaxLines(1);
        nameTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nameTextView.setTextColor(Theme.getColor(Theme.key_avatar_text, resourcesProvider));
        nameTextView.setTypeface(Theme.chat_contactNamePaint.getTypeface());
        addView(nameTextView);

        statusTextView = new TextView(context);
        statusTextView.setTextSize(SharedConfig.fontSize);
        statusTextView.setEllipsize(TextUtils.TruncateAt.END);
        statusTextView.setSingleLine(true);
        statusTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        statusTextView.setTextColor(Theme.getColor(Theme.key_profile_status, resourcesProvider));
        statusTextView.setTypeface(Theme.avatar_backgroundPaint.getTypeface(), Typeface.NORMAL);
        addView(statusTextView);

        HEADER_MINIMAL_HEIGHT = marginTop;

        quickActions = new ProfileHeaderQuickActions(context);
        quickActions.addButton(new ProfileHeaderQuickActionsButton(context, R.drawable.msg_filled_data_messages, "Message"));
        quickActions.addButton(new ProfileHeaderQuickActionsButton(context, R.drawable.list_mute, "Mute"));
        quickActions.addButton(new ProfileHeaderQuickActionsButton(context, R.drawable.ic_call, "Call"));
        quickActions.addButton(new ProfileHeaderQuickActionsButton(context, R.drawable.calls_video, "Video"));
        addView(quickActions);

        animatedStatusView = new DrawerProfileCell.AnimatedStatusView(context, 10, 10);
        addView(animatedStatusView);

        updateColors(null);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                adjustHeight((int) -distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                animateToStickyPosition(velocityY);
                return true;
            }
        });
    }

    public GestureDetector getGestureDetector() {
        return gestureDetector;
    }

    private void adjustHeight(int dy) {
        currentHeight = Math.max(HEADER_MINIMAL_HEIGHT, Math.min(HEADER_FULL_HEIGHT, currentHeight + dy));
        getLayoutParams().height = currentHeight;
        requestLayout();

        float progress = (float) (HEADER_FULL_HEIGHT - currentHeight) / (HEADER_FULL_HEIGHT - HEADER_MINIMAL_HEIGHT);
        setScrollProgress(progress);
    }

    private void animateToStickyPosition(float velocityY) {
        int targetHeight;

        if (velocityY < -2000 || currentHeight > (HEADER_FULL_HEIGHT + HEADER_INTERMEDIATE_HEIGHT) / 2) {
            targetHeight = HEADER_FULL_HEIGHT;
        } else if (currentHeight <= (HEADER_INTERMEDIATE_HEIGHT + HEADER_MINIMAL_HEIGHT) / 2) {
            targetHeight = HEADER_MINIMAL_HEIGHT;
        } else {
            targetHeight = HEADER_INTERMEDIATE_HEIGHT;
        }

        ValueAnimator animator = ValueAnimator.ofInt(currentHeight, targetHeight);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            currentHeight = (int) animation.getAnimatedValue();
            getLayoutParams().height = currentHeight;
            requestLayout();

            float progress = (float) (HEADER_FULL_HEIGHT - currentHeight) / (HEADER_FULL_HEIGHT - HEADER_MINIMAL_HEIGHT);
            setScrollProgress(progress);
        });
        animator.start();
    }

    public void setScrollProgress(float progress) {
        this.scrollProgress = Math.max(0f, Math.min(1f, progress));
        applyLayoutForScroll();
    }

    public void updateColors(ProfileState state) {
        peerColor = null;
        if (state!= null && state.getUser() != null) {
            peerColor = MessagesController.PeerColor.fromCollectible(state.getUser().emoji_status);
            if (peerColor == null) {
                final int colorId = UserObject.getProfileColorId(state.getUser());
                final MessagesController.PeerColors peerColors = MessagesController.getInstance(UserConfig.selectedAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
        }

        if (peerColor != null) {
            color1 = peerColor.getBgColor1(Theme.isCurrentThemeDark());
            color2 = peerColor.getBgColor2(Theme.isCurrentThemeDark());
        } else {
            color1 = Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider);
            color2 = color1;
        }
        emojiColor = PeerColorActivity.adaptProfileEmojiColor(color1);

        gradientDrawable.setColors(new int[] {
                color2,
                color1
        });
        setBackground(gradientDrawable);
        updateEmojiStatusDrawableColor();
    }

    public void bind(ProfileState state) {
        if (state.getUser() != null) {
            List<String> elements = new ArrayList<>();
            if (state.getUser().last_name != null) {
                elements.add(state.getUser().last_name);
            }
            if (state.getUser().first_name != null) {
                elements.add(state.getUser().first_name);
            }
            nameTextView.setText(String.join(" ", elements.toArray(new String[0])));

            String status = LocaleController.formatUserStatus(UserConfig.selectedAccount, state.getUser());
            statusTextView.setText(status);

            if (state.getUser().bot) {
                nameTextView.setRightDrawable(state.getUser().verified ? getVerifiedCrossfadeDrawable(0) : null);
            } else if (state.getUser().premium){
                nameTextView.setRightDrawable(state.getUser().emoji_status != null ? getEmojiStatusDrawable(state.getUser().emoji_status, true, true, 0) : getPremiumCrossfadeDrawable(0));
            }

            if (state.getDialogId() != null) {
                profileGalleryView.setData(state.getDialogId());
            }

            quickActions.bind(state);
            updateColors(state);
        }

        if (state.getChatInfo() != null) {
            profileGalleryView.setChatInfo(state.getChatInfo());

            if (state.getChatInfo().bot_verification != null && state.getChatInfo().bot_verification.icon != 0) {
                nameTextView.setLeftDrawable(getBotVerificationDrawable(state.getChatInfo().bot_verification.icon, false, 0));
            }

            if (state.getCurrentEncryptedChat() != null) {
                nameTextView.setLeftDrawable(state.getCurrentEncryptedChat() != null ? getLockIconDrawable() : null);
            }
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void applyLayoutForScroll() {
        ProfileHeaderMode currentMode;
        float subProgress;

        // Determine the current mode based on scroll progress
        if (scrollProgress < 0.25f) {
            currentMode = ProfileHeaderMode.FULL;
            subProgress = scrollProgress * 4;
        } else if (scrollProgress < 0.5f) {
            currentMode = ProfileHeaderMode.CIRCLE_CENTERED;
            subProgress = (scrollProgress - 0.25f) * 4;
        } else if (scrollProgress < 0.75f) {
            currentMode = ProfileHeaderMode.COLLAPSIBLE;
            subProgress = (scrollProgress - 0.5f) * 4;
        } else {
            currentMode = ProfileHeaderMode.MINIMAL;
            subProgress = (scrollProgress - 0.75f) * 4;
        }

        float profileAlpha = 0f;

        // Apply specific layout for each mode
        switch (currentMode) {
            case FULL:
                // Name and status displayed on bottom left
                nameTextView.setTranslationX(lerp(
                        AndroidUtilities.dp(16),
                        (getWidth() - nameTextView.getTextWidth()) / 2f,
                        subProgress));
                nameTextView.setGravity(android.view.Gravity.START);

                statusTextView.setTextSize(AndroidUtilities.dpf2(6f));
                statusTextView.setTranslationX(lerp(
                        AndroidUtilities.dp(16),
                        (getWidth() - statusTextView.getWidth()) / 2f,
                        subProgress));
                statusTextView.setGravity(android.view.Gravity.START);

                profileAlpha = 1 - subProgress;
                break;

            case CIRCLE_CENTERED:
                // Name and status centered a bit under the middle of the view
                nameTextView.setTranslationX((getWidth() - nameTextView.getWidth()) / 2f);
                nameTextView.setGravity(android.view.Gravity.CENTER);

                statusTextView.setTextSize(AndroidUtilities.dpf2(6f));
                statusTextView.setTranslationX((getWidth() - statusTextView.getWidth()) / 2f);
                statusTextView.setGravity(android.view.Gravity.CENTER);
                break;

            case COLLAPSIBLE:
                nameTextView.setTranslationX(getWidth() / 2f - (nameTextView.getWidth()) / 2f);
                nameTextView.setGravity(android.view.Gravity.CENTER);

                statusTextView.setTextSize(AndroidUtilities.dpf2(6f));
                statusTextView.setTranslationX(getWidth() / 2f - statusTextView.getWidth() / 2f);
                statusTextView.setGravity(android.view.Gravity.CENTER);
                break;

            case MINIMAL:
                nameTextView.setTranslationX(getWidth() / 2f - (nameTextView.getWidth()) / 2f);
                nameTextView.setGravity(android.view.Gravity.CENTER);

                statusTextView.setTextSize(AndroidUtilities.dpf2(6f));
                statusTextView.setTranslationX(getWidth() / 2f - statusTextView.getWidth() / 2f);
                statusTextView.setGravity(android.view.Gravity.CENTER);
                break;
        }
        HEADER_MINIMAL_HEIGHT = marginTop + nameTextView.getHeight() + statusTextView.getHeight();
        profileGalleryView.setAlpha(profileAlpha);

        int availableQuickActionsHeight = Math.max(0, getHeight() - nameTextView.getHeight() - statusTextView.getHeight() - marginTop);
        // Apply common properties
        quickActions.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(availableQuickActionsHeight, QUICK_ACTIONS_HEIGHT)));
        quickActions.setTranslationY(getHeight() - quickActions.getHeight());

        nameTextView.setTranslationY(Math.max(
                marginTop,
                getHeight() - quickActions.getHeight() - statusTextView.getHeight() - nameTextView.getHeight()));
        statusTextView.setTranslationY(Math.max(
                marginTop + nameTextView.getHeight(),
                getHeight() - quickActions.getHeight() - statusTextView.getHeight()));
    }

    public void animateToScrollProgress(float targetProgress) {
        ValueAnimator animator = ValueAnimator.ofFloat(scrollProgress, targetProgress);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            setScrollProgress((float) animation.getAnimatedValue());
        });
        animator.start();
    }

    private boolean fragmentViewAttached;
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Update height
        adjustHeight(0);

        fragmentViewAttached = true;

        for (int i = 0; i < emojiStatusDrawable.length; i++) {
            if (emojiStatusDrawable[i] != null) {
                emojiStatusDrawable[i].attach();
            }
        }
        for (int i = 0; i < botVerificationDrawable.length; ++i) {
            if (botVerificationDrawable[i] != null) {
                botVerificationDrawable[i].attach();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        fragmentViewAttached = false;

        for (int i = 0; i < emojiStatusDrawable.length; i++) {
            if (emojiStatusDrawable[i] != null) {
                emojiStatusDrawable[i].detach();
            }
        }
        for (int i = 0; i < botVerificationDrawable.length; ++i) {
            if (botVerificationDrawable[i] != null) {
                botVerificationDrawable[i].detach();
            }
        }
    }

    private Drawable getScamDrawable(int type) {
        if (scamDrawable == null) {
            scamDrawable = new ScamDrawable(11, type);
            scamDrawable.setColor(Theme.getColor(Theme.key_avatar_subtitleInProfileBlue, resourcesProvider));
        }
        return scamDrawable;
    }

    private Drawable getLockIconDrawable() {
        if (lockIconDrawable == null) {
            lockIconDrawable = Theme.chat_lockIconDrawable.getConstantState().newDrawable().mutate();
        }
        return lockIconDrawable;
    }

    private Drawable getVerifiedCrossfadeDrawable(int a) {
        if (verifiedCrossfadeDrawable[a] == null) {
            verifiedDrawable[a] = Theme.profile_verifiedDrawable.getConstantState().newDrawable().mutate();
            verifiedCheckDrawable[a] = Theme.profile_verifiedCheckDrawable.getConstantState().newDrawable().mutate();
            if (a == 1 && peerColor != null) {
                int color = Theme.adaptHSV(peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f);
                verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, Theme.getColor(Theme.key_player_actionBarTitle, resourcesProvider), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
                color = Color.WHITE;
                verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            verifiedCrossfadeDrawable[a] = new CrossfadeDrawable(
                new CombinedDrawable(verifiedDrawable[a], verifiedCheckDrawable[a]),
                ContextCompat.getDrawable(getContext(), R.drawable.verified_profile)
            );
        }
        return verifiedCrossfadeDrawable[a];
    }

    private Drawable getPremiumCrossfadeDrawable(int a) {
        if (premiumCrossfadeDrawable[a] == null) {
            premiumStarDrawable[a] = ContextCompat.getDrawable(getContext(), R.drawable.msg_premium_liststar).mutate();
            int color = Theme.getColor(Theme.key_profile_verifiedBackground, resourcesProvider);
            if (a == 1) {
                color = color; // Don't apply peer color
            }
            premiumStarDrawable[a].setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            premiumCrossfadeDrawable[a] = new CrossfadeDrawable(premiumStarDrawable[a], ContextCompat.getDrawable(getContext(), R.drawable.msg_premium_prolfilestar).mutate());
        }
        return premiumCrossfadeDrawable[a];
    }
    private Drawable getBotVerificationDrawable(long icon, boolean animated, int a) {
        if (botVerificationDrawable[a] == null) {
            botVerificationDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView, AndroidUtilities.dp(17), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            botVerificationDrawable[a].offset(0, AndroidUtilities.dp(1));
            if (fragmentViewAttached) {
                botVerificationDrawable[a].attach();
            }
        }
        if (icon != 0) {
            botVerificationDrawable[a].set(icon, animated);
        } else {
            botVerificationDrawable[a].set((Drawable) null, animated);
        }
        updateEmojiStatusDrawableColor();
        return botVerificationDrawable[a];
    }

    private Drawable getEmojiStatusDrawable(TLRPC.EmojiStatus emojiStatus, boolean switchable, boolean animated, int a) {
        if (emojiStatusDrawable[a] == null) {
            emojiStatusDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView, AndroidUtilities.dp(24), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            if (fragmentViewAttached) {
                emojiStatusDrawable[a].attach();
            }
        }
        if (a == 1) {
            emojiStatusGiftId = null;
        }
        if (emojiStatus instanceof TLRPC.TL_emojiStatus) {
            final TLRPC.TL_emojiStatus status = (TLRPC.TL_emojiStatus) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else if (emojiStatus instanceof TLRPC.TL_emojiStatusCollectible) {
            final TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                if (a == 1) {
                    emojiStatusGiftId = status.collectible_id;
                }
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(true, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else {
            emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
            emojiStatusDrawable[a].setParticles(false, animated);
        }
        updateEmojiStatusDrawableColor();
        return emojiStatusDrawable[a];
    }

    private float lastEmojiStatusProgress;
    private void updateEmojiStatusDrawableColor() {
        updateEmojiStatusDrawableColor(lastEmojiStatusProgress);
    }
    private void updateEmojiStatusDrawableColor(float progress) {
        for (int a = 0; a < 2; ++a) {
            final int fromColor;
            if (peerColor != null && a == 1) {
                fromColor = ColorUtils.blendARGB(peerColor.getStoryColor1(Theme.isCurrentThemeDark()), 0xFFFFFFFF, 0.25f);
            } else {
                fromColor = AndroidUtilities.getOffsetColor(Theme.getColor(Theme.key_profile_verifiedBackground, resourcesProvider), Theme.getColor(Theme.key_player_actionBarTitle, resourcesProvider), mediaHeaderAnimationProgress, 1.0f);
            }
            final int color = ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0xffffffff, progress), Theme.getColor(Theme.key_player_actionBarTitle, resourcesProvider), mediaHeaderAnimationProgress);
            if (emojiStatusDrawable[a] != null) {
                emojiStatusDrawable[a].setColor(color);
            }
            if (botVerificationDrawable[a] != null) {
                botVerificationDrawable[a].setColor(ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0x99ffffff, progress), Theme.getColor(Theme.key_player_actionBarTitle, resourcesProvider), mediaHeaderAnimationProgress));
            }
            if (a == 1) {
                animatedStatusView.setColor(color);
            }
        }
        lastEmojiStatusProgress = progress;
    }

    private void updateEmojiStatusEffectPosition() {
        /*
        animatedStatusView.setScaleX(nameTextView[1].getScaleX());
        animatedStatusView.setScaleY(nameTextView[1].getScaleY());
        animatedStatusView.translate(
            nameTextView[1].getX() + nameTextView[1].getRightDrawableX() * nameTextView[1].getScaleX(),
            nameTextView[1].getY() + (nameTextView[1].getHeight() - (nameTextView[1].getHeight() - nameTextView[1].getRightDrawableY()) * nameTextView[1].getScaleY())
        );*/
    }
}

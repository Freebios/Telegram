/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Profile;

import static androidx.core.view.ViewCompat.TYPE_TOUCH;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.ContactsController.PRIVACY_RULES_TYPE_ADDED_BY_PHONE;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Stars.StarsIntroActivity.formatStarsAmountShort;
import static org.telegram.ui.bots.AffiliateProgramFragment.percents;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Property;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.AuthTokensHelper;
import org.telegram.messenger.BillingController;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_fragment;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.OKLCH;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.ArchivedStickersActivity;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.AutoDeleteMessagesActivity;
import org.telegram.ui.Business.OpeningHoursActivity;
import org.telegram.ui.Business.ProfileHoursCell;
import org.telegram.ui.Business.ProfileLocationCell;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Cells.SettingsSuggestionCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChangeBioActivity;
import org.telegram.ui.ChangeNameActivity;
import org.telegram.ui.ChangeUsernameActivity;
import org.telegram.ui.ChannelAdminLogActivity;
import org.telegram.ui.ChannelMonetizationLayout;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatRightsEditActivity;
import org.telegram.ui.ChatUsersActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.AutoDeletePopupWrapper;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackButtonMenu;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.CanvasButton;
import org.telegram.ui.Components.ChatActivityInterface;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.ChatNotificationsPopupWrapper;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CrossfadeDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DotDividerSpan;
import org.telegram.ui.Components.EmojiPacksAlert;
import org.telegram.ui.Components.EmptyStubSpan;
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.HintView;
import org.telegram.ui.Components.IdenticonDrawable;
import org.telegram.ui.Components.ImageUpdater;
import org.telegram.ui.Components.InstantCameraView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.MessagePrivateSeenView;
import org.telegram.ui.Components.Paint.PersistColorPalette;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.Premium.PremiumGradient;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.Premium.ProfilePremiumCell;
import org.telegram.ui.Components.Premium.boosts.UserSelectorBottomSheet;
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScamDrawable;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickerEmptyView;
import org.telegram.ui.Components.TimerDrawable;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.VectorAvatarThumbDrawable;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.ContactAddActivity;
import org.telegram.ui.DataAutoDownloadActivity;
import org.telegram.ui.DataSettingsActivity;
import org.telegram.ui.DataUsage2Activity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FiltersSetupActivity;
import org.telegram.ui.FragmentUsernameBottomSheet;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.GroupCreateActivity;
import org.telegram.ui.IdenticonActivity;
import org.telegram.ui.LanguageSelectActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LiteModeSettingsActivity;
import org.telegram.ui.LocationActivity;
import org.telegram.ui.LoginActivity;
import org.telegram.ui.MemberRequestsActivity;
import org.telegram.ui.NotificationsCustomSettingsActivity;
import org.telegram.ui.NotificationsSettingsActivity;
import org.telegram.ui.PasscodeActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PinchToZoomHelper;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.PrivacyControlActivity;
import org.telegram.ui.PrivacySettingsActivity;
import org.telegram.ui.PrivacyUsersActivity;
import org.telegram.ui.Profile.model.ProfileState;
import org.telegram.ui.Profile.model.ProfileUiEvent;
import org.telegram.ui.Profile.utils.ActionBarActions;
import org.telegram.ui.Profile.view.OverlaysView;
import org.telegram.ui.Profile.view.ProfileActionBar;
import org.telegram.ui.Profile.view.ProfileAvatarImageView;
import org.telegram.ui.Profile.view.ProfileHeaderView;
import org.telegram.ui.Profile.view.TopView;
import org.telegram.ui.Profile.viewmodel.ProfileViewModel;
import org.telegram.ui.ProfileBirthdayEffect;
import org.telegram.ui.ProfileNotificationsActivity;
import org.telegram.ui.ProxyListActivity;
import org.telegram.ui.QrActivity;
import org.telegram.ui.ReactionsDoubleTapManageActivity;
import org.telegram.ui.ReportBottomSheet;
import org.telegram.ui.RestrictedLanguagesSelectActivity;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import org.telegram.ui.SessionsActivity;
import org.telegram.ui.Stars.BotStarsActivity;
import org.telegram.ui.Stars.BotStarsController;
import org.telegram.ui.Stars.ProfileGiftsView;
import org.telegram.ui.Stars.StarGiftSheet;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.StatisticActivity;
import org.telegram.ui.StickersActivity;
import org.telegram.ui.Stories.ProfileStoriesView;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.StoriesListPlaceProvider;
import org.telegram.ui.Stories.StoryViewer;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.Stories.recorder.DualCameraView;
import org.telegram.ui.Stories.recorder.HintView2;
import org.telegram.ui.Stories.recorder.StoryRecorder;
import org.telegram.ui.TON.TONIntroActivity;
import org.telegram.ui.ThemeActivity;
import org.telegram.ui.TopicsFragment;
import org.telegram.ui.TopicsNotifySettingsFragments;
import org.telegram.ui.TwoStepVerificationActivity;
import org.telegram.ui.TwoStepVerificationSetupActivity;
import org.telegram.ui.UserInfoActivity;
import org.telegram.ui.WallpapersListActivity;
import org.telegram.ui.bots.AffiliateProgramFragment;
import org.telegram.ui.bots.BotBiometry;
import org.telegram.ui.bots.BotDownloads;
import org.telegram.ui.bots.BotLocation;
import org.telegram.ui.bots.ChannelAffiliateProgramsFragment;
import org.telegram.ui.bots.SetupEmojiStatusSheet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.Getter;

public class ProfileActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate, SharedMediaLayout.SharedMediaPreloaderDelegate, ImageUpdater.ImageUpdaterDelegate, SharedMediaLayout.Delegate, LifecycleOwner {

    // TODO make private and reduce interconnection betweens view
    public ProfileViewModel viewModel;
    private final LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    private final static int PHONE_OPTION_CALL = 0,
        PHONE_OPTION_COPY = 1,
        PHONE_OPTION_TELEGRAM_CALL = 2,
        PHONE_OPTION_TELEGRAM_VIDEO_CALL = 3;


    ProfileHeaderView profileHeaderView;


    private RecyclerListView listView;
    private RecyclerListView searchListView;
    public LinearLayoutManager layoutManager;
    private ListAdapter listAdapter;
    private SearchAdapter searchAdapter;
    private SimpleTextView[] nameTextView = new SimpleTextView[2];
    private String nameTextViewRightDrawableContentDescription = null;
    private String nameTextViewRightDrawable2ContentDescription = null;
    private SimpleTextView[] onlineTextView = new SimpleTextView[4];
    private AudioPlayerAlert.ClippingTextViewSwitcher mediaCounterTextView;
    private RLottieImageView writeButton;
    private AnimatorSet writeButtonAnimation;
    private AnimatorSet qrItemAnimation;
    private Drawable lockIconDrawable;
    private final Drawable[] verifiedDrawable = new Drawable[2];
    private final Drawable[] premiumStarDrawable = new Drawable[2];
    private Long emojiStatusGiftId;
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] emojiStatusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] botVerificationDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final Drawable[] verifiedCheckDrawable = new Drawable[2];
    private final CrossfadeDrawable[] verifiedCrossfadeDrawable = new CrossfadeDrawable[2];
    private final CrossfadeDrawable[] premiumCrossfadeDrawable = new CrossfadeDrawable[2];
    private ScamDrawable scamDrawable;
    private UndoView undoView;
    private OverlaysView overlaysView;
    public SharedMediaLayout sharedMediaLayout;
    private StickerEmptyView emptyView;
    private boolean sharedMediaLayoutAttached;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    private boolean preloadedChannelEmojiStatuses;

    private View blurredView;

    private RLottieDrawable cameraDrawable;
    private RLottieDrawable cellCameraDrawable;

    private HintView fwdRestrictedHint;
    public FrameLayout avatarContainer;
    private FrameLayout avatarContainer2;
    private DrawerProfileCell.AnimatedStatusView animatedStatusView;
    public ProfileAvatarImageView avatarImage;
    private View avatarOverlay;
    private AnimatorSet avatarAnimation;
    private RadialProgressView avatarProgressView;
    private ImageView timeItem;
    private ImageView starBgItem, starFgItem;
    private TimerDrawable timerDrawable;
    @Getter private ProfileGalleryView avatarsViewPager;
    private PagerIndicatorView avatarsViewPagerIndicatorView;
    private AvatarDrawable avatarDrawable;
    public ImageUpdater imageUpdater;
    private int avatarColor;
    TimerDrawable autoDeleteItemDrawable;
    private ProfileStoriesView storyView;
    public ProfileGiftsView giftsView;

    private View scrimView = null;
    private Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {
        @Override
        public void setAlpha(int a) {
            super.setAlpha(a);
            fragmentView.invalidate();
        }
    };
    private Paint actionBarBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ActionBarPopupWindow scrimPopupWindow;
    private Theme.ResourcesProvider resourcesProvider;

    public int overlayCountVisible;

    private ImageLocation prevLoadedImageLocation;

    private int lastMeasuredContentWidth;
    private int lastMeasuredContentHeight;
    private int listContentHeight;
    private boolean openingAvatar;
    private boolean fragmentViewAttached;

    private boolean doNotSetForeground;

    private boolean[] isOnline = new boolean[1];

    private boolean callItemVisible;
    private boolean videoCallItemVisible;
    private boolean editItemVisible;
    private ActionBarMenuItem animatingItem;
    private ActionBarMenuItem callItem;
    private ActionBarMenuItem videoCallItem;
    private ActionBarMenuItem editItem;
    private ActionBarMenuItem otherItem;
    private ActionBarMenuItem searchItem;
    private ActionBarMenuSubItem editColorItem;
    private ActionBarMenuSubItem linkItem;
    private ActionBarMenuSubItem setUsernameItem;
    private ImageView ttlIconView;
    public ActionBarMenuItem qrItem;
    private ActionBarMenuSubItem autoDeleteItem;
    AutoDeletePopupWrapper autoDeletePopupWrapper;
    @Getter protected float headerShadowAlpha = 1.0f;
    private int actionBarBackgroundColor;
    private TopView topView;


    private long mergeDialogId;
    private boolean hasVoiceChatItem;

    private boolean openedGifts;


    private boolean scrolling;

    private boolean canSearchMembers;

    private boolean openAnimationInProgress;
    private boolean transitionAnimationInProress;
    private boolean recreateMenuAfterAnimation;
    @Getter private int playProfileAnimation;
    private boolean allowProfileAnimation = true;
    @Getter private float extraHeight;
    private float initialAnimationExtraHeight;
    @Getter private float avatarAnimationProgress;

    @Getter private int searchTransitionOffset;
    private float searchTransitionProgress;
    private Animator searchViewTransition;
    private boolean searchMode;

    private FlagSecureReason flagSecure;

    private HashMap<Integer, Integer> positionToOffset = new HashMap<>();

    private float avatarX;
    private float avatarY;
    private float avatarScale;
    private float nameX;
    private float nameY;
    private float onlineX;
    private float onlineY;
    private float expandProgress;
    private float listViewVelocityY;
    private ValueAnimator expandAnimator;
    private float currentExpandAnimatorValue;
    private float currentExpanAnimatorFracture;
    private float[] expandAnimatorValues = new float[]{0f, 1f};
    private boolean isPulledDown;

    private Paint whitePaint = new Paint();

    private BotLocation botLocation;
    private BotBiometry botBiometry;

    public ProfileChannelCell.ChannelMessageFetcher profileChannelMessageFetcher;
    public boolean createdBirthdayFetcher;
    public ProfileBirthdayEffect.BirthdayEffectFetcher birthdayFetcher;

    private CharSequence currentBio;

    private long selectedUser;

    private TLRPC.ChannelParticipant currentChannelParticipant;
    private TL_account.TL_password currentPassword;


    private Rect rect = new Rect();

    private TextCell setAvatarCell;

    private int rowCount;

    private int setAvatarRow;
    private int setAvatarSectionRow;
    private int channelRow;
    private int channelDividerRow;
    private int numberSectionRow;
    private int numberRow;
    public int birthdayRow;
    private int setUsernameRow;
    private int bioRow;
    private int phoneSuggestionSectionRow;
    private int graceSuggestionRow;
    private int graceSuggestionSectionRow;
    private int phoneSuggestionRow;
    private int passwordSuggestionSectionRow;
    private int passwordSuggestionRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int notificationRow;
    private int languageRow;
    private int privacyRow;
    private int dataRow;
    private int chatRow;
    private int filtersRow;
    private int liteModeRow;
    private int stickersRow;
    private int devicesRow;
    private int devicesSectionRow;
    private int helpHeaderRow;
    private int questionRow;
    private int faqRow;
    private int policyRow;
    private int helpSectionCell;
    private int debugHeaderRow;
    private int sendLogsRow;
    private int sendLastLogsRow;
    private int clearLogsRow;
    private int switchBackendRow;
    private int versionRow;
    private int emptyRow;
    private int bottomPaddingRow;
    private int infoHeaderRow;
    private int phoneRow;
    private int locationRow;
    private int userInfoRow;
    private int channelInfoRow;
    private int usernameRow;
    private int notificationsDividerRow;
    private int notificationsRow;
    private int bizHoursRow;
    private int bizLocationRow;
    private int notificationsSimpleRow;
    private int infoStartRow, infoEndRow;
    private int infoSectionRow;
    private int affiliateRow;
    private int infoAffiliateRow;
    private int sendMessageRow;
    private int reportRow;
    private int reportReactionRow;
    private int reportDividerRow;
    private int addToContactsRow;
    private int addToGroupButtonRow;
    private int addToGroupInfoRow;
    private int premiumRow;
    private int starsRow;
    private int tonRow;
    private int businessRow;
    private int premiumGiftingRow;
    private int premiumSectionsRow;
    private int botAppRow;
    private int botPermissionsHeader;
    @Keep
    private int botPermissionLocation;
    @Keep
    private int botPermissionEmojiStatus;
    private int botPermissionEmojiStatusReqId;
    @Keep
    private int botPermissionBiometry;
    private int botPermissionsDivider;

    private int settingsTimerRow;
    private int settingsKeyRow;
    private int secretSettingsSectionRow;

    private int membersHeaderRow;
    private int membersStartRow;
    private int membersEndRow;
    private int addMemberRow;
    private int subscribersRow;
    private int subscribersRequestsRow;
    private int administratorsRow;
    private int settingsRow;
    private int botStarsBalanceRow;
    private int botTonBalanceRow;
    private int channelBalanceRow;
    private int channelBalanceSectionRow;
    private int balanceDividerRow;
    private int blockedUsersRow;
    private int membersSectionRow;

    private int sharedMediaRow;

    private int unblockRow;
    private int joinRow;
    private int lastSectionRow;

    private boolean hoursExpanded;
    private boolean hoursShownMine;

    private int transitionIndex;
    private final ArrayList<TLRPC.ChatParticipant> visibleChatParticipants = new ArrayList<>();
    private final ArrayList<Integer> visibleSortedUsers = new ArrayList<>();
    private int usersForceShowingIn = 0;

    private boolean firstLayout = true;
    private boolean invalidateScroll = true;
    private boolean isQrItemVisible = true;

    PinchToZoomHelper pinchToZoomHelper;

    private View transitionOnlineText;
    private int actionBarAnimationColorFrom = 0;
    private int navigationBarAnimationColorFrom = 0;


    private boolean isFragmentPhoneNumber;

    private int openSimi;



    @Getter BaseFragment previousTransitionMainFragment;
    @Getter ChatActivityInterface previousTransitionFragment;

    HashSet<Integer> notificationsExceptionTopics = new HashSet<>();

    private CharacterStyle loadingSpan;

    private final Property<ProfileActivity, Float> HEADER_SHADOW = new AnimationProperties.FloatProperty<ProfileActivity>("headerShadow") {
        @Override
        public void setValue(ProfileActivity object, float value) {
            headerShadowAlpha = value;
            topView.invalidate();
        }

        @Override
        public Float get(ProfileActivity object) {
            return headerShadowAlpha;
        }
    };

    private PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview, boolean closing) {
            if (fileLocation == null) {
                return null;
            }

            TLRPC.FileLocation photoBig = null;
            if (viewModel.getArgs().userId != 0) {
                TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                if (user != null && user.photo != null && user.photo.photo_big != null) {
                    photoBig = user.photo.photo_big;
                }
            } else if (viewModel.getArgs().chatId != 0) {
                TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
                if (chat != null && chat.photo != null && chat.photo.photo_big != null) {
                    photoBig = chat.photo.photo_big;
                }
            }

            if (photoBig != null && photoBig.local_id == fileLocation.local_id && photoBig.volume_id == fileLocation.volume_id && photoBig.dc_id == fileLocation.dc_id) {
                int[] coords = new int[2];
                avatarImage.getLocationInWindow(coords);
                PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                object.parentView = avatarImage;
                object.imageReceiver = avatarImage.getImageReceiver();
                if (viewModel.getArgs().userId != 0) {
                    object.dialogId = viewModel.getArgs().userId;
                } else if (viewModel.getArgs().chatId != 0) {
                    object.dialogId = -viewModel.getArgs().chatId;
                }
                object.thumb = object.imageReceiver.getBitmapSafe();
                object.size = -1;
                object.radius = avatarImage.getImageReceiver().getRoundRadius(true);
                object.scale = avatarContainer.getScaleX();
                object.canEdit = viewModel.getArgs().userId == getUserConfig().clientUserId;
                return object;
            }
            return null;
        }

        @Override
        public void willHidePhotoViewer() {
            avatarImage.getImageReceiver().setVisible(true, true);
        }

        @Override
        public void openPhotoForEdit(String file, String thumb, boolean isVideo) {
            imageUpdater.openPhotoForEdit(file, thumb, 0, isVideo);
        }
    };
    private boolean fragmentOpened;
    private float titleAnimationsYDiff;
    private float customAvatarProgress;
    private float customPhotoOffset;
    private boolean hasCustomPhoto;
    private ImageReceiver fallbackImage;
    private boolean loadingBoostsStats;
    private FrameLayout bottomButtonsContainer;
    private FrameLayout[] bottomButtonContainer;
    private SpannableStringBuilder bottomButtonPostText;
    private ButtonWithCounterView[] bottomButton;
    private Runnable applyBulletin;

    public static ProfileActivity of(long dialogId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }
        return new ProfileActivity(bundle);
    }

    public long getTopicId() {
        return viewModel.getArgs().topicId;
    }

    public TLRPC.UserFull getUserInfo() {
        return viewModel.getState().getValue().getUserInfo();
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    public class NestedFrameLayout extends SizeNotifierFrameLayout implements NestedScrollingParent3 {

        private NestedScrollingParentHelper nestedScrollingParentHelper;

        public NestedFrameLayout(Context context) {
            super(context);
            nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, int[] consumed) {
            try {
                if (target == listView && sharedMediaLayoutAttached) {
                    RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                    int top = sharedMediaLayout.getTop();
                    if (top == 0) {
                        consumed[1] = dyUnconsumed;
                        innerListView.scrollBy(0, dyUnconsumed);
                    }
                }
                if (dyConsumed != 0 && type == TYPE_TOUCH) {
                    hideFloatingButton(!(sharedMediaLayout == null || sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_STORIES || sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_ARCHIVED_STORIES) || dyConsumed > 0);
                }
            } catch (Throwable e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        if (innerListView != null && innerListView.getAdapter() != null) {
                            innerListView.getAdapter().notifyDataSetChanged();
                        }
                    } catch (Throwable e2) {

                    }
                });
            }
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed, int type) {
            if (target == listView && sharedMediaRow != -1 && sharedMediaLayoutAttached) {
                boolean searchVisible = actionBar.isSearchFieldVisible();
                int t = sharedMediaLayout.getTop();
                if (dy < 0) {
                    boolean scrolledInner = false;
                    if (t <= 0) {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        if (innerListView != null) {
                            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) innerListView.getLayoutManager();
                            int pos = linearLayoutManager.findFirstVisibleItemPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                RecyclerView.ViewHolder holder = innerListView.findViewHolderForAdapterPosition(pos);
                                int top = holder != null ? holder.itemView.getTop() : -1;
                                int paddingTop = innerListView.getPaddingTop();
                                if (top != paddingTop || pos != 0) {
                                    consumed[1] = pos != 0 ? dy : Math.max(dy, (top - paddingTop));
                                    innerListView.scrollBy(0, dy);
                                    scrolledInner = true;
                                }
                            }
                        }
                    }
                    if (searchVisible) {
                        if (!scrolledInner && t < 0) {
                            consumed[1] = dy - Math.max(t, dy);
                        } else {
                            consumed[1] = dy;
                        }
                    }
                } else {
                    if (searchVisible) {
                        RecyclerListView innerListView = sharedMediaLayout.getCurrentListView();
                        consumed[1] = dy;
                        if (t > 0) {
                            consumed[1] -= dy;
                        }
                        if (innerListView != null && consumed[1] > 0) {
                            innerListView.scrollBy(0, consumed[1]);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes, int type) {
            return sharedMediaRow != -1 && axes == ViewCompat.SCROLL_AXIS_VERTICAL;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes, int type) {
            nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(View target, int type) {
            nestedScrollingParentHelper.onStopNestedScroll(target);
        }

        @Override
        public void onStopNestedScroll(View child) {

        }

        @Override
        protected void drawList(Canvas blurCanvas, boolean top, ArrayList<IViewWithInvalidateCallback> views) {
            super.drawList(blurCanvas, top, views);
            blurCanvas.save();
            blurCanvas.translate(0, listView.getY());
            sharedMediaLayout.drawListForBlur(blurCanvas, views);
            blurCanvas.restore();
        }
    }

    private class PagerIndicatorView extends View {

        private final RectF indicatorRect = new RectF();

        private final TextPaint textPaint;
        private final Paint backgroundPaint;

        private final ValueAnimator animator;
        private final float[] animatorValues = new float[]{0f, 1f};

        private final PagerAdapter adapter = avatarsViewPager.getAdapter();

        private boolean isIndicatorVisible;

        public PagerIndicatorView(Context context) {
            super(context);
            setVisibility(GONE);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTypeface(Typeface.SANS_SERIF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(AndroidUtilities.dpf2(15f));
            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(0x26000000);
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
            animator.addUpdateListener(a -> {
                final float value = AndroidUtilities.lerp(animatorValues, a.getAnimatedFraction());
                if (searchItem != null && !isPulledDown) {
                    searchItem.setScaleX(1f - value);
                    searchItem.setScaleY(1f - value);
                    searchItem.setAlpha(1f - value);
                }
                if (editItemVisible) {
                    editItem.setScaleX(1f - value);
                    editItem.setScaleY(1f - value);
                    editItem.setAlpha(1f - value);
                }
                if (callItemVisible) {
                    callItem.setScaleX(1f - value);
                    callItem.setScaleY(1f - value);
                    callItem.setAlpha(1f - value);
                }
                if (videoCallItemVisible) {
                    videoCallItem.setScaleX(1f - value);
                    videoCallItem.setScaleY(1f - value);
                    videoCallItem.setAlpha(1f - value);
                }
                setScaleX(value);
                setScaleY(value);
                setAlpha(value);
            });
            boolean expanded = viewModel.getArgs().expandPhoto;
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isIndicatorVisible) {
                        if (searchItem != null) {
                            searchItem.setClickable(false);
                        }
                        if (editItemVisible) {
                            editItem.setVisibility(GONE);
                        }
                        if (callItemVisible) {
                            callItem.setVisibility(GONE);
                        }
                        if (videoCallItemVisible) {
                            videoCallItem.setVisibility(GONE);
                        }
                    } else {
                        setVisibility(GONE);
                    }
                    updateStoriesViewBounds(false);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    if (searchItem != null && !expanded) {
                        searchItem.setClickable(true);
                    }
                    if (editItemVisible) {
                        editItem.setVisibility(VISIBLE);
                    }
                    if (callItemVisible) {
                        callItem.setVisibility(VISIBLE);
                    }
                    if (videoCallItemVisible) {
                        videoCallItem.setVisibility(VISIBLE);
                    }
                    setVisibility(VISIBLE);
                    updateStoriesViewBounds(false);
                }
            });
            avatarsViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                private int prevPage;

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    int realPosition = avatarsViewPager.getRealPosition(position);
                    invalidateIndicatorRect(prevPage != realPosition);
                    prevPage = realPosition;
                    updateAvatarItems();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    int count = avatarsViewPager.getRealCount();
                    if (overlayCountVisible == 0 && count > 1 && count <= 20 && overlaysView.isOverlaysVisible()) {
                        overlayCountVisible = 1;
                    }
                    invalidateIndicatorRect(false);
                    refreshVisibility(1f);
                    updateAvatarItems();
                }
            });
        }

        private void updateAvatarItemsInternal() {
            if (otherItem == null || avatarsViewPager == null) {
                return;
            }
            if (isPulledDown) {
                int position = avatarsViewPager.getRealPosition();
                if (position == 0) {
                    otherItem.hideSubItem(ActionBarActions.set_as_main);
                    otherItem.showSubItem(ActionBarActions.add_photo);
                } else {
                    otherItem.showSubItem(ActionBarActions.set_as_main);
                    otherItem.hideSubItem(ActionBarActions.add_photo);
                }
            }
        }

        private void updateAvatarItems() {
            if (imageUpdater == null) {
                return;
            }
            if (otherItem.isSubMenuShowing()) {
                AndroidUtilities.runOnUIThread(this::updateAvatarItemsInternal, 500);
            } else {
                updateAvatarItemsInternal();
            }
        }

        public boolean isIndicatorVisible() {
            return isIndicatorVisible;
        }

        public boolean isIndicatorFullyVisible() {
            return isIndicatorVisible && !animator.isRunning();
        }

        public void setIndicatorVisible(boolean indicatorVisible, float durationFactor) {
            if (indicatorVisible != isIndicatorVisible) {
                isIndicatorVisible = indicatorVisible;
                animator.cancel();
                final float value = AndroidUtilities.lerp(animatorValues, animator.getAnimatedFraction());
                if (durationFactor <= 0f) {
                    animator.setDuration(0);
                } else if (indicatorVisible) {
                    animator.setDuration((long) ((1f - value) * 250f / durationFactor));
                } else {
                    animator.setDuration((long) (value * 250f / durationFactor));
                }
                animatorValues[0] = value;
                animatorValues[1] = indicatorVisible ? 1f : 0f;
                animator.start();
            }
        }

        public void refreshVisibility(float durationFactor) {
            setIndicatorVisible(isPulledDown && avatarsViewPager.getRealCount() > 20, durationFactor);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            invalidateIndicatorRect(false);
        }

        private void invalidateIndicatorRect(boolean pageChanged) {
            if (pageChanged) {
                overlaysView.saveCurrentPageProgress();
            }
            overlaysView.invalidate();
            final float textWidth = textPaint.measureText(getCurrentTitle());
            indicatorRect.right = getMeasuredWidth() - AndroidUtilities.dp(54f) - (qrItem != null ? AndroidUtilities.dp(48) : 0);
            indicatorRect.left = indicatorRect.right - (textWidth + AndroidUtilities.dpf2(16f));
            indicatorRect.top = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(15f);
            indicatorRect.bottom = indicatorRect.top + AndroidUtilities.dp(26);
            setPivotX(indicatorRect.centerX());
            setPivotY(indicatorRect.centerY());
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float radius = AndroidUtilities.dpf2(12);
            canvas.drawRoundRect(indicatorRect, radius, radius, backgroundPaint);
            canvas.drawText(getCurrentTitle(), indicatorRect.centerX(), indicatorRect.top + AndroidUtilities.dpf2(18.5f), textPaint);
        }

        private String getCurrentTitle() {
            return adapter.getPageTitle(avatarsViewPager.getCurrentItem()).toString();
        }

        private ActionBarMenuItem getSecondaryMenuItem() {
            if (callItemVisible) {
                return callItem;
            } else if (editItemVisible) {
                return editItem;
            } else if (searchItem != null) {
                return searchItem;
            } else {
                return null;
            }
        }
    }

    public ProfileActivity(Bundle args) {
        this(args, null);
    }

    public ProfileActivity(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        super(args);
        sharedMediaPreloader = preloader;
    }

    @Override
    public boolean onFragmentCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        viewModel = new ProfileViewModel(arguments, classGuid, resourcesProvider);
        boolean loaded = viewModel.loadArgs();

        if (!loaded) {
            return false;
        }

        viewModel.getState().observe(this, this::onStateChanged);
        viewModel.getProfileUiEventLiveData().observe(this, this::onUiEvent);

        if (flagSecure != null) {
            flagSecure.invalidate();
        }

        if (viewModel.getState().getValue().isUserSelf()) {
            imageUpdater = new ImageUpdater(true, ImageUpdater.FOR_TYPE_USER, true);
            imageUpdater.setOpenWithFrontfaceCamera(true);
            imageUpdater.parentFragment = this;
            imageUpdater.setDelegate(this);
            getMediaDataController().checkFeaturedStickers();
            getMessagesController().loadSuggestedFilters();
            getMessagesController().loadUserInfo(getUserConfig().getCurrentUser(), true, classGuid);
        }

        if (viewModel.getArgs().expandPhoto) {
            currentExpandAnimatorValue = 1f;
        }

        if (sharedMediaPreloader == null) {
            sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);
        }
        sharedMediaPreloader.addDelegate(this);

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

        updateExceptions();
        updateRowsIds();

        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public int getTopOffset(int tag) {
                return AndroidUtilities.statusBarHeight;
            }

            @Override
            public int getBottomOffset(int tag) {
                if (bottomButtonsContainer == null) {
                    return 0;
                }
                final float gifts = Utilities.clamp01(1f - Math.abs(sharedMediaLayout.getTabProgress() - SharedMediaLayout.TAB_GIFTS));
                final float stories = Utilities.clamp01(1f - Math.abs(sharedMediaLayout.getTabProgress() - SharedMediaLayout.TAB_STORIES));
                final float archivedStories = Utilities.clamp01(1f - Math.abs(sharedMediaLayout.getTabProgress() - SharedMediaLayout.TAB_ARCHIVED_STORIES));
                return lerp((int) (dp(72) - bottomButtonsContainer.getTranslationY() - archivedStories * bottomButtonContainer[1].getTranslationY() - stories * bottomButtonContainer[0].getTranslationY()), 0, gifts);
            }

            @Override
            public boolean bottomOffsetAnimated() {
                return bottomButtonsContainer == null;
            }
        });

        return true;
    }

    private void updateExceptions() {
        if (!viewModel.getArgs().isTopic && ChatObject.isForum(viewModel.getState().getValue().getCurrentChat())) {
            getNotificationsController().loadTopicsNotificationsExceptions(-viewModel.getArgs().chatId, (topics) -> {
                ArrayList<Integer> arrayList = new ArrayList<>(topics);
                for (int i = 0; i < arrayList.size(); i++) {
                    if (getMessagesController().getTopicsController().findTopic(viewModel.getArgs().chatId, arrayList.get(i)) == null) {
                        arrayList.remove(i);
                        i--;
                    }
                }
                notificationsExceptionTopics.clear();
                notificationsExceptionTopics.addAll(arrayList);

                if (notificationsRow >= 0 && listAdapter != null) {
                    listAdapter.notifyItemChanged(notificationsRow);
                }
            });
        }
    }

    @Override
    public boolean isActionBarCrossfadeEnabled() {
        return !isPulledDown;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        if (sharedMediaLayout != null) {
            sharedMediaLayout.onDestroy();
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.onDestroy(this);
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.removeDelegate(this);
        }

        viewModel.onDestroy();

        if (avatarsViewPager != null) {
            avatarsViewPager.onDestroy();
        }
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
        if (imageUpdater != null) {
            imageUpdater.clear();
        }
        if (pinchToZoomHelper != null) {
            pinchToZoomHelper.clear();
        }
        if (birthdayFetcher != null && createdBirthdayFetcher) {
            birthdayFetcher.detach(true);
            birthdayFetcher = null;
        }

        if (applyBulletin != null) {
            Runnable runnable = applyBulletin;
            applyBulletin = null;
            AndroidUtilities.runOnUIThread(runnable);
        }
    }

    @Override
    public ActionBar createActionBar(Context context) {
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        ActionBar actionBar = new ActionBar(context, resourcesProvider) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                avatarContainer.getHitRect(rect);
                if (rect.contains((int) event.getX(), (int) event.getY())) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            @Override
            public void setItemsColor(int color, boolean isActionMode) {
                super.setItemsColor(color, isActionMode);
                if (!isActionMode && ttlIconView != null) {
                    ttlIconView.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateStoriesViewBounds(false);
            }
        };
        actionBar.setForceSkipTouches(true);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setItemsBackgroundColor(peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), true);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setClipContent(true);
        actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21 && !AndroidUtilities.isTablet() && !inBubbleMode);
        ImageView backButton = actionBar.getBackButton();
        backButton.setOnLongClickListener(e -> {
            ActionBarPopupWindow menu = BackButtonMenu.show(this, backButton, viewModel.getDialogId(), getTopicId(), resourcesProvider);
            if (menu != null) {
                menu.setOnDismissListener(() -> dimBehindView(false));
                dimBehindView(backButton, 0.3f);
                if (undoView != null) {
                    undoView.hide(true, 1);
                }
                return true;
            } else {
                return false;
            }
        });
        return actionBar;
    }

    @Override
    public void setParentLayout(INavigationLayout layout) {
        super.setParentLayout(layout);

        if (flagSecure != null) {
            flagSecure.detach();
            flagSecure = null;
        }
        if (layout != null && layout.getParentActivity() != null) {
            flagSecure = new FlagSecureReason(layout.getParentActivity().getWindow(), () -> viewModel.getState().getValue().getCurrentEncryptedChat() != null || getMessagesController().isChatNoForwards(viewModel.getState().getValue().getCurrentChat()));
        }
    }

    @Override
    public View createView(Context context) {
        Theme.createProfileResources(context);
        Theme.createChatResources(context, false);
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        searchTransitionOffset = 0;
        searchTransitionProgress = 1f;
        searchMode = false;
        hasOwnBackground = true;
        extraHeight = AndroidUtilities.dp(88f);

        ProfileActionBar profileActionBar = new ProfileActionBar(this, context, viewModel, actionBar, resourcesProvider);

        if (sharedMediaLayout != null) {
            sharedMediaLayout.onDestroy();
        }
        final long did;
        if (viewModel.getArgs().dialogId != 0) {
            did = viewModel.getArgs().dialogId;
        } else if (viewModel.getArgs().userId != 0) {
            did = viewModel.getArgs().userId;
        } else {
            did = -viewModel.getArgs().chatId;
        }

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;

        profileHeaderView = new ProfileHeaderView(context);
        frameLayout.addView(profileHeaderView);

        if (viewModel.getArgs().myProfile) {
            bottomButtonsContainer = new FrameLayout(context);

            bottomButtonContainer = new FrameLayout[2];
            bottomButton = new ButtonWithCounterView[2];
            for (int a = 0; a < 2; ++a) {
                bottomButtonContainer[a] = new FrameLayout(context);
                bottomButtonContainer[a].setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));

                View shadow = new View(context);
                shadow.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
                bottomButtonContainer[a].addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1 / AndroidUtilities.density, Gravity.TOP | Gravity.FILL_HORIZONTAL));

                bottomButton[a] = new ButtonWithCounterView(context, resourcesProvider);
                if (a == 0) {
                    bottomButtonPostText = new SpannableStringBuilder("c");
                    bottomButtonPostText.setSpan(new ColoredImageSpan(R.drawable.filled_premium_camera), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bottomButtonPostText.append("  ").append(getString(R.string.StoriesAddPost));
                    bottomButton[a].setText(bottomButtonPostText, false);
                } else {
                    bottomButton[a].setText(getString(R.string.StorySave), false);
                }
                final int finalA = a;
                bottomButton[a].setOnClickListener(v -> {
                    if (finalA == 0 && !sharedMediaLayout.isActionModeShown()) {
                        if (!getMessagesController().storiesEnabled()) {
                            showDialog(new PremiumFeatureBottomSheet(this, PremiumPreviewFragment.PREMIUM_FEATURE_STORIES, true));
                            return;
                        }
                        getMessagesController().getMainSettings().edit().putBoolean("story_keep", true).apply();
                        StoryRecorder.getInstance(getParentActivity(), getCurrentAccount())
                                .closeToWhenSent(new StoryRecorder.ClosingViewProvider() {
                                    @Override
                                    public void preLayout(long dialogId, Runnable runnable) {
                                        avatarImage.setHasStories(needInsetForStories());
                                        if (dialogId == viewModel.getDialogId()) {
                                            collapseAvatarInstant();
                                        }
                                        AndroidUtilities.runOnUIThread(runnable, 30);
                                    }

                                    @Override
                                    public StoryRecorder.SourceView getView(long dialogId) {
                                        if (dialogId != viewModel.getDialogId()) {
                                            return null;
                                        }
                                        updateAvatarRoundRadius();
                                        return StoryRecorder.SourceView.fromAvatarImage(avatarImage, ChatObject.isForum(viewModel.getState().getValue().getCurrentChat()));
                                    }
                                })
                                .open(null);
                    } else {
                        final long dialogId = getUserConfig().getClientUserId();
                        if (applyBulletin != null) {
                            applyBulletin.run();
                            applyBulletin = null;
                        }
                        Bulletin.hideVisible();
                        boolean pin = sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_ARCHIVED_STORIES;
                        int count = 0;
                        ArrayList<TL_stories.StoryItem> storyItems = new ArrayList<>();
                        SparseArray<MessageObject> actionModeMessageObjects = sharedMediaLayout.getActionModeSelected();
                        if (actionModeMessageObjects != null) {
                            for (int i = 0; i < actionModeMessageObjects.size(); ++i) {
                                MessageObject messageObject = actionModeMessageObjects.valueAt(i);
                                if (messageObject.storyItem != null) {
                                    storyItems.add(messageObject.storyItem);
                                    count++;
                                }
                            }
                        }
                        sharedMediaLayout.closeActionMode(false);
                        if (pin) {
                            sharedMediaLayout.scrollToPage(SharedMediaLayout.TAB_STORIES);
                        }
                        if (storyItems.isEmpty()) {
                            return;
                        }
                        boolean[] pastValues = new boolean[storyItems.size()];
                        for (int i = 0; i < storyItems.size(); ++i) {
                            TL_stories.StoryItem storyItem = storyItems.get(i);
                            pastValues[i] = storyItem.pinned;
                            storyItem.pinned = pin;
                        }
                        getMessagesController().getStoriesController().updateStoriesInLists(dialogId, storyItems);
                        final boolean[] undone = new boolean[] { false };
                        applyBulletin = () -> {
                            getMessagesController().getStoriesController().updateStoriesPinned(dialogId, storyItems, pin, null);
                        };
                        final Runnable undo = () -> {
                            undone[0] = true;
                            AndroidUtilities.cancelRunOnUIThread(applyBulletin);
                            for (int i = 0; i < storyItems.size(); ++i) {
                                TL_stories.StoryItem storyItem = storyItems.get(i);
                                storyItem.pinned = pastValues[i];
                            }
                            getMessagesController().getStoriesController().updateStoriesInLists(dialogId, storyItems);
                        };
                        Bulletin bulletin;
                        if (pin) {
                            bulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, LocaleController.formatPluralString("StorySavedTitle", count), LocaleController.getString(R.string.StorySavedSubtitle), LocaleController.getString(R.string.Undo), undo).show();
                        } else {
                            bulletin = BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_archived, LocaleController.formatPluralString("StoryArchived", count), LocaleController.getString(R.string.Undo), Bulletin.DURATION_PROLONG, undo).show();
                        }
                        bulletin.setOnHideListener(() -> {
                            if (!undone[0] && applyBulletin != null) {
                                applyBulletin.run();
                            }
                            applyBulletin = null;
                        });
                    }
                });
                bottomButtonContainer[a].addView(bottomButton[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 12, 12, 12, 12));

                bottomButtonsContainer.addView(bottomButtonContainer[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
                if (a == 1 || !getMessagesController().storiesEnabled()) {
                    bottomButtonContainer[a].setTranslationY(dp(72));
                }
            }
        }

        final ArrayList<Integer> users = viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null && viewModel.getState().getValue().getChatInfo().participants.participants.size() > 5 ? viewModel.getState().getValue().getSortedUsers() : null;
        int initialTab = -1;
        if (viewModel.getArgs().openCommonChats) {
            initialTab = SharedMediaLayout.TAB_COMMON_GROUPS;
        } else if (viewModel.getArgs().openGifts && (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().stargifts_count > 0 || viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stargifts_count > 0)) {
            initialTab = SharedMediaLayout.TAB_GIFTS;
            openedGifts = true;
        } else if (viewModel.getArgs().openSimilar) {
            initialTab = SharedMediaLayout.TAB_RECOMMENDED_CHANNELS;
        } else if (users != null) {
            initialTab = SharedMediaLayout.TAB_GROUPUSERS;
        }
        sharedMediaLayout = new SharedMediaLayout(context, did, sharedMediaPreloader, viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().common_chats_count : 0, viewModel.getState().getValue().getSortedUsers(), viewModel.getState().getValue().getChatInfo(), viewModel.getState().getValue().getUserInfo(), initialTab, this, this, SharedMediaLayout.VIEW_TYPE_PROFILE_ACTIVITY, resourcesProvider) {
            @Override
            protected int processColor(int color) {
                return dontApplyPeerColor(color, false);
            }
            @Override
            protected void onSelectedTabChanged() {
                updateSelectedMediaTabText();
            }
            @Override
            protected boolean includeSavedDialogs() {
                return viewModel.getArgs().dialogId == getUserConfig().getClientUserId() && !viewModel.getArgs().saved;
            }
            @Override
            protected boolean isSelf() {
                return viewModel.getArgs().myProfile;
            }

            @Override
            protected boolean isStoriesView() {
                return viewModel.getArgs().myProfile;
            }

            @Override
            protected void onSearchStateChanged(boolean expanded) {
                AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);

                listView.stopScroll();
                avatarContainer2.setPivotY(avatarContainer.getPivotY() + avatarContainer.getMeasuredHeight() / 2f);
                avatarContainer2.setPivotX(avatarContainer2.getMeasuredWidth() / 2f);
                AndroidUtilities.updateViewVisibilityAnimated(avatarContainer2, !expanded, 0.95f, true);

                callItem.setVisibility(expanded || !callItemVisible ? GONE : INVISIBLE);
                videoCallItem.setVisibility(expanded || !videoCallItemVisible ? GONE : INVISIBLE);
                editItem.setVisibility(expanded || !editItemVisible ? GONE : INVISIBLE);
                otherItem.setVisibility(expanded ? GONE : INVISIBLE);
                if (qrItem != null) {
                    qrItem.setVisibility(expanded ? GONE : INVISIBLE);
                }
                updateStoriesViewBounds(false);
            }

            @Override
            protected boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, View view) {
                return ProfileActivity.this.onMemberClick(participant, isLong, view);
            }

            @Override
            protected int getInitialTab() {
                return TAB_STORIES;
            }

            @Override
            protected void showActionMode(boolean show) {
                super.showActionMode(show);
                if (viewModel.getArgs().myProfile) {
                    disableScroll(show);

                    int a = getSelectedTab() - SharedMediaLayout.TAB_STORIES;
                    if (a < 0 || a > 1) return;
                    bottomButtonContainer[a]
                        .animate()
                        .translationY(show || a == 0 && MessagesController.getInstance(currentAccount).storiesEnabled() ? 0 : dp(72))
                        .setDuration(320)
                        .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                        .setUpdateListener(anm -> updateBottomButtonY())
                        .start();
                }
            }
            @Override
            protected void onTabProgress(float progress) {
                super.onTabProgress(progress);
                if (viewModel.getArgs().myProfile) {
                    int width = sharedMediaLayout == null ? AndroidUtilities.displaySize.x : sharedMediaLayout.getMeasuredWidth();
                    if (bottomButtonContainer[0] != null)
                        bottomButtonContainer[0].setTranslationX((SharedMediaLayout.TAB_STORIES - progress) * width);
                    if (bottomButtonContainer[1] != null)
                        bottomButtonContainer[1].setTranslationX((SharedMediaLayout.TAB_ARCHIVED_STORIES - progress) * width);
                    updateBottomButtonY();
                }
            }

            @Override
            protected void onActionModeSelectedUpdate(SparseArray<MessageObject> messageObjects) {
                super.onActionModeSelectedUpdate(messageObjects);
                if (viewModel.getArgs().myProfile) {
                    final int count = messageObjects.size();
                    int a = getSelectedTab() - SharedMediaLayout.TAB_STORIES;
                    if (a < 0 || a > 1) return;
                    if (a == 0) {
                        bottomButton[a].setText(count > 0 || !MessagesController.getInstance(currentAccount).storiesEnabled() ? formatPluralString("ArchiveStories", count) : bottomButtonPostText, true);
                    }
                    bottomButton[a].setCount(count, true);
                }
            }

            @Override
            public void openStoryRecorder() {
                StoryRecorder.getInstance(getParentActivity(), currentAccount)
                    .selectedPeerId(viewModel.getDialogId())
                    .canChangePeer(false)
                    .closeToWhenSent(new StoryRecorder.ClosingViewProvider() {
                        @Override
                        public void preLayout(long dialogId, Runnable runnable) {
                            avatarImage.setHasStories(needInsetForStories());
                            if (dialogId == viewModel.getDialogId()) {
                                collapseAvatarInstant();
                            }
                            AndroidUtilities.runOnUIThread(runnable, 30);
                        }

                        @Override
                        public StoryRecorder.SourceView getView(long dialogId) {
                            if (dialogId != viewModel.getDialogId()) {
                                return null;
                            }
                            updateAvatarRoundRadius();
                            return StoryRecorder.SourceView.fromAvatarImage(avatarImage, ChatObject.isForum(viewModel.getState().getValue().getCurrentChat()));
                        }
                    })
                    .open(StoryRecorder.SourceView.fromFloatingButton(floatingButtonContainer), true);
            }

            @Override
            public void updateTabs(boolean animated) {
                super.updateTabs(animated);
                if (viewModel.getArgs().openGifts && !openedGifts && scrollSlidingTextTabStrip.hasTab(TAB_GIFTS)) {
                    openedGifts = true;
                    scrollToPage(TAB_GIFTS);
                }
            }
        };
        sharedMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
        ActionBarMenu menu = actionBar.createMenu();

        if (viewModel.getArgs().userId == getUserConfig().clientUserId && !viewModel.getArgs().myProfile) {
            qrItem = menu.addItem(ActionBarActions.qr_button, R.drawable.msg_qr_mini, getResourceProvider());
            qrItem.setContentDescription(LocaleController.getString(R.string.GetQRCode));
            updateQrItemVisibility(false);
            if (ContactsController.getInstance(currentAccount).getPrivacyRules(PRIVACY_RULES_TYPE_ADDED_BY_PHONE) == null) {
                ContactsController.getInstance(currentAccount).loadPrivacySettings();
            }
        }
        if (imageUpdater != null && !viewModel.getArgs().myProfile) {
            searchItem = menu.addItem(ActionBarActions.search_button, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

                @Override
                public Animator getCustomToggleTransition() {
                    searchMode = !searchMode;
                    if (!searchMode) {
                        searchItem.clearFocusOnSearchView();
                    }
                    if (searchMode) {
                        searchItem.getSearchField().setText("");
                    }
                    return searchExpandTransition(searchMode);
                }

                @Override
                public void onTextChanged(EditText editText) {
                    searchAdapter.search(editText.getText().toString().toLowerCase());
                }
            });
            searchItem.setContentDescription(LocaleController.getString(R.string.SearchInSettings));
            searchItem.setSearchFieldHint(LocaleController.getString(R.string.SearchInSettings));
            sharedMediaLayout.getSearchItem().setVisibility(View.GONE);
            if (sharedMediaLayout.getSearchOptionsItem() != null) {
                sharedMediaLayout.getSearchOptionsItem().setVisibility(View.GONE);
            }
            if (sharedMediaLayout.getSaveItem() != null) {
                sharedMediaLayout.getSaveItem().setVisibility(View.GONE);
            }
            if (viewModel.getArgs().expandPhoto) {
                searchItem.setVisibility(View.GONE);
            }
        }

        videoCallItem = menu.addItem(ActionBarActions.video_call_item, R.drawable.profile_video);
        videoCallItem.setContentDescription(LocaleController.getString(R.string.VideoCall));
        if (viewModel.getArgs().chatId != 0) {
            callItem = menu.addItem(ActionBarActions.call_item, R.drawable.msg_voicechat2);
            if (ChatObject.isChannelOrGiga(viewModel.getState().getValue().getCurrentChat())) {
                callItem.setContentDescription(LocaleController.getString(R.string.VoipChannelVoiceChat));
            } else {
                callItem.setContentDescription(LocaleController.getString(R.string.VoipGroupVoiceChat));
            }
        } else {
            callItem = menu.addItem(ActionBarActions.call_item, R.drawable.ic_call);
            callItem.setContentDescription(LocaleController.getString(R.string.Call));
        }
        if (viewModel.getArgs().myProfile) {
            editItem = menu.addItem(ActionBarActions.edit_profile, R.drawable.group_edit_profile);
            editItem.setContentDescription(LocaleController.getString(R.string.Edit));
        } else {
            editItem = menu.addItem(ActionBarActions.edit_channel, R.drawable.group_edit_profile);
            editItem.setContentDescription(LocaleController.getString(R.string.Edit));
        }
        otherItem = menu.addItem(10, R.drawable.ic_ab_other, resourcesProvider);
        ttlIconView = new ImageView(context);
        ttlIconView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefaultIcon), PorterDuff.Mode.MULTIPLY));
        AndroidUtilities.updateViewVisibilityAnimated(ttlIconView, false, 0.8f, false);
        ttlIconView.setImageResource(R.drawable.msg_mini_autodelete_timer);
        otherItem.addView(ttlIconView, LayoutHelper.createFrame(12, 12, Gravity.CENTER_VERTICAL | Gravity.LEFT, 8, 2, 0, 0));
        otherItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));

        int scrollTo;
        int scrollToPosition = 0;
        Object writeButtonTag = null;
        if (listView != null && imageUpdater != null) {
            scrollTo = layoutManager.findFirstVisibleItemPosition();
            View topView = layoutManager.findViewByPosition(scrollTo);
            if (topView != null) {
                scrollToPosition = topView.getTop() - listView.getPaddingTop();
            } else {
                scrollTo = -1;
            }
            writeButtonTag = writeButton.getTag();
        } else {
            scrollTo = -1;
        }

        createActionBarMenu(false);

        listAdapter = new ListAdapter(context);
        searchAdapter = new SearchAdapter(context);
        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setProfile(true);

        fragmentView.setWillNotDraw(false);
        //FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new ClippedListView(context) {

            private VelocityTracker velocityTracker;

            @Override
            protected boolean canHighlightChildAt(View child, float x, float y) {
                return !(child instanceof AboutLinkCell);
            }

            @Override
            protected boolean allowSelectChildAtPosition(View child) {
                return child != sharedMediaLayout;
            }

            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }

            @Override
            protected void requestChildOnScreen(View child, View focused) {

            }

            @Override
            public void invalidate() {
                super.invalidate();
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent e) {
                if (sharedMediaLayout != null) {
                    if (sharedMediaLayout.canEditStories() && sharedMediaLayout.isActionModeShown() && sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_BOT_PREVIEWS) {
                        return false;
                    }
                    if (sharedMediaLayout.canEditStories() && sharedMediaLayout.isActionModeShown() && sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_STORIES) {
                        return false;
                    }
                    if (sharedMediaLayout.giftsContainer != null && sharedMediaLayout.giftsContainer.isReordering()) {
                        return false;
                    }
                }
                return super.onInterceptTouchEvent(e);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                final int action = e.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    } else {
                        velocityTracker.clear();
                    }
                    velocityTracker.addMovement(e);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(e);
                        velocityTracker.computeCurrentVelocity(1000);
                        listViewVelocityY = velocityTracker.getYVelocity(e.getPointerId(e.getActionIndex()));
                    }
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                }
                final boolean result = super.onTouchEvent(e);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (viewModel.getState().getValue().isAllowPullingDown()) {
                        final View view = layoutManager.findViewByPosition(0);
                        if (view != null) {
                            if (isPulledDown) {
                                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                                listView.smoothScrollBy(0, view.getTop() - listView.getMeasuredWidth() + actionBarHeight, CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else {
                                listView.smoothScrollBy(0, view.getTop() - AndroidUtilities.dp(88), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        }
                    }
                }
                return result;
            }

            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (getItemAnimator().isRunning() && child.getBackground() == null && child.getTranslationY() != 0) {
                    boolean useAlpha = listView.getChildAdapterPosition(child) == sharedMediaRow && child.getAlpha() != 1f;
                    if (useAlpha) {
                        whitePaint.setAlpha((int) (255 * listView.getAlpha() * child.getAlpha()));
                    }
                    canvas.drawRect(listView.getX(), child.getY(), listView.getX() + listView.getMeasuredWidth(), child.getY() + child.getHeight(), whitePaint);
                    if (useAlpha) {
                        whitePaint.setAlpha((int) (255 * listView.getAlpha()));
                    }
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (bizHoursRow >= 0 && infoStartRow >= 0 && infoEndRow >= 0) {
                    drawSectionBackground(canvas, infoStartRow, infoEndRow, getThemedColor(Theme.key_windowBackgroundWhite));
                }
                super.dispatchDraw(canvas);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                updateBottomButtonY();
            }
        };
        listView.setVerticalScrollBarEnabled(false);
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator() {

            int animationIndex = -1;

            @Override
            protected void onAllAnimationsDone() {
                super.onAllAnimationsDone();
                AndroidUtilities.runOnUIThread(() -> {
                    getNotificationCenter().onAnimationFinish(animationIndex);
                });
            }

            @Override
            public void runPendingAnimations() {
                boolean removalsPending = !mPendingRemovals.isEmpty();
                boolean movesPending = !mPendingMoves.isEmpty();
                boolean changesPending = !mPendingChanges.isEmpty();
                boolean additionsPending = !mPendingAdditions.isEmpty();
                if (removalsPending || movesPending || additionsPending || changesPending) {
                    ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
                    valueAnimator.addUpdateListener(valueAnimator1 -> listView.invalidate());
                    valueAnimator.setDuration(getMoveDuration());
                    valueAnimator.start();
                    animationIndex = getNotificationCenter().setAnimationInProgress(animationIndex, null);
                }
                super.runPendingAnimations();
            }

            @Override
            protected long getAddAnimationDelay(long removeDuration, long moveDuration, long changeDuration) {
                return 0;
            }

            @Override
            protected void onMoveAnimationUpdate(RecyclerView.ViewHolder holder) {
                super.onMoveAnimationUpdate(holder);
                updateBottomButtonY();
            }
        };
        listView.setItemAnimator(defaultItemAnimator);
        defaultItemAnimator.setMoveDelay(0);
        defaultItemAnimator.setMoveDuration(320);
        defaultItemAnimator.setRemoveDuration(320);
        defaultItemAnimator.setAddDuration(320);
        defaultItemAnimator.setSupportsChangeAnimations(false);
        defaultItemAnimator.setDelayAnimations(false);
        defaultItemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        listView.setClipToPadding(false);
        listView.setHideIfEmpty(false);

        layoutManager = new LinearLayoutManager(context) {

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return imageUpdater != null;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
                final View view = layoutManager.findViewByPosition(0);
                if (view != null && !openingAvatar) {
                    final int canScroll = view.getTop() - AndroidUtilities.dp(88);
                    if (!viewModel.getState().getValue().isAllowPullingDown() && canScroll > dy) {
                        dy = canScroll;
                        if (avatarsViewPager.hasImages() && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled() && !viewModel.getState().getValue().isInLandscapeMode() && !AndroidUtilities.isTablet()) {
                            viewModel.allowPullingDown(viewModel.getState().getValue().getAvatarBig() == null);
                        }
                    } else if (viewModel.getState().getValue().isAllowPullingDown()) {
                        if (dy >= canScroll) {
                            dy = canScroll;
                            viewModel.allowPullingDown(false);
                        } else if (listView.getScrollState() == RecyclerListView.SCROLL_STATE_DRAGGING) {
                            if (!isPulledDown) {
                                dy /= 2;
                            }
                        }
                    }
                }
                return super.scrollVerticallyBy(dy, recycler, state);
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.mIgnoreTopPadding = false;
        listView.setLayoutManager(layoutManager);
        listView.setGlowColor(0);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (getParentActivity() == null) {
                return;
            }
            listView.stopScroll();
            if (position == affiliateRow) {
                TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().starref_program != null) {
                    final long selfId = getUserConfig().getClientUserId();
                    BotStarsController.getInstance(currentAccount).getConnectedBot(getContext(), selfId, viewModel.getArgs().userId, connectedBot -> {
                        if (connectedBot == null) {
                            ChannelAffiliateProgramsFragment.showConnectAffiliateAlert(context, currentAccount, viewModel.getState().getValue().getUserInfo().starref_program, getUserConfig().getClientUserId(), resourcesProvider, false);
                        } else {
                            ChannelAffiliateProgramsFragment.showShareAffiliateAlert(context, currentAccount, connectedBot, selfId, resourcesProvider);
                        }
                    });
                } else if (user != null && user.bot_can_edit) {
                    presentFragment(new AffiliateProgramFragment(viewModel.getArgs().userId));
                }
            } else if (position == notificationsSimpleRow) {
                boolean muted = getMessagesController().isDialogMuted(did, viewModel.getArgs().topicId);
                getNotificationsController().muteDialog(did, viewModel.getArgs().topicId, !muted);
                BulletinFactory.createMuteBulletin(ProfileActivity.this, !muted, null).show();
                updateExceptions();
                if (notificationsSimpleRow >= 0 && listAdapter != null) {
                    listAdapter.notifyItemChanged(notificationsSimpleRow);
                }
            } else if (position == addToContactsRow) {
                TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                args.putBoolean("addContact", true);
                args.putString("phone", viewModel.getArgs().vcardPhone);
                args.putString("first_name_card", viewModel.getArgs().vcardFirstName);
                args.putString("last_name_card", viewModel.getArgs().vcardLastName);
                openAddToContact(user, args);
            } else if (position == reportReactionRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.ReportReaction));
                builder.setMessage(LocaleController.getString(R.string.ReportAlertReaction));

                TLRPC.Chat chat = getMessagesController().getChat(-viewModel.getArgs().reportReactionFromDialogId);
                CheckBoxCell[] cells = new CheckBoxCell[1];
                if (chat != null && ChatObject.canBlockUsers(chat)) {
                    LinearLayout linearLayout = new LinearLayout(getParentActivity());
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    cells[0] = new CheckBoxCell(getParentActivity(), 1, resourcesProvider);
                    cells[0].setBackgroundDrawable(Theme.getSelectorDrawable(false));
                    cells[0].setText(LocaleController.getString(R.string.BanUser), "", true, false);
                    cells[0].setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
                    linearLayout.addView(cells[0], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                    cells[0].setOnClickListener(v -> {
                        cells[0].setChecked(!cells[0].isChecked(), true);
                    });
                    builder.setView(linearLayout);
                }

                builder.setPositiveButton(LocaleController.getString(R.string.ReportChat), (dialog, which) -> {
                    TLRPC.TL_messages_reportReaction req = new TLRPC.TL_messages_reportReaction();
                    req.user_id = getMessagesController().getInputUser(viewModel.getArgs().userId);
                    req.peer = getMessagesController().getInputPeer(viewModel.getArgs().reportReactionFromDialogId);
                    req.id = viewModel.getArgs().reportReactionMessageId;
                    ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {

                    });

                    if (cells[0] != null && cells[0].isChecked()) {
                        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                        getMessagesController().deleteParticipantFromChat(-viewModel.getArgs().reportReactionFromDialogId, user);
                    }

                    // TODO fix
                    // viewModel.getArgs().reportReactionMessageId = 0;
                    updateListAnimated(false);
                    BulletinFactory.of(ProfileActivity.this).createReportSent(resourcesProvider).show();
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> {
                    dialog.dismiss();
                });
                AlertDialog dialog = builder.show();
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
            } else if (position == settingsKeyRow) {
                Bundle args = new Bundle();
                args.putInt("chat_id", DialogObject.getEncryptedChatId(viewModel.getArgs().dialogId));
                presentFragment(new IdenticonActivity(args));
            } else if (position == settingsTimerRow) {
                showDialog(AlertsCreator.createTTLAlert(getParentActivity(), viewModel.getState().getValue().getCurrentEncryptedChat(), resourcesProvider).create());
            } else if (position == notificationsRow) {
                if (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {
                    NotificationsCheckCell checkCell = (NotificationsCheckCell) view;
                    boolean checked = !checkCell.isChecked();

                    boolean defaultEnabled = getNotificationsController().isGlobalNotificationsEnabled(did, false, false);

                    String key = NotificationsController.getSharedPrefKey(did, viewModel.getArgs().topicId);
                    if (checked) {
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        SharedPreferences.Editor editor = preferences.edit();
                        if (defaultEnabled) {
                            editor.remove("notify2_" + key);
                        } else {
                            editor.putInt("notify2_" + key, 0);
                        }
                        if (viewModel.getArgs().topicId == 0) {
                            getMessagesStorage().setDialogFlags(did, 0);
                            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                            }
                        }
                        editor.apply();
                    } else {
                        int untilTime = Integer.MAX_VALUE;
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        SharedPreferences.Editor editor = preferences.edit();
                        long flags;
                        if (!defaultEnabled) {
                            editor.remove("notify2_" + key);
                            flags = 0;
                        } else {
                            editor.putInt("notify2_" + key, 2);
                            flags = 1;
                        }
                        getNotificationsController().removeNotificationsForDialog(did);
                        if (viewModel.getArgs().topicId == 0) {
                            getMessagesStorage().setDialogFlags(did, flags);
                            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(did);
                            if (dialog != null) {
                                dialog.notify_settings = new TLRPC.TL_peerNotifySettings();
                                if (defaultEnabled) {
                                    dialog.notify_settings.mute_until = untilTime;
                                }
                            }
                        }
                        editor.apply();
                    }
                    updateExceptions();
                    getNotificationsController().updateServerNotificationsSettings(did, viewModel.getArgs().topicId);
                    checkCell.setChecked(checked);
                    RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForPosition(notificationsRow);
                    if (holder != null) {
                        listAdapter.onBindViewHolder(holder, notificationsRow);
                    }
                    return;
                }
                ChatNotificationsPopupWrapper chatNotificationsPopupWrapper = new ChatNotificationsPopupWrapper(context, currentAccount, null, true, true, new ChatNotificationsPopupWrapper.Callback() {
                    @Override
                    public void toggleSound() {
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        boolean enabled = !preferences.getBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(did, viewModel.getArgs().topicId), true);
                        preferences.edit().putBoolean("sound_enabled_" + NotificationsController.getSharedPrefKey(did, viewModel.getArgs().topicId), enabled).apply();
                        if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                            BulletinFactory.createSoundEnabledBulletin(ProfileActivity.this, enabled ? NotificationsController.SETTING_SOUND_ON : NotificationsController.SETTING_SOUND_OFF, getResourceProvider()).show();
                        }
                    }

                    @Override
                    public void muteFor(int timeInSeconds) {
                        if (timeInSeconds == 0) {
                            if (getMessagesController().isDialogMuted(did, viewModel.getArgs().topicId)) {
                                toggleMute();
                            }
                            if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                                BulletinFactory.createMuteBulletin(ProfileActivity.this, NotificationsController.SETTING_MUTE_UNMUTE, timeInSeconds, getResourceProvider()).show();
                            }
                        } else {
                            getNotificationsController().muteUntil(did, viewModel.getArgs().topicId, timeInSeconds);
                            if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                                BulletinFactory.createMuteBulletin(ProfileActivity.this, NotificationsController.SETTING_MUTE_CUSTOM, timeInSeconds, getResourceProvider()).show();
                            }
                            updateExceptions();
                            if (notificationsRow >= 0 && listAdapter != null) {
                                listAdapter.notifyItemChanged(notificationsRow);
                            }
                        }
                    }

                    @Override
                    public void showCustomize() {
                        if (did != 0) {
                            Bundle args = new Bundle();
                            args.putLong("dialog_id", did);
                            args.putLong("topic_id", viewModel.getArgs().topicId);
                            presentFragment(new ProfileNotificationsActivity(args, resourcesProvider));
                        }
                    }

                    @Override
                    public void toggleMute() {
                        boolean muted = getMessagesController().isDialogMuted(did, viewModel.getArgs().topicId);
                        getNotificationsController().muteDialog(did, viewModel.getArgs().topicId, !muted);
                        if (ProfileActivity.this.fragmentView != null) {
                            BulletinFactory.createMuteBulletin(ProfileActivity.this, !muted, null).show();
                        }
                        updateExceptions();
                        if (notificationsRow >= 0 && listAdapter != null) {
                            listAdapter.notifyItemChanged(notificationsRow);
                        }
                    }

                    @Override
                    public void openExceptions() {
                        Bundle bundle = new Bundle();
                        bundle.putLong("dialog_id", did);
                        TopicsNotifySettingsFragments notifySettings = new TopicsNotifySettingsFragments(bundle);
                        notifySettings.setExceptions(notificationsExceptionTopics);
                        presentFragment(notifySettings);
                    }
                }, getResourceProvider());
                chatNotificationsPopupWrapper.update(did, viewModel.getArgs().topicId, notificationsExceptionTopics);
                if (AndroidUtilities.isTablet()) {
                    View v = parentLayout.getView();
                    x += v.getX() + v.getPaddingLeft();
                    y += v.getY() + v.getPaddingTop();
                }
                chatNotificationsPopupWrapper.showAsOptions(ProfileActivity.this, view, x, y);
            } else if (position == unblockRow) {
                getMessagesController().unblockPeer(viewModel.getArgs().userId);
                if (BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createBanBulletin(ProfileActivity.this, false).show();
                }
            } else if (position == addToGroupButtonRow) {
                try {
                    actionBar.getActionBarMenuOnItemClick().onItemClick(ActionBarActions.invite_to_group);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else if (position == sendMessageRow) {
                onWriteButtonClick();
            } else if (position == reportRow) {
                ReportBottomSheet.openChat(ProfileActivity.this, viewModel.getDialogId());
            } else if (position >= membersStartRow && position < membersEndRow) {
                TLRPC.ChatParticipant participant;
                if (!viewModel.getState().getValue().getSortedUsers().isEmpty()) {
                    participant = viewModel.getState().getValue().getChatInfo().participants.participants.get(viewModel.getState().getValue().getSortedUsers().get(position - membersStartRow));
                } else {
                    participant = viewModel.getState().getValue().getChatInfo().participants.participants.get(position - membersStartRow);
                }
                onMemberClick(participant, false, view);
            } else if (position == addMemberRow) {
                openAddMember();
            } else if (position == usernameRow) {
                processOnClickOrPress(position, view, x, y);
            } else if (position == locationRow) {
                if (viewModel.getState().getValue().getChatInfo().location instanceof TLRPC.TL_channelLocation) {
                    LocationActivity fragment = new LocationActivity(LocationActivity.LOCATION_TYPE_GROUP_VIEW);
                    fragment.setChatLocation(viewModel.getArgs().chatId, (TLRPC.TL_channelLocation) viewModel.getState().getValue().getChatInfo().location);
                    presentFragment(fragment);
                }
            } else if (position == joinRow) {
                getMessagesController().addUserToChat(viewModel.getState().getValue().getCurrentChat().id, getUserConfig().getCurrentUser(), 0, null, ProfileActivity.this, true, () -> {
                    updateRowsIds();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }, err -> {
                    if (err != null && "INVITE_REQUEST_SENT".equals(err.text)) {
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        preferences.edit().putLong("dialog_join_requested_time_" + viewModel.getArgs().dialogId, System.currentTimeMillis()).commit();
                        JoinGroupAlert.showBulletin(context, ProfileActivity.this, ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup());
                        updateRowsIds();
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                        if (lastFragment instanceof ChatActivity) {
                            ((ChatActivity) lastFragment).showBottomOverlayProgress(false, true);
                        }
                        return false;
                    }
                    return true;
                });
                viewModel.postNotificationName(NotificationCenter.closeSearchByActiveAction);
            } else if (position == subscribersRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", viewModel.getArgs().chatId);
                args.putInt("type", ChatUsersActivity.TYPE_USERS);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(viewModel.getState().getValue().getChatInfo());
                presentFragment(fragment);
            } else if (position == subscribersRequestsRow) {
                MemberRequestsActivity activity = new MemberRequestsActivity(viewModel.getArgs().chatId);
                presentFragment(activity);
            } else if (position == administratorsRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", viewModel.getArgs().chatId);
                args.putInt("type", ChatUsersActivity.TYPE_ADMIN);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(viewModel.getState().getValue().getChatInfo());
                presentFragment(fragment);
            } else if (position == settingsRow) {
                editItem.performClick();
            } else if (position == botStarsBalanceRow) {
                presentFragment(new BotStarsActivity(BotStarsActivity.TYPE_STARS, viewModel.getArgs().userId));
            } else if (position == botTonBalanceRow) {
                presentFragment(new BotStarsActivity(BotStarsActivity.TYPE_TON, viewModel.getArgs().userId));
            } else if (position == channelBalanceRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", viewModel.getArgs().chatId);
                args.putBoolean("start_from_monetization", true);
                presentFragment(new StatisticActivity(args));
            } else if (position == blockedUsersRow) {
                Bundle args = new Bundle();
                args.putLong("chat_id", viewModel.getArgs().chatId);
                args.putInt("type", ChatUsersActivity.TYPE_BANNED);
                ChatUsersActivity fragment = new ChatUsersActivity(args);
                fragment.setInfo(viewModel.getState().getValue().getChatInfo());
                presentFragment(fragment);
            } else if (position == notificationRow) {
                presentFragment(new NotificationsSettingsActivity());
            } else if (position == privacyRow) {
                presentFragment(new PrivacySettingsActivity().setCurrentPassword(currentPassword));
            } else if (position == dataRow) {
                presentFragment(new DataSettingsActivity());
            } else if (position == chatRow) {
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
            } else if (position == filtersRow) {
                presentFragment(new FiltersSetupActivity());
            } else if (position == stickersRow) {
                presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null));
            } else if (position == liteModeRow) {
                presentFragment(new LiteModeSettingsActivity());
            } else if (position == devicesRow) {
                presentFragment(new SessionsActivity(0));
            } else if (position == questionRow) {
                showDialog(AlertsCreator.createSupportAlert(ProfileActivity.this, resourcesProvider));
            } else if (position == faqRow) {
                Browser.openUrl(getParentActivity(), LocaleController.getString(R.string.TelegramFaqUrl));
            } else if (position == policyRow) {
                Browser.openUrl(getParentActivity(), LocaleController.getString(R.string.PrivacyPolicyUrl));
            } else if (position == sendLogsRow) {
                sendLogs(getParentActivity(), false);
            } else if (position == sendLastLogsRow) {
                sendLogs(getParentActivity(), true);
            } else if (position == clearLogsRow) {
                FileLog.cleanupLogs();
            } else if (position == switchBackendRow) {
                if (getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder1.setMessage(LocaleController.getString(R.string.AreYouSure));
                builder1.setTitle(LocaleController.getString(R.string.AppName));
                builder1.setPositiveButton(LocaleController.getString(R.string.OK), (dialogInterface, i) -> {
                    SharedConfig.pushAuthKey = null;
                    SharedConfig.pushAuthKeyId = null;
                    SharedConfig.saveConfig();
                    getConnectionsManager().switchBackend(true);
                });
                builder1.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder1.create());
            } else if (position == languageRow) {
                presentFragment(new LanguageSelectActivity());
            } else if (position == setUsernameRow) {
                presentFragment(new ChangeUsernameActivity());
            } else if (position == bioRow) {
                presentFragment(new UserInfoActivity());
            } else if (position == numberRow) {
                presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER));
            } else if (position == setAvatarRow) {
                onWriteButtonClick();
            } else if (position == premiumRow) {
                presentFragment(new PremiumPreviewFragment("settings"));
            } else if (position == starsRow) {
                presentFragment(new StarsIntroActivity());
            } else if (position == tonRow) {
                presentFragment(new TONIntroActivity());
            } else if (position == businessRow) {
                presentFragment(new PremiumPreviewFragment(PremiumPreviewFragment.FEATURES_BUSINESS, "settings"));
            } else if (position == premiumGiftingRow) {
                UserSelectorBottomSheet.open(0, BirthdayController.getInstance(currentAccount).getState());
            } else if (position == botPermissionLocation) {
                if (botLocation != null) {
                    botLocation.setGranted(!botLocation.granted(), () -> {
                        ((TextCell) view).setChecked(botLocation.granted());
                    });
                }
            } else if (position == botPermissionBiometry) {
                if (botBiometry != null) {
                    botBiometry.setGranted(!botBiometry.granted());
                    ((TextCell) view).setChecked(botBiometry.granted());
                }
            } else if (position == botPermissionEmojiStatus) {
                ((TextCell) view).setChecked(!((TextCell) view).isChecked());
                if (botPermissionEmojiStatusReqId > 0) {
                    getConnectionsManager().cancelRequest(botPermissionEmojiStatusReqId, true);
                }
                TL_bots.toggleUserEmojiStatusPermission req = new TL_bots.toggleUserEmojiStatusPermission();
                req.bot = getMessagesController().getInputUser(viewModel.getArgs().userId);
                req.enabled = ((TextCell) view).isChecked();
                if (viewModel.getState().getValue().getUserInfo() != null) {
                    viewModel.getState().getValue().getUserInfo().bot_can_manage_emoji_status = req.enabled;
                }
                final int[] reqId = new int[1];
                reqId[0] = botPermissionEmojiStatusReqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                    if (!(res instanceof TLRPC.TL_boolTrue)) {
                        BulletinFactory.of(ProfileActivity.this).showForError(err);
                    }
                    if (botPermissionEmojiStatusReqId == reqId[0]) {
                        botPermissionEmojiStatusReqId = 0;
                    }
                }));
            } else if (position == bizHoursRow) {
                hoursExpanded = !hoursExpanded;
                saveScrollPosition();
                view.requestLayout();
                listAdapter.notifyItemChanged(bizHoursRow);
                if (savedScrollPosition >= 0) {
                    layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset - listView.getPaddingTop());
                }
            } else if (position == bizLocationRow) {
                openLocation(false);
            } else if (position == channelRow) {
                if (viewModel.getState().getValue().getUserInfo() == null) return;
                Bundle args = new Bundle();
                args.putLong("chat_id", viewModel.getState().getValue().getUserInfo().personal_channel_id);
                presentFragment(new ChatActivity(args));
            } else if (position == birthdayRow) {
                if (birthdayEffect != null && birthdayEffect.start()) {
                    return;
                }
                if (editRow(view, position)) {
                    return;
                }
                TextDetailCell cell = (TextDetailCell) view;
                if (cell.hasImage()) {
                    onTextDetailCellImageClicked(cell.getImageView());
                }
            } else {
                processOnClickOrPress(position, view, x, y);
            }
        });

        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {

            private int pressCount = 0;

            @Override
            public boolean onItemClick(View view, int position) {
                if (position == versionRow) {
                    pressCount++;
                    if (pressCount >= 2 || BuildVars.DEBUG_PRIVATE_VERSION) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                        builder.setTitle(getString(R.string.DebugMenu));
                        CharSequence[] items;
                        items = new CharSequence[]{
                                getString(R.string.DebugMenuImportContacts),
                                getString(R.string.DebugMenuReloadContacts),
                                getString(R.string.DebugMenuResetContacts),
                                getString(R.string.DebugMenuResetDialogs),
                                BuildVars.DEBUG_VERSION ? null : (BuildVars.LOGS_ENABLED ? getString("DebugMenuDisableLogs", R.string.DebugMenuDisableLogs) : getString("DebugMenuEnableLogs", R.string.DebugMenuEnableLogs)),
                                SharedConfig.inappCamera ? getString("DebugMenuDisableCamera", R.string.DebugMenuDisableCamera) : getString("DebugMenuEnableCamera", R.string.DebugMenuEnableCamera),
                                getString("DebugMenuClearMediaCache", R.string.DebugMenuClearMediaCache),
                                getString(R.string.DebugMenuCallSettings),
                                null,
                                BuildVars.DEBUG_PRIVATE_VERSION || ApplicationLoader.isStandaloneBuild() || ApplicationLoader.isBetaBuild() ? getString("DebugMenuCheckAppUpdate", R.string.DebugMenuCheckAppUpdate) : null,
                                getString("DebugMenuReadAllDialogs", R.string.DebugMenuReadAllDialogs),
                                BuildVars.DEBUG_PRIVATE_VERSION ? (SharedConfig.disableVoiceAudioEffects ? "Enable voip audio effects" : "Disable voip audio effects") : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Clean app update" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Reset suggestions" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? getString(R.string.DebugMenuClearWebViewCache) : null,
                                getString(R.string.DebugMenuClearWebViewCookies),
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? getString(SharedConfig.debugWebView ? R.string.DebugMenuDisableWebViewDebug : R.string.DebugMenuEnableWebViewDebug) : null,
                                (AndroidUtilities.isTabletInternal() && BuildVars.DEBUG_PRIVATE_VERSION) ? (SharedConfig.forceDisableTabletMode ? "Enable tablet mode" : "Disable tablet mode") : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? getString(SharedConfig.isFloatingDebugActive ? R.string.FloatingDebugDisable : R.string.FloatingDebugEnable) : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Force remove premium suggestions" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Share device info" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Force performance class" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION && !InstantCameraView.allowBigSizeCameraDebug() ? (!SharedConfig.bigCameraForRound ? "Force big camera for round" : "Disable big camera for round") : null,
                                getString(DualCameraView.dualAvailableStatic(getContext()) ? "DebugMenuDualOff" : "DebugMenuDualOn"),
                                BuildVars.DEBUG_VERSION ? (SharedConfig.useSurfaceInStories ? "back to TextureView in stories" : "use SurfaceView in stories") : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? (SharedConfig.photoViewerBlur ? "do not blur in photoviewer" : "blur in photoviewer") : null,
                                !SharedConfig.payByInvoice ? "Enable Invoice Payment" : "Disable Invoice Payment",
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Update Attach Bots" : null,
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? (!SharedConfig.isUsingCamera2(currentAccount) ? "Use Camera 2 API" : "Use old Camera 1 API") : null,
                                BuildVars.DEBUG_VERSION ? "Clear Mini Apps Permissions and Files" : null,
                                BuildVars.DEBUG_PRIVATE_VERSION ? "Clear all login tokens" : null,
                                SharedConfig.canBlurChat() && Build.VERSION.SDK_INT >= 31 ? (SharedConfig.useNewBlur ? "back to cpu blur" : "use new gpu blur") : null,
                                SharedConfig.adaptableColorInBrowser ? "Disabled adaptive browser colors" : "Enable adaptive browser colors",
                                SharedConfig.debugVideoQualities ? "Disable video qualities debug" : "Enable video qualities debug",
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? getString(SharedConfig.useSystemBoldFont ? R.string.DebugMenuDontUseSystemBoldFont : R.string.DebugMenuUseSystemBoldFont) : null,
                                "Reload app config",
                                !SharedConfig.forceForumTabs ? "Force Forum Tabs" : "Do Not Force Forum Tabs"
                        };

                        builder.setItems(items, (dialog, which) -> {
                            if (which == 0) { // Import Contacts
                                getUserConfig().syncContacts = true;
                                getUserConfig().saveConfig(false);
                                getContactsController().forceImportContacts();
                            } else if (which == 1) { // Reload Contacts
                                getContactsController().loadContacts(false, 0);
                            } else if (which == 2) { // Reset Imported Contacts
                                getContactsController().resetImportedContacts();
                            } else if (which == 3) { // Reset Dialogs
                                getMessagesController().forceResetDialogs();
                            } else if (which == 4) { // Logs
                                BuildVars.LOGS_ENABLED = !BuildVars.LOGS_ENABLED;
                                SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
                                sharedPreferences.edit().putBoolean("logsEnabled", BuildVars.LOGS_ENABLED).commit();
                                updateRowsIds();
                                listAdapter.notifyDataSetChanged();
                                if (BuildVars.LOGS_ENABLED) {
                                    FileLog.d("app start time = " + ApplicationLoader.startTime);
                                    try {
                                        FileLog.d("buildVersion = " + ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0).versionCode);
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                    }
                                }
                            } else if (which == 5) { // In-app camera
                                SharedConfig.toggleInappCamera();
                            } else if (which == 6) { // Clear sent media cache
                                getMessagesStorage().clearSentMedia();
                                SharedConfig.setNoSoundHintShowed(false);
                                SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                                editor.remove("archivehint").remove("proximityhint").remove("archivehint_l").remove("speedhint").remove("gifhint").remove("reminderhint").remove("soundHint").remove("themehint").remove("bganimationhint").remove("filterhint").remove("n_0").remove("storyprvhint").remove("storyhint").remove("storyhint2").remove("storydualhint").remove("storysvddualhint").remove("stories_camera").remove("dualcam").remove("dualmatrix").remove("dual_available").remove("archivehint").remove("askNotificationsAfter").remove("askNotificationsDuration").remove("viewoncehint").remove("voicepausehint").remove("taptostorysoundhint").remove("nothanos").remove("voiceoncehint").remove("savedhint").remove("savedsearchhint").remove("savedsearchtaghint").remove("groupEmojiPackHintShown").remove("newppsms").remove("monetizationadshint").remove("seekSpeedHintShowed").remove("unsupport_video/av01").remove("channelgifthint").remove("statusgiftpage").remove("multistorieshint").remove("channelsuggesthint").remove("trimvoicehint").apply();
                                MessagesController.getEmojiSettings(currentAccount).edit().remove("featured_hidden").remove("emoji_featured_hidden").commit();
                                SharedConfig.textSelectionHintShows = 0;
                                SharedConfig.lockRecordAudioVideoHint = 0;
                                SharedConfig.stickersReorderingHintUsed = false;
                                SharedConfig.forwardingOptionsHintShown = false;
                                SharedConfig.replyingOptionsHintShown = false;
                                SharedConfig.messageSeenHintCount = 3;
                                SharedConfig.emojiInteractionsHintCount = 3;
                                SharedConfig.dayNightThemeSwitchHintCount = 3;
                                SharedConfig.fastScrollHintCount = 3;
                                SharedConfig.stealthModeSendMessageConfirm = 2;
                                SharedConfig.updateStealthModeSendMessageConfirm(2);
                                SharedConfig.setStoriesReactionsLongPressHintUsed(false);
                                SharedConfig.setStoriesIntroShown(false);
                                SharedConfig.setMultipleReactionsPromoShowed(false);
                                ChatThemeController.getInstance(currentAccount).clearCache();
                                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
                                RestrictedLanguagesSelectActivity.cleanup();
                                PersistColorPalette.getInstance(currentAccount).cleanup();
                                SharedPreferences prefs = getMessagesController().getMainSettings();
                                editor = prefs.edit();
                                editor.remove("peerColors").remove("profilePeerColors").remove("boostingappearance").remove("bizbothint").remove("movecaptionhint");
                                for (String key : prefs.getAll().keySet()) {
                                    if (key.contains("show_gift_for_") || key.contains("bdayhint_") || key.contains("bdayanim_") || key.startsWith("ask_paid_message_") || key.startsWith("topicssidetabs")) {
                                        editor.remove(key);
                                    }
                                }
                                editor.apply();
                                editor = MessagesController.getNotificationsSettings(currentAccount).edit();
                                for (String key : MessagesController.getNotificationsSettings(currentAccount).getAll().keySet()) {
                                    if (key.startsWith("dialog_bar_botver")) {
                                        editor.remove(key);
                                    }
                                }
                                editor.apply();
                            } else if (which == 7) { // Call settings
                                VoIPHelper.showCallDebugSettings(getParentActivity());
                            } else if (which == 8) { // ?
                                SharedConfig.toggleRoundCamera16to9();
                            } else if (which == 9) { // Check app update
                                ((LaunchActivity) getParentActivity()).checkAppUpdate(true, null);
                            } else if (which == 10) { // Read all chats
                                getMessagesStorage().readAllDialogs(-1);
                            } else if (which == 11) { // Voip audio effects
                                SharedConfig.toggleDisableVoiceAudioEffects();
                            } else if (which == 12) { // Clean app update
                                SharedConfig.pendingAppUpdate = null;
                                SharedConfig.saveConfig();
                                viewModel.postNotificationName(NotificationCenter.appUpdateAvailable);
                            } else if (which == 13) { // Reset suggestions
                                Set<String> suggestions = getMessagesController().pendingSuggestions;
                                suggestions.add("VALIDATE_PHONE_NUMBER");
                                suggestions.add("VALIDATE_PASSWORD");
                                getNotificationCenter().postNotificationName(NotificationCenter.newSuggestionsAvailable);
                            } else if (which == 14) { // WebView Cache
                                ApplicationLoader.applicationContext.deleteDatabase("webview.db");
                                ApplicationLoader.applicationContext.deleteDatabase("webviewCache.db");
                                WebStorage.getInstance().deleteAllData();
                                try {
                                    WebView webView = new WebView(ApplicationLoader.applicationContext);
                                    webView.clearHistory();
                                    webView.destroy();
                                } catch (Exception e) {}
                            } else if (which == 15) {
                                CookieManager cookieManager = CookieManager.getInstance();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    cookieManager.removeAllCookies(null);
                                    cookieManager.flush();
                                }
                            } else if (which == 16) { // WebView debug
                                SharedConfig.toggleDebugWebView();
                                Toast.makeText(getParentActivity(), getString(SharedConfig.debugWebView ? R.string.DebugMenuWebViewDebugEnabled : R.string.DebugMenuWebViewDebugDisabled), Toast.LENGTH_SHORT).show();
                            } else if (which == 17) { // Tablet mode
                                SharedConfig.toggleForceDisableTabletMode();

                                Activity activity = AndroidUtilities.findActivity(context);
                                final PackageManager pm = activity.getPackageManager();
                                final Intent intent = pm.getLaunchIntentForPackage(activity.getPackageName());
                                activity.finishAffinity(); // Finishes all activities.
                                activity.startActivity(intent);    // Start the launch activity
                                System.exit(0);
                            } else if (which == 18) {
                                FloatingDebugController.setActive((LaunchActivity) getParentActivity(), !FloatingDebugController.isActive());
                            } else if (which == 19) {
                                getMessagesController().loadAppConfig();
                                TLRPC.TL_help_dismissSuggestion req = new TLRPC.TL_help_dismissSuggestion();
                                req.suggestion = "VALIDATE_PHONE_NUMBER";
                                req.peer = new TLRPC.TL_inputPeerEmpty();
                                getConnectionsManager().sendRequest(req, (response, error) -> {
                                    TLRPC.TL_help_dismissSuggestion req2 = new TLRPC.TL_help_dismissSuggestion();
                                    req2.suggestion = "VALIDATE_PASSWORD";
                                    req2.peer = new TLRPC.TL_inputPeerEmpty();
                                    getConnectionsManager().sendRequest(req2, (res2, err2) -> {
                                        getMessagesController().loadAppConfig();
                                    });
                                });
                            } else if (which == 20) {
                                int androidVersion = Build.VERSION.SDK_INT;
                                int cpuCount = ConnectionsManager.CPU_COUNT;
                                int memoryClass = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
                                long minFreqSum = 0, minFreqCount = 0;
                                long maxFreqSum = 0, maxFreqCount = 0;
                                long curFreqSum = 0, curFreqCount = 0;
                                long capacitySum = 0, capacityCount = 0;
                                StringBuilder cpusInfo = new StringBuilder();
                                for (int i = 0; i < cpuCount; i++) {
                                    Long minFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq");
                                    Long curFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_cur_freq");
                                    Long maxFreq = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
                                    Long capacity = AndroidUtilities.getSysInfoLong("/sys/devices/system/cpu/cpu" + i + "/cpu_capacity");
                                    cpusInfo.append("#").append(i).append(" ");
                                    if (minFreq != null) {
                                        cpusInfo.append("min=").append(minFreq / 1000L).append(" ");
                                        minFreqSum += (minFreq / 1000L);
                                        minFreqCount++;
                                    }
                                    if (curFreq != null) {
                                        cpusInfo.append("cur=").append(curFreq / 1000L).append(" ");
                                        curFreqSum += (curFreq / 1000L);
                                        curFreqCount++;
                                    }
                                    if (maxFreq != null) {
                                        cpusInfo.append("max=").append(maxFreq / 1000L).append(" ");
                                        maxFreqSum += (maxFreq / 1000L);
                                        maxFreqCount++;
                                    }
                                    if (capacity != null) {
                                        cpusInfo.append("cpc=").append(capacity).append(" ");
                                        capacitySum += capacity;
                                        capacityCount++;
                                    }
                                    cpusInfo.append("\n");
                                }
                                StringBuilder info = new StringBuilder();
                                info.append(Build.MANUFACTURER).append(", ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(", ").append(Build.DEVICE).append(") ").append(" (android ").append(Build.VERSION.SDK_INT).append(")\n");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    info.append("SoC: ").append(Build.SOC_MANUFACTURER).append(", ").append(Build.SOC_MODEL).append("\n");
                                }
                                String gpuModel = AndroidUtilities.getSysInfoString("/sys/kernel/gpu/gpu_model");
                                if (gpuModel != null) {
                                    info.append("GPU: ").append(gpuModel);
                                    Long minClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_min_clock");
                                    Long mminClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_mm_min_clock");
                                    Long maxClock = AndroidUtilities.getSysInfoLong("/sys/kernel/gpu/gpu_max_clock");
                                    if (minClock != null) {
                                        info.append(", min=").append(minClock / 1000L);
                                    }
                                    if (mminClock != null) {
                                        info.append(", mmin=").append(mminClock / 1000L);
                                    }
                                    if (maxClock != null) {
                                        info.append(", max=").append(maxClock / 1000L);
                                    }
                                    info.append("\n");
                                }
                                ConfigurationInfo configurationInfo = ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
                                info.append("GLES Version: ").append(configurationInfo.getGlEsVersion()).append("\n");
                                info.append("Memory: class=").append(AndroidUtilities.formatFileSize(memoryClass * 1024L * 1024L));
                                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                                ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
                                info.append(", total=").append(AndroidUtilities.formatFileSize(memoryInfo.totalMem));
                                info.append(", avail=").append(AndroidUtilities.formatFileSize(memoryInfo.availMem));
                                info.append(", low?=").append(memoryInfo.lowMemory);
                                info.append(" (threshold=").append(AndroidUtilities.formatFileSize(memoryInfo.threshold)).append(")");
                                info.append("\n");
                                info.append("Current class: ").append(SharedConfig.performanceClassName(SharedConfig.getDevicePerformanceClass())).append(", measured: ").append(SharedConfig.performanceClassName(SharedConfig.measureDevicePerformanceClass()));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    info.append(", suggest=").append(Build.VERSION.MEDIA_PERFORMANCE_CLASS);
                                }
                                info.append("\n");
                                info.append(cpuCount).append(" CPUs");
                                if (minFreqCount > 0) {
                                    info.append(", avgMinFreq=").append(minFreqSum / minFreqCount);
                                }
                                if (curFreqCount > 0) {
                                    info.append(", avgCurFreq=").append(curFreqSum / curFreqCount);
                                }
                                if (maxFreqCount > 0) {
                                    info.append(", avgMaxFreq=").append(maxFreqSum / maxFreqCount);
                                }
                                if (capacityCount > 0) {
                                    info.append(", avgCapacity=").append(capacitySum / capacityCount);
                                }
                                info.append("\n").append(cpusInfo);

                                listCodecs("video/avc", info);
                                listCodecs("video/hevc", info);
                                listCodecs("video/x-vnd.on2.vp8", info);
                                listCodecs("video/x-vnd.on2.vp9", info);

                                showDialog(new ShareAlert(getParentActivity(), null, info.toString(), false, null, false) {
                                    @Override
                                    protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic, boolean showToast) {
                                        if (!showToast) return;
                                        AndroidUtilities.runOnUIThread(() -> {
                                            BulletinFactory.createInviteSentBulletin(getParentActivity(), frameLayout, dids.size(), dids.size() == 1 ? dids.valueAt(0).id : 0, count, getThemedColor(Theme.key_undo_background), getThemedColor(Theme.key_undo_infoColor)).show();
                                        }, 250);
                                    }
                                });
                            } else if (which == 21) {
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                                builder2.setTitle("Force performance class");
                                int currentClass = SharedConfig.getDevicePerformanceClass();
                                int trueClass = SharedConfig.measureDevicePerformanceClass();
                                builder2.setItems(new CharSequence[] {
                                    AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_HIGH ? "**HIGH**" : "HIGH") + (trueClass == SharedConfig.PERFORMANCE_CLASS_HIGH ? " (measured)" : "")),
                                    AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_AVERAGE ? "**AVERAGE**" : "AVERAGE") + (trueClass == SharedConfig.PERFORMANCE_CLASS_AVERAGE ? " (measured)" : "")),
                                    AndroidUtilities.replaceTags((currentClass == SharedConfig.PERFORMANCE_CLASS_LOW ? "**LOW**" : "LOW") + (trueClass == SharedConfig.PERFORMANCE_CLASS_LOW ? " (measured)" : ""))
                                }, (dialog2, which2) -> {
                                    int newClass = 2 - which2;
                                    if (newClass == trueClass) {
                                        SharedConfig.overrideDevicePerformanceClass(-1);
                                    } else {
                                        SharedConfig.overrideDevicePerformanceClass(newClass);
                                    }
                                });
                                builder2.setNegativeButton(getString("Cancel", R.string.Cancel), null);
                                builder2.show();
                            } else if (which == 22) {
                                SharedConfig.toggleRoundCamera();
                            } else if (which == 23) {
                                boolean enabled = DualCameraView.dualAvailableStatic(getContext());
                                MessagesController.getGlobalMainSettings().edit().putBoolean("dual_available", !enabled).apply();
                                try {
                                    Toast.makeText(getParentActivity(), getString(!enabled ? R.string.DebugMenuDualOnToast : R.string.DebugMenuDualOffToast), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {}
                            } else if (which == 24) {
                                SharedConfig.toggleSurfaceInStories();
                                for (int i = 0; i < getParentLayout().getFragmentStack().size(); i++) {
                                    getParentLayout().getFragmentStack().get(i).clearSheets();
                                }
                            } else if (which == 25) {
                                SharedConfig.togglePhotoViewerBlur();
                            } else if (which == 26) {
                                SharedConfig.togglePaymentByInvoice();
                            } else if (which == 27) {
                                getMediaDataController().loadAttachMenuBots(false, true);
                            } else if (which == 28) {
                                SharedConfig.toggleUseCamera2(currentAccount);
                            } else if (which == 29) {
                                BotBiometry.clear();
                                BotLocation.clear();
                                BotDownloads.clear();
                                SetupEmojiStatusSheet.clear();
                            } else if (which == 30) {
                                AuthTokensHelper.clearLogInTokens();
                            } else if (which == 31) {
                                SharedConfig.toggleUseNewBlur();
                            } else if (which == 32) {
                                SharedConfig.toggleBrowserAdaptableColors();
                            } else if (which == 33) {
                                SharedConfig.toggleDebugVideoQualities();
                            } else if (which == 34) {
                                SharedConfig.toggleUseSystemBoldFont();
                            } else if (which == 35) {
                                MessagesController.getInstance(currentAccount).loadAppConfig(true);
                            } else if (which == 36) {
                                SharedConfig.toggleForceForumTabs();
                            }
                        });
                        builder.setNegativeButton(getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    } else {
                        try {
                            Toast.makeText(getParentActivity(), getString("DebugMenuLongPress", R.string.DebugMenuLongPress), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    return true;
                } else if (position >= membersStartRow && position < membersEndRow) {
                    final TLRPC.ChatParticipant participant;
                    if (!viewModel.getState().getValue().getSortedUsers().isEmpty()) {
                        participant = visibleChatParticipants.get(viewModel.getState().getValue().getSortedUsers().get(position - membersStartRow));
                    } else {
                        participant = visibleChatParticipants.get(position - membersStartRow);
                    }
                    return onMemberClick(participant, true, view);
                } else if (position == birthdayRow) {
                    if (editRow(view, position)) return true;
                    if (viewModel.getState().getValue().getUserInfo() == null) return false;
                    try {
                        AndroidUtilities.addToClipboard(UserInfoActivity.birthdayString(viewModel.getState().getValue().getUserInfo().birthday));
                        BulletinFactory.of(ProfileActivity.this).createCopyBulletin(getString(R.string.BirthdayCopied)).show();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    return true;
                } else {
                    if (editRow(view, position)) return true;
                    return processOnClickOrPress(position, view, view.getWidth() / 2f, (int) (view.getHeight() * .75f));
                }
            }
        });

        if (viewModel.getArgs().openSimilar) {
            updateRowsIds();
            scrollToSharedMedia();
            savedScrollToSharedMedia = true;
            savedScrollPosition = sharedMediaRow;
            savedScrollOffset = 0;
        }

        if (searchItem != null) {
            searchListView = new RecyclerListView(context);
            searchListView.setVerticalScrollBarEnabled(false);
            searchListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            searchListView.setGlowColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));
            searchListView.setAdapter(searchAdapter);
            searchListView.setItemAnimator(null);
            searchListView.setVisibility(View.GONE);
            searchListView.setLayoutAnimation(null);
            searchListView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            frameLayout.addView(searchListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
            searchListView.setOnItemClickListener((view, position) -> {
                if (position < 0) {
                    return;
                }
                Object object = numberRow;
                boolean add = true;
                if (searchAdapter.searchWas) {
                    if (position < searchAdapter.searchResults.size()) {
                        object = searchAdapter.searchResults.get(position);
                    } else {
                        position -= searchAdapter.searchResults.size() + 1;
                        if (position >= 0 && position < searchAdapter.faqSearchResults.size()) {
                            object = searchAdapter.faqSearchResults.get(position);
                        }
                    }
                } else {
                    if (!searchAdapter.recentSearches.isEmpty()) {
                        position--;
                    }
                    if (position >= 0 && position < searchAdapter.recentSearches.size()) {
                        object = searchAdapter.recentSearches.get(position);
                    } else {
                        position -= searchAdapter.recentSearches.size() + 1;
                        if (position >= 0 && position < searchAdapter.faqSearchArray.size()) {
                            object = searchAdapter.faqSearchArray.get(position);
                            add = false;
                        }
                    }
                }
                if (object instanceof SearchAdapter.SearchResult) {
                    SearchAdapter.SearchResult result = (SearchAdapter.SearchResult) object;
                    result.open();
                } else if (object instanceof MessagesController.FaqSearchResult) {
                    MessagesController.FaqSearchResult result = (MessagesController.FaqSearchResult) object;
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.openArticle, searchAdapter.faqWebPage, result.url);
                }
                if (add && object != null) {
                    searchAdapter.addRecent(object);
                }
            });
            searchListView.setOnItemLongClickListener((view, position) -> {
                if (searchAdapter.isSearchWas() || searchAdapter.recentSearches.isEmpty()) {
                    return false;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.ClearSearchAlertTitle));
                builder.setMessage(LocaleController.getString(R.string.ClearSearchAlert));
                builder.setPositiveButton(LocaleController.getString(R.string.ClearButton), (dialogInterface, i) -> searchAdapter.clearRecent());
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                }
                return true;
            });
            searchListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                }
            });
            searchListView.setAnimateEmptyView(true, RecyclerListView.EMPTY_VIEW_ANIMATION_TYPE_ALPHA_SCALE);

            emptyView = new StickerEmptyView(context, null, 1);
            emptyView.setAnimateLayoutChange(true);
            emptyView.subtitle.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            frameLayout.addView(emptyView);

            searchAdapter.loadFaqWebPage();
        }

        if (viewModel.getArgs().banFromGroup != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().banFromGroup);
            if (currentChannelParticipant == null) {
                TLRPC.TL_channels_getParticipant req = new TLRPC.TL_channels_getParticipant();
                req.channel = MessagesController.getInputChannel(chat);
                req.participant = getMessagesController().getInputPeer(viewModel.getArgs().userId);
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (response != null) {
                        AndroidUtilities.runOnUIThread(() -> currentChannelParticipant = ((TLRPC.TL_channels_channelParticipant) response).participant);
                    }
                });
            }
            FrameLayout frameLayout1 = new FrameLayout(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
                    Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
                    Theme.chat_composeShadowDrawable.draw(canvas);
                    canvas.drawRect(0, bottom, getMeasuredWidth(), getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
                }
            };
            frameLayout1.setWillNotDraw(false);

            frameLayout.addView(frameLayout1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.LEFT | Gravity.BOTTOM));
            frameLayout1.setOnClickListener(v -> {
                ChatRightsEditActivity fragment = new ChatRightsEditActivity(viewModel.getArgs().userId, viewModel.getArgs().banFromGroup, null, chat.default_banned_rights, currentChannelParticipant != null ? currentChannelParticipant.banned_rights : null, "", ChatRightsEditActivity.TYPE_BANNED, true, false, null);
                fragment.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
                    @Override
                    public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                        removeSelfFromStack();
                        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                        if (user != null && chat != null && viewModel.getArgs().userId != 0 && fragment != null && fragment.banning && fragment.getParentLayout() != null) {
                            for (BaseFragment fragment : fragment.getParentLayout().getFragmentStack()) {
                                if (fragment instanceof ChannelAdminLogActivity) {
                                    ((ChannelAdminLogActivity) fragment).reloadLastMessages();
                                    AndroidUtilities.runOnUIThread(() -> {
                                        BulletinFactory.createRemoveFromChatBulletin(fragment, user, chat.title).show();
                                    });
                                    return;
                                }
                            }
                        }
                    }
                    @Override
                    public void didChangeOwner(TLRPC.User user) {
                        undoView.showWithAction(-viewModel.getArgs().chatId, viewModel.getState().getValue().isCurrentChatMagagroup() ? UndoView.ACTION_OWNER_TRANSFERED_GROUP : UndoView.ACTION_OWNER_TRANSFERED_CHANNEL, user);
                    }
                });
                presentFragment(fragment);
            });

            TextView textView = new TextView(context);
            textView.setTextColor(getThemedColor(Theme.key_text_RedRegular));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setGravity(Gravity.CENTER);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setText(LocaleController.getString(R.string.BanFromTheGroup));
            frameLayout1.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 1, 0, 0));

            listView.setPadding(0, AndroidUtilities.dp(88), 0, AndroidUtilities.dp(48));
            listView.setBottomGlowOffset(AndroidUtilities.dp(48));
        } else {
            listView.setPadding(0, AndroidUtilities.dp(88), 0, 0);
        }

        topView = new TopView(this, context, getResourceProvider());
        topView.setBackgroundColorId(peerColor, false);
        topView.setBackgroundColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(topView);

        animatedStatusView = new DrawerProfileCell.AnimatedStatusView(context, 20, 60);
        animatedStatusView.setPivotX(AndroidUtilities.dp(30));
        animatedStatusView.setPivotY(AndroidUtilities.dp(30));

        avatarContainer = new FrameLayout(context);
        avatarContainer2 = new FrameLayout(context) {

            CanvasButton canvasButton;

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (transitionOnlineText != null) {
                    canvas.save();
                    canvas.translate(onlineTextView[0].getX(), onlineTextView[0].getY());
                    canvas.saveLayerAlpha(0, 0, transitionOnlineText.getMeasuredWidth(), transitionOnlineText.getMeasuredHeight(), (int) (255 * (1f - avatarAnimationProgress)), Canvas.ALL_SAVE_FLAG);
                    transitionOnlineText.draw(canvas);
                    canvas.restore();
                    canvas.restore();
                    invalidate();
                }
                if (viewModel.getState().getValue().isHasFallbackPhoto() && photoDescriptionProgress != 0 && customAvatarProgress != 1f) {
                    float cy =  onlineTextView[1].getY() + onlineTextView[1].getMeasuredHeight() / 2f;
                    float size = AndroidUtilities.dp(22);
                    float x = AndroidUtilities.dp(28) - customPhotoOffset + onlineTextView[1].getX() - size;

                    fallbackImage.setImageCoords(x, cy - size / 2f, size, size);
                    fallbackImage.setAlpha(photoDescriptionProgress);
                    canvas.save();
                    float s = photoDescriptionProgress;
                    canvas.scale(s, s, fallbackImage.getCenterX(), fallbackImage.getCenterY());
                    fallbackImage.draw(canvas);
                    canvas.restore();

                    if (customAvatarProgress == 0) {
                        if (canvasButton == null) {
                            canvasButton = new CanvasButton(this);
                            canvasButton.setDelegate(() -> {
                                if (customAvatarProgress != 1f) {
                                    avatarsViewPager.scrollToLastItem();
                                }
                            });
                        }
                        AndroidUtilities.rectTmp.set(x - AndroidUtilities.dp(4), cy - AndroidUtilities.dp(14), x + onlineTextView[2].getTextWidth() + AndroidUtilities.dp(28) * (1f - customAvatarProgress) + AndroidUtilities.dp(4), cy + AndroidUtilities.dp(14));
                        canvasButton.setRect(AndroidUtilities.rectTmp);
                        canvasButton.setRounded(true);
                        canvasButton.setColor(Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, 50));
                        canvasButton.draw(canvas);
                    } else {
                        if (canvasButton != null) {
                            canvasButton.cancelRipple();
                        }
                    }
                }
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return (canvasButton != null && canvasButton.checkTouchEvent(ev)) || super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return (canvasButton != null && canvasButton.checkTouchEvent(event)) || super.onTouchEvent(event);
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                fallbackImage.onAttachedToWindow();
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                fallbackImage.onDetachedFromWindow();
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateCollectibleHint();
            }
        };
        fallbackImage = new ImageReceiver(avatarContainer2);
        fallbackImage.setRoundRadius(AndroidUtilities.dp(11));
        AndroidUtilities.updateViewVisibilityAnimated(avatarContainer2, true, 1f, false);
        frameLayout.addView(avatarContainer2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.START, 0, 0, 0, 0));
        avatarContainer.setPivotX(0);
        avatarContainer.setPivotY(0);
        avatarContainer2.addView(avatarContainer, LayoutHelper.createFrame(42, 42, Gravity.TOP | Gravity.LEFT, 64, 0, 0, 0));
        avatarImage = new ProfileAvatarImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (getImageReceiver().hasNotThumb()) {
                    info.setText(LocaleController.getString(R.string.AccDescrProfilePicture));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, LocaleController.getString(R.string.Open)));
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, LocaleController.getString(R.string.AccDescrOpenInPhotoViewer)));
                    }
                } else {
                    info.setVisibleToUser(false);
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (animatedEmojiDrawable != null && animatedEmojiDrawable.getImageReceiver() != null) {
                    animatedEmojiDrawable.getImageReceiver().startAnimation();
                }
            }
        };
        avatarImage.getImageReceiver().setAllowDecodeSingleFrame(true);
        avatarImage.setRoundRadius(getSmallAvatarRoundRadius());
        avatarImage.setPivotX(0);
        avatarImage.setPivotY(0);
        avatarContainer.addView(avatarImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        avatarImage.setOnClickListener(v -> {
            if (viewModel.getState().getValue().getAvatarBig() != null) {
                return;
            }
            if (viewModel.getArgs().isTopic && !getMessagesController().premiumFeaturesBlocked()) {
                ArrayList<TLRPC.TL_forumTopic> topics = getMessagesController().getTopicsController().getTopics(viewModel.getArgs().chatId);
                if (topics != null) {
                    TLRPC.TL_forumTopic currentTopic = null;
                    for (int i = 0; currentTopic == null && i < topics.size(); ++i) {
                        TLRPC.TL_forumTopic topic = topics.get(i);
                        if (topic != null && topic.id == viewModel.getArgs().topicId) {
                            currentTopic = topic;
                        }
                    }
                    if (currentTopic != null && currentTopic.icon_emoji_id != 0) {
                        long documentId = currentTopic.icon_emoji_id;
                        TLRPC.Document document = AnimatedEmojiDrawable.findDocument(currentAccount, documentId);
                        if (document == null) {
                            return;
                        }
                        Bulletin bulletin = BulletinFactory.of(ProfileActivity.this).createContainsEmojiBulletin(document, BulletinFactory.CONTAINS_EMOJI_IN_TOPIC, set -> {
                            ArrayList<TLRPC.InputStickerSet> inputSets = new ArrayList<>(1);
                            inputSets.add(set);
                            EmojiPacksAlert alert = new EmojiPacksAlert(ProfileActivity.this, getParentActivity(), resourcesProvider, inputSets);
                            showDialog(alert);
                        });
                        if (bulletin != null) {
                            bulletin.show();
                        }
                    }
                }
                return;
            }
            if (expandAvatar()) {
                return;
            }
            openAvatar();
        });
        avatarImage.setHasStories(needInsetForStories());
        avatarImage.setOnLongClickListener(v -> {
            if (viewModel.getState().getValue().getAvatarBig() != null || viewModel.getArgs().isTopic) {
                return false;
            }
            openAvatar();
            return false;
        });

        avatarProgressView = new RadialProgressView(context) {
            private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                paint.setColor(0x55000000);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (avatarImage != null && avatarImage.getImageReceiver().hasNotThumb()) {
                    paint.setAlpha((int) (0x55 * avatarImage.getImageReceiver().getCurrentAlpha()));
                    canvas.drawCircle(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f, getMeasuredWidth() / 2.0f, paint);
                }
                super.onDraw(canvas);
            }
        };
        avatarProgressView.setSize(AndroidUtilities.dp(26));
        avatarProgressView.setProgressColor(0xffffffff);
        avatarProgressView.setNoProgress(false);
        avatarContainer.addView(avatarProgressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        timeItem = new ImageView(context);
        timeItem.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
        timeItem.setScaleType(ImageView.ScaleType.CENTER);
        timeItem.setAlpha(0.0f);
        timeItem.setImageDrawable(timerDrawable = new TimerDrawable(context, null));
        timeItem.setTranslationY(-1);
        frameLayout.addView(timeItem, LayoutHelper.createFrame(34, 34, Gravity.TOP | Gravity.LEFT));

        starBgItem = new ImageView(context);
        starBgItem.setImageResource(R.drawable.star_small_outline);
        starBgItem.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefault), PorterDuff.Mode.SRC_IN));
        starBgItem.setAlpha(0.0f);
        starBgItem.setScaleY(0.0f);
        starBgItem.setScaleX(0.0f);
        frameLayout.addView(starBgItem, LayoutHelper.createFrame(20, 20, Gravity.TOP | Gravity.LEFT));

        starFgItem = new ImageView(context);
        starFgItem.setImageResource(R.drawable.star_small_inner);
        starFgItem.setAlpha(0.0f);
        starFgItem.setScaleY(0.0f);
        starFgItem.setScaleX(0.0f);
        frameLayout.addView(starFgItem, LayoutHelper.createFrame(20, 20, Gravity.TOP | Gravity.LEFT));

        showAvatarProgress(false, false);

        if (avatarsViewPager != null) {
            avatarsViewPager.onDestroy();
        }
        overlaysView = new OverlaysView(this, context);
        avatarsViewPager = new ProfileGalleryView(context, viewModel.getArgs().userId != 0 ? viewModel.getArgs().userId : -viewModel.getArgs().chatId, actionBar, listView, avatarImage, getClassGuid(), overlaysView) {
            @Override
            protected void setCustomAvatarProgress(float progress) {
                customAvatarProgress = progress;
                checkPhotoDescriptionAlpha();
            }
        };
        if (viewModel.getArgs().userId != getUserConfig().clientUserId && viewModel.getState().getValue().getUserInfo() != null) {
            customAvatarProgress = viewModel.getState().getValue().getUserInfo().profile_photo == null ? 0 : 1;
        }
        if (!viewModel.getArgs().isTopic) {
            avatarsViewPager.setChatInfo(viewModel.getState().getValue().getChatInfo());
        }
        avatarContainer2.addView(avatarsViewPager);
        avatarContainer2.addView(overlaysView);
        avatarImage.setAvatarsViewPager(avatarsViewPager);

        avatarsViewPagerIndicatorView = new PagerIndicatorView(context);
        avatarContainer2.addView(avatarsViewPagerIndicatorView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        frameLayout.addView(actionBar);

        float rightMargin = (54 + ((callItemVisible && viewModel.getArgs().userId != 0) ? 54 : 0));
        boolean hasTitleExpanded = false;
        int initialTitleWidth = LayoutHelper.WRAP_CONTENT;
        if (parentLayout != null && parentLayout.getLastFragment() instanceof ChatActivity) {
            ChatAvatarContainer avatarContainer = ((ChatActivity) parentLayout.getLastFragment()).getAvatarContainer();
            if (avatarContainer != null) {
                hasTitleExpanded = avatarContainer.getTitleTextView().getPaddingRight() != 0;
                if (avatarContainer.getLayoutParams() != null && avatarContainer.getTitleTextView() != null) {
                    rightMargin =
                            (((ViewGroup.MarginLayoutParams) avatarContainer.getLayoutParams()).rightMargin +
                                    (avatarContainer.getWidth() - avatarContainer.getTitleTextView().getRight())) / AndroidUtilities.density;
//                initialTitleWidth = (int) (avatarContainer.getTitleTextView().getWidth() / AndroidUtilities.density);
                }
            }
        }

        for (int a = 0; a < nameTextView.length; a++) {
            if (playProfileAnimation == 0 && a == 0) {
                continue;
            }
            nameTextView[a] = new SimpleTextView(context) {
                @Override
                public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(info);
                    if (isFocusable() && (nameTextViewRightDrawableContentDescription != null || nameTextViewRightDrawable2ContentDescription != null)) {
                        StringBuilder s = new StringBuilder(getText());
                        if (nameTextViewRightDrawable2ContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawable2ContentDescription);
                        }
                        if (nameTextViewRightDrawableContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawableContentDescription);
                        }
                        info.setText(s);
                    }
                }
                @Override
                protected void onDraw(Canvas canvas) {
                    final int wasRightDrawableX = getRightDrawableX();
                    super.onDraw(canvas);
                    if (wasRightDrawableX != getRightDrawableX()) {
                        updateCollectibleHint();
                    }
                }
            };
            if (a == 1) {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_profile_title));
            } else {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_actionBarDefaultTitle));
            }
            nameTextView[a].setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(a == 0 ? 12 : 4));
            nameTextView[a].setTextSize(18);
            nameTextView[a].setGravity(Gravity.LEFT);
            nameTextView[a].setTypeface(AndroidUtilities.bold());
            nameTextView[a].setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
            nameTextView[a].setPivotX(0);
            nameTextView[a].setPivotY(0);
            nameTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            if (a == 1) {
                nameTextView[a].setScrollNonFitText(true);
                nameTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            nameTextView[a].setFocusable(a == 0);
            nameTextView[a].setEllipsizeByGradient(true);
            nameTextView[a].setRightDrawableOutside(a == 0);
            avatarContainer2.addView(nameTextView[a], LayoutHelper.createFrame(a == 0 ? initialTitleWidth : LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118, -6, (a == 0 ? rightMargin - (hasTitleExpanded ? 10 : 0) : 0), 0));
        }
        for (int a = 0; a < onlineTextView.length; a++) {
            if (a == 1) {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context) {

                    @Override
                    public void setAlpha(float alpha) {
                        super.setAlpha(alpha);
                        checkPhotoDescriptionAlpha();
                    }

                    @Override
                    public void setTranslationY(float translationY) {
                        super.setTranslationY(translationY);
                        onlineTextView[2].setTranslationY(translationY);
                        onlineTextView[3].setTranslationY(translationY);
                    }

                    @Override
                    public void setTranslationX(float translationX) {
                        super.setTranslationX(translationX);
                        onlineTextView[2].setTranslationX(translationX);
                        onlineTextView[3].setTranslationX(translationX);
                    }

                    @Override
                    public void setTextColor(int color) {
                        super.setTextColor(color);
                        if (onlineTextView[2] != null) {
                            onlineTextView[2].setTextColor(color);
                            onlineTextView[3].setTextColor(color);
                        }
                        if (showStatusButton != null) {
                            showStatusButton.setTextColor(Theme.multAlpha(Theme.adaptHSV(color, -.02f, +.15f), 1.4f));
                        }
                    }
                };
            } else {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context);
            }

            onlineTextView[a].setEllipsizeByGradient(true);
            onlineTextView[a].setTextColor(applyPeerColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), true, null));
            onlineTextView[a].setTextSize(14);
            onlineTextView[a].setGravity(Gravity.LEFT);
            onlineTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            if (a == 1 || a == 2 || a == 3) {
                onlineTextView[a].setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
            }
            if (a > 0) {
                onlineTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            onlineTextView[a].setFocusable(a == 0);
            avatarContainer2.addView(onlineTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118 - (a == 1 || a == 2 || a == 3? 4 : 0), (a == 1 || a == 2 || a == 3 ? -2 : 0), (a == 0 ? rightMargin - (hasTitleExpanded ? 10 : 0) : 8) - (a == 1 || a == 2 || a == 3 ? 4 : 0), 0));
        }
        checkPhotoDescriptionAlpha();
        avatarContainer2.addView(animatedStatusView);

        mediaCounterTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setTextColor(getThemedColor(Theme.key_player_actionBarSubtitle));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, AndroidUtilities.dp(14));
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setGravity(Gravity.LEFT);
                return textView;
            }
        };
        mediaCounterTextView.setAlpha(0.0f);
        avatarContainer2.addView(mediaCounterTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 118.33f, -2, 8, 0));
        storyView = new ProfileStoriesView(context, currentAccount, viewModel.getDialogId(), viewModel.getArgs().isTopic, avatarContainer, avatarImage, resourcesProvider) {
            @Override
            protected void onTap(StoryViewer.PlaceProvider provider) {
                long did = viewModel.getDialogId();
                StoriesController storiesController = getMessagesController().getStoriesController();
                if (storiesController.hasStories(did) || storiesController.hasUploadingStories(did) || storiesController.isLastUploadingFailed(did)) {
                    getOrCreateStoryViewer().open(context, did, provider);
                } else if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().stories != null && !viewModel.getState().getValue().getUserInfo().stories.stories.isEmpty() && viewModel.getArgs().userId != getUserConfig().clientUserId) {
                    getOrCreateStoryViewer().open(context, viewModel.getState().getValue().getUserInfo().stories, provider);
                } else if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stories != null && !viewModel.getState().getValue().getChatInfo().stories.stories.isEmpty()) {
                    getOrCreateStoryViewer().open(context, viewModel.getState().getValue().getChatInfo().stories, provider);
                } else {
                    expandAvatar();
                }
            }

            @Override
            protected void onLongPress() {
                openAvatar();
            }
        };
        updateStoriesViewBounds(false);
        if (viewModel.getState().getValue().getUserInfo() != null) {
            storyView.setStories(viewModel.getState().getValue().getUserInfo().stories);
        } else if (viewModel.getState().getValue().getChatInfo() != null) {
            storyView.setStories(viewModel.getState().getValue().getChatInfo().stories);
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        avatarContainer2.addView(storyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        giftsView = new ProfileGiftsView(context, currentAccount, viewModel.getDialogId(), avatarContainer, avatarImage, resourcesProvider);
        avatarContainer2.addView(giftsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        updateProfileData(true);

        writeButton = new RLottieImageView(context);
        writeButtonSetBackground();
        if (viewModel.getArgs().userId != 0) {
            if (imageUpdater != null) {
                cameraDrawable = new RLottieDrawable(R.raw.camera_outline, String.valueOf(R.raw.camera_outline), AndroidUtilities.dp(56), AndroidUtilities.dp(56), false, null);
                cellCameraDrawable = new RLottieDrawable(R.raw.camera_outline, R.raw.camera_outline + "_cell", AndroidUtilities.dp(42), AndroidUtilities.dp(42), false, null);

                writeButton.setAnimation(cameraDrawable);
                writeButton.setContentDescription(LocaleController.getString(R.string.AccDescrChangeProfilePicture));
                writeButton.setPadding(AndroidUtilities.dp(2), 0, 0, AndroidUtilities.dp(2));
            } else {
                writeButton.setImageResource(R.drawable.profile_newmsg);
                writeButton.setContentDescription(LocaleController.getString(R.string.AccDescrOpenChat));
            }
        } else {
            writeButton.setImageResource(R.drawable.profile_discuss);
            writeButton.setContentDescription(LocaleController.getString(R.string.ViewDiscussion));
        }
        writeButton.setScaleType(ImageView.ScaleType.CENTER);

        frameLayout.addView(writeButton, LayoutHelper.createFrame(60, 60, Gravity.RIGHT | Gravity.TOP, 0, 0, 16, 0));
        writeButton.setOnClickListener(v -> {
            if (writeButton.getTag() != null) {
                return;
            }
            onWriteButtonClick();
        });
        needLayout(false);

        if (scrollTo != -1) {
            if (writeButtonTag != null) {
                writeButton.setTag(0);
                writeButton.setScaleX(0.2f);
                writeButton.setScaleY(0.2f);
                writeButton.setAlpha(0.0f);
            }
        }

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
                if (openingAvatar && newState != RecyclerView.SCROLL_STATE_SETTLING) {
                    openingAvatar = false;
                }
                if (searchItem != null) {
                    scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                    searchItem.setEnabled(!scrolling && !isPulledDown);
                }
                sharedMediaLayout.scrollingByUser = listView.scrollingByUser;
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (fwdRestrictedHint != null) {
                    fwdRestrictedHint.hide();
                }
                checkListViewScroll();
                if (viewModel.getState().getValue().getParticipantsMap() != null && !viewModel.getState().getValue().isUsersEndReached() && layoutManager.findLastVisibleItemPosition() > membersEndRow - 8) {
                    viewModel.getChannelParticipants(false);
                }
                sharedMediaLayout.setPinnedToTop(sharedMediaLayout.getY() <= 0);
                updateBottomButtonY();
            }
        });

        undoView = new UndoView(context, null, false, resourcesProvider);
        frameLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        expandAnimator = ValueAnimator.ofFloat(0f, 1f);
        expandAnimator.addUpdateListener(anim -> {
            setAvatarExpandProgress(anim.getAnimatedFraction());
        });
        expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        expandAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                actionBar.setItemsBackgroundColor(isPulledDown ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), false);
                avatarImage.clearForeground();
                doNotSetForeground = false;
                updateStoriesViewBounds(false);
            }
        });
        updateRowsIds();

        updateSelectedMediaTabText();

        fwdRestrictedHint = new HintView(getParentActivity(), 9);
        fwdRestrictedHint.setAlpha(0);
        frameLayout.addView(fwdRestrictedHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 12, 0, 12, 0));
        sharedMediaLayout.setForwardRestrictedHint(fwdRestrictedHint);

        ViewGroup decorView;
        if (Build.VERSION.SDK_INT >= 21) {
            decorView = (ViewGroup) getParentActivity().getWindow().getDecorView();
        } else {
            decorView = frameLayout;
        }
        pinchToZoomHelper = new PinchToZoomHelper(decorView, frameLayout) {

            Paint statusBarPaint;

            @Override
            protected void invalidateViews() {
                super.invalidateViews();
                fragmentView.invalidate();
                for (int i = 0; i < avatarsViewPager.getChildCount(); i++) {
                    avatarsViewPager.getChildAt(i).invalidate();
                }
                if (writeButton != null) {
                    writeButton.invalidate();
                }
            }

            @Override
            protected void drawOverlays(Canvas canvas, float alpha, float parentOffsetX, float parentOffsetY, float clipTop, float clipBottom) {
                if (alpha > 0) {
                    AndroidUtilities.rectTmp.set(0, 0, avatarsViewPager.getMeasuredWidth(), avatarsViewPager.getMeasuredHeight() + AndroidUtilities.dp(30));
                    canvas.saveLayerAlpha(AndroidUtilities.rectTmp, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);

                    avatarContainer2.draw(canvas);

                    if (actionBar.getOccupyStatusBar() && !SharedConfig.noStatusBar) {
                        if (statusBarPaint == null) {
                            statusBarPaint = new Paint();
                            statusBarPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.2f)));
                        }
                        canvas.drawRect(actionBar.getX(), actionBar.getY(), actionBar.getX() + actionBar.getMeasuredWidth(), actionBar.getY() + AndroidUtilities.statusBarHeight, statusBarPaint);
                    }
                    canvas.save();
                    canvas.translate(actionBar.getX(), actionBar.getY());
                    actionBar.draw(canvas);
                    canvas.restore();

                    if (writeButton != null && writeButton.getVisibility() == View.VISIBLE && writeButton.getAlpha() > 0) {
                        canvas.save();
                        float s = 0.5f + 0.5f * alpha;
                        canvas.scale(s, s, writeButton.getX() + writeButton.getMeasuredWidth() / 2f, writeButton.getY() + writeButton.getMeasuredHeight() / 2f);
                        canvas.translate(writeButton.getX(), writeButton.getY());
                        writeButton.draw(canvas);
                        canvas.restore();
                    }
                    canvas.restore();
                }
            }

            @Override
            protected boolean zoomEnabled(View child, ImageReceiver receiver) {
                if (!super.zoomEnabled(child, receiver)) {
                    return false;
                }
                return listView.getScrollState() != RecyclerView.SCROLL_STATE_DRAGGING;
            }
        };
        pinchToZoomHelper.setCallback(new PinchToZoomHelper.Callback() {
            @Override
            public void onZoomStarted(MessageObject messageObject) {
                listView.cancelClickRunnables(true);
                if (sharedMediaLayout != null && sharedMediaLayout.getCurrentListView() != null) {
                    sharedMediaLayout.getCurrentListView().cancelClickRunnables(true);
                }
                topView.setBackgroundColor(ColorUtils.blendARGB(getAverageColor(pinchToZoomHelper.getPhotoImage()), getThemedColor(Theme.key_windowBackgroundWhite), 0.1f));
            }
        });
        avatarsViewPager.setPinchToZoomHelper(pinchToZoomHelper);
        scrimPaint.setAlpha(0);
        actionBarBackgroundPaint.setColor(getThemedColor(Theme.key_listSelector));
        updateTtlIcon();

        blurredView = new View(context) {
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            blurredView.setForeground(new ColorDrawable(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_windowBackgroundWhite), 100)));
        }
        blurredView.setFocusable(false);
        blurredView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        blurredView.setOnClickListener(e -> {
            finishPreviewFragment();
        });
        blurredView.setVisibility(View.GONE);
        blurredView.setFitsSystemWindows(true);
        frameLayout.addView(blurredView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        createBirthdayEffect();
        createFloatingActionButton(getContext());

        if (viewModel.getArgs().myProfile) {
            frameLayout.addView(bottomButtonsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 72 + (1 / AndroidUtilities.density), Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
        }

        if (viewModel.getArgs().openGifts || viewModel.getArgs().openCommonChats) {
            AndroidUtilities.runOnUIThread(this::scrollToSharedMedia);
        }

        return fragmentView;
    }

    private void updateBottomButtonY() {
        if (bottomButtonsContainer == null) {
            return;
        }
        bottomButtonsContainer.setTranslationY(sharedMediaLayout != null && sharedMediaLayout.isAttachedToWindow() ? Math.max(0, dp(72 + 64 + 48) - (listView.getMeasuredHeight() - sharedMediaLayout.getY())) : dp(72));
        final Bulletin bulletin = Bulletin.getVisibleBulletin();
        if (bulletin != null) {
            bulletin.updatePosition();
        }
    }

    FrameLayout floatingButtonContainer;
    RLottieImageView floatingButton;
    boolean floatingHidden;
    float floatingButtonHideProgress;

    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private void updateAvatarRoundRadius() {
        avatarImage.setRoundRadius((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, currentExpandAnimatorValue));
    }

    private void createFloatingActionButton(Context context) {
        if (!getMessagesController().storiesEnabled()) {
            return;
        }
        if (viewModel.getDialogId() > 0L) {
            return;
        }
        TLRPC.Chat currentChat = MessagesController.getInstance(currentAccount).getChat(viewModel.getArgs().chatId);
        if (!ChatObject.isBoostSupported(currentChat)) {
            return;
        }
        StoriesController storiesController = getMessagesController().getStoriesController();
        if (!storiesController.canPostStories(viewModel.getDialogId())) {
            return;
        } else {
            viewModel.checkCanSendStoryForPosting();
        }
        long dialogId = viewModel.getDialogId();
        floatingButtonContainer = new FrameLayout(context);
        floatingButtonContainer.setVisibility(View.VISIBLE);
        //frameLayout.addView(floatingButtonContainer, LayoutHelper.createFrame((Build.VERSION.SDK_INT >= 21 ? 56 : 60), (Build.VERSION.SDK_INT >= 21 ? 56 : 60), (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButtonContainer.setOnClickListener(v -> {
            if (viewModel.getState().getValue().isShowBoostsAlert()) {
                if (loadingBoostsStats) {
                    return;
                }
                MessagesController messagesController = MessagesController.getInstance(currentAccount);
                loadingBoostsStats = true;
                messagesController.getBoostsController().getBoostsStats(dialogId, boostsStatus -> {
                    loadingBoostsStats = false;
                    if (boostsStatus == null) {
                        return;
                    }
                    messagesController.getBoostsController().userCanBoostChannel(dialogId, boostsStatus, canApplyBoost -> {
                        if (canApplyBoost == null) {
                            return;
                        }
                        BaseFragment lastFragment = LaunchActivity.getLastFragment();
                        LimitReachedBottomSheet.openBoostsForPostingStories(lastFragment, dialogId, canApplyBoost, boostsStatus, () -> {
                            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
                            presentFragment(StatisticActivity.create(chat));
                        });
                    });
                });
                return;
            }
            StoryRecorder recorder = StoryRecorder.getInstance(getParentActivity(), currentAccount)
                    .selectedPeerId(viewModel.getDialogId())
                    .canChangePeer(false)
                    .closeToWhenSent(new StoryRecorder.ClosingViewProvider() {
                        @Override
                        public void preLayout(long dialogId, Runnable runnable) {
                            avatarImage.setHasStories(needInsetForStories());
                            if (dialogId == viewModel.getDialogId()) {
                                collapseAvatarInstant();
                            }
                            AndroidUtilities.runOnUIThread(runnable, 30);
                        }

                        @Override
                        public StoryRecorder.SourceView getView(long dialogId) {
                            if (dialogId != viewModel.getDialogId()) {
                                return null;
                            }
                            updateAvatarRoundRadius();
                            return StoryRecorder.SourceView.fromAvatarImage(avatarImage, ChatObject.isForum(viewModel.getState().getValue().getCurrentChat()));
                        }
                    });
            recorder.open(StoryRecorder.SourceView.fromFloatingButton(floatingButtonContainer), true);
        });

        floatingButton = new RLottieImageView(context);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButtonContainer, View.TRANSLATION_Z, AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButtonContainer, View.TRANSLATION_Z, AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButtonContainer.setStateListAnimator(animator);
            floatingButtonContainer.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        floatingButtonContainer.addView(floatingButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        floatingButton.setAnimation(R.raw.write_contacts_fab_icon_camera, 56, 56);
        floatingButtonContainer.setContentDescription(LocaleController.getString(R.string.AccDescrCaptureStory));
        updateFloatingButtonColor();

        floatingHidden = true;
        floatingButtonHideProgress = 1.0f;
        updateFloatingButtonOffset();
    }

    private void collapseAvatarInstant() {
        if (viewModel.getState().getValue().isAllowPullingDown() && currentExpandAnimatorValue > 0) {
            layoutManager.scrollToPositionWithOffset(0, AndroidUtilities.dp(88) - listView.getPaddingTop());
            listView.post(() -> {
                needLayout(true);
                if (expandAnimator.isRunning()) {
                    expandAnimator.cancel();
                }
                setAvatarExpandProgress(1f);
            });
        }
    }

    private void updateFloatingButtonColor() {
        if (getParentActivity() == null) {
            return;
        }
        Drawable drawable;
        if (floatingButtonContainer != null) {
            drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), dontApplyPeerColor(Theme.getColor(Theme.key_chats_actionBackground), false), dontApplyPeerColor(Theme.getColor(Theme.key_chats_actionPressedBackground), false));
            if (Build.VERSION.SDK_INT < 21) {
                Drawable shadowDrawable = ContextCompat.getDrawable(getParentActivity(), R.drawable.floating_shadow).mutate();
                shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
                CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
                combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                drawable = combinedDrawable;
            }
            floatingButtonContainer.setBackground(drawable);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide || floatingButtonContainer == null || viewModel.getState().getValue().isWaitCanSendStoryRequest()) {
            return;
        }
        TLRPC.User bot = getMessagesController().getUser(viewModel.getArgs().userId);
        if (bot != null && bot.bot && bot.bot_can_edit && bot.bot_has_main_app) {
            StoriesController.BotPreviewsList list = (StoriesController.BotPreviewsList) getMessagesController().getStoriesController().getStoriesList(viewModel.getArgs().userId, StoriesController.StoriesList.TYPE_BOTS);
            ArrayList<StoriesController.UploadingStory> uploadingStories = getMessagesController().getStoriesController().getUploadingStories(viewModel.getArgs().userId);
            if (list != null && list.getCount() + (uploadingStories == null ? 0 : uploadingStories.size()) >= getMessagesController().botPreviewMediasMax) {
                hide = true;
            }
        }
        floatingHidden = hide;
        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(floatingButtonHideProgress, floatingHidden ? 1f : 0f);
        valueAnimator.addUpdateListener(animation -> {
            floatingButtonHideProgress = (float) animation.getAnimatedValue();
            updateFloatingButtonOffset();
        });
        animatorSet.playTogether(valueAnimator);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(floatingInterpolator);
        floatingButtonContainer.setClickable(!hide);
        animatorSet.start();
    }

    private void updateFloatingButtonOffset() {
        if (floatingButtonContainer != null) {
            floatingButtonContainer.setTranslationY(AndroidUtilities.dp(100) * floatingButtonHideProgress);
        }
    }

    private boolean expandAvatar() {
        if (!AndroidUtilities.isTablet() && !viewModel.getState().getValue().isInLandscapeMode() && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
            openingAvatar = true;
            viewModel.allowPullingDown(true);
            View child = null;
            for (int i = 0; i < listView.getChildCount(); i++) {
                if (listView.getChildAdapterPosition(listView.getChildAt(i)) == 0) {
                    child = listView.getChildAt(i);
                    break;
                }
            }
            if (child != null) {
                RecyclerView.ViewHolder holder = listView.findContainingViewHolder(child);
                if (holder != null) {
                    Integer offset = positionToOffset.get(holder.getAdapterPosition());
                    if (offset != null) {
                        listView.smoothScrollBy(0, -(offset + (listView.getPaddingTop() - child.getTop() - actionBar.getMeasuredHeight())), CubicBezierInterpolator.EASE_OUT_QUINT);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setAvatarExpandProgress(float animatedFracture) {
        final int newTop = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
        final float value = currentExpandAnimatorValue = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture = animatedFracture);
        checkPhotoDescriptionAlpha();
        avatarContainer.setScaleX(avatarScale);
        avatarContainer.setScaleY(avatarScale);
        avatarContainer.setTranslationX(AndroidUtilities.lerp(avatarX, 0f, value));
        avatarContainer.setTranslationY(AndroidUtilities.lerp((float) Math.ceil(avatarY), 0f, value));
        avatarImage.setRoundRadius((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, value));
        if (storyView != null) {
            storyView.setExpandProgress(value);
        }
        if (giftsView != null) {
            giftsView.setExpandProgress(value);
        }
        if (searchItem != null) {
            searchItem.setAlpha(1.0f - value);
            searchItem.setScaleY(1.0f - value);
            searchItem.setVisibility(View.VISIBLE);
            searchItem.setClickable(searchItem.getAlpha() > .5f);
            if (qrItem != null) {
                float translation = AndroidUtilities.dp(48) * value;
//                    if (searchItem.getVisibility() == View.VISIBLE)
//                        translation += AndroidUtilities.dp(48);
                qrItem.setTranslationX(translation);
                avatarsViewPagerIndicatorView.setTranslationX(translation - AndroidUtilities.dp(48));
            }
        }

        if (extraHeight > AndroidUtilities.dp(88f) && expandProgress < 0.33f) {
            refreshNameAndOnlineXY();
        }

        if (scamDrawable != null) {
            scamDrawable.setColor(ColorUtils.blendARGB(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), Color.argb(179, 255, 255, 255), value));
        }

        if (lockIconDrawable != null) {
            lockIconDrawable.setColorFilter(ColorUtils.blendARGB(getThemedColor(Theme.key_chat_lockIcon), Color.WHITE, value), PorterDuff.Mode.MULTIPLY);
        }

        if (verifiedCrossfadeDrawable[0] != null) {
            verifiedCrossfadeDrawable[0].setProgress(value);
        }
        if (verifiedCrossfadeDrawable[1] != null) {
            verifiedCrossfadeDrawable[1].setProgress(value);
        }

        if (premiumCrossfadeDrawable[0] != null) {
            premiumCrossfadeDrawable[0].setProgress(value);
        }
        if (premiumCrossfadeDrawable[1] != null) {
            premiumCrossfadeDrawable[1].setProgress(value);
        }

        updateEmojiStatusDrawableColor(value);

        final float k = AndroidUtilities.dpf2(8f);

        final float nameTextViewXEnd = AndroidUtilities.dpf2(18f) - nameTextView[1].getLeft();
        final float nameTextViewYEnd = newTop + extraHeight - AndroidUtilities.dpf2(38f) - nameTextView[1].getBottom();
        final float nameTextViewCx = k + nameX + (nameTextViewXEnd - nameX) / 2f;
        final float nameTextViewCy = k + nameY + (nameTextViewYEnd - nameY) / 2f;
        final float nameTextViewX = (1 - value) * (1 - value) * nameX + 2 * (1 - value) * value * nameTextViewCx + value * value * nameTextViewXEnd;
        final float nameTextViewY = (1 - value) * (1 - value) * nameY + 2 * (1 - value) * value * nameTextViewCy + value * value * nameTextViewYEnd;

        final float onlineTextViewXEnd = AndroidUtilities.dpf2(16f) - onlineTextView[1].getLeft();
        final float onlineTextViewYEnd = newTop + extraHeight - AndroidUtilities.dpf2(18f) - onlineTextView[1].getBottom();
        final float onlineTextViewCx = k + onlineX + (onlineTextViewXEnd - onlineX) / 2f;
        final float onlineTextViewCy = k + onlineY + (onlineTextViewYEnd - onlineY) / 2f;
        final float onlineTextViewX = (1 - value) * (1 - value) * onlineX + 2 * (1 - value) * value * onlineTextViewCx + value * value * onlineTextViewXEnd;
        final float onlineTextViewY = (1 - value) * (1 - value) * onlineY + 2 * (1 - value) * value * onlineTextViewCy + value * value * onlineTextViewYEnd;

        nameTextView[1].setTranslationX(nameTextViewX);
        nameTextView[1].setTranslationY(nameTextViewY);
        onlineTextView[1].setTranslationX(onlineTextViewX + customPhotoOffset);
        onlineTextView[1].setTranslationY(onlineTextViewY);
        mediaCounterTextView.setTranslationX(onlineTextViewX);
        mediaCounterTextView.setTranslationY(onlineTextViewY);
        final Object onlineTextViewTag = onlineTextView[1].getTag();
        int statusColor;
        boolean online = false;
        if (onlineTextViewTag instanceof Integer) {
            statusColor = getThemedColor((Integer) onlineTextViewTag);
            online = (Integer) onlineTextViewTag == Theme.key_profile_status;
        } else {
            statusColor = getThemedColor(Theme.key_avatar_subtitleInProfileBlue);
        }
        onlineTextView[1].setTextColor(ColorUtils.blendARGB(applyPeerColor(statusColor, true, online), 0xB3FFFFFF, value));
        if (extraHeight > AndroidUtilities.dp(88f)) {
            nameTextView[1].setPivotY(AndroidUtilities.lerp(0, nameTextView[1].getMeasuredHeight(), value));
            nameTextView[1].setScaleX(AndroidUtilities.lerp(1.12f, 1.67f, value));
            nameTextView[1].setScaleY(AndroidUtilities.lerp(1.12f, 1.67f, value));
        }
        if (showStatusButton != null) {
            showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }

        needLayoutText(Math.min(1f, extraHeight / AndroidUtilities.dp(88f)));

        nameTextView[1].setTextColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title), Color.WHITE, currentExpandAnimatorValue));
        actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), Color.WHITE, value), false);
        actionBar.setMenuOffsetSuppressed(true);

        avatarImage.setForegroundAlpha(value);

        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
        params.width = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(42f), listView.getMeasuredWidth() / avatarScale, value);
        params.height = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(42f), (extraHeight + newTop) / avatarScale, value);
        params.leftMargin = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(64f), 0f, value);
        avatarContainer.requestLayout();

        updateCollectibleHint();
    }

    private int getSmallAvatarRoundRadius() {
        if (viewModel.getArgs().chatId != 0) {
            TLRPC.Chat chatLocal = getMessagesController().getChat(viewModel.getArgs().chatId);
            if (ChatObject.isForum(chatLocal)) {
                return AndroidUtilities.dp(needInsetForStories() ? 11 : 16);
            }
        }
        return AndroidUtilities.dp(21);
    }

    // TODO : fix
    public boolean getInBubbleMode() {
        return inBubbleMode;
    }

    private void updateTtlIcon() {
        if (ttlIconView == null) {
            return;
        }
        boolean visible = false;
        if (viewModel.getState().getValue().getCurrentEncryptedChat() == null) {
            if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().ttl_period > 0) {
                visible = true;
            } else if (viewModel.getState().getValue().getChatInfo() != null && ChatObject.canUserDoAdminAction(viewModel.getState().getValue().getCurrentChat(), ChatObject.ACTION_DELETE_MESSAGES) && viewModel.getState().getValue().getChatInfo().ttl_period > 0) {
                visible = true;
            }
        }
        AndroidUtilities.updateViewVisibilityAnimated(ttlIconView, visible, 0.8f, fragmentOpened);
    }

    public void getEmojiStatusLocation(Rect rect) {
        if (nameTextView[1] == null) {
            return;
        }
        if (nameTextView[1].getRightDrawable() == null) {
            rect.set(nameTextView[1].getWidth() - 1, nameTextView[1].getHeight() / 2 - 1, nameTextView[1].getWidth() + 1, nameTextView[1].getHeight() / 2 + 1);
            return;
        }
        rect.set(nameTextView[1].getRightDrawable().getBounds());
        rect.offset((int) (rect.centerX() * (nameTextView[1].getScaleX() - 1f)), 0);
        rect.offset((int) nameTextView[1].getX(), (int) nameTextView[1].getY());
    }

    public void goToForum() {
        if (getParentLayout() != null && getParentLayout().getFragmentStack() != null) {
            for (int i = 0; i < getParentLayout().getFragmentStack().size(); ++i) {
                BaseFragment fragment = getParentLayout().getFragmentStack().get(i);
                if (fragment instanceof DialogsActivity) {
                    if (((DialogsActivity) fragment).rightSlidingDialogContainer != null) {
                        BaseFragment previewFragment = ((DialogsActivity) fragment).rightSlidingDialogContainer.getFragment();
                        if (previewFragment instanceof TopicsFragment && ((TopicsFragment) previewFragment).getDialogId() == viewModel.getDialogId()) {
                            ((DialogsActivity) fragment).rightSlidingDialogContainer.finishPreview();
                        }
                    }
                } else if (fragment instanceof ChatActivity) {
                    if (((ChatActivity) fragment).getDialogId() == viewModel.getDialogId()) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                } else if (fragment instanceof TopicsFragment) {
                    if (((TopicsFragment) fragment).getDialogId() == viewModel.getDialogId()) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                } else if (fragment instanceof ProfileActivity) {
                    if (fragment != this && ((ProfileActivity) fragment).viewModel.getDialogId() == viewModel.getDialogId() && ((ProfileActivity) fragment).viewModel.getArgs().isTopic) {
                        getParentLayout().removeFragmentFromStack(fragment);
                        i--;
                    }
                }
            }
        }

        playProfileAnimation = 0;

        Bundle args = new Bundle();
        args.putLong("chat_id", viewModel.getArgs().chatId);
        presentFragment(TopicsFragment.getTopicsOrChat(this, args));
    }

    private SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow selectAnimatedEmojiDialog;
    public void showStatusSelect() {
        if (selectAnimatedEmojiDialog != null) {
            return;
        }
        final SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[] popup = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[1];
        int xoff, yoff;
        getEmojiStatusLocation(AndroidUtilities.rectTmp2);
        int topMarginDp = nameTextView[1].getScaleX() < 1.5f ? 16 : 32;
        yoff = -(avatarContainer2.getHeight() - AndroidUtilities.rectTmp2.centerY()) - AndroidUtilities.dp(topMarginDp);
        int popupWidth = (int) Math.min(AndroidUtilities.dp(340 - 16), AndroidUtilities.displaySize.x * .95f);
        int ecenter = AndroidUtilities.rectTmp2.centerX();
        xoff = MathUtils.clamp(ecenter - popupWidth / 2, 0, AndroidUtilities.displaySize.x - popupWidth);
        ecenter -= xoff;
        SelectAnimatedEmojiDialog popupLayout = new SelectAnimatedEmojiDialog(this, getContext(), true, Math.max(0, ecenter), viewModel.getState().getValue().getCurrentChat() == null ? SelectAnimatedEmojiDialog.TYPE_EMOJI_STATUS : SelectAnimatedEmojiDialog.TYPE_EMOJI_STATUS_CHANNEL, true, resourcesProvider, topMarginDp) {
            @Override
            protected boolean willApplyEmoji(View view, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    return savedStarGift == null || MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) >= 2;
                }
                return true;
            }

            @Override
            public long getDialogId() {
                return ProfileActivity.this.viewModel.getDialogId();
            }

            @Override
            protected void onEmojiSelected(View emojiView, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                final TLRPC.EmojiStatus emojiStatus;
                if (gift != null) {
                    final TL_stars.SavedStarGift savedStarGift = StarsController.getInstance(currentAccount).findUserStarGift(gift.id);
                    if (savedStarGift != null && MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) < 2) {
                        MessagesController.getGlobalMainSettings().edit().putInt("statusgiftpage", MessagesController.getGlobalMainSettings().getInt("statusgiftpage", 0) + 1).apply();
                        new StarGiftSheet(getContext(), currentAccount, UserConfig.getInstance(currentAccount).getClientUserId(), resourcesProvider)
                            .set(savedStarGift, null)
                            .setupWearPage()
                            .show();
                        if (popup[0] != null) {
                            selectAnimatedEmojiDialog = null;
                            popup[0].dismiss();
                        }
                        return;
                    }
                    final TLRPC.TL_inputEmojiStatusCollectible status = new TLRPC.TL_inputEmojiStatusCollectible();
                    status.collectible_id = gift.id;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                } else if (documentId == null) {
                    emojiStatus = new TLRPC.TL_emojiStatusEmpty();
                } else {
                    final TLRPC.TL_emojiStatus status = new TLRPC.TL_emojiStatus();
                    status.document_id = documentId;
                    if (until != null) {
                        status.flags |= 1;
                        status.until = until;
                    }
                    emojiStatus = status;
                }
                emojiStatusGiftId = gift != null ? gift.id : null;
                getMessagesController().updateEmojiStatus(viewModel.getState().getValue().getCurrentChat() == null ? 0 : -viewModel.getState().getValue().getCurrentChat().id, emojiStatus, gift);
                for (int a = 0; a < 2; ++a) {
                    if (emojiStatusDrawable[a] != null) {
                        if (documentId == null && viewModel.getState().getValue().getCurrentChat() == null) {
                            emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), true);
                        } else if (documentId != null) {
                            emojiStatusDrawable[a].set(documentId, true);
                        } else {
                            emojiStatusDrawable[a].set((Drawable) null, true);
                        }
                        emojiStatusDrawable[a].setParticles(gift != null, true);
                    }
                }
                if (documentId != null) {
                    animatedStatusView.animateChange(ReactionsLayoutInBubble.VisibleReaction.fromCustomEmoji(documentId));
                }
                updateEmojiStatusDrawableColor();
                updateEmojiStatusEffectPosition();
                if (popup[0] != null) {
                    selectAnimatedEmojiDialog = null;
                    popup[0].dismiss();
                }
            }
        };
        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
        if (user != null) {
            popupLayout.setExpireDateHint(DialogObject.getEmojiStatusUntil(user.emoji_status));
        }
        if (emojiStatusGiftId != null) {
            popupLayout.setSelected(emojiStatusGiftId);
        } else {
            popupLayout.setSelected(emojiStatusDrawable[1] != null && emojiStatusDrawable[1].getDrawable() instanceof AnimatedEmojiDrawable ? ((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).getDocumentId() : null);
        }
        popupLayout.setSaveState(3);
        popupLayout.setScrimDrawable(emojiStatusDrawable[1], nameTextView[1]);
        popup[0] = selectAnimatedEmojiDialog = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                selectAnimatedEmojiDialog = null;
            }
        };
        int[] loc = new int[2];
        if (nameTextView[1] != null) {
            nameTextView[1].getLocationOnScreen(loc);
        }
        popup[0].showAsDropDown(fragmentView, xoff, yoff, Gravity.TOP | Gravity.LEFT);
        popup[0].dimBehind();
    }

    @Override
    public boolean isFragmentOpened() {
        return isFragmentOpened;
    }

    private void openAvatar() {
        if (listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
            return;
        }
        if (viewModel.getArgs().userId != 0) {
            TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user.photo != null && user.photo.photo_big != null) {
                PhotoViewer.getInstance().setParentActivity(ProfileActivity.this);
                if (user.photo.dc_id != 0) {
                    user.photo.photo_big.dc_id = user.photo.dc_id;
                }
                PhotoViewer.getInstance().openPhoto(user.photo.photo_big, provider);
            }
        } else if (viewModel.getArgs().chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
            if (chat.photo != null && chat.photo.photo_big != null) {
                PhotoViewer.getInstance().setParentActivity(ProfileActivity.this);
                if (chat.photo.dc_id != 0) {
                    chat.photo.photo_big.dc_id = chat.photo.dc_id;
                }
                ImageLocation videoLocation;
                if (viewModel.getState().getValue().getChatInfo() != null && (viewModel.getState().getValue().getChatInfo().chat_photo instanceof TLRPC.TL_photo) && !viewModel.getState().getValue().getChatInfo().chat_photo.video_sizes.isEmpty()) {
                    videoLocation = ImageLocation.getForPhoto(viewModel.getState().getValue().getChatInfo().chat_photo.video_sizes.get(0), viewModel.getState().getValue().getChatInfo().chat_photo);
                } else {
                    videoLocation = null;
                }
                PhotoViewer.getInstance().openPhotoWithVideo(chat.photo.photo_big, videoLocation, provider);
            }
        }
    }

    public void onWriteButtonClick() {
        if (viewModel.getArgs().userId != 0) {
            if (imageUpdater != null) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).getClientUserId());
                if (user == null) {
                    user = UserConfig.getInstance(currentAccount).getCurrentUser();
                }
                if (user == null) {
                    return;
                }
                imageUpdater.openMenu(user.photo != null && user.photo.photo_big != null && !(user.photo instanceof TLRPC.TL_userProfilePhotoEmpty), () -> {
                    MessagesController.getInstance(currentAccount).deleteUserPhoto(null);
                    cameraDrawable.setCurrentFrame(0);
                    cellCameraDrawable.setCurrentFrame(0);
                }, dialog -> {
                    if (!imageUpdater.isUploadingImage()) {
                        cameraDrawable.setCustomEndFrame(86);
                        cellCameraDrawable.setCustomEndFrame(86);
                        writeButton.playAnimation();
                        if (setAvatarCell != null) {
                            setAvatarCell.getImageView().playAnimation();
                        }
                    } else {
                        cameraDrawable.setCurrentFrame(0, false);
                        cellCameraDrawable.setCurrentFrame(0, false);
                    }
                }, 0);
                cameraDrawable.setCurrentFrame(0);
                cameraDrawable.setCustomEndFrame(43);
                cellCameraDrawable.setCurrentFrame(0);
                cellCameraDrawable.setCustomEndFrame(43);
                writeButton.playAnimation();
                if (setAvatarCell != null) {
                    setAvatarCell.getImageView().playAnimation();
                }
            } else {
                if (playProfileAnimation != 0 && parentLayout != null && parentLayout.getFragmentStack() != null && parentLayout.getFragmentStack().size() >= 2 && parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2) instanceof ChatActivity) {
                    finishFragment();
                } else {
                    TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                    if (user == null || user instanceof TLRPC.TL_userEmpty) {
                        return;
                    }
                    Bundle args = new Bundle();
                    args.putLong("user_id", viewModel.getArgs().userId);
                    if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
                        return;
                    }
                    boolean removeFragment = arguments.getBoolean("removeFragmentOnChatOpen", true);
                    if (!AndroidUtilities.isTablet() && removeFragment) {
                        getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                    }
                    int distance = getArguments().getInt("nearby_distance", -1);
                    if (distance >= 0) {
                        args.putInt("nearby_distance", distance);
                    }
                    ChatActivity chatActivity = new ChatActivity(args);
                    chatActivity.setPreloadedSticker(getMediaDataController().getGreetingsSticker(), false);
                    presentFragment(chatActivity, removeFragment);
                    if (AndroidUtilities.isTablet()) {
                        finishFragment();
                    }
                }
            }
        } else {
            openDiscussion();
        }
    }

    public void openDiscussion() {
        if (viewModel.getState().getValue().getChatInfo() == null || viewModel.getState().getValue().getChatInfo().linked_chat_id == 0) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("chat_id", viewModel.getState().getValue().getChatInfo().linked_chat_id);
        if (!getMessagesController().checkCanOpenChat(args, ProfileActivity.this)) {
            return;
        }
        presentFragment(new ChatActivity(args));
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, View view) {
        return onMemberClick(participant, isLong, false, view);
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, boolean resultOnly) {
        return onMemberClick(participant, isLong, resultOnly, null);
    }

    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean isLong, boolean resultOnly, View view) {
        if (getParentActivity() == null) {
            return false;
        }
        if (isLong) {
            TLRPC.User user = getMessagesController().getUser(participant.user_id);
            if (user == null || participant.user_id == getUserConfig().getClientUserId()) {
                return false;
            }
            selectedUser = participant.user_id;
            boolean allowKick;
            boolean canEditAdmin;
            boolean canRestrict;
            boolean editingAdmin;
            final TLRPC.ChannelParticipant channelParticipant;

            if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat())) {
                channelParticipant = ((TLRPC.TL_chatChannelParticipant) participant).channelParticipant;
                TLRPC.User u = getMessagesController().getUser(participant.user_id);
                canEditAdmin = ChatObject.canAddAdmins(viewModel.getState().getValue().getCurrentChat());
                if (canEditAdmin && (channelParticipant instanceof TLRPC.TL_channelParticipantCreator || channelParticipant instanceof TLRPC.TL_channelParticipantAdmin && !channelParticipant.can_edit)) {
                    canEditAdmin = false;
                }
                allowKick = canRestrict = ChatObject.canBlockUsers(viewModel.getState().getValue().getCurrentChat()) && (!(channelParticipant instanceof TLRPC.TL_channelParticipantAdmin || channelParticipant instanceof TLRPC.TL_channelParticipantCreator) || channelParticipant.can_edit);
                if (viewModel.getState().getValue().getCurrentChat().gigagroup) {
                    canRestrict = false;
                }
                editingAdmin = channelParticipant instanceof TLRPC.TL_channelParticipantAdmin;
            } else {
                channelParticipant = null;
                allowKick = viewModel.getState().getValue().getCurrentChat().creator || participant instanceof TLRPC.TL_chatParticipant && (ChatObject.canBlockUsers(viewModel.getState().getValue().getCurrentChat()) || participant.inviter_id == getUserConfig().getClientUserId());
                canEditAdmin = viewModel.getState().getValue().getCurrentChat().creator;
                canRestrict = viewModel.getState().getValue().getCurrentChat().creator;
                editingAdmin = participant instanceof TLRPC.TL_chatParticipantAdmin;
            }

            boolean result = (canEditAdmin || canRestrict || allowKick);
            if (resultOnly || !result) {
                return result;
            }

            Utilities.Callback<Integer> openRightsEdit = action -> {
                if (channelParticipant != null) {
                    openRightsEdit(action, user, participant, channelParticipant.admin_rights, channelParticipant.banned_rights, channelParticipant.rank, editingAdmin);
                } else {
                    openRightsEdit(action, user, participant, null, null, "", editingAdmin);
                }
            };

            ItemOptions.makeOptions(this, view)
                .setScrimViewBackground(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundWhite)))
                .addIf(canEditAdmin, R.drawable.msg_admins, editingAdmin ? LocaleController.getString(R.string.EditAdminRights) : LocaleController.getString(R.string.SetAsAdmin), () -> openRightsEdit.run(0))
                .addIf(canRestrict, R.drawable.msg_permissions, LocaleController.getString(R.string.ChangePermissions), () -> {
                    if (channelParticipant instanceof TLRPC.TL_channelParticipantAdmin || participant instanceof TLRPC.TL_chatParticipantAdmin) {
                        showDialog(
                            new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                                .setTitle(LocaleController.getString(R.string.AppName))
                                .setMessage(formatString("AdminWillBeRemoved", R.string.AdminWillBeRemoved, ContactsController.formatName(user.first_name, user.last_name)))
                                .setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> openRightsEdit.run(1))
                                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                                .create()
                        );
                    } else {
                        openRightsEdit.run(1);
                    }
                })
                .addIf(allowKick, R.drawable.msg_remove, LocaleController.getString(R.string.KickFromGroup), true, () -> {
                    kickUser(selectedUser, participant);
                })
                .setMinWidth(190)
                .show();
        } else {
            if (participant.user_id == getUserConfig().getClientUserId()) {
                return false;
            }
            Bundle args = new Bundle();
            args.putLong("user_id", participant.user_id);
            args.putBoolean("preload_messages", true);
            presentFragment(new ProfileActivity(args));
        }
        return true;
    }

    @Override
    public TLRPC.Chat getCurrentChat() {
        return viewModel.getState().getValue().getCurrentChat();
    }

    private void openRightsEdit(int action, TLRPC.User user, TLRPC.ChatParticipant participant, TLRPC.TL_chatAdminRights adminRights, TLRPC.TL_chatBannedRights bannedRights, String rank, boolean editingAdmin) {
        boolean[] needShowBulletin = new boolean[1];
        ChatRightsEditActivity fragment = new ChatRightsEditActivity(user.id, viewModel.getArgs().chatId, adminRights, viewModel.getState().getValue().getCurrentChat().default_banned_rights, bannedRights, rank, action, true, false, null) {
            @Override
            public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
                if (!isOpen && backward && needShowBulletin[0] && BulletinFactory.canShowBulletin(ProfileActivity.this)) {
                    BulletinFactory.createPromoteToAdminBulletin(ProfileActivity.this, user.first_name).show();
                }
            }
        };
        fragment.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
            @Override
            public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                if (action == 0) {
                    if (participant instanceof TLRPC.TL_chatChannelParticipant) {
                        TLRPC.TL_chatChannelParticipant channelParticipant1 = ((TLRPC.TL_chatChannelParticipant) participant);
                        if (rights == 1) {
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipantAdmin();
                            channelParticipant1.channelParticipant.flags |= 4;
                        } else {
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipant();
                        }
                        channelParticipant1.channelParticipant.inviter_id = getUserConfig().getClientUserId();
                        channelParticipant1.channelParticipant.peer = new TLRPC.TL_peerUser();
                        channelParticipant1.channelParticipant.peer.user_id = participant.user_id;
                        channelParticipant1.channelParticipant.date = participant.date;
                        channelParticipant1.channelParticipant.banned_rights = rightsBanned;
                        channelParticipant1.channelParticipant.admin_rights = rightsAdmin;
                        channelParticipant1.channelParticipant.rank = rank;
                    } else if (participant != null) {
                        TLRPC.ChatParticipant newParticipant;
                        if (rights == 1) {
                            newParticipant = new TLRPC.TL_chatParticipantAdmin();
                        } else {
                            newParticipant = new TLRPC.TL_chatParticipant();
                        }
                        newParticipant.user_id = participant.user_id;
                        newParticipant.date = participant.date;
                        newParticipant.inviter_id = participant.inviter_id;
                        int index = viewModel.getState().getValue().getChatInfo().participants.participants.indexOf(participant);
                        if (index >= 0) {
                            viewModel.getState().getValue().getChatInfo().participants.participants.set(index, newParticipant);
                        }
                    }
                    if (rights == 1 && !editingAdmin) {
                        needShowBulletin[0] = true;
                    }
                } else if (action == 1) {
                    if (rights == 0) {
                        if (viewModel.getState().getValue().isCurrentChatMagagroup() && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null) {
                            boolean changed = false;
                            for (int a = 0; a < viewModel.getState().getValue().getChatInfo().participants.participants.size(); a++) {
                                TLRPC.ChannelParticipant p = ((TLRPC.TL_chatChannelParticipant) viewModel.getState().getValue().getChatInfo().participants.participants.get(a)).channelParticipant;
                                if (MessageObject.getPeerId(p.peer) == participant.user_id) {
                                    viewModel.getState().getValue().getChatInfo().participants_count--;
                                    viewModel.getState().getValue().getChatInfo().participants.participants.remove(a);
                                    changed = true;
                                    break;
                                }
                            }
                            if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null) {
                                for (int a = 0; a < viewModel.getState().getValue().getChatInfo().participants.participants.size(); a++) {
                                    TLRPC.ChatParticipant p = viewModel.getState().getValue().getChatInfo().participants.participants.get(a);
                                    if (p.user_id == participant.user_id) {
                                        viewModel.getState().getValue().getChatInfo().participants.participants.remove(a);
                                        changed = true;
                                        break;
                                    }
                                }
                            }
                            if (changed) {
                                viewModel.updateOnlineCount(true);
                                updateRowsIds();
                                listAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }

            @Override
            public void didChangeOwner(TLRPC.User user) {
                undoView.showWithAction(-viewModel.getArgs().chatId, viewModel.getState().getValue().isCurrentChatMagagroup() ? UndoView.ACTION_OWNER_TRANSFERED_GROUP : UndoView.ACTION_OWNER_TRANSFERED_CHANNEL, user);
            }
        });
        presentFragment(fragment);
    }

    private boolean processOnClickOrPress(final int position, final View view, final float x, final float y) {
        if (position == usernameRow || position == setUsernameRow) {
            final String username;
            final TLRPC.TL_username usernameObj;
            if (viewModel.getArgs().userId != 0) {
                final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                String username1 = UserObject.getPublicUsername(user);
                if (user == null || username1 == null) {
                    return false;
                }
                username = username1;
                usernameObj = DialogObject.findUsername(username, user);
            } else if (viewModel.getArgs().chatId != 0) {
                final TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
                if (chat == null || viewModel.getArgs().topicId == 0 && !ChatObject.isPublic(chat)) {
                    return false;
                }
                username = ChatObject.getPublicUsername(chat);
                usernameObj = DialogObject.findUsername(username, chat);
            } else {
                return false;
            }
            if (viewModel.getArgs().userId == 0) {
                TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
                String link;
                if (ChatObject.isPublic(chat)) {
                    link = "https://" + getMessagesController().linkPrefix + "/" + ChatObject.getPublicUsername(chat) + (viewModel.getArgs().topicId != 0 ? "/" + viewModel.getArgs().topicId : "");
                } else {
                    link = "https://" + getMessagesController().linkPrefix + "/c/" + chat.id + (viewModel.getArgs().topicId != 0 ? "/" + viewModel.getArgs().topicId : "");
                }
                ShareAlert shareAlert = new ShareAlert(getParentActivity(), null, link, false, link, false) {
                    @Override
                    protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic, boolean showToast) {
                        if (!showToast) return;
                        AndroidUtilities.runOnUIThread(() -> {
                            BulletinFactory.createInviteSentBulletin(getParentActivity(), (FrameLayout) fragmentView, dids.size(), dids.size() == 1 ? dids.valueAt(0).id : 0, count, getThemedColor(Theme.key_undo_background), getThemedColor(Theme.key_undo_infoColor)).show();
                        }, 250);
                    }
                };
                showDialog(shareAlert);
                if (usernameObj != null && !usernameObj.editable) {
                    TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                    TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                    input.username = usernameObj.username;
                    req.collectible = input;
                    int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TL_fragment.TL_collectibleInfo) {
                            TL_fragment.TL_collectibleInfo info = (TL_fragment.TL_collectibleInfo) res;
                            TLObject obj;
                            if (viewModel.getArgs().userId != 0) {
                                obj = getMessagesController().getUser(viewModel.getArgs().userId);
                            } else {
                                obj = getMessagesController().getChat(viewModel.getArgs().chatId);
                            }
                            final String usernameStr = "@" + usernameObj.username;
                            final String date = LocaleController.getInstance().getFormatterBoostExpired().format(new Date(info.purchase_date * 1000L));
                            final String cryptoAmount = BillingController.getInstance().formatCurrency(info.crypto_amount, info.crypto_currency);
                            final String amount = BillingController.getInstance().formatCurrency(info.amount, info.currency);
                            BulletinFactory.of(shareAlert.bulletinContainer2, resourcesProvider)
                                .createImageBulletin(
                                    R.drawable.filled_username,
                                    AndroidUtilities.withLearnMore(AndroidUtilities.replaceTags(formatString(R.string.FragmentChannelUsername, usernameStr, date, cryptoAmount, TextUtils.isEmpty(amount) ? "" : "("+amount+")")), () -> {
                                        Bulletin.hideVisible();
                                        Browser.openUrl(getContext(), info.url);
                                    })
                                )
                                .setOnClickListener(v -> {
                                    Bulletin.hideVisible();
                                    Browser.openUrl(getContext(), info.url);
                                })
                                .show(false);
                        } else {
                            BulletinFactory.showError(err);
                        }
                    }));
                    getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                }
            } else {
                if (editRow(view, position)) return true;

                if (usernameObj != null && !usernameObj.editable) {
                    TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                    TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                    input.username = usernameObj.username;
                    req.collectible = input;
                    int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TL_fragment.TL_collectibleInfo) {
                            TLObject obj;
                            if (viewModel.getArgs().userId != 0) {
                                obj = getMessagesController().getUser(viewModel.getArgs().userId);
                            } else {
                                obj = getMessagesController().getChat(viewModel.getArgs().chatId);
                            }
                            FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_USERNAME, usernameObj.username, obj, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                        } else {
                            BulletinFactory.showError(err);
                        }
                    }));
                    getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                    return true;
                }

                try {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    String text = "@" + username;
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.UsernameCopied), resourcesProvider).show();
                    android.content.ClipData clip = android.content.ClipData.newPlainText("label", text);
                    clipboard.setPrimaryClip(clip);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return true;
        } else if (position == phoneRow || position == numberRow) {
            if (editRow(view, position)) return true;

            final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user == null || user.phone == null || user.phone.length() == 0 || getParentActivity() == null) {
                return false;
            }

            if (position == phoneRow && user.phone.startsWith("888")) {
                TL_fragment.TL_inputCollectiblePhone input = new TL_fragment.TL_inputCollectiblePhone();
                final String phone = input.phone = user.phone;
                TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                req.collectible = input;
                int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                    if (res instanceof TL_fragment.TL_collectibleInfo) {
                        FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_PHONE, phone, user, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                    } else {
                        BulletinFactory.showError(err);
                    }
                }));
                getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                return true;
            }

            ArrayList<CharSequence> items = new ArrayList<>();
            ArrayList<Integer> actions = new ArrayList<>();
            List<Integer> icons = new ArrayList<>();
            if (position == phoneRow) {
                if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().phone_calls_available) {
                    icons.add(R.drawable.msg_calls);
                    items.add(LocaleController.getString(R.string.CallViaTelegram));
                    actions.add(PHONE_OPTION_TELEGRAM_CALL);
                    if (Build.VERSION.SDK_INT >= 18 && viewModel.getState().getValue().getUserInfo().video_calls_available) {
                        icons.add(R.drawable.msg_videocall);
                        items.add(LocaleController.getString(R.string.VideoCallViaTelegram));
                        actions.add(PHONE_OPTION_TELEGRAM_VIDEO_CALL);
                    }
                }
                if (!isFragmentPhoneNumber) {
                    icons.add(R.drawable.msg_calls_regular);
                    items.add(LocaleController.getString(R.string.Call));
                    actions.add(PHONE_OPTION_CALL);
                }
            }
            icons.add(R.drawable.msg_copy);
            items.add(LocaleController.getString(R.string.Copy));
            actions.add(PHONE_OPTION_COPY);

            AtomicReference<ActionBarPopupWindow> popupWindowRef = new AtomicReference<>();
            ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext(), R.drawable.popup_fixed_alert, resourcesProvider) {
                Path path = new Path();

                @Override
                protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                    canvas.save();
                    path.rewind();
                    AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                    path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                    canvas.clipPath(path);
                    boolean draw = super.drawChild(canvas, child, drawingTime);
                    canvas.restore();
                    return draw;
                }
            };
            popupLayout.setFitItems(true);

            for (int i = 0; i < icons.size(); i++) {
                int action = actions.get(i);
                ActionBarMenuItem.addItem(popupLayout, icons.get(i), items.get(i), false, resourcesProvider).setOnClickListener(v -> {
                    popupWindowRef.get().dismiss();
                    switch (action) {
                        case PHONE_OPTION_CALL:
                            try {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + user.phone));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getParentActivity().startActivityForResult(intent, 500);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            break;
                        case PHONE_OPTION_COPY:
                            try {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("label", "+" + user.phone);
                                clipboard.setPrimaryClip(clip);
                                if (AndroidUtilities.shouldShowClipboardToast()) {
                                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.PhoneCopied)).show();
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            break;
                        case PHONE_OPTION_TELEGRAM_CALL:
                        case PHONE_OPTION_TELEGRAM_VIDEO_CALL:
                            if (getParentActivity() == null) {
                                return;
                            }
                            VoIPHelper.startCall(user, action == PHONE_OPTION_TELEGRAM_VIDEO_CALL, viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().video_calls_available, getParentActivity(), viewModel.getState().getValue().getUserInfo(), getAccountInstance());
                            break;
                    }
                });
            }
            if (isFragmentPhoneNumber) {
                FrameLayout gap = new FrameLayout(getContext());
                gap.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider));
                popupLayout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));

                TextView fragmentInfoView = new TextView(getContext());
                fragmentInfoView.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(8), AndroidUtilities.dp(13), AndroidUtilities.dp(8));
                fragmentInfoView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                fragmentInfoView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider));
                fragmentInfoView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText, resourcesProvider));
                fragmentInfoView.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider), 0,6));

                SpannableStringBuilder spanned = new SpannableStringBuilder(AndroidUtilities.replaceTags(LocaleController.getString(R.string.AnonymousNumberNotice)));

                int startIndex = TextUtils.indexOf(spanned, '*');
                int lastIndex = TextUtils.lastIndexOf(spanned, '*');
                if (startIndex != -1 && lastIndex != -1 && startIndex != lastIndex) {
                    spanned.replace(lastIndex, lastIndex + 1, "");
                    spanned.replace(startIndex, startIndex + 1, "");
                    spanned.setSpan(new TypefaceSpan(AndroidUtilities.bold()), startIndex, lastIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spanned.setSpan(new ForegroundColorSpan(fragmentInfoView.getLinkTextColors().getDefaultColor()), startIndex, lastIndex - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                fragmentInfoView.setText(spanned);
                fragmentInfoView.setOnClickListener(v -> {
                    try {
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://fragment.com")));
                    } catch (ActivityNotFoundException e) {
                        FileLog.e(e);
                    }
                });

                gap.setTag(R.id.fit_width_tag, 1);
                fragmentInfoView.setTag(R.id.fit_width_tag, 1);
                popupLayout.addView(fragmentInfoView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            }

            ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            popupWindow.setPauseNotifications(true);
            popupWindow.setDismissAnimationDuration(220);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
            popupWindow.setFocusable(true);
            popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.getContentView().setFocusableInTouchMode(true);
            popupWindowRef.set(popupWindow);

            float px = x, py = y;
            View v = view;
            while (v != getFragmentView() && v != null) {
                px += v.getX();
                py += v.getY();
                v = (View) v.getParent();
            }
            if (AndroidUtilities.isTablet()) {
                View pv = parentLayout.getView();
                if (pv != null) {
                    px += pv.getX() + pv.getPaddingLeft();
                    py += pv.getY() + pv.getPaddingTop();
                }
            }
            px -= popupLayout.getMeasuredWidth() / 2f;
            popupWindow.showAtLocation(getFragmentView(), 0, (int) px, (int) py);
            popupWindow.dimBehind();
            return true;
        } else if (position == channelInfoRow || position == userInfoRow || position == locationRow || position == bioRow) {
            if (position == bioRow && (viewModel.getState().getValue().getUserInfo() == null || TextUtils.isEmpty(viewModel.getState().getValue().getUserInfo().about))) {
                return false;
            }
            if (editRow(view, position)) return true;
            if (view instanceof AboutLinkCell && ((AboutLinkCell) view).onClick()) {
                return false;
            }
            String text;
            if (position == locationRow) {
                text = viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().location instanceof TLRPC.TL_channelLocation ? ((TLRPC.TL_channelLocation) viewModel.getState().getValue().getChatInfo().location).address : null;
            } else if (position == channelInfoRow) {
                text = viewModel.getState().getValue().getChatInfo() != null ? viewModel.getState().getValue().getChatInfo().about : null;
            } else {
                text = viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().about : null;
            }
            final String finalText = text;
            if (TextUtils.isEmpty(finalText)) {
                return false;
            }
            final String[] fromLanguage = new String[1];
            fromLanguage[0] = "und";
            final boolean translateButtonEnabled = MessagesController.getInstance(currentAccount).getTranslateController().isContextTranslateEnabled();
            final boolean[] withTranslate = new boolean[1];
            withTranslate[0] = position == bioRow || position == channelInfoRow || position == userInfoRow;
            final String toLang = LocaleController.getInstance().getCurrentLocale().getLanguage();
            Runnable showMenu = () -> {
                if (getParentActivity() == null) {
                    return;
                }
                CharSequence[] items = withTranslate[0] ? new CharSequence[]{LocaleController.getString(R.string.Copy), LocaleController.getString(R.string.TranslateMessage)} : new CharSequence[]{LocaleController.getString(R.string.Copy)};
                int[] icons = withTranslate[0] ? new int[] {R.drawable.msg_copy, R.drawable.msg_translate} : new int[] {R.drawable.msg_copy};

                AtomicReference<ActionBarPopupWindow> popupWindowRef = new AtomicReference<>();
                ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext(), R.drawable.popup_fixed_alert, resourcesProvider) {
                    Path path = new Path();

                    @Override
                    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                        canvas.save();
                        path.rewind();
                        AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                        path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                        canvas.clipPath(path);
                        boolean draw = super.drawChild(canvas, child, drawingTime);
                        canvas.restore();
                        return draw;
                    }
                };
                popupLayout.setFitItems(true);

                for (int i = 0; i < icons.length; i++) {
                    int j = i;
                    ActionBarMenuItem.addItem(popupLayout, icons[i], items[i], false, resourcesProvider).setOnClickListener(v -> {
                        popupWindowRef.get().dismiss();
                        try {
                            if (j == 0) {
                                AndroidUtilities.addToClipboard(finalText);
                                if (position == bioRow) {
                                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BioCopied)).show();
                                } else {
                                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                                }
                            } else if (j == 1) {
                                TranslateAlert2.showAlert(fragmentView.getContext(), this, currentAccount, fromLanguage[0], toLang, finalText, null, false, span -> {
                                    if (span != null) {
                                        openUrl(span.getURL(), null);
                                        return true;
                                    }
                                    return false;
                                }, null);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    });
                }

                ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
                popupWindow.setPauseNotifications(true);
                popupWindow.setDismissAnimationDuration(220);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setClippingEnabled(true);
                popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
                popupWindow.setFocusable(true);
                popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
                popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
                popupWindow.getContentView().setFocusableInTouchMode(true);
                popupWindowRef.set(popupWindow);

                float px = x, py = y;
                View v = view;
                while (v != null && v != getFragmentView()) {
                    px += v.getX();
                    py += v.getY();
                    v = (View) v.getParent();
                }
                if (AndroidUtilities.isTablet()) {
                    View pv = parentLayout.getView();
                    if (pv != null) {
                        px += pv.getX() + pv.getPaddingLeft();
                        py += pv.getY() + pv.getPaddingTop();
                    }
                }
                px -= popupLayout.getMeasuredWidth() / 2f;
                popupWindow.showAtLocation(getFragmentView(), 0, (int) px, (int) py);
                popupWindow.dimBehind();
            };
            if (withTranslate[0]) {
                if (LanguageDetector.hasSupport()) {
                    LanguageDetector.detectLanguage(finalText, (fromLang) -> {
                        fromLanguage[0] = fromLang;
                        withTranslate[0] = fromLang != null && (!fromLang.equals(toLang) || fromLang.equals("und")) && (
                            translateButtonEnabled && !RestrictedLanguagesSelectActivity.getRestrictedLanguages().contains(fromLang) ||
                            (viewModel.getState().getValue().getCurrentChat() != null && (viewModel.getState().getValue().getCurrentChat().has_link || ChatObject.isPublic(viewModel.getState().getValue().getCurrentChat()))) && ("uk".equals(fromLang) || "ru".equals(fromLang)));
                        showMenu.run();
                    }, (error) -> {
                        FileLog.e("mlkit: failed to detect language in selection", error);
                        showMenu.run();
                    });
                } else {
                    showMenu.run();
                }
            } else {
                showMenu.run();
            }
            return true;
        } else if (position == bizHoursRow || position == bizLocationRow) {
            if (getParentActivity() == null || viewModel.getState().getValue().getUserInfo() == null) {
                return false;
            }
            final String finalText;
            if (position == bizHoursRow) {
                if (viewModel.getState().getValue().getUserInfo().business_work_hours == null) return false;
                finalText = OpeningHoursActivity.toString(currentAccount, viewModel.getState().getValue().getUserInfo().user, viewModel.getState().getValue().getUserInfo().business_work_hours);
            } else if (position == bizLocationRow) {
                if (editRow(view, position)) return true;
                if (viewModel.getState().getValue().getUserInfo().business_location == null) return false;
                finalText = viewModel.getState().getValue().getUserInfo().business_location.address;
            } else return true;

            AtomicReference<ActionBarPopupWindow> popupWindowRef = new AtomicReference<>();
            ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext(), R.drawable.popup_fixed_alert, resourcesProvider) {
                Path path = new Path();

                @Override
                protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                    canvas.save();
                    path.rewind();
                    AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                    path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                    canvas.clipPath(path);
                    boolean draw = super.drawChild(canvas, child, drawingTime);
                    canvas.restore();
                    return draw;
                }
            };
            popupLayout.setFitItems(true);

            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, LocaleController.getString(R.string.Copy), false, resourcesProvider).setOnClickListener(v -> {
                popupWindowRef.get().dismiss();
                try {
                    AndroidUtilities.addToClipboard(finalText);
                    if (position == bizHoursRow) {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BusinessHoursCopied)).show();
                    } else {
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.BusinessLocationCopied)).show();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });

            ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            popupWindow.setPauseNotifications(true);
            popupWindow.setDismissAnimationDuration(220);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
            popupWindow.setFocusable(true);
            popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.getContentView().setFocusableInTouchMode(true);
            popupWindowRef.set(popupWindow);

            float px = x, py = y;
            View v = view;
            while (v != null && v != getFragmentView()) {
                px += v.getX();
                py += v.getY();
                v = (View) v.getParent();
            }
            if (AndroidUtilities.isTablet()) {
                View pv = parentLayout.getView();
                if (pv != null) {
                    px += pv.getX() + pv.getPaddingLeft();
                    py += pv.getY() + pv.getPaddingTop();
                }
            }
            px -= popupLayout.getMeasuredWidth() / 2f;
            popupWindow.showAtLocation(getFragmentView(), 0, (int) px, (int) py);
            popupWindow.dimBehind();
            return true;
        }
        return false;
    }

    public void leaveChatPressed() {
        boolean isForum = ChatObject.isForum(viewModel.getState().getValue().getCurrentChat());
        AlertsCreator.createClearOrDeleteDialogAlert(ProfileActivity.this, false, viewModel.getState().getValue().getCurrentChat(), null, false, isForum, !isForum, (param) -> {
            playProfileAnimation = 0;
            getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            finishFragment();
            getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, -viewModel.getState().getValue().getCurrentChat().id, null, viewModel.getState().getValue().getCurrentChat(), param);
        }, resourcesProvider);
    }

    private AnimatorSet headerAnimatorSet;
    private AnimatorSet headerShadowAnimatorSet;
    @Getter private float mediaHeaderAnimationProgress;
    private boolean mediaHeaderVisible;
    private Property<ActionBar, Float> ACTIONBAR_HEADER_PROGRESS = new AnimationProperties.FloatProperty<ActionBar>("avatarAnimationProgress") {
        @Override
        public void setValue(ActionBar object, float value) {
            mediaHeaderAnimationProgress = value;
            if (storyView != null) {
                storyView.setActionBarActionMode(value);
            }
            if (giftsView != null) {
                giftsView.setActionBarActionMode(value);
            }
            topView.invalidate();

            int color1 = getThemedColor(Theme.key_profile_title);
            int color2 = getThemedColor(Theme.key_player_actionBarTitle);
            int c = AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f);
            nameTextView[1].setTextColor(c);
            if (lockIconDrawable != null) {
                lockIconDrawable.setColorFilter(c, PorterDuff.Mode.MULTIPLY);
            }
            if (scamDrawable != null) {
                color1 = getThemedColor(Theme.key_avatar_subtitleInProfileBlue);
                scamDrawable.setColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f));
            }

            color1 = peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon);
            color2 = getThemedColor(Theme.key_actionBarActionModeDefaultIcon);
            actionBar.setItemsColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), false);

            color1 = peerColor != null ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue);
            color2 = getThemedColor(Theme.key_actionBarActionModeDefaultSelector);
            actionBar.setItemsBackgroundColor(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), false);

            topView.invalidate();
            otherItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            callItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            videoCallItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));
            editItem.setIconColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon));

            if (verifiedDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                verifiedDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (verifiedDrawable[1] != null) {
                color1 = peerColor != null ? Theme.adaptHSV(ColorUtils.blendARGB(peerColor.getColor2(), peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), .4f), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f) : getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }

            if (verifiedCheckDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedCheck);
                color2 = getThemedColor(Theme.key_windowBackgroundWhite);
                verifiedCheckDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (verifiedCheckDrawable[1] != null) {
                color1 = peerColor != null ? Color.WHITE : dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedCheck));
                color2 = getThemedColor(Theme.key_windowBackgroundWhite);
                verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }


            if (premiumStarDrawable[0] != null) {
                color1 = getThemedColor(Theme.key_profile_verifiedBackground);
                color2 = getThemedColor(Theme.key_player_actionBarTitle);
                premiumStarDrawable[0].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            if (premiumStarDrawable[1] != null) {
                color1 = dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedBackground));
                color2 = dontApplyPeerColor(getThemedColor(Theme.key_player_actionBarTitle));
                premiumStarDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, value, 1.0f), PorterDuff.Mode.MULTIPLY);
            }

            updateEmojiStatusDrawableColor();

            if (avatarsViewPagerIndicatorView.getSecondaryMenuItem() != null && (videoCallItemVisible || editItemVisible || callItemVisible)) {
                needLayoutText(Math.min(1f, extraHeight / AndroidUtilities.dp(88f)));
            }
        }

        @Override
        public Float get(ActionBar object) {
            return mediaHeaderAnimationProgress;
        }
    };

    private void setMediaHeaderVisible(boolean visible) {
        if (mediaHeaderVisible == visible) {
            return;
        }
        mediaHeaderVisible = visible;
        if (headerAnimatorSet != null) {
            headerAnimatorSet.cancel();
        }
        if (headerShadowAnimatorSet != null) {
            headerShadowAnimatorSet.cancel();
        }
        ActionBarMenuItem mediaSearchItem = sharedMediaLayout.getSearchItem();
        ImageView mediaOptionsItem = sharedMediaLayout.getSearchOptionsItem();
        TextView saveItem = sharedMediaLayout.getSaveItem();
        if (!mediaHeaderVisible) {
            if (callItemVisible) {
                callItem.setVisibility(View.VISIBLE);
            }
            if (videoCallItemVisible) {
                videoCallItem.setVisibility(View.VISIBLE);
            }
            if (editItemVisible) {
                editItem.setVisibility(View.VISIBLE);
            }
            otherItem.setVisibility(View.VISIBLE);
            if (mediaOptionsItem != null) {
                mediaOptionsItem.setVisibility(View.GONE);
            }
            if (saveItem != null) {
                saveItem.setVisibility(View.GONE);
            }
        } else {
            if (sharedMediaLayout.isSearchItemVisible()) {
                mediaSearchItem.setVisibility(View.VISIBLE);
            }
            if (mediaOptionsItem != null) {
                mediaOptionsItem.setVisibility(View.VISIBLE);
            }
            if (sharedMediaLayout.isOptionsItemVisible()) {
                sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.VISIBLE);
                sharedMediaLayout.animateSearchToOptions(true, false);
            } else {
                sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.INVISIBLE);
                sharedMediaLayout.animateSearchToOptions(false, false);
            }
        }
        updateStoriesViewBounds(false);

        if (actionBar != null) {
            actionBar.createMenu().requestLayout();
        }

        ArrayList<Animator> animators = new ArrayList<>();

        animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(otherItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(callItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(videoCallItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(otherItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(editItem, View.TRANSLATION_Y, visible ? -AndroidUtilities.dp(10) : 0.0f));
        animators.add(ObjectAnimator.ofFloat(mediaSearchItem, View.ALPHA, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(mediaSearchItem, View.TRANSLATION_Y, visible ? 0.0f : AndroidUtilities.dp(10)));
        animators.add(ObjectAnimator.ofFloat(sharedMediaLayout.photoVideoOptionsItem, View.ALPHA, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(sharedMediaLayout.photoVideoOptionsItem, View.TRANSLATION_Y, visible ? 0.0f : AndroidUtilities.dp(10)));
        animators.add(ObjectAnimator.ofFloat(actionBar, ACTIONBAR_HEADER_PROGRESS, visible ? 1.0f : 0.0f));
        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, visible ? 0.0f : 1.0f));
        if (viewModel.getArgs().myProfile) animators.add(ObjectAnimator.ofFloat(onlineTextView[3], View.ALPHA, visible ? 0.0f : 1.0f));
        animators.add(ObjectAnimator.ofFloat(mediaCounterTextView, View.ALPHA, visible ? 1.0f : 0.0f));
        if (visible) {
            animators.add(ObjectAnimator.ofFloat(this, HEADER_SHADOW, 0.0f));
        }
        if (storyView != null || giftsView != null) {
            ValueAnimator va = ValueAnimator.ofFloat(0, 1);
            va.addUpdateListener(a -> updateStoriesViewBounds(true));
            animators.add(va);
        }

        headerAnimatorSet = new AnimatorSet();
        headerAnimatorSet.playTogether(animators);
        headerAnimatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
        headerAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (headerAnimatorSet != null) {
                    if (mediaHeaderVisible) {
                        if (callItemVisible) {
                            callItem.setVisibility(View.GONE);
                        }
                        if (videoCallItemVisible) {
                            videoCallItem.setVisibility(View.GONE);
                        }
                        if (editItemVisible) {
                            editItem.setVisibility(View.GONE);
                        }
                        otherItem.setVisibility(View.GONE);
                    } else {
                        if (sharedMediaLayout.isSearchItemVisible()) {
                            mediaSearchItem.setVisibility(View.VISIBLE);
                        }

                        sharedMediaLayout.photoVideoOptionsItem.setVisibility(View.INVISIBLE);

                        headerShadowAnimatorSet = new AnimatorSet();
                        headerShadowAnimatorSet.playTogether(ObjectAnimator.ofFloat(ProfileActivity.this, HEADER_SHADOW, 1.0f));
                        headerShadowAnimatorSet.setDuration(100);
                        headerShadowAnimatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                headerShadowAnimatorSet = null;
                            }
                        });
                        headerShadowAnimatorSet.start();
                    }
                }
                updateStoriesViewBounds(false);
                headerAnimatorSet = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                headerAnimatorSet = null;
            }
        });
        headerAnimatorSet.setDuration(150);
        headerAnimatorSet.start();
        viewModel.postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
    }

    public void openAddMember() {
        Bundle args = new Bundle();
        args.putBoolean("addToGroup", true);
        args.putLong("chatId", viewModel.getState().getValue().getCurrentChat().id);
        GroupCreateActivity fragment = new GroupCreateActivity(args);
        fragment.setInfo(viewModel.getState().getValue().getChatInfo());
        if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null) {
            LongSparseArray<TLObject> users = new LongSparseArray<>();
            for (int a = 0; a < viewModel.getState().getValue().getChatInfo().participants.participants.size(); a++) {
                users.put(viewModel.getState().getValue().getChatInfo().participants.participants.get(a).user_id, null);
            }
            fragment.setIgnoreUsers(users);
        }
        fragment.setDelegate2((users, fwdCount) -> {
            HashSet<Long> currentParticipants = new HashSet<>();
            ArrayList<TLRPC.User> addedUsers = new ArrayList<>();
            if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null && viewModel.getState().getValue().getChatInfo().participants.participants != null) {
                for (int i = 0; i < viewModel.getState().getValue().getChatInfo().participants.participants.size(); i++) {
                    currentParticipants.add(viewModel.getState().getValue().getChatInfo().participants.participants.get(i).user_id);
                }
            }
            getMessagesController().addUsersToChat(viewModel.getState().getValue().getCurrentChat(), ProfileActivity.this, users, fwdCount, user -> {
                addedUsers.add(user);
            }, restrictedUser -> {
                for (int i = 0; i < viewModel.getState().getValue().getChatInfo().participants.participants.size(); i++) {
                    if (viewModel.getState().getValue().getChatInfo().participants.participants.get(i).user_id == restrictedUser.id) {
                        viewModel.getState().getValue().getChatInfo().participants.participants.remove(i);
                        updateListAnimated(true);
                        break;
                    }
                }
            }, () -> {
                int N = addedUsers.size();
                int[] finished = new int[1];
                for (int a = 0; a < N; a++) {
                    TLRPC.User user = addedUsers.get(a);
                    if (!currentParticipants.contains(user.id)) {
                        if (viewModel.getState().getValue().getChatInfo().participants == null) {
                            viewModel.getState().getValue().getChatInfo().participants = new TLRPC.TL_chatParticipants();
                        }
                        if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat())) {
                            TLRPC.TL_chatChannelParticipant channelParticipant1 = new TLRPC.TL_chatChannelParticipant();
                            channelParticipant1.channelParticipant = new TLRPC.TL_channelParticipant();
                            channelParticipant1.channelParticipant.inviter_id = getUserConfig().getClientUserId();
                            channelParticipant1.channelParticipant.peer = new TLRPC.TL_peerUser();
                            channelParticipant1.channelParticipant.peer.user_id = user.id;
                            channelParticipant1.channelParticipant.date = getConnectionsManager().getCurrentTime();
                            channelParticipant1.user_id = user.id;
                            viewModel.getState().getValue().getChatInfo().participants.participants.add(channelParticipant1);
                        } else {
                            TLRPC.ChatParticipant participant = new TLRPC.TL_chatParticipant();
                            participant.user_id = user.id;
                            participant.inviter_id = getAccountInstance().getUserConfig().clientUserId;
                            viewModel.getState().getValue().getChatInfo().participants.participants.add(participant);
                        }
                        viewModel.getState().getValue().getChatInfo().participants_count++;
                        getMessagesController().putUser(user, false);
                    }
                }
                updateListAnimated(true);
            });

        });
        presentFragment(fragment);
    }

    private void checkListViewScroll() {
        if (listView.getVisibility() != View.VISIBLE) {
            return;
        }
        if (sharedMediaLayoutAttached) {
            sharedMediaLayout.setVisibleHeight(listView.getMeasuredHeight() - sharedMediaLayout.getTop());
        }

        if (listView.getChildCount() <= 0 || openAnimationInProgress) {
            return;
        }

        int newOffset = 0;
        View child = null;
        for (int i = 0; i < listView.getChildCount(); i++) {
            if (listView.getChildAdapterPosition(listView.getChildAt(i)) == 0) {
                child = listView.getChildAt(i);
                break;
            }
        }
        RecyclerListView.Holder holder = child == null ? null : (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = child == null ? 0 : child.getTop();
        int adapterPosition = holder != null ? holder.getAdapterPosition() : RecyclerView.NO_POSITION;
        if (top >= 0 && adapterPosition == 0) {
            newOffset = top;
        }
        boolean mediaHeaderVisible;
        boolean searchVisible = imageUpdater == null && actionBar.isSearchFieldVisible();
        if (sharedMediaRow != -1 && !searchVisible) {
            holder = (RecyclerListView.Holder) listView.findViewHolderForAdapterPosition(sharedMediaRow);
            mediaHeaderVisible = holder != null && holder.itemView.getTop() <= 0;
        } else {
            mediaHeaderVisible = searchVisible;
        }
        setMediaHeaderVisible(mediaHeaderVisible);

        if (extraHeight != newOffset && !transitionAnimationInProress) {
            extraHeight = newOffset;
            topView.invalidate();
            if (playProfileAnimation != 0) {
                allowProfileAnimation = extraHeight != 0;
            }
            needLayout(true);
        }
    }

    public void updateSelectedMediaTabText() {
        if (sharedMediaLayout == null || mediaCounterTextView == null) {
            return;
        }
        int id = sharedMediaLayout.getClosestTab();
        int[] mediaCount = sharedMediaPreloader.getLastMediaCount();
        if (id == SharedMediaLayout.TAB_PHOTOVIDEO) {
            if (mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY] <= 0 && mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY] <= 0) {
                if (mediaCount[MediaDataController.MEDIA_PHOTOVIDEO] <= 0) {
                    mediaCounterTextView.setText(LocaleController.getString(R.string.SharedMedia));
                } else {
                    mediaCounterTextView.setText(LocaleController.formatPluralString("Media", mediaCount[MediaDataController.MEDIA_PHOTOVIDEO]));
                }
            } else if (sharedMediaLayout.getPhotosVideosTypeFilter() == SharedMediaLayout.FILTER_PHOTOS_ONLY || mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY] <= 0) {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Photos", mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY]));
            } else if (sharedMediaLayout.getPhotosVideosTypeFilter() == SharedMediaLayout.FILTER_VIDEOS_ONLY || mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY] <= 0) {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Videos", mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY]));
            } else {
                String str = String.format("%s, %s", LocaleController.formatPluralString("Photos", mediaCount[MediaDataController.MEDIA_PHOTOS_ONLY]), LocaleController.formatPluralString("Videos", mediaCount[MediaDataController.MEDIA_VIDEOS_ONLY]));
                mediaCounterTextView.setText(str);
            }
        } else if (id == SharedMediaLayout.TAB_FILES) {
            if (mediaCount[MediaDataController.MEDIA_FILE] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Files));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Files", mediaCount[MediaDataController.MEDIA_FILE]));
            }
        } else if (id == SharedMediaLayout.TAB_VOICE) {
            if (mediaCount[MediaDataController.MEDIA_AUDIO] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Voice));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Voice", mediaCount[MediaDataController.MEDIA_AUDIO]));
            }
        } else if (id == SharedMediaLayout.TAB_LINKS) {
            if (mediaCount[MediaDataController.MEDIA_URL] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.SharedLinks));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("Links", mediaCount[MediaDataController.MEDIA_URL]));
            }
        } else if (id == SharedMediaLayout.TAB_AUDIO) {
            if (mediaCount[MediaDataController.MEDIA_MUSIC] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.Music));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("MusicFiles", mediaCount[MediaDataController.MEDIA_MUSIC]));
            }
        } else if (id == SharedMediaLayout.TAB_GIF) {
            if (mediaCount[MediaDataController.MEDIA_GIF] <= 0) {
                mediaCounterTextView.setText(LocaleController.getString(R.string.AccDescrGIFs));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("GIFs", mediaCount[MediaDataController.MEDIA_GIF]));
            }
        } else if (id == SharedMediaLayout.TAB_COMMON_GROUPS) {
            mediaCounterTextView.setText(LocaleController.formatPluralString("CommonGroups", viewModel.getState().getValue().getUserInfo().common_chats_count));
        } else if (id == SharedMediaLayout.TAB_GROUPUSERS) {
            mediaCounterTextView.setText(onlineTextView[1].getText());
        } else if (id == SharedMediaLayout.TAB_STORIES) {
            if (viewModel.getState().getValue().isBot()) {
                mediaCounterTextView.setText(sharedMediaLayout.getBotPreviewsSubtitle(false));
            } else {
                mediaCounterTextView.setText(LocaleController.formatPluralString("ProfileStoriesCount", sharedMediaLayout.getStoriesCount(id)));
            }
        } else if (id == SharedMediaLayout.TAB_BOT_PREVIEWS) {
            mediaCounterTextView.setText(sharedMediaLayout.getBotPreviewsSubtitle(true));
        } else if (id == SharedMediaLayout.TAB_ARCHIVED_STORIES) {
            mediaCounterTextView.setText(LocaleController.formatPluralString("ProfileStoriesArchiveCount", sharedMediaLayout.getStoriesCount(id)));
        } else if (id == SharedMediaLayout.TAB_RECOMMENDED_CHANNELS) {
            final MessagesController.ChannelRecommendations rec = MessagesController.getInstance(currentAccount).getChannelRecommendations(viewModel.getDialogId());
            mediaCounterTextView.setText(LocaleController.formatPluralString(viewModel.getState().getValue().isBot() ? "Bots" : "Channels", rec == null ? 0 : rec.chats.size() + rec.more));
        } else if (id == SharedMediaLayout.TAB_SAVED_MESSAGES) {
            int messagesCount = getMessagesController().getSavedMessagesController().getMessagesCount(viewModel.getDialogId());
            mediaCounterTextView.setText(LocaleController.formatPluralString("SavedMessagesCount", Math.max(1, messagesCount)));
        } else if (id == SharedMediaLayout.TAB_GIFTS) {
            mediaCounterTextView.setText(LocaleController.formatPluralStringComma("ProfileGiftsCount", sharedMediaLayout.giftsContainer == null ? 0 : sharedMediaLayout.giftsContainer.getGiftsCount()));
        }
    }

    private void needLayout(boolean animated) {
        final int newTop = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight();

        FrameLayout.LayoutParams layoutParams;
        if (listView != null && !openAnimationInProgress) {
            layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            if (layoutParams.topMargin != newTop) {
                layoutParams.topMargin = newTop;
                listView.setLayoutParams(layoutParams);
            }
        }

        if (avatarContainer != null) {
            final float diff = Math.min(1f, extraHeight / AndroidUtilities.dp(88f));

            listView.setTopGlowOffset((int) extraHeight);

            listView.setOverScrollMode(extraHeight > AndroidUtilities.dp(88f) && extraHeight < listView.getMeasuredWidth() - newTop ? View.OVER_SCROLL_NEVER : View.OVER_SCROLL_ALWAYS);

            if (writeButton != null) {
                writeButton.setTranslationY((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight + searchTransitionOffset - AndroidUtilities.dp(29.5f));

                boolean writeButtonVisible = diff > 0.2f && !searchMode && (imageUpdater == null || setAvatarRow == -1);
                if (writeButtonVisible && viewModel.getArgs().chatId != 0) {
                    writeButtonVisible = ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup() && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().linked_chat_id != 0 && infoHeaderRow != -1;
                }
                if (!openAnimationInProgress) {
                    boolean currentVisible = writeButton.getTag() == null;
                    if (writeButtonVisible != currentVisible) {
                        if (writeButtonVisible) {
                            writeButton.setTag(null);
                        } else {
                            writeButton.setTag(0);
                        }
                        if (writeButtonAnimation != null) {
                            AnimatorSet old = writeButtonAnimation;
                            writeButtonAnimation = null;
                            old.cancel();
                        }
                        if (animated) {
                            writeButtonAnimation = new AnimatorSet();
                            if (writeButtonVisible) {
                                writeButtonAnimation.setInterpolator(new DecelerateInterpolator());
                                writeButtonAnimation.playTogether(
                                        ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 1.0f),
                                        ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 1.0f),
                                        ObjectAnimator.ofFloat(writeButton, View.ALPHA, 1.0f)
                                );
                            } else {
                                writeButtonAnimation.setInterpolator(new AccelerateInterpolator());
                                writeButtonAnimation.playTogether(
                                        ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 0.2f),
                                        ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 0.2f),
                                        ObjectAnimator.ofFloat(writeButton, View.ALPHA, 0.0f)
                                );
                            }
                            writeButtonAnimation.setDuration(150);
                            writeButtonAnimation.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (writeButtonAnimation != null && writeButtonAnimation.equals(animation)) {
                                        writeButtonAnimation = null;
                                    }
                                }
                            });
                            writeButtonAnimation.start();
                        } else {
                            writeButton.setScaleX(writeButtonVisible ? 1.0f : 0.2f);
                            writeButton.setScaleY(writeButtonVisible ? 1.0f : 0.2f);
                            writeButton.setAlpha(writeButtonVisible ? 1.0f : 0.0f);
                        }
                    }

                    if (qrItem != null) {
                        updateQrItemVisibility(animated);
                        if (!animated) {
                            float translation = AndroidUtilities.dp(48) * qrItem.getAlpha();
                            qrItem.setTranslationX(translation);
                            if (avatarsViewPagerIndicatorView != null) {
                                avatarsViewPagerIndicatorView.setTranslationX(translation - AndroidUtilities.dp(48));
                            }
                        }
                    }
                }

                if (storyView != null) {
                    storyView.setExpandCoords(avatarContainer2.getMeasuredWidth() - AndroidUtilities.dp(40), writeButtonVisible, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight + searchTransitionOffset);
                }
                if (giftsView != null) {
                    giftsView.setExpandCoords(avatarContainer2.getMeasuredWidth() - AndroidUtilities.dp(40), writeButtonVisible, (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + extraHeight + searchTransitionOffset);
                }
            }

            avatarX = -AndroidUtilities.dpf2(47f) * diff;
            avatarY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f * (1.0f + diff) - 21 * AndroidUtilities.density + 27 * AndroidUtilities.density * diff + actionBar.getTranslationY();

            float h = openAnimationInProgress ? initialAnimationExtraHeight : extraHeight;
            if (h > AndroidUtilities.dp(88f) || isPulledDown) {
                expandProgress = Math.max(0f, Math.min(1f, (h - AndroidUtilities.dp(88f)) / (listView.getMeasuredWidth() - newTop - AndroidUtilities.dp(88f))));
                avatarScale = AndroidUtilities.lerp((42f + 18f) / 42f, (42f + 42f + 18f) / 42f, Math.min(1f, expandProgress * 3f));
                if (storyView != null) {
                    storyView.invalidate();
                }
                if (giftsView != null) {
                    giftsView.invalidate();
                }

                final float durationFactor = Math.min(AndroidUtilities.dpf2(2000f), Math.max(AndroidUtilities.dpf2(1100f), Math.abs(listViewVelocityY))) / AndroidUtilities.dpf2(1100f);

                if (viewModel.getState().getValue().isAllowPullingDown() && (openingAvatar || expandProgress >= 0.33f)) {
                    if (!isPulledDown) {
                        if (otherItem != null) {
                            if (!getMessagesController().isChatNoForwards(viewModel.getState().getValue().getCurrentChat())) {
                                otherItem.showSubItem(ActionBarActions.gallery_menu_save);
                            } else {
                                otherItem.hideSubItem(ActionBarActions.gallery_menu_save);
                            }
                            if (imageUpdater != null) {
                                otherItem.showSubItem(ActionBarActions.add_photo);
                                otherItem.showSubItem(ActionBarActions.edit_avatar);
                                otherItem.showSubItem(ActionBarActions.delete_avatar);
                                otherItem.hideSubItem(ActionBarActions.set_as_main);
                                otherItem.hideSubItem(ActionBarActions.logout);
                            }
                        }
                        if (searchItem != null) {
                            searchItem.setEnabled(false);
                        }
                        isPulledDown = true;
                        viewModel.postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                        overlaysView.setOverlaysVisible(true, durationFactor);
                        avatarsViewPagerIndicatorView.refreshVisibility(durationFactor);
                        avatarsViewPager.setCreateThumbFromParent(true);
                        avatarsViewPager.getAdapter().notifyDataSetChanged();
                        expandAnimator.cancel();
                        float value = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture);
                        expandAnimatorValues[0] = value;
                        expandAnimatorValues[1] = 1f;
                        if (storyView != null && !storyView.isEmpty()) {
                            expandAnimator.setInterpolator(new FastOutSlowInInterpolator());
                            expandAnimator.setDuration((long) ((1f - value) * 1.3f * 250f / durationFactor));
                        } else {
                            expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
                            expandAnimator.setDuration((long) ((1f - value) * 250f / durationFactor));
                        }
                        expandAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                setForegroundImage(false);
                                avatarsViewPager.setAnimatedFileMaybe(avatarImage.getImageReceiver().getAnimation());
                                avatarsViewPager.resetCurrentItem();
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                expandAnimator.removeListener(this);
                                topView.setBackgroundColor(Color.BLACK);
                                avatarContainer.setVisibility(View.GONE);
                                avatarsViewPager.setVisibility(View.VISIBLE);
                            }
                        });
                        expandAnimator.start();
                    }
                    ViewGroup.LayoutParams params = avatarsViewPager.getLayoutParams();
                    params.width = listView.getMeasuredWidth();
                    params.height = (int) (h + newTop);
                    avatarsViewPager.requestLayout();
                    if (!expandAnimator.isRunning()) {
                        float additionalTranslationY = 0;
                        if (openAnimationInProgress && playProfileAnimation == 2) {
                            additionalTranslationY = -(1.0f - avatarAnimationProgress) * AndroidUtilities.dp(50);
                        }
                        onlineX = AndroidUtilities.dpf2(16f) - onlineTextView[1].getLeft();
                        nameTextView[1].setTranslationX(AndroidUtilities.dpf2(18f) - nameTextView[1].getLeft());
                        nameTextView[1].setTranslationY(newTop + h - AndroidUtilities.dpf2(38f) - nameTextView[1].getBottom() + additionalTranslationY);
                        onlineTextView[1].setTranslationX(onlineX + customPhotoOffset);
                        onlineTextView[1].setTranslationY(newTop + h - AndroidUtilities.dpf2(18f) - onlineTextView[1].getBottom() + additionalTranslationY);
                        mediaCounterTextView.setTranslationX(onlineTextView[1].getTranslationX());
                        mediaCounterTextView.setTranslationY(onlineTextView[1].getTranslationY());
                        updateCollectibleHint();
                    }
                } else {
                    if (isPulledDown) {
                        isPulledDown = false;
                        viewModel.postNotificationName(NotificationCenter.needCheckSystemBarColors, true);
                        if (otherItem != null) {
                            otherItem.hideSubItem(ActionBarActions.gallery_menu_save);
                            if (imageUpdater != null) {
                                otherItem.hideSubItem(ActionBarActions.set_as_main);
                                otherItem.hideSubItem(ActionBarActions.edit_avatar);
                                otherItem.hideSubItem(ActionBarActions.delete_avatar);
                                otherItem.showSubItem(ActionBarActions.add_photo);
                                otherItem.showSubItem(ActionBarActions.logout);
//                                otherItem.showSubItem(edit_name);
                            }
                        }
                        if (searchItem != null) {
                            searchItem.setEnabled(!scrolling);
                        }
                        overlaysView.setOverlaysVisible(false, durationFactor);
                        avatarsViewPagerIndicatorView.refreshVisibility(durationFactor);
                        expandAnimator.cancel();
                        avatarImage.getImageReceiver().setAllowStartAnimation(true);
                        avatarImage.getImageReceiver().startAnimation();

                        float value = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture);
                        expandAnimatorValues[0] = value;
                        expandAnimatorValues[1] = 0f;
                        expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
                        if (!viewModel.getState().getValue().isInLandscapeMode()) {
                            expandAnimator.setDuration((long) (value * 250f / durationFactor));
                        } else {
                            expandAnimator.setDuration(0);
                        }
                        topView.setBackgroundColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));

                        if (!doNotSetForeground) {
                            BackupImageView imageView = avatarsViewPager.getCurrentItemView();
                            if (imageView != null) {
                                if (imageView.getImageReceiver().getDrawable() instanceof VectorAvatarThumbDrawable) {
                                    avatarImage.drawForeground(false);
                                } else {
                                    avatarImage.drawForeground(true);
                                    avatarImage.setForegroundImageDrawable(imageView.getImageReceiver().getDrawableSafe());
                                }
                            }
                        }
                        avatarImage.setForegroundAlpha(1f);
                        avatarContainer.setVisibility(View.VISIBLE);
                        avatarsViewPager.setVisibility(View.GONE);
                        expandAnimator.start();
                    }

                    avatarContainer.setScaleX(avatarScale);
                    avatarContainer.setScaleY(avatarScale);

                    if (expandAnimator == null || !expandAnimator.isRunning()) {
                        refreshNameAndOnlineXY();
                        nameTextView[1].setTranslationX(nameX);
                        nameTextView[1].setTranslationY(nameY);
                        onlineTextView[1].setTranslationX(onlineX + customPhotoOffset);
                        onlineTextView[1].setTranslationY(onlineY);
                        mediaCounterTextView.setTranslationX(onlineX);
                        mediaCounterTextView.setTranslationY(onlineY);
                        updateCollectibleHint();
                    }
                }
            }

            if (openAnimationInProgress && playProfileAnimation == 2) {
                float avX = 0;
                float avY = (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f - 21 * AndroidUtilities.density + actionBar.getTranslationY();

                nameTextView[0].setTranslationX(0);
                nameTextView[0].setTranslationY((float) Math.floor(avY) + AndroidUtilities.dp(1.3f));
                onlineTextView[0].setTranslationX(0);
                onlineTextView[0].setTranslationY((float) Math.floor(avY) + AndroidUtilities.dp(24));
                nameTextView[0].setScaleX(1.0f);
                nameTextView[0].setScaleY(1.0f);

                nameTextView[1].setPivotY(nameTextView[1].getMeasuredHeight());
                nameTextView[1].setScaleX(1.67f);
                nameTextView[1].setScaleY(1.67f);

                avatarScale = AndroidUtilities.lerp(1.0f, (42f + 42f + 18f) / 42f, avatarAnimationProgress);
                if (storyView != null) {
                    storyView.setExpandProgress(1f);
                }
                if (giftsView != null) {
                    giftsView.setExpandProgress(1f);
                }

                avatarImage.setRoundRadius((int) AndroidUtilities.lerp(getSmallAvatarRoundRadius(), 0f, avatarAnimationProgress));
                avatarContainer.setTranslationX(AndroidUtilities.lerp(avX, 0, avatarAnimationProgress));
                avatarContainer.setTranslationY(AndroidUtilities.lerp((float) Math.ceil(avY), 0f, avatarAnimationProgress));
                float extra = (avatarContainer.getMeasuredWidth() - AndroidUtilities.dp(42)) * avatarScale;
                timeItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(16) + extra);
                timeItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(15) + extra);
                starBgItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(28) + extra);
                starBgItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(24) + extra);
                starFgItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(28) + extra);
                starFgItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(24) + extra);
                avatarContainer.setScaleX(avatarScale);
                avatarContainer.setScaleY(avatarScale);

                overlaysView.setAlphaValue(avatarAnimationProgress, false);
                actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), Color.WHITE, avatarAnimationProgress), false);

                if (scamDrawable != null) {
                    scamDrawable.setColor(ColorUtils.blendARGB(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), Color.argb(179, 255, 255, 255), avatarAnimationProgress));
                }
                if (lockIconDrawable != null) {
                    lockIconDrawable.setColorFilter(ColorUtils.blendARGB(getThemedColor(Theme.key_chat_lockIcon), Color.WHITE, avatarAnimationProgress), PorterDuff.Mode.MULTIPLY);
                }
                if (verifiedCrossfadeDrawable[1] != null) {
                    verifiedCrossfadeDrawable[1].setProgress(avatarAnimationProgress);
                    nameTextView[1].invalidate();
                }
                if (premiumCrossfadeDrawable[1] != null) {
                    premiumCrossfadeDrawable[1].setProgress(avatarAnimationProgress);
                    nameTextView[1].invalidate();
                }
                updateEmojiStatusDrawableColor(avatarAnimationProgress);

                final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) avatarContainer.getLayoutParams();
                params.width = params.height = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(42f), (extraHeight + newTop) / avatarScale, avatarAnimationProgress);
                params.leftMargin = (int) AndroidUtilities.lerp(AndroidUtilities.dpf2(64f), 0f, avatarAnimationProgress);
                avatarContainer.requestLayout();

                updateCollectibleHint();
            } else if (extraHeight <= AndroidUtilities.dp(88f)) {
                avatarScale = (42 + 18 * diff) / 42.0f;
                if (storyView != null) {
                    storyView.invalidate();
                }
                if (giftsView != null) {
                    giftsView.invalidate();
                }
                float nameScale = 1.0f + 0.12f * diff;
                if (expandAnimator == null || !expandAnimator.isRunning()) {
                    avatarContainer.setScaleX(avatarScale);
                    avatarContainer.setScaleY(avatarScale);
                    avatarContainer.setTranslationX(avatarX);
                    avatarContainer.setTranslationY((float) Math.ceil(avatarY));
                    float extra = AndroidUtilities.dp(42) * avatarScale - AndroidUtilities.dp(42);
                    timeItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(16) + extra);
                    timeItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(15) + extra);
                    starBgItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(28) + extra);
                    starBgItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(24) + extra);
                    starFgItem.setTranslationX(avatarContainer.getX() + AndroidUtilities.dp(28) + extra);
                    starFgItem.setTranslationY(avatarContainer.getY() + AndroidUtilities.dp(24) + extra);
                }
                nameX = -21 * AndroidUtilities.density * diff;
                nameY = (float) Math.floor(avatarY) + AndroidUtilities.dp(1.3f) + AndroidUtilities.dp(7) * diff + titleAnimationsYDiff * (1f - avatarAnimationProgress);
                onlineX = -21 * AndroidUtilities.density * diff;
                onlineY = (float) Math.floor(avatarY) + AndroidUtilities.dp(24) + (float) Math.floor(11 * AndroidUtilities.density) * diff;
                if (showStatusButton != null) {
                    showStatusButton.setAlpha((int) (0xFF * diff));
                }
                for (int a = 0; a < nameTextView.length; a++) {
                    if (nameTextView[a] == null) {
                        continue;
                    }
                    if (expandAnimator == null || !expandAnimator.isRunning()) {
                        nameTextView[a].setTranslationX(nameX);
                        nameTextView[a].setTranslationY(nameY);

                        onlineTextView[a].setTranslationX(onlineX + customPhotoOffset);
                        onlineTextView[a].setTranslationY(onlineY);
                        if (a == 1) {
                            mediaCounterTextView.setTranslationX(onlineX);
                            mediaCounterTextView.setTranslationY(onlineY);
                        }
                    }
                    nameTextView[a].setScaleX(nameScale);
                    nameTextView[a].setScaleY(nameScale);
                }
                updateCollectibleHint();
            }

            if (!openAnimationInProgress && (expandAnimator == null || !expandAnimator.isRunning())) {
                needLayoutText(diff);
            }
        }

        if (isPulledDown || (overlaysView != null && overlaysView.getAnimator() != null && overlaysView.getAnimator().isRunning())) {
            final ViewGroup.LayoutParams overlaysLp = overlaysView.getLayoutParams();
            overlaysLp.width = listView.getMeasuredWidth();
            overlaysLp.height = (int) (extraHeight + newTop);
            overlaysView.requestLayout();
        }

        updateEmojiStatusEffectPosition();
    }

    public void updateQrItemVisibility(boolean animated) {
        if (qrItem == null) {
            return;
        }
        boolean setQrVisible = isQrNeedVisible() && Math.min(1f, extraHeight / AndroidUtilities.dp(88f)) > .5f && searchTransitionProgress > .5f;
        if (animated) {
            if (setQrVisible != isQrItemVisible) {
                isQrItemVisible = setQrVisible;
                if (qrItemAnimation != null) {
                    qrItemAnimation.cancel();
                    qrItemAnimation = null;
                }
                qrItem.setClickable(isQrItemVisible);
                qrItemAnimation = new AnimatorSet();
                if (!(qrItem.getVisibility() == View.GONE && !setQrVisible)) {
                    qrItem.setVisibility(View.VISIBLE);
                }
                if (setQrVisible) {
                    qrItemAnimation.setInterpolator(new DecelerateInterpolator());
                    qrItemAnimation.playTogether(
                            ObjectAnimator.ofFloat(qrItem, View.ALPHA, 1.0f),
                            ObjectAnimator.ofFloat(qrItem, View.SCALE_Y, 1f),
                            ObjectAnimator.ofFloat(avatarsViewPagerIndicatorView, View.TRANSLATION_X, -AndroidUtilities.dp(48))
                    );
                } else {
                    qrItemAnimation.setInterpolator(new AccelerateInterpolator());
                    qrItemAnimation.playTogether(
                            ObjectAnimator.ofFloat(qrItem, View.ALPHA, 0.0f),
                            ObjectAnimator.ofFloat(qrItem, View.SCALE_Y, 0f),
                            ObjectAnimator.ofFloat(avatarsViewPagerIndicatorView, View.TRANSLATION_X, 0)
                    );
                }
                qrItemAnimation.setDuration(150);
                qrItemAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        qrItemAnimation = null;
                    }
                });
                qrItemAnimation.start();
            }
        } else {
            if (qrItemAnimation != null) {
                qrItemAnimation.cancel();
                qrItemAnimation = null;
            }
            isQrItemVisible = setQrVisible;
            qrItem.setClickable(isQrItemVisible);
            qrItem.setAlpha(setQrVisible ? 1.0f : 0.0f);
            qrItem.setVisibility(setQrVisible ? View.VISIBLE : View.GONE);
        }
    }

    public void setForegroundImage(boolean secondParent) {
        Drawable drawable = avatarImage.getImageReceiver().getDrawable();
        if (drawable instanceof VectorAvatarThumbDrawable) {
            avatarImage.setForegroundImage(null, null, drawable);
        } else if (drawable instanceof AnimatedFileDrawable) {
            AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) drawable;
            avatarImage.setForegroundImage(null, null, fileDrawable);
            if (secondParent) {
                fileDrawable.addSecondParentView(avatarImage);
            }
        } else {
            ImageLocation location = avatarsViewPager.getImageLocation(0);
            String filter;
            if (location != null && location.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                filter = "avatar";
            } else {
                filter = null;
            }
            avatarImage.setForegroundImage(location, filter, drawable);
        }
    }

    private void refreshNameAndOnlineXY() {
        nameX = AndroidUtilities.dp(-21f) + avatarContainer.getMeasuredWidth() * (avatarScale - (42f + 18f) / 42f);
        nameY = (float) Math.floor(avatarY) + AndroidUtilities.dp(1.3f) + AndroidUtilities.dp(7f) + avatarContainer.getMeasuredHeight() * (avatarScale - (42f + 18f) / 42f) / 2f;
        onlineX = AndroidUtilities.dp(-21f) + avatarContainer.getMeasuredWidth() * (avatarScale - (42f + 18f) / 42f);
        onlineY = (float) Math.floor(avatarY) + AndroidUtilities.dp(24) + (float) Math.floor(11 * AndroidUtilities.density) + avatarContainer.getMeasuredHeight() * (avatarScale - (42f + 18f) / 42f) / 2f;
    }

    public RecyclerListView getListView() {
        return listView;
    }

    private void needLayoutText(float diff) {
        FrameLayout.LayoutParams layoutParams;
        float scale = nameTextView[1].getScaleX();
        float maxScale = extraHeight > AndroidUtilities.dp(88f) ? 1.67f : 1.12f;

        if (extraHeight > AndroidUtilities.dp(88f) && scale != maxScale) {
            return;
        }

        int viewWidth = AndroidUtilities.isTablet() ? AndroidUtilities.dp(490) : AndroidUtilities.displaySize.x;
        ActionBarMenuItem item = avatarsViewPagerIndicatorView.getSecondaryMenuItem();
        int extra = 0;
        if (editItemVisible) {
            extra += 48;
        }
        if (callItemVisible) {
            extra += 48;
        }
        if (videoCallItemVisible) {
            extra += 48;
        }
        if (searchItem != null) {
            extra += 48;
        }
        int buttonsWidth = AndroidUtilities.dp(118 + 8 + (40 + extra * (1.0f - mediaHeaderAnimationProgress)));
        int minWidth = viewWidth - buttonsWidth;

        int width = (int) (viewWidth - buttonsWidth * Math.max(0.0f, 1.0f - (diff != 1.0f ? diff * 0.15f / (1.0f - diff) : 1.0f)) - nameTextView[1].getTranslationX());
        float width2 = nameTextView[1].getPaint().measureText(nameTextView[1].getText().toString()) * scale + nameTextView[1].getSideDrawablesSize();
        layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
        int prevWidth = layoutParams.width;
        if (width < width2) {
            layoutParams.width = Math.max(minWidth, (int) Math.ceil((width - AndroidUtilities.dp(24)) / (scale + ((maxScale - scale) * 7.0f))));
        } else {
            layoutParams.width = (int) Math.ceil(width2);
        }
        layoutParams.width = (int) Math.min((viewWidth - nameTextView[1].getX()) / scale - AndroidUtilities.dp(8), layoutParams.width);
        if (layoutParams.width != prevWidth) {
            nameTextView[1].requestLayout();
        }

        width2 = onlineTextView[1].getPaint().measureText(onlineTextView[1].getText().toString()) + onlineTextView[1].getRightDrawableWidth();
        layoutParams = (FrameLayout.LayoutParams) onlineTextView[1].getLayoutParams();
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) mediaCounterTextView.getLayoutParams();
        prevWidth = layoutParams.width;
        layoutParams2.rightMargin = layoutParams.rightMargin = (int) Math.ceil(onlineTextView[1].getTranslationX() + AndroidUtilities.dp(8) + AndroidUtilities.dp(40) * (1.0f - diff));
        if (width < width2) {
            layoutParams2.width = layoutParams.width = (int) Math.ceil(width);
        } else {
            layoutParams2.width = layoutParams.width = LayoutHelper.WRAP_CONTENT;
        }
        if (prevWidth != layoutParams.width) {
            onlineTextView[2].getLayoutParams().width = layoutParams.width;
            onlineTextView[2].requestLayout();
            onlineTextView[3].getLayoutParams().width = layoutParams.width;
            onlineTextView[3].requestLayout();
            onlineTextView[1].requestLayout();
            mediaCounterTextView.requestLayout();
        }
    }

    private void fixLayout() {
        if (fragmentView == null) {
            return;
        }
        fragmentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (fragmentView != null) {
                    checkListViewScroll();
                    needLayout(true);
                    fragmentView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onConfigurationChanged(newConfig);
        }
        invalidateIsInLandscapeMode();
        if (viewModel.getState().getValue().isInLandscapeMode() && isPulledDown) {
            final View view = layoutManager.findViewByPosition(0);
            if (view != null) {
                listView.scrollBy(0, view.getTop() - AndroidUtilities.dp(88));
            }
        }
        fixLayout();
    }

    private void invalidateIsInLandscapeMode() {
        final Point size = new Point();
        final Display display = getParentActivity().getWindowManager().getDefaultDisplay();
        display.getSize(size);
        viewModel.isInLandscapeMode(size.x > size.y);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, final Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            boolean infoChanged = (mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0;
            if (viewModel.getArgs().userId != 0) {
                if (infoChanged) {
                    updateProfileData(true);
                }
                if ((mask & MessagesController.UPDATE_MASK_PHONE) != 0) {
                    if (listView != null) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForPosition(phoneRow);
                        if (holder != null) {
                            listAdapter.onBindViewHolder(holder, phoneRow);
                        }
                    }
                }
            } else if (viewModel.getArgs().chatId != 0) {
                if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0) {
                    if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0) {
                        updateListAnimated(true);
                    } else {
                        viewModel.updateOnlineCount(true);
                    }
                    updateProfileData(true);
                }
                if (infoChanged) {
                    if (listView != null) {
                        int count = listView.getChildCount();
                        for (int a = 0; a < count; a++) {
                            View child = listView.getChildAt(a);
                            if (child instanceof UserCell) {
                                ((UserCell) child).update(mask);
                            }
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.contactsDidLoad || id == NotificationCenter.channelRightsUpdated) {
            createActionBarMenu(true);
        } else if (id == NotificationCenter.encryptedChatCreated) {
            if (viewModel.getState().getValue().isCreatingChat()) {
                AndroidUtilities.runOnUIThread(() -> {
                    getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                    getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                    TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat) args[0];
                    Bundle args2 = new Bundle();
                    args2.putInt("enc_id", encryptedChat.id);
                    presentFragment(new ChatActivity(args2), true);
                });
            }
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) args[0];
            if (viewModel.getState().getValue().getCurrentEncryptedChat() != null && chat.id == viewModel.getState().getValue().getCurrentEncryptedChat().id) {
                // TODO : fix
                //viewModel.getState().getValue().getCurrentEncryptedChat() = chat;
                updateListAnimated(false);
                if (flagSecure != null) {
                    flagSecure.invalidate();
                }
            }
        } else if (id == NotificationCenter.groupCallUpdated) {
            Long chatId = (Long) args[0];
            if (viewModel.getState().getValue().getCurrentChat() != null && chatId == viewModel.getState().getValue().getCurrentChat().id && ChatObject.canManageCalls(viewModel.getState().getValue().getCurrentChat())) {
                TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(chatId);
                if (chatFull != null) {
                    if (viewModel.getState().getValue().getChatInfo() != null) {
                        chatFull.participants = viewModel.getState().getValue().getChatInfo().participants;
                    }
                    viewModel.chatInfo(chatFull);
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setChatInfo(viewModel.getState().getValue().getChatInfo());
                }
                if (viewModel.getState().getValue().getChatInfo() != null && (viewModel.getState().getValue().getChatInfo().call == null && !hasVoiceChatItem || viewModel.getState().getValue().getChatInfo().call != null && hasVoiceChatItem)) {
                    createActionBarMenu(false);
                }
                if (storyView != null && viewModel.getState().getValue().getChatInfo() != null) {
                    storyView.setStories(viewModel.getState().getValue().getChatInfo().stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (chatId != 0) {
                    otherItem.setSubItemShown(ActionBarActions.gift_premium, !BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked() && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stargifts_available);
                }
            }
        } else if (id == NotificationCenter.chatInfoDidLoad) {
            final TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
            if (chatFull.id == viewModel.getArgs().chatId) {
                final boolean byChannelUsers = (Boolean) args[2];
                if (viewModel.getState().getValue().getChatInfo() instanceof TLRPC.TL_channelFull) {
                    if (chatFull.participants == null) {
                        chatFull.participants = viewModel.getState().getValue().getChatInfo().participants;
                    }
                }
                final boolean loadChannelParticipants = viewModel.getState().getValue().getChatInfo() == null && chatFull instanceof TLRPC.TL_channelFull;
                viewModel.chatInfo(chatFull);
                if (mergeDialogId == 0 && viewModel.getState().getValue().getChatInfo().migrated_from_chat_id != 0) {
                    mergeDialogId = -viewModel.getState().getValue().getChatInfo().migrated_from_chat_id;
                    getMediaDataController().getMediaCount(mergeDialogId, viewModel.getArgs().topicId, MediaDataController.MEDIA_PHOTOVIDEO, classGuid, true);
                }
                fetchUsersFromChannelInfo();
                if (avatarsViewPager != null && !viewModel.getArgs().isTopic) {
                    avatarsViewPager.setChatInfo(viewModel.getState().getValue().getChatInfo());
                }
                updateListAnimated(true);
                TLRPC.Chat newChat = getMessagesController().getChat(viewModel.getArgs().chatId);
                if (newChat != null) {
                    //TODO fix
                    //viewModel.getState().getValue().getCurrentChat() = newChat;
                    createActionBarMenu(true);
                }
                if (flagSecure != null) {
                    flagSecure.invalidate();
                }
                if (viewModel.getState().getValue().isCurrentChatMagagroup() && (loadChannelParticipants || !byChannelUsers)) {
                    viewModel.getChannelParticipants(true);
                }

                updateAutoDeleteItem();
                updateTtlIcon();
                if (storyView != null && viewModel.getState().getValue().getChatInfo() != null) {
                    storyView.setStories(viewModel.getState().getValue().getChatInfo().stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setChatInfo(viewModel.getState().getValue().getChatInfo());
                }
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack(true);
        } else if (id == NotificationCenter.userInfoDidLoad) {
            final long uid = (Long) args[0];
            if (uid == viewModel.getArgs().userId) {
                viewModel.userInfo((TLRPC.UserFull) args[1]);
                if (storyView != null) {
                    storyView.setStories(viewModel.getState().getValue().getUserInfo().stories);
                }
                if (giftsView != null) {
                    giftsView.update();
                }
                if (avatarImage != null) {
                    avatarImage.setHasStories(needInsetForStories());
                }
                if (sharedMediaLayout != null) {
                    sharedMediaLayout.setUserInfo(viewModel.getState().getValue().getUserInfo());
                }
                if (imageUpdater != null) {
                    if (listAdapter != null && !TextUtils.equals(viewModel.getState().getValue().getUserInfo().about, currentBio)) {
                        listAdapter.notifyItemChanged(bioRow);
                    }
                } else {
                    if (!openAnimationInProgress && !callItemVisible) {
                        createActionBarMenu(true);
                    } else {
                        recreateMenuAfterAnimation = true;
                    }
                    updateListAnimated(false);
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.setCommonGroupsCount(viewModel.getState().getValue().getUserInfo().common_chats_count);
                        updateSelectedMediaTabText();
                        if (sharedMediaPreloader == null || sharedMediaPreloader.isMediaWasLoaded()) {
                            resumeDelayedFragmentAnimation();
                            needLayout(true);
                        }
                    }
                }
                updateAutoDeleteItem();
                updateTtlIcon();
                if (profileChannelMessageFetcher == null && !isSettings()) {
                    profileChannelMessageFetcher = new ProfileChannelCell.ChannelMessageFetcher(currentAccount);
                    profileChannelMessageFetcher.subscribe(() -> updateListAnimated(false));
                    profileChannelMessageFetcher.fetch(viewModel.getState().getValue().getUserInfo());
                }
                if (!isSettings()) {
                    ProfileBirthdayEffect.BirthdayEffectFetcher oldFetcher = birthdayFetcher;
                    birthdayFetcher = ProfileBirthdayEffect.BirthdayEffectFetcher.of(currentAccount, viewModel.getState().getValue().getUserInfo(), birthdayFetcher);
                    createdBirthdayFetcher = birthdayFetcher != oldFetcher;
                    if (birthdayFetcher != null) {
                        birthdayFetcher.subscribe(this::createBirthdayEffect);
                    }
                }
                if (otherItem != null) {
                    if (hasPrivacyCommand()) {
                        otherItem.showSubItem(ActionBarActions.bot_privacy);
                    } else {
                        otherItem.hideSubItem(ActionBarActions.bot_privacy);
                    }
                }
            }
        } else if (id == NotificationCenter.privacyRulesUpdated) {
            if (qrItem != null) {
                updateQrItemVisibility(true);
            }
        } else if (id == NotificationCenter.didReceiveNewMessages) {
            final boolean scheduled = (Boolean) args[2];
            if (scheduled) {
                return;
            }
            final long did = viewModel.getDialogId();
            if (did == (Long) args[0]) {
                boolean enc = DialogObject.isEncryptedDialog(did);
                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                for (int a = 0; a < arr.size(); a++) {
                    MessageObject obj = arr.get(a);
                    if (viewModel.getState().getValue().getCurrentEncryptedChat() != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction && obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
                        TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) obj.messageOwner.action.encryptedAction;
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.emojiLoaded) {
            if (listView != null) {
                listView.invalidateViews();
            }
        } else if (id == NotificationCenter.reloadInterface) {
            updateListAnimated(false);
        } else if (id == NotificationCenter.newSuggestionsAvailable) {
            final int prevRow1 = passwordSuggestionRow;
            final int prevRow2 = phoneSuggestionRow;
            final int prevRow3 = graceSuggestionRow;
            updateRowsIds();
            if (prevRow1 != passwordSuggestionRow || prevRow2 != phoneSuggestionRow || prevRow3 != graceSuggestionRow) {
                listAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.updateSearchSettings) {
            if (searchAdapter != null) {
                searchAdapter.searchArray = searchAdapter.onCreateSearchArray();
                searchAdapter.recentSearches.clear();
                searchAdapter.updateSearchArray();
                searchAdapter.search(searchAdapter.lastSearchString);
            }
        } else if (id == NotificationCenter.storiesUpdated || id == NotificationCenter.storiesReadUpdated) {
            if (avatarImage != null) {
                avatarImage.setHasStories(needInsetForStories());
                updateAvatarRoundRadius();
            }
            if (storyView != null) {
                if (viewModel.getState().getValue().getUserInfo() != null) {
                    storyView.setStories(viewModel.getState().getValue().getUserInfo().stories);
                } else if (viewModel.getState().getValue().getChatInfo() != null) {
                    storyView.setStories(viewModel.getState().getValue().getChatInfo().stories);
                }
            }
        } else if (id == NotificationCenter.userIsPremiumBlockedUpadted) {
            if (otherItem != null) {
                otherItem.setSubItemShown(ActionBarActions.start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(viewModel.getArgs().userId)));
            }
            updateEditColorIcon();
        } else if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            updateEditColorIcon();
        } else if (id == NotificationCenter.dialogDeleted) {
            final long dialogId = (long) args[0];
            if (viewModel.getDialogId() == dialogId) {
                if (parentLayout != null && parentLayout.getLastFragment() == this) {
                    finishFragment();
                } else {
                    removeSelfFromStack();
                }
            }
        } else if (id == NotificationCenter.channelRecommendationsLoaded) {
            final long dialogId = (long) args[0];
            if (sharedMediaRow < 0 && dialogId == viewModel.getDialogId()) {
                updateRowsIds();
                updateSelectedMediaTabText();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.starUserGiftsLoaded) {
            final long dialogId = (long) args[0];
            if (dialogId == viewModel.getDialogId() && !isSettings()) {
                if (sharedMediaRow < 0) {
                    updateRowsIds();
                    updateSelectedMediaTabText();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.updateTabs(true);
                            sharedMediaLayout.updateAdapters();
                        }
                    });
                } else if (sharedMediaLayout != null) {
                    sharedMediaLayout.updateTabs(true);
                }
            }
        }
    }

    private void updateAutoDeleteItem() {
        if (autoDeleteItem == null || autoDeletePopupWrapper == null) {
            return;
        }
        int ttl = 0;
        if (viewModel.getState().getValue().getUserInfo() != null || viewModel.getState().getValue().getChatInfo() != null) {
            ttl = viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().ttl_period : viewModel.getState().getValue().getChatInfo().ttl_period;
        }
        autoDeleteItemDrawable.setTime(ttl);
        autoDeletePopupWrapper.updateItems(ttl);
    }

    private void updateTimeItem() {
        if (timerDrawable == null) {
            return;
        }
        final boolean fromMonoforum = previousTransitionFragment instanceof ChatActivity && ChatObject.isMonoForum(((ChatActivity) previousTransitionFragment).getCurrentChat());
        if (fromMonoforum) {
            timeItem.setTag(null);
            timeItem.setVisibility(View.GONE);
        } else if (viewModel.getState().getValue().getCurrentEncryptedChat() != null) {
            timerDrawable.setTime(viewModel.getState().getValue().getCurrentEncryptedChat().ttl);
            timeItem.setTag(1);
            timeItem.setVisibility(View.VISIBLE);
        } else if (viewModel.getState().getValue().getUserInfo() != null) {
            timerDrawable.setTime(viewModel.getState().getValue().getUserInfo().ttl_period);
            if (viewModel.getState().getValue().isNeedTimerImage() && viewModel.getState().getValue().getUserInfo().ttl_period != 0) {
                timeItem.setTag(1);
                timeItem.setVisibility(View.VISIBLE);
            } else {
                timeItem.setTag(null);
                timeItem.setVisibility(View.GONE);
            }
        } else if (viewModel.getState().getValue().getChatInfo() != null) {
            timerDrawable.setTime(viewModel.getState().getValue().getChatInfo().ttl_period);
            if (viewModel.getState().getValue().isNeedTimerImage() && viewModel.getState().getValue().getChatInfo().ttl_period != 0) {
                timeItem.setTag(1);
                timeItem.setVisibility(View.VISIBLE);
            } else {
                timeItem.setTag(null);
                timeItem.setVisibility(View.GONE);
            }
        } else {
            timeItem.setTag(null);
            timeItem.setVisibility(View.GONE);
        }
    }

    private void updateStar() {
        if (starBgItem == null || starFgItem == null) return;
        if (viewModel.getState().getValue().isNeedStarImage() && viewModel.getState().getValue().getCurrentChat() != null && (viewModel.getState().getValue().getCurrentChat().flags2 & 2048) != 0) {
            starFgItem.setTag(1);
            starFgItem.setVisibility(View.VISIBLE);
            starBgItem.setTag(1);
            starBgItem.setVisibility(View.VISIBLE);
        } else {
            starFgItem.setTag(null);
            starFgItem.setVisibility(View.GONE);
            starBgItem.setTag(null);
            starBgItem.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean needDelayOpenAnimation() {
        if (playProfileAnimation == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void mediaCountUpdated() {
        if (sharedMediaLayout != null && sharedMediaPreloader != null) {
            sharedMediaLayout.setNewMediaCounts(sharedMediaPreloader.getLastMediaCount());
        }
        updateSharedMediaRows();
        updateSelectedMediaTabText();

        if (viewModel.getState().getValue().getUserInfo() != null) {
            resumeDelayedFragmentAnimation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        if (sharedMediaLayout != null) {
            sharedMediaLayout.onResume();
        }
        invalidateIsInLandscapeMode();
        if (listAdapter != null) {
            // saveScrollPosition();
            firstLayout = true;
            listAdapter.notifyDataSetChanged();
        }
        if (!parentLayout.isInPreviewMode() && blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            blurredView.setVisibility(View.GONE);
            blurredView.setBackground(null);
        }

        if (imageUpdater != null) {
            imageUpdater.onResume();
            setParentActivityTitle(LocaleController.getString(R.string.Settings));
        }

        updateProfileData(true);
        fixLayout();
        if (nameTextView[1] != null) {
            setParentActivityTitle(nameTextView[1].getText());
        }
        if (viewModel.getArgs().userId != 0) {
            final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user != null && user.photo == null) {
                if (extraHeight >= AndroidUtilities.dp(88f)) {
                    expandAnimator.cancel();
                    expandAnimatorValues[0] = 1f;
                    expandAnimatorValues[1] = 0f;
                    setAvatarExpandProgress(1f);
                    avatarsViewPager.setVisibility(View.GONE);
                    extraHeight = AndroidUtilities.dp(88f);
                    viewModel.allowPullingDown(false);
                    layoutManager.scrollToPositionWithOffset(0, AndroidUtilities.dp(88) - listView.getPaddingTop());
                }
            }
        }
        if (flagSecure != null) {
            flagSecure.attach();
        }
        updateItemsUsername();
    }

    @Override
    public void onPause() {
        super.onPause();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        if (undoView != null) {
            undoView.hide(true, 0);
        }
        if (imageUpdater != null) {
            imageUpdater.onPause();
        }
        if (flagSecure != null) {
            flagSecure.detach();
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onPause();
        }
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        if (avatarsViewPager != null && avatarsViewPager.getVisibility() == View.VISIBLE && avatarsViewPager.getRealCount() > 1) {
            avatarsViewPager.getHitRect(rect);
            if (event != null && rect.contains((int) event.getX(), (int) event.getY() - actionBar.getMeasuredHeight())) {
                return false;
            }
        }
        if (sharedMediaRow == -1 || sharedMediaLayout == null) {
            return true;
        }
        if (!sharedMediaLayout.isSwipeBackEnabled()) {
            return false;
        }
        sharedMediaLayout.getHitRect(rect);
        if (event != null && !rect.contains((int) event.getX(), (int) event.getY() - actionBar.getMeasuredHeight())) {
            return true;
        }
        return sharedMediaLayout.isCurrentTabFirst();
    }

    @Override
    public boolean canBeginSlide() {
        if (!sharedMediaLayout.isSwipeBackEnabled()) {
            return false;
        }
        return super.canBeginSlide();
    }

    public UndoView getUndoView() {
        return undoView;
    }

    public boolean onBackPressed() {
        if (closeSheet()) {
            return false;
        }
        return actionBar.isEnabled() && (sharedMediaRow == -1 || sharedMediaLayout == null || !sharedMediaLayout.closeActionMode());
    }

    public boolean isSettings() {
        return imageUpdater != null && !viewModel.getArgs().myProfile;
    }

    @Override
    public void onBecomeFullyHidden() {
        if (undoView != null) {
            undoView.hide(true, 0);
        }
        super.onBecomeFullyHidden();
        fullyVisible = false;
    }

    private void updateSharedMediaRows() {
        if (listAdapter == null) {
            return;
        }
        updateListAnimated(false);
    }

    public boolean isFragmentOpened;

    @Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        isFragmentOpened = isOpen;
        if ((!isOpen && backward || isOpen && !backward) && playProfileAnimation != 0 && allowProfileAnimation && !isPulledDown) {
            openAnimationInProgress = true;
        }
        if (isOpen) {
            if (imageUpdater != null) {
                transitionIndex = getNotificationCenter().setAnimationInProgress(transitionIndex, new int[]{NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats, NotificationCenter.mediaCountDidLoad, NotificationCenter.mediaCountsDidLoad, NotificationCenter.userInfoDidLoad, NotificationCenter.needCheckSystemBarColors});
            } else {
                transitionIndex = getNotificationCenter().setAnimationInProgress(transitionIndex, new int[]{NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats, NotificationCenter.mediaCountDidLoad, NotificationCenter.mediaCountsDidLoad, NotificationCenter.needCheckSystemBarColors});
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !backward && getParentActivity() != null) {
                navigationBarAnimationColorFrom = getParentActivity().getWindow().getNavigationBarColor();
            }
        }
        transitionAnimationInProress = true;
        checkPhotoDescriptionAlpha();
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            if (!backward) {
                if (playProfileAnimation != 0 && allowProfileAnimation) {
                    if (playProfileAnimation == 1) {
                        currentExpandAnimatorValue = 0f;
                    }
                    openAnimationInProgress = false;
                    checkListViewScroll();
                    if (recreateMenuAfterAnimation) {
                        createActionBarMenu(true);
                    }
                }
                if (!fragmentOpened) {
                    fragmentOpened = true;
                    invalidateScroll = true;
                    fragmentView.requestLayout();
                }
            }
            getNotificationCenter().onAnimationFinish(transitionIndex);

            if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
                blurredView.setVisibility(View.GONE);
                blurredView.setBackground(null);
            }
        }
        transitionAnimationInProress = false;
        checkPhotoDescriptionAlpha();
    }

    private AboutLinkCell aboutLinkCell;

    @Keep
    public void setAvatarAnimationProgress(float progress) {
        avatarAnimationProgress = currentExpandAnimatorValue = progress;
        checkPhotoDescriptionAlpha();
        if (playProfileAnimation == 2) {
            avatarImage.setProgressToExpand(progress);
        }

        listView.setAlpha(progress);

        listView.setTranslationX(AndroidUtilities.dp(48) - AndroidUtilities.dp(48) * progress);

        int color;
        if (playProfileAnimation == 2 && avatarColor != 0) {
            color = avatarColor;
        } else {
            color = AvatarDrawable.getProfileBackColorForId(viewModel.getArgs().userId != 0 || ChatObject.isChannel(viewModel.getArgs().chatId, currentAccount) && !viewModel.getState().getValue().isCurrentChatMagagroup() ? 5 : viewModel.getArgs().chatId, resourcesProvider);
        }

        int actionBarColor = actionBarAnimationColorFrom != 0 ? actionBarAnimationColorFrom : getThemedColor(Theme.key_actionBarDefault);
        int actionBarColor2 = actionBarColor;
        if (SharedConfig.chatBlurEnabled()) {
            actionBarColor = ColorUtils.setAlphaComponent(actionBarColor, 0);
        }
        topView.setBackgroundColor(ColorUtils.blendARGB(actionBarColor, color, progress));
        timerDrawable.setBackgroundColor(ColorUtils.blendARGB(actionBarColor2, color, progress));

        color = AvatarDrawable.getIconColorForId(viewModel.getArgs().userId != 0 || ChatObject.isChannel(viewModel.getArgs().chatId, currentAccount) && !viewModel.getState().getValue().isCurrentChatMagagroup() ? 5 : viewModel.getArgs().chatId, resourcesProvider);
        int iconColor = peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon);
        actionBar.setItemsColor(ColorUtils.blendARGB(iconColor, color, avatarAnimationProgress), false);

        color = getThemedColor(Theme.key_profile_title);
        int titleColor = getThemedColor(Theme.key_actionBarDefaultTitle);
        for (int i = 0; i < 2; i++) {
            if (nameTextView[i] == null || i == 1 && playProfileAnimation == 2) {
                continue;
            }
            nameTextView[i].setTextColor(ColorUtils.blendARGB(titleColor, color, progress));
        }

        color = isOnline[0] ? getThemedColor(Theme.key_profile_status) : AvatarDrawable.getProfileTextColorForId(viewModel.getArgs().userId != 0 || ChatObject.isChannel(viewModel.getArgs().chatId, currentAccount) && !viewModel.getState().getValue().isCurrentChatMagagroup() ? 5 : viewModel.getArgs().chatId, resourcesProvider);
        int subtitleColor = getThemedColor(isOnline[0] ? Theme.key_chat_status : Theme.key_actionBarDefaultSubtitle);
        for (int i = 0; i < 3; i++) {
            if (onlineTextView[i] == null || i == 1 || i == 2 && playProfileAnimation == 2) {
                continue;
            }
            onlineTextView[i].setTextColor(ColorUtils.blendARGB(i == 0 ? subtitleColor : applyPeerColor(subtitleColor, true, isOnline[0]), i == 0 ? color : applyPeerColor(color, true, isOnline[0]), progress));
        }
        extraHeight = initialAnimationExtraHeight * progress;
        color = AvatarDrawable.getProfileColorForId(viewModel.getArgs().userId != 0 ? viewModel.getArgs().userId : viewModel.getArgs().chatId, resourcesProvider);
        int color2 = AvatarDrawable.getColorForId(viewModel.getArgs().userId != 0 ? viewModel.getArgs().userId : viewModel.getArgs().chatId);
        if (color != color2) {
            avatarDrawable.setColor(ColorUtils.blendARGB(color2, color, progress));
            avatarImage.invalidate();
        }

        if (navigationBarAnimationColorFrom != 0) {
            color = ColorUtils.blendARGB(navigationBarAnimationColorFrom, getNavigationBarColor(), progress);
            setNavigationBarColor(color);
        }

        topView.invalidate();

        needLayout(true);
        if (fragmentView != null) {
            fragmentView.invalidate();
        }

        if (aboutLinkCell != null) {
            aboutLinkCell.invalidate();
        }

        if (viewModel.getDialogId() > 0) {
            if (avatarImage != null) {
                avatarImage.setProgressToStoriesInsets(avatarAnimationProgress);
            }
            if (storyView != null) {
                storyView.setProgressToStoriesInsets(avatarAnimationProgress);
            }
            if (giftsView != null) {
                giftsView.setProgressToStoriesInsets(avatarAnimationProgress);
            }
        }
    }

    boolean profileTransitionInProgress;

    @Override
    public AnimatorSet onCustomTransitionAnimation(final boolean isOpen, final Runnable callback) {
        if (playProfileAnimation != 0 && allowProfileAnimation && !isPulledDown && !viewModel.getState().getValue().isDisableProfileAnimation()) {
            if (timeItem != null) {
                timeItem.setAlpha(1.0f);
            }
            if (starFgItem != null) {
                starFgItem.setAlpha(1.0f);
                starFgItem.setScaleX(1.0f);
                starFgItem.setScaleY(1.0f);
            }
            if (starBgItem != null) {
                starBgItem.setAlpha(1.0f);
                starBgItem.setScaleX(1.0f);
                starBgItem.setScaleY(1.0f);
            }
            previousTransitionMainFragment = null;
            if (parentLayout != null && parentLayout.getFragmentStack().size() >= 2) {
                BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
                if (fragment instanceof ChatActivityInterface) {
                    previousTransitionFragment = (ChatActivityInterface) fragment;
                }
                if (fragment instanceof DialogsActivity) {
                    DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                    if (dialogsActivity.rightSlidingDialogContainer != null && dialogsActivity.rightSlidingDialogContainer.currentFragment instanceof ChatActivityInterface) {
                        previousTransitionMainFragment = dialogsActivity;
                        previousTransitionFragment = (ChatActivityInterface) dialogsActivity.rightSlidingDialogContainer.currentFragment;
                    }
                }
            }
            final boolean fromChat = previousTransitionFragment instanceof ChatActivity && ((ChatActivity) previousTransitionFragment).getCurrentChat() != null;
            if (previousTransitionFragment != null) {
                updateTimeItem();
                updateStar();
            }
            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(playProfileAnimation == 2 ? 250 : 180);
            listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            ActionBarMenu menu = actionBar.createMenu();
            if (menu.getItem(10) == null) {
                if (animatingItem == null) {
                    animatingItem = menu.addItem(10, R.drawable.ic_ab_other);
                }
            }
            if (isOpen) {
                for (int i = 0; i < 2; i++) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) onlineTextView[i + 1].getLayoutParams();
                    layoutParams.rightMargin = (int) (-21 * AndroidUtilities.density + AndroidUtilities.dp(8));
                    onlineTextView[i + 1].setLayoutParams(layoutParams);
                }


                if (playProfileAnimation != 2) {
                    int width = (int) Math.ceil(AndroidUtilities.displaySize.x - AndroidUtilities.dp(118 + 8) + 21 * AndroidUtilities.density);
                    float width2 = nameTextView[1].getPaint().measureText(nameTextView[1].getText().toString()) * 1.12f + nameTextView[1].getSideDrawablesSize();
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
                    if (width < width2) {
                        layoutParams.width = (int) Math.ceil(width / 1.12f);
                    } else {
                        layoutParams.width = LayoutHelper.WRAP_CONTENT;
                    }
                    nameTextView[1].setLayoutParams(layoutParams);

                    initialAnimationExtraHeight = AndroidUtilities.dp(88f);
                } else {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) nameTextView[1].getLayoutParams();
                    layoutParams.width = (int) ((AndroidUtilities.displaySize.x - AndroidUtilities.dp(32)) / 1.67f);
                    nameTextView[1].setLayoutParams(layoutParams);
                }
                fragmentView.setBackgroundColor(0);
                setAvatarAnimationProgress(0);
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "avatarAnimationProgress", 0.0f, 1.0f));
                if (writeButton != null && writeButton.getTag() == null) {
                    writeButton.setScaleX(0.2f);
                    writeButton.setScaleY(0.2f);
                    writeButton.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.ALPHA, 1.0f));
                }
                if (playProfileAnimation == 2) {
                    avatarColor = getAverageColor(avatarImage.getImageReceiver());
                    nameTextView[1].setTextColor(Color.WHITE);
                    onlineTextView[1].setTextColor(0xB3FFFFFF);
                    actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, false);
                    if (showStatusButton != null) {
                        showStatusButton.setBackgroundColor(0x23ffffff);
                    }
                    overlaysView.setOverlaysVisible();
                }
                for (int a = 0; a < 2; a++) {
                    nameTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], View.ALPHA, a == 0 ? 0.0f : 1.0f));
                }
                if (storyView != null) {
                    if (viewModel.getDialogId() > 0) {
                        storyView.setAlpha(0f);
                        animators.add(ObjectAnimator.ofFloat(storyView, View.ALPHA, 1.0f));
                    } else {
                        storyView.setAlpha(1f);
                        storyView.setFragmentTransitionProgress(0);
                        animators.add(ObjectAnimator.ofFloat(storyView, ProfileStoriesView.FRAGMENT_TRANSITION_PROPERTY, 1.0f));
                    }
                }
                if (giftsView != null) {
                    giftsView.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(giftsView, View.ALPHA, 1.0f));
                }
                if (timeItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (starFgItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (starBgItem.getTag() != null) {
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.ALPHA, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_X, 1.0f, 0.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_Y, 1.0f, 0.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, View.ALPHA, 0.0f));
                }
                if (callItemVisible && (viewModel.getArgs().chatId != 0 || fromChat)) {
                    callItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, 1.0f));
                }
                if (videoCallItemVisible) {
                    videoCallItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, 1.0f));
                }
                if (editItemVisible) {
                    editItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, 1.0f));
                }
                if (ttlIconView.getTag() != null) {
                    ttlIconView.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(ttlIconView, View.ALPHA, 1.0f));
                }
                if (floatingButtonContainer != null) {
                    floatingButtonContainer.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(floatingButtonContainer, View.ALPHA, 1.0f));
                }
                if (avatarImage != null) {
                    avatarImage.setCrossfadeProgress(1.0f);
                    animators.add(ObjectAnimator.ofFloat(avatarImage, ProfileAvatarImageView.CROSSFADE_PROGRESS, 0.0f));
                }

                boolean onlineTextCrosafade = false;

                if (previousTransitionFragment != null) {
                    ChatAvatarContainer avatarContainer = previousTransitionFragment.getAvatarContainer();
                    if (avatarContainer != null && avatarContainer.getSubtitleTextView() instanceof SimpleTextView && ((SimpleTextView) avatarContainer.getSubtitleTextView()).getLeftDrawable() != null || avatarContainer.statusMadeShorter[0]) {
                        transitionOnlineText = avatarContainer.getSubtitleTextView();
                        avatarContainer2.invalidate();
                        onlineTextCrosafade = true;
                        onlineTextView[0].setAlpha(0f);
                        onlineTextView[1].setAlpha(0f);
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, 1.0f));
                    }
                }

                if (!onlineTextCrosafade) {
                    for (int a = 0; a < 2; a++) {
                        onlineTextView[a].setAlpha(a == 0 ? 1.0f : 0.0f);
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[a], View.ALPHA, a == 0 ? 0.0f : 1.0f));
                    }
                }
                animatorSet.playTogether(animators);
            } else {
                initialAnimationExtraHeight = extraHeight;
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofFloat(this, "avatarAnimationProgress", 1.0f, 0.0f));
                if (writeButton != null) {
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_X, 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.SCALE_Y, 0.2f));
                    animators.add(ObjectAnimator.ofFloat(writeButton, View.ALPHA, 0.0f));
                }
                for (int a = 0; a < 2; a++) {
                    animators.add(ObjectAnimator.ofFloat(nameTextView[a], View.ALPHA, a == 0 ? 1.0f : 0.0f));
                }
                if (storyView != null) {
                    if (viewModel.getArgs().dialogId > 0) {
                        animators.add(ObjectAnimator.ofFloat(storyView, View.ALPHA, 0.0f));
                    } else {
                        animators.add(ObjectAnimator.ofFloat(storyView, ProfileStoriesView.FRAGMENT_TRANSITION_PROPERTY, 0.0f));
                    }
                }
                if (giftsView != null) {
                    animators.add(ObjectAnimator.ofFloat(giftsView, View.ALPHA, 0.0f));
                }
                if (timeItem.getTag() != null) {
                    timeItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(timeItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (starFgItem.getTag() != null) {
                    starFgItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starFgItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (starBgItem.getTag() != null) {
                    starBgItem.setAlpha(0f);
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.ALPHA, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_X, 0.0f, 1.0f));
                    animators.add(ObjectAnimator.ofFloat(starBgItem, View.SCALE_Y, 0.0f, 1.0f));
                }
                if (animatingItem != null) {
                    animatingItem.setAlpha(0.0f);
                    animators.add(ObjectAnimator.ofFloat(animatingItem, View.ALPHA, 1.0f));
                }
                if (callItemVisible && (viewModel.getArgs().chatId != 0 || fromChat)) {
                    callItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(callItem, View.ALPHA, 0.0f));
                }
                if (videoCallItemVisible) {
                    videoCallItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(videoCallItem, View.ALPHA, 0.0f));
                }
                if (editItemVisible) {
                    editItem.setAlpha(1.0f);
                    animators.add(ObjectAnimator.ofFloat(editItem, View.ALPHA, 0.0f));
                }
                if (ttlIconView != null) {
                    animators.add(ObjectAnimator.ofFloat(ttlIconView, View.ALPHA, ttlIconView.getAlpha(), 0.0f));
                }
                if (floatingButtonContainer != null) {
                    animators.add(ObjectAnimator.ofFloat(floatingButtonContainer, View.ALPHA, 0.0f));
                }
                if (avatarImage != null) {
                    animators.add(ObjectAnimator.ofFloat(avatarImage, ProfileAvatarImageView.CROSSFADE_PROGRESS, 1.0f));
                }

                boolean crossfadeOnlineText = false;
                BaseFragment previousFragment = parentLayout.getFragmentStack().size() > 1 ? parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2) : null;
                if (previousFragment instanceof ChatActivity) {
                    ChatAvatarContainer avatarContainer = ((ChatActivity) previousFragment).getAvatarContainer();
                    View subtitleTextView = avatarContainer.getSubtitleTextView();
                    if (subtitleTextView instanceof SimpleTextView && ((SimpleTextView) subtitleTextView).getLeftDrawable() != null || avatarContainer.statusMadeShorter[0]) {
                        transitionOnlineText = avatarContainer.getSubtitleTextView();
                        avatarContainer2.invalidate();
                        crossfadeOnlineText = true;
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[0], View.ALPHA, 0.0f));
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[1], View.ALPHA, 0.0f));
                    }
                }
                if (!crossfadeOnlineText) {
                    for (int a = 0; a < 2; a++) {
                        animators.add(ObjectAnimator.ofFloat(onlineTextView[a], View.ALPHA, a == 0 ? 1.0f : 0.0f));
                    }
                }
                animatorSet.playTogether(animators);
                if (birthdayEffect != null) {
                    birthdayEffect.hide();
                }
            }
            profileTransitionInProgress = true;
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1f);
            valueAnimator.addUpdateListener(valueAnimator1 -> {
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
                updateStoriesViewBounds(true);
            });
            animatorSet.playTogether(valueAnimator);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (fragmentView == null) {
                        callback.run();
                        return;
                    }
                    avatarImage.setProgressToExpand(0);
                    listView.setLayerType(View.LAYER_TYPE_NONE, null);
                    if (animatingItem != null) {
                        ActionBarMenu menu = actionBar.createMenu();
                        menu.clearItems();
                        animatingItem = null;
                    }
                    callback.run();
                    if (playProfileAnimation == 2) {
                        playProfileAnimation = 1;
                        avatarImage.setForegroundAlpha(1.0f);
                        avatarContainer.setVisibility(View.GONE);
                        avatarsViewPager.resetCurrentItem();
                        avatarsViewPager.setVisibility(View.VISIBLE);
                    }
                    transitionOnlineText = null;
                    avatarContainer2.invalidate();
                    profileTransitionInProgress = false;
                    previousTransitionFragment = null;
                    previousTransitionMainFragment = null;
                    fragmentView.invalidate();
                }
            });
            animatorSet.setInterpolator(playProfileAnimation == 2 ? CubicBezierInterpolator.DEFAULT : new DecelerateInterpolator());

            AndroidUtilities.runOnUIThread(animatorSet::start, 50);
            return animatorSet;
        }
        return null;
    }

    private int getAverageColor(ImageReceiver imageReceiver) {
        if (imageReceiver.getDrawable() instanceof VectorAvatarThumbDrawable) {
            return ((VectorAvatarThumbDrawable)imageReceiver.getDrawable()).gradientTools.getAverageColor();
        }
        return AndroidUtilities.calcBitmapColor(avatarImage.getImageReceiver().getBitmap());
    }

    public void setChatInfo() {
        if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().migrated_from_chat_id != 0 && mergeDialogId == 0) {
            mergeDialogId = -viewModel.getState().getValue().getChatInfo().migrated_from_chat_id;
            getMediaDataController().getMediaCounts(mergeDialogId, viewModel.getArgs().topicId, classGuid);
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.setChatInfo(viewModel.getState().getValue().getChatInfo());
        }
        if (avatarsViewPager != null && !viewModel.getArgs().isTopic) {
            avatarsViewPager.setChatInfo(viewModel.getState().getValue().getChatInfo());
        }
        if (storyView != null && viewModel.getState().getValue().getChatInfo() != null) {
            storyView.setStories(viewModel.getState().getValue().getChatInfo().stories);
        }
        if (giftsView != null) {
            giftsView.update();
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        fetchUsersFromChannelInfo();
        if (viewModel != null && viewModel.getArgs().chatId != 0) {
            otherItem.setSubItemShown(ActionBarActions.gift_premium, !BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked() && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stargifts_available);
        }
    }

    private boolean needInsetForStories() {
        return getMessagesController().getStoriesController().hasStories(viewModel.getDialogId()) && !viewModel.getArgs().isTopic;
    }

    public void setFetcher(ProfileChannelCell.ChannelMessageFetcher channelMessageFetcher, ProfileBirthdayEffect.BirthdayEffectFetcher birthdayAssetsFetcher) {
        if (profileChannelMessageFetcher == null) {
            profileChannelMessageFetcher = channelMessageFetcher;
        }
        if (profileChannelMessageFetcher == null) {
            profileChannelMessageFetcher = new ProfileChannelCell.ChannelMessageFetcher(currentAccount);
        }
        profileChannelMessageFetcher.subscribe(() -> updateListAnimated(false));
        profileChannelMessageFetcher.fetch(viewModel.getState().getValue().getUserInfo());

        if (birthdayFetcher == null) {
            birthdayFetcher = birthdayAssetsFetcher;
        }
        if (birthdayFetcher == null) {
            birthdayFetcher = ProfileBirthdayEffect.BirthdayEffectFetcher.of(currentAccount, viewModel.getState().getValue().getUserInfo(), birthdayFetcher);
            createdBirthdayFetcher = birthdayFetcher != null;
        }
        if (birthdayFetcher != null) {
            birthdayFetcher.subscribe(this::createBirthdayEffect);
        }
    }

    public void setUserInfo() {
        if (storyView != null) {
            storyView.setStories(viewModel.getState().getValue().getUserInfo().stories);
        }
        if (giftsView != null) {
            giftsView.update();
        }
        if (avatarImage != null) {
            avatarImage.setHasStories(needInsetForStories());
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.setUserInfo(viewModel.getState().getValue().getUserInfo());
        }



        if (otherItem != null) {
            otherItem.setSubItemShown(ActionBarActions.start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(viewModel.getArgs().userId)));
            if (hasPrivacyCommand()) {
                otherItem.showSubItem(ActionBarActions.bot_privacy);
            } else {
                otherItem.hideSubItem(ActionBarActions.bot_privacy);
            }
        }
    }

    public boolean canSearchMembers() {
        return canSearchMembers;
    }

    private void fetchUsersFromChannelInfo() {
        // TODO do not call if viewmodel null
        if (viewModel == null || viewModel.getState().getValue().getCurrentChat() == null || !viewModel.getState().getValue().isCurrentChatMagagroup()) {
            return;
        }
        if (viewModel.getState().getValue().getChatInfo() instanceof TLRPC.TL_channelFull && viewModel.getState().getValue().getChatInfo().participants != null) {
            for (int a = 0; a < viewModel.getState().getValue().getChatInfo().participants.participants.size(); a++) {
                TLRPC.ChatParticipant chatParticipant = viewModel.getState().getValue().getChatInfo().participants.participants.get(a);
                // TODO : remove Update in view
                viewModel.getState().getValue().getParticipantsMap().put(chatParticipant.user_id, chatParticipant);
            }
        }
    }

    private void kickUser(long uid, TLRPC.ChatParticipant participant) {
        if (uid != 0) {
            TLRPC.User user = getMessagesController().getUser(uid);
            getMessagesController().deleteParticipantFromChat(viewModel.getArgs().chatId, user);
            if (viewModel.getState().getValue().getCurrentChat() != null && user != null && BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.createRemoveFromChatBulletin(this, user, viewModel.getState().getValue().getCurrentChat().title).show();
            }
            if (viewModel.getState().getValue().getChatInfo().participants.participants.remove(participant)) {
                updateListAnimated(true);
            }
        } else {
            getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);
            if (AndroidUtilities.isTablet()) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats, -viewModel.getArgs().chatId);
            } else {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            }
            getMessagesController().deleteParticipantFromChat(viewModel.getArgs().chatId, getMessagesController().getUser(getUserConfig().getClientUserId()));
            playProfileAnimation = 0;
            finishFragment();
        }
    }

    public boolean isChat() {
        return viewModel.getArgs().chatId != 0;
    }

    private void updateRowsIds() {
        // TODO remove early exit when truly responsive update
        if (viewModel == null)
            return;

        int prevRowsCount = rowCount;
        rowCount = 0;

        setAvatarRow = -1;
        setAvatarSectionRow = -1;
        numberSectionRow = -1;
        numberRow = -1;
        birthdayRow = -1;
        setUsernameRow = -1;
        bioRow = -1;
        channelRow = -1;
        channelDividerRow = -1;
        phoneSuggestionSectionRow = -1;
        phoneSuggestionRow = -1;
        passwordSuggestionSectionRow = -1;
        graceSuggestionRow = -1;
        graceSuggestionSectionRow = -1;
        passwordSuggestionRow = -1;
        settingsSectionRow = -1;
        settingsSectionRow2 = -1;
        notificationRow = -1;
        languageRow = -1;
        premiumRow = -1;
        starsRow = -1;
        tonRow = -1;
        businessRow = -1;
        premiumGiftingRow = -1;
        premiumSectionsRow = -1;
        privacyRow = -1;
        dataRow = -1;
        chatRow = -1;
        filtersRow = -1;
        liteModeRow = -1;
        stickersRow = -1;
        devicesRow = -1;
        devicesSectionRow = -1;
        helpHeaderRow = -1;
        questionRow = -1;
        faqRow = -1;
        policyRow = -1;
        helpSectionCell = -1;
        debugHeaderRow = -1;
        sendLogsRow = -1;
        sendLastLogsRow = -1;
        clearLogsRow = -1;
        switchBackendRow = -1;
        versionRow = -1;
        botAppRow = -1;
        botPermissionsHeader = -1;
        botPermissionBiometry = -1;
        botPermissionEmojiStatus = -1;
        botPermissionLocation = -1;
        botPermissionsDivider = -1;

        sendMessageRow = -1;
        reportRow = -1;
        reportReactionRow = -1;
        addToContactsRow = -1;
        emptyRow = -1;
        infoHeaderRow = -1;
        phoneRow = -1;
        userInfoRow = -1;
        locationRow = -1;
        channelInfoRow = -1;
        usernameRow = -1;
        settingsTimerRow = -1;
        settingsKeyRow = -1;
        notificationsDividerRow = -1;
        reportDividerRow = -1;
        notificationsRow = -1;
        bizLocationRow = -1;
        bizHoursRow = -1;
        infoSectionRow = -1;
        affiliateRow = -1;
        infoAffiliateRow = -1;
        secretSettingsSectionRow = -1;
        bottomPaddingRow = -1;
        addToGroupButtonRow = -1;
        addToGroupInfoRow = -1;
        infoStartRow = -1;
        infoEndRow = -1;

        membersHeaderRow = -1;
        membersStartRow = -1;
        membersEndRow = -1;
        addMemberRow = -1;
        subscribersRow = -1;
        subscribersRequestsRow = -1;
        administratorsRow = -1;
        blockedUsersRow = -1;
        membersSectionRow = -1;
        channelBalanceSectionRow = -1;
        sharedMediaRow = -1;
        notificationsSimpleRow = -1;
        settingsRow = -1;
        botStarsBalanceRow = -1;
        botTonBalanceRow = -1;
        channelBalanceRow = -1;
        balanceDividerRow = -1;

        unblockRow = -1;
        joinRow = -1;
        lastSectionRow = -1;
        visibleChatParticipants.clear();
        visibleSortedUsers.clear();

        boolean hasMedia = false;
        if (sharedMediaPreloader != null) {
            int[] lastMediaCount = sharedMediaPreloader.getLastMediaCount();
            for (int a = 0; a < lastMediaCount.length; a++) {
                if (lastMediaCount[a] > 0) {
                    hasMedia = true;
                    break;
                }
            }
            if (!hasMedia) {
                hasMedia = sharedMediaPreloader.hasSavedMessages;
            }
            if (!hasMedia) {
                hasMedia = sharedMediaPreloader.hasPreviews;
            }
        }
        if (!hasMedia && viewModel.getState().getValue().getUserInfo() != null) {
            hasMedia = viewModel.getState().getValue().getUserInfo().stories_pinned_available;
        }
        if (!hasMedia && viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().bot_info != null) {
            hasMedia = viewModel.getState().getValue().getUserInfo().bot_info.has_preview_medias;
        }
        if (!hasMedia && (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().stargifts_count > 0 || viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stargifts_count > 0)) {
            hasMedia = true;
        }
        if (!hasMedia && viewModel.getState().getValue().getChatInfo() != null) {
            hasMedia = viewModel.getState().getValue().getChatInfo().stories_pinned_available;
        }
        if (!hasMedia) {
            if (viewModel.getArgs().chatId != 0 && MessagesController.ChannelRecommendations.hasRecommendations(currentAccount, -viewModel.getArgs().chatId)) {
                hasMedia = true;
            } else if (viewModel.getState().getValue().isBot() && viewModel.getArgs().userId != 0 && MessagesController.ChannelRecommendations.hasRecommendations(currentAccount, viewModel.getArgs().userId)) {
                hasMedia = true;
            }
        }

        if (viewModel.getArgs().userId != 0) {
            if (LocaleController.isRTL) {
                emptyRow = rowCount++;
            }
            TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);

            if (UserObject.isUserSelf(user) && !viewModel.getArgs().myProfile) {
                if (viewModel.getState().getValue().getAvatarBig() == null && (user.photo == null || !(user.photo.photo_big instanceof TLRPC.TL_fileLocation_layer97) && !(user.photo.photo_big instanceof TLRPC.TL_fileLocationToBeDeprecated)) && (avatarsViewPager == null || avatarsViewPager.getRealCount() == 0)) {
                    setAvatarRow = rowCount++;
                    setAvatarSectionRow = rowCount++;
                }
                numberSectionRow = rowCount++;
                numberRow = rowCount++;
                setUsernameRow = rowCount++;
                bioRow = rowCount++;

                settingsSectionRow = rowCount++;

                Set<String> suggestions = getMessagesController().pendingSuggestions;
                if (suggestions.contains("PREMIUM_GRACE")) {
                    graceSuggestionRow = rowCount++;
                    graceSuggestionSectionRow = rowCount++;
                } else if (suggestions.contains("VALIDATE_PHONE_NUMBER")) {
                    phoneSuggestionRow = rowCount++;
                    phoneSuggestionSectionRow = rowCount++;
                } else if (suggestions.contains("VALIDATE_PASSWORD")) {
                    passwordSuggestionRow = rowCount++;
                    passwordSuggestionSectionRow = rowCount++;
                }

                settingsSectionRow2 = rowCount++;
                chatRow = rowCount++;
                privacyRow = rowCount++;
                notificationRow = rowCount++;
                dataRow = rowCount++;
                liteModeRow = rowCount++;
//                stickersRow = rowCount++;
                if (getMessagesController().filtersEnabled || !getMessagesController().dialogFilters.isEmpty()) {
                    filtersRow = rowCount++;
                }
                devicesRow = rowCount++;
                languageRow = rowCount++;
                devicesSectionRow = rowCount++;
                if (!getMessagesController().premiumFeaturesBlocked()) {
                    premiumRow = rowCount++;
                }
                if (getMessagesController().starsPurchaseAvailable()) {
                    starsRow = rowCount++;
                }
                if (ApplicationLoader.isBetaBuild() || ApplicationLoader.isStandaloneBuild() || ApplicationLoader.isHuaweiStoreBuild() || (StarsController.getInstance(currentAccount, true).balanceAvailable() && (StarsController.getInstance(currentAccount, true).hasTransactions() || StarsController.getInstance(currentAccount, true).getBalance().positive()))) {
                    tonRow = rowCount++;
                }
                if (!getMessagesController().premiumFeaturesBlocked()) {
                    businessRow = rowCount++;
                }
                if (!getMessagesController().premiumPurchaseBlocked()) {
                    premiumGiftingRow = rowCount++;
                }
                if (premiumRow >= 0 || starsRow >= 0 || tonRow >= 0 || businessRow >= 0 || premiumGiftingRow >= 0) {
                    premiumSectionsRow = rowCount++;
                }
                helpHeaderRow = rowCount++;
                questionRow = rowCount++;
                faqRow = rowCount++;
                policyRow = rowCount++;
                if (BuildVars.LOGS_ENABLED || BuildVars.DEBUG_PRIVATE_VERSION) {
                    helpSectionCell = rowCount++;
                    debugHeaderRow = rowCount++;
                }
                if (BuildVars.LOGS_ENABLED) {
                    sendLogsRow = rowCount++;
                    sendLastLogsRow = rowCount++;
                    clearLogsRow = rowCount++;
                }
                if (BuildVars.DEBUG_VERSION) {
                    switchBackendRow = rowCount++;
                }
                versionRow = rowCount++;
            } else {
                String username = UserObject.getPublicUsername(user);
                boolean hasInfo = viewModel.getState().getValue().getUserInfo() != null && !TextUtils.isEmpty(viewModel.getState().getValue().getUserInfo().about) || user != null && !TextUtils.isEmpty(username);
                boolean hasPhone = user != null && (!TextUtils.isEmpty(user.phone) || !TextUtils.isEmpty(viewModel.getArgs().vcardPhone));

                if (viewModel.getState().getValue().getUserInfo() != null && (viewModel.getState().getValue().getUserInfo().flags2 & 64) != 0 && (profileChannelMessageFetcher == null || !profileChannelMessageFetcher.loaded || profileChannelMessageFetcher.messageObject != null)) {
                    final TLRPC.Chat channel = getMessagesController().getChat(viewModel.getState().getValue().getUserInfo().personal_channel_id);
                    if (channel != null && (ChatObject.isPublic(channel) || !ChatObject.isNotInChat(channel))) {
                        channelRow = rowCount++;
                        channelDividerRow = rowCount++;
                    }
                }
                infoStartRow = rowCount;
                infoHeaderRow = rowCount++;
                if (!viewModel.getState().getValue().isBot() && (hasPhone || !hasInfo)) {
                    phoneRow = rowCount++;
                }
                if (viewModel.getState().getValue().getUserInfo() != null && !TextUtils.isEmpty(viewModel.getState().getValue().getUserInfo().about)) {
                    userInfoRow = rowCount++;
                }
                if (user != null && username != null) {
                    usernameRow = rowCount++;
                }
                if (viewModel.getState().getValue().getUserInfo() != null) {
                    if (viewModel.getState().getValue().getUserInfo().birthday != null) {
                        birthdayRow = rowCount++;
                    }
                    if (viewModel.getState().getValue().getUserInfo().business_work_hours != null) {
                        bizHoursRow = rowCount++;
                    }
                    if (viewModel.getState().getValue().getUserInfo().business_location != null) {
                        bizLocationRow = rowCount++;
                    }
                }
//                if (phoneRow != -1 || userInfoRow != -1 || usernameRow != -1 || bizHoursRow != -1 || bizLocationRow != -1) {
//                    notificationsDividerRow = rowCount++;
//                }
                if (viewModel.getArgs().userId != getUserConfig().getClientUserId()) {
                    notificationsRow = rowCount++;
                }
                if (viewModel.getState().getValue().isBot() && user != null && user.bot_has_main_app) {
                    botAppRow = rowCount++;
                }
                infoEndRow = rowCount - 1;
                infoSectionRow = rowCount++;

                if (viewModel.getState().getValue().isBot() && viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().starref_program != null && (viewModel.getState().getValue().getUserInfo().starref_program.flags & 2) == 0 && getMessagesController().starrefConnectAllowed) {
                    affiliateRow = rowCount++;
                    infoAffiliateRow = rowCount++;
                }

                if (viewModel.getState().getValue().isBot()) {
                    if (botLocation == null && getContext() != null) botLocation = BotLocation.get(getContext(), currentAccount, viewModel.getArgs().userId);
                    if (botBiometry == null && getContext() != null) botBiometry = BotBiometry.get(getContext(), currentAccount, viewModel.getArgs().userId);
                    final boolean containsPermissionLocation = botLocation != null && botLocation.asked();
                    final boolean containsPermissionBiometry = botBiometry != null && botBiometry.asked();
                    final boolean containsPermissionEmojiStatus = viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().bot_can_manage_emoji_status || SetupEmojiStatusSheet.getAccessRequested(getContext(), currentAccount, viewModel.getArgs().userId);

                    if (containsPermissionEmojiStatus || containsPermissionLocation || containsPermissionBiometry) {
                        botPermissionsHeader = rowCount++;
                        if (containsPermissionEmojiStatus) {
                            botPermissionEmojiStatus = rowCount++;
                        }
                        if (containsPermissionLocation) {
                            botPermissionLocation = rowCount++;
                        }
                        if (containsPermissionBiometry) {
                            botPermissionBiometry = rowCount++;
                        }
                        botPermissionsDivider = rowCount++;
                    }
                }

                if (viewModel.getState().getValue().getCurrentEncryptedChat() instanceof TLRPC.TL_encryptedChat) {
                    settingsTimerRow = rowCount++;
                    settingsKeyRow = rowCount++;
                    secretSettingsSectionRow = rowCount++;
                }

                if (user != null && !viewModel.getState().getValue().isBot() && viewModel.getState().getValue().getCurrentEncryptedChat() == null && user.id != getUserConfig().getClientUserId()) {
                    if (viewModel.getState().getValue().isUserBlocked()) {
                        unblockRow = rowCount++;
                        lastSectionRow = rowCount++;
                    }
                }


                boolean divider = false;
                if (user != null && user.bot) {
                    if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().can_view_revenue && BotStarsController.getInstance(currentAccount).getTONBalance(viewModel.getArgs().userId) > 0) {
                        botTonBalanceRow = rowCount++;
                    }
                    if (BotStarsController.getInstance(currentAccount).getBotStarsBalance(viewModel.getArgs().userId).amount > 0 || BotStarsController.getInstance(currentAccount).hasTransactions(viewModel.getArgs().userId)) {
                        botStarsBalanceRow = rowCount++;
                    }
                }

                if (user != null && viewModel.getState().getValue().isBot() && !user.bot_nochats) {
                    addToGroupButtonRow = rowCount++;
                    addToGroupInfoRow = rowCount++;
                } else if (botStarsBalanceRow >= 0) {
                    divider = true;
                }

                if (!viewModel.getArgs().myProfile && viewModel.getArgs().showAddToContacts && user != null && !user.contact && !user.bot && !UserObject.isService(user.id)) {
                    addToContactsRow = rowCount++;
                    divider = true;
                }
                if (!viewModel.getArgs().myProfile && viewModel.getArgs().reportReactionMessageId != 0 && !ContactsController.getInstance(currentAccount).isContact(viewModel.getArgs().userId)) {
                    reportReactionRow = rowCount++;
                    divider = true;
                }
                if (divider) {
                    reportDividerRow = rowCount++;
                }

                if (hasMedia || (user != null && user.bot && user.bot_can_edit) || viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().common_chats_count != 0 || viewModel.getArgs().myProfile) {
                    sharedMediaRow = rowCount++;
                } else if (lastSectionRow == -1 && viewModel.getState().getValue().isNeedSendMessage()) {
                    sendMessageRow = rowCount++;
                    reportRow = rowCount++;
                    lastSectionRow = rowCount++;
                }
            }
        } else if (viewModel.getArgs().isTopic) {
            infoHeaderRow = rowCount++;
            usernameRow = rowCount++;
            notificationsSimpleRow = rowCount++;
            infoSectionRow = rowCount++;
            if (hasMedia) {
                sharedMediaRow = rowCount++;
            }
        } else if (viewModel.getArgs().chatId != 0) {
            if (viewModel.getState().getValue().getChatInfo() != null && (!TextUtils.isEmpty(viewModel.getState().getValue().getChatInfo().about) || viewModel.getState().getValue().getChatInfo().location instanceof TLRPC.TL_channelLocation) || ChatObject.isPublic(viewModel.getState().getValue().getCurrentChat())) {
                if (LocaleController.isRTL && ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && viewModel.getState().getValue().getChatInfo() != null && !viewModel.getState().getValue().isCurrentChatMagagroup() && viewModel.getState().getValue().getChatInfo().linked_chat_id != 0) {
                    emptyRow = rowCount++;
                }
                infoHeaderRow = rowCount++;
                if (viewModel.getState().getValue().getChatInfo() != null) {
                    if (!TextUtils.isEmpty(viewModel.getState().getValue().getChatInfo().about)) {
                        channelInfoRow = rowCount++;
                    }
                    if (viewModel.getState().getValue().getChatInfo().location instanceof TLRPC.TL_channelLocation) {
                        locationRow = rowCount++;
                    }
                }
                if (ChatObject.isPublic(viewModel.getState().getValue().getCurrentChat())) {
                    usernameRow = rowCount++;
                }
            }
//            if (infoHeaderRow != -1) {
//                notificationsDividerRow = rowCount++;
//            }
            notificationsRow = rowCount++;
            infoSectionRow = rowCount++;

            if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup()) {
                if (viewModel.getState().getValue().getChatInfo() != null && (viewModel.getState().getValue().getCurrentChat().creator || viewModel.getState().getValue().getChatInfo().can_view_participants)) {
                    membersHeaderRow = rowCount++;
                    subscribersRow = rowCount++;
                    if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().requests_pending > 0) {
                        subscribersRequestsRow = rowCount++;
                    }
                    administratorsRow = rowCount++;
                    if (viewModel.getState().getValue().getChatInfo() != null && (viewModel.getState().getValue().getChatInfo().banned_count != 0 || viewModel.getState().getValue().getChatInfo().kicked_count != 0)) {
                        blockedUsersRow = rowCount++;
                    }
                    if (
                        viewModel.getState().getValue().getChatInfo() != null &&
                        viewModel.getState().getValue().getChatInfo().can_view_stars_revenue && (
                            BotStarsController.getInstance(currentAccount).getBotStarsBalance(-viewModel.getArgs().chatId).amount > 0 ||
                            BotStarsController.getInstance(currentAccount).hasTransactions(-viewModel.getArgs().chatId)
                        ) ||
                        viewModel.getState().getValue().getChatInfo() != null &&
                        viewModel.getState().getValue().getChatInfo().can_view_revenue &&
                        BotStarsController.getInstance(currentAccount).getTONBalance(-viewModel.getArgs().chatId) > 0
                    ) {
                        channelBalanceRow = rowCount++;
                    }
                    settingsRow = rowCount++;
                    channelBalanceSectionRow = rowCount++;
                }
            } else {
                if (
                    viewModel.getState().getValue().getChatInfo() != null &&
                        viewModel.getState().getValue().getChatInfo().can_view_stars_revenue && (
                        BotStarsController.getInstance(currentAccount).getBotStarsBalance(-viewModel.getArgs().chatId).amount > 0 ||
                        BotStarsController.getInstance(currentAccount).hasTransactions(-viewModel.getArgs().chatId)
                    ) ||
                    viewModel.getState().getValue().getChatInfo() != null &&
                    viewModel.getState().getValue().getChatInfo().can_view_revenue &&
                    BotStarsController.getInstance(currentAccount).getTONBalance(-viewModel.getArgs().chatId) > 0
                ) {
                    channelBalanceRow = rowCount++;
                    channelBalanceSectionRow = rowCount++;
                }
            }

            if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat())) {
                if (!viewModel.getArgs().isTopic && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().isCurrentChatMagagroup() && viewModel.getState().getValue().getChatInfo().participants != null && viewModel.getState().getValue().getChatInfo().participants.participants != null && !viewModel.getState().getValue().getChatInfo().participants.participants.isEmpty()) {
                    if (!ChatObject.isNotInChat(viewModel.getState().getValue().getCurrentChat()) && ChatObject.canAddUsers(viewModel.getState().getValue().getCurrentChat()) && viewModel.getState().getValue().getChatInfo().participants_count < getMessagesController().maxMegagroupCount) {
                        addMemberRow = rowCount++;
                    }
                    int count = viewModel.getState().getValue().getChatInfo().participants.participants.size();
                    if ((count <= 5 || !hasMedia || usersForceShowingIn == 1) && usersForceShowingIn != 2) {
                        if (addMemberRow == -1) {
                            membersHeaderRow = rowCount++;
                        }
                        membersStartRow = rowCount;
                        rowCount += count;
                        membersEndRow = rowCount;
                        membersSectionRow = rowCount++;
                        visibleChatParticipants.addAll(viewModel.getState().getValue().getChatInfo().participants.participants);
                        if (viewModel.getState().getValue().getSortedUsers() != null) {
                            visibleSortedUsers.addAll(viewModel.getState().getValue().getSortedUsers());
                        }
                        usersForceShowingIn = 1;
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(null, null);
                        }
                    } else {
                        if (addMemberRow != -1) {
                            membersSectionRow = rowCount++;
                        }
                        if (sharedMediaLayout != null) {
                            if (!viewModel.getState().getValue().getSortedUsers().isEmpty()) {
                                usersForceShowingIn = 2;
                            }
                            sharedMediaLayout.setChatUsers(viewModel.getState().getValue().getSortedUsers(), viewModel.getState().getValue().getChatInfo());
                        }
                    }
                } else {
                    if (!ChatObject.isNotInChat(viewModel.getState().getValue().getCurrentChat()) && ChatObject.canAddUsers(viewModel.getState().getValue().getCurrentChat()) && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants_hidden) {
                        addMemberRow = rowCount++;
                        membersSectionRow = rowCount++;
                    }
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.updateAdapters();
                    }
                }

                if (lastSectionRow == -1 && viewModel.getState().getValue().getCurrentChat().left && !viewModel.getState().getValue().getCurrentChat().kicked) {
                    long requestedTime = MessagesController.getNotificationsSettings(currentAccount).getLong("dialog_join_requested_time_" + viewModel.getArgs().dialogId, -1);
                    if (!(requestedTime > 0 && System.currentTimeMillis() - requestedTime < 1000 * 60 * 2)) {
                        joinRow = rowCount++;
                        lastSectionRow = rowCount++;
                    }
                }
            } else if (viewModel.getState().getValue().getChatInfo() != null) {
                if (!viewModel.getArgs().isTopic && viewModel.getState().getValue().getChatInfo().participants != null && viewModel.getState().getValue().getChatInfo().participants.participants != null && !(viewModel.getState().getValue().getChatInfo().participants instanceof TLRPC.TL_chatParticipantsForbidden)) {
                    if (ChatObject.canAddUsers(viewModel.getState().getValue().getCurrentChat()) || viewModel.getState().getValue().getCurrentChat().default_banned_rights == null || !viewModel.getState().getValue().getCurrentChat().default_banned_rights.invite_users) {
                        addMemberRow = rowCount++;
                    }
                    int count = viewModel.getState().getValue().getChatInfo().participants.participants.size();
                    if (count <= 5 || !hasMedia) {
                        if (addMemberRow == -1) {
                            membersHeaderRow = rowCount++;
                        }
                        membersStartRow = rowCount;
                        rowCount += viewModel.getState().getValue().getChatInfo().participants.participants.size();
                        membersEndRow = rowCount;
                        membersSectionRow = rowCount++;
                        visibleChatParticipants.addAll(viewModel.getState().getValue().getChatInfo().participants.participants);
                        if (viewModel.getState().getValue().getSortedUsers() != null) {
                            visibleSortedUsers.addAll(viewModel.getState().getValue().getSortedUsers());
                        }
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(null, null);
                        }
                    } else {
                        if (addMemberRow != -1) {
                            membersSectionRow = rowCount++;
                        }
                        if (sharedMediaLayout != null) {
                            sharedMediaLayout.setChatUsers(viewModel.getState().getValue().getSortedUsers(), viewModel.getState().getValue().getChatInfo());
                        }
                    }
                } else {
                    if (!ChatObject.isNotInChat(viewModel.getState().getValue().getCurrentChat()) && ChatObject.canAddUsers(viewModel.getState().getValue().getCurrentChat()) && viewModel.getState().getValue().getChatInfo().participants_hidden) {
                        addMemberRow = rowCount++;
                        membersSectionRow = rowCount++;
                    }
                    if (sharedMediaLayout != null) {
                        sharedMediaLayout.updateAdapters();
                    }
                }
            }

            if (hasMedia) {
                sharedMediaRow = rowCount++;
            }
        }
        if (sharedMediaRow == -1) {
            bottomPaddingRow = rowCount++;
        }
        final int actionBarHeight = actionBar != null ? ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) : 0;
        if (listView == null || prevRowsCount > rowCount || listContentHeight != 0 && listContentHeight + actionBarHeight + AndroidUtilities.dp(88) < listView.getMeasuredHeight()) {
            lastMeasuredContentWidth = 0;
        }
        if (listView != null) {
            listView.setTranslateSelectorPosition(bizHoursRow);
        }
    }

    private Drawable getScamDrawable(int type) {
        if (scamDrawable == null) {
            scamDrawable = new ScamDrawable(11, type);
            scamDrawable.setColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue));
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
                verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
                color = Color.WHITE;
                verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_windowBackgroundWhite), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
            }
            verifiedCrossfadeDrawable[a] = new CrossfadeDrawable(
                new CombinedDrawable(verifiedDrawable[a], verifiedCheckDrawable[a]),
                ContextCompat.getDrawable(getParentActivity(), R.drawable.verified_profile)
            );
        }
        return verifiedCrossfadeDrawable[a];
    }

    private Drawable getPremiumCrossfadeDrawable(int a) {
        if (premiumCrossfadeDrawable[a] == null) {
            premiumStarDrawable[a] = ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_liststar).mutate();
            int color = getThemedColor(Theme.key_profile_verifiedBackground);
            if (a == 1) {
                color = dontApplyPeerColor(color);
            }
            premiumStarDrawable[a].setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            premiumCrossfadeDrawable[a] = new CrossfadeDrawable(premiumStarDrawable[a], ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_prolfilestar).mutate());
        }
        return premiumCrossfadeDrawable[a];
    }
    private Drawable getBotVerificationDrawable(long icon, boolean animated, int a) {
        if (botVerificationDrawable[a] == null) {
            botVerificationDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(17), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            botVerificationDrawable[a].offset(0, dp(1));
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
            emojiStatusDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(24), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
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
                fromColor = AndroidUtilities.getOffsetColor(getThemedColor(Theme.key_profile_verifiedBackground), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f);
            }
            final int color = ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0xffffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress);
            if (emojiStatusDrawable[a] != null) {
                emojiStatusDrawable[a].setColor(color);
            }
            if (botVerificationDrawable[a] != null) {
                botVerificationDrawable[a].setColor(ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0x99ffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress));
            }
            if (a == 1) {
                animatedStatusView.setColor(color);
            }
        }
        lastEmojiStatusProgress = progress;
    }

    private void updateEmojiStatusEffectPosition() {
        animatedStatusView.setScaleX(nameTextView[1].getScaleX());
        animatedStatusView.setScaleY(nameTextView[1].getScaleY());
        animatedStatusView.translate(
            nameTextView[1].getX() + nameTextView[1].getRightDrawableX() * nameTextView[1].getScaleX(),
            nameTextView[1].getY() + (nameTextView[1].getHeight() - (nameTextView[1].getHeight() - nameTextView[1].getRightDrawableY()) * nameTextView[1].getScaleY())
        );
    }

    private MessagesController.PeerColor peerColor;

    public void updateProfileData(boolean reload) {
        if (avatarContainer == null || nameTextView == null || getParentActivity() == null) {
            return;
        }
        String onlineTextOverride;
        int currentConnectionState = getConnectionsManager().getConnectionState();
        if (currentConnectionState == ConnectionsManager.ConnectionStateWaitingForNetwork) {
            onlineTextOverride = LocaleController.getString(R.string.WaitingForNetwork);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnecting) {
            onlineTextOverride = LocaleController.getString(R.string.Connecting);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateUpdating) {
            onlineTextOverride = LocaleController.getString(R.string.Updating);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnectingToProxy) {
            onlineTextOverride = LocaleController.getString(R.string.ConnectingToProxy);
        } else {
            onlineTextOverride = null;
        }

        BaseFragment prevFragment = null;
        if (parentLayout != null && parentLayout.getFragmentStack().size() >= 2) {
            BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
            if (fragment instanceof ChatActivityInterface) {
                prevFragment = fragment;
            }
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                if (dialogsActivity.rightSlidingDialogContainer != null && dialogsActivity.rightSlidingDialogContainer.currentFragment instanceof ChatActivityInterface) {
                    previousTransitionMainFragment = dialogsActivity;
                    prevFragment = dialogsActivity.rightSlidingDialogContainer.currentFragment;
                }
            }
        }
        final boolean copyFromChatActivity = prevFragment instanceof ChatActivity && ((ChatActivity) prevFragment).avatarContainer != null && ((ChatActivity) prevFragment).getChatMode() == ChatActivity.MODE_SUGGESTIONS;

        TLRPC.TL_forumTopic topic = null;
        boolean shortStatus;

        viewModel.hasFallbackPhoto(false);
        hasCustomPhoto = false;
        if (viewModel.getArgs().userId != 0) {
            TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user == null) {
                return;
            }
            shortStatus = user.photo != null && user.photo.personal;
            TLRPC.FileLocation photoBig = null;
            if (user.photo != null) {
                photoBig = user.photo.photo_big;
            }
            avatarDrawable.setInfo(currentAccount, user);

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(user.emoji_status);
            if (peerColor == null) {
                final int colorId = UserObject.getProfileColorId(user);
                final MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }
            if (topView != null) {
                topView.setBackgroundEmojiId(UserObject.getProfileEmojiId(user), user != null && user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible, true);
            }
            setCollectibleGiftStatus(user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible ? (TLRPC.TL_emojiStatusCollectible) user.emoji_status : null);

            final ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
            final ImageLocation thumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL);
            final ImageLocation videoThumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_VIDEO_BIG);
            VectorAvatarThumbDrawable vectorAvatarThumbDrawable = null;
            TLRPC.VideoSize vectorAvatar = null;
            if (viewModel.getState().getValue().getUserInfo() != null) {
                vectorAvatar = FileLoader.getVectorMarkupVideoSize(user.photo != null && user.photo.personal ? viewModel.getState().getValue().getUserInfo().personal_photo : viewModel.getState().getValue().getUserInfo().profile_photo);
                if (vectorAvatar != null) {
                    vectorAvatarThumbDrawable = new VectorAvatarThumbDrawable(vectorAvatar, user.premium, VectorAvatarThumbDrawable.TYPE_PROFILE);
                }
            }
            final ImageLocation videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            if (viewModel.getState().getValue().getAvatar() == null) {
                avatarsViewPager.initIfEmpty(vectorAvatarThumbDrawable, imageLocation, thumbLocation, reload);
            }
            if (viewModel.getState().getValue().getAvatarBig() == null) {
                if (vectorAvatar != null) {
                    avatarImage.setImageDrawable(vectorAvatarThumbDrawable);
                } else if (videoThumbLocation != null && !user.photo.personal) {
                    avatarImage.getImageReceiver().setVideoThumbIsSame(true);
                    avatarImage.setImage(videoThumbLocation, "avatar", thumbLocation, "50_50", avatarDrawable, user);
                } else {
                    avatarImage.setImage(videoLocation, ImageLoader.AUTOPLAY_FILTER, thumbLocation, "50_50", avatarDrawable, user);
                }
            }

            if (thumbLocation != null && setAvatarRow != -1 || thumbLocation == null && setAvatarRow == -1) {
                updateListAnimated(false);
                needLayout(true);
            }
            if (imageLocation != null && (prevLoadedImageLocation == null || imageLocation.photoId != prevLoadedImageLocation.photoId)) {
                prevLoadedImageLocation = imageLocation;
                getFileLoader().loadFile(imageLocation, user, null, FileLoader.PRIORITY_LOW, 1);
            }

            CharSequence newString = UserObject.getUserName(user);
            String newString2;
            boolean hiddenStatusButton = false;
            if (user.id == getUserConfig().getClientUserId()) {
                if (UserObject.hasFallbackPhoto(viewModel.getState().getValue().getUserInfo())) {
                    newString2 = "";
                    viewModel.hasFallbackPhoto(true);
                    TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(viewModel.getState().getValue().getUserInfo().fallback_photo.sizes, 1000);
                    if (smallSize != null) {
                        fallbackImage.setImage(ImageLocation.getForPhoto(smallSize, viewModel.getState().getValue().getUserInfo().fallback_photo), "50_50", (Drawable) null, 0, null, UserConfig.getInstance(currentAccount).getCurrentUser(), 0);
                    }
                } else {
                    newString2 = LocaleController.getString(R.string.Online);
                }
            } else if (user.id == UserObject.VERIFY) {
                newString2 = LocaleController.getString(R.string.VerifyCodesNotifications);
            } else if (user.id == 333000 || user.id == 777000 || user.id == 42777) {
                newString2 = LocaleController.getString(R.string.ServiceNotifications);
            } else if (MessagesController.isSupportUser(user)) {
                newString2 = LocaleController.getString(R.string.SupportStatus);
            } else if (viewModel.getState().getValue().isBot()) {
                if (user.bot_active_users != 0) {
                    newString2 = LocaleController.formatPluralStringComma("BotUsers", user.bot_active_users, ',');
                } else {
                    newString2 = LocaleController.getString(R.string.Bot);
                }
            } else {
                isOnline[0] = false;
                newString2 = LocaleController.formatUserStatus(currentAccount, user, isOnline, shortStatus ? new boolean[1] : null);
                hiddenStatusButton = user != null && !isOnline[0] && !getUserConfig().isPremium() && user.status != null && (user.status instanceof TLRPC.TL_userStatusRecently || user.status instanceof TLRPC.TL_userStatusLastMonth || user.status instanceof TLRPC.TL_userStatusLastWeek) && user.status.by_me;
                if (onlineTextView[1] != null && !mediaHeaderVisible) {
                    int key = isOnline[0] && peerColor == null ? Theme.key_profile_status : Theme.key_avatar_subtitleInProfileBlue;
                    onlineTextView[1].setTag(key);
                    if (!isPulledDown) {
                        onlineTextView[1].setTextColor(applyPeerColor(getThemedColor(key), true, isOnline[0]));
                    }
                }
            }
            hasCustomPhoto = user.photo != null && user.photo.personal;
            try {
                newString = Emoji.replaceEmoji(newString, nameTextView[1].getPaint().getFontMetricsInt(), false);
            } catch (Exception ignore) {
            }
            if (copyFromChatActivity) {
                ChatActivity chatActivity = (ChatActivity) prevFragment;
                BackupImageView fromAvatarImage = chatActivity.avatarContainer.getAvatarImageView();
                avatarImage.setAnimateFromImageReceiver(fromAvatarImage.getImageReceiver());
            }
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    nameTextView[a].setText(titleTextView.getText());
                    nameTextView[a].setRightDrawable(titleTextView.getRightDrawable());
                    nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2());
                } else if (a == 0 && user.id != getUserConfig().getClientUserId() && !MessagesController.isSupportUser(user) && user.phone != null && user.phone.length() != 0 && getContactsController().contactsDict.get(user.id) == null &&
                        (getContactsController().contactsDict.size() != 0 || !getContactsController().isLoadingContacts())) {
                    nameTextView[a].setText(PhoneFormat.getInstance().format("+" + user.phone));
                } else {
                    nameTextView[a].setText(newString);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                        SimpleTextView textView = (SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                        AnimatedTextView textView = (AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    }
                } else {
                    onlineTextView[a].setText(newString2);
                }
                onlineTextView[a].setDrawablePadding(dp(9));
                onlineTextView[a].setRightDrawableInside(true);
                onlineTextView[a].setRightDrawable(a == 1 && hiddenStatusButton ? getShowStatusButton() : null);
                onlineTextView[a].setRightDrawableOnClick(a == 1 && hiddenStatusButton ? v -> {
                    MessagePrivateSeenView.showSheet(getContext(), currentAccount, viewModel.getDialogId(), true, null, () -> {
                        getMessagesController().reloadUser(viewModel.getDialogId());
                    }, resourcesProvider);
                } : null);
                Drawable leftIcon = viewModel.getState().getValue().getCurrentEncryptedChat() != null ? getLockIconDrawable() : null;
                boolean rightIconIsPremium = false, rightIconIsStatus = false;
                nameTextView[a].setRightDrawableOutside(a == 0);
                if (a == 0 && !copyFromChatActivity) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else if (getMessagesController().isDialogMuted(viewModel.getArgs().dialogId != 0 ? viewModel.getArgs().dialogId : viewModel.getArgs().userId, viewModel.getArgs().topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.NotificationsMuted);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawable2ContentDescription = null;
                    }
                    if (user != null && !getMessagesController().premiumFeaturesBlocked() && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, false, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrPremium);
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, false, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString( R.string.AccDescrPremium);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                } else if (a == 1) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (!getMessagesController().premiumFeaturesBlocked() && user != null && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, true, true, a));
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, true, true, a));
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (leftIcon == null && viewModel.getState().getValue().getCurrentEncryptedChat() == null && user.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    leftIcon = getBotVerificationDrawable(user.bot_verification_icon, false, a);
                } else {
                    nameTextView[a].setLeftDrawableOutside(false);
                }
                nameTextView[a].setLeftDrawable(leftIcon);
                if (a == 1 && (rightIconIsStatus || rightIconIsPremium)) {
                    nameTextView[a].setRightDrawableOutside(true);
                }
                if (user.self && getMessagesController().isPremiumUser(user)) {
                    nameTextView[a].setRightDrawableOnClick(v -> {
                        showStatusSelect();
                    });
                }
                if (!user.self && getMessagesController().isPremiumUser(user)) {
                    final SimpleTextView textView = nameTextView[a];
                    nameTextView[a].setRightDrawableOnClick(v -> {
                        if (user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) user.emoji_status;
                            if (status != null) {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + status.slug);
                            }
                            return;
                        }
                        PremiumPreviewBottomSheet premiumPreviewBottomSheet = new PremiumPreviewBottomSheet(ProfileActivity.this, currentAccount, user, resourcesProvider);
                        int[] coords = new int[2];
                        textView.getLocationOnScreen(coords);
                        premiumPreviewBottomSheet.startEnterFromX = textView.rightDrawableX;
                        premiumPreviewBottomSheet.startEnterFromY = textView.rightDrawableY;
                        premiumPreviewBottomSheet.startEnterFromScale = textView.getScaleX();
                        premiumPreviewBottomSheet.startEnterFromX1 = textView.getLeft();
                        premiumPreviewBottomSheet.startEnterFromY1 = textView.getTop();
                        premiumPreviewBottomSheet.startEnterFromView = textView;
                        if (textView.getRightDrawable() == emojiStatusDrawable[1] && emojiStatusDrawable[1] != null && emojiStatusDrawable[1].getDrawable() instanceof AnimatedEmojiDrawable) {
                            premiumPreviewBottomSheet.startEnterFromScale *= 0.98f;
                            TLRPC.Document document = ((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).getDocument();
                            if (document != null) {
                                BackupImageView icon = new BackupImageView(getContext());
                                String filter = "160_160";
                                ImageLocation mediaLocation;
                                String mediaFilter;
                                SvgHelper.SvgDrawable thumbDrawable = DocumentObject.getSvgThumb(document.thumbs, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                                if ("video/webm".equals(document.mime_type)) {
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter + "_" + ImageLoader.AUTOPLAY_FILTER;
                                    if (thumbDrawable != null) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                } else {
                                    if (thumbDrawable != null && MessageObject.isAnimatedStickerDocument(document, false)) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter;
                                }
                                icon.setLayerNum(7);
                                icon.setRoundRadius(AndroidUtilities.dp(4));
                                icon.setImage(mediaLocation, mediaFilter, ImageLocation.getForDocument(thumb, document), "140_140", thumbDrawable, document);
                                if (((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).canOverrideColor()) {
                                    icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN));
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                } else {
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                }
                                premiumPreviewBottomSheet.overrideTitleIcon = icon;
                                premiumPreviewBottomSheet.isEmojiStatus = true;
                            }
                        }
                        showDialog(premiumPreviewBottomSheet);
                    });
                }
            }

            if (viewModel.getArgs().userId == UserConfig.getInstance(currentAccount).clientUserId) {
                onlineTextView[2].setText(LocaleController.getString(R.string.FallbackTooltip));
                onlineTextView[3].setText(LocaleController.getString(R.string.Online));
            } else {
                if (user.photo != null && user.photo.personal && user.photo.has_video) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltipVideo));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                } else {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltip));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                }
            }

            onlineTextView[2].setVisibility(View.VISIBLE);
            if (!searchMode) {
                onlineTextView[3].setVisibility(View.VISIBLE);
            }

            if (previousTransitionFragment != null) {
                previousTransitionFragment.checkAndUpdateAvatar();
            }
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        } else if (viewModel.getArgs().chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
            if (chat != null) {
                // TODO fix
                //viewModel.getState().getValue().getCurrentChat() = chat;
            } else {
                chat = viewModel.getState().getValue().getCurrentChat();
            }
            if (flagSecure != null) {
                flagSecure.invalidate();
            }

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(chat.emoji_status);
            if (peerColor == null) {
                final int colorId = ChatObject.getProfileColorId(chat);
                MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }
            if (topView != null) {
                topView.setBackgroundEmojiId(ChatObject.getProfileEmojiId(chat), chat != null && chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible, true);
            }
            setCollectibleGiftStatus(chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible ? (TLRPC.TL_emojiStatusCollectible) chat.emoji_status : null);

            if (viewModel.getArgs().isTopic) {
                topic = getMessagesController().getTopicsController().findTopic(viewModel.getArgs().chatId, viewModel.getArgs().topicId);
            }

            CharSequence statusString;
            CharSequence profileStatusString;
            boolean profileStatusIsButton = false;
            if (ChatObject.isChannel(chat)) {
                if (!viewModel.getArgs().isTopic && (viewModel.getState().getValue().getChatInfo() == null || (viewModel.getState().getValue().getCurrentChat() != null && !viewModel.getState().getValue().isCurrentChatMagagroup()) && (viewModel.getState().getValue().getChatInfo().participants_count == 0 || ChatObject.hasAdminRights(viewModel.getState().getValue().getCurrentChat()) || viewModel.getState().getValue().getChatInfo().can_view_participants))) {
                    if (viewModel.getState().getValue().isCurrentChatMagagroup()) {
                        statusString = profileStatusString = LocaleController.getString(R.string.Loading).toLowerCase();
                    } else {
                        if (ChatObject.isPublic(chat)) {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPublic).toLowerCase();
                        } else {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPrivate).toLowerCase();
                        }
                    }
                } else {
                    if (viewModel.getArgs().isTopic) {
                        int count = 0;
                        if (topic != null) {
                            count = topic.totalMessagesCount - 1;
                        }
                        if (count > 0) {
                            statusString = LocaleController.formatPluralString("messages", count, count);
                        } else {
                            statusString = formatString("TopicProfileStatus", R.string.TopicProfileStatus, chat.title);
                        }
                        SpannableString arrowString = new SpannableString(">");
                        arrowString.setSpan(new ColoredImageSpan(R.drawable.arrow_newchat), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        profileStatusString = new SpannableStringBuilder(chat.title).append(' ').append(arrowString);
                        profileStatusIsButton = true;
                    } else if (viewModel.getState().getValue().isCurrentChatMagagroup()) {
                        if (viewModel.getState().getValue().getOnlineCount() > 1 && viewModel.getState().getValue().getChatInfo().participants_count != 0) {
                            statusString = String.format("%s, %s", LocaleController.formatPluralString("Members", viewModel.getState().getValue().getChatInfo().participants_count), LocaleController.formatPluralString("OnlineCount", Math.min(viewModel.getState().getValue().getOnlineCount(), viewModel.getState().getValue().getChatInfo().participants_count)));
                            profileStatusString = String.format("%s, %s", LocaleController.formatPluralStringComma("Members", viewModel.getState().getValue().getChatInfo().participants_count), LocaleController.formatPluralStringComma("OnlineCount", Math.min(viewModel.getState().getValue().getOnlineCount(), viewModel.getState().getValue().getChatInfo().participants_count)));
                        } else {
                            if (viewModel.getState().getValue().getChatInfo().participants_count == 0) {
                                if (chat.has_geo) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaLocation).toLowerCase();
                                } else if (ChatObject.isPublic(chat)) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPublic).toLowerCase();
                                } else {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPrivate).toLowerCase();
                                }
                            } else {
                                statusString = LocaleController.formatPluralString("Members", viewModel.getState().getValue().getChatInfo().participants_count);
                                profileStatusString = LocaleController.formatPluralStringComma("Members", viewModel.getState().getValue().getChatInfo().participants_count);
                            }
                        }
                    } else {
                        int[] result = new int[1];
                        String shortNumber = LocaleController.formatShortNumber(viewModel.getState().getValue().getChatInfo().participants_count, result);
                        if (viewModel.getState().getValue().isCurrentChatMagagroup()) {
                            statusString = LocaleController.formatPluralString("Members", viewModel.getState().getValue().getChatInfo().participants_count);
                            profileStatusString = LocaleController.formatPluralStringComma("Members", viewModel.getState().getValue().getChatInfo().participants_count);
                        } else {
                            statusString = LocaleController.formatPluralString("Subscribers", viewModel.getState().getValue().getChatInfo().participants_count);
                            profileStatusString = LocaleController.formatPluralStringComma("Subscribers", viewModel.getState().getValue().getChatInfo().participants_count);
                        }
                    }
                }
            } else {
                if (ChatObject.isKickedFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouWereKicked);
                } else if (ChatObject.isLeftFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouLeft);
                } else {
                    int count = chat.participants_count;
                    if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants != null) {
                        count = viewModel.getState().getValue().getChatInfo().participants.participants.size();
                    }
                    if (count != 0 && viewModel.getState().getValue().getOnlineCount() > 1) {
                        statusString = profileStatusString = String.format("%s, %s", LocaleController.formatPluralString("Members", count), LocaleController.formatPluralString("OnlineCount", viewModel.getState().getValue().getOnlineCount()));
                    } else {
                        statusString = profileStatusString = LocaleController.formatPluralString("Members", count);
                    }
                }
            }
            if (copyFromChatActivity) {
                ChatActivity chatActivity = (ChatActivity) prevFragment;
                if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                    statusString = ((SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                    statusString = ((AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                }
                BackupImageView fromAvatarImage = chatActivity.avatarContainer.getAvatarImageView();
                avatarImage.setAnimateFromImageReceiver(fromAvatarImage.getImageReceiver());
            }

            boolean changed = false;
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    if (nameTextView[a].setText(titleTextView.getText())) {
                        changed = true;
                    }
                    if (nameTextView[a].setRightDrawable(titleTextView.getRightDrawable())) {
                        changed = true;
                    }
                    if (nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2())) {
                        changed = true;
                    }
                } else if (viewModel.getArgs().isTopic) {
                    CharSequence title = topic == null ? "" : topic.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                } else if (ChatObject.isMonoForum(chat)) {
                    CharSequence title = getString(R.string.ChatMessageSuggestions);
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                } else if (chat.title != null) {
                    CharSequence title = chat.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    if (nameTextView[a].setText(title)) {
                        changed = true;
                    }
                }
                nameTextView[a].setLeftDrawableOutside(false);
                nameTextView[a].setLeftDrawable(null);
                nameTextView[a].setRightDrawableOutside(a == 0);
                nameTextView[a].setRightDrawableOnClick(null);
                if (a != 0) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, true, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                        nameTextViewRightDrawableContentDescription = null;
                        if (ChatObject.canChangeChatInfo(chat)) {
                            nameTextView[a].setRightDrawableOnClick(v -> {
                                showStatusSelect();
                            });
                            if (preloadedChannelEmojiStatuses) {
                                preloadedChannelEmojiStatuses = true;
                                getMediaDataController().loadRestrictedStatusEmojis();
                            }
                        } else if (chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            final String slug = ((TLRPC.TL_emojiStatusCollectible) chat.emoji_status).slug;
                            nameTextView[a].setRightDrawableOnClick(v -> {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + slug);
                            });
                        }
                    }
                } else if (!copyFromChatActivity) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a)) ;
                    } else if (getMessagesController().isDialogMuted(-viewModel.getArgs().chatId, viewModel.getArgs().topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, false, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (chat.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    nameTextView[a].setLeftDrawable(getBotVerificationDrawable(chat.bot_verification_icon, false, a));
                } else {
                    nameTextView[a].setLeftDrawable(null);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else {
                    if (copyFromChatActivity || (viewModel.getState().getValue().isCurrentChatMagagroup() && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getOnlineCount() > 0) || (viewModel.getArgs().isTopic)) {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    } else if (a == 0 && ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().participants_count != 0 && (viewModel.getState().getValue().isCurrentChatMagagroup() || viewModel.getState().getValue().getCurrentChat().broadcast)) {
                        int[] result = new int[1];
                        boolean ignoreShort = AndroidUtilities.isAccessibilityScreenReaderEnabled();
                        String shortNumber = ignoreShort ? String.valueOf(result[0] = viewModel.getState().getValue().getChatInfo().participants_count) : LocaleController.formatShortNumber(viewModel.getState().getValue().getChatInfo().participants_count, result);
                        if (viewModel.getState().getValue().isCurrentChatMagagroup()) {
                            if (viewModel.getState().getValue().getChatInfo().participants_count == 0) {
                                if (chat.has_geo) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaLocation).toLowerCase());
                                } else if (ChatObject.isPublic(chat)) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPublic).toLowerCase());
                                } else {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPrivate).toLowerCase());
                                }
                            } else {
                                onlineTextView[a].setText(LocaleController.formatPluralString("Members", result[0]).replace(String.format("%d", result[0]), shortNumber));
                            }
                        } else {
                            onlineTextView[a].setText(LocaleController.formatPluralString("Subscribers", result[0]).replace(String.format("%d", result[0]), shortNumber));
                        }
                    } else {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    }
                }
                if (a == 1 && viewModel.getArgs().isTopic) {
                    if (profileStatusIsButton) {
                        onlineTextView[a].setOnClickListener(e -> goToForum());
                    } else {
                        onlineTextView[a].setOnClickListener(null);
                        onlineTextView[a].setClickable(false);
                    }
                }
            }
            if (changed) {
                needLayout(true);
            }

            TLRPC.FileLocation photoBig = null;
            if (chat.photo != null && !viewModel.getArgs().isTopic) {
                photoBig = chat.photo.photo_big;
            }

            final ImageLocation imageLocation;
            final ImageLocation thumbLocation;
            final ImageLocation videoLocation;
            if (viewModel.getArgs().isTopic) {
                imageLocation = null;
                thumbLocation = null;
                videoLocation = null;
                ForumUtilities.setTopicIcon(avatarImage, topic, true, true, resourcesProvider);
            } else if (ChatObject.isMonoForum(viewModel.getState().getValue().getCurrentChat())) {
                TLRPC.Chat channel = getMessagesController().getMonoForumLinkedChat(viewModel.getState().getValue().getCurrentChat().id);
                avatarDrawable.setInfo(currentAccount, channel);
                imageLocation = ImageLocation.getForUserOrChat(channel, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(channel, ImageLocation.TYPE_SMALL);
                videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            } else {
                avatarDrawable.setInfo(currentAccount, chat);
                imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_SMALL);
                videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            }

            boolean initied = avatarsViewPager.initIfEmpty(null, imageLocation, thumbLocation, reload);
            if ((imageLocation == null || initied) && isPulledDown) {
                final View view = layoutManager.findViewByPosition(0);
                if (view != null) {
                    listView.smoothScrollBy(0, view.getTop() - AndroidUtilities.dp(88), CubicBezierInterpolator.EASE_OUT_QUINT);
                }
            }
            String filter;
            if (videoLocation != null && videoLocation.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                filter = ImageLoader.AUTOPLAY_FILTER;
            } else {
                filter = null;
            }
            if (viewModel.getState().getValue().getAvatarBig() == null && !viewModel.getArgs().isTopic) {
                avatarImage.setImage(videoLocation, filter, thumbLocation, "50_50", avatarDrawable, chat);
            }
            if (imageLocation != null && (prevLoadedImageLocation == null || imageLocation.photoId != prevLoadedImageLocation.photoId)) {
                prevLoadedImageLocation = imageLocation;
                getFileLoader().loadFile(imageLocation, chat, null, FileLoader.PRIORITY_LOW, 1);
            }
            avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        }

        if (qrItem != null) {
            updateQrItemVisibility(true);
        }
        AndroidUtilities.runOnUIThread(this::updateEmojiStatusEffectPosition);
    }

    private void updatedPeerColor() {
        adaptedColors.clear();
        if (topView != null) {
            topView.setBackgroundColorId(peerColor, true);
        }
        if (onlineTextView[1] != null) {
            int statusColor;
            if (onlineTextView[1].getTag() instanceof Integer) {
                statusColor = getThemedColor((Integer) onlineTextView[1].getTag());
            } else {
                statusColor = getThemedColor(Theme.key_avatar_subtitleInProfileBlue);
            }
            onlineTextView[1].setTextColor(ColorUtils.blendARGB(applyPeerColor(statusColor, true, isOnline[0]), 0xB3FFFFFF, currentExpandAnimatorValue));
        }
        if (showStatusButton != null) {
            showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }
        if (actionBar != null) {
            actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), getThemedColor(Theme.key_actionBarActionModeDefaultIcon), mediaHeaderAnimationProgress), false);
            actionBar.setItemsBackgroundColor(ColorUtils.blendARGB(peerColor != null ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), getThemedColor(Theme.key_actionBarActionModeDefaultSelector), mediaHeaderAnimationProgress), false);
        }
        if (verifiedDrawable[1] != null) {
            final int color1 = peerColor != null ? Theme.adaptHSV(ColorUtils.blendARGB(peerColor.getColor2(), peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), .4f), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f) : getThemedColor(Theme.key_profile_verifiedBackground);
            final int color2 = getThemedColor(Theme.key_player_actionBarTitle);
            verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        }
        if (verifiedCheckDrawable[1] != null) {
            final int color1 = peerColor != null ? Color.WHITE : dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedCheck));
            final int color2 = getThemedColor(Theme.key_windowBackgroundWhite);
            verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        }
        if (nameTextView[1] != null) {
            nameTextView[1].setTextColor(ColorUtils.blendARGB(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress), Color.WHITE, currentExpandAnimatorValue));
        }
        if (autoDeletePopupWrapper != null && autoDeletePopupWrapper.textView != null) {
            autoDeletePopupWrapper.textView.invalidate();
        }
        AndroidUtilities.forEachViews(listView, view -> {
            if (view instanceof HeaderCell) {
                ((HeaderCell) view).setTextColor(dontApplyPeerColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), false));
            } else if (view instanceof TextDetailCell) {
                ((TextDetailCell) view).updateColors();
            } else if (view instanceof TextCell) {
                ((TextCell) view).updateColors();
            } else if (view instanceof AboutLinkCell) {
                ((AboutLinkCell) view).updateColors();
            } else if (view instanceof NotificationsCheckCell) {
                ((NotificationsCheckCell) view).getCheckBox().invalidate();
            } else if (view instanceof ProfileHoursCell) {
                ((ProfileHoursCell) view).updateColors();
            } else if (view instanceof ProfileChannelCell) {
                ((ProfileChannelCell) view).updateColors();
            }
        });
        if (sharedMediaLayout != null && sharedMediaLayout.scrollSlidingTextTabStrip != null) {
            sharedMediaLayout.scrollSlidingTextTabStrip.updateColors();
        }
        if (sharedMediaLayout != null && sharedMediaLayout.giftsContainer != null) {
            sharedMediaLayout.giftsContainer.updateColors();
        }
        writeButtonSetBackground();
        updateEmojiStatusDrawableColor();
        if (storyView != null) {
            storyView.update();
        }
        if (giftsView != null) {
            giftsView.update();
        }
    }

    private int dontApplyPeerColor(int color) {
        return dontApplyPeerColor(color, true, null);
    }

    private int dontApplyPeerColor(int color, boolean actionBar) {
        return dontApplyPeerColor(color, actionBar, null);
    }

    private final SparseIntArray adaptedColors = new SparseIntArray();
    private int dontApplyPeerColor(int color, boolean actionBar, Boolean online) {
        return color;
    }

    private int applyPeerColor(int color, boolean actionBar, Boolean online) {
        if (!actionBar && isSettings()) return color;
        if (peerColor != null) {
            if (!actionBar) {
                int index = adaptedColors.indexOfKey(color);
                if (index < 0) {
                    final int baseColor = Theme.adaptHSV(peerColor.getBgColor1(Theme.isCurrentThemeDark()), Theme.isCurrentThemeDark() ? 0 : +.05f, Theme.isCurrentThemeDark() ? -.1f : -.04f);
                    int adapted = OKLCH.adapt(color, baseColor);
                    adaptedColors.put(color, adapted);
                    return adapted;
                } else {
                    return adaptedColors.valueAt(index);
                }
            }
            final int baseColor = getThemedColor(actionBar ? Theme.key_actionBarDefault : Theme.key_windowBackgroundWhiteBlueIcon);
            final int storyColor = ColorUtils.blendARGB(peerColor.getStoryColor1(Theme.isCurrentThemeDark()), peerColor.getStoryColor2(Theme.isCurrentThemeDark()), .5f);
            int accentColor = actionBar ? storyColor : peerColor.getBgColor1(Theme.isCurrentThemeDark());
            if (!Theme.hasHue(baseColor)) {
                return online != null && !online ? Theme.adaptHSV(Theme.multAlpha(storyColor, .7f), -.2f, +.2f) : storyColor;
            }
            return Theme.changeColorAccent(baseColor, accentColor, color, Theme.isCurrentThemeDark(), online != null && !online ? Theme.multAlpha(storyColor, .7f) : storyColor);
        }
        return color;
    }

    private int applyPeerColor2(int color) {
        if (peerColor != null) {
            int accentColor = peerColor.getBgColor2(Theme.isCurrentThemeDark());
            return Theme.changeColorAccent(getThemedColor(Theme.key_windowBackgroundWhiteBlueIcon), accentColor, color, Theme.isCurrentThemeDark(), accentColor);
        }
        return color;
    }

    private void createActionBarMenu(boolean animated) {
        if (actionBar == null || otherItem == null) {
            return;
        }
        Context context = actionBar.getContext();
        otherItem.removeAllSubItems();
        animatingItem = null;

        editItemVisible = false;
        callItemVisible = false;
        videoCallItemVisible = false;
        canSearchMembers = false;
        boolean selfUser = false;

        if (viewModel.getArgs().userId != 0) {
            TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user == null) {
                return;
            }
            if (UserObject.isUserSelf(user)) {
                editItemVisible = viewModel.getArgs().myProfile;
                otherItem.addSubItem(ActionBarActions.edit_info, R.drawable.msg_edit, LocaleController.getString(R.string.EditInfo));
                if (imageUpdater != null) {
                    otherItem.addSubItem(ActionBarActions.add_photo, R.drawable.msg_addphoto, LocaleController.getString(R.string.AddPhoto));
                }
                editColorItem = otherItem.addSubItem(ActionBarActions.edit_color, R.drawable.menu_profile_colors, LocaleController.getString(R.string.ProfileColorEdit));
                updateEditColorIcon();
                if (viewModel.getArgs().myProfile) {
                    setUsernameItem = otherItem.addSubItem(ActionBarActions.set_username, R.drawable.menu_username_change, getString(R.string.ProfileUsernameEdit));
                    linkItem = otherItem.addSubItem(ActionBarActions.copy_link_profile, R.drawable.msg_link2, getString(R.string.ProfileCopyLink));
                    updateItemsUsername();
                }
                selfUser = true;
            } else {
                if (user.bot && user.bot_can_edit) {
                    editItemVisible = true;
                }

                if (viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().phone_calls_available) {
                    callItemVisible = true;
                    videoCallItemVisible = Build.VERSION.SDK_INT >= 18 && viewModel.getState().getValue().getUserInfo().video_calls_available;
                }
                if (viewModel.getState().getValue().isBot() || getContactsController().contactsDict.get(viewModel.getArgs().userId) == null) {
                    if (MessagesController.isSupportUser(user)) {
                        if (viewModel.getState().getValue().isUserBlocked()) {
                            otherItem.addSubItem(ActionBarActions.block_contact, R.drawable.msg_block, LocaleController.getString(R.string.Unblock));
                        }
                        otherItem.addSubItem(ActionBarActions.add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
                    } else if (viewModel.getDialogId() != UserObject.VERIFY) {
                        if (viewModel.getState().getValue().getCurrentEncryptedChat() == null) {
                            createAutoDeleteItem(context);
                        }
                        otherItem.addSubItem(ActionBarActions.add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
                        if (viewModel.getState().getValue().isBot()) {
                            otherItem.addSubItem(ActionBarActions.share, R.drawable.msg_share, LocaleController.getString(R.string.BotShare));
                        } else {
                            otherItem.addSubItem(ActionBarActions.add_contact, R.drawable.msg_addcontact, LocaleController.getString(R.string.AddContact));
                        }
                        if (!TextUtils.isEmpty(user.phone)) {
                            otherItem.addSubItem(ActionBarActions.share_contact, R.drawable.msg_share, LocaleController.getString(R.string.ShareContact));
                        }
                        if (viewModel.getState().getValue().isBot()) {
                            otherItem.addSubItem(ActionBarActions.bot_privacy, R.drawable.menu_privacy_policy, getString(R.string.BotPrivacyPolicy));
                            if (hasPrivacyCommand()) {
                                otherItem.showSubItem(ActionBarActions.bot_privacy);
                            } else {
                                otherItem.hideSubItem(ActionBarActions.bot_privacy);
                            }
                            otherItem.addSubItem(ActionBarActions.report, R.drawable.msg_report, LocaleController.getString(R.string.ReportBot)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
                            if (!viewModel.getState().getValue().isUserBlocked()) {
                                otherItem.addSubItem(ActionBarActions.block_contact, R.drawable.msg_block2, LocaleController.getString(R.string.DeleteAndBlock)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
                            } else {
                                otherItem.addSubItem(ActionBarActions.block_contact, R.drawable.msg_retry, LocaleController.getString(R.string.BotRestart));
                            }
                        } else {
                            otherItem.addSubItem(ActionBarActions.block_contact, !viewModel.getState().getValue().isUserBlocked() ? R.drawable.msg_block : R.drawable.msg_block, !viewModel.getState().getValue().isUserBlocked() ? LocaleController.getString(R.string.BlockContact) : LocaleController.getString(R.string.Unblock));
                        }
                    }
                } else {
                    if (viewModel.getState().getValue().getCurrentEncryptedChat() == null) {
                        createAutoDeleteItem(context);
                    }
                    if (!TextUtils.isEmpty(user.phone)) {
                        otherItem.addSubItem(ActionBarActions.share_contact, R.drawable.msg_share, LocaleController.getString(R.string.ShareContact));
                    }
                    otherItem.addSubItem(ActionBarActions.block_contact, !viewModel.getState().getValue().isUserBlocked() ? R.drawable.msg_block : R.drawable.msg_block, !viewModel.getState().getValue().isUserBlocked() ? LocaleController.getString(R.string.BlockContact) : LocaleController.getString(R.string.Unblock));
                    otherItem.addSubItem(ActionBarActions.edit_contact, R.drawable.msg_edit, LocaleController.getString(R.string.EditContact));
                    otherItem.addSubItem(ActionBarActions.delete_contact, R.drawable.msg_delete, LocaleController.getString(R.string.DeleteContact));
                }
                if (!UserObject.isDeleted(user) && !viewModel.getState().getValue().isBot() && viewModel.getState().getValue().getCurrentEncryptedChat() == null && !viewModel.getState().getValue().isUserBlocked() && viewModel.getArgs().userId != 333000 && viewModel.getArgs().userId != 777000 && viewModel.getArgs().userId != 42777) {
                    if (!BuildVars.IS_BILLING_UNAVAILABLE && !user.self && !user.bot && !MessagesController.isSupportUser(user) && !getMessagesController().premiumPurchaseBlocked()) {
                        StarsController.getInstance(currentAccount).loadStarGifts();
                        otherItem.addSubItem(ActionBarActions.gift_premium, R.drawable.msg_gift_premium, LocaleController.getString(R.string.ProfileSendAGift));
                    }
                    otherItem.addSubItem(ActionBarActions.start_secret_chat, R.drawable.msg_secret, LocaleController.getString(R.string.StartEncryptedChat));
                    otherItem.setSubItemShown(ActionBarActions.start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(viewModel.getArgs().userId)));
                }
                if (!viewModel.getState().getValue().isBot() && getContactsController().contactsDict.get(viewModel.getArgs().userId) != null) {
                    otherItem.addSubItem(ActionBarActions.add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
                }
            }
        } else if (viewModel.getArgs().chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
            hasVoiceChatItem = false;

            if (viewModel.getArgs().topicId == 0 && ChatObject.canChangeChatInfo(chat)) {
                createAutoDeleteItem(context);
            }
            if (ChatObject.isChannel(chat)) {
                if (viewModel.getArgs().isTopic) {
                    if (ChatObject.canManageTopic(currentAccount, chat, viewModel.getArgs().topicId)) {
                        editItemVisible = true;
                    }
                } else {
                    if (ChatObject.hasAdminRights(chat) || chat.megagroup && ChatObject.canChangeChatInfo(chat)) {
                        editItemVisible = true;
                    }
                }
                if (viewModel.getState().getValue().getChatInfo() != null) {
                    if (ChatObject.canManageCalls(chat) && viewModel.getState().getValue().getChatInfo().call == null) {
                        otherItem.addSubItem(ActionBarActions.call_item, R.drawable.msg_voicechat, chat.megagroup && !chat.gigagroup ? LocaleController.getString(R.string.StartVoipChat) : LocaleController.getString(R.string.StartVoipChannel));
                        hasVoiceChatItem = true;
                    }
                    if ((viewModel.getState().getValue().getChatInfo().can_view_stats || viewModel.getState().getValue().getChatInfo().can_view_revenue || viewModel.getState().getValue().getChatInfo().can_view_stars_revenue || getMessagesController().getStoriesController().canPostStories(viewModel.getDialogId())) && viewModel.getArgs().topicId == 0) {
                        otherItem.addSubItem(ActionBarActions.statistics, R.drawable.msg_stats, LocaleController.getString(R.string.Statistics));
                    }
                    ChatObject.Call call = getMessagesController().getGroupCall(viewModel.getArgs().chatId, false);
                    callItemVisible = call != null;
                }
                if (chat.megagroup) {
                    if (viewModel.getState().getValue().getChatInfo() == null || !viewModel.getState().getValue().getChatInfo().participants_hidden || ChatObject.hasAdminRights(chat)) {
                        canSearchMembers = true;
                        otherItem.addSubItem(ActionBarActions.search_members, R.drawable.msg_search, LocaleController.getString(R.string.SearchMembers));
                    }
                    if (!chat.creator && !chat.left && !chat.kicked && !viewModel.getArgs().isTopic) {
                        otherItem.addSubItem(ActionBarActions.leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveMegaMenu));
                    }
                    if (viewModel.getArgs().isTopic && ChatObject.canDeleteTopic(currentAccount, chat, viewModel.getArgs().topicId)) {
                        otherItem.addSubItem(ActionBarActions.delete_topic, R.drawable.msg_delete, LocaleController.getPluralString("DeleteTopics", 1));
                    }
                } else {
                    if (chat.creator || chat.admin_rights != null && chat.admin_rights.edit_stories) {
                        otherItem.addSubItem(ActionBarActions.channel_stories, R.drawable.msg_archive, LocaleController.getString(R.string.OpenChannelArchiveStories));
                    }
                    if (ChatObject.isPublic(chat)) {
                        otherItem.addSubItem(ActionBarActions.share, R.drawable.msg_share, LocaleController.getString(R.string.BotShare));
                    }
                    if (!BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked()) {
                        StarsController.getInstance(currentAccount).loadStarGifts();
                        otherItem.addSubItem(ActionBarActions.gift_premium, R.drawable.msg_gift_premium, LocaleController.getString(R.string.ProfileSendAGiftToChannel));
                        otherItem.setSubItemShown(ActionBarActions.gift_premium, viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().stargifts_available);
                    }
                    if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().linked_chat_id != 0) {
                        otherItem.addSubItem(ActionBarActions.view_discussion, R.drawable.msg_discussion, LocaleController.getString(R.string.ViewDiscussion));
                    }
                    if (!viewModel.getState().getValue().isCurrentChatCreator() && !viewModel.getState().getValue().isCurrentChatLeft() && !viewModel.getState().getValue().isCurrentChatLeft()) {
                        otherItem.addSubItem(ActionBarActions.leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveChannelMenu));
                    }
                }
            } else {
                if (viewModel.getState().getValue().getChatInfo() != null) {
                    if (ChatObject.canManageCalls(chat) && viewModel.getState().getValue().getChatInfo().call == null) {
                        otherItem.addSubItem(ActionBarActions.call_item, R.drawable.msg_voicechat, LocaleController.getString(R.string.StartVoipChat));
                        hasVoiceChatItem = true;
                    }
                    ChatObject.Call call = getMessagesController().getGroupCall(viewModel.getArgs().chatId, false);
                    callItemVisible = call != null;
                }
                if (ChatObject.canChangeChatInfo(chat)) {
                    editItemVisible = true;
                }
                if (!ChatObject.isKickedFromChat(chat) && !ChatObject.isLeftFromChat(chat)) {
                    if (viewModel.getState().getValue().getChatInfo() == null || !viewModel.getState().getValue().getChatInfo().participants_hidden || ChatObject.hasAdminRights(chat)) {
                        canSearchMembers = true;
                        otherItem.addSubItem(ActionBarActions.search_members, R.drawable.msg_search, LocaleController.getString(R.string.SearchMembers));
                    }
                }
                otherItem.addSubItem(ActionBarActions.leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.DeleteAndExit));
            }
            if (viewModel.getArgs().topicId == 0) {
                otherItem.addSubItem(ActionBarActions.add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
            }
        }

        if (imageUpdater != null) {
            otherItem.addSubItem(ActionBarActions.set_as_main, R.drawable.msg_openprofile, LocaleController.getString(R.string.SetAsMain));
            otherItem.addSubItem(ActionBarActions.gallery_menu_save, R.drawable.msg_gallery, LocaleController.getString(R.string.SaveToGallery));
            //otherItem.addSubItem(edit_avatar, R.drawable.photo_paint, LocaleController.getString(R.string.EditPhoto));
            otherItem.addSubItem(ActionBarActions.delete_avatar, R.drawable.msg_delete, LocaleController.getString(R.string.Delete));
        } else {
            otherItem.addSubItem(ActionBarActions.gallery_menu_save, R.drawable.msg_gallery, LocaleController.getString(R.string.SaveToGallery));
        }
        if (getMessagesController().isChatNoForwards(viewModel.getState().getValue().getCurrentChat())) {
            otherItem.hideSubItem(ActionBarActions.gallery_menu_save);
        }

        if (selfUser && !viewModel.getArgs().myProfile) {
            otherItem.addSubItem(ActionBarActions.logout, R.drawable.msg_leave, LocaleController.getString(R.string.LogOut));
        }
        if (!isPulledDown) {
            otherItem.hideSubItem(ActionBarActions.gallery_menu_save);
            otherItem.hideSubItem(ActionBarActions.set_as_main);
            otherItem.showSubItem(ActionBarActions.add_photo);
            otherItem.hideSubItem(ActionBarActions.edit_avatar);
            otherItem.hideSubItem(ActionBarActions.delete_avatar);
        }
        if (!mediaHeaderVisible) {
            if (callItemVisible) {
                if (callItem.getVisibility() != View.VISIBLE) {
                    callItem.setVisibility(View.VISIBLE);
                    if (animated) {
                        callItem.setAlpha(0);
                        callItem.animate().alpha(1f).setDuration(150).start();
                    }
                }
            } else {
                if (callItem.getVisibility() != View.GONE) {
                    callItem.setVisibility(View.GONE);
                }
            }
            if (videoCallItemVisible) {
                if (videoCallItem.getVisibility() != View.VISIBLE) {
                    videoCallItem.setVisibility(View.VISIBLE);
                    if (animated) {
                        videoCallItem.setAlpha(0);
                        videoCallItem.animate().alpha(1f).setDuration(150).start();
                    }
                }
            } else {
                if (videoCallItem.getVisibility() != View.GONE) {
                    videoCallItem.setVisibility(View.GONE);
                }
            }
            if (editItemVisible) {
                if (editItem.getVisibility() != View.VISIBLE) {
                    editItem.setVisibility(View.VISIBLE);
                    if (animated) {
                        editItem.setAlpha(0);
                        editItem.animate().alpha(1f).setDuration(150).start();
                    }
                }
            } else {
                if (editItem.getVisibility() != View.GONE) {
                    editItem.setVisibility(View.GONE);
                }
            }
        }
        if (avatarsViewPagerIndicatorView != null) {
            if (avatarsViewPagerIndicatorView.isIndicatorFullyVisible()) {
                if (editItemVisible) {
                    editItem.setVisibility(View.GONE);
                    editItem.animate().cancel();
                    editItem.setAlpha(1f);
                }
                if (callItemVisible) {
                    callItem.setVisibility(View.GONE);
                    callItem.animate().cancel();
                    callItem.setAlpha(1f);
                }
                if (videoCallItemVisible) {
                    videoCallItem.setVisibility(View.GONE);
                    videoCallItem.animate().cancel();
                    videoCallItem.setAlpha(1f);
                }
            }
        }
        if (sharedMediaLayout != null) {
            sharedMediaLayout.getSearchItem().requestLayout();
        }
        updateStoriesViewBounds(false);
    }

    private void createAutoDeleteItem(Context context) {
        autoDeletePopupWrapper = new AutoDeletePopupWrapper(context, otherItem.getPopupLayout().getSwipeBack(), new AutoDeletePopupWrapper.Callback() {

            @Override
            public void dismiss() {
                otherItem.toggleSubMenu();
            }

            @Override
            public void setAutoDeleteHistory(int time, int action) {
                ProfileActivity.this.setAutoDeleteHistory(time, action);
            }

            @Override
            public void showGlobalAutoDeleteScreen() {
                presentFragment(new AutoDeleteMessagesActivity());
                dismiss();
            }
        }, false, 0, resourcesProvider);
        if (viewModel.getArgs().dialogId > 0 || viewModel.getArgs().userId > 0) {
            int linkColor = dontApplyPeerColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText), false);
            autoDeletePopupWrapper.allowExtendedHint(linkColor);
        }
        int ttl = 0;
        if (viewModel.getState().getValue().getUserInfo() != null || viewModel.getState().getValue().getChatInfo() != null) {
            ttl = viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().ttl_period : viewModel.getState().getValue().getChatInfo().ttl_period;
        }
        autoDeleteItemDrawable = TimerDrawable.getTtlIcon(ttl);
        autoDeleteItem = otherItem.addSwipeBackItem(0, autoDeleteItemDrawable, LocaleController.getString(R.string.AutoDeletePopupTitle), autoDeletePopupWrapper.windowLayout);
        otherItem.addColoredGap();
        updateAutoDeleteItem();
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return resourcesProvider;
    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    @Override
    public Drawable getThemedDrawable(String drawableKey) {
        Drawable drawable = resourcesProvider != null ? resourcesProvider.getDrawable(drawableKey) : null;
        return drawable != null ? drawable : super.getThemedDrawable(drawableKey);
    }

    private void setAutoDeleteHistory(int time, int action) {
        long did = viewModel.getDialogId();
        getMessagesController().setDialogHistoryTTL(did, time);
        if (viewModel.getState().getValue().getUserInfo() != null || viewModel.getState().getValue().getChatInfo() != null) {
            undoView.showWithAction(did, action, getMessagesController().getUser(did), viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().ttl_period : viewModel.getState().getValue().getChatInfo().ttl_period, null, null);
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        if (listView != null) {
            listView.invalidateViews();
        }
    }

    @Override
    public boolean didSelectDialogs(DialogsActivity fragment, ArrayList<MessagesStorage.TopicKey> dids, CharSequence message, boolean param, boolean notify, int scheduleDate, TopicsFragment topicsFragment) {
        long did = dids.get(0).dialogId;
        Bundle args = new Bundle();
        args.putBoolean("scrollToTopOnResume", true);
        if (DialogObject.isEncryptedDialog(did)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(did));
        } else if (DialogObject.isUserDialog(did)) {
            args.putLong("user_id", did);
        } else if (DialogObject.isChatDialog(did)) {
            args.putLong("chat_id", -did);
        }
        if (!getMessagesController().checkCanOpenChat(args, fragment)) {
            return false;
        }

        getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);
        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
        presentFragment(new ChatActivity(args), true);
        removeSelfFromStack();
        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
        getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(user, did, null, null, null, null, notify, scheduleDate));
        if (!TextUtils.isEmpty(message)) {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            SendMessagesHelper.prepareSendingText(accountInstance, message.toString(), did, notify, scheduleDate, 0);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (imageUpdater != null) {
            imageUpdater.onRequestPermissionsResultFragment(requestCode, permissions, grantResults);
        }
        if (requestCode == 101 || requestCode == 102) {
            final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
            if (user == null) {
                return;
            }
            boolean allGranted = true;
            for (int a = 0; a < grantResults.length; a++) {
                if (grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (grantResults.length > 0 && allGranted) {
                VoIPHelper.startCall(user, requestCode == 102, viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().video_calls_available, getParentActivity(), viewModel.getState().getValue().getUserInfo(), getAccountInstance());
            } else {
                VoIPHelper.permissionDenied(getParentActivity(), null, requestCode);
            }
        } else if (requestCode == 103) {
            if (viewModel.getState().getValue().getCurrentChat() == null) {
                return;
            }
            boolean allGranted = true;
            for (int a = 0; a < grantResults.length; a++) {
                if (grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (grantResults.length > 0 && allGranted) {
                ChatObject.Call call = getMessagesController().getGroupCall(viewModel.getArgs().chatId, false);
                VoIPHelper.startCall(viewModel.getState().getValue().getCurrentChat(), null, null, call == null, getParentActivity(), ProfileActivity.this, getAccountInstance());
            } else {
                VoIPHelper.permissionDenied(getParentActivity(), null, requestCode);
            }
        }
    }

    @Override
    public void dismissCurrentDialog() {
        if (imageUpdater != null && imageUpdater.dismissCurrentDialog(visibleDialog)) {
            return;
        }
        super.dismissCurrentDialog();
    }

    @Override
    public boolean dismissDialogOnPause(Dialog dialog) {
        return (imageUpdater == null || imageUpdater.dismissDialogOnPause(dialog)) && super.dismissDialogOnPause(dialog);
    }

    private Animator searchExpandTransition(boolean enter) {
        if (enter) {
            AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
            AndroidUtilities.setAdjustResizeToNothing(getParentActivity(), classGuid);
        }
        if (searchViewTransition != null) {
            searchViewTransition.removeAllListeners();
            searchViewTransition.cancel();
        }
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(searchTransitionProgress, enter ? 0f : 1f);
        float offset = extraHeight;
        searchListView.setTranslationY(offset);
        searchListView.setVisibility(View.VISIBLE);
        searchItem.setVisibility(View.VISIBLE);

        listView.setVisibility(View.VISIBLE);

        needLayout(true);

        avatarContainer.setVisibility(View.VISIBLE);
        nameTextView[1].setVisibility(View.VISIBLE);
        onlineTextView[1].setVisibility(View.VISIBLE);
        onlineTextView[3].setVisibility(View.VISIBLE);

        actionBar.onSearchFieldVisibilityChanged(searchTransitionProgress > 0.5f);
        int itemVisibility = searchTransitionProgress > 0.5f ? View.VISIBLE : View.GONE;
        if (otherItem != null) {
            otherItem.setVisibility(itemVisibility);
        }
        if (qrItem != null) {
            updateQrItemVisibility(false);
        }
        searchItem.setVisibility(itemVisibility);

        searchItem.getSearchContainer().setVisibility(searchTransitionProgress > 0.5f ? View.GONE : View.VISIBLE);
        searchListView.setEmptyView(emptyView);
        avatarContainer.setClickable(false);

        valueAnimator.addUpdateListener(animation -> {
            searchTransitionProgress = (float) valueAnimator.getAnimatedValue();
            float progressHalf = (searchTransitionProgress - 0.5f) / 0.5f;
            float progressHalfEnd = (0.5f - searchTransitionProgress) / 0.5f;
            if (progressHalf < 0) {
                progressHalf = 0f;
            }
            if (progressHalfEnd < 0) {
                progressHalfEnd = 0f;
            }

            searchTransitionOffset = (int) (-offset * (1f - searchTransitionProgress));
            searchListView.setTranslationY(offset * searchTransitionProgress);
            emptyView.setTranslationY(offset * searchTransitionProgress);
            listView.setTranslationY(-offset * (1f - searchTransitionProgress));

            listView.setScaleX(1f - 0.01f * (1f - searchTransitionProgress));
            listView.setScaleY(1f - 0.01f * (1f - searchTransitionProgress));
            listView.setAlpha(searchTransitionProgress);
            needLayout(true);

            listView.setAlpha(progressHalf);

            searchListView.setAlpha(1f - searchTransitionProgress);
            searchListView.setScaleX(1f + 0.05f * searchTransitionProgress);
            searchListView.setScaleY(1f + 0.05f * searchTransitionProgress);
            emptyView.setAlpha(1f - progressHalf);

            avatarContainer.setAlpha(progressHalf);
            if (storyView != null) {
                storyView.setAlpha(progressHalf);
            }
            nameTextView[1].setAlpha(progressHalf);
            onlineTextView[1].setAlpha(progressHalf);
            onlineTextView[3].setAlpha(progressHalf);

            searchItem.getSearchField().setAlpha(progressHalfEnd);
            if (enter && searchTransitionProgress < 0.7f) {
                searchItem.requestFocusOnSearchView();
            }

            searchItem.getSearchContainer().setVisibility(searchTransitionProgress < 0.5f ? View.VISIBLE : View.GONE);
            int visibility = searchTransitionProgress > 0.5f ? View.VISIBLE : View.GONE;
            if (otherItem != null) {
                otherItem.setVisibility(visibility);
                otherItem.setAlpha(progressHalf);
            }
            if (qrItem != null) {
                qrItem.setAlpha(progressHalf);
                updateQrItemVisibility(false);
            }
            searchItem.setVisibility(visibility);

            actionBar.onSearchFieldVisibilityChanged(searchTransitionProgress < 0.5f);

            if (otherItem != null) {
                otherItem.setAlpha(progressHalf);
            }
            if (qrItem != null) {
                qrItem.setAlpha(progressHalf);
            }
            searchItem.setAlpha(progressHalf);
            topView.invalidate();
            fragmentView.invalidate();
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                updateSearchViewState(enter);
                avatarContainer.setClickable(true);
                if (enter) {
                    searchItem.requestFocusOnSearchView();
                }
                needLayout(true);
                searchViewTransition = null;
                fragmentView.invalidate();

                if (enter) {
                    invalidateScroll = true;
                    saveScrollPosition();
                    AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
                    emptyView.setPreventMoving(false);
                }
            }
        });

        if (!enter) {
            invalidateScroll = true;
            saveScrollPosition();
            AndroidUtilities.requestAdjustNothing(getParentActivity(), classGuid);
            emptyView.setPreventMoving(true);
        }

        valueAnimator.setDuration(220);
        valueAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        searchViewTransition = valueAnimator;
        return valueAnimator;
    }

    private void updateSearchViewState(boolean enter) {
        int hide = enter ? View.GONE : View.VISIBLE;
        listView.setVisibility(hide);
        searchListView.setVisibility(enter ? View.VISIBLE : View.GONE);
        searchItem.getSearchContainer().setVisibility(enter ? View.VISIBLE : View.GONE);

        actionBar.onSearchFieldVisibilityChanged(enter);

        avatarContainer.setVisibility(hide);
        if (storyView != null) {
            storyView.setVisibility(hide);
        }
        nameTextView[1].setVisibility(hide);
        onlineTextView[1].setVisibility(hide);
        onlineTextView[3].setVisibility(hide);

        if (otherItem != null) {
            otherItem.setAlpha(1f);
            otherItem.setVisibility(hide);
        }
        if (qrItem != null) {
            qrItem.setAlpha(1f);
            qrItem.setVisibility(enter || !isQrNeedVisible() ? View.GONE : View.VISIBLE);
        }
        searchItem.setVisibility(hide);

        avatarContainer.setAlpha(1f);
        if (storyView != null) {
            storyView.setAlpha(1f);
        }
        if (giftsView != null) {
            giftsView.setAlpha(1f);
        }
        nameTextView[1].setAlpha(1f);
        onlineTextView[1].setAlpha(1f);
        searchItem.setAlpha(1f);
        listView.setAlpha(1f);
        searchListView.setAlpha(1f);
        emptyView.setAlpha(1f);
        if (enter) {
            searchListView.setEmptyView(emptyView);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUploadProgressChanged(float progress) {
        if (avatarProgressView == null) {
            return;
        }
        avatarProgressView.setProgress(progress);
        avatarsViewPager.setUploadProgress(viewModel.getState().getValue().getUploadingImageLocation(), progress);
    }

    @Override
    public void didStartUpload(boolean fromAvatarConstructor, boolean isVideo) {
        if (avatarProgressView == null) {
            return;
        }
        avatarProgressView.setProgress(0.0f);
    }

    @Override
    public void didUploadPhoto(final TLRPC.InputFile photo, final TLRPC.InputFile video, double videoStartTimestamp, String videoPath, TLRPC.PhotoSize bigSize, final TLRPC.PhotoSize smallSize, boolean isVideo, TLRPC.VideoSize emojiMarkup) {
        AndroidUtilities.runOnUIThread(() -> {
            if (photo != null || video != null || emojiMarkup != null) {
                if (viewModel.getState().getValue().getAvatar() == null) {
                    return;
                }
                TLRPC.TL_photos_uploadProfilePhoto req = new TLRPC.TL_photos_uploadProfilePhoto();
                if (photo != null) {
                    req.file = photo;
                    req.flags |= 1;
                }
                if (video != null) {
                    req.video = video;
                    req.flags |= 2;
                    req.video_start_ts = videoStartTimestamp;
                    req.flags |= 4;
                }
                if (emojiMarkup != null) {
                    req.video_emoji_markup = emojiMarkup;
                    req.flags |= 16;
                }
                int avatarUploadingRequest = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    if (error == null) {
                        TLRPC.User user = getMessagesController().getUser(getUserConfig().getClientUserId());
                        if (user == null) {
                            user = getUserConfig().getCurrentUser();
                            if (user == null) {
                                return;
                            }
                            getMessagesController().putUser(user, false);
                        } else {
                            getUserConfig().setCurrentUser(user);
                        }

                        TLRPC.TL_photos_photo photos_photo = (TLRPC.TL_photos_photo) response;
                        ArrayList<TLRPC.PhotoSize> sizes = photos_photo.photo.sizes;
                        TLRPC.PhotoSize small = FileLoader.getClosestPhotoSizeWithSize(sizes, 150);
                        TLRPC.PhotoSize big = FileLoader.getClosestPhotoSizeWithSize(sizes, 800);
                        TLRPC.VideoSize videoSize = photos_photo.photo.video_sizes.isEmpty() ? null : FileLoader.getClosestVideoSizeWithSize(photos_photo.photo.video_sizes, 1000);
                        user.photo = new TLRPC.TL_userProfilePhoto();
                        user.photo.photo_id = photos_photo.photo.id;
                        if (small != null) {
                            user.photo.photo_small = small.location;
                        }
                        if (big != null) {
                            user.photo.photo_big = big.location;
                        }

                        if (small != null && viewModel.getState().getValue().getAvatar() != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(small, true);
                            File src = FileLoader.getInstance(currentAccount).getPathToAttach(viewModel.getState().getValue().getAvatar(), true);
                            src.renameTo(destFile);
                            String oldKey = viewModel.getState().getValue().getAvatar().volume_id + "_" + viewModel.getState().getValue().getAvatar().local_id + "@50_50";
                            String newKey = small.location.volume_id + "_" + small.location.local_id + "@50_50";
                            ImageLoader.getInstance().replaceImageInCache(oldKey, newKey, ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL), false);
                        }

                        if (videoSize != null && videoPath != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(videoSize, "mp4", true);
                            File src = new File(videoPath);
                            src.renameTo(destFile);
                        } else if (big != null && viewModel.getState().getValue().getAvatarBig() != null) {
                            File destFile = FileLoader.getInstance(currentAccount).getPathToAttach(big, true);
                            File src = FileLoader.getInstance(currentAccount).getPathToAttach(viewModel.getState().getValue().getAvatarBig(), true);
                            src.renameTo(destFile);
                        }
                        getMessagesController().getDialogPhotos(user.id).addPhotoAtStart(((TLRPC.TL_photos_photo) response).photo);
                        ArrayList<TLRPC.User> users = new ArrayList<>();
                        users.add(user);
                        getMessagesStorage().putUsersAndChats(users, null, false, true);
                        TLRPC.UserFull userFull = getMessagesController().getUserFull(viewModel.getArgs().userId);
                        if (userFull != null) {
                            userFull.profile_photo = photos_photo.photo;
                            getMessagesStorage().updateUserInfo(userFull, false);
                        }
                    }

                    viewModel.allowPullingDown(!AndroidUtilities.isTablet() && !viewModel.getState().getValue().isInLandscapeMode() && avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled());
                    viewModel.avatar(null);
                    viewModel.avatarBig(null);
                    avatarsViewPager.scrolledByUser = true;
                    avatarsViewPager.removeUploadingImage(viewModel.getState().getValue().getUploadingImageLocation());
                    avatarsViewPager.setCreateThumbFromParent(false);
                    updateProfileData(true);
                    showAvatarProgress(false, true);
                    getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                    getUserConfig().saveConfig(true);

                }));
                viewModel.avatarUploadingRequest(avatarUploadingRequest);
            } else {
                viewModel.avatar(smallSize.location);
                viewModel.avatarBig(bigSize.location);
                avatarImage.setImage(ImageLocation.getForLocal(viewModel.getState().getValue().getAvatar()), "50_50", avatarDrawable, null);
                if (setAvatarRow != -1) {
                    updateRowsIds();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    needLayout(true);
                }
                viewModel.uploadingImageLocation(ImageLocation.getForLocal(viewModel.getState().getValue().getAvatarBig()));
                avatarsViewPager.addUploadingImage(viewModel.getState().getValue().getUploadingImageLocation(), ImageLocation.getForLocal(viewModel.getState().getValue().getAvatar()));
                showAvatarProgress(true, false);
            }
            actionBar.createMenu().requestLayout();
        });
    }

    public void showAvatarProgress(boolean show, boolean animated) {
        if (avatarProgressView == null) {
            return;
        }
        if (avatarAnimation != null) {
            avatarAnimation.cancel();
            avatarAnimation = null;
        }
        if (animated) {
            avatarAnimation = new AnimatorSet();
            if (show) {
                avatarProgressView.setVisibility(View.VISIBLE);
                avatarAnimation.playTogether(ObjectAnimator.ofFloat(avatarProgressView, View.ALPHA, 1.0f));
            } else {
                avatarAnimation.playTogether(ObjectAnimator.ofFloat(avatarProgressView, View.ALPHA, 0.0f));
            }
            avatarAnimation.setDuration(180);
            avatarAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (avatarAnimation == null || avatarProgressView == null) {
                        return;
                    }
                    if (!show) {
                        avatarProgressView.setVisibility(View.INVISIBLE);
                    }
                    avatarAnimation = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    avatarAnimation = null;
                }
            });
            avatarAnimation.start();
        } else {
            if (show) {
                avatarProgressView.setAlpha(1.0f);
                avatarProgressView.setVisibility(View.VISIBLE);
            } else {
                avatarProgressView.setAlpha(0.0f);
                avatarProgressView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (imageUpdater != null) {
            imageUpdater.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void saveSelfArgs(Bundle args) {
        if (imageUpdater != null && imageUpdater.currentPicturePath != null) {
            args.putString("path", imageUpdater.currentPicturePath);
        }
    }

    @Override
    public void restoreSelfArgs(Bundle args) {
        if (imageUpdater != null) {
            imageUpdater.currentPicturePath = args.getString("path");
        }
    }

    public static void sendLogs(Activity activity, boolean last) {
        if (activity == null) {
            return;
        }
        AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                File dir = AndroidUtilities.getLogsDir();
                if (dir == null) {
                    AndroidUtilities.runOnUIThread(progressDialog::dismiss);
                    return;
                }

                File zipFile = new File(dir, "logs.zip");
                if (zipFile.exists()) {
                    zipFile.delete();
                }

                ArrayList<File> files = new ArrayList<>();

                File[] logFiles = dir.listFiles();
                for (File f : logFiles) {
                    files.add(f);
                }

                File filesDir = ApplicationLoader.getFilesDirFixed();
                filesDir = new File(filesDir, "malformed_database/");
                if (filesDir.exists() && filesDir.isDirectory()) {
                    File[] malformedDatabaseFiles = filesDir.listFiles();
                    for (File file : malformedDatabaseFiles) {
                        files.add(file);
                    }
                }

                boolean[] finished = new boolean[1];
                long currentDate = System.currentTimeMillis();

                BufferedInputStream origin = null;
                ZipOutputStream out = null;
                try {
                    FileOutputStream dest = new FileOutputStream(zipFile);
                    out = new ZipOutputStream(new BufferedOutputStream(dest));
                    byte[] data = new byte[1024 * 64];

                    for (int i = 0; i < files.size(); i++) {
                        File file = files.get(i);
                        if (!file.getName().contains("cache4") && (last || file.getName().contains("_mtproto")) && (currentDate - file.lastModified()) > 24 * 60 * 60 * 1000) {
                            continue;
                        }
                        if (!file.exists()) {
                            continue;
                        }
                        FileInputStream fi = new FileInputStream(file);
                        origin = new BufferedInputStream(fi, data.length);

                        ZipEntry entry = new ZipEntry(file.getName());
                        out.putNextEntry(entry);
                        int count;
                        while ((count = origin.read(data, 0, data.length)) != -1) {
                            out.write(data, 0, count);
                        }
                        origin.close();
                        origin = null;
                    }
                    finished[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (origin != null) {
                        origin.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignore) {

                    }
                    if (finished[0]) {
                        Uri uri;
                        if (Build.VERSION.SDK_INT >= 24) {
                            uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", zipFile);
                        } else {
                            uri = Uri.fromFile(zipFile);
                        }

                        Intent i = new Intent(Intent.ACTION_SEND);
                        if (Build.VERSION.SDK_INT >= 24) {
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        i.setType("message/rfc822");
                        i.putExtra(Intent.EXTRA_EMAIL, "");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Logs from " + LocaleController.getInstance().getFormatterStats().format(System.currentTimeMillis()));
                        i.putExtra(Intent.EXTRA_STREAM, uri);
                        if (activity != null) {
                            try {
                                activity.startActivityForResult(Intent.createChooser(i, "Select email application."), 500);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                    } else {
                        if (activity != null) {
                            Toast.makeText(activity, LocaleController.getString(R.string.ErrorOccurred), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final static int VIEW_TYPE_HEADER = 1,
                VIEW_TYPE_TEXT_DETAIL = 2,
                VIEW_TYPE_ABOUT_LINK = 3,
                VIEW_TYPE_TEXT = 4,
                VIEW_TYPE_DIVIDER = 5,
                VIEW_TYPE_NOTIFICATIONS_CHECK = 6,
                VIEW_TYPE_SHADOW = 7,
                VIEW_TYPE_USER = 8,
                VIEW_TYPE_EMPTY = 11,
                VIEW_TYPE_BOTTOM_PADDING = 12,
                VIEW_TYPE_SHARED_MEDIA = 13,
                VIEW_TYPE_VERSION = 14,
                VIEW_TYPE_SUGGESTION = 15,
                VIEW_TYPE_ADDTOGROUP_INFO = 17,
                VIEW_TYPE_PREMIUM_TEXT_CELL = 18,
                VIEW_TYPE_TEXT_DETAIL_MULTILINE = 19,
                VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE = 20,
                VIEW_TYPE_LOCATION = 21,
                VIEW_TYPE_HOURS = 22,
                VIEW_TYPE_CHANNEL = 23,
                VIEW_TYPE_STARS_TEXT_CELL = 24,
                VIEW_TYPE_BOT_APP = 25,
                VIEW_TYPE_SHADOW_TEXT = 26,
                VIEW_TYPE_COLORFUL_TEXT = 27;

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER: {
                    view = new HeaderCell(mContext, 23, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_TEXT_DETAIL_MULTILINE:
                case VIEW_TYPE_TEXT_DETAIL:
                    final TextDetailCell textDetailCell = new TextDetailCell(mContext, resourcesProvider, viewType == VIEW_TYPE_TEXT_DETAIL_MULTILINE) {
                        @Override
                        protected int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    textDetailCell.setContentDescriptionValueFirst(true);
                    view = textDetailCell;
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_ABOUT_LINK: {
                    view = aboutLinkCell = new AboutLinkCell(mContext, ProfileActivity.this, resourcesProvider) {
                        @Override
                        protected void didPressUrl(String url, Browser.Progress progress) {
                            openUrl(url, progress);
                        }

                        @Override
                        protected void didResizeEnd() {
                            layoutManager.mIgnoreTopPadding = false;
                        }

                        @Override
                        protected void didResizeStart() {
                            layoutManager.mIgnoreTopPadding = true;
                        }

                        @Override
                        protected int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_TEXT: {
                    view = new TextCell(mContext, resourcesProvider) {
                        @Override
                        protected int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_DIVIDER: {
                    view = new DividerCell(mContext, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    view.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), 0, 0);
                    break;
                }
                case VIEW_TYPE_NOTIFICATIONS_CHECK: {
                    view = new NotificationsCheckCell(mContext, 23, 70, false, resourcesProvider) {
                        @Override
                        protected int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE: {
                    view = new TextCheckCell(mContext, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_SHADOW: {
                    view = new ShadowSectionCell(mContext, resourcesProvider);
                    break;
                }
                case VIEW_TYPE_SHADOW_TEXT: {
                    view = new TextInfoPrivacyCell(mContext, resourcesProvider);
                    break;
                }
                case VIEW_TYPE_COLORFUL_TEXT: {
                    view = new AffiliateProgramFragment.ColorfulTextCell(mContext, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_USER: {
                    view = new UserCell(mContext, addMemberRow == -1 ? 9 : 6, 0, true, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_EMPTY: {
                    view = new View(mContext) {
                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
                        }
                    };
                    break;
                }
                case VIEW_TYPE_BOTTOM_PADDING: {
                    view = new View(mContext) {

                        private int lastPaddingHeight = 0;
                        private int lastListViewHeight = 0;

                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            if (lastListViewHeight != listView.getMeasuredHeight()) {
                                lastPaddingHeight = 0;
                            }
                            lastListViewHeight = listView.getMeasuredHeight();
                            int n = listView.getChildCount();
                            if (n == listAdapter.getItemCount()) {
                                int totalHeight = 0;
                                for (int i = 0; i < n; i++) {
                                    View view = listView.getChildAt(i);
                                    int p = listView.getChildAdapterPosition(view);
                                    if (p >= 0 && p != bottomPaddingRow) {
                                        totalHeight += listView.getChildAt(i).getMeasuredHeight();
                                    }
                                }
                                int paddingHeight = (fragmentView == null ? 0 : fragmentView.getMeasuredHeight()) - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.statusBarHeight - totalHeight;
                                if (paddingHeight > AndroidUtilities.dp(88)) {
                                    paddingHeight = 0;
                                }
                                if (paddingHeight <= 0) {
                                    paddingHeight = 0;
                                }
                                setMeasuredDimension(listView.getMeasuredWidth(), lastPaddingHeight = paddingHeight);
                            } else {
                                setMeasuredDimension(listView.getMeasuredWidth(), lastPaddingHeight);
                            }
                        }
                    };
                    view.setBackground(new ColorDrawable(Color.TRANSPARENT));
                    break;
                }
                case VIEW_TYPE_SHARED_MEDIA: {
                    if (sharedMediaLayout.getParent() != null) {
                        ((ViewGroup) sharedMediaLayout.getParent()).removeView(sharedMediaLayout);
                    }
                    view = sharedMediaLayout;
                    break;
                }
                case VIEW_TYPE_ADDTOGROUP_INFO: {
                    view = new TextInfoPrivacyCell(mContext, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_LOCATION:
                    view = new ProfileLocationCell(mContext, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_HOURS:
                    view = new ProfileHoursCell(mContext, resourcesProvider) {
                        @Override
                        protected int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_VERSION:
                default: {
                    TextInfoPrivacyCell cell = new TextInfoPrivacyCell(mContext, 10, resourcesProvider);
                    cell.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
                    cell.getTextView().setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText3));
                    cell.getTextView().setMovementMethod(null);
                    try {
                        PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                        int code = pInfo.versionCode / 10;
                        String abi = "";
                        switch (pInfo.versionCode % 10) {
                            case 1:
                            case 2:
                                abi = "store bundled " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                                break;
                            default:
                            case 9:
                                if (ApplicationLoader.isStandaloneBuild()) {
                                    abi = "direct " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                                } else {
                                    abi = "universal " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                                }
                                break;
                        }
                        cell.setText(formatString("TelegramVersion", R.string.TelegramVersion, String.format(Locale.US, "v%s (%d) %s", pInfo.versionName, code, abi)));
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    cell.getTextView().setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
                    view = cell;
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    break;
                }
                case VIEW_TYPE_SUGGESTION: {
                    view = new SettingsSuggestionCell(mContext, resourcesProvider) {
                        @Override
                        protected void onYesClick(int type) {
                            AndroidUtilities.runOnUIThread(() -> {
                                getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.newSuggestionsAvailable);
                                if (type == SettingsSuggestionCell.TYPE_GRACE) {
                                    getMessagesController().removeSuggestion(0, "PREMIUM_GRACE");
                                    Browser.openUrl(getContext(), getMessagesController().premiumManageSubscriptionUrl);
                                } else {
                                    getMessagesController().removeSuggestion(0, type == SettingsSuggestionCell.TYPE_PHONE ? "VALIDATE_PHONE_NUMBER" : "VALIDATE_PASSWORD");
                                }
                                getNotificationCenter().addObserver(ProfileActivity.this, NotificationCenter.newSuggestionsAvailable);
                                updateListAnimated(false);
                            });
                        }

                        @Override
                        protected void onNoClick(int type) {
                            if (type == SettingsSuggestionCell.TYPE_PHONE) {
                                presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER));
                            } else {
                                presentFragment(new TwoStepVerificationSetupActivity(TwoStepVerificationSetupActivity.TYPE_VERIFY, null));
                            }
                        }
                    };
                    break;
                }
                case VIEW_TYPE_PREMIUM_TEXT_CELL:
                case VIEW_TYPE_STARS_TEXT_CELL:
                    view = new ProfilePremiumCell(mContext, viewType == VIEW_TYPE_PREMIUM_TEXT_CELL ? 0 : 1, resourcesProvider);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_CHANNEL:
                    view = new ProfileChannelCell(ProfileActivity.this) {
                        @Override
                        public int processColor(int color) {
                            return dontApplyPeerColor(color, false);
                        }
                    };
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_BOT_APP:
                    FrameLayout frameLayout = new FrameLayout(mContext);
                    ButtonWithCounterView button = new ButtonWithCounterView(mContext, resourcesProvider);
                    button.setText(LocaleController.getString(R.string.ProfileBotOpenApp), false);
                    button.setOnClickListener(v -> {
                        TLRPC.User bot = getMessagesController().getUser(viewModel.getArgs().userId);
                        getMessagesController().openApp(ProfileActivity.this, bot, null, getClassGuid(), null);
                    });
                    frameLayout.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.FILL, 18, 14, 18, 14));
                    view = frameLayout;
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            if (viewType != VIEW_TYPE_SHARED_MEDIA) {
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView == sharedMediaLayout) {
                sharedMediaLayoutAttached = true;
            }
            if (holder.itemView instanceof TextDetailCell) {
                ((TextDetailCell) holder.itemView).textView.setLoading(loadingSpan);
                ((TextDetailCell) holder.itemView).valueTextView.setLoading(loadingSpan);
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView == sharedMediaLayout) {
                sharedMediaLayoutAttached = false;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == infoHeaderRow) {
                        if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup() && channelInfoRow != -1) {
                            headerCell.setText(LocaleController.getString(R.string.ReportChatDescription));
                        } else {
                            headerCell.setText(LocaleController.getString(R.string.Info));
                        }
                    } else if (position == membersHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.ChannelMembers));
                    } else if (position == settingsSectionRow2) {
                        headerCell.setText(LocaleController.getString(R.string.SETTINGS));
                    } else if (position == numberSectionRow) {
                        headerCell.setText(LocaleController.getString(R.string.Account));
                    } else if (position == helpHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.SettingsHelp));
                    } else if (position == debugHeaderRow) {
                        headerCell.setText(LocaleController.getString(R.string.SettingsDebug));
                    } else if (position == botPermissionsHeader) {
                        headerCell.setText(LocaleController.getString(R.string.BotProfilePermissions));
                    }
                    headerCell.setTextColor(dontApplyPeerColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), false));
                    break;
                case VIEW_TYPE_TEXT_DETAIL_MULTILINE:
                case VIEW_TYPE_TEXT_DETAIL:
                    TextDetailCell detailCell = (TextDetailCell) holder.itemView;
                    boolean containsQr = false;
                    boolean containsGift = false;
                    if (position == birthdayRow) {
                        TLRPC.UserFull userFull = getMessagesController().getUserFull(viewModel.getArgs().userId);
                        if (userFull != null && userFull.birthday != null) {
                            final boolean today = BirthdayController.isToday(userFull);
                            final boolean withYear = (userFull.birthday.flags & 1) != 0;
                            final int age = withYear ? Period.between(LocalDate.of(userFull.birthday.year, userFull.birthday.month, userFull.birthday.day), LocalDate.now()).getYears() : -1;

                            String text = UserInfoActivity.birthdayString(userFull.birthday);

                            if (withYear) {
                                text = LocaleController.formatPluralString(today ? "ProfileBirthdayTodayValueYear" : "ProfileBirthdayValueYear", age, text);
                            } else {
                                text = LocaleController.formatString(today ? R.string.ProfileBirthdayTodayValue : R.string.ProfileBirthdayValue, text);
                            }

                            detailCell.setTextAndValue(
                                Emoji.replaceWithRestrictedEmoji(text, detailCell.textView, () -> {
                                    if (holder.getAdapterPosition() == position && birthdayRow == position && holder.getItemViewType() == VIEW_TYPE_TEXT_DETAIL) {
                                        onBindViewHolder(holder, position);
                                    }
                                }),
                                LocaleController.getString(today ? R.string.ProfileBirthdayToday : R.string.ProfileBirthday),
                                    viewModel.getArgs().isTopic || bizHoursRow != -1 || bizLocationRow != -1
                            );

                            containsGift = !viewModel.getArgs().myProfile && today && !getMessagesController().premiumPurchaseBlocked();
                        }
                    } else if (position == phoneRow) {
                        String text;
                        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                        String phoneNumber;
                        if (user != null && !TextUtils.isEmpty(viewModel.getArgs().vcardPhone)) {
                            text = PhoneFormat.getInstance().format("+" + viewModel.getArgs().vcardPhone);
                            phoneNumber = viewModel.getArgs().vcardPhone;
                        } else if (user != null && !TextUtils.isEmpty(user.phone)) {
                            text = PhoneFormat.getInstance().format("+" + user.phone);
                            phoneNumber = user.phone;
                        } else {
                            text = LocaleController.getString(R.string.PhoneHidden);
                            phoneNumber = null;
                        }
                        isFragmentPhoneNumber = phoneNumber != null && phoneNumber.matches("888\\d{8}");
                        detailCell.setTextAndValue(text, LocaleController.getString(isFragmentPhoneNumber ? R.string.AnonymousNumber : R.string.PhoneMobile), false);
                    } else if (position == usernameRow) {
                        String username = null;
                        CharSequence text;
                        CharSequence value;
                        ArrayList<TLRPC.TL_username> usernames = new ArrayList<>();
                        if (viewModel.getArgs().userId != 0) {
                            final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                            if (user != null) {
                                usernames.addAll(user.usernames);
                            }
                            TLRPC.TL_username usernameObj = null;
                            if (user != null && !TextUtils.isEmpty(user.username)) {
                                usernameObj = DialogObject.findUsername(user.username, usernames);
                                username = user.username;
                            }
                            usernames = user == null ? new ArrayList<>() : new ArrayList<>(user.usernames);
                            if (TextUtils.isEmpty(username) && usernames != null) {
                                for (int i = 0; i < usernames.size(); ++i) {
                                    TLRPC.TL_username u = usernames.get(i);
                                    if (u != null && u.active && !TextUtils.isEmpty(u.username)) {
                                        usernameObj = u;
                                        username = u.username;
                                        break;
                                    }
                                }
                            }
                            value = LocaleController.getString(R.string.Username);
                            if (username != null) {
                                text = "@" + username;
                                if (usernameObj != null && !usernameObj.editable) {
                                    text = new SpannableString(text);
                                    ((SpannableString) text).setSpan(makeUsernameLinkSpan(usernameObj), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            } else {
                                text = "—";
                            }
                            containsQr = true;
                        } else if (viewModel.getState().getValue().getCurrentChat() != null) {
                            TLRPC.Chat chat = getMessagesController().getChat(viewModel.getArgs().chatId);
                            username = ChatObject.getPublicUsername(chat);
                            if (chat != null) {
                                usernames.addAll(chat.usernames);
                            }
                            if (ChatObject.isPublic(chat)) {
                                containsQr = true;
                                text = getMessagesController().linkPrefix + "/" + username + (viewModel.getArgs().topicId != 0 ? "/" + viewModel.getArgs().topicId : "");
                                value = LocaleController.getString(R.string.InviteLink);
                            } else {
                                text = getMessagesController().linkPrefix + "/c/" + viewModel.getArgs().chatId + (viewModel.getArgs().topicId != 0 ? "/" + viewModel.getArgs().topicId : "");
                                value = LocaleController.getString(R.string.InviteLinkPrivate);
                            }
                        } else {
                            text = "";
                            value = "";
                            usernames = new ArrayList<>();
                        }
                        detailCell.setTextAndValue(text, alsoUsernamesString(username, usernames, value), (viewModel.getArgs().isTopic || bizHoursRow != -1 || bizLocationRow != -1) && birthdayRow < 0);
                    } else if (position == locationRow) {
                        if (viewModel.getState().getValue().getChatInfo() != null && viewModel.getState().getValue().getChatInfo().location instanceof TLRPC.TL_channelLocation) {
                            TLRPC.TL_channelLocation location = (TLRPC.TL_channelLocation) viewModel.getState().getValue().getChatInfo().location;
                            detailCell.setTextAndValue(location.address, LocaleController.getString(R.string.AttachLocation), false);
                        }
                    } else if (position == numberRow) {
                        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
                        String value;
                        if (user != null && user.phone != null && user.phone.length() != 0) {
                            value = PhoneFormat.getInstance().format("+" + user.phone);
                        } else {
                            value = LocaleController.getString(R.string.NumberUnknown);
                        }
                        detailCell.setTextAndValue(value, LocaleController.getString(R.string.TapToChangePhone), true);
                        detailCell.setContentDescriptionValueFirst(false);
                    } else if (position == setUsernameRow) {
                        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
                        String text = "";
                        CharSequence value = LocaleController.getString(R.string.Username);
                        String username = null;
                        if (user != null && user.usernames.size() > 0) {
                            for (int i = 0; i < user.usernames.size(); ++i) {
                                TLRPC.TL_username u = user.usernames.get(i);
                                if (u != null && u.active && !TextUtils.isEmpty(u.username)) {
                                    username = u.username;
                                    break;
                                }
                            }
                            if (username == null) {
                                username = user.username;
                            }
                            if (username == null || TextUtils.isEmpty(username)) {
                                text = LocaleController.getString(R.string.UsernameEmpty);
                            } else {
                                text = "@" + username;
                            }
                            value = alsoUsernamesString(username, user.usernames, value);
                        } else {
                            username = UserObject.getPublicUsername(user);
                            if (user != null && !TextUtils.isEmpty(username)) {
                                text = "@" + username;
                            } else {
                                text = LocaleController.getString(R.string.UsernameEmpty);
                            }
                        }
                        detailCell.setTextAndValue(text, value, true);
                        detailCell.setContentDescriptionValueFirst(true);
                    }
                    if (containsGift) {
                        Drawable drawable = ContextCompat.getDrawable(detailCell.getContext(), R.drawable.msg_input_gift);
                        drawable.setColorFilter(new PorterDuffColorFilter(dontApplyPeerColor(getThemedColor(Theme.key_switch2TrackChecked), false), PorterDuff.Mode.MULTIPLY));
                        if (UserObject.areGiftsDisabled(viewModel.getState().getValue().getUserInfo())) {
                            detailCell.setImage(null);
                            detailCell.setImageClickListener(null);
                        } else {
                            detailCell.setImage(drawable, LocaleController.getString(R.string.GiftPremium));
                            detailCell.setImageClickListener(ProfileActivity.this::onTextDetailCellImageClicked);
                        }
                    } else if (containsQr) {
                        Drawable drawable = ContextCompat.getDrawable(detailCell.getContext(), R.drawable.msg_qr_mini);
                        drawable.setColorFilter(new PorterDuffColorFilter(dontApplyPeerColor(getThemedColor(Theme.key_switch2TrackChecked), false), PorterDuff.Mode.MULTIPLY));
                        detailCell.setImage(drawable, LocaleController.getString(R.string.GetQRCode));
                        detailCell.setImageClickListener(ProfileActivity.this::onTextDetailCellImageClicked);
                    } else {
                        detailCell.setImage(null);
                        detailCell.setImageClickListener(null);
                    }
                    detailCell.setTag(position);
                    detailCell.textView.setLoading(loadingSpan);
                    detailCell.valueTextView.setLoading(loadingSpan);
                    break;
                case VIEW_TYPE_ABOUT_LINK:
                    AboutLinkCell aboutLinkCell = (AboutLinkCell) holder.itemView;
                    if (position == userInfoRow) {
                        TLRPC.User user = viewModel.getState().getValue().getUserInfo().user != null ? viewModel.getState().getValue().getUserInfo().user : getMessagesController().getUser(viewModel.getState().getValue().getUserInfo().id);
                        boolean addlinks = viewModel.getState().getValue().isBot() || (user != null && user.premium && viewModel.getState().getValue().getUserInfo().about != null);
                        aboutLinkCell.setTextAndValue(viewModel.getState().getValue().getUserInfo().about, LocaleController.getString(R.string.UserBio), addlinks);
                    } else if (position == channelInfoRow) {
                        String text = viewModel.getState().getValue().getChatInfo().about;
                        while (text.contains("\n\n\n")) {
                            text = text.replace("\n\n\n", "\n\n");
                        }
                        aboutLinkCell.setText(text, ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup());
                    } else if (position == bioRow) {
                        String value;
                        if (viewModel.getState().getValue().getUserInfo() == null || !TextUtils.isEmpty(viewModel.getState().getValue().getUserInfo().about)) {
                            value = viewModel.getState().getValue().getUserInfo() == null ? LocaleController.getString(R.string.Loading) : viewModel.getState().getValue().getUserInfo().about;
                            aboutLinkCell.setTextAndValue(value, LocaleController.getString(R.string.UserBio), getUserConfig().isPremium());
                            currentBio = viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().about : null;
                        } else {
                            aboutLinkCell.setTextAndValue(LocaleController.getString(R.string.UserBio), LocaleController.getString(R.string.UserBioDetail), false);
                            currentBio = null;
                        }
                        aboutLinkCell.setMoreButtonDisabled(true);
                    }
                    break;
                case VIEW_TYPE_PREMIUM_TEXT_CELL:
                case VIEW_TYPE_STARS_TEXT_CELL:
                case VIEW_TYPE_TEXT:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                    textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                    if (position == settingsTimerRow) {
                        TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(viewModel.getArgs().dialogId));
                        String value;
                        if (encryptedChat.ttl == 0) {
                            value = LocaleController.getString(R.string.ShortMessageLifetimeForever);
                        } else {
                            value = LocaleController.formatTTLString(encryptedChat.ttl);
                        }
                        textCell.setTextAndValue(LocaleController.getString(R.string.MessageLifetime), value, false,false);
                    } else if (position == unblockRow) {
                        textCell.setText(LocaleController.getString(R.string.Unblock), false);
                        textCell.setColors(-1, Theme.key_text_RedRegular);
                    } else if (position == settingsKeyRow) {
                        IdenticonDrawable identiconDrawable = new IdenticonDrawable();
                        TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(viewModel.getArgs().dialogId));
                        identiconDrawable.setEncryptedChat(encryptedChat);
                        textCell.setTextAndValueDrawable(LocaleController.getString(R.string.EncryptionKey), identiconDrawable, false);
                    } else if (position == joinRow) {
                        textCell.setColors(-1, Theme.key_windowBackgroundWhiteBlueText2);
                        if (viewModel.getState().getValue().isCurrentChatMagagroup()) {
                            textCell.setText(LocaleController.getString(R.string.ProfileJoinGroup), false);
                        } else {
                            textCell.setText(LocaleController.getString(R.string.ProfileJoinChannel), false);
                        }
                    } else if (position == subscribersRow) {
                        if (viewModel.getState().getValue().getChatInfo() != null) {
                            if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup()) {
                                textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelSubscribers), LocaleController.formatNumber(viewModel.getState().getValue().getChatInfo().participants_count, ','), R.drawable.msg_groups, position != membersSectionRow - 1);
                            } else {
                                textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelMembers), LocaleController.formatNumber(viewModel.getState().getValue().getChatInfo().participants_count, ','), R.drawable.msg_groups, position != membersSectionRow - 1);
                            }
                        } else {
                            if (ChatObject.isChannel(viewModel.getState().getValue().getCurrentChat()) && !viewModel.getState().getValue().isCurrentChatMagagroup()) {
                                textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelSubscribers), R.drawable.msg_groups, position != membersSectionRow - 1);
                            } else {
                                textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelMembers), R.drawable.msg_groups, position != membersSectionRow - 1);
                            }
                        }
                    } else if (position == subscribersRequestsRow) {
                        if (viewModel.getState().getValue().getChatInfo() != null) {
                            textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.SubscribeRequests), String.format("%d", viewModel.getState().getValue().getChatInfo().requests_pending), R.drawable.msg_requests, position != membersSectionRow - 1);
                        }
                    } else if (position == administratorsRow) {
                        if (viewModel.getState().getValue().getChatInfo() != null) {
                            textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelAdministrators), String.format("%d", viewModel.getState().getValue().getChatInfo().admins_count), R.drawable.msg_admins, position != membersSectionRow - 1);
                        } else {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelAdministrators), R.drawable.msg_admins, position != membersSectionRow - 1);
                        }
                    } else if (position == settingsRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelAdminSettings), R.drawable.msg_customize, position != membersSectionRow - 1);
                    } else if (position == channelBalanceRow) {
                        final TL_stars.StarsAmount stars_balance = BotStarsController.getInstance(currentAccount).getBotStarsBalance(-viewModel.getArgs().chatId);
                        final long ton_balance = BotStarsController.getInstance(currentAccount).getTONBalance(-viewModel.getArgs().chatId);
                        SpannableStringBuilder ssb = new SpannableStringBuilder();
                        if (ton_balance > 0) {
                            if (ton_balance / 1_000_000_000.0 > 1000.0) {
                                ssb.append("TON ").append(AndroidUtilities.formatWholeNumber((int) (ton_balance / 1_000_000_000.0), 0));
                            } else {
                                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                                symbols.setDecimalSeparator('.');
                                DecimalFormat formatterTON = new DecimalFormat("#.##", symbols);
                                formatterTON.setMinimumFractionDigits(2);
                                formatterTON.setMaximumFractionDigits(3);
                                formatterTON.setGroupingUsed(false);
                                ssb.append("TON ").append(formatterTON.format(ton_balance / 1_000_000_000.0));
                            }
                        }
                        if (stars_balance.amount > 0) {
                            if (ssb.length() > 0) ssb.append(" ");
                            ssb.append("XTR ").append(formatStarsAmountShort(stars_balance));
                        }
                        textCell.setTextAndValueAndIcon(getString(R.string.ChannelStars), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.menu_feature_paid, true);
                    } else if (position == botStarsBalanceRow) {
                        final TL_stars.StarsAmount stars_balance = BotStarsController.getInstance(currentAccount).getBotStarsBalance(viewModel.getArgs().userId);
                        SpannableStringBuilder ssb = new SpannableStringBuilder();
                        if (stars_balance.amount > 0) {
                            ssb.append("XTR ").append(formatStarsAmountShort(stars_balance));
                        }
                        textCell.setTextAndValueAndIcon(getString(R.string.BotBalanceStars), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.menu_premium_main, true);
                    } else if (position == botTonBalanceRow) {
                        long ton_balance = BotStarsController.getInstance(currentAccount).getTONBalance(viewModel.getArgs().userId);
                        SpannableStringBuilder ssb = new SpannableStringBuilder();
                        if (ton_balance > 0) {
                            if (ton_balance / 1_000_000_000.0 > 1000.0) {
                                ssb.append("TON ").append(AndroidUtilities.formatWholeNumber((int) (ton_balance / 1_000_000_000.0), 0));
                            } else {
                                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                                symbols.setDecimalSeparator('.');
                                DecimalFormat formatterTON = new DecimalFormat("#.##", symbols);
                                formatterTON.setMinimumFractionDigits(2);
                                formatterTON.setMaximumFractionDigits(3);
                                formatterTON.setGroupingUsed(false);
                                ssb.append("TON ").append(formatterTON.format(ton_balance / 1_000_000_000.0));
                            }
                        }
                        textCell.setTextAndValueAndIcon(getString(R.string.BotBalanceTON), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.msg_ton, true);
                    } else if (position == blockedUsersRow) {
                        if (viewModel.getState().getValue().getChatInfo() != null) {
                            textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelBlacklist), String.format("%d", Math.max(viewModel.getState().getValue().getChatInfo().banned_count, viewModel.getState().getValue().getChatInfo().kicked_count)), R.drawable.msg_user_remove, position != membersSectionRow - 1);
                        } else {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelBlacklist), R.drawable.msg_user_remove, position != membersSectionRow - 1);
                        }
                    } else if (position == addMemberRow) {
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                        boolean isNextPositionMember = position + 1 >= membersStartRow && position + 1 < membersEndRow;
                        textCell.setTextAndIcon(LocaleController.getString(R.string.AddMember), R.drawable.msg_contact_add, membersSectionRow == -1 || isNextPositionMember);
                    } else if (position == sendMessageRow) {
                        textCell.setText(LocaleController.getString(R.string.SendMessageLocation), true);
                    } else if (position == addToContactsRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.AddToContacts), R.drawable.msg_contact_add, false);
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == reportReactionRow) {
                        TLRPC.Chat chat = getMessagesController().getChat(-viewModel.getArgs().reportReactionFromDialogId);
                        if (chat != null && ChatObject.canBlockUsers(chat)) {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ReportReactionAndBan), R.drawable.msg_block2, false);
                        } else {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ReportReaction), R.drawable.msg_report,false);
                        }

                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedRegular);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedRegular);
                    } else if (position == reportRow) {
                        textCell.setText(LocaleController.getString(R.string.ReportUserLocation), false);
                        textCell.setColors(-1, Theme.key_text_RedRegular);
                        textCell.setColors(-1, Theme.key_text_RedRegular);
                    } else if (position == languageRow) {
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.Language), LocaleController.getCurrentLanguageName(), false, R.drawable.msg2_language, false);
                        textCell.setImageLeft(23);
                    } else if (position == notificationRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.NotificationsAndSounds), R.drawable.msg2_notifications, true);
                    } else if (position == privacyRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.PrivacySettings), R.drawable.msg2_secret, true);
                    } else if (position == dataRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.DataSettings), R.drawable.msg2_data, true);
                    } else if (position == chatRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ChatSettings), R.drawable.msg2_discussion, true);
                    } else if (position == filtersRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.Filters), R.drawable.msg2_folder, true);
                    } else if (position == stickersRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.StickersName), R.drawable.msg2_sticker, true);
                    } else if (position == liteModeRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.PowerUsage), R.drawable.msg2_battery, true);
                    } else if (position == questionRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.AskAQuestion), R.drawable.msg2_ask_question, true);
                    } else if (position == faqRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramFAQ), R.drawable.msg2_help, true);
                    } else if (position == policyRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.PrivacyPolicy), R.drawable.msg2_policy, false);
                    } else if (position == sendLogsRow) {
                        textCell.setText(LocaleController.getString(R.string.DebugSendLogs), true);
                    } else if (position == sendLastLogsRow) {
                        textCell.setText(LocaleController.getString(R.string.DebugSendLastLogs), true);
                    } else if (position == clearLogsRow) {
                        textCell.setText(LocaleController.getString(R.string.DebugClearLogs), switchBackendRow != -1);
                    } else if (position == switchBackendRow) {
                        textCell.setText("Switch Backend", false);
                    } else if (position == devicesRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.Devices), R.drawable.msg2_devices, true);
                    } else if (position == setAvatarRow) {
                        cellCameraDrawable.setCustomEndFrame(86);
                        cellCameraDrawable.setCurrentFrame(85, false);
                        textCell.setTextAndIcon(LocaleController.getString(R.string.SetProfilePhoto), cellCameraDrawable, false);
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                        textCell.getImageView().setPadding(0, 0, 0, AndroidUtilities.dp(8));
                        textCell.setImageLeft(12);
                        setAvatarCell = textCell;
                    } else if (position == addToGroupButtonRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.AddToGroupOrChannel), R.drawable.msg_groups_create, false);
                    } else if (position == premiumRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramPremium), new AnimatedEmojiDrawable.WrapSizeDrawable(PremiumGradient.getInstance().premiumStarMenuDrawable, dp(24), dp(24)), true);
                        textCell.setImageLeft(23);
                    } else if (position == starsRow) {
                        StarsController c = StarsController.getInstance(currentAccount);
                        long balance = c.getBalance().amount;
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.MenuTelegramStars), c.balanceAvailable() && balance > 0 ? StarsIntroActivity.formatStarsAmount(c.getBalance(), 0.85f, ' ') : "", new AnimatedEmojiDrawable.WrapSizeDrawable(PremiumGradient.getInstance().goldenStarMenuDrawable, dp(24), dp(24)), true);
                        textCell.setImageLeft(23);
                    } else if (position == tonRow) {
                        StarsController c = StarsController.getTonInstance(currentAccount);
                        long balance = c.getBalance().amount;
                        textCell.setTextAndValueAndIcon(getString(R.string.MyTON), c.balanceAvailable() && balance > 0 ? StarsIntroActivity.formatStarsAmount(c.getBalance(), 0.85f, ' ') : "", R.drawable.menu_my_ton, true);
                        textCell.setImageLeft(23);
                    } else if (position == businessRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramBusiness), R.drawable.menu_shop, true);
                        textCell.setImageLeft(23);
                    } else if (position == premiumGiftingRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.SendAGift), R.drawable.menu_gift, false);
                        textCell.setImageLeft(23);
                    } else if (position == botPermissionLocation) {
                        textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionLocation), botLocation != null && botLocation.granted(), R.drawable.filled_access_location, getThemedColor(Theme.key_color_green), botPermissionBiometry != -1);
                    } else if (position == botPermissionBiometry) {
                        textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionBiometry), botBiometry != null && botBiometry.granted(), R.drawable.filled_access_fingerprint, getThemedColor(Theme.key_color_orange), false);
                    } else if (position == botPermissionEmojiStatus) {
                        textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionEmojiStatus), viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().bot_can_manage_emoji_status, R.drawable.filled_access_sleeping, getThemedColor(Theme.key_color_lightblue), botPermissionLocation != -1 || botPermissionBiometry != -1);
                    }
                    textCell.valueTextView.setTextColor(dontApplyPeerColor(getThemedColor(Theme.key_windowBackgroundWhiteValueText), false));
                    break;
                case VIEW_TYPE_NOTIFICATIONS_CHECK:
                    NotificationsCheckCell checkCell = (NotificationsCheckCell) holder.itemView;
                    if (position == notificationsRow) {
                        SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                        long did;
                        if (viewModel.getArgs().dialogId != 0) {
                            did = viewModel.getArgs().dialogId;
                        } else if (viewModel.getArgs().userId != 0) {
                            did = viewModel.getArgs().userId;
                        } else {
                            did = -viewModel.getArgs().chatId;
                        }
                        String key = NotificationsController.getSharedPrefKey(did, viewModel.getArgs().topicId);
                        boolean enabled = false;
                        boolean custom = preferences.getBoolean("custom_" + key, false);
                        boolean hasOverride = preferences.contains("notify2_" + key);
                        int value = preferences.getInt("notify2_" + key, 0);
                        int delta = preferences.getInt("notifyuntil_" + key, 0);
                        String val;
                        if (value == 3 && delta != Integer.MAX_VALUE) {
                            delta -= getConnectionsManager().getCurrentTime();
                            if (delta <= 0) {
                                if (custom) {
                                    val = LocaleController.getString(R.string.NotificationsCustom);
                                } else {
                                    val = LocaleController.getString(R.string.NotificationsOn);
                                }
                                enabled = true;
                            } else if (delta < 60 * 60) {
                                val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Minutes", delta / 60));
                            } else if (delta < 60 * 60 * 24) {
                                val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Hours", (int) Math.ceil(delta / 60.0f / 60)));
                            } else if (delta < 60 * 60 * 24 * 365) {
                                val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Days", (int) Math.ceil(delta / 60.0f / 60 / 24)));
                            } else {
                                val = null;
                            }
                        } else {
                            if (value == 0) {
                                if (hasOverride) {
                                    enabled = true;
                                } else {
                                    enabled = getNotificationsController().isGlobalNotificationsEnabled(did, false, false);
                                }
                            } else if (value == 1) {
                                enabled = true;
                            }
                            if (enabled && custom) {
                                val = LocaleController.getString(R.string.NotificationsCustom);
                            } else {
                                val = enabled ? LocaleController.getString(R.string.NotificationsOn) : LocaleController.getString(R.string.NotificationsOff);
                            }
                        }
                        if (val == null) {
                            val = LocaleController.getString(R.string.NotificationsOff);
                        }
                        if (notificationsExceptionTopics != null && !notificationsExceptionTopics.isEmpty()) {
                            val = String.format(Locale.US, LocaleController.getPluralString("NotificationTopicExceptionsDesctription", notificationsExceptionTopics.size()), val, notificationsExceptionTopics.size());
                        }
                        checkCell.setAnimationsEnabled(fragmentOpened);
                        checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.Notifications), val, enabled, botAppRow >= 0);
                    }
                    break;
                case VIEW_TYPE_SHADOW:
                    View sectionCell = holder.itemView;
                    sectionCell.setTag(position);
                    Drawable drawable;
                    if (position == infoSectionRow && lastSectionRow == -1 && secretSettingsSectionRow == -1 && sharedMediaRow == -1 && membersSectionRow == -1 || position == secretSettingsSectionRow || position == lastSectionRow || position == membersSectionRow && lastSectionRow == -1 && sharedMediaRow == -1) {
                        sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    } else {
                        sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    }
                    break;
                case VIEW_TYPE_SHADOW_TEXT: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setLinkTextRippleColor(null);
                    if (position == infoSectionRow) {
                        final long did = viewModel.getDialogId();
                        TLObject obj = getMessagesController().getUserOrChat(did);
                        TL_bots.botVerification bot_verification = viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().bot_verification : viewModel.getState().getValue().getChatInfo() != null ? viewModel.getState().getValue().getChatInfo().bot_verification : null;
                        if (botAppRow >= 0 || bot_verification != null) {
                            cell.setFixedSize(0);
                            final TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
                            final boolean botOwner = user != null && user.bot && user.bot_can_edit;
                            SpannableStringBuilder sb = new SpannableStringBuilder();

                            if (botAppRow >= 0) {
                                sb.append(AndroidUtilities.replaceSingleTag(getString(botOwner ? R.string.ProfileBotOpenAppInfoOwner : R.string.ProfileBotOpenAppInfo), () -> {
                                    Browser.openUrl(getContext(), getString(botOwner ? R.string.ProfileBotOpenAppInfoOwnerLink : R.string.ProfileBotOpenAppInfoLink));
                                }));
                                if (bot_verification != null) {
                                    sb.append("\n\n\n");
                                }
                            }
                            if (bot_verification != null) {
                                sb.append("x");
                                sb.setSpan(new AnimatedEmojiSpan(bot_verification.icon, cell.getTextView().getPaint().getFontMetricsInt()), sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                sb.append(" ");
                                SpannableString description = new SpannableString(bot_verification.description);
                                try {
                                    AndroidUtilities.addLinksSafe(description, Linkify.WEB_URLS, false, false);
                                    URLSpan[] spans = description.getSpans(0, description.length(), URLSpan.class);
                                    for (int i = 0; i < spans.length; ++i) {
                                        URLSpan span = spans[i];
                                        int start = description.getSpanStart(span);
                                        int end = description.getSpanEnd(span);
                                        final String url = span.getURL();

                                        description.removeSpan(span);
                                        description.setSpan(new URLSpan(url) {
                                            @Override
                                            public void onClick(View widget) {
                                                Browser.openUrl(getContext(), url);
                                            }
                                            @Override
                                            public void updateDrawState(@NonNull TextPaint ds) {
                                                ds.setUnderlineText(true);
                                            }
                                        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                                sb.append(description);
                            }

                            cell.setLinkTextRippleColor(Theme.multAlpha(getThemedColor(Theme.key_windowBackgroundWhiteGrayText4), 0.2f));
                            cell.setText(sb);
                        } else {
                            cell.setFixedSize(14);
                            cell.setText(null);
                        }
                    } else if (position == infoAffiliateRow) {
                        final TLRPC.User botUser = getMessagesController().getUser(viewModel.getArgs().userId);
                        if (botUser != null && botUser.bot && botUser.bot_can_edit) {
                            cell.setFixedSize(0);
                            cell.setText(formatString(R.string.ProfileBotAffiliateProgramInfoOwner, UserObject.getUserName(botUser), percents(viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().starref_program != null ? viewModel.getState().getValue().getUserInfo().starref_program.commission_permille : 0)));
                        } else {
                            cell.setFixedSize(0);
                            cell.setText(formatString(R.string.ProfileBotAffiliateProgramInfo, UserObject.getUserName(botUser), percents(viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().starref_program != null ? viewModel.getState().getValue().getUserInfo().starref_program.commission_permille : 0)));
                        }
                    }
                    if (position == infoSectionRow && lastSectionRow == -1 && secretSettingsSectionRow == -1 && sharedMediaRow == -1 && membersSectionRow == -1 || position == secretSettingsSectionRow || position == lastSectionRow || position == membersSectionRow && lastSectionRow == -1 && sharedMediaRow == -1) {
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    } else {
                        cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    }
                    break;
                }
                case VIEW_TYPE_COLORFUL_TEXT: {
                    AffiliateProgramFragment.ColorfulTextCell cell = (AffiliateProgramFragment.ColorfulTextCell) holder.itemView;
                    cell.set(getThemedColor(Theme.key_color_green), R.drawable.filled_affiliate, getString(R.string.ProfileBotAffiliateProgram), null);
                    cell.setPercent(viewModel.getState().getValue().getUserInfo() != null && viewModel.getState().getValue().getUserInfo().starref_program != null ? percents(viewModel.getState().getValue().getUserInfo().starref_program.commission_permille) : null);
                    break;
                }
                case VIEW_TYPE_USER:
                    UserCell userCell = (UserCell) holder.itemView;
                    TLRPC.ChatParticipant part;
                    try {
                        if (!visibleSortedUsers.isEmpty()) {
                            part = visibleChatParticipants.get(visibleSortedUsers.get(position - membersStartRow));
                        } else {
                            part = visibleChatParticipants.get(position - membersStartRow);
                        }
                    } catch (Exception e) {
                        part = null;
                        FileLog.e(e);
                    }
                    if (part != null) {
                        String role;
                        if (part instanceof TLRPC.TL_chatChannelParticipant) {
                            TLRPC.ChannelParticipant channelParticipant = ((TLRPC.TL_chatChannelParticipant) part).channelParticipant;
                            if (!TextUtils.isEmpty(channelParticipant.rank)) {
                                role = channelParticipant.rank;
                            } else {
                                if (channelParticipant instanceof TLRPC.TL_channelParticipantCreator) {
                                    role = LocaleController.getString(R.string.ChannelCreator);
                                } else if (channelParticipant instanceof TLRPC.TL_channelParticipantAdmin) {
                                    role = LocaleController.getString(R.string.ChannelAdmin);
                                } else {
                                    role = null;
                                }
                            }
                        } else {
                            if (part instanceof TLRPC.TL_chatParticipantCreator) {
                                role = LocaleController.getString(R.string.ChannelCreator);
                            } else if (part instanceof TLRPC.TL_chatParticipantAdmin) {
                                role = getString(R.string.ChannelAdmin);
                            } else {
                                role = null;
                            }
                        }
                        userCell.setAdminRole(role);
                        userCell.setData(getMessagesController().getUser(part.user_id), null, null, 0, position != membersEndRow - 1);
                    }
                    break;
                case VIEW_TYPE_BOTTOM_PADDING:
                    holder.itemView.requestLayout();
                    break;
                case VIEW_TYPE_SUGGESTION:
                    SettingsSuggestionCell suggestionCell = (SettingsSuggestionCell) holder.itemView;
                    if (position == passwordSuggestionRow) {
                        suggestionCell.setType(SettingsSuggestionCell.TYPE_PASSWORD);
                    } else if (position == phoneSuggestionRow) {
                        suggestionCell.setType(SettingsSuggestionCell.TYPE_PHONE);
                    } else if (position == graceSuggestionRow) {
                        suggestionCell.setType(SettingsSuggestionCell.TYPE_GRACE);
                    }
                    break;
                case VIEW_TYPE_ADDTOGROUP_INFO:
                    TextInfoPrivacyCell addToGroupInfo = (TextInfoPrivacyCell) holder.itemView;
                    addToGroupInfo.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    addToGroupInfo.setText(LocaleController.getString(R.string.BotAddToGroupOrChannelInfo));
                    break;
                case VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setTextAndCheck(LocaleController.getString(R.string.Notifications), !getMessagesController().isDialogMuted(viewModel.getDialogId(), viewModel.getArgs().topicId), false);
                    break;
                case VIEW_TYPE_LOCATION:
                    ((ProfileLocationCell) holder.itemView).set(viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().business_location : null, notificationsDividerRow < 0 && !viewModel.getArgs().myProfile);
                    break;
                case VIEW_TYPE_HOURS:
                    ProfileHoursCell hoursCell = (ProfileHoursCell) holder.itemView;
                    hoursCell.setOnTimezoneSwitchClick(view -> {
                        hoursShownMine = !hoursShownMine;
                        if (!hoursExpanded) {
                            hoursExpanded = true;
                        }
                        saveScrollPosition();
                        view.requestLayout();
                        listAdapter.notifyItemChanged(bizHoursRow);
                        if (savedScrollPosition >= 0) {
                            layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset - listView.getPaddingTop());
                        }
                    });
                    hoursCell.set(viewModel.getState().getValue().getUserInfo() != null ? viewModel.getState().getValue().getUserInfo().business_work_hours : null, hoursExpanded, hoursShownMine, notificationsDividerRow < 0 && !viewModel.getArgs().myProfile || bizLocationRow >= 0);
                    break;
                case VIEW_TYPE_CHANNEL:
                    ((ProfileChannelCell) holder.itemView).set(
                        getMessagesController().getChat(viewModel.getState().getValue().getUserInfo().personal_channel_id),
                        profileChannelMessageFetcher != null ? profileChannelMessageFetcher.messageObject : null
                    );
                    break;
                case VIEW_TYPE_BOT_APP:

                    break;
            }
        }

        private CharSequence alsoUsernamesString(String originalUsername, ArrayList<TLRPC.TL_username> alsoUsernames, CharSequence fallback) {
            if (alsoUsernames == null) {
                return fallback;
            }
            alsoUsernames = new ArrayList<>(alsoUsernames);
            for (int i = 0; i < alsoUsernames.size(); ++i) {
                if (
                    !alsoUsernames.get(i).active ||
                    originalUsername != null && originalUsername.equals(alsoUsernames.get(i).username)
                ) {
                    alsoUsernames.remove(i--);
                }
            }
            if (alsoUsernames.size() > 0) {
                SpannableStringBuilder usernames = new SpannableStringBuilder();
                for (int i = 0; i < alsoUsernames.size(); ++i) {
                    TLRPC.TL_username usernameObj = alsoUsernames.get(i);
                    final String usernameRaw = usernameObj.username;
                    SpannableString username = new SpannableString("@" + usernameRaw);
                    username.setSpan(makeUsernameLinkSpan(usernameObj), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    username.setSpan(new ForegroundColorSpan(dontApplyPeerColor(getThemedColor(Theme.key_chat_messageLinkIn), false)), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    usernames.append(username);
                    if (i < alsoUsernames.size() - 1) {
                        usernames.append(", ");
                    }
                }
                String string = getString(R.string.UsernameAlso);
                SpannableStringBuilder finalString = new SpannableStringBuilder(string);
                final String toFind = "%1$s";
                int index = string.indexOf(toFind);
                if (index >= 0) {
                    finalString.replace(index, index + toFind.length(), usernames);
                }
                return finalString;
            } else {
                return fallback;
            }
        }

        private final HashMap<TLRPC.TL_username, ClickableSpan> usernameSpans = new HashMap<TLRPC.TL_username, ClickableSpan>();
        public ClickableSpan makeUsernameLinkSpan(TLRPC.TL_username usernameObj) {
            ClickableSpan span = usernameSpans.get(usernameObj);
            if (span != null) return span;

            final String usernameRaw = usernameObj.username;
            span = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    if (!usernameObj.editable) {
                        if (loadingSpan == this) return;
                        setLoadingSpan(this);
                        TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                        TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                        input.username = usernameObj.username;
                        req.collectible = input;
                        int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                            setLoadingSpan(null);
                            if (res instanceof TL_fragment.TL_collectibleInfo) {
                                TLObject obj;
                                if (viewModel.getArgs().userId != 0) {
                                    obj = getMessagesController().getUser(viewModel.getArgs().userId);
                                } else {
                                    obj = getMessagesController().getChat(viewModel.getArgs().chatId);
                                }
                                if (getContext() == null) {
                                    return;
                                }
                                FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_USERNAME, usernameObj.username, obj, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                            } else {
                                BulletinFactory.showError(err);
                            }
                        }));
                        getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                    } else {
                        setLoadingSpan(null);
                        String urlFinal = getMessagesController().linkPrefix + "/" + usernameRaw;
                        if (viewModel.getState().getValue().getCurrentChat() == null || !viewModel.getState().getValue().getCurrentChat().noforwards) {
                            AndroidUtilities.addToClipboard(urlFinal);
                            undoView.showWithAction(0, UndoView.ACTION_USERNAME_COPIED, null);
                        }
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(ds.linkColor);
                }
            };
            usernameSpans.put(usernameObj, span);
            return span;
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getAdapterPosition() == setAvatarRow) {
                setAvatarCell = null;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            if (notificationRow != -1) {
                int position = holder.getAdapterPosition();
                return position == notificationRow || position == numberRow || position == privacyRow ||
                        position == languageRow || position == setUsernameRow || position == bioRow ||
                        position == versionRow || position == dataRow || position == chatRow ||
                        position == questionRow || position == devicesRow || position == filtersRow || position == stickersRow ||
                        position == faqRow || position == policyRow || position == sendLogsRow || position == sendLastLogsRow ||
                        position == clearLogsRow || position == switchBackendRow || position == setAvatarRow ||
                        position == addToGroupButtonRow || position == premiumRow || position == premiumGiftingRow ||
                        position == businessRow || position == liteModeRow || position == birthdayRow || position == channelRow ||
                        position == starsRow || position == tonRow;
            }
            if (holder.itemView instanceof UserCell) {
                UserCell userCell = (UserCell) holder.itemView;
                Object object = userCell.getCurrentObject();
                if (object instanceof TLRPC.User) {
                    TLRPC.User user = (TLRPC.User) object;
                    if (UserObject.isUserSelf(user)) {
                        return false;
                    }
                }
            }
            int type = holder.getItemViewType();
            return type != VIEW_TYPE_HEADER && type != VIEW_TYPE_DIVIDER && type != VIEW_TYPE_SHADOW &&
                    type != VIEW_TYPE_EMPTY && type != VIEW_TYPE_BOTTOM_PADDING && type != VIEW_TYPE_SHARED_MEDIA &&
                    type != 9 && type != 10 && type != VIEW_TYPE_BOT_APP; // These are legacy ones, left for compatibility
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == infoHeaderRow || position == membersHeaderRow || position == settingsSectionRow2 ||
                    position == numberSectionRow || position == helpHeaderRow || position == debugHeaderRow || position == botPermissionsHeader) {
                return VIEW_TYPE_HEADER;
            } else if (position == phoneRow || position == locationRow || position == numberRow || position == birthdayRow) {
                return VIEW_TYPE_TEXT_DETAIL;
            } else if (position == usernameRow || position == setUsernameRow) {
                return VIEW_TYPE_TEXT_DETAIL_MULTILINE;
            } else if (position == userInfoRow || position == channelInfoRow || position == bioRow) {
                return VIEW_TYPE_ABOUT_LINK;
            } else if (position == settingsTimerRow || position == settingsKeyRow || position == reportRow || position == reportReactionRow ||
                    position == subscribersRow || position == subscribersRequestsRow || position == administratorsRow || position == settingsRow || position == blockedUsersRow ||
                    position == addMemberRow || position == joinRow || position == unblockRow ||
                    position == sendMessageRow || position == notificationRow || position == privacyRow ||
                    position == languageRow || position == dataRow || position == chatRow ||
                    position == questionRow || position == devicesRow || position == filtersRow || position == stickersRow ||
                    position == faqRow || position == policyRow || position == sendLogsRow || position == sendLastLogsRow ||
                    position == clearLogsRow || position == switchBackendRow || position == setAvatarRow || position == addToGroupButtonRow ||
                    position == addToContactsRow || position == liteModeRow || position == premiumGiftingRow || position == businessRow ||
                    position == botStarsBalanceRow || position == botTonBalanceRow || position == channelBalanceRow || position == botPermissionLocation ||
                    position == botPermissionBiometry || position == botPermissionEmojiStatus || position == tonRow
            ) {
                return VIEW_TYPE_TEXT;
            } else if (position == notificationsDividerRow) {
                return VIEW_TYPE_DIVIDER;
            } else if (position == notificationsRow) {
                return VIEW_TYPE_NOTIFICATIONS_CHECK;
            } else if (position == notificationsSimpleRow) {
                return VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE;
            } else if (position == lastSectionRow || position == membersSectionRow ||
                    position == secretSettingsSectionRow || position == settingsSectionRow || position == devicesSectionRow ||
                    position == helpSectionCell || position == setAvatarSectionRow || position == passwordSuggestionSectionRow ||
                    position == phoneSuggestionSectionRow || position == premiumSectionsRow || position == reportDividerRow ||
                    position == channelDividerRow || position == graceSuggestionSectionRow || position == balanceDividerRow ||
                    position == botPermissionsDivider || position == channelBalanceSectionRow
            ) {
                return VIEW_TYPE_SHADOW;
            } else if (position >= membersStartRow && position < membersEndRow) {
                return VIEW_TYPE_USER;
            } else if (position == emptyRow) {
                return VIEW_TYPE_EMPTY;
            } else if (position == bottomPaddingRow) {
                return VIEW_TYPE_BOTTOM_PADDING;
            } else if (position == sharedMediaRow) {
                return VIEW_TYPE_SHARED_MEDIA;
            } else if (position == versionRow) {
                return VIEW_TYPE_VERSION;
            } else if (position == passwordSuggestionRow || position == phoneSuggestionRow || position == graceSuggestionRow) {
                return VIEW_TYPE_SUGGESTION;
            } else if (position == addToGroupInfoRow) {
                return VIEW_TYPE_ADDTOGROUP_INFO;
            } else if (position == premiumRow) {
                return VIEW_TYPE_PREMIUM_TEXT_CELL;
            } else if (position == starsRow) {
                return VIEW_TYPE_STARS_TEXT_CELL;
            } else if (position == bizLocationRow) {
                return VIEW_TYPE_LOCATION;
            } else if (position == bizHoursRow) {
                return VIEW_TYPE_HOURS;
            } else if (position == channelRow) {
                return VIEW_TYPE_CHANNEL;
            } else if (position == botAppRow) {
                return VIEW_TYPE_BOT_APP;
            } else if (position == infoSectionRow || position == infoAffiliateRow) {
                return VIEW_TYPE_SHADOW_TEXT;
            } else if (position == affiliateRow) {
                return VIEW_TYPE_COLORFUL_TEXT;
            }
            return 0;
        }
    }

    private class SearchAdapter extends RecyclerListView.SelectionAdapter {

        private class SearchResult {

            private String searchTitle;
            private Runnable openRunnable;
            private String rowName;
            private String[] path;
            private int iconResId;
            private int guid;
            private int num;

            public SearchResult(int g, String search, int icon, Runnable open) {
                this(g, search, null, null, null, icon, open);
            }

            public SearchResult(int g, String search, String pathArg1, int icon, Runnable open) {
                this(g, search, null, pathArg1, null, icon, open);
            }

            public SearchResult(int g, String search, String row, String pathArg1, int icon, Runnable open) {
                this(g, search, row, pathArg1, null, icon, open);
            }

            public SearchResult(int g, String search, String row, String pathArg1, String pathArg2, int icon, Runnable open) {
                guid = g;
                searchTitle = search;
                rowName = row;
                openRunnable = open;
                iconResId = icon;
                if (pathArg1 != null && pathArg2 != null) {
                    path = new String[]{pathArg1, pathArg2};
                } else if (pathArg1 != null) {
                    path = new String[]{pathArg1};
                }
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof SearchResult)) {
                    return false;
                }
                SearchResult result = (SearchResult) obj;
                return guid == result.guid;
            }

            @Override
            public String toString() {
                SerializedData data = new SerializedData();
                data.writeInt32(num);
                data.writeInt32(1);
                data.writeInt32(guid);
                return Utilities.bytesToHex(data.toByteArray());
            }

            private void open() {
                openRunnable.run();
                AndroidUtilities.scrollToFragmentRow(parentLayout, rowName);
            }
        }

        private SearchResult[] searchArray = onCreateSearchArray();
        private ArrayList<MessagesController.FaqSearchResult> faqSearchArray = new ArrayList<>();

        private Context mContext;
        private ArrayList<CharSequence> resultNames = new ArrayList<>();
        private ArrayList<SearchResult> searchResults = new ArrayList<>();
        private ArrayList<MessagesController.FaqSearchResult> faqSearchResults = new ArrayList<>();
        private ArrayList<Object> recentSearches = new ArrayList<>();
        private boolean searchWas;
        private Runnable searchRunnable;
        private String lastSearchString;
        private TLRPC.WebPage faqWebPage;
        private boolean loadingFaqPage;

        public SearchAdapter(Context context) {
            mContext = context;

            updateSearchArray();
        }

        private void updateSearchArray() {
            HashMap<Integer, SearchResult> resultHashMap = new HashMap<>();
            for (int a = 0; a < searchArray.length; a++) {
                if (searchArray[a] == null) {
                    continue;
                }
                resultHashMap.put(searchArray[a].guid, searchArray[a]);
            }
            Set<String> set = MessagesController.getGlobalMainSettings().getStringSet("settingsSearchRecent2", null);
            if (set != null) {
                for (String value : set) {
                    try {
                        SerializedData data = new SerializedData(Utilities.hexToBytes(value));
                        int num = data.readInt32(false);
                        int type = data.readInt32(false);
                        if (type == 0) {
                            String title = data.readString(false);
                            int count = data.readInt32(false);
                            String[] path = null;
                            if (count > 0) {
                                path = new String[count];
                                for (int a = 0; a < count; a++) {
                                    path[a] = data.readString(false);
                                }
                            }
                            String url = data.readString(false);
                            MessagesController.FaqSearchResult result = new MessagesController.FaqSearchResult(title, path, url);
                            result.num = num;
                            recentSearches.add(result);
                        } else if (type == 1) {
                            SearchResult result = resultHashMap.get(data.readInt32(false));
                            if (result != null) {
                                result.num = num;
                                recentSearches.add(result);
                            }
                        }
                    } catch (Exception ignore) {

                    }
                }
            }
            Collections.sort(recentSearches, (o1, o2) -> {
                int n1 = getNum(o1);
                int n2 = getNum(o2);
                if (n1 < n2) {
                    return -1;
                } else if (n1 > n2) {
                    return 1;
                }
                return 0;
            });
        }

        private SearchResult[] onCreateSearchArray() {
            return new SearchResult[]{
                new SearchResult(500, getString(R.string.EditName), 0, () -> presentFragment(new ChangeNameActivity(resourcesProvider))),
                new SearchResult(501, getString(R.string.ChangePhoneNumber), 0, () -> presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER))),
                new SearchResult(502, getString(R.string.AddAnotherAccount), 0, () -> {
                    int freeAccount = -1;
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        if (!UserConfig.getInstance(a).isClientActivated()) {
                            freeAccount = a;
                            break;
                        }
                    }
                    if (freeAccount >= 0) {
                        presentFragment(new LoginActivity(freeAccount));
                    }
                }),
                new SearchResult(503, getString(R.string.UserBio), 0, () -> {
                    if (viewModel.getState().getValue().getUserInfo() != null) {
                        presentFragment(new ChangeBioActivity());
                    }
                }),
                new SearchResult(504, getString(R.string.AddPhoto), 0, ProfileActivity.this::onWriteButtonClick),

                new SearchResult(1, getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(2, getString(R.string.NotificationsPrivateChats), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_PRIVATE, new ArrayList<>(), null, true))),
                new SearchResult(3, getString(R.string.NotificationsGroups), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_GROUP, new ArrayList<>(), null, true))),
                new SearchResult(4, getString(R.string.NotificationsChannels), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_CHANNEL, new ArrayList<>(), null, true))),
                new SearchResult(5, getString(R.string.VoipNotificationSettings), "callsSectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(6, getString(R.string.BadgeNumber), "badgeNumberSection", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(7, getString(R.string.InAppNotifications), "inappSectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(8, getString(R.string.ContactJoined), "contactJoinedRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(9, getString(R.string.PinnedMessages), "pinnedMessageRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(10, getString(R.string.ResetAllNotifications), "resetNotificationsRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(11, getString(R.string.NotificationsService), "notificationsServiceRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(12, getString(R.string.NotificationsServiceConnection), "notificationsServiceConnectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),
                new SearchResult(13, getString(R.string.RepeatNotifications), "repeatRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> presentFragment(new NotificationsSettingsActivity())),

                new SearchResult(100, getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(109, getString(R.string.TwoStepVerification), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new TwoStepVerificationActivity())),
                new SearchResult(124, getString(R.string.AutoDeleteMessages), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> {
                    if (getUserConfig().getGlobalTTl() >= 0) {
                        presentFragment(new AutoDeleteMessagesActivity());
                    }
                }),
                new SearchResult(108, getString(R.string.Passcode), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(PasscodeActivity.determineOpenFragment())),
                SharedConfig.hasEmailLogin ? new SearchResult(125, getString(R.string.EmailLogin), "emailLoginRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())) : null,
                new SearchResult(101, getString(R.string.BlockedUsers), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyUsersActivity().loadBlocked())),
                new SearchResult(110, getString(R.string.SessionsTitle), R.drawable.msg2_secret, () -> presentFragment(new SessionsActivity(0))),
                new SearchResult(105, getString(R.string.PrivacyPhone), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_PHONE, true))),
                new SearchResult(102, getString(R.string.PrivacyLastSeen), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_LASTSEEN, true))),
                new SearchResult(103, getString(R.string.PrivacyProfilePhoto), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_PHOTO, true))),
                new SearchResult(104, getString(R.string.PrivacyForwards), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_FORWARDS, true))),
                new SearchResult(122, getString(R.string.PrivacyP2P), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_P2P, true))),
                new SearchResult(106, getString(R.string.Calls), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_CALLS, true))),
                new SearchResult(107, getString(R.string.PrivacyInvites), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_INVITE, true))),
                new SearchResult(123, getString(R.string.PrivacyVoiceMessages), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> {
                    if (!getUserConfig().isPremium()) {
                        try {
                            fragmentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        } catch (Exception ignored) {}
                        BulletinFactory.of(ProfileActivity.this).createRestrictVoiceMessagesPremiumBulletin().show();
                        return;
                    }
                    presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_VOICE_MESSAGES, true));
                }),
                getMessagesController().autoarchiveAvailable ? new SearchResult(121, getString(R.string.ArchiveAndMute), "newChatsRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())) : null,
                new SearchResult(112, getString(R.string.DeleteAccountIfAwayFor2), "deleteAccountRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(113, getString(R.string.PrivacyPaymentsClear), "paymentsClearRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(114, getString(R.string.WebSessionsTitle), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new SessionsActivity(1))),
                new SearchResult(115, getString(R.string.SyncContactsDelete), "contactsDeleteRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(116, getString(R.string.SyncContacts), "contactsSyncRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(117, getString(R.string.SuggestContacts), "contactsSuggestRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(118, getString(R.string.MapPreviewProvider), "secretMapRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),
                new SearchResult(119, getString(R.string.SecretWebPage), "secretWebpageRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> presentFragment(new PrivacySettingsActivity())),

                new SearchResult(120, getString(R.string.Devices), R.drawable.msg2_devices, () -> presentFragment(new SessionsActivity(0))),
                new SearchResult(121, getString(R.string.TerminateAllSessions), "terminateAllSessionsRow", getString(R.string.Devices), R.drawable.msg2_devices, () -> presentFragment(new SessionsActivity(0))),
                new SearchResult(122, getString(R.string.LinkDesktopDevice), getString(R.string.Devices), R.drawable.msg2_devices, () -> presentFragment(new SessionsActivity(0).setHighlightLinkDesktopDevice())),

                new SearchResult(200, getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(201, getString(R.string.DataUsage), "usageSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(202, getString(R.string.StorageUsage), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new CacheControlActivity())),
                new SearchResult(203, getString(R.string.KeepMedia), "keepMediaRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> presentFragment(new CacheControlActivity())),
                new SearchResult(204, getString(R.string.ClearMediaCache), "cacheRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> presentFragment(new CacheControlActivity())),
                new SearchResult(205, getString(R.string.LocalDatabase), "databaseRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> presentFragment(new CacheControlActivity())),
                new SearchResult(206, getString(R.string.NetworkUsage), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataUsage2Activity())),
                new SearchResult(207, getString(R.string.AutomaticMediaDownload), "mediaDownloadSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(208, getString(R.string.WhenUsingMobileData), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataAutoDownloadActivity(0))),
                new SearchResult(209, getString(R.string.WhenConnectedOnWiFi), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataAutoDownloadActivity(1))),
                new SearchResult(210, getString(R.string.WhenRoaming), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataAutoDownloadActivity(2))),
                new SearchResult(211, getString(R.string.ResetAutomaticMediaDownload), "resetDownloadRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(215, getString(R.string.Streaming), "streamSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(216, getString(R.string.EnableStreaming), "enableStreamRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(217, getString(R.string.Calls), "callsSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(218, getString(R.string.VoipUseLessData), "useLessDataForCallsRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(219, getString(R.string.VoipQuickReplies), "quickRepliesRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(220, getString(R.string.ProxySettings), getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new ProxyListActivity())),
                new SearchResult(221, getString(R.string.UseProxyForCalls), "callsRow", getString(R.string.DataSettings), getString(R.string.ProxySettings), R.drawable.msg2_data, () -> presentFragment(new ProxyListActivity())),
                new SearchResult(111, getString(R.string.PrivacyDeleteCloudDrafts), "clearDraftsRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(222, getString(R.string.SaveToGallery), "saveToGallerySectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(223, getString(R.string.SaveToGalleryPrivate), "saveToGalleryPeerRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(224, getString(R.string.SaveToGalleryGroups), "saveToGalleryGroupsRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),
                new SearchResult(225, getString(R.string.SaveToGalleryChannels), "saveToGalleryChannelsRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> presentFragment(new DataSettingsActivity())),

                new SearchResult(300, getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(301, getString(R.string.TextSizeHeader), "textSizeHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(302, getString(R.string.ChangeChatBackground), getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL))),
                new SearchResult(303, getString(R.string.SetColor), null, getString(R.string.ChatSettings), getString(R.string.ChatBackground), R.drawable.msg2_discussion, () -> presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_COLOR))),
                new SearchResult(304, getString(R.string.ResetChatBackgrounds), "resetRow", getString(R.string.ChatSettings), getString(R.string.ChatBackground), R.drawable.msg2_discussion, () -> presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL))),
                new SearchResult(306, getString(R.string.ColorTheme), "themeHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(319, getString(R.string.BrowseThemes), null, getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_THEMES_BROWSER))),
                new SearchResult(320, getString(R.string.CreateNewTheme), "createNewThemeRow", getString(R.string.ChatSettings), getString(R.string.BrowseThemes), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_THEMES_BROWSER))),
                new SearchResult(321, getString(R.string.BubbleRadius), "bubbleRadiusHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(322, getString(R.string.ChatList), "chatListHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(323, getString(R.string.ChatListSwipeGesture), "swipeGestureHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(324, getString(R.string.AppIcon), "appIconHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(305, getString(R.string.AutoNightTheme), getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_NIGHT))),
                new SearchResult(328, getString(R.string.NextMediaTap), "nextMediaTapRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(327, getString(R.string.RaiseToListen), "raiseToListenRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(310, getString(R.string.RaiseToSpeak), "raiseToSpeakRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(326, getString(R.string.PauseMusicOnMedia), "pauseOnMediaRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(325, getString(R.string.MicrophoneForVoiceMessages), "bluetoothScoRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(308, getString(R.string.DirectShare), "directShareRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(311, getString(R.string.SendByEnter), "sendByEnterRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchResult(318, getString(R.string.DistanceUnits), "distanceRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),

                new SearchResult(600, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchResult(601, getString(R.string.SuggestStickers), "suggestRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchResult(602, getString(R.string.FeaturedStickers), "featuredStickersHeaderRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchResult(603, getString(R.string.Masks), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_MASK, null))),
                new SearchResult(604, getString(R.string.ArchivedStickers), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new ArchivedStickersActivity(MediaDataController.TYPE_IMAGE))),
                new SearchResult(605, getString(R.string.ArchivedMasks), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new ArchivedStickersActivity(MediaDataController.TYPE_MASK))),
                new SearchResult(606, getString(R.string.LargeEmoji), "largeEmojiRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchResult(607, getString(R.string.LoopAnimatedStickers), "loopRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchResult(608, getString(R.string.Emoji), null, getString(R.string.StickersName), R.drawable.input_smile, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchResult(609, getString(R.string.SuggestAnimatedEmoji), "suggestAnimatedEmojiRow", getString(R.string.StickersName), getString(R.string.Emoji), R.drawable.input_smile, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchResult(610, getString(R.string.FeaturedEmojiPacks), "featuredStickersHeaderRow", getString(R.string.StickersName), getString(R.string.Emoji), R.drawable.input_smile, () -> presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchResult(611, getString(R.string.DoubleTapSetting), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> presentFragment(new ReactionsDoubleTapManageActivity())),

                new SearchResult(700, getString(R.string.Filters), null, R.drawable.msg2_folder, () -> presentFragment(new FiltersSetupActivity())),
                new SearchResult(701, getString(R.string.CreateNewFilter), "createFilterRow", getString(R.string.Filters), R.drawable.msg2_folder, () -> presentFragment(new FiltersSetupActivity())),

                isPremiumFeatureAvailable(-1) ? new SearchResult(800, getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> presentFragment(new PremiumPreviewFragment("settings"))) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_LIMITS) ? new SearchResult(801, getString(R.string.PremiumPreviewLimits), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_LIMITS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI) ? new SearchResult(802, getString(R.string.PremiumPreviewEmoji), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_UPLOAD_LIMIT) ? new SearchResult(803, getString(R.string.PremiumPreviewUploads), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_UPLOAD_LIMIT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_DOWNLOAD_SPEED) ? new SearchResult(804, getString(R.string.PremiumPreviewDownloadSpeed), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_DOWNLOAD_SPEED, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_VOICE_TO_TEXT) ? new SearchResult(805, getString(R.string.PremiumPreviewVoiceToText), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_VOICE_TO_TEXT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ADS) ? new SearchResult(806, getString(R.string.PremiumPreviewNoAds), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_ADS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_REACTIONS) ? new SearchResult(807, getString(R.string.PremiumPreviewReactions), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_REACTIONS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_STICKERS) ? new SearchResult(808, getString(R.string.PremiumPreviewStickers), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_STICKERS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ADVANCED_CHAT_MANAGEMENT) ? new SearchResult(809, getString(R.string.PremiumPreviewAdvancedChatManagement), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_ADVANCED_CHAT_MANAGEMENT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_PROFILE_BADGE) ? new SearchResult(810, getString(R.string.PremiumPreviewProfileBadge), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_PROFILE_BADGE, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_AVATARS) ? new SearchResult(811, getString(R.string.PremiumPreviewAnimatedProfiles), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_AVATARS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS) ? new SearchResult(812, getString(R.string.PremiumPreviewAppIcon), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_EMOJI_STATUS) ? new SearchResult(813, getString(R.string.PremiumPreviewEmojiStatus), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> showDialog(new PremiumFeatureBottomSheet(ProfileActivity.this, PremiumPreviewFragment.PREMIUM_FEATURE_EMOJI_STATUS, false).setForceAbout())) : null,

                new SearchResult(900, getString(R.string.PowerUsage), null, R.drawable.msg2_battery, () -> presentFragment(new LiteModeSettingsActivity())),
                new SearchResult(901, getString(R.string.LiteOptionsStickers), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_ANIMATED_STICKERS);
                }),
                new SearchResult(902, getString(R.string.LiteOptionsAutoplayKeyboard), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsStickers), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_STICKERS, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_STICKERS_KEYBOARD);
                }),
                new SearchResult(903, getString(R.string.LiteOptionsAutoplayChat), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsStickers), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_STICKERS, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_STICKERS_CHAT);
                }),
                new SearchResult(904, getString(R.string.LiteOptionsEmoji), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_ANIMATED_EMOJI);
                }),
                new SearchResult(905, getString(R.string.LiteOptionsAutoplayKeyboard), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_KEYBOARD);
                }),
                new SearchResult(906, getString(R.string.LiteOptionsAutoplayReactions), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_REACTIONS);
                }),
                new SearchResult(907, getString(R.string.LiteOptionsAutoplayChat), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_CHAT);
                }),
                new SearchResult(908, getString(R.string.LiteOptionsChat), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_CHAT);
                }),
                new SearchResult(909, getString(R.string.LiteOptionsBackground), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_BACKGROUND);
                }),
                new SearchResult(910, getString(R.string.LiteOptionsTopics), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_FORUM_TWOCOLUMN);
                }),
                new SearchResult(911, getString(R.string.LiteOptionsSpoiler), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_SPOILER);
                }),
                SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE ? new SearchResult(326 /* for compatibility */, getString(R.string.LiteOptionsBlur), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_BLUR);
                }) : null,
                new SearchResult(912, getString(R.string.LiteOptionsScale), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_SCALE);
                }),
                new SearchResult(913, getString(R.string.LiteOptionsCalls), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_CALLS_ANIMATIONS);
                }),
                new SearchResult(214 /* for compatibility */, getString(R.string.LiteOptionsAutoplayVideo), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_AUTOPLAY_VIDEOS);
                }),
                new SearchResult(213 /* for compatibility */, getString(R.string.LiteOptionsAutoplayGifs), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_AUTOPLAY_GIFS);
                }),
                new SearchResult(914, getString(R.string.LiteSmoothTransitions), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    presentFragment(set);
                    set.scrollToType(LiteModeSettingsActivity.SWITCH_TYPE_SMOOTH_TRANSITIONS);
                }),

                new SearchResult(400, getString(R.string.Language), R.drawable.msg2_language, () -> presentFragment(new LanguageSelectActivity())),
                new SearchResult(405, getString(R.string.ShowTranslateButton), getString(R.string.Language), R.drawable.msg2_language, () -> presentFragment(new LanguageSelectActivity())),
                MessagesController.getInstance(currentAccount).getTranslateController().isContextTranslateEnabled() ? new SearchResult(406, getString(R.string.DoNotTranslate), getString(R.string.Language), R.drawable.msg2_language, () -> presentFragment(new LanguageSelectActivity())) : null,

                new SearchResult(402, getString(R.string.AskAQuestion), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> showDialog(AlertsCreator.createSupportAlert(ProfileActivity.this, null))),
                new SearchResult(403, getString(R.string.TelegramFAQ), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), getString(R.string.TelegramFaqUrl))),
                new SearchResult(404, getString(R.string.PrivacyPolicy), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), getString(R.string.PrivacyPolicyUrl))),
            };
        }

        private boolean isPremiumFeatureAvailable(int feature) {
            if (getMessagesController().premiumFeaturesBlocked() && !getUserConfig().isPremium()) {
                return false;
            }

            if (feature == -1) {
                return true;
            }
            return getMessagesController().premiumFeaturesTypesToPosition.get(feature, -1) != -1;
        }

        private void loadFaqWebPage() {
            faqWebPage = getMessagesController().faqWebPage;
            if (faqWebPage != null) {
                faqSearchArray.addAll(getMessagesController().faqSearchArray);
            }
            if (faqWebPage != null || loadingFaqPage) {
                return;
            }
            loadingFaqPage = true;
            final TLRPC.TL_messages_getWebPage req2 = new TLRPC.TL_messages_getWebPage();
            req2.url = LocaleController.getString(R.string.TelegramFaqUrl);
            req2.hash = 0;
            getConnectionsManager().sendRequest(req2, (response2, error2) -> {
                if (response2 instanceof TLRPC.TL_messages_webPage) {
                    TLRPC.TL_messages_webPage res = (TLRPC.TL_messages_webPage) response2;
                    MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                    MessagesController.getInstance(currentAccount).putChats(res.chats, false);
                    response2 = res.webpage;
                }
                if (response2 instanceof TLRPC.WebPage) {
                    ArrayList<MessagesController.FaqSearchResult> arrayList = new ArrayList<>();
                    TLRPC.WebPage page = (TLRPC.WebPage) response2;
                    if (page.cached_page != null) {
                        for (int a = 0, N = page.cached_page.blocks.size(); a < N; a++) {
                            TLRPC.PageBlock block = page.cached_page.blocks.get(a);
                            if (block instanceof TLRPC.TL_pageBlockList) {
                                String paragraph = null;
                                if (a != 0) {
                                    TLRPC.PageBlock prevBlock = page.cached_page.blocks.get(a - 1);
                                    if (prevBlock instanceof TLRPC.TL_pageBlockParagraph) {
                                        TLRPC.TL_pageBlockParagraph pageBlockParagraph = (TLRPC.TL_pageBlockParagraph) prevBlock;
                                        paragraph = ArticleViewer.getPlainText(pageBlockParagraph.text).toString();
                                    }
                                }
                                TLRPC.TL_pageBlockList list = (TLRPC.TL_pageBlockList) block;
                                for (int b = 0, N2 = list.items.size(); b < N2; b++) {
                                    TLRPC.PageListItem item = list.items.get(b);
                                    if (item instanceof TLRPC.TL_pageListItemText) {
                                        TLRPC.TL_pageListItemText itemText = (TLRPC.TL_pageListItemText) item;
                                        String url = ArticleViewer.getUrl(itemText.text);
                                        String text = ArticleViewer.getPlainText(itemText.text).toString();
                                        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(text)) {
                                            continue;
                                        }
                                        String[] path;
                                        if (paragraph != null) {
                                            path = new String[]{LocaleController.getString(R.string.SettingsSearchFaq), paragraph};
                                        } else {
                                            path = new String[]{LocaleController.getString(R.string.SettingsSearchFaq)};
                                        }
                                        arrayList.add(new MessagesController.FaqSearchResult(text, path, url));
                                    }
                                }
                            } else if (block instanceof TLRPC.TL_pageBlockAnchor) {
                                break;
                            }
                        }
                        faqWebPage = page;
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        faqSearchArray.addAll(arrayList);
                        getMessagesController().faqSearchArray = arrayList;
                        getMessagesController().faqWebPage = faqWebPage;
                        if (!searchWas) {
                            notifyDataSetChanged();
                        }
                    });
                }
                loadingFaqPage = false;
            });
        }

        @Override
        public int getItemCount() {
            if (searchWas) {
                return searchResults.size() + (faqSearchResults.isEmpty() ? 0 : 1 + faqSearchResults.size());
            }
            return (recentSearches.isEmpty() ? 0 : recentSearches.size() + 1) + (faqSearchArray.isEmpty() ? 0 : faqSearchArray.size() + 1);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 0;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    SettingsSearchCell searchCell = (SettingsSearchCell) holder.itemView;
                    if (searchWas) {
                        if (position < searchResults.size()) {
                            SearchResult result = searchResults.get(position);
                            SearchResult prevResult = position > 0 ? searchResults.get(position - 1) : null;
                            int icon;
                            if (prevResult != null && prevResult.iconResId == result.iconResId) {
                                icon = 0;
                            } else {
                                icon = result.iconResId;
                            }
                            searchCell.setTextAndValueAndIcon(resultNames.get(position), result.path, icon, position < searchResults.size() - 1);
                        } else {
                            position -= searchResults.size() + 1;
                            MessagesController.FaqSearchResult result = faqSearchResults.get(position);
                            searchCell.setTextAndValue(resultNames.get(position + searchResults.size()), result.path, true, position < searchResults.size() - 1);
                        }
                    } else {
                        if (!recentSearches.isEmpty()) {
                            position--;
                        }
                        if (position < recentSearches.size()) {
                            Object object = recentSearches.get(position);
                            if (object instanceof SearchResult) {
                                SearchResult result = (SearchResult) object;
                                searchCell.setTextAndValue(result.searchTitle, result.path, false, position < recentSearches.size() - 1);
                            } else if (object instanceof MessagesController.FaqSearchResult) {
                                MessagesController.FaqSearchResult result = (MessagesController.FaqSearchResult) object;
                                searchCell.setTextAndValue(result.title, result.path, true, position < recentSearches.size() - 1);
                            }
                        } else {
                            position -= recentSearches.size() + 1;
                            MessagesController.FaqSearchResult result = faqSearchArray.get(position);
                            searchCell.setTextAndValue(result.title, result.path, true, position < recentSearches.size() - 1);
                        }
                    }
                    break;
                }
                case 1: {
                    GraySectionCell sectionCell = (GraySectionCell) holder.itemView;
                    sectionCell.setText(LocaleController.getString(R.string.SettingsFaqSearchTitle));
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(LocaleController.getString(R.string.SettingsRecent));
                    break;
                }
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new SettingsSearchCell(mContext);
                    break;
                case 1:
                    view = new GraySectionCell(mContext);
                    break;
                case 2:
                default:
                    view = new HeaderCell(mContext, 16);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (searchWas) {
                if (position < searchResults.size()) {
                    return 0;
                } else if (position == searchResults.size()) {
                    return 1;
                }
            } else {
                if (position == 0) {
                    if (!recentSearches.isEmpty()) {
                        return 2;
                    } else {
                        return 1;
                    }
                } else if (!recentSearches.isEmpty() && position == recentSearches.size() + 1) {
                    return 1;
                }
            }
            return 0;
        }

        public void addRecent(Object object) {
            int index = recentSearches.indexOf(object);
            if (index >= 0) {
                recentSearches.remove(index);
            }
            recentSearches.add(0, object);
            if (!searchWas) {
                notifyDataSetChanged();
            }
            if (recentSearches.size() > 20) {
                recentSearches.remove(recentSearches.size() - 1);
            }
            LinkedHashSet<String> toSave = new LinkedHashSet<>();
            for (int a = 0, N = recentSearches.size(); a < N; a++) {
                Object o = recentSearches.get(a);
                if (o instanceof SearchResult) {
                    ((SearchResult) o).num = a;
                } else if (o instanceof MessagesController.FaqSearchResult) {
                    ((MessagesController.FaqSearchResult) o).num = a;
                }
                toSave.add(o.toString());
            }
            MessagesController.getGlobalMainSettings().edit().putStringSet("settingsSearchRecent2", toSave).commit();
        }

        public void clearRecent() {
            recentSearches.clear();
            MessagesController.getGlobalMainSettings().edit().remove("settingsSearchRecent2").commit();
            notifyDataSetChanged();
        }

        private int getNum(Object o) {
            if (o instanceof SearchResult) {
                return ((SearchResult) o).num;
            } else if (o instanceof MessagesController.FaqSearchResult) {
                return ((MessagesController.FaqSearchResult) o).num;
            }
            return 0;
        }

        public void search(String text) {
            lastSearchString = text;
            if (searchRunnable != null) {
                Utilities.searchQueue.cancelRunnable(searchRunnable);
                searchRunnable = null;
            }
            if (TextUtils.isEmpty(text)) {
                searchWas = false;
                searchResults.clear();
                faqSearchResults.clear();
                resultNames.clear();
                emptyView.stickerView.getImageReceiver().startAnimation();
                emptyView.title.setText(getString(R.string.SettingsNoRecent));
                notifyDataSetChanged();
                return;
            }
            Utilities.searchQueue.postRunnable(searchRunnable = () -> {
                ArrayList<SearchResult> results = new ArrayList<>();
                ArrayList<MessagesController.FaqSearchResult> faqResults = new ArrayList<>();
                ArrayList<CharSequence> names = new ArrayList<>();
                String[] searchArgs = text.split(" ");
                String[] translitArgs = new String[searchArgs.length];
                for (int a = 0; a < searchArgs.length; a++) {
                    translitArgs[a] = LocaleController.getInstance().getTranslitString(searchArgs[a]);
                    if (translitArgs[a].equals(searchArgs[a])) {
                        translitArgs[a] = null;
                    }
                }

                for (int a = 0; a < searchArray.length; a++) {
                    SearchResult result = searchArray[a];
                    if (result == null) {
                        continue;
                    }
                    String title = " " + result.searchTitle.toLowerCase();
                    SpannableStringBuilder stringBuilder = null;
                    for (int i = 0; i < searchArgs.length; i++) {
                        if (searchArgs[i].length() != 0) {
                            String searchString = searchArgs[i];
                            int index = title.indexOf(" " + searchString);
                            if (index < 0 && translitArgs[i] != null) {
                                searchString = translitArgs[i];
                                index = title.indexOf(" " + searchString);
                            }
                            if (index >= 0) {
                                if (stringBuilder == null) {
                                    stringBuilder = new SpannableStringBuilder(result.searchTitle);
                                }
                                stringBuilder.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + searchString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else {
                                break;
                            }
                        }
                        if (stringBuilder != null && i == searchArgs.length - 1) {
                            if (result.guid == 502) {
                                int freeAccount = -1;
                                for (int b = 0; b < UserConfig.MAX_ACCOUNT_COUNT; b++) {
                                    if (!UserConfig.getInstance(b).isClientActivated()) {
                                        freeAccount = b;
                                        break;
                                    }
                                }
                                if (freeAccount < 0) {
                                    continue;
                                }
                            }
                            results.add(result);
                            names.add(stringBuilder);
                        }
                    }
                }
                if (faqWebPage != null) {
                    for (int a = 0, N = faqSearchArray.size(); a < N; a++) {
                        MessagesController.FaqSearchResult result = faqSearchArray.get(a);
                        String title = " " + result.title.toLowerCase();
                        SpannableStringBuilder stringBuilder = null;
                        for (int i = 0; i < searchArgs.length; i++) {
                            if (searchArgs[i].length() != 0) {
                                String searchString = searchArgs[i];
                                int index = title.indexOf(" " + searchString);
                                if (index < 0 && translitArgs[i] != null) {
                                    searchString = translitArgs[i];
                                    index = title.indexOf(" " + searchString);
                                }
                                if (index >= 0) {
                                    if (stringBuilder == null) {
                                        stringBuilder = new SpannableStringBuilder(result.title);
                                    }
                                    stringBuilder.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + searchString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                } else {
                                    break;
                                }
                            }
                            if (stringBuilder != null && i == searchArgs.length - 1) {
                                faqResults.add(result);
                                names.add(stringBuilder);
                            }
                        }
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    if (!text.equals(lastSearchString)) {
                        return;
                    }
                    if (!searchWas) {
                        emptyView.stickerView.getImageReceiver().startAnimation();
                        emptyView.title.setText(LocaleController.getString(R.string.SettingsNoResults));
                    }
                    searchWas = true;
                    searchResults = results;
                    faqSearchResults = faqResults;
                    resultNames = names;
                    notifyDataSetChanged();
                    emptyView.stickerView.getImageReceiver().startAnimation();
                });
            }, 300);
        }

        public boolean isSearchWas() {
            return searchWas;
        }
    }

    private void openUrl(String url, Browser.Progress progress) {
        if (url.startsWith("@")) {
            getMessagesController().openByUserName(url.substring(1), ProfileActivity.this, 0, progress);
        } else if (url.startsWith("#") || url.startsWith("$")) {
            DialogsActivity fragment = new DialogsActivity(null);
            fragment.setSearchString(url);
            presentFragment(fragment);
        } else if (url.startsWith("/")) {
            if (parentLayout.getFragmentStack().size() > 1) {
                BaseFragment previousFragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
                if (previousFragment instanceof ChatActivity) {
                    finishFragment();
                    ((ChatActivity) previousFragment).getChatActivityEnterView().setCommand(null, url, false, false);
                }
            }
        }
    }

    private void dimBehindView(View view, boolean enable) {
        scrimView = view;
        dimBehindView(enable);
    }

    private void dimBehindView(View view, float value) {
        scrimView = view;
        dimBehindView(value);
    }

    private void dimBehindView(boolean enable) {
        dimBehindView(enable ? 0.2f : 0);
    }

    private AnimatorSet scrimAnimatorSet = null;

    private void dimBehindView(float value) {
        boolean enable = value > 0;
        fragmentView.invalidate();
        if (scrimAnimatorSet != null) {
            scrimAnimatorSet.cancel();
        }
        scrimAnimatorSet = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<>();
        ValueAnimator scrimPaintAlphaAnimator;
        if (enable) {
            animators.add(scrimPaintAlphaAnimator = ValueAnimator.ofFloat(0, value));
        } else {
            animators.add(scrimPaintAlphaAnimator = ValueAnimator.ofFloat(scrimPaint.getAlpha() / 255f, 0));
        }
        scrimPaintAlphaAnimator.addUpdateListener(a -> {
            scrimPaint.setAlpha((int) (255 * (float) a.getAnimatedValue()));
        });
        scrimAnimatorSet.playTogether(animators);
        scrimAnimatorSet.setDuration(enable ? 150 : 220);
        if (!enable) {
            scrimAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    scrimView = null;
                    fragmentView.invalidate();
                }
            });
        }
        scrimAnimatorSet.start();
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        if (resourcesProvider != null) {
            return null;
        }
        ThemeDescription.ThemeDescriptionDelegate themeDelegate = () -> {
            if (listView != null) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof UserCell) {
                        ((UserCell) child).update(0);
                    }
                }
            }
            if (!isPulledDown) {
                if (onlineTextView[1] != null) {
                    final Object onlineTextViewTag = onlineTextView[1].getTag();
                    for (int i = 0; i < 2; i++) {
                        if (onlineTextViewTag instanceof Integer) {
                            onlineTextView[i + 1].setTextColor(applyPeerColor(getThemedColor((Integer) onlineTextViewTag), true, isOnline[0]));
                        } else {
                            onlineTextView[i + 1].setTextColor(applyPeerColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue), true, true));
                        }
                    }
                }
                if (lockIconDrawable != null) {
                    lockIconDrawable.setColorFilter(getThemedColor(Theme.key_chat_lockIcon), PorterDuff.Mode.MULTIPLY);
                }
                if (scamDrawable != null) {
                    scamDrawable.setColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue));
                }
                if (nameTextView[1] != null) {
                    nameTextView[1].setTextColor(getThemedColor(Theme.key_profile_title));
                }
                if (actionBar != null) {
                    actionBar.setItemsColor(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), false);
                    actionBar.setItemsBackgroundColor(peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), false);
                }
            }
            updateEmojiStatusDrawableColor();
        };
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        if (sharedMediaLayout != null) {
            arrayList.addAll(sharedMediaLayout.getThemeDescriptions());
        }

        arrayList.add(new ThemeDescription(listView, 0, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(searchListView, 0, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(listView, 0, null, null, null, null, Theme.key_windowBackgroundGray));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_actionBarSelectorBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_chat_lockIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_subtitleInProfileBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundActionBarBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_profile_title));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_profile_status));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_subtitleInProfileBlue));

        if (mediaCounterTextView != null) {
            arrayList.add(new ThemeDescription(mediaCounterTextView.getTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, themeDelegate, Theme.key_player_actionBarSubtitle));
            arrayList.add(new ThemeDescription(mediaCounterTextView.getNextTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, themeDelegate, Theme.key_player_actionBarSubtitle));
        }

        arrayList.add(new ThemeDescription(topView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(avatarImage, 0, null, null, Theme.avatarDrawables, null, Theme.key_avatar_text));
        arrayList.add(new ThemeDescription(avatarImage, 0, null, null, new Drawable[]{avatarDrawable}, null, Theme.key_avatar_backgroundInProfileBlue));

        arrayList.add(new ThemeDescription(writeButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_profile_actionIcon));
        arrayList.add(new ThemeDescription(writeButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_profile_actionBackground));
        arrayList.add(new ThemeDescription(writeButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_profile_actionPressedBackground));

        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGreenText2));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_text_RedRegular));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText2));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueButton));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueIcon));

        arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextDetailCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextDetailCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        arrayList.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        arrayList.add(new ThemeDescription(listView, 0, new Class[]{SettingsSuggestionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{SettingsSuggestionCell.class}, new String[]{"detailTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_LINKCOLOR, new Class[]{SettingsSuggestionCell.class}, new String[]{"detailTextView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{SettingsSuggestionCell.class}, new String[]{"yesButton"}, null, null, null, Theme.key_featuredStickers_buttonText));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE, new Class[]{SettingsSuggestionCell.class}, new String[]{"yesButton"}, null, null, null, Theme.key_featuredStickers_addButton));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{SettingsSuggestionCell.class}, new String[]{"yesButton"}, null, null, null, Theme.key_featuredStickers_addButtonPressed));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{SettingsSuggestionCell.class}, new String[]{"noButton"}, null, null, null, Theme.key_featuredStickers_buttonText));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE, new Class[]{SettingsSuggestionCell.class}, new String[]{"noButton"}, null, null, null, Theme.key_featuredStickers_addButton));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{SettingsSuggestionCell.class}, new String[]{"noButton"}, null, null, null, Theme.key_featuredStickers_addButtonPressed));

        arrayList.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{UserCell.class}, new String[]{"adminTextView"}, null, null, null, Theme.key_profile_creatorIcon));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusColor"}, null, null, themeDelegate, Theme.key_windowBackgroundWhiteGrayText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, new String[]{"statusOnlineColor"}, null, null, themeDelegate, Theme.key_windowBackgroundWhiteBlueText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{UserCell.class}, null, Theme.avatarDrawables, null, Theme.key_avatar_text));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundRed));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundOrange));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundViolet));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundGreen));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundCyan));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, themeDelegate, Theme.key_avatar_backgroundPink));

        arrayList.add(new ThemeDescription(undoView, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_undo_background));
        arrayList.add(new ThemeDescription(undoView, 0, new Class[]{UndoView.class}, new String[]{"undoImageView"}, null, null, null, Theme.key_undo_cancelColor));
        arrayList.add(new ThemeDescription(undoView, 0, new Class[]{UndoView.class}, new String[]{"undoTextView"}, null, null, null, Theme.key_undo_cancelColor));
        arrayList.add(new ThemeDescription(undoView, 0, new Class[]{UndoView.class}, new String[]{"infoTextView"}, null, null, null, Theme.key_undo_infoColor));
        arrayList.add(new ThemeDescription(undoView, 0, new Class[]{UndoView.class}, new String[]{"textPaint"}, null, null, null, Theme.key_undo_infoColor));
        arrayList.add(new ThemeDescription(undoView, 0, new Class[]{UndoView.class}, new String[]{"progressPaint"}, null, null, null, Theme.key_undo_infoColor));
        arrayList.add(new ThemeDescription(undoView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{UndoView.class}, new String[]{"leftImageView"}, null, null, null, Theme.key_undo_infoColor));

        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{AboutLinkCell.class}, Theme.profile_aboutTextPaint, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_LINKCOLOR, new Class[]{AboutLinkCell.class}, Theme.profile_aboutTextPaint, null, null, Theme.key_windowBackgroundWhiteLinkText));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{AboutLinkCell.class}, Theme.linkSelectionPaint, null, null, Theme.key_windowBackgroundWhiteLinkSelection));

        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        arrayList.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        arrayList.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        arrayList.add(new ThemeDescription(searchListView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        arrayList.add(new ThemeDescription(searchListView, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_graySectionText));
        arrayList.add(new ThemeDescription(searchListView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection));

        arrayList.add(new ThemeDescription(searchListView, 0, new Class[]{SettingsSearchCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(searchListView, 0, new Class[]{SettingsSearchCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        arrayList.add(new ThemeDescription(searchListView, 0, new Class[]{SettingsSearchCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));

        if (mediaHeaderVisible) {
            arrayList.add(new ThemeDescription(nameTextView[1], 0, null, null, verifiedCheckDrawable, null, Theme.key_player_actionBarTitle));
            arrayList.add(new ThemeDescription(nameTextView[1], 0, null, null, verifiedDrawable, null, Theme.key_windowBackgroundWhite));
        } else {
            arrayList.add(new ThemeDescription(nameTextView[1], 0, null, null, verifiedCheckDrawable, null, Theme.key_profile_verifiedCheck));
            arrayList.add(new ThemeDescription(nameTextView[1], 0, null, null, verifiedDrawable, null, Theme.key_profile_verifiedBackground));
        }

        return arrayList;
    }

    public void updateListAnimated(boolean updateOnlineCount) {
        updateListAnimated(updateOnlineCount, false);
    }

    private void updateListAnimated(boolean updateOnlineCount, boolean triedInLayout) {
        if (listAdapter == null) {
            if (updateOnlineCount) {
                viewModel.updateOnlineCount(false);
            }
            updateRowsIds();
            return;
        }

        if (!triedInLayout && listView.isInLayout()) {
            if (!listView.isAttachedToWindow()) return;
            listView.post(() -> updateListAnimated(updateOnlineCount, true));
            return;
        }

        DiffCallback diffCallback = new DiffCallback();
        diffCallback.oldRowCount = rowCount;
        diffCallback.fillPositions(diffCallback.oldPositionToItem);
        diffCallback.oldChatParticipant.clear();
        diffCallback.oldChatParticipantSorted.clear();
        diffCallback.oldChatParticipant.addAll(visibleChatParticipants);
        diffCallback.oldChatParticipantSorted.addAll(visibleSortedUsers);
        diffCallback.oldMembersStartRow = membersStartRow;
        diffCallback.oldMembersEndRow = membersEndRow;
        if (updateOnlineCount) {
            viewModel.updateOnlineCount(false);
        }
        saveScrollPosition();
        updateRowsIds();
        diffCallback.fillPositions(diffCallback.newPositionToItem);
        try {
            DiffUtil.calculateDiff(diffCallback).dispatchUpdatesTo(listAdapter);
        } catch (Exception e) {
            FileLog.e(e);
            listAdapter.notifyDataSetChanged();
        }
        if (savedScrollPosition >= 0) {
            layoutManager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset - listView.getPaddingTop());
        }
        AndroidUtilities.updateVisibleRows(listView);
    }

    int savedScrollPosition = -1;
    int savedScrollOffset;
    boolean savedScrollToSharedMedia;

    private void saveScrollPosition() {
        if (listView != null && layoutManager != null && listView.getChildCount() > 0 && !savedScrollToSharedMedia) {
            View view = null;
            int position = -1;
            int top = Integer.MAX_VALUE;
            for (int i = 0; i < listView.getChildCount(); i++) {
                int childPosition = listView.getChildAdapterPosition(listView.getChildAt(i));
                View child = listView.getChildAt(i);
                if (childPosition != RecyclerListView.NO_POSITION && child.getTop() < top) {
                    view = child;
                    position = childPosition;
                    top = child.getTop();
                }
            }
            if (view != null) {
                savedScrollPosition = position;
                savedScrollOffset = view.getTop();
                if (savedScrollPosition == 0 && !viewModel.getState().getValue().isAllowPullingDown() && savedScrollOffset > AndroidUtilities.dp(88)) {
                    savedScrollOffset = AndroidUtilities.dp(88);
                }

                layoutManager.scrollToPositionWithOffset(position, view.getTop() - listView.getPaddingTop());
            }
        }
    }

    public void scrollToSharedMedia() {
        scrollToSharedMedia(false);
    }
    public void scrollToSharedMedia(boolean animated) {
        if (sharedMediaRow >= 0) {
            if (animated) {
                LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(getContext(), LinearSmoothScrollerCustom.POSITION_TOP, .6f);
                linearSmoothScroller.setTargetPosition(sharedMediaRow);
                linearSmoothScroller.setOffset(-listView.getPaddingTop());
                layoutManager.startSmoothScroll(linearSmoothScroller);
            } else {
                layoutManager.scrollToPositionWithOffset(sharedMediaRow, -listView.getPaddingTop());
            }
        }
    }

    private void onTextDetailCellImageClicked(View view) {
        View parent = (View) view.getParent();
        if (parent.getTag() != null && ((int) parent.getTag()) == usernameRow) {
            Bundle args = new Bundle();
            args.putLong("chat_id", viewModel.getArgs().chatId);
            args.putLong("user_id", viewModel.getArgs().userId);
            presentFragment(new QrActivity(args));
        } else if (parent.getTag() != null && ((int) parent.getTag()) == birthdayRow) {
            if (viewModel.getArgs().userId == getUserConfig().getClientUserId()) {
                presentFragment(new PremiumPreviewFragment("my_profile_gift"));
                return;
            }
            if (UserObject.areGiftsDisabled(viewModel.getState().getValue().getUserInfo())) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserDisallowedGifts, DialogObject.getShortName(viewModel.getArgs().userId)))).show();
                return;
            }
            showDialog(new GiftSheet(getContext(), currentAccount, viewModel.getArgs().userId, null, null));
        }
    }

    private boolean fullyVisible;

    @Override
    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        writeButtonSetBackground();
        fullyVisible = true;
        createBirthdayEffect();
    }

    private void writeButtonSetBackground() {
        if (writeButton == null) return;
        try {
            Drawable shadowDrawable = fragmentView.getContext().getResources().getDrawable(R.drawable.floating_shadow_profile).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
            int color1 = getThemedColor(Theme.key_profile_actionBackground);
            int color2 = getThemedColor(Theme.key_profile_actionPressedBackground);
            int iconColor = getThemedColor(Theme.key_profile_actionIcon);
            if (peerColor != null && Theme.hasHue(color1)) {
                color1 = Theme.adaptHSV(peerColor.getBgColor1(false), +.05f, -.04f);
                color2 = applyPeerColor2(color2);
                iconColor = Color.WHITE;
            }
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable,
                    Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), color1, color2),
                    0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            writeButton.setBackground(combinedDrawable);
            writeButton.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.MULTIPLY));
        } catch (Exception e) {}
    }

    @SuppressLint("NotifyDataSetChanged")
    public void openAddToContact(TLRPC.User user, Bundle args) {
        ContactAddActivity contactAddActivity = new ContactAddActivity(args, resourcesProvider);
        contactAddActivity.setDelegate(() -> {
            int currentAddToContactsRow = addToContactsRow;
            if (currentAddToContactsRow >= 0) {
                if (sharedMediaRow == -1) {
                    updateRowsIds();
                    listAdapter.notifyDataSetChanged();
                } else {
                    updateListAnimated(false);
                }
            }

            if (sharedMediaRow == -1) {
                if (viewModel.getState().getValue().isInLandscapeMode() || AndroidUtilities.isTablet()) {
                    listView.setPadding(0, AndroidUtilities.dp(88), 0, 0);
                    expandAnimator.cancel();
                    expandAnimatorValues[0] = 1f;
                    expandAnimatorValues[1] = 0f;
                    setAvatarExpandProgress(1f);
                    extraHeight = AndroidUtilities.dp(88);
                } else {
                    final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                    int ws = View.MeasureSpec.makeMeasureSpec(listView.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
                    int hs = View.MeasureSpec.makeMeasureSpec(listView.getMeasuredHeight(), View.MeasureSpec.UNSPECIFIED);
                    int contentHeight = 0;
                    for (int i = 0; i < listAdapter.getItemCount(); i++) {
                        RecyclerView.ViewHolder holder = listAdapter.createViewHolder(null, listAdapter.getItemViewType(i));
                        listAdapter.onBindViewHolder(holder, i);
                        holder.itemView.measure(ws, hs);
                        contentHeight += holder.itemView.getMeasuredHeight();
                    }
                    int paddingBottom = Math.max(0, fragmentView.getMeasuredHeight() - (contentHeight + AndroidUtilities.dp(88) + actionBarHeight));
                    listView.setPadding(0, listView.getPaddingTop(), 0, paddingBottom);
                }
            }
            undoView.showWithAction(viewModel.getArgs().dialogId, UndoView.ACTION_CONTACT_ADDED, user);
        });
        presentFragment(contactAddActivity);
    }

    private boolean isQrNeedVisible() {
        return true;
    }

    private class DiffCallback extends DiffUtil.Callback {

        int oldRowCount;

        SparseIntArray oldPositionToItem = new SparseIntArray();
        SparseIntArray newPositionToItem = new SparseIntArray();
        ArrayList<TLRPC.ChatParticipant> oldChatParticipant = new ArrayList<>();
        ArrayList<Integer> oldChatParticipantSorted = new ArrayList<>();
        int oldMembersStartRow;
        int oldMembersEndRow;

        @Override
        public int getOldListSize() {
            return oldRowCount;
        }

        @Override
        public int getNewListSize() {
            return rowCount;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (newItemPosition >= membersStartRow && newItemPosition < membersEndRow) {
                if (oldItemPosition >= oldMembersStartRow && oldItemPosition < oldMembersEndRow) {
                    TLRPC.ChatParticipant oldItem;
                    TLRPC.ChatParticipant newItem;
                    if (!oldChatParticipantSorted.isEmpty()) {
                        oldItem = oldChatParticipant.get(oldChatParticipantSorted.get(oldItemPosition - oldMembersStartRow));
                    } else {
                        oldItem = oldChatParticipant.get(oldItemPosition - oldMembersStartRow);
                    }

                    if (!viewModel.getState().getValue().getSortedUsers().isEmpty()) {
                        newItem = visibleChatParticipants.get(visibleSortedUsers.get(newItemPosition - membersStartRow));
                    } else {
                        newItem = visibleChatParticipants.get(newItemPosition - membersStartRow);
                    }
                    return oldItem.user_id == newItem.user_id;
                }
            }
            int oldIndex = oldPositionToItem.get(oldItemPosition, -1);
            int newIndex = newPositionToItem.get(newItemPosition, -1);
            return oldIndex == newIndex && oldIndex >= 0;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }

        public void fillPositions(SparseIntArray sparseIntArray) {
            sparseIntArray.clear();
            int pointer = 0;
            put(++pointer, setAvatarRow, sparseIntArray);
            put(++pointer, setAvatarSectionRow, sparseIntArray);
            put(++pointer, numberSectionRow, sparseIntArray);
            put(++pointer, numberRow, sparseIntArray);
            put(++pointer, setUsernameRow, sparseIntArray);
            put(++pointer, bioRow, sparseIntArray);
            put(++pointer, phoneSuggestionRow, sparseIntArray);
            put(++pointer, phoneSuggestionSectionRow, sparseIntArray);
            put(++pointer, passwordSuggestionRow, sparseIntArray);
            put(++pointer, passwordSuggestionSectionRow, sparseIntArray);
            put(++pointer, graceSuggestionRow, sparseIntArray);
            put(++pointer, graceSuggestionSectionRow, sparseIntArray);
            put(++pointer, settingsSectionRow, sparseIntArray);
            put(++pointer, settingsSectionRow2, sparseIntArray);
            put(++pointer, notificationRow, sparseIntArray);
            put(++pointer, languageRow, sparseIntArray);
            put(++pointer, premiumRow, sparseIntArray);
            put(++pointer, starsRow, sparseIntArray);
            put(++pointer, businessRow, sparseIntArray);
            put(++pointer, premiumSectionsRow, sparseIntArray);
            put(++pointer, premiumGiftingRow, sparseIntArray);
            put(++pointer, privacyRow, sparseIntArray);
            put(++pointer, dataRow, sparseIntArray);
            put(++pointer, liteModeRow, sparseIntArray);
            put(++pointer, chatRow, sparseIntArray);
            put(++pointer, filtersRow, sparseIntArray);
            put(++pointer, stickersRow, sparseIntArray);
            put(++pointer, devicesRow, sparseIntArray);
            put(++pointer, devicesSectionRow, sparseIntArray);
            put(++pointer, helpHeaderRow, sparseIntArray);
            put(++pointer, questionRow, sparseIntArray);
            put(++pointer, faqRow, sparseIntArray);
            put(++pointer, policyRow, sparseIntArray);
            put(++pointer, helpSectionCell, sparseIntArray);
            put(++pointer, debugHeaderRow, sparseIntArray);
            put(++pointer, sendLogsRow, sparseIntArray);
            put(++pointer, sendLastLogsRow, sparseIntArray);
            put(++pointer, clearLogsRow, sparseIntArray);
            put(++pointer, switchBackendRow, sparseIntArray);
            put(++pointer, versionRow, sparseIntArray);
            put(++pointer, emptyRow, sparseIntArray);
            put(++pointer, bottomPaddingRow, sparseIntArray);
            put(++pointer, infoHeaderRow, sparseIntArray);
            put(++pointer, phoneRow, sparseIntArray);
            put(++pointer, locationRow, sparseIntArray);
            put(++pointer, userInfoRow, sparseIntArray);
            put(++pointer, channelInfoRow, sparseIntArray);
            put(++pointer, usernameRow, sparseIntArray);
            put(++pointer, notificationsDividerRow, sparseIntArray);
            put(++pointer, reportDividerRow, sparseIntArray);
            put(++pointer, notificationsRow, sparseIntArray);
            put(++pointer, infoSectionRow, sparseIntArray);
            put(++pointer, affiliateRow, sparseIntArray);
            put(++pointer, infoAffiliateRow, sparseIntArray);
            put(++pointer, sendMessageRow, sparseIntArray);
            put(++pointer, reportRow, sparseIntArray);
            put(++pointer, reportReactionRow, sparseIntArray);
            put(++pointer, addToContactsRow, sparseIntArray);
            put(++pointer, settingsTimerRow, sparseIntArray);
            put(++pointer, settingsKeyRow, sparseIntArray);
            put(++pointer, secretSettingsSectionRow, sparseIntArray);
            put(++pointer, membersHeaderRow, sparseIntArray);
            put(++pointer, addMemberRow, sparseIntArray);
            put(++pointer, subscribersRow, sparseIntArray);
            put(++pointer, subscribersRequestsRow, sparseIntArray);
            put(++pointer, administratorsRow, sparseIntArray);
            put(++pointer, settingsRow, sparseIntArray);
            put(++pointer, blockedUsersRow, sparseIntArray);
            put(++pointer, membersSectionRow, sparseIntArray);
            put(++pointer, channelBalanceSectionRow, sparseIntArray);
            put(++pointer, sharedMediaRow, sparseIntArray);
            put(++pointer, unblockRow, sparseIntArray);
            put(++pointer, addToGroupButtonRow, sparseIntArray);
            put(++pointer, addToGroupInfoRow, sparseIntArray);
            put(++pointer, joinRow, sparseIntArray);
            put(++pointer, lastSectionRow, sparseIntArray);
            put(++pointer, notificationsSimpleRow, sparseIntArray);
            put(++pointer, bizHoursRow, sparseIntArray);
            put(++pointer, bizLocationRow, sparseIntArray);
            put(++pointer, birthdayRow, sparseIntArray);
            put(++pointer, channelRow, sparseIntArray);
            put(++pointer, botStarsBalanceRow, sparseIntArray);
            put(++pointer, botTonBalanceRow, sparseIntArray);
            put(++pointer, channelBalanceRow, sparseIntArray);
            put(++pointer, balanceDividerRow, sparseIntArray);
            put(++pointer, botAppRow, sparseIntArray);
            put(++pointer, botPermissionsHeader, sparseIntArray);
            put(++pointer, botPermissionLocation, sparseIntArray);
            put(++pointer, botPermissionEmojiStatus, sparseIntArray);
            put(++pointer, botPermissionBiometry, sparseIntArray);
            put(++pointer, botPermissionsDivider, sparseIntArray);
            put(++pointer, channelDividerRow, sparseIntArray);
        }

        private void put(int id, int position, SparseIntArray sparseIntArray) {
            if (position >= 0) {
                sparseIntArray.put(position, id);
            }
        }
    }

    @Override
    public boolean isLightStatusBar() {
        int color;
        if (isPulledDown) {
            return false;
        }
        if (actionBar.isActionModeShowed()) {
            color = getThemedColor(Theme.key_actionBarActionModeDefault);
        } else if (mediaHeaderVisible) {
            color = getThemedColor(Theme.key_windowBackgroundWhite);
        } else if (peerColor != null) {
            color = peerColor.getBgColor2(Theme.isCurrentThemeDark());
        } else {
            color = getThemedColor(Theme.key_actionBarDefault);
        }
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

    public String getLink(String username, int topicId) {
        String link = getMessagesController().linkPrefix + "/" + username;
        if (topicId != 0) {
            link += "/" + topicId;
        }
        return link;
    }

    float photoDescriptionProgress = -1;
    private void checkPhotoDescriptionAlpha() {
        float p = photoDescriptionProgress;
        if (playProfileAnimation == 1 && (!fragmentOpened || openAnimationInProgress)) {
            photoDescriptionProgress = 0;
        } else if (playProfileAnimation == 2 && (!fragmentOpened || openAnimationInProgress)) {
            photoDescriptionProgress = onlineTextView[1] == null ? 0 : onlineTextView[1].getAlpha();
        } else {
            if (viewModel.getArgs().userId == UserConfig.getInstance(currentAccount).clientUserId) {
                photoDescriptionProgress = currentExpandAnimatorValue * (1f - customAvatarProgress);
            } else {
                photoDescriptionProgress = currentExpandAnimatorValue * customAvatarProgress;
            }
        }
//        if (p == photoDescriptionProgress) {
//            return;
//        }
        if (viewModel.getArgs().userId == UserConfig.getInstance(currentAccount).clientUserId) {
            if (viewModel.getState().getValue().isHasFallbackPhoto()) {
                customPhotoOffset = AndroidUtilities.dp(28) * photoDescriptionProgress;
                if (onlineTextView[2] != null) {
                    onlineTextView[2].setAlpha(currentExpandAnimatorValue);
                    onlineTextView[3].setAlpha(1f - currentExpandAnimatorValue);
                    //  onlineTextView[1].setAlpha(1f - expandProgress);
                    onlineTextView[1].setTranslationX(onlineX + customPhotoOffset);
                    avatarContainer2.invalidate();
                    if (showStatusButton != null) {
                        showStatusButton.setAlpha2(1f - currentExpandAnimatorValue);
                    }
                }
            } else {
                if (onlineTextView[2] != null) {
                    onlineTextView[2].setAlpha(0);
                    onlineTextView[3].setAlpha(0);
                }
                if (showStatusButton != null) {
                    showStatusButton.setAlpha2(1f);
                }
            }

        } else {
            if (hasCustomPhoto) {
                if (onlineTextView[2] != null) {
                    onlineTextView[2].setAlpha(photoDescriptionProgress);
                }
                if (showStatusButton != null) {
                    showStatusButton.setAlpha2(1f - photoDescriptionProgress);
                }
            } else {
                if (onlineTextView[2] != null) {
                    onlineTextView[2].setAlpha(0);
                }
                if (showStatusButton != null) {
                    showStatusButton.setAlpha2(1f);
                }
            }
        }
    }

    private void updateStoriesViewBounds(boolean animated) {
        if (storyView == null && giftsView == null || actionBar == null) {
            return;
        }
        float atop = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
        float aleft = 0;
        float aright = actionBar.getWidth();

        if (actionBar.getBackButton() != null) {
            aleft = Math.max(aleft, actionBar.getBackButton().getRight());
        }
        if (actionBar.menu != null) {
            for (int i = 0; i < actionBar.menu.getChildCount(); ++i) {
                View child = actionBar.menu.getChildAt(i);
                if (child.getAlpha() <= 0 || child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                int left = actionBar.menu.getLeft() + (int) child.getX();
                if (left < aright) {
                    aright = AndroidUtilities.lerp(aright, left, child.getAlpha());
                }
            }
        }
        if (storyView != null) {
            storyView.setBounds(aleft, aright, atop + (actionBar.getHeight() - atop) / 2f, !animated);
        }
        if (giftsView != null) {
            giftsView.setBounds(aleft, aright, atop + (actionBar.getHeight() - atop) / 2f, !animated);
        }
    }

    private class ClippedListView extends RecyclerListView implements StoriesListPlaceProvider.ClippedView {
        public ClippedListView(Context context) {
            super(context);
        }

        @Override
        public void updateClip(int[] clip) {
            clip[0] = actionBar.getMeasuredHeight();
            clip[1] = getMeasuredHeight() - getPaddingBottom();
        }
    }

    private void listCodecs(String type, StringBuilder info) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return;
        }
        try {
            final int allCodecsCount = MediaCodecList.getCodecCount();
            final ArrayList<Integer> decoderIndexes = new ArrayList<>();
            final ArrayList<Integer> encoderIndexes = new ArrayList<>();
            boolean first = true;
            for (int i = 0; i < allCodecsCount; ++i) {
                MediaCodecInfo codec = MediaCodecList.getCodecInfoAt(i);
                if (codec == null) {
                    continue;
                }
                String[] types = codec.getSupportedTypes();
                if (types == null) {
                    continue;
                }
                boolean found = false;
                for (int j = 0; j < types.length; ++j) {
                    if (types[j].equals(type)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    (codec.isEncoder() ? encoderIndexes : decoderIndexes).add(i);
                }
            }
            if (decoderIndexes.isEmpty() && encoderIndexes.isEmpty()) {
                return;
            }
            info.append("\n").append(decoderIndexes.size()).append("+").append(encoderIndexes.size()).append(" ").append(type.substring(6)).append(" codecs:\n");
            for (int a = 0; a < decoderIndexes.size(); ++a) {
                if (a > 0) {
                    info.append("\n");
                }
                MediaCodecInfo codec = MediaCodecList.getCodecInfoAt(decoderIndexes.get(a));
                info.append("{d} ").append(codec.getName()).append(" (");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    if (codec.isHardwareAccelerated()) {
                        info.append("gpu"); // as Gpu
                    }
                    if (codec.isSoftwareOnly()) {
                        info.append("cpu"); // as Cpu
                    }
                    if (codec.isVendor()) {
                        info.append(", v"); // as Vendor
                    }
                }
                MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(type);
                info.append("; mi=").append(capabilities.getMaxSupportedInstances()).append(")");
            }
            for (int a = 0; a < encoderIndexes.size(); ++a) {
                if (a > 0 || !decoderIndexes.isEmpty()) {
                    info.append("\n");
                }
                MediaCodecInfo codec = MediaCodecList.getCodecInfoAt(encoderIndexes.get(a));
                info.append("{e} ").append(codec.getName()).append(" (");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    if (codec.isHardwareAccelerated()) {
                        info.append("gpu"); // as Gpu
                    }
                    if (codec.isSoftwareOnly()) {
                        info.append("cpu"); // as Cpu
                    }
                    if (codec.isVendor()) {
                        info.append(", v"); // as Vendor
                    }
                }
                MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(type);
                info.append("; mi=").append(capabilities.getMaxSupportedInstances()).append(")");
            }
            info.append("\n");
        } catch (Exception ignore) {}
    }

    @Override
    public void onTransitionAnimationProgress(boolean isOpen, float progress) {
        super.onTransitionAnimationProgress(isOpen, progress);
        if (blurredView != null && blurredView.getVisibility() == View.VISIBLE) {
            if (isOpen) {
                blurredView.setAlpha(1.0f - progress);
            } else {
                blurredView.setAlpha(progress);
            }
        }
    }

    public void prepareBlurBitmap() {
        if (blurredView == null) {
            return;
        }
        int w = (int) (fragmentView.getMeasuredWidth() / 6.0f);
        int h = (int) (fragmentView.getMeasuredHeight() / 6.0f);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1.0f / 6.0f, 1.0f / 6.0f);
        fragmentView.draw(canvas);
        Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w, h) / 180));
        blurredView.setBackground(new BitmapDrawable(bitmap));
        blurredView.setAlpha(0.0f);
        blurredView.setVisibility(View.VISIBLE);
    }

    private ShowDrawable showStatusButton;
    public ShowDrawable getShowStatusButton() {
        if (showStatusButton == null) {
            showStatusButton = new ShowDrawable(LocaleController.getString(R.string.StatusHiddenShow));
            showStatusButton.setAlpha((int) (0xFF * Math.min(1f, extraHeight / AndroidUtilities.dp(88f))));
            showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }
        return showStatusButton;
    }

    public static class ShowDrawable extends Drawable implements SimpleTextView.PressableDrawable {

        public final AnimatedTextView.AnimatedTextDrawable textDrawable;
        public final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        public ShowDrawable(String string) {
            textDrawable = new AnimatedTextView.AnimatedTextDrawable();
            textDrawable.setCallback(new Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable who) {
                    if (view != null) {
                        view.invalidate();
                    }
                }
                @Override
                public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {}
                @Override
                public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {}
            });
            textDrawable.setText(string);
            textDrawable.setTextSize(dp(11));
            textDrawable.setGravity(Gravity.CENTER);
            backgroundPaint.setColor(0x1f000000);
        }

        private int textColor;
        public void setBackgroundColor(int backgroundColor) {
            if (backgroundPaint.getColor() != backgroundColor) {
                backgroundPaint.setColor(backgroundColor);
                invalidateSelf();
            }
        }
        public void setTextColor(int textColor) {
            if (this.textColor != textColor) {
                this.textColor = textColor;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final float alpha = this.alpha * this.alpha2;
            if (alpha <= 0) return;
            AndroidUtilities.rectTmp.set(getBounds());
            canvas.save();
            final float s = bounce.getScale(0.1f);
            canvas.scale(s, s, AndroidUtilities.rectTmp.centerX(), AndroidUtilities.rectTmp.centerY());
            final int wasAlpha = backgroundPaint.getAlpha();
            backgroundPaint.setAlpha((int) (wasAlpha * alpha));
            canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(20), dp(20), backgroundPaint);
            backgroundPaint.setAlpha(wasAlpha);
            textDrawable.setTextColor(textColor);
            textDrawable.setAlpha((int) (0xFF * alpha));
            textDrawable.setBounds((int) AndroidUtilities.rectTmp.left, (int) AndroidUtilities.rectTmp.top, (int) AndroidUtilities.rectTmp.right, (int) AndroidUtilities.rectTmp.bottom);
            textDrawable.draw(canvas);
            canvas.restore();
        }

        private float alpha = 1f, alpha2 = 1f;
        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha / 255f;
            invalidateSelf();
        }

        public void setAlpha2(float alpha) {
            this.alpha2 = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }

        @Override
        public int getIntrinsicWidth() {
            return (int) (textDrawable.getAnimateToWidth() + dp(11));
        }

        @Override
        public int getIntrinsicHeight() {
            return dp(17.33f);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSPARENT;
        }

        private boolean pressed;
        private final ButtonBounce bounce = new ButtonBounce(null) {
            @Override
            public void invalidate() {
                invalidateSelf();
            }
        };

        @Override
        public void setPressed(boolean pressed) {
            bounce.setPressed(pressed);
            this.pressed = pressed;
        }

        @Override
        public boolean isPressed() {
            return pressed;
        }

        private View view;
        public void setView(View view) {
            this.view = view;
        }
    }

    public void setLoadingSpan(CharacterStyle span) {
        if (loadingSpan == span) return;
        loadingSpan = span;
        AndroidUtilities.forEachViews(listView, view -> {
            if (view instanceof TextDetailCell) {
                ((TextDetailCell) view).textView.setLoading(loadingSpan);
                ((TextDetailCell) view).valueTextView.setLoading(loadingSpan);
            }
        });
    }

    private ProfileBirthdayEffect birthdayEffect;

    private void createBirthdayEffect() {
        if (fragmentView == null || !fullyVisible || birthdayFetcher == null || getContext() == null)
            return;

        if (birthdayEffect != null) {
            birthdayEffect.updateFetcher(birthdayFetcher);
            birthdayEffect.invalidate();
            return;
        }

        birthdayEffect = new ProfileBirthdayEffect(this, birthdayFetcher);
        ((FrameLayout) fragmentView).addView(birthdayEffect, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));
    }

    private void openLocation(boolean inMapsApp) {
        if (viewModel.getState().getValue().getUserInfo() == null || viewModel.getState().getValue().getUserInfo().business_location == null) return;
        if (viewModel.getState().getValue().getUserInfo().business_location.geo_point != null && !inMapsApp) {
            LocationActivity fragment = new LocationActivity(3) {
                @Override
                protected boolean disablePermissionCheck() {
                    return true;
                }
            };
            fragment.setResourceProvider(resourcesProvider);
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.local_id = -1;
            message.peer_id = getMessagesController().getPeer(viewModel.getDialogId());
            TLRPC.TL_messageMediaGeo media = new TLRPC.TL_messageMediaGeo();
            media.geo = viewModel.getState().getValue().getUserInfo().business_location.geo_point;
            media.address = viewModel.getState().getValue().getUserInfo().business_location.address;
            message.media = media;
            fragment.setSharingAllowed(false);
            fragment.setMessageObject(new MessageObject(UserConfig.selectedAccount, message, false, false));
            presentFragment(fragment);
        } else {
            String domain;
            if (BuildVars.isHuaweiStoreApp()) {
                domain = "mapapp://navigation";
            } else {
                domain = "http://maps.google.com/maps";
            }
//                    if (myLocation != null) {
//                        try {
//                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, domain + "?saddr=%f,%f&daddr=%f,%f", myLocation.getLatitude(), myLocation.getLongitude(), daddrLat, daddrLong)));
//                            getParentActivity().startActivity(intent);
//                        } catch (Exception e) {
//                            FileLog.e(e);
//                        }
//                    } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, domain + "?q=" + viewModel.getState().getValue().getUserInfo().business_location.address )));
                getParentActivity().startActivity(intent);
            } catch (Exception e) {
                FileLog.e(e);
            }
//                    }
        }

    }

    private boolean editRow(View view, int position) {
        if (!viewModel.getArgs().myProfile) return false;

        if (view instanceof ProfileChannelCell) {
            view = ((ProfileChannelCell) view).dialogCell;
        }

        TLRPC.User user = getUserConfig().getCurrentUser();
        if (user == null) return false;
        TLRPC.UserFull userFull = viewModel.getState().getValue().getUserInfo() == null ? getMessagesController().getUserFull(user.id) : viewModel.getState().getValue().getUserInfo();
        if (userFull == null) return false;

        String copyButton = getString(R.string.Copy);
        String textToCopy = null;
        if (position == channelInfoRow || position == userInfoRow || position == bioRow) {
            textToCopy = userFull.about;
        } else if (position == bizHoursRow) {
            textToCopy = OpeningHoursActivity.toString(currentAccount, user, userFull.business_work_hours);
            copyButton = getString(R.string.ProfileHoursCopy);
        } else if (position == bizLocationRow) {
            textToCopy = userFull.business_location.address;
            copyButton = getString(R.string.ProfileLocationCopy);
        } else if (position == usernameRow) {
            textToCopy = UserObject.getPublicUsername(user);
            if (textToCopy != null) textToCopy = "@" + textToCopy;
            copyButton = getString(R.string.ProfileCopyUsername);
        } else if (position == phoneRow) {
            textToCopy = user.phone;
        } else if (position == birthdayRow) {
            textToCopy = UserInfoActivity.birthdayString(viewModel.getState().getValue().getUserInfo().birthday);
        }

        ItemOptions itemOptions = ItemOptions.makeOptions((FrameLayout) fragmentView, resourcesProvider, view);
        itemOptions.setGravity(Gravity.LEFT);

        if (position == bizLocationRow && userFull.business_location != null) {
            if (userFull.business_location.geo_point != null) {
                itemOptions.add(R.drawable.msg_view_file, getString(R.string.ProfileLocationView), () -> {
                    openLocation(false);
                });
            }
            itemOptions.add(R.drawable.msg_map, getString(R.string.ProfileLocationMaps), () -> {
                openLocation(true);
            });
        }

        if (textToCopy != null) {
            final String text = textToCopy;
            itemOptions.add(R.drawable.msg_copy, copyButton, () -> {
                AndroidUtilities.addToClipboard(text);
            });
        }

        if (position == bizHoursRow) {
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileHoursEdit), () -> {
                presentFragment(new OpeningHoursActivity());
            });
            itemOptions.add(R.drawable.msg_delete, getString(R.string.ProfileHoursRemove), true, () -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.BusinessHoursClearTitle));
                builder.setMessage(LocaleController.getString(R.string.BusinessHoursClearMessage));
                builder.setPositiveButton(LocaleController.getString(R.string.Remove), (di, w) -> {
                    TL_account.updateBusinessWorkHours req = new TL_account.updateBusinessWorkHours();
                    if (userFull != null) {
                        userFull.business_work_hours = null;
                        userFull.flags2 &=~ 1;
                    }
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (err != null) {
                            BulletinFactory.showError(err);
                        } else if (res instanceof TLRPC.TL_boolFalse) {
                            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
                        }
                    }));
                    updateRowsIds();
                    listAdapter.notifyItemRemoved(position);
                    getMessagesStorage().updateUserInfo(userFull, false);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder.create());
            });
        } else if (position == bizLocationRow) {
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileLocationEdit), () -> {
                presentFragment(new org.telegram.ui.Business.LocationActivity());
            });
            itemOptions.add(R.drawable.msg_delete, getString(R.string.ProfileLocationRemove), true, () -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.BusinessLocationClearTitle));
                builder.setMessage(LocaleController.getString(R.string.BusinessLocationClearMessage));
                builder.setPositiveButton(LocaleController.getString(R.string.Remove), (di, w) -> {
                    TL_account.updateBusinessLocation req = new TL_account.updateBusinessLocation();
                    if (userFull != null) {
                        userFull.business_location = null;
                        userFull.flags2 &=~ 2;
                    }
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (err != null) {
                            BulletinFactory.showError(err);
                        } else if (res instanceof TLRPC.TL_boolFalse) {
                            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
                        }
                    }));
                    updateRowsIds();
                    listAdapter.notifyItemRemoved(position);
                    getMessagesStorage().updateUserInfo(userFull, false);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder.create());
            });
        } else if (position == usernameRow) {
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileUsernameEdit), () -> {
                presentFragment(new ChangeUsernameActivity());
            });
        } else if (position == channelInfoRow || position == userInfoRow || position == bioRow) {
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileEditBio), () -> {
                presentFragment(new UserInfoActivity());
            });
        } else if (position == phoneRow) {
            itemOptions.add(R.drawable.menu_storage_path, getString(R.string.ProfilePhoneEdit), () -> {
                presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER));
            });
        } else if (position == birthdayRow) {
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileBirthdayChange), () -> {
                showDialog(AlertsCreator.createBirthdayPickerDialog(getContext(), getString(R.string.EditProfileBirthdayTitle), getString(R.string.EditProfileBirthdayButton), userFull.birthday, birthday -> {
                    TL_account.updateBirthday req = new TL_account.updateBirthday();
                    req.flags |= 1;
                    req.birthday = birthday;
                    TL_account.TL_birthday oldBirthday = userFull != null ? userFull.birthday : null;
                    if (userFull != null) {
                        userFull.flags2 |= 32;
                        userFull.birthday = birthday;
                    }
                    getMessagesController().invalidateContentSettings();
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (res instanceof TLRPC.TL_boolTrue) {
                            BulletinFactory.of(ProfileActivity.this)
                                    .createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.PrivacyBirthdaySetDone))
                                    .setDuration(Bulletin.DURATION_PROLONG).show();
                        } else {
                            if (userFull != null) {
                                if (oldBirthday == null) {
                                    userFull.flags2 &=~ 32;
                                } else {
                                    userFull.flags2 |= 32;
                                }
                                userFull.birthday = oldBirthday;
                                getMessagesStorage().updateUserInfo(userFull, false);
                            }
                            if (err != null && err.text != null && err.text.startsWith("FLOOD_WAIT_")) {
                                if (getContext() != null) {
                                    showDialog(
                                        new AlertDialog.Builder(getContext(), resourceProvider)
                                            .setTitle(getString(R.string.PrivacyBirthdayTooOftenTitle))
                                            .setMessage(getString(R.string.PrivacyBirthdayTooOftenMessage))
                                            .setPositiveButton(getString(R.string.OK), null)
                                            .create()
                                    );
                                }
                            } else {
                                BulletinFactory.of(ProfileActivity.this)
                                    .createSimpleBulletin(R.raw.error, LocaleController.getString(R.string.UnknownError))
                                    .show();
                            }
                        }
                    }), ConnectionsManager.RequestFlagDoNotWaitFloodWait);
                }, () -> {
                    BaseFragment.BottomSheetParams params = new BaseFragment.BottomSheetParams();
                    params.transitionFromLeft = true;
                    params.allowNestedScroll = false;
                    showAsSheet(new PrivacyControlActivity(PrivacyControlActivity.PRIVACY_RULES_TYPE_BIRTHDAY), params);
                }, getResourceProvider()).create());
            });
            itemOptions.add(R.drawable.msg_delete, getString(R.string.Remove), true, () -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.BirthdayClearTitle));
                builder.setMessage(LocaleController.getString(R.string.BirthdayClearMessage));
                builder.setPositiveButton(LocaleController.getString(R.string.Remove), (di, w) -> {
                    TL_account.updateBirthday req = new TL_account.updateBirthday();
                    if (userFull != null) {
                        userFull.birthday = null;
                        userFull.flags2 &=~ 32;
                    }
                    getMessagesController().invalidateContentSettings();
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (err != null) {
                            BulletinFactory.showError(err);
                        } else if (res instanceof TLRPC.TL_boolFalse) {
                            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
                        }
                    }));
                    updateListAnimated(false);
                    getMessagesStorage().updateUserInfo(userFull, false);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder.create());
            });
        } else if (position == channelRow) {
            TLRPC.Chat channel = getMessagesController().getChat(userFull.personal_channel_id);
            if (channel != null && ChatObject.getPublicUsername(channel) != null) {
                itemOptions.add(R.drawable.msg_copy, getString(R.string.ProfileChannelCopy), () -> {
                    AndroidUtilities.addToClipboard("https://" + getMessagesController().linkPrefix + "/" + ChatObject.getPublicUsername(channel));
                });
            }
            itemOptions.add(R.drawable.msg_edit, getString(R.string.ProfileChannelChange), () -> {
                presentFragment(new UserInfoActivity());
            });
            itemOptions.add(R.drawable.msg_delete, getString(R.string.Remove), true, () -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString(R.string.ProfileChannelClearTitle));
                builder.setMessage(LocaleController.getString(R.string.ProfileChannelClearMessage));
                builder.setPositiveButton(LocaleController.getString(R.string.Remove), (di, w) -> {
                    TL_account.updatePersonalChannel req = new TL_account.updatePersonalChannel();
                    req.channel = new TLRPC.TL_inputChannelEmpty();
                    if (userFull != null) {
                        userFull.personal_channel_id = 0;
                        userFull.personal_channel_message = 0;
                        userFull.flags2 &=~ 64;
                    }
                    getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        if (err != null) {
                            BulletinFactory.showError(err);
                        } else if (res instanceof TLRPC.TL_boolFalse) {
                            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
                        }
                    }));
                    updateListAnimated(false);
                    getMessagesStorage().updateUserInfo(userFull, false);
                });
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                showDialog(builder.create());
            });
        }

        if (itemOptions.getItemsCount() <= 0) {
            return false;
        }

        itemOptions.show();

        return true;
    }

    private void updateItemsUsername() {
        if (!viewModel.getArgs().myProfile || setUsernameItem == null || linkItem == null) return;
        TLRPC.User user = getMessagesController().getUser(viewModel.getArgs().userId);
        if (user == null) {
            return;
        }
        final boolean hasUsername = UserObject.getPublicUsername(user) != null;
        setUsernameItem.setIcon(hasUsername ? R.drawable.menu_username_change : R.drawable.menu_username_set);
        setUsernameItem.setText(hasUsername ? getString(R.string.ProfileUsernameEdit) : getString(R.string.ProfileUsernameSet));
        linkItem.setVisibility(UserObject.getPublicUsername(user) != null ? View.VISIBLE : View.GONE);
    }

    private void updateEditColorIcon() {
        if (getContext() == null || editColorItem == null) return;
        if (getUserConfig().isPremium()) {
            editColorItem.setIcon(R.drawable.menu_profile_colors);
        } else {
            Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.menu_profile_colors_locked);
            icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.SRC_IN));
            Drawable lockIcon = ContextCompat.getDrawable(getContext(), R.drawable.msg_gallery_locked2);
            lockIcon.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(Color.WHITE, Color.BLACK, 0.5f), PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(icon, lockIcon, dp(1), -dp(1)) {
                @Override
                public void setColorFilter(ColorFilter colorFilter) {}
            };
            editColorItem.setIcon(combinedDrawable);
        }
    }

    public boolean hasPrivacyCommand() {
        if (!viewModel.getState().getValue().isBot()) return false;
        if (viewModel.getState().getValue().getUserInfo() == null || viewModel.getState().getValue().getUserInfo().bot_info == null) return false;
        if (viewModel.getState().getValue().getUserInfo().bot_info.privacy_policy_url != null) return true;
        for (TLRPC.TL_botCommand command : viewModel.getState().getValue().getUserInfo().bot_info.commands) {
            if ("privacy".equals(command.command)) {
                return true;
            }
        }
        return true;
    }


    private HintView2 collectibleHint;
    private int collectibleHintBackgroundColor;
    private Boolean collectibleHintVisible;
    private TLRPC.TL_emojiStatusCollectible collectibleStatus;

    public void setCollectibleGiftStatus(TLRPC.TL_emojiStatusCollectible status) {
        if (avatarContainer2 == null) return;
        if (collectibleStatus == status) return;
        if (collectibleStatus != null && status != null && collectibleStatus.collectible_id == status.collectible_id) return;
        collectibleStatus = status;
        if (collectibleHint != null) {
            collectibleHint.hide();
        }
        if (status != null && !TextUtils.isEmpty(status.slug)) {
            collectibleHintVisible = null;
            collectibleHint = new HintView2(getContext(), HintView2.DIRECTION_BOTTOM);
            collectibleHintBackgroundColor = Theme.blendOver(status.center_color | 0xFF000000, Theme.multAlpha(status.pattern_color | 0xFF000000, .5f));
            collectibleHint.setPadding(dp(4), 0, dp(4), dp(2));
            collectibleHint.setFlicker(.66f, Theme.multAlpha(status.text_color | 0xFF000000, 0.5f));
            avatarContainer2.addView(collectibleHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 24));
            collectibleHint.setTextSize(9.33f);
            collectibleHint.setTextTypeface(AndroidUtilities.bold());
            collectibleHint.setText(status.title);
            collectibleHint.setDuration(-1);
            collectibleHint.setInnerPadding(4.66f + 1, 2.66f, 4.66f + 1, 2.66f);
            collectibleHint.setArrowSize(4, 2.66f);
            collectibleHint.setRoundingWithCornerEffect(false);
            collectibleHint.setRounding(16);
            collectibleHint.show();
            final String slug = status.slug;
            collectibleHint.setOnClickListener(v -> {
                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + slug);
            });
            if (extraHeight < dp(82)) {
                collectibleHintVisible = false;
                collectibleHint.setAlpha(0.0f);
            }
            updateCollectibleHint();
            AndroidUtilities.runOnUIThread(collectibleHint::hide, 6 * 1000);
        }
    }

    public void updateCollectibleHint() {
        if (collectibleHint == null) return;
        collectibleHint.setJointPx(0, -collectibleHint.getPaddingLeft() + nameTextView[1].getX() + (nameTextView[1].getRightDrawableX() - nameTextView[1].getRightDrawableWidth() * lerp(0.45f, 0.25f, currentExpandAnimatorValue)) * nameTextView[1].getScaleX());
        final float expanded = AndroidUtilities.lerp(expandAnimatorValues, currentExpanAnimatorFracture);
        collectibleHint.setTranslationY(-collectibleHint.getPaddingBottom() + nameTextView[1].getY() - dp(24) + lerp(dp(6), -dp(12), expanded));
        collectibleHint.setBgColor(ColorUtils.blendARGB(collectibleHintBackgroundColor, 0x50000000, expanded));
        final boolean visible = extraHeight >= dp(82);
        if (collectibleHintVisible == null || collectibleHintVisible != visible) {
            collectibleHint.animate().alpha((collectibleHintVisible = visible) ? 1.0f : 0.0f).setInterpolator(CubicBezierInterpolator.EASE_OUT).setDuration(200).start();
        }
    }


    private void onStateChanged(ProfileState state) {
    }

    private void onUiEvent(ProfileUiEvent event) {
        if (event == null) return;
        ProfileState state = viewModel.getState().getValue();
        switch (event) {
            case ALERT_UNBLOCK:
                alertUnblock(state.getUser());
                break;
            case ALERT_CLEAR_OR_DELETE:
                alertClearOrDeleteDialog(
                        viewModel.getArgs().dialogId,
                        state.getUser(),
                        state.getCurrentChat(),
                        state.getCurrentEncryptedChat());
                break;
            case ALERT_BLOCK_AND_REPORT_SPAM:
                alertBlockAndReportSpam(
                        viewModel.getArgs().userId,
                        state.getUser(),
                        state.getCurrentChat(),
                        state.getCurrentEncryptedChat());
                break;
            case CREATE_BAN_BULLETIN:
                createBanBulletin();
                break;
            case UNBLOCK_BOT:
                viewModel.unblockPeer(()-> getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of("/start", viewModel.getArgs().userId, null, null, null, false, null, null, null, true, 0, null, false)));
                finishFragment();
                break;
            case HIDE_FLOATING_BUTTON:
                hideFloatingButton(!(sharedMediaLayout == null || sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_STORIES || sharedMediaLayout.getClosestTab() == SharedMediaLayout.TAB_ARCHIVED_STORIES));
                break;
            case CREATE_ACTION_BAR_MENU:
                createActionBarMenu(false);
                break;
            case CREATE_ACTION_BAR_MENU_ANIMATED:
                createActionBarMenu(true);
                break;
            case UPDATE_LIST_ANIMATED:
                updateListAnimated(false);
                break;
            case INVITE_TO_GROUP:
                inviteToGroup();
                break;
            case SHOW_UNDO_PROFILE_PHOTO_CHANGED:
                getUndoView().showWithAction(viewModel.getArgs().userId, UndoView.ACTION_PROFILE_PHOTO_CHANGED, viewModel.getState().getValue().getUpdatedProfilePhoto().video_sizes.isEmpty() ? null : 1);
                break;
            case UPDATE_PROFILE_DATA:
                updateProfileData(true);
                break;
            case UPDATE_PROFILE_DATA_NO_RELOAD:
                updateProfileData(true);
                break;
            case FINISH_SETTING_MAIN_PHOTO:
                getAvatarsViewPager().finishSettingMainPhoto();
                break;
            case REPLACE_FIRST_PHOTO:
                getAvatarsViewPager().replaceFirstPhoto(viewModel.getState().getValue().getUpdatedProfilePhoto(), viewModel.getState().getValue().getUpdatedProfilePhotoResponse().photo);
                break;
            case UPDATE_ONLINE_COUNT:
                if (sharedMediaLayout != null && sharedMediaRow != -1 && (state.getSortedUsers().size() > 5 || usersForceShowingIn == 2) && usersForceShowingIn != 1) {
                    sharedMediaLayout.setChatUsers(state.getSortedUsers(), state.getChatInfo());
                }
                break;
            case UPDATE_ONLINE_COUNT_NOTIFY:
                if (listAdapter != null && membersStartRow > 0) {
                    AndroidUtilities.updateVisibleRows(listView);
                }
                if (sharedMediaLayout != null && sharedMediaRow != -1 && (state.getSortedUsers().size() > 5 || usersForceShowingIn == 2) && usersForceShowingIn != 1) {
                    sharedMediaLayout.setChatUsers(state.getSortedUsers(), state.getChatInfo());
                }
                break;
            case SET_CHAT_INFO:
                setChatInfo();
                break;
            case SET_USER_INFO:
                setUserInfo();
                break;
        }
    }

    public void alertClearOrDeleteDialog(long dialogId, TLRPC.User user, TLRPC.Chat currentChat, TLRPC.EncryptedChat currentEncryptedChat) {
        AlertsCreator.createClearOrDeleteDialogAlert(this, false, currentChat, user, currentEncryptedChat != null, true, true, (param) -> {
            if (getParentLayout() != null) {
                List<BaseFragment> fragmentStack = getParentLayout().getFragmentStack();
                BaseFragment prevFragment = fragmentStack == null || fragmentStack.size() < 2 ? null : fragmentStack.get(fragmentStack.size() - 2);
                if (prevFragment instanceof ChatActivity) {
                    getParentLayout().removeFragmentFromStack(fragmentStack.size() - 2);
                }
            }
            finishFragment();
            viewModel.postNotificationName(NotificationCenter.needDeleteDialog, dialogId, user, currentChat, param);
        }, getResourceProvider());
    }

    public void alertBlockAndReportSpam(long userId, TLRPC.User user, TLRPC.Chat currentChat, TLRPC.EncryptedChat currentEncryptedChat) {
        AlertsCreator.showBlockReportSpamAlert(this, userId, user, null, currentEncryptedChat, false, null, param -> {
            if (param == 1) {
                viewModel.unregisterCloseChatObserver();
                playProfileAnimation = 0;
                finishFragment();
            } else {
                viewModel.postNotificationName(NotificationCenter.peerSettingsDidLoad, userId);
            }
        }, resourcesProvider);
    }

    public void alertUnblock(TLRPC.User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.BlockUser));
        builder.setMessage(AndroidUtilities.replaceTags(formatString("AreYouSureBlockContact2", R.string.AreYouSureBlockContact2, ContactsController.formatName(user.first_name, user.last_name))));
        builder.setPositiveButton(LocaleController.getString(R.string.BlockContact), (dialogInterface, i) -> {
            viewModel.blockPeer();
            if (BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.createBanBulletin(this, true).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    public void createBanBulletin() {
        if (BulletinFactory.canShowBulletin(this)) {
            BulletinFactory.createBanBulletin(this, false).show();
        }
    }

    public void inviteToGroup() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_ADD_USERS_TO);
        args.putBoolean("resetDelegate", false);
        args.putBoolean("closeFragment", false);
//                    args.putString("addToGroupAlertString", LocaleController.formatString("AddToTheGroupAlertText", R.string.AddToTheGroupAlertText, UserObject.getUserName(user), "%1$s"));
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((fragment1, dids, message, param, notify, scheduleDate, topicsFragment) -> {
            long did = dids.get(0).dialogId;

            TLRPC.Chat chat = MessagesController.getInstance(getCurrentAccount()).getChat(-did);
            if (chat != null && (chat.creator || chat.admin_rights != null && chat.admin_rights.add_admins)) {
                getMessagesController().checkIsInChat(false, chat, viewModel.getState().getValue().getUser(), (isInChatAlready, rightsAdmin, currentRank) -> AndroidUtilities.runOnUIThread(() -> {
                    ChatRightsEditActivity editRightsActivity = new ChatRightsEditActivity(viewModel.getArgs().userId, -did, rightsAdmin, null, null, currentRank, ChatRightsEditActivity.TYPE_ADD_BOT, true, !isInChatAlready, null);
                    editRightsActivity.setDelegate(new ChatRightsEditActivity.ChatRightsEditActivityDelegate() {
                        @Override
                        public void didSetRights(int rights, TLRPC.TL_chatAdminRights rightsAdmin, TLRPC.TL_chatBannedRights rightsBanned, String rank) {
                            viewModel.disableProfileAnimation(true);
                            fragment.removeSelfFromStack();
                            getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
                            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                        }

                        @Override
                        public void didChangeOwner(TLRPC.User user) {
                        }
                    });
                    presentFragment(editRightsActivity);
                }));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                builder.setTitle(LocaleController.getString(R.string.AddBot));
                String chatName = chat == null ? "" : chat.title;
                builder.setMessage(AndroidUtilities.replaceTags(formatString("AddMembersAlertNamesText", R.string.AddMembersAlertNamesText, UserObject.getUserName(viewModel.getState().getValue().getUser()), chatName)));
                builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                builder.setPositiveButton(LocaleController.getString(R.string.AddBot), (di, i) -> {
                    viewModel.disableProfileAnimation(true);

                    Bundle args1 = new Bundle();
                    args1.putBoolean("scrollToTopOnResume", true);
                    args1.putLong("chat_id", -did);
                    if (!getMessagesController().checkCanOpenChat(args1, fragment1)) {
                        return;
                    }
                    ChatActivity chatActivity = new ChatActivity(args1);
                    getNotificationCenter().removeObserver(this, NotificationCenter.closeChats);
                    getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                    getMessagesController().addUserToChat(-did, viewModel.getState().getValue().getUser(), 0, null, chatActivity, true, null, null);
                    presentFragment(chatActivity, true);
                });
                showDialog(builder.create());
            }
            return true;
        });
        presentFragment(fragment);
    }

}

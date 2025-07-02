package org.telegram.ui.Profile.view;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.AccountFrozenAlert;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.ChangeUsernameActivity;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatEditActivity;
import org.telegram.ui.ChatRightsEditActivity;
import org.telegram.ui.ChatUsersActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.MediaActivity;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.ContactAddActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Gifts.GiftSheet;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.LogoutActivity;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.Profile.ProfileActivity;
import org.telegram.ui.Profile.model.ProfileState;
import org.telegram.ui.Profile.utils.ActionBarActions;
import org.telegram.ui.Profile.viewmodel.ProfileViewModel;
import org.telegram.ui.QrActivity;
import org.telegram.ui.ReportBottomSheet;
import org.telegram.ui.StatisticActivity;
import org.telegram.ui.TopicCreateFragment;
import org.telegram.ui.UserInfoActivity;
import org.telegram.ui.bots.BotWebViewAttachedSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProfileActionBar {

    private final ProfileActivity profileActivity;
    private final Context context;
    private final ProfileViewModel viewModel;
    private final ActionBar actionBar;
    private final Theme.ResourcesProvider resourcesProvider;

    public ProfileActionBar(ProfileActivity profileActivity, Context context, ProfileViewModel viewModel, ActionBar actionBar, Theme.ResourcesProvider resourcesProvider) {
        this.profileActivity = profileActivity;
        this.context = context;
        this.viewModel = viewModel;
        this.actionBar = actionBar;
        this.resourcesProvider = resourcesProvider;

        this.setActionBarMenuOnItemClick();
    }

    private void setActionBarMenuOnItemClick() {
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                ProfileState state = viewModel.getState().getValue();
                TLRPC.UserFull userInfo = state.getUserInfo();
                TLRPC.ChatFull chatInfo = state.getChatInfo();

                if (profileActivity.getParentActivity() == null) {
                    return;
                }
                if (id == -1) {
                    profileActivity.finishFragment();
                } else if (id == ActionBarActions.block_contact) {
                    viewModel.blockContact();
                } else if (id == ActionBarActions.add_contact) {
                    TLRPC.User user = state.getUser();
                    Bundle args = new Bundle();
                    args.putLong("user_id", user.id);
                    args.putBoolean("addContact", true);
                    profileActivity.openAddToContact(user, args);
                } else if (id == ActionBarActions.share_contact) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
                    args.putString("selectAlertString", LocaleController.getString(R.string.SendContactToText));
                    args.putString("selectAlertStringGroup", LocaleController.getString(R.string.SendContactToGroupText));
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(profileActivity);
                    profileActivity.presentFragment(fragment);
                } else if (id == ActionBarActions.edit_contact) {
                    Bundle args = new Bundle();
                    args.putLong("user_id", viewModel.getArgs().userId);
                    profileActivity.presentFragment(new ContactAddActivity(args, resourcesProvider));
                } else if (id == ActionBarActions.delete_contact) {
                    final TLRPC.User user = state.getUser();
                    if (user == null || profileActivity.getParentActivity() == null) {
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(profileActivity.getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString(R.string.DeleteContact));
                    builder.setMessage(LocaleController.getString(R.string.AreYouSureDeleteContact));
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialogInterface, i) -> {
                        ArrayList<TLRPC.User> arrayList = new ArrayList<>();
                        arrayList.add(user);
                        viewModel.deleteContact(arrayList, true);
                        if (user != null) {
                            user.contact = false;
                            profileActivity.updateListAnimated(false);
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog dialog = builder.create();
                    profileActivity.showDialog(dialog);
                    TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(profileActivity.getThemedColor(Theme.key_text_RedBold));
                    }
                } else if (id == ActionBarActions.leave_group) {
                    profileActivity.leaveChatPressed();
                } else if (id == ActionBarActions.delete_topic) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(LocaleController.getPluralString("DeleteTopics", 1));
                    TLRPC.TL_forumTopic topic = MessagesController.getInstance(profileActivity.getCurrentAccount()).getTopicsController().findTopic(viewModel.getArgs().chatId, viewModel.getArgs().topicId);
                    builder.setMessage(formatString("DeleteSelectedTopic", R.string.DeleteSelectedTopic, topic == null ? "topic" : topic.title));
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                        ArrayList<Integer> topicIds = new ArrayList<>();
                        topicIds.add((int) viewModel.getArgs().topicId);
                        viewModel.deleteTopics(viewModel.getArgs().chatId, topicIds);
                        viewModel.setPlayProfileAnimation(0);
                        if (profileActivity.getParentLayout() != null && profileActivity.getParentLayout().getFragmentStack() != null) {
                            for (int i = 0; i < profileActivity.getParentLayout().getFragmentStack().size(); ++i) {
                                BaseFragment fragment = profileActivity.getParentLayout().getFragmentStack().get(i);
                                if (fragment instanceof ChatActivity && ((ChatActivity) fragment).getTopicId() == viewModel.getArgs().topicId) {
                                    fragment.removeSelfFromStack();
                                }
                            }
                        }
                        profileActivity.finishFragment();

                        Context context = profileActivity.getContext();
                        if (context != null) {
                            BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.ic_delete, LocaleController.getPluralString("TopicsDeleted", 1)).show();
                        }
                        dialog.dismiss();
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> dialog.dismiss());
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
                    }
                } else if (id == ActionBarActions.report) {
                    ReportBottomSheet.openChat(profileActivity, viewModel.getDialogId());
                } else if (id == ActionBarActions.edit_channel) {
                    if (viewModel.getArgs().isTopic) {
                        Bundle args = new Bundle();
                        args.putLong("chat_id", viewModel.getArgs().chatId);
                        TopicCreateFragment fragment = TopicCreateFragment.create(viewModel.getArgs().chatId, viewModel.getArgs().topicId);
                        profileActivity.presentFragment(fragment);
                    } else {
                        Bundle args = new Bundle();
                        if (viewModel.getArgs().chatId != 0) {
                            args.putLong("chat_id", viewModel.getArgs().chatId);
                        } else if (state.isBot()) {
                            args.putLong("user_id", viewModel.getArgs().userId);
                        }
                        ChatEditActivity fragment = new ChatEditActivity(args);
                        if (chatInfo != null) {
                            fragment.setInfo(chatInfo);
                        } else {
                            fragment.setInfo(userInfo);
                        }
                        profileActivity.presentFragment(fragment);
                    }
                } else if (id == ActionBarActions.edit_profile) {
                    profileActivity.presentFragment(new UserInfoActivity());
                } else if (id == ActionBarActions.invite_to_group) {
                    viewModel.inviteToGroup();
                } else if (id == ActionBarActions.share) {
                    try {
                        String text = null;
                        if (viewModel.getArgs().userId != 0) {
                            TLRPC.User user = state.getUser();
                            if (user == null) {
                                return;
                            }
                            if (state.getBotInfo() != null && userInfo != null && !TextUtils.isEmpty(userInfo.about)) {
                                text = String.format("%s https://" + state.getLinkPrefix() + "/%s", userInfo.about, UserObject.getPublicUsername(user));
                            } else {
                                text = String.format("https://" + state.getLinkPrefix() + "/%s", UserObject.getPublicUsername(user));
                            }
                        } else if (viewModel.getArgs().chatId != 0) {
                            TLRPC.Chat chat = state.getCurrentChat();
                            if (chat == null) {
                                return;
                            }
                            if (chatInfo != null && !TextUtils.isEmpty(chatInfo.about)) {
                                text = String.format("%s\nhttps://" + state.getLinkPrefix() + "/%s", chatInfo.about, ChatObject.getPublicUsername(chat));
                            } else {
                                text = String.format("https://" + state.getLinkPrefix() + "/%s", ChatObject.getPublicUsername(chat));
                            }
                        }
                        if (TextUtils.isEmpty(text)) {
                            return;
                        }
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, text);
                        profileActivity.startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.BotShare)), 500);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (id == ActionBarActions.add_shortcut) {
                    try {
                        long did;
                        if (state.getCurrentEncryptedChat() != null) {
                            did = DialogObject.makeEncryptedDialogId(state.getCurrentEncryptedChat().id);
                        } else if (viewModel.getArgs().userId != 0) {
                            did = viewModel.getArgs().userId;
                        } else if (viewModel.getArgs().chatId != 0) {
                            did = -viewModel.getArgs().chatId;
                        } else {
                            return;
                        }
                        viewModel.installShortcut(did, MediaDataController.SHORTCUT_TYPE_USER_OR_CHAT);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (id == ActionBarActions.call_item || id == ActionBarActions.video_call_item) {
                    if (viewModel.getArgs().userId != 0) {
                        TLRPC.User user = state.getUser();
                        if (user != null) {
                            VoIPHelper.startCall(user, id == ActionBarActions.video_call_item, userInfo != null && userInfo.video_calls_available, profileActivity.getParentActivity(), userInfo, profileActivity.getAccountInstance());
                        }
                    } else if (viewModel.getArgs().chatId != 0) {
                        ChatObject.Call call = viewModel.getGroupCall();
                        if (call == null) {
                            VoIPHelper.showGroupCallAlert(profileActivity, state.getCurrentChat(), null, false, profileActivity.getAccountInstance());
                        } else {
                            VoIPHelper.startCall(state.getCurrentChat(), null, null, false, profileActivity.getParentActivity(), profileActivity, profileActivity.getAccountInstance());
                        }
                    }
                } else if (id == ActionBarActions.search_members) {
                    Bundle args = new Bundle();
                    args.putLong("chat_id", viewModel.getArgs().chatId);
                    args.putInt("type", ChatUsersActivity.TYPE_USERS);
                    args.putBoolean("open_search", true);
                    ChatUsersActivity fragment = new ChatUsersActivity(args);
                    fragment.setInfo(chatInfo);
                    profileActivity.presentFragment(fragment);
                } else if (id == ActionBarActions.add_member) {
                    profileActivity.openAddMember();
                } else if (id == ActionBarActions.statistics) {
                    TLRPC.Chat chat = state.getCurrentChat();
                    profileActivity.presentFragment(StatisticActivity.create(chat, false));
                } else if (id == ActionBarActions.view_discussion) {
                    profileActivity.openDiscussion();
                } else if (id == ActionBarActions.gift_premium) {
                    if (userInfo != null && UserObject.areGiftsDisabled(userInfo)) {
                        BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                        if (lastFragment != null) {
                            BulletinFactory.of(lastFragment).createSimpleBulletin(R.raw.error, AndroidUtilities.replaceTags(LocaleController.formatString(R.string.UserDisallowedGifts, DialogObject.getShortName(viewModel.getDialogId())))).show();
                        }
                        return;
                    }
                    if (state.getCurrentChat() != null) {
                        MessagesController.getGlobalMainSettings().edit().putInt("channelgifthint", 3).apply();
                    }
                    profileActivity.showDialog(new GiftSheet(profileActivity.getContext(), profileActivity.getCurrentAccount(), viewModel.getDialogId(), null, null));
                } else if (id == ActionBarActions.channel_stories) {
                    Bundle args = new Bundle();
                    args.putInt("type", MediaActivity.TYPE_ARCHIVED_CHANNEL_STORIES);
                    args.putLong("dialog_id", -viewModel.getArgs().chatId);
                    MediaActivity fragment = new MediaActivity(args, null);
                    fragment.setChatInfo(chatInfo);
                    profileActivity.presentFragment(fragment);
                } else if (id == ActionBarActions.start_secret_chat) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(profileActivity.getParentActivity(), resourcesProvider);
                    builder.setTitle(LocaleController.getString(R.string.AreYouSureSecretChatTitle));
                    builder.setMessage(LocaleController.getString(R.string.AreYouSureSecretChat));
                    builder.setPositiveButton(LocaleController.getString(R.string.Start), (dialogInterface, i) -> {
                        if (MessagesController.getInstance(profileActivity.getCurrentAccount()).isFrozen()) {
                            AccountFrozenAlert.show(profileActivity.getCurrentAccount());
                            return;
                        }
                        viewModel.creatingChat(true);
                        viewModel.startSecretChat(profileActivity);
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    profileActivity.showDialog(builder.create());
                } else if (id == ActionBarActions.bot_privacy) {
                    BotWebViewAttachedSheet.openPrivacy(profileActivity.getCurrentAccount(), viewModel.getArgs().userId);
                } else if (id == ActionBarActions.gallery_menu_save) {
                    if (profileActivity.getParentActivity() == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 23 && (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && profileActivity.getParentActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        profileActivity.getParentActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                        return;
                    }
                    ImageLocation location = profileActivity.getAvatarsViewPager().getImageLocation(profileActivity.getAvatarsViewPager().getRealPosition());
                    if (location == null) {
                        return;
                    }
                    final boolean isVideo = location.imageType == FileLoader.IMAGE_TYPE_ANIMATION;
                    File f = FileLoader.getInstance(profileActivity.getCurrentAccount()).getPathToAttach(location.location, isVideo ? "mp4" : null, true);
                    if (isVideo && !f.exists()) {
                        f = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_IMAGE), FileLoader.getAttachFileName(location.location, "mp4"));
                    }
                    if (f.exists()) {
                        MediaController.saveFile(f.toString(), profileActivity.getParentActivity(), 0, null, null, uri -> {
                            if (profileActivity.getParentActivity() == null) {
                                return;
                            }
                            BulletinFactory.createSaveToGalleryBulletin(profileActivity, isVideo, null).show();
                        });
                    }
                } else if (id == ActionBarActions.edit_info) {
                    profileActivity.presentFragment(new UserInfoActivity());
                } else if (id == ActionBarActions.edit_color) {
                    if (!profileActivity.getUserConfig().isPremium()) {
                        profileActivity.showDialog(new PremiumFeatureBottomSheet(profileActivity, PremiumPreviewFragment.PREMIUM_FEATURE_NAME_COLOR, true));
                        return;
                    }
                    profileActivity.presentFragment(new PeerColorActivity(0).startOnProfile().setOnApplied(profileActivity));
                } else if (id == ActionBarActions.copy_link_profile) {
                    TLRPC.User user = state.getUser();
                    AndroidUtilities.addToClipboard(state.getLinkPrefix() + "/" + UserObject.getPublicUsername(user));
                } else if (id == ActionBarActions.set_username) {
                    profileActivity.presentFragment(new ChangeUsernameActivity());
                } else if (id == ActionBarActions.logout) {
                    profileActivity.presentFragment(new LogoutActivity());
                } else if (id == ActionBarActions.set_as_main) {
                    int position = profileActivity.getAvatarsViewPager().getRealPosition();
                    TLRPC.Photo photo = profileActivity.getAvatarsViewPager().getPhoto(position);
                    if (photo == null) {
                        return;
                    }
                    profileActivity.getAvatarsViewPager().startMovePhotoToBegin(position);

                    viewModel.setProfilePhoto(photo);

                    profileActivity.getAvatarsViewPager().commitMoveToBegin();
                } else if (id == ActionBarActions.edit_avatar) {
                    if (MessagesController.getInstance(profileActivity.getCurrentAccount()).isFrozen()) {
                        AccountFrozenAlert.show(profileActivity.getCurrentAccount());
                        return;
                    }
                    int position = profileActivity.getAvatarsViewPager().getRealPosition();
                    ImageLocation location = profileActivity.getAvatarsViewPager().getImageLocation(position);
                    if (location == null) {
                        return;
                    }

                    File f = FileLoader.getInstance(profileActivity.getCurrentAccount()).getPathToAttach(PhotoViewer.getFileLocation(location), PhotoViewer.getFileLocationExt(location), true);
                    boolean isVideo = location.imageType == FileLoader.IMAGE_TYPE_ANIMATION;
                    String thumb;
                    if (isVideo) {
                        ImageLocation imageLocation = profileActivity.getAvatarsViewPager().getRealImageLocation(position);
                        thumb = FileLoader.getInstance(profileActivity.getCurrentAccount()).getPathToAttach(PhotoViewer.getFileLocation(imageLocation), PhotoViewer.getFileLocationExt(imageLocation), true).getAbsolutePath();
                    } else {
                        thumb = null;
                    }
                    profileActivity.imageUpdater.openPhotoForEdit(f.getAbsolutePath(), thumb, 0, isVideo);
                } else if (id == ActionBarActions.delete_avatar) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(profileActivity.getParentActivity(), resourcesProvider);
                    ImageLocation location = profileActivity.getAvatarsViewPager().getImageLocation(profileActivity.getAvatarsViewPager().getRealPosition());
                    if (location == null) {
                        return;
                    }
                    if (location.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                        builder.setTitle(LocaleController.getString(R.string.AreYouSureDeleteVideoTitle));
                        builder.setMessage(getString(R.string.AreYouSureDeleteVideo));
                    } else {
                        builder.setTitle(LocaleController.getString(R.string.AreYouSureDeletePhotoTitle));
                        builder.setMessage(getString(R.string.AreYouSureDeletePhoto));
                    }
                    builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialogInterface, i) -> {
                        int position = profileActivity.getAvatarsViewPager().getRealPosition();
                        TLRPC.Photo photo = profileActivity.getAvatarsViewPager().getPhoto(position);
                        TLRPC.UserFull userFull = state.getUserInfo();
                        if (state.getAvatar() != null && position == 0) {
                            profileActivity.imageUpdater.cancel();
                            if (state.getAvatarUploadingRequest() != 0) {
                                viewModel.cancelAvatarUpload();
                            }
                            viewModel.allowPullingDown(!AndroidUtilities.isTablet() && !state.isInLandscapeMode() && profileActivity.avatarImage.getImageReceiver().hasNotThumb() && !AndroidUtilities.isAccessibilityScreenReaderEnabled());
                            viewModel.avatar(null);
                            viewModel.avatarBig(null);
                            profileActivity.getAvatarsViewPager().scrolledByUser = true;
                            profileActivity.getAvatarsViewPager().removeUploadingImage(state.getUploadingImageLocation());
                            profileActivity.getAvatarsViewPager().setCreateThumbFromParent(false);
                            profileActivity.updateProfileData(true);
                            profileActivity.showAvatarProgress(false, true);
                            viewModel.userInfoChangedNotification();
                            profileActivity.getUserConfig().saveConfig(true);
                            return;
                        }
                        if (state.isHasFallbackPhoto() && photo != null && userFull != null && userFull.fallback_photo != null && userFull.fallback_photo.id == photo.id) {
                            userFull.fallback_photo = null;
                            userFull.flags &= ~4194304;
                            viewModel.updateUserInfo(userFull);
                            profileActivity.updateProfileData(false);
                        }
                        if (profileActivity.getAvatarsViewPager().getRealCount() == 1) {
                            profileActivity.setForegroundImage(true);
                        }
                        if (photo == null || profileActivity.getAvatarsViewPager().getRealPosition() == 0) {
                            TLRPC.Photo nextPhoto = profileActivity.getAvatarsViewPager().getPhoto(1);
                            if (nextPhoto != null) {
                                profileActivity.getUserConfig().getCurrentUser().photo =new TLRPC.TL_userProfilePhoto();
                                TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(nextPhoto.sizes, 90);
                                TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(nextPhoto.sizes, 1000);
                                if (smallSize != null && bigSize != null) {
                                    profileActivity.getUserConfig().getCurrentUser().photo.photo_small = smallSize.location;
                                    profileActivity.getUserConfig().getCurrentUser().photo.photo_big = bigSize.location;
                                }
                            } else {
                                profileActivity.getUserConfig().getCurrentUser().photo = new TLRPC.TL_userProfilePhotoEmpty();
                            }
                            viewModel.deleteUserPhoto(null);
                        } else {
                            TLRPC.TL_inputPhoto inputPhoto = new TLRPC.TL_inputPhoto();
                            inputPhoto.id = photo.id;
                            inputPhoto.access_hash = photo.access_hash;
                            inputPhoto.file_reference = photo.file_reference;
                            if (inputPhoto.file_reference == null) {
                                inputPhoto.file_reference = new byte[0];
                            }
                            viewModel.deleteUserPhoto(inputPhoto);
                            viewModel.clearUserPhoto(photo.id);
                        }
                        if (profileActivity.getAvatarsViewPager().removePhotoAtIndex(position) || profileActivity.getAvatarsViewPager().getRealCount() <= 0) {
                            profileActivity.getAvatarsViewPager().setVisibility(View.GONE);
                            profileActivity.avatarImage.setForegroundAlpha(1f);
                            profileActivity.avatarContainer.setVisibility(View.VISIBLE);
                            viewModel.doNotSetForeground(true);
                            final View view = profileActivity.layoutManager.findViewByPosition(0);
                            if (view != null) {
                                profileActivity.getListView().smoothScrollBy(0, view.getTop() - AndroidUtilities.dp(88), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        }
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                    AlertDialog alertDialog = builder.create();
                    profileActivity.showDialog(alertDialog);
                    TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(profileActivity.getThemedColor(Theme.key_text_RedBold));
                    }
                } else if (id == ActionBarActions.add_photo) {
                    profileActivity.onWriteButtonClick();
                } else if (id == ActionBarActions.qr_button) {
                    if (profileActivity.qrItem != null && profileActivity.qrItem.getAlpha() > 0) {
                        Bundle args = new Bundle();
                        args.putLong("chat_id", viewModel.getArgs().chatId);
                        args.putLong("user_id", viewModel.getArgs().userId);
                        profileActivity.presentFragment(new QrActivity(args));
                    }
                }
            }
        });
    }

}

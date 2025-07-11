package org.telegram.ui.Profile.view;

import static org.telegram.ui.Profile.utils.ActionBarActions.search_members;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Profile.model.ProfileState;
import org.telegram.ui.Stars.StarsController;

import java.util.ArrayList;
import java.util.List;

public class ProfileHeaderQuickActions extends LinearLayout {

    List<org.telegram.ui.Profile.view.ProfileHeaderQuickActionsButton> quickActionsButtons = new ArrayList<>();

    public ProfileHeaderQuickActions(Context context) {
        super(context);

        setOrientation(LinearLayout.HORIZONTAL);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(100)));
        setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
    }

    public void addButton(org.telegram.ui.Profile.view.ProfileHeaderQuickActionsButton button) {
        int spacing = AndroidUtilities.dp(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
        );
        params.setMargins(spacing, 0, spacing, 0);

        quickActionsButtons.add(button);
        addView(button, params);
    }

    public void bind(ProfileState state) {
//        boolean editItemVisible = false;
//        boolean callVisible = false;
//        boolean videoCallVisible = false;
//
//        boolean hasVoiceChatItem = false;
//
//        if (state.getUser() != null) {
//            // QUICK ACTIONS
//            if (state.getUser().id != 0) {
//                if (state.getUserInfo() != null && state.getUserInfo().phone_calls_available) {
//                    callVisible = true;
//                    videoCallVisible = Build.VERSION.SDK_INT >= 18 && state.getUserInfo().video_calls_available;
//                }
//                if (state.isBot() || AccountInstance.getInstance(UserConfig.selectedAccount).getContactsController().contactsDict.get(state.getUser().id) == null) {
//                    if (MessagesController.isSupportUser(state.getUser())) {
//                        if (state.isUserBlocked()) {
//                            // QUICK ACTIONS
//                            // otherItem.addSubItem(block_contact, R.drawable.msg_block, LocaleController.getString(R.string.Unblock));
//                        }
//                    }
//                }
//            }
//
//
//            if (state.getUser().id != 0) {
//                if (state.getUser().bot && state.getUser().bot_can_edit) {
//                    editItemVisible = true;
//                }
//
//                if (state.isBot() || AccountInstance.getInstance(UserConfig.selectedAccount).getContactsController().contactsDict.get(state.getUser().id) == null) {
//                    if (MessagesController.isSupportUser(state.getUser())) {
//                        // MORE
//                        // otherItem.addSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
//                    } else if (state.getDialogId() != UserObject.VERIFY) {
//                        if (state.getCurrentEncryptedChat() == null) {
//                            // SUB LIST
//                            //  createAutoDeleteItem(context);
//                        }
//                        // otherItem.addSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
//                        if (state.isBot()) {
//                            // MORE
//                            // otherItem.addSubItem(share, R.drawable.msg_share, LocaleController.getString(R.string.BotShare));
//                        }
////                        else {
////                            // SUB LIST
////                            // otherItem.addSubItem(add_contact, R.drawable.msg_addcontact, LocaleController.getString(R.string.AddContact));
////                        }
//                        if (!TextUtils.isEmpty(state.getUser().phone)) {
//                            // MORE
//                            // otherItem.addSubItem(share_contact, R.drawable.msg_share, LocaleController.getString(R.string.ShareContact));
//                        }
//                        if (state.isBot()) {
//                            // MORE
//                            // otherItem.addSubItem(bot_privacy, R.drawable.menu_privacy_policy, getString(R.string.BotPrivacyPolicy));
//                            if (hasPrivacyCommand(state)) {
//                                // MORE
//                                // otherItem.showSubItem(bot_privacy);
//                            } else {
//                                // MORE
//                                // otherItem.hideSubItem(bot_privacy);
//                            }
//                            // SUBLIST
//                            // otherItem.addSubItem(report, R.drawable.msg_report, LocaleController.getString(R.string.ReportBot)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
//                            if (state.isUserBlocked()) {
//                                // SUB LIST
//                                // otherItem.addSubItem(block_contact, R.drawable.msg_block2, LocaleController.getString(R.string.DeleteAndBlock)).setColors(getThemedColor(Theme.key_text_RedRegular), getThemedColor(Theme.key_text_RedRegular));
//                            } else {
//                                // SUB LIST
//                                // otherItem.addSubItem(block_contact, R.drawable.msg_retry, LocaleController.getString(R.string.BotRestart));
//                            }
//                        } else {
//                            // SUB LIST
//                            // otherItem.addSubItem(block_contact, !userBlocked ? R.drawable.msg_block : R.drawable.msg_block, !userBlocked ? LocaleController.getString(R.string.BlockContact) : LocaleController.getString(R.string.Unblock));
//                        }
//                    }
//                } else {
//                    if (state.getCurrentEncryptedChat() == null) {
//                        // MORE
//                        // createAutoDeleteItem(context);
//                    }
//                    if (!TextUtils.isEmpty(state.getUser().phone)) {
//                        // MORE
//                        // otherItem.addSubItem(share_contact, R.drawable.msg_share, LocaleController.getString(R.string.ShareContact));
//                    }
//                    // MORE
//                    // otherItem.addSubItem(block_contact, !userBlocked ? R.drawable.msg_block : R.drawable.msg_block, !userBlocked ? LocaleController.getString(R.string.BlockContact) : LocaleController.getString(R.string.Unblock));
//                    // EDIT
//                    // otherItem.addSubItem(edit_contact, R.drawable.msg_edit, LocaleController.getString(R.string.EditContact));
//                    // EDIT
//                    // otherItem.addSubItem(delete_contact, R.drawable.msg_delete, LocaleController.getString(R.string.DeleteContact));
//                }
//                if (!UserObject.isDeleted(state.getUser()) && !state.isBot() && state.getCurrentEncryptedChat() == null && !state.isUserBlocked() && state.getUser().id != 333000 && state.getUser().id != 777000 && state.getUser().id != 42777) {
//                    if (!BuildVars.IS_BILLING_UNAVAILABLE && !state.getUser().self && !state.getUser().bot && !MessagesController.isSupportUser(state.getUser()) && !AccountInstance.getInstance(UserConfig.selectedAccount).getMessagesController().premiumPurchaseBlocked()) {
//                        StarsController.getInstance(UserConfig.selectedAccount).loadStarGifts();
//                        // MORE
//                        // otherItem.addSubItem(gift_premium, R.drawable.msg_gift_premium, LocaleController.getString(R.string.ProfileSendAGift));
//                    }
//                    // MORE
//                    // otherItem.addSubItem(start_secret_chat, R.drawable.msg_secret, LocaleController.getString(R.string.StartEncryptedChat));
//                    // MORE
//                    // otherItem.setSubItemShown(start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(userId)));
//                }
//                if (!state.isBot() && AccountInstance.getInstance(UserConfig.selectedAccount).getContactsController().contactsDict.get(state.getUser().id) != null) {
//                    // MORE
//                    // otherItem.addSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
//                }
//            } else if (state.getCurrentChat().id != 0) {
//                hasVoiceChatItem = false;
//
//                if (topicId == 0 && ChatObject.canChangeChatInfo(state.getCurrentChat())) {
//                    // MORE
//                    //createAutoDeleteItem(context);
//                }
//                if (ChatObject.isChannel(state.getCurrentChat())) {
//                    if (isTopic) {
//                        if (ChatObject.canManageTopic(currentAccount, chat, topicId)) {
//                            editItemVisible = true;
//                        }
//                    } else {
//                        if (ChatObject.hasAdminRights(state.getCurrentChat()) || state.getCurrentChat().megagroup && ChatObject.canChangeChatInfo(state.getCurrentChat())) {
//                            editItemVisible = true;
//                        }
//                    }
//                    if (state.getChatInfo() != null) {
//                        if (ChatObject.canManageCalls(state.getCurrentChat()) && state.getChatInfo().call == null) {
//                            //otherItem.addSubItem(call_item, R.drawable.msg_voicechat, state.getCurrentChat().megagroup && !state.getCurrentChat().gigagroup ? LocaleController.getString(R.string.StartVoipChat) : LocaleController.getString(R.string.StartVoipChannel));
//                            //hasVoiceChatItem = true;
//                        }
//                        if ((state.getChatInfo().can_view_stats || state.getChatInfo().can_view_revenue || state.getChatInfo().can_view_stars_revenue || getMessagesController().getStoriesController().canPostStories(state.getDialogId())) && topicId == 0) {
//                            //otherItem.addSubItem(statistics, R.drawable.msg_stats, LocaleController.getString(R.string.Statistics));
//                        }
//                        //ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
//                        //callItemVisible = call != null;
//                    }
//                    if (state.getCurrentChat().megagroup) {
//                        if (state.getChatInfo() == null || !state.getChatInfo().participants_hidden || ChatObject.hasAdminRights(state.getCurrentChat())) {
//                            //canSearchMembers = true;
//                            //otherItem.addSubItem(search_members, R.drawable.msg_search, LocaleController.getString(R.string.SearchMembers));
//                        }
//                        if (!state.getCurrentChat().creator && !state.getCurrentChat().left && !state.getCurrentChat().kicked && !isTopic) {
//                            //otherItem.addSubItem(leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveMegaMenu));
//                        }
//                        if (isTopic && ChatObject.canDeleteTopic(currentAccount, chat, topicId)) {
//                            //otherItem.addSubItem(delete_topic, R.drawable.msg_delete, LocaleController.getPluralString("DeleteTopics", 1));
//                        }
//                    } else {
//                        if (state.getCurrentChat().creator || state.getCurrentChat().admin_rights != null && state.getCurrentChat().admin_rights.edit_stories) {
//                            //otherItem.addSubItem(channel_stories, R.drawable.msg_archive, LocaleController.getString(R.string.OpenChannelArchiveStories));
//                        }
//                        if (ChatObject.isPublic(chat)) {
//                            //otherItem.addSubItem(share, R.drawable.msg_share, LocaleController.getString(R.string.BotShare));
//                        }
//                        if (!BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked()) {
//                            //StarsController.getInstance(currentAccount).loadStarGifts();
//                            //otherItem.addSubItem(gift_premium, R.drawable.msg_gift_premium, LocaleController.getString(R.string.ProfileSendAGiftToChannel));
//                            //otherItem.setSubItemShown(gift_premium, chatInfo != null && chatInfo.stargifts_available);
//                        }
//                        if (state.getChatInfo() != null && state.getChatInfo().linked_chat_id != 0) {
//                            //otherItem.addSubItem(view_discussion, R.drawable.msg_discussion, LocaleController.getString(R.string.ViewDiscussion));
//                        }
//                        if (!state.getCurrentChat().creator && !state.getCurrentChat().left && !state.getCurrentChat().kicked) {
//                            //otherItem.addSubItem(leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.LeaveChannelMenu));
//                        }
//                    }
//                } else {
//                    if (state.getChatInfo() != null) {
//                        if (ChatObject.canManageCalls(state.getCurrentChat()) && state.getChatInfo().call == null) {
//                            //otherItem.addSubItem(call_item, R.drawable.msg_voicechat, LocaleController.getString(R.string.StartVoipChat));
//                            hasVoiceChatItem = true;
//                        }
//                        //ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
//                        callVisible = call != null;
//                    }
//                    if (ChatObject.canChangeChatInfo(state.getCurrentChat())) {
//                        editItemVisible = true;
//                    }
//                    if (!ChatObject.isKickedFromChat(state.getCurrentChat()) && !ChatObject.isLeftFromChat(state.getCurrentChat())) {
//                        if (state.getChatInfo() == null || !state.getChatInfo().participants_hidden || ChatObject.hasAdminRights(state.getCurrentChat())) {
//                            //canSearchMembers = true;
//                            //otherItem.addSubItem(search_members, R.drawable.msg_search, LocaleController.getString(R.string.SearchMembers));
//                        }
//                    }
//                    //otherItem.addSubItem(leave_group, R.drawable.msg_leave, LocaleController.getString(R.string.DeleteAndExit));
//                }
//                if (topicId == 0) {
//                    //otherItem.addSubItem(add_shortcut, R.drawable.msg_home, LocaleController.getString(R.string.AddShortcut));
//                }
//            }
//
//            if (imageUpdater != null) {
//                //otherItem.addSubItem(set_as_main, R.drawable.msg_openprofile, LocaleController.getString(R.string.SetAsMain));
//                //otherItem.addSubItem(gallery_menu_save, R.drawable.msg_gallery, LocaleController.getString(R.string.SaveToGallery));
//                //otherItem.addSubItem(edit_avatar, R.drawable.photo_paint, LocaleController.getString(R.string.EditPhoto));
//                //otherItem.addSubItem(delete_avatar, R.drawable.msg_delete, LocaleController.getString(R.string.Delete));
//            } else {
//                //otherItem.addSubItem(gallery_menu_save, R.drawable.msg_gallery, LocaleController.getString(R.string.SaveToGallery));
//            }
//            if (getMessagesController().isChatNoForwards(currentChat)) {
//                //otherItem.hideSubItem(gallery_menu_save);
//            }
//        }
    }

    public boolean hasPrivacyCommand(ProfileState state) {
        if (!state.isBot()) return false;
        if (state.getUserInfo() == null || state.getUserInfo().bot_info == null) return false;
        if (state.getUserInfo().bot_info.privacy_policy_url != null) return true;
        for (TLRPC.TL_botCommand command : state.getUserInfo().bot_info.commands) {
            if ("privacy".equals(command.command)) {
                return true;
            }
        }
        return true;
    }
}

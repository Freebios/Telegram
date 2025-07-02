package org.telegram.ui.Profile.viewmodel;

import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatString;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.collection.LongSparseArray;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Profile.ProfileActivity;
import org.telegram.ui.Profile.data.ProfileRepository;
import org.telegram.ui.Profile.model.ProfileArgs;
import org.telegram.ui.Profile.model.ProfileState;
import org.telegram.ui.Profile.model.ProfileUiEvent;
import org.telegram.ui.Stories.StoriesController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProfileViewModel implements NotificationCenter.NotificationCenterDelegate {

    private final ProfileArgs args;
    private final ProfileRepository repository;
    private ProfileState state = new ProfileState(new ArrayList<>(), -1, 0, 0, false, false, false, 0, false, false, false, false, false, false, false, false, false, false, false,false, false, false, false, false, false, false, null, new LongSparseArray<>(), null, null, null, null, null, null, null, null, null, null, null, null);
    private MutableLiveData<ProfileState> stateLiveData = new MutableLiveData<>(state);
    private MutableLiveData<ProfileUiEvent> profileUiEventLiveData = new MutableLiveData<>(null);

    private final int classGuid;
    private final Theme.ResourcesProvider resourcesProvider;

    public ProfileViewModel(Bundle arguments, int classGuid, Theme.ResourcesProvider resourcesProvider) {
        args = new ProfileArgs(arguments);
        repository = new ProfileRepository();
        this.classGuid = classGuid;
        this.resourcesProvider = resourcesProvider;
    }

    public LiveData<ProfileState> getState() {
        return stateLiveData;
    }
    public LiveData<ProfileUiEvent> getProfileUiEventLiveData() {
        return profileUiEventLiveData;
    }

    public void setState(ProfileState state) {
        this.state = state;
        stateLiveData.postValue(state);
    }

    public void setProfileUiEvent(ProfileUiEvent profileUiEvent) {
        new Handler(Looper.getMainLooper()).post(() -> {
            profileUiEventLiveData.setValue(profileUiEvent);
        });
    }

    public ProfileArgs getArgs() {
        return args;
    }

    public boolean loadArgs() {
        if (args.userId != 0) {
            if (args.dialogId != 0) {
                setState(state.withCurrentEncryptedChat(repository.getEncryptedChat(args.dialogId)));
            }
            setState(state.withUser(repository.getMessagesController().getUser(args.userId)));
            if (state.getUser() == null) {
                return false;
            }
            repository.registerUserObserver(this);
            setState(state.withUserBlocked(repository.isUserBlocked(args.userId)));

            if (state.getUser().bot) {
                setState(state.withBot(true));
                repository.getMediaDataController().loadBotInfo(state.getUser().id, state.getUser().id, true, classGuid);
            }
            setState(state.withUserInfo(repository.getMessagesController().getUserFull(args.userId)));
            repository.loadFullUser(args.userId, classGuid, true);
            setState(state.withParticipantsMap(null));

            setState(state.withUserSelf(UserObject.isUserSelf(state.getUser())));
            setState(state.withActionBarAnimationColorFrom(args.actionBarColor));
            setProfileUiEvent(ProfileUiEvent.SET_USER_INFO);
        } else if (args.chatId != 0) {
            TLRPC.Chat currentChat = repository.waitChat(args.chatId);
            setState(state
                    .withCurrentChat(currentChat)
                    .withCurrentChatMagagroup(currentChat != null && currentChat.megagroup)
                    .withCurrentChatCreator(currentChat != null && currentChat.creator)
                    .withCurrentChatLeft(currentChat != null && currentChat.left)
                    .withCurrentChatKicked(currentChat != null && currentChat.kicked)
            );
            if (currentChat == null) {
                return false;
            }
            if (currentChat.megagroup) {
                getChannelParticipants(true);
            } else {
                setState(state.withParticipantsMap(null));
            }

            repository.registerChatObserver(this);

            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.uploadStoryEnd);
            updateOnlineCount(true);
            if (state.getChatInfo() == null) {
                setState(state.withChatInfo(repository.getMessagesController().getChatFull(args.chatId)));
            }
            if (ChatObject.isChannel(state.getCurrentChat())) {
                repository.getMessagesController().loadFullChat(args.chatId, classGuid, true);
            } else if (state.getChatInfo() == null) {
                setState(state.withChatInfo(repository.getMessagesStorage().loadChatInfo(args.chatId, false, null, false, false)));
            }
            setProfileUiEvent(ProfileUiEvent.SET_CHAT_INFO);
        } else {
            return false;
        }

        setState(state.withLinkPrefix(repository.getMessagesController().linkPrefix));

        if (args.expandPhoto) {
            setState(state.withNeedSendMessage(true));
        }
        setState(state.withExpandPhoto(args.expandPhoto));
        setPlayProfileAnimation(args.playProfileAnimation);

        repository.registerCommonObserver(this);

        if (args.preload_messages) {
            repository.getMessagesController().ensureMessagesLoaded(args.userId, 0, null);
        }

        if (args.userId != 0) {
            setState(state.withUser(repository.getMessagesController().getUser(args.userId)));

            if (state.isUserSelf()) {
                repository.getPassword((response, error) -> {
                    if (response instanceof TL_account.TL_password) {
                        setState(state.withCurrentPassword((TL_account.TL_password) response));
                    }
                });
            }
        }

        // profileState.isUserSelf ?
        if (args.userId != 0 && UserObject.isUserSelf(repository.getMessagesController().getUser(args.userId)) && !args.myProfile) {
            repository.getMessagesController().getContentSettings(null);
        }

        return true;
    }

    public void onDestroy() {
        repository.unregisterCommonObserver(this);

        if (args.userId != 0) {
            repository.unregisterUserObserver(this);
        } else if (args.chatId != 0) {
            repository.unregisterChatObserver(this);
        }
    }

    // TODO Make private
    public void getChannelParticipants(boolean reload) {
        if (state.isLoadingUsers() || state.getParticipantsMap() == null || state.getChatInfo() == null) {
            return;
        }
        setState(state.withLoadingUsers(true));
        final int delay = state.getParticipantsMap().size() != 0 && reload ? 300 : 0;

        final TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.channel = repository.getMessagesController().getInputChannel(args.chatId);
        req.filter = new TLRPC.TL_channelParticipantsRecent();
        req.offset = reload ? 0 : state.getParticipantsMap().size();
        req.limit = 200;
        int reqId = repository.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> repository.getNotificationCenter().doOnIdle(() -> {
            if (error == null) {
                LongSparseArray<TLRPC.ChatParticipant> participantsMap = state.getParticipantsMap().clone();

                TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
                repository.getMessagesController().putUsers(res.users, false);
                repository.getMessagesController().putChats(res.chats, false);
                if (res.users.size() < 200) {
                    setState(state.withUsersEndReached(true));
                }
                if (req.offset == 0) {
                    participantsMap.clear();
                    state.getChatInfo().participants = new TLRPC.TL_chatParticipants();
                    repository.getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                    repository.getMessagesStorage().updateChannelUsers(args.chatId, res.participants);
                }
                for (int a = 0; a < res.participants.size(); a++) {
                    TLRPC.TL_chatChannelParticipant participant = new TLRPC.TL_chatChannelParticipant();
                    participant.channelParticipant = res.participants.get(a);
                    participant.inviter_id = participant.channelParticipant.inviter_id;
                    participant.user_id = MessageObject.getPeerId(participant.channelParticipant.peer);
                    participant.date = participant.channelParticipant.date;
                    if (participantsMap.indexOfKey(participant.user_id) < 0) {
                        if (state.getChatInfo().participants == null) {
                            state.getChatInfo().participants = new TLRPC.TL_chatParticipants();
                        }
                        state.getChatInfo().participants.participants.add(participant);
                        participantsMap.put(participant.user_id, participant);
                    }
                }
                setState(state.withParticipantsMap(participantsMap));
            }
            setState(state.withLoadingUsers(false));
            // TODO
            //saveScrollPosition();
            //updateListAnimated(true);
        }), delay));
        repository.getConnectionsManager().bindRequestToGuid(reqId, classGuid);
    }

    // TODO : make private
    public void updateOnlineCount(boolean notify) {
        int onlineCount = 0;
        int currentTime = repository.getConnectionsManager().getCurrentTime();
        state.getSortedUsers().clear();
        if (state.getChatInfo() instanceof TLRPC.TL_chatFull || state.getChatInfo() instanceof TLRPC.TL_channelFull && state.getChatInfo().participants_count <= 200 && state.getChatInfo().participants != null) {
            final ArrayList<Integer> sortNum = new ArrayList<>();
            for (int a = 0; a < state.getChatInfo().participants.participants.size(); a++) {
                TLRPC.ChatParticipant participant = state.getChatInfo().participants.participants.get(a);
                TLRPC.User user = repository.getMessagesController().getUser(participant.user_id);
                if (user != null && user.status != null && (user.status.expires > currentTime || user.id == repository.getUserConfig().getClientUserId()) && user.status.expires > 10000) {
                    onlineCount++;
                }
                state.getSortedUsers().add(a);
                int sort = Integer.MIN_VALUE;
                if (user != null) {
                    if (user.bot) {
                        sort = -110;
                    } else if (user.self) {
                        sort = currentTime + 50000;
                    } else if (user.status != null) {
                        sort = user.status.expires;
                    }
                }
                sortNum.add(sort);
            }

            setState(state.withOnlineCount(onlineCount));

            try {
                Collections.sort(state.getSortedUsers(), Comparator.comparingInt(hs -> sortNum.get((int) hs)).reversed());
            } catch (Exception e) {
                FileLog.e(e);
            }

            if (notify) {
                setProfileUiEvent(ProfileUiEvent.UPDATE_ONLINE_COUNT_NOTIFY);
            } else {
                setProfileUiEvent(ProfileUiEvent.UPDATE_ONLINE_COUNT);
            }
        } else if (state.getChatInfo() instanceof TLRPC.TL_channelFull && state.getChatInfo().participants_count > 200) {
            setState(state.withOnlineCount(state.getChatInfo().online_count));
        }
    }


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.uploadStoryEnd || id == NotificationCenter.chatWasBoostedByUser) {
            checkCanSendStoryForPosting();
        } else if (id == NotificationCenter.updateInterfaces) {
//            int mask = (Integer) args[0];
//            boolean infoChanged = (mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0;
//            if (viewModel.getArgs().userId != 0) {
//                if (infoChanged) {
//                    updateProfileData(true);
//                }
//                if ((mask & MessagesController.UPDATE_MASK_PHONE) != 0) {
//                    if (listView != null) {
//                        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForPosition(phoneRow);
//                        if (holder != null) {
//                            listAdapter.onBindViewHolder(holder, phoneRow);
//                        }
//                    }
//                }
//            } else if (viewModel.getArgs().chatId != 0) {
//                if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0 || (mask & MessagesController.UPDATE_MASK_CHAT_MEMBERS) != 0 || (mask & MessagesController.UPDATE_MASK_STATUS) != 0 || (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0) {
//                    if ((mask & MessagesController.UPDATE_MASK_CHAT) != 0) {
//                        updateListAnimated(true);
//                    } else {
//                        viewModel.updateOnlineCount(true);
//                    }
//                    updateProfileData(true);
//                }
//                if (infoChanged) {
//                    if (listView != null) {
//                        int count = listView.getChildCount();
//                        for (int a = 0; a < count; a++) {
//                            View child = listView.getChildAt(a);
//                            if (child instanceof UserCell) {
//                                ((UserCell) child).update(mask);
//                            }
//                        }
//                    }
//                }
//            }
        } else if (id == NotificationCenter.chatOnlineCountDidLoad) {
            Long chatId = (Long) args[0];
            if (state.getChatInfo() == null || state.getCurrentChat() == null || state.getCurrentChat().id != chatId) {
                return;
            }
            state.getChatInfo().online_count = (Integer) args[1];
            updateOnlineCount(true);
            setProfileUiEvent(ProfileUiEvent.UPDATE_PROFILE_DATA_NO_RELOAD);
        } else if (id == NotificationCenter.contactsDidLoad || id == NotificationCenter.channelRightsUpdated) {
//            createActionBarMenu(true);
        } else if (id == NotificationCenter.encryptedChatCreated) {
//            if (creatingChat) {
//                AndroidUtilities.runOnUIThread(() -> {
//                    getNotificationCenter().removeObserver(ProfileActivity.this, NotificationCenter.closeChats);
//                    getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
//                    TLRPC.EncryptedChat encryptedChat = (TLRPC.EncryptedChat) args[0];
//                    Bundle args2 = new Bundle();
//                    args2.putInt("enc_id", encryptedChat.id);
//                    presentFragment(new ChatActivity(args2), true);
//                });
//            }
        } else if (id == NotificationCenter.encryptedChatUpdated) {
//            TLRPC.EncryptedChat chat = (TLRPC.EncryptedChat) args[0];
//            if (viewModel.getState().getValue().getCurrentEncryptedChat() != null && chat.id == viewModel.getState().getValue().getCurrentEncryptedChat().id) {
//                // TODO : fix
//                //viewModel.getState().getValue().getCurrentEncryptedChat() = chat;
//                updateListAnimated(false);
//                if (flagSecure != null) {
//                    flagSecure.invalidate();
//                }
//            }
        } else if (id == NotificationCenter.blockedUsersDidLoad) {
            boolean oldValue = state.isUserBlocked();
            boolean userBlocked = repository.getMessagesController().blockePeers.indexOfKey(this.args.userId) >= 0;
            if (oldValue != userBlocked) {
                setState(state.withUserBlocked(userBlocked));
                setProfileUiEvent(ProfileUiEvent.CREATE_ACTION_BAR_MENU_ANIMATED);
                setProfileUiEvent(ProfileUiEvent.UPDATE_LIST_ANIMATED);
            }
        } else if (id == NotificationCenter.groupCallUpdated) {
//            Long chatId = (Long) args[0];
//            if (viewModel.getState().getValue().getCurrentChat() != null && chatId == viewModel.getState().getValue().getCurrentChat().id && ChatObject.canManageCalls(viewModel.getState().getValue().getCurrentChat())) {
//                TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(chatId);
//                if (chatFull != null) {
//                    if (chatInfo != null) {
//                        chatFull.participants = chatInfo.participants;
//                    }
//                    chatInfo = chatFull;
//                }
//                if (sharedMediaLayout != null) {
//                    sharedMediaLayout.setChatInfo(chatInfo);
//                }
//                if (chatInfo != null && (chatInfo.call == null && !hasVoiceChatItem || chatInfo.call != null && hasVoiceChatItem)) {
//                    createActionBarMenu(false);
//                }
//                if (storyView != null && chatInfo != null) {
//                    storyView.setStories(chatInfo.stories);
//                }
//                if (giftsView != null) {
//                    giftsView.update();
//                }
//                if (avatarImage != null) {
//                    avatarImage.setHasStories(needInsetForStories());
//                }
//                if (chatId != 0) {
//                    otherItem.setSubItemShown(gift_premium, !BuildVars.IS_BILLING_UNAVAILABLE && !getMessagesController().premiumPurchaseBlocked() && chatInfo != null && chatInfo.stargifts_available);
//                }
//            }
        } else if (id == NotificationCenter.chatInfoDidLoad) {
//            final TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
//            if (chatFull.id == viewModel.getArgs().chatId) {
//                final boolean byChannelUsers = (Boolean) args[2];
//                if (chatInfo instanceof TLRPC.TL_channelFull) {
//                    if (chatFull.participants == null) {
//                        chatFull.participants = chatInfo.participants;
//                    }
//                }
//                final boolean loadChannelParticipants = chatInfo == null && chatFull instanceof TLRPC.TL_channelFull;
//                chatInfo = chatFull;
//                if (mergeDialogId == 0 && chatInfo.migrated_from_chat_id != 0) {
//                    mergeDialogId = -chatInfo.migrated_from_chat_id;
//                    getMediaDataController().getMediaCount(mergeDialogId, viewModel.getArgs().topicId, MediaDataController.MEDIA_PHOTOVIDEO, classGuid, true);
//                }
//                fetchUsersFromChannelInfo();
//                if (avatarsViewPager != null && !viewModel.getArgs().isTopic) {
//                    avatarsViewPager.setChatInfo(chatInfo);
//                }
//                updateListAnimated(true);
//                TLRPC.Chat newChat = getMessagesController().getChat(viewModel.getArgs().chatId);
//                if (newChat != null) {
//                    //TODO fix
//                    //viewModel.getState().getValue().getCurrentChat() = newChat;
//                    createActionBarMenu(true);
//                }
//                if (flagSecure != null) {
//                    flagSecure.invalidate();
//                }
//                if (viewModel.getState().getValue().getCurrentChat().megagroup && (loadChannelParticipants || !byChannelUsers)) {
//                    viewModel.getChannelParticipants(true);
//                }
//
//                updateAutoDeleteItem();
//                updateTtlIcon();
//                if (storyView != null && chatInfo != null) {
//                    storyView.setStories(chatInfo.stories);
//                }
//                if (giftsView != null) {
//                    giftsView.update();
//                }
//                if (avatarImage != null) {
//                    avatarImage.setHasStories(needInsetForStories());
//                }
//                if (sharedMediaLayout != null) {
//                    sharedMediaLayout.setChatInfo(chatInfo);
//                }
//            }
        } else if (id == NotificationCenter.closeChats) {
//            removeSelfFromStack(true);
        } else if (id == NotificationCenter.botInfoDidLoad) {
            final TL_bots.BotInfo info = (TL_bots.BotInfo) args[0];
            if (info.user_id == this.args.userId) {
                setState(state.withBotInfo(info));
                setProfileUiEvent(ProfileUiEvent.UPDATE_LIST_ANIMATED);
            }
        } else if (id == NotificationCenter.userInfoDidLoad) {
//            final long uid = (Long) args[0];
//            if (uid == viewModel.getArgs().userId) {
//                userInfo = (TLRPC.UserFull) args[1];
//                if (storyView != null) {
//                    storyView.setStories(userInfo.stories);
//                }
//                if (giftsView != null) {
//                    giftsView.update();
//                }
//                if (avatarImage != null) {
//                    avatarImage.setHasStories(needInsetForStories());
//                }
//                if (sharedMediaLayout != null) {
//                    sharedMediaLayout.setUserInfo(userInfo);
//                }
//                if (imageUpdater != null) {
//                    if (listAdapter != null && !TextUtils.equals(userInfo.about, currentBio)) {
//                        listAdapter.notifyItemChanged(bioRow);
//                    }
//                } else {
//                    if (!openAnimationInProgress && !callItemVisible) {
//                        createActionBarMenu(true);
//                    } else {
//                        recreateMenuAfterAnimation = true;
//                    }
//                    updateListAnimated(false);
//                    if (sharedMediaLayout != null) {
//                        sharedMediaLayout.setCommonGroupsCount(userInfo.common_chats_count);
//                        updateSelectedMediaTabText();
//                        if (sharedMediaPreloader == null || sharedMediaPreloader.isMediaWasLoaded()) {
//                            resumeDelayedFragmentAnimation();
//                            needLayout(true);
//                        }
//                    }
//                }
//                updateAutoDeleteItem();
//                updateTtlIcon();
//                if (profileChannelMessageFetcher == null && !isSettings()) {
//                    profileChannelMessageFetcher = new ProfileChannelCell.ChannelMessageFetcher(currentAccount);
//                    profileChannelMessageFetcher.subscribe(() -> updateListAnimated(false));
//                    profileChannelMessageFetcher.fetch(userInfo);
//                }
//                if (!isSettings()) {
//                    ProfileBirthdayEffect.BirthdayEffectFetcher oldFetcher = birthdayFetcher;
//                    birthdayFetcher = ProfileBirthdayEffect.BirthdayEffectFetcher.of(currentAccount, userInfo, birthdayFetcher);
//                    createdBirthdayFetcher = birthdayFetcher != oldFetcher;
//                    if (birthdayFetcher != null) {
//                        birthdayFetcher.subscribe(this::createBirthdayEffect);
//                    }
//                }
//                if (otherItem != null) {
//                    if (hasPrivacyCommand()) {
//                        otherItem.showSubItem(bot_privacy);
//                    } else {
//                        otherItem.hideSubItem(bot_privacy);
//                    }
//                }
//            }
        } else if (id == NotificationCenter.privacyRulesUpdated) {
//            if (qrItem != null) {
//                updateQrItemVisibility(true);
//            }
        } else if (id == NotificationCenter.didReceiveNewMessages) {
//            final boolean scheduled = (Boolean) args[2];
//            if (scheduled) {
//                return;
//            }
//            final long did = getDialogId();
//            if (did == (Long) args[0]) {
//                boolean enc = DialogObject.isEncryptedDialog(did);
//                ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
//                for (int a = 0; a < arr.size(); a++) {
//                    MessageObject obj = arr.get(a);
//                    if (viewModel.getState().getValue().getCurrentEncryptedChat() != null && obj.messageOwner.action instanceof TLRPC.TL_messageEncryptedAction && obj.messageOwner.action.encryptedAction instanceof TLRPC.TL_decryptedMessageActionSetMessageTTL) {
//                        TLRPC.TL_decryptedMessageActionSetMessageTTL action = (TLRPC.TL_decryptedMessageActionSetMessageTTL) obj.messageOwner.action.encryptedAction;
//                        if (listAdapter != null) {
//                            listAdapter.notifyDataSetChanged();
//                        }
//                    }
//                }
//            }
        } else if (id == NotificationCenter.emojiLoaded) {
//            if (listView != null) {
//                listView.invalidateViews();
//            }
        } else if (id == NotificationCenter.reloadInterface) {
//            updateListAnimated(false);
        } else if (id == NotificationCenter.newSuggestionsAvailable) {
//            final int prevRow1 = passwordSuggestionRow;
//            final int prevRow2 = phoneSuggestionRow;
//            final int prevRow3 = graceSuggestionRow;
//            updateRowsIds();
//            if (prevRow1 != passwordSuggestionRow || prevRow2 != phoneSuggestionRow || prevRow3 != graceSuggestionRow) {
//                listAdapter.notifyDataSetChanged();
//            }
        } else if (id == NotificationCenter.topicsDidLoaded) {
            if (this.args.isTopic) {
                setProfileUiEvent(ProfileUiEvent.UPDATE_PROFILE_DATA_NO_RELOAD);
            }
        } else if (id == NotificationCenter.updateSearchSettings) {
//            if (searchAdapter != null) {
//                searchAdapter.searchArray = searchAdapter.onCreateSearchArray();
//                searchAdapter.recentSearches.clear();
//                searchAdapter.updateSearchArray();
//                searchAdapter.search(searchAdapter.lastSearchString);
//            }
        } else if (id == NotificationCenter.reloadDialogPhotos) {
            setProfileUiEvent(ProfileUiEvent.UPDATE_PROFILE_DATA_NO_RELOAD);
        } else if (id == NotificationCenter.storiesUpdated || id == NotificationCenter.storiesReadUpdated) {
//            if (avatarImage != null) {
//                avatarImage.setHasStories(needInsetForStories());
//                updateAvatarRoundRadius();
//            }
//            if (storyView != null) {
//                if (userInfo != null) {
//                    storyView.setStories(userInfo.stories);
//                } else if (chatInfo != null) {
//                    storyView.setStories(chatInfo.stories);
//                }
//            }
        } else if (id == NotificationCenter.userIsPremiumBlockedUpadted) {
//            if (otherItem != null) {
//                otherItem.setSubItemShown(start_secret_chat, DialogObject.isEmpty(getMessagesController().isUserContactBlocked(viewModel.getArgs().userId)));
//            }
//            updateEditColorIcon();
        } else if (id == NotificationCenter.currentUserPremiumStatusChanged) {
//            updateEditColorIcon();
        } else if (id == NotificationCenter.starBalanceUpdated) {
            setProfileUiEvent(ProfileUiEvent.UPDATE_LIST_ANIMATED);
        } else if (id == NotificationCenter.botStarsUpdated) {
            setProfileUiEvent(ProfileUiEvent.UPDATE_LIST_ANIMATED);
        } else if (id == NotificationCenter.botStarsTransactionsLoaded) {
            setProfileUiEvent(ProfileUiEvent.UPDATE_LIST_ANIMATED);
        } else if (id == NotificationCenter.dialogDeleted) {
//            final long dialogId = (long) args[0];
//            if (getDialogId() == dialogId) {
//                if (parentLayout != null && parentLayout.getLastFragment() == this) {
//                    finishFragment();
//                } else {
//                    removeSelfFromStack();
//                }
//            }
        } else if (id == NotificationCenter.channelRecommendationsLoaded) {
//            final long dialogId = (long) args[0];
//            if (sharedMediaRow < 0 && dialogId == getDialogId()) {
//                updateRowsIds();
//                updateSelectedMediaTabText();
//                if (listAdapter != null) {
//                    listAdapter.notifyDataSetChanged();
//                }
//            }
        } else if (id == NotificationCenter.starUserGiftsLoaded) {
//            final long dialogId = (long) args[0];
//            if (dialogId == getDialogId() && !isSettings()) {
//                if (sharedMediaRow < 0) {
//                    updateRowsIds();
//                    updateSelectedMediaTabText();
//                    if (listAdapter != null) {
//                        listAdapter.notifyDataSetChanged();
//                    }
//                    AndroidUtilities.runOnUIThread(() -> {
//                        if (sharedMediaLayout != null) {
//                            sharedMediaLayout.updateTabs(true);
//                            sharedMediaLayout.updateAdapters();
//                        }
//                    });
//                } else if (sharedMediaLayout != null) {
//                    sharedMediaLayout.updateTabs(true);
//                }
//            }
        }
    }

    public long getDialogId() {
        if (args.dialogId != 0) {
            return args.dialogId;
        } else if (args.userId != 0) {
            return args.userId;
        } else {
            return -args.chatId;
        }
    }

    // TODO make private
    public void checkCanSendStoryForPosting() {
        TLRPC.Chat chat = MessagesController.getInstance(UserConfig.selectedAccount).getChat(args.chatId);
        if (!ChatObject.isBoostSupported(chat)) {
            return;
        }
        StoriesController storiesController = repository.getMessagesController().getStoriesController();
        setState(state.withWaitCanSendStoryRequest(true));
        storiesController.canSendStoryFor(getDialogId(), canSend -> {
            setState(state
                    .withWaitCanSendStoryRequest(false)
                    .withShowBoostsAlert(!canSend));
            setProfileUiEvent(ProfileUiEvent.HIDE_FLOATING_BUTTON);
        }, false, resourcesProvider);
    }

    public void updateUserInfo(TLRPC.UserFull userFull) {
        repository.getMessagesStorage().updateUserInfo(userFull, true);

    }

    public void deleteContact(ArrayList<TLRPC.User> contacts, boolean showBulletin) {
        repository.getContactsController().deleteContact(contacts, showBulletin);
    }

    public boolean blockContact() {
        if (state.getUser() == null) {
            return false;
        }
        if (!state.isBot() || MessagesController.isSupportUser(state.getUser())) {
            if (state.isUserBlocked()) {
                repository.getMessagesController().unblockPeer(args.userId);
                setProfileUiEvent(ProfileUiEvent.CREATE_BAN_BULLETIN);
            } else {
                if (args.reportSpam) {
                    setProfileUiEvent(ProfileUiEvent.ALERT_BLOCK_AND_REPORT_SPAM);
                } else {
                    setProfileUiEvent(ProfileUiEvent.ALERT_UNBLOCK);
                }
            }
        } else {
            if (!state.isUserBlocked()) {
                setProfileUiEvent(ProfileUiEvent.ALERT_CLEAR_OR_DELETE);
            } else {
                setProfileUiEvent(ProfileUiEvent.UNBLOCK_BOT);
            }
        }
        return true;
    }

    public void unblockPeer(Runnable callback) {
        repository.getMessagesController().unblockPeer(args.userId, callback);
    }

    public void blockPeer() {
        repository.getMessagesController().blockPeer(args.userId);
    }

    public void deleteTopics(long chatId, ArrayList<Integer> topicIds) {
        repository.getMessagesController().getTopicsController().deleteTopics(chatId, topicIds);
    }

    public void postNotificationName(int id, Object... args) {
        repository.getNotificationCenter().postNotificationName(id, args);
    }

    public void unregisterCloseChatObserver() {
        repository.unRegisterCloseChatObserver(this);
    }

    public void deleteUserPhoto(TLRPC.TL_inputPhoto photo) {
        repository.getMessagesController().deleteUserPhoto(photo);
    }

    public void clearUserPhoto(long photoId) {
        repository.getMessagesStorage().clearUserPhoto(args.userId, photoId);
    }

    public void setPlayProfileAnimation(int type) {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        if (!AndroidUtilities.isTablet()) {
            setState(state.withNeedTimerImage(type != 0)
                    .withNeedStarImage(type != 0));
            if (preferences.getBoolean("view_animations", true)) {
                setState(state.withPlayProfileAnimation(type));
            } else if (type == 2) {
                setState(state.withExpandPhoto(true));
            }
        }
    }

    public void inviteToGroup() {
        final TLRPC.User user = state.getUser();
        if (user == null) {
            return;
        }
        setProfileUiEvent(ProfileUiEvent.INVITE_TO_GROUP);
    }

    public void installShortcut(long did, int shortcutTypeUserOrChat) {
        repository.getMediaDataController().installShortcut(did, shortcutTypeUserOrChat);
    }

    public ChatObject.Call getGroupCall() {
        return repository.getMessagesController().getGroupCall(args.chatId, false);
    }

    public void setProfilePhoto(TLRPC.Photo photo) {
        setState(state.withUpdatedProfilePhoto(photo));
        TLRPC.TL_photos_updateProfilePhoto req = new TLRPC.TL_photos_updateProfilePhoto();
        req.id = new TLRPC.TL_inputPhoto();
        req.id.id = photo.id;
        req.id.access_hash = photo.access_hash;
        req.id.file_reference = photo.file_reference;
        UserConfig userConfig = AccountInstance.getInstance(UserConfig.selectedAccount).getUserConfig();
        repository.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            setProfileUiEvent(ProfileUiEvent.FINISH_SETTING_MAIN_PHOTO);
            if (response instanceof TLRPC.TL_photos_photo) {
                TLRPC.TL_photos_photo photos_photo = (TLRPC.TL_photos_photo) response;
                repository.getMessagesController().putUsers(photos_photo.users, false);
                TLRPC.User user = repository.getMessagesController().getUser(userConfig.clientUserId);
                if (photos_photo.photo instanceof TLRPC.TL_photo) {
                    setState(state.withUpdatedProfilePhotoResponse(photos_photo));
                    setProfileUiEvent(ProfileUiEvent.REPLACE_FIRST_PHOTO);
                    if (user != null) {
                        user.photo.photo_id = photos_photo.photo.id;
                        userConfig.setCurrentUser(user);
                        userConfig.saveConfig(true);
                    }
                }
            }
        }));
        setProfileUiEvent(ProfileUiEvent.SHOW_UNDO_PROFILE_PHOTO_CHANGED);
        TLRPC.User user = state.getUser();

        TLRPC.PhotoSize bigSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 800);
        if (user != null) {
            TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 90);
            user.photo.photo_id = photo.id;
            user.photo.photo_small = smallSize.location;
            user.photo.photo_big = bigSize.location;
            userConfig.setCurrentUser(user);
            userConfig.saveConfig(true);
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            setProfileUiEvent(ProfileUiEvent.UPDATE_PROFILE_DATA);
        }
    }

    public void startSecretChat(ProfileActivity profileActivity) {
        repository.getSecretChatHelper().startSecretChat(profileActivity.getParentActivity(), state.getUser());
    }

    public void cancelAvatarUpload() {
        repository.getConnectionsManager().cancelRequest(state.getAvatarUploadingRequest(), true);
    }

    /*
    TODO delete when update logic has been migrated
     */
    public void disableProfileAnimation(boolean b) {
        setState(state.withDisableProfileAnimation(b));
    }

    public void doNotSetForeground(boolean b) {
        setState(state.withDoNotSetForeground(b));
    }

    public void allowPullingDown(boolean b) {
        setState(state.withAllowPullingDown(b));
    }

    public void hasFallbackPhoto(boolean b) {
        setState(state.withHasFallbackPhoto(b));
    }

    public void isInLandscapeMode(boolean b) {
        setState(state.withInLandscapeMode(b));
    }

    public void uploadingImageLocation(ImageLocation location) {
        setState(state.withUploadingImageLocation(location));
    }

    public void avatar(TLRPC.FileLocation location) {
        setState(state.withAvatar(location));
    }

    public void avatarBig(TLRPC.FileLocation location) {
        setState(state.withAvatarBig(location));
    }

    public void creatingChat(boolean b) {
        setState(state.withCreatingChat(b));
    }

    public void avatarUploadingRequest(int requestId) {
        setState(state.withAvatarUploadingRequest(requestId));
    }

    public void chatInfo(TLRPC.ChatFull chatInfo) {
        setState(state.withChatInfo(chatInfo));
    }

    public void userInfo(TLRPC.UserFull userInfo) {
        setState(state.withUserInfo(userInfo));
    }

    public void userInfoChangedNotification() {
        repository.getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        repository.getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
    }
}

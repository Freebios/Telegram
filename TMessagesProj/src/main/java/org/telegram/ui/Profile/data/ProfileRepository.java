package org.telegram.ui.Profile.data;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SecretChatHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.concurrent.CountDownLatch;

import lombok.Getter;

public class ProfileRepository {

    private final AccountInstance accountInstance;
    @Getter private final ConnectionsManager connectionsManager;
    @Getter private final MessagesController messagesController;
    @Getter private final MessagesStorage messagesStorage;
    @Getter private final NotificationCenter notificationCenter;
    @Getter private final MediaDataController mediaDataController;
    @Getter private final ContactsController contactsController;
    @Getter private final SecretChatHelper secretChatHelper;

    public ProfileRepository() {
        int currentAccount = UserConfig.selectedAccount;
        accountInstance = AccountInstance.getInstance(currentAccount);

        connectionsManager = accountInstance.getConnectionsManager();
        mediaDataController = accountInstance.getMediaDataController();
        messagesController = accountInstance.getMessagesController();
        messagesStorage = accountInstance.getMessagesStorage();
        notificationCenter = accountInstance.getNotificationCenter();
        contactsController = accountInstance.getContactsController();
        secretChatHelper = accountInstance.getSecretChatHelper();
    }

    public UserConfig getUserConfig() {
        return accountInstance.getUserConfig();
    }

    public TLRPC.EncryptedChat getEncryptedChat(long dialogId) {
        return messagesController.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
    }

    public void loadFullUser(long userId, int classGuid, boolean force) {
        messagesController.loadFullUser(messagesController.getUser(userId), classGuid, force);
    }

    public void getPassword(RequestDelegate completionBlock) {
        TL_account.getPassword req = new TL_account.getPassword();
        connectionsManager.sendRequest(req, completionBlock);
    }

    public TLRPC.Chat waitChat(long chatId) {
        final TLRPC.Chat[] currentChat = {messagesController.getChat(chatId)};
        if (currentChat[0] == null) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            messagesStorage.getStorageQueue().postRunnable(() -> {
                currentChat[0] = messagesController.getChat(chatId);
                countDownLatch.countDown();
            });
            try {
                countDownLatch.await();
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (currentChat[0] != null) {
                messagesController.putChat(currentChat[0], true);
            } else {
                return null;
            }
        }
        return currentChat[0];
    }

    public boolean isUserBlocked(long userId) {
        return messagesController.blockePeers.indexOfKey(userId) >= 0;
    }

    public void registerChatObserver(NotificationCenter.NotificationCenterDelegate observer) {
        notificationCenter.addObserver(observer, NotificationCenter.chatInfoDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.chatOnlineCountDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.groupCallUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.channelRightsUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.chatWasBoostedByUser);
        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.uploadStoryEnd);
    }

    public void unregisterChatObserver(NotificationCenter.NotificationCenterDelegate observer) {
        NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.uploadStoryEnd);
        getNotificationCenter().removeObserver(observer, NotificationCenter.chatWasBoostedByUser);
        getNotificationCenter().removeObserver(observer, NotificationCenter.chatInfoDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.chatOnlineCountDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.groupCallUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.channelRightsUpdated);
    }

    public void registerUserObserver(NotificationCenter.NotificationCenterDelegate observer) {
        notificationCenter.addObserver(observer, NotificationCenter.contactsDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.newSuggestionsAvailable);
        notificationCenter.addObserver(observer, NotificationCenter.encryptedChatCreated);
        notificationCenter.addObserver(observer, NotificationCenter.encryptedChatUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.blockedUsersDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.botInfoDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.userInfoDidLoad);
        notificationCenter.addObserver(observer, NotificationCenter.privacyRulesUpdated);
        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.reloadInterface);
    }

    public void unregisterUserObserver(NotificationCenter.NotificationCenterDelegate observer) {
        getNotificationCenter().removeObserver(observer, NotificationCenter.newSuggestionsAvailable);
        getNotificationCenter().removeObserver(observer, NotificationCenter.contactsDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.encryptedChatCreated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.encryptedChatUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.blockedUsersDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.botInfoDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.userInfoDidLoad);
        getNotificationCenter().removeObserver(observer, NotificationCenter.privacyRulesUpdated);
        NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.reloadInterface);
    }

    public void registerCommonObserver(NotificationCenter.NotificationCenterDelegate observer) {
        notificationCenter.addObserver(observer, NotificationCenter.updateInterfaces);
        notificationCenter.addObserver(observer, NotificationCenter.didReceiveNewMessages);
        notificationCenter.addObserver(observer, NotificationCenter.closeChats);
        notificationCenter.addObserver(observer, NotificationCenter.topicsDidLoaded);
        notificationCenter.addObserver(observer, NotificationCenter.updateSearchSettings);
        notificationCenter.addObserver(observer, NotificationCenter.reloadDialogPhotos);
        notificationCenter.addObserver(observer, NotificationCenter.storiesUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.storiesReadUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.userIsPremiumBlockedUpadted);
        notificationCenter.addObserver(observer, NotificationCenter.currentUserPremiumStatusChanged);
        notificationCenter.addObserver(observer, NotificationCenter.starBalanceUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.botStarsUpdated);
        notificationCenter.addObserver(observer, NotificationCenter.botStarsTransactionsLoaded);
        notificationCenter.addObserver(observer, NotificationCenter.dialogDeleted);
        notificationCenter.addObserver(observer, NotificationCenter.channelRecommendationsLoaded);
        notificationCenter.addObserver(observer, NotificationCenter.starUserGiftsLoaded);
        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.emojiLoaded);
    }

    public void unregisterCommonObserver(NotificationCenter.NotificationCenterDelegate observer) {
        getNotificationCenter().removeObserver(observer, NotificationCenter.updateInterfaces);
        getNotificationCenter().removeObserver(observer, NotificationCenter.closeChats);
        getNotificationCenter().removeObserver(observer, NotificationCenter.didReceiveNewMessages);
        getNotificationCenter().removeObserver(observer, NotificationCenter.topicsDidLoaded);
        getNotificationCenter().removeObserver(observer, NotificationCenter.updateSearchSettings);
        getNotificationCenter().removeObserver(observer, NotificationCenter.reloadDialogPhotos);
        getNotificationCenter().removeObserver(observer, NotificationCenter.storiesUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.storiesReadUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.userIsPremiumBlockedUpadted);
        getNotificationCenter().removeObserver(observer, NotificationCenter.currentUserPremiumStatusChanged);
        getNotificationCenter().removeObserver(observer, NotificationCenter.starBalanceUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.botStarsUpdated);
        getNotificationCenter().removeObserver(observer, NotificationCenter.botStarsTransactionsLoaded);
        getNotificationCenter().removeObserver(observer, NotificationCenter.dialogDeleted);
        getNotificationCenter().removeObserver(observer, NotificationCenter.channelRecommendationsLoaded);
        getNotificationCenter().removeObserver(observer, NotificationCenter.starUserGiftsLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.emojiLoaded);
    }

    public void unRegisterCloseChatObserver(NotificationCenter.NotificationCenterDelegate observer) {
        getNotificationCenter().removeObserver(observer, NotificationCenter.closeChats);
        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
    }

}

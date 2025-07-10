package org.telegram.ui.Profile.model;

import androidx.collection.LongSparseArray;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_bots;

import java.util.ArrayList;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.With;

@Value
public class ProfileState {
    @With ArrayList<Integer> sortedUsers;
    @With int onlineCount;
    @With int playProfileAnimation;
    @With int avatarUploadingRequest;

    @With Long dialogId;

    @With boolean isUserBlocked;
    @With boolean isBot;
    @With boolean isUserSelf;
    @With int actionBarAnimationColorFrom;
    @With boolean loadingUsers;
    @With boolean usersEndReached;
    @With boolean needSendMessage;
    @With boolean waitCanSendStoryRequest;
    @With boolean showBoostsAlert;
    @With boolean disableProfileAnimation;
    @With boolean creatingChat;
    @With boolean allowPullingDown;
    @With boolean hasFallbackPhoto;
    @With boolean isInLandscapeMode;
    @With boolean doNotSetForeground;
    @With boolean needTimerImage;
    @With boolean needStarImage;
    @With boolean expandPhoto;

    @With boolean currentChatMagagroup;
    @With boolean currentChatCreator;
    @With boolean currentChatLeft;
    @With boolean currentChatKicked;

    @With String linkPrefix;

    @With LongSparseArray<TLRPC.ChatParticipant> participantsMap;
    @With TLRPC.User user;
    @With TLRPC.UserFull userInfo;
    @With TL_bots.BotInfo botInfo;
    @With TLRPC.ChatFull chatInfo;
    @With TL_account.TL_password currentPassword;
    @With TLRPC.Chat currentChat;
    @With TLRPC.EncryptedChat currentEncryptedChat;
    @With TLRPC.FileLocation avatar;
    @With TLRPC.FileLocation avatarBig;
    @With ImageLocation uploadingImageLocation;
    @With TLRPC.Photo updatedProfilePhoto;
    @With TLRPC.TL_photos_photo updatedProfilePhotoResponse;

}

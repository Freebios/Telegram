package org.telegram.ui.Profile.model;

import android.os.Bundle;

import org.telegram.PhoneFormat.PhoneFormat;

public class ProfileArgs {
    public final long userId;
    public final long chatId;
    public final long topicId;
    public final boolean saved;
    public final boolean openSimilar;
    public final boolean isTopic;
    public final long banFromGroup;
    public final int reportReactionMessageId;
    public final long reportReactionFromDialogId;
    public final boolean showAddToContacts;
    public final String vcardPhone;
    public final String vcardFirstName;
    public final String vcardLastName;
    public final boolean reportSpam;
    public final boolean myProfile;
    public final boolean openGifts;
    public final boolean openCommonChats;
    public final long dialogId;
    public final boolean preload_messages;
    public final int actionBarColor;
    public final boolean expandPhoto;
    public final int playProfileAnimation;

    public ProfileArgs(Bundle arguments) {
        userId = arguments.getLong("user_id", 0);
        chatId = arguments.getLong("chat_id", 0);
        topicId = arguments.getLong("topic_id", 0);
        saved = arguments.getBoolean("saved", false);
        openSimilar = arguments.getBoolean("similar", false);
        isTopic = topicId != 0;
        banFromGroup = arguments.getLong("ban_chat_id", 0);
        reportReactionMessageId = arguments.getInt("report_reaction_message_id", 0);
        reportReactionFromDialogId = arguments.getLong("report_reaction_from_dialog_id", 0);
        showAddToContacts = arguments.getBoolean("show_add_to_contacts", true);
        vcardPhone = PhoneFormat.stripExceptNumbers(arguments.getString("vcard_phone"));
        vcardFirstName = arguments.getString("vcard_first_name");
        vcardLastName = arguments.getString("vcard_last_name");
        reportSpam = arguments.getBoolean("reportSpam", false);
        myProfile = arguments.getBoolean("my_profile", false);
        openGifts = arguments.getBoolean("open_gifts", false);
        openCommonChats = arguments.getBoolean("open_common", false);
        expandPhoto = arguments.getBoolean("expandPhoto", false);
        dialogId = userId != 0 ? arguments.getLong("dialog_id", 0) : 0;
        preload_messages = arguments.getBoolean("preload_messages", false);
        actionBarColor = arguments.getInt("actionBarColor", 0);
        playProfileAnimation = arguments.getInt("playProfileAnimation", 0);
    }
}

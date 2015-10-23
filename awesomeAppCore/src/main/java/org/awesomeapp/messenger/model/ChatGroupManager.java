/*
 * Copyright (C) 2007 Esmertec AG. Copyright (C) 2007 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.awesomeapp.messenger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ChatGroupManager manages the creating, removing and the member of ChatGroups.
 */
public abstract class ChatGroupManager {

    protected HashMap<String, ChatGroup> mGroups;

    protected HashMap<String, Invitation> mInvitations;

    protected CopyOnWriteArrayList<GroupListener> mGroupListeners;

    protected InvitationListener mInvitationListener;

    protected ChatGroupManager() {
        mGroups = new HashMap<String, ChatGroup>();
        mInvitations = new HashMap<String, Invitation>();
        mGroupListeners = new CopyOnWriteArrayList<GroupListener>();
    }

    /**
     * Adds a {@link GroupListener} to this manager so that it will be notified when a
     * certain group changes.
     *
     * @param listener the listener to be notified.
     */
    public void addGroupListener(GroupListener listener) {
        mGroupListeners.add(listener);
    }

    /**
     * Removes a {@link GroupListener} from this manager so that it won't be notified
     * any more.
     *
     * @param listener the listener to remove.
     */
    public void removeGroupListener(GroupListener listener) {
        mGroupListeners.remove(listener);
    }

    /**
     * Sets the {@link InvitationListener} to the manager so that it will be notified
     * when an invitation from another users received.
     *
     * @param listener the {@link InvitationListener}.
     */
    public synchronized void setInvitationListener(InvitationListener listener) {
        mInvitationListener = listener;
    }

    /**
     * Creates a new {@link ChatGroup} with specified name. This method returns
     * immediately and the registered {@link GroupListener}s will be notified when the
     * group is created or any error occurs. The newly created group is a
     * temporary group and will be automatically deleted when all joined users
     * have left.
     *
     * @param address the name of the ChatGroup to be created.
     * @throws Exception
     */
    public abstract boolean createChatGroupAsync(String address, String subject, String nickname) throws Exception;

    /**
     * Deletes a certain {@link ChatGroup}. This method returns immediately and the
     * registered GroupListeners will be notified when the group is deleted or
     * any error occurs. Only the administrator of the ChatGroup can delete it.
     *
     * @param group the ChatGroup to be deleted.
     */
    public abstract void deleteChatGroupAsync(ChatGroup group);

    /**
     * Adds a member to a certain {@link ChatGroup}. This method returns immediately and
     * the GroupGroupListeners registered on the group will be notified when the
     * member is added or any error occurs. Only the administrator of the
     * ChatGroup can add member to it.
     *
     * @param group the ChatGroup to which the member will add.
     * @param contact the member to add.
     */
    protected abstract void addGroupMemberAsync(ChatGroup group, Contact contact);

    /**
     * Removes a member from certain {@link ChatGroup}. This method returns immediately
     * and the {@link GroupListener}s registered on the group will be notified when
     * the member is added or any error occurs. Only the administrator of the
     * ChatGroup can remove its members.
     *
     * @param group the {@link ChatGroup} whose member will be removed.
     * @param contact the member {@link Contact} to be removed.
     */
    protected abstract void removeGroupMemberAsync(ChatGroup group, Contact contact);

    /**
     * Joins into a certain {@link ChatGroup}. This method returns immediately and the
     * registered {@link GroupListener}s will be notified when the user joined into the
     * group or any error occurs.
     *
     * @param address the address of the {@link ChatGroup}.
     */
    public abstract void joinChatGroupAsync(Address address);

    /**
     * Leaves a certain {@link ChatGroup}.This method returns immediately and the
     * registered {@link GroupListener}s will be notified when the the user left the
     * group or any error occurs.
     *
     * @param group the ChatGroup.
     */
    public abstract void leaveChatGroupAsync(ChatGroup group);

    /**
     * Invites a user to join a certain {@link ChatGroup}. If success, the invitee will
     * receive an invitation with information of the group. Otherwise, the
     * registered {@link GroupListener}s will be notified if any error occurs.
     *
     * @param group the {@link ChatGroup}.
     * @param invitee the invitee.
     */
    public abstract void inviteUserAsync(ChatGroup group, Contact invitee);

    /**
     * Accepts an {@link Invitation}. The user will join the group automatically after
     * accept the invitation.
     *
     * @param invitation the {@link Invitation} to accept.
     */
    public abstract void acceptInvitationAsync(Invitation invitation);

    /**
     * Accepts an {@link Invitation}. The user can only accept or reject the same
     * invitation only once.
     *
     * @param inviteId the id of the {@link Invitation} to accept.
     * @see #acceptInvitationAsync(Invitation)
     */
    public void acceptInvitationAsync(String inviteId) {
        Invitation invitation = mInvitations.remove(inviteId);
        if (invitation != null) {
            acceptInvitationAsync(invitation);
        }
    }

    /**
     * Rejects an {@link Invitation}.
     *
     * @param inviteId the id of the {@link Invitation} to reject.
     * @see #rejectInvitationAsync(Invitation)
     */
    public void rejectInvitationAsync(String inviteId) {
        Invitation invitation = mInvitations.remove(inviteId);
        if (invitation != null) {
            rejectInvitationAsync(invitation);
        }
    }

    /**
     * Rejects an {@link Invitation}
     *
     * @param invitation the {@link Invitation} to reject.
     */
    public abstract void rejectInvitationAsync(Invitation invitation);

    /**
     * Gets a {@link ChatGroup} by address.
     *
     * @param address the address of the {@link ChatGroup}
     * @return a {@link ChatGroup}.
     */
    public ChatGroup getChatGroup(Address address) {
        return mGroups.get(address.getBareAddress());
    }

    /**
     * Notifies the {@link GroupListener}s that a {@link ChatGroup} has changed.
     *
     * @param groupAddress the {@link Address} of {@link ChatGroup} which has changed.
     * @param joined a list of {@link Contact}s that have joined the group.
     * @param left a list of {@link Contact}s that have left the group.
     */
    protected void notifyGroupChanged(Address groupAddress, ArrayList<Contact> joined,
            ArrayList<Contact> left) {
        ChatGroup group = mGroups.get(groupAddress.getAddress());
        if (group == null) {
            group = new ChatGroup(groupAddress, groupAddress.getUser(), this);
            mGroups.put(groupAddress.getAddress(), group);
        }
        if (joined != null) {
            for (Contact contact : joined) {
                notifyMemberJoined(group, contact);
            }
        }
        if (left != null) {
            for (Contact contact : left) {
                notifyMemberLeft(group, contact);
            }
        }
    }

    protected synchronized void notifyGroupCreated(ChatGroup group) {
        mGroups.put(group.getAddress().getAddress(), group);
        for (GroupListener listener : mGroupListeners) {
            listener.onGroupCreated(group);
        }
    }

    protected synchronized void notifyGroupDeleted(ChatGroup group) {
        mGroups.remove(group.getAddress().getAddress());
        for (GroupListener listener : mGroupListeners) {
            listener.onGroupDeleted(group);
        }
    }

    protected synchronized void notifyJoinedGroup(ChatGroup group) {
        mGroups.put(group.getAddress().getAddress(), group);
        for (GroupListener listener : mGroupListeners) {
            listener.onJoinedGroup(group);
        }
    }

    /**
     * Notifies the {@link GroupListener}s that the user has left a certain group.
     *
     * @param group the address of the {@link ChatGroup}.
     */
    protected synchronized void notifyLeftGroup(ChatGroup group) {
        mGroups.remove(group.getAddress().getAddress());
        for (GroupListener listener : mGroupListeners) {
            listener.onLeftGroup(group);
        }
    }

    protected synchronized void notifyGroupError(int errorType, String groupName, ImErrorInfo error) {
        for (GroupListener listener : mGroupListeners) {
            listener.onGroupError(errorType, groupName, error);
        }
    }

    /**
     * Notifies the InvitationListener that another user invited the current
     * logged user to join a group chat.
     */
    protected synchronized void notifyGroupInvitation(Invitation invitation) {
        mInvitations.put(invitation.getInviteID(), invitation);
        if (mInvitationListener != null) {
            mInvitationListener.onGroupInvitation(invitation);
        }
    }

    /**
     * Notifies that a contact has joined into this group.
     *
     * @param group the group into which the contact has joined.
     * @param contact the contact who has joined into the group.
     */
    protected void notifyMemberJoined(ChatGroup group, Contact contact) {
        group.notifyMemberJoined(contact);
    }

    /**
     * Notifies that a contact has left this group.
     *
     * @param group the group which the contact has left.
     * @param contact the contact who has left this group.
     */
    protected void notifyMemberLeft(ChatGroup group, Contact contact) {
        group.notifyMemberLeft(contact);
    }

    /**
     * Notifies that previous operation on this group has failed.
     *
     * @param error the error information.
     */
    protected void notifyGroupMemberError(ChatGroup group, ImErrorInfo error) {
        group.notifyGroupMemberError(error);
    }

    public abstract String getDefaultGroupChatService ();
}
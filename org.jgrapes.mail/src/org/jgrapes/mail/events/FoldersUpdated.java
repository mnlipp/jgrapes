/*
 * JGrapes Event driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail.events;

import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessageRemovedException;
import jakarta.mail.MessagingException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jgrapes.core.Event;
import org.jgrapes.mail.MailChannel;
import org.jgrapes.mail.MailStoreMonitor;

/**
 * Signals the retrieval of mails (update) by a {@link MailStoreMonitor}.
 * Must be fired on a {@link MailChannel}.
 */
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class FoldersUpdated extends Event<Void> {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Logger logger
        = Logger.getLogger(FoldersUpdated.class.getName());

    private final List<Folder> folders;
    private final List<Message> newMessages;

    /**
     * Instantiates a new event.
     *
     * @param allMessages the messages
     */
    public FoldersUpdated(List<Folder> folders, List<Message> newMessages) {
        this.folders = folders;
        this.newMessages = newMessages;
    }

    /**
     * Returns the folders.
     *
     * @return the list
     */
    public List<Folder> folders() {
        return folders;
    }

    /**
     * Return the new messages. New messages have not been reported
     * before by an event.
     *
     * @return the list
     */
    public List<Message> newMessages() {
        return newMessages;
    }

    /**
     * Execute the action with the given folder. The method ensures that
     * the folder is open.
     *
     * @param folder the folder
     * @param action the action
     * @throws MessagingException the messaging exception
     */
    @SuppressWarnings("PMD.GuardLogStatement")
    public static <R> R withFolder(Folder folder, Function<Folder, R> action)
            throws MessagingException {
        synchronized (folder) {
            if (!folder.isOpen()) {
                logger.fine("Found folder \"" + folder.getFullName()
                    + "\" to be unexpectedly closed.");
                folder.open(Folder.READ_WRITE);
            }
            return action.apply(folder);
        }
    }

    /**
     * Return all messages (which are not deleted) from the folder.
     *
     * @param folder the folder
     * @return the message[]
     * @throws MessagingException the messaging exception
     */
    public static List<Message> messages(Folder folder)
            throws MessagingException {
        return messages(folder, Integer.MAX_VALUE);
    }

    /**
     * Return all (or max) messages (which are not deleted) from the folder,
     * starting with the newest message.
     *
     * @param folder the folder
     * @param max the limit
     * @return the message[]
     * @throws MessagingException the messaging exception
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    public static List<Message> messages(Folder folder, int max)
            throws MessagingException {
        MessagingException[] exception = { null };
        @SuppressWarnings({ "PMD.PrematureDeclaration",
            "PMD.GuardLogStatement" })
        var result = withFolder(folder, f -> {
            List<Message> msgs = new LinkedList<>();
            try {
                int available = folder.getMessageCount();
                int retrieve = Math.min(available, max);
                int start = available - retrieve + 1;
                if (start > available) {
                    return msgs;
                }
                // Loops from older to newer
                for (var msg : f.getMessages(start, available)) {
                    if (canBeAdded(msg)) {
                        // prepend newer
                        msgs.add(0, msg);
                    }
                }
                // Adds older messages to fill until max
                while (start > 1 && msgs.size() < max) {
                    Message msg = f.getMessage(--start);
                    if (canBeAdded(msg)) {
                        msgs.add(msg);
                    }
                }
            } catch (MessagingException e) {
                logger.log(Level.FINE, "Problem getting messages: "
                    + e.getMessage(), e);
                exception[0] = e;
            }
            return msgs;
        });
        if (exception[0] != null) {
            throw exception[0];
        }
        return result;
    }

    private static boolean canBeAdded(Message msg)
            throws MessagingException {
        try {
            if (msg.getFlags().contains(Flag.DELETED)) {
                return false;
            }
        } catch (MessageRemovedException e) {
            return false;
        }
        return true;
    }
}

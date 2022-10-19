/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.mail.events;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;

/**
 * Indicates the arrival of a new message. Handler should delete
 * the message after successful processing.
 */
public class SendMessage extends Event<Void> {

    private Address from;
    @SuppressWarnings({ "PMD.ShortVariable", "PMD.AvoidDuplicateLiterals" })
    private Address[] to;
    @SuppressWarnings("PMD.ShortVariable")
    private Address[] cc = new Address[0];
    private Address[] bcc = new Address[0];
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, String> headers = new HashMap<>();
    private String subject;
    private MimeMultipart content;

    /**
     * Creates a new event.
     *
     * @param channels the channels
     */
    public SendMessage(Channel... channels) {
        super(channels);
    }

    /**
     * Gets the from addresses.
     *
     * @return the from
     */
    public Address from() {
        return from;
    }

    /**
     * Sets the from.
     *
     * @param from the from to set
     */
    public SendMessage setFrom(Address from) {
        this.from = from;
        return this;
    }

    /**
     * Gets the to addresses.
     *
     * @return the to
     */
    @SuppressWarnings({ "PMD.ShortMethodName",
        "PMD.MethodReturnsInternalArray" })
    public Address[] to() {
        return to;
    }

    /**
     * Sets the to addresses.
     *
     * @param to the to addresses to set
     */
    @SuppressWarnings({ "PMD.ShortVariable", "PMD.ArrayIsStoredDirectly" })
    public SendMessage setTo(Address... to) {
        this.to = to;
        return this;
    }

    /**
     * Sets the to addresses.
     *
     * @param to the to addresses to set
     */
    @SuppressWarnings("PMD.ShortVariable")
    public SendMessage setTo(List<Address> to) {
        this.to = to.toArray(new Address[0]);
        return this;
    }

    /**
     * Gets the cc addresses.
     *
     * @return the cc
     */
    @SuppressWarnings({ "PMD.ShortMethodName",
        "PMD.MethodReturnsInternalArray" })
    public Address[] cc() {
        return cc;
    }

    /**
     * Sets the cc addresses.
     *
     * @param cc the cc adresses to set
     */
    @SuppressWarnings({ "PMD.ShortVariable", "PMD.ArrayIsStoredDirectly" })
    public SendMessage setCc(Address... cc) {
        this.cc = cc;
        return this;
    }

    /**
     * Sets the cc addresses.
     *
     * @param cc the cc adresses to set
     */
    @SuppressWarnings("PMD.ShortVariable")
    public SendMessage setCc(List<Address> cc) {
        this.cc = cc.toArray(new Address[0]);
        return this;
    }

    /**
     * Gets the bcc addresses.
     *
     * @return the bcc
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Address[] bcc() {
        return bcc;
    }

    /**
     * Sets the bcc addresses.
     *
     * @param bcc the bcc addresses to set
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public SendMessage setBcc(Address... bcc) {
        this.bcc = bcc;
        return this;
    }

    /**
     * Sets the bcc addresses.
     *
     * @param bcc the bcc addresses to set
     */
    public SendMessage setBcc(List<Address> bcc) {
        this.bcc = bcc.toArray(new Address[0]);
        return this;
    }

    /**
     * Return the headers.
     *
     * @return the headers
     */
    public Map<String, String> headers() {
        return headers;
    }

    /**
     * Sets a header.
     *
     * @param name the name
     * @param value the value
     * @return the send mail message
     */
    public SendMessage setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public String subject() {
        return subject;
    }

    /**
     * Sets the subject.
     *
     * @param subject the subject to set
     */
    public SendMessage setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Returns the content.
     *
     * @return the mime multipart
     */
    public MimeMultipart content() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the content
     * @return the send mail message
     */
    public SendMessage setContent(MimeMultipart content) {
        this.content = content;
        return this;
    }

    /**
     * Adds the part to the content.
     *
     * @param part the part
     * @return the send mail message
     * @throws MessagingException the messaging exception
     */
    public SendMessage addContent(BodyPart part)
            throws MessagingException {
        if (content == null) {
            content = new MimeMultipart();
        }
        content.addBodyPart(part);
        return this;
    }
}

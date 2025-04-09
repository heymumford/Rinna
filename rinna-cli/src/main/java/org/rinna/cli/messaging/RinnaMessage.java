/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.messaging;

import java.time.Instant;
import java.util.Objects;

/**
 * Model class representing a message within the Rinna system.
 */
public class RinnaMessage {
    private final String id;
    private final String sender;
    private final String recipient;
    private final String content;
    private final String project;
    private final Instant timestamp;
    private MessageStatus status;
    private String inReplyTo;

    /**
     * Constructs a new RinnaMessage.
     *
     * @param id        unique identifier of the message
     * @param sender    username of the sender
     * @param recipient username of the recipient
     * @param content   content of the message
     * @param project   project context of the message
     * @param timestamp when the message was sent
     * @param status    read/unread status of the message
     */
    public RinnaMessage(String id, String sender, String recipient, String content, 
                        String project, Instant timestamp, MessageStatus status) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.project = project;
        this.timestamp = timestamp;
        this.status = status;
        this.inReplyTo = null;
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public String getProject() {
        return project;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RinnaMessage message = (RinnaMessage) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RinnaMessage{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", recipient='" + recipient + '\'' +
                ", content='" + content + '\'' +
                ", project='" + project + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                ", inReplyTo='" + inReplyTo + '\'' +
                '}';
    }
}
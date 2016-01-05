package se.kth.id2212.project.fish.common;

import java.io.Serializable;

public class Message implements Serializable {

    private MessageDescriptor descriptor;
    private Serializable content;

    public Message(MessageDescriptor descriptor, Serializable content) {
        this.descriptor = descriptor;
        this.content = content;
    }

    public MessageDescriptor getDescriptor() {
        return descriptor;
    }

    public Serializable getContent() {
        return content;
    }
}

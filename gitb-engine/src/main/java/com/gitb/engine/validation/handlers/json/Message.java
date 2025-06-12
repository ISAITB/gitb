package com.gitb.engine.validation.handlers.json;

/**
 * Class used to encapsulate the messages reported by the internal validation engine.
 */
public class Message {

    private final String contentPath;
    private String description;

    /**
     * Constructor.
     *
     * @param description The message description.
     */
    public Message(String description) {
        this(description, null);
    }

    /**
     * Constructor.
     *
     * @param description The message description.
     * @param contentPath The relevant content's path expression.
     */
    public Message(String description, String contentPath) {
        this.description = description;
        this.contentPath = contentPath;
    }

    /**
     * @param description The message description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return The message description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The relevant content's path expression.
     */
    public String getContentPath() {
        return contentPath;
    }

}

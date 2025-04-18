package net.fellbaum.jemoji;

/**
 * The EmojiType enum represents various types or formats in which emojis
 * can be encoded or represented.
 */
public enum EmojiType {

    /**
     * Represents a format for encoding emojis using HTML decimal notation.
     * In this representation, emojis are encoded as decimal values of their
     * Unicode code points, encapsulated within an HTML entity (e.g., "&#value;" format).
     * This format is commonly used in HTML documents to ensure proper rendering
     * of emojis or special characters across different platforms.
     */
    HTML_DECIMAL,
    /**
     * Represents a format for encoding emojis using HTML hexadecimal notation.
     * In this representation, emojis are encoded as hexadecimal values of their
     * Unicode code points, encapsulated within an HTML entity (e.g., "&#xvalue;" format).
     * This format is often used in HTML documents to ensure consistent rendering
     * of emojis or special characters across various platforms and browsers.
     */
    HTML_HEXADECIMAL,
    /**
     * Represents an encoding format in which emojis are transformed into
     * a URL-safe format by replacing non-ASCII characters with percent-encoded
     * values. This is typically used in web applications or HTTP requests
     * where emojis or special characters need to be included in URLs.
     */
    URL_ENCODED,
    /**
     * Represents the Unicode representation of an emoji. Selecting this type
     * ensures that the emoji is encoded in its standard Unicode format often
     * used in text processing and rendering.
     */
    UNICODE;

}


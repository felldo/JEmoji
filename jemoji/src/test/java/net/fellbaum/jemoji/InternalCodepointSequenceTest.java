package net.fellbaum.jemoji;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InternalCodepointSequenceTest {

    @Test
    public void testConstructorWithCodepoints() {
        // Arrange
        int[] codepoints = {0x1F44D}; // Thumbs up emoji codepoint

        // Act
        InternalCodepointSequence sequence = new InternalCodepointSequence(codepoints);

        // Assert
        assertNotNull(sequence);
        assertArrayEquals(codepoints, sequence.codepoints());
    }

    @Test
    public void testConstructorWithString() {
        // Arrange
        String emoji = "👍"; // Thumbs up emoji
        int[] expectedCodepoints = {0x1F44D};

        // Act
        InternalCodepointSequence sequence = new InternalCodepointSequence(emoji);

        // Assert
        assertNotNull(sequence);
        assertArrayEquals(expectedCodepoints, sequence.codepoints());
    }

    @Test
    public void testGetCodepoints() {
        // Arrange
        int[] codepoints = {0x1F600}; // Grinning face emoji codepoint
        InternalCodepointSequence sequence = new InternalCodepointSequence(codepoints);

        // Act
        int[] result = sequence.codepoints();

        // Assert
        assertNotNull(result);
        assertArrayEquals(codepoints, result);
        // Note: The class returns the actual internal array reference, not a copy
    }

    @Test
    public void testEquals_SameCodepoints() {
        // Arrange
        int[] codepoints1 = {0x1F44D}; // Thumbs up emoji codepoint
        int[] codepoints2 = {0x1F44D}; // Same codepoint in a different array
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints1);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(codepoints2);

        // Act & Assert
        assertEquals(sequence1, sequence2);
        assertEquals(sequence2, sequence1);
    }

    @Test
    public void testEquals_DifferentCodepoints() {
        // Arrange
        int[] codepoints1 = {0x1F44D}; // Thumbs up emoji codepoint
        int[] codepoints2 = {0x1F44E}; // Thumbs down emoji codepoint
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints1);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(codepoints2);

        // Act & Assert
        assertNotEquals(sequence1, sequence2);
        assertNotEquals(sequence2, sequence1);
    }

    @Test
    public void testEquals_DifferentLength() {
        // Arrange
        int[] codepoints1 = {0x1F44D}; // Single codepoint
        int[] codepoints2 = {0x1F44D, 0x1F44D}; // Two codepoints
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints1);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(codepoints2);

        // Act & Assert
        assertNotEquals(sequence1, sequence2);
        assertNotEquals(sequence2, sequence1);
    }

    @Test
    public void testEquals_WithNull() {
        // Arrange
        int[] codepoints = {0x1F44D}; // Thumbs up emoji codepoint
        InternalCodepointSequence sequence = new InternalCodepointSequence(codepoints);

        // Act & Assert
        assertNotEquals(sequence, null);
    }

    @Test
    public void testEquals_WithDifferentClass() {
        // Arrange
        int[] codepoints = {0x1F44D}; // Thumbs up emoji codepoint
        InternalCodepointSequence sequence = new InternalCodepointSequence(codepoints);
        Object differentObject = "Not a sequence";

        // Act & Assert
        assertNotEquals(sequence, differentObject);
    }

    @Test
    public void testHashCode_SameCodepoints() {
        // Arrange
        int[] codepoints1 = {0x1F44D}; // Thumbs up emoji codepoint
        int[] codepoints2 = {0x1F44D}; // Same codepoint in a different array
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints1);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(codepoints2);

        // Act & Assert
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }

    @Test
    public void testHashCode_DifferentCodepoints() {
        // Arrange
        int[] codepoints1 = {0x1F44D}; // Thumbs up emoji codepoint
        int[] codepoints2 = {0x1F44E}; // Thumbs down emoji codepoint
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints1);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(codepoints2);

        // Act & Assert
        assertNotEquals(sequence1.hashCode(), sequence2.hashCode());
    }

    @Test
    public void testMultipleCodepoints() {
        // Arrange
        int[] codepoints = {0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467, 0x200D, 0x1F466}; // Family emoji codepoints
        String familyEmoji = "👨‍👩‍👧‍👦"; // Family emoji

        // Act
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(codepoints);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(familyEmoji);

        // Assert
        assertArrayEquals(sequence1.codepoints(), sequence2.codepoints());
        assertEquals(sequence1, sequence2);
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }

    @Test
    public void testEmptyCodepoints() {
        // Arrange
        int[] emptyCodepoints = new int[0];
        String emptyString = "";

        // Act
        InternalCodepointSequence sequence1 = new InternalCodepointSequence(emptyCodepoints);
        InternalCodepointSequence sequence2 = new InternalCodepointSequence(emptyString);

        // Assert
        assertArrayEquals(emptyCodepoints, sequence1.codepoints());
        assertArrayEquals(emptyCodepoints, sequence2.codepoints());
        assertEquals(sequence1, sequence2);
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }

}

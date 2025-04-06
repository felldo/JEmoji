package net.fellbaum.jemoji.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Objects;

public interface ResourceFilesProvider {

    default Object readFileAsObject(final String filePathName) {
        try {
            try (final InputStream is = this.getClass().getResourceAsStream(filePathName);
                 final BufferedInputStream bis = new BufferedInputStream(Objects.requireNonNull(is));
                 final ObjectInputStream ois = new ObjectInputStream(bis);) {
                return ois.readObject();
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
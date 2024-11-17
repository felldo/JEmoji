package net.fellbaum.jemoji.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public interface ResourceFilesProvider {

    default Object readFileAsObject(final String filePathName) {
        try {
            try (final InputStream is = this.getClass().getResourceAsStream(filePathName)) {
                if (null == is) throw new IllegalStateException("InputStream is null");
                final ObjectInputStream ois = new ObjectInputStream(is);
                final Object readObject = ois.readObject();
                ois.close();
                return readObject;
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
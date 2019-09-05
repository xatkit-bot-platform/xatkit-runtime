package com.xatkit.util;

import com.xatkit.core.XatkitCore;
import org.apache.commons.configuration2.Configuration;

import java.io.File;

/**
 * Utility methods to load Xatkit-related files.
 */
public class FileUtils {

    /**
     * Retrieves the {@link File} corresponding to the provided {@code path}.
     * <p>
     * This method supports absolute and relative paths. Relative path are resolved against the configuration
     * directory (see {@link XatkitCore#CONFIGURATION_FOLDER_PATH_KEY}). If this directory is not specified then the
     * execution directory is used to resolve relative paths.
     *
     * @param path          the path of the {@link File} to retrieve
     * @param configuration the Xatkit {@link Configuration}
     * @return the retrieved {@link File}
     */
    public static File getFile(String path, Configuration configuration) {
        File file = new File(path);
        if (file.isAbsolute() || path.charAt(0) == '/') {
            /*
             * '/' comparison is a quickfix for windows, see https://bugs.openjdk.java.net/browse/JDK-8130462
             */
            return file;
        } else {
            String configurationDirectoryPath = configuration.getString(XatkitCore.CONFIGURATION_FOLDER_PATH_KEY, "");
            String relativePath = configurationDirectoryPath + File.separator + path;
            return new File(relativePath);
        }
    }
}

package com.xatkit.util;

import com.xatkit.core.XatkitBot;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;

import static java.util.Objects.isNull;

/**
 * Utility methods to load Xatkit-related files.
 */
public final class FileUtils {

    /**
     * Disables the default constructor, this class only provides static methods and should not be constructed.
     */
    private FileUtils() {
    }

    /**
     * Retrieves the {@link File} corresponding to the provided {@code path}.
     * <p>
     * This method supports absolute and relative paths. Relative path are resolved against the configuration
     * directory (see {@link XatkitBot#CONFIGURATION_FOLDER_PATH_KEY}). If this directory is not specified then the
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
            String relativePath = path;
            String configurationDirectoryPath = configuration.getString(XatkitBot.CONFIGURATION_FOLDER_PATH_KEY, "");
            if (!configurationDirectoryPath.isEmpty()) {
                relativePath = configurationDirectoryPath + File.separator + path;
            }
            return new File(relativePath);
        }
    }

    /**
     * Returns the Xatkit installation directory.
     * <p>
     * This method looks for the directory location stored in the {@code XATKIT} environment variable. If the
     * directory does not exist a {@link FileNotFoundException} is thrown.
     * <p>
     * {@code XATKIT} environment variable can be set following this tutorial: https://github
     * .com/xatkit-bot-platform/xatkit-releases/wiki/Installation.
     *
     * @return the Xatkit installation directory
     * @throws FileNotFoundException if the Xatkit installation directory does not exist
     */
    public static File getXatkitDirectory() throws FileNotFoundException {
        String xatkitDirectoryPath = System.getenv("XATKIT");
        if (isNull(xatkitDirectoryPath) || xatkitDirectoryPath.isEmpty()) {
            throw new FileNotFoundException("Cannot find the Xatkit installation directory, please check this "
                    + "tutorial article to see how to install Xatkit: https://github"
                    + ".com/xatkit-bot-platform/xatkit-releases/wiki/Installation");
        }
        File xatkitDirectoryFile = new File(xatkitDirectoryPath);
        if (!xatkitDirectoryFile.exists()) {
            throw new FileNotFoundException(MessageFormat.format("Cannot find the Xatkit installation directory ({0})"
                    + ", please check this tutorial article to see how to install Xatkit: https://github"
                    + ".com/xatkit-bot-platform/xatkit-releases/wiki/Installation", xatkitDirectoryPath));
        }
        return xatkitDirectoryFile;
    }
}

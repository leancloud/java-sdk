package com.avos.avoscloud.utils;

import java.util.regex.Pattern;

import com.avos.avoscloud.AVUtils;

/**
 * 
 * @author Android source code
 *
 */
public class MimeTypeMap {
  private static final MimeTypeMap sMimeTypeMap = new MimeTypeMap();

  private MimeTypeMap() {}

  /**
   * Returns the file extension or an empty string iff there is no extension. This method is a
   * convenience method for obtaining the extension of a url and has undefined results for other
   * Strings.
   * 
   * @param url
   * @return The file extension of the given url.
   */
  public static String getFileExtensionFromUrl(String url) {
    if (!AVUtils.isBlankString(url)) {
      int fragment = url.lastIndexOf('#');
      if (fragment > 0) {
        url = url.substring(0, fragment);
      }

      int query = url.lastIndexOf('?');
      if (query > 0) {
        url = url.substring(0, query);
      }

      int filenamePos = url.lastIndexOf('/');
      String filename = 0 <= filenamePos ? url.substring(filenamePos + 1) : url;

      // if the filename contains special characters, we don't
      // consider it valid for our matching purposes:
      if (!filename.isEmpty() && Pattern.matches("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+", filename)) {
        int dotPos = filename.lastIndexOf('.');
        if (0 <= dotPos) {
          return filename.substring(dotPos + 1);
        }
      }
    }

    return "";
  }

  /**
   * Get the singleton instance of MimeTypeMap.
   * 
   * @return The singleton instance of the MIME-type map.
   */
  public static MimeTypeMap getSingleton() {
    return sMimeTypeMap;
  }

  public String getMimeTypeFromExtension(String extension) {
    return MimeUtils.guessMimeTypeFromExtension(extension);
  }
}

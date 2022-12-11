package com.robothy.netty.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MimeTypeUtils {

  private static final Map<String, String> extensionToMimeType;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    map.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
    map.put("docm", "application/vnd.ms-word.document.macroEnabled.12");
    map.put("dotm", "application/vnd.ms-word.template.macroEnabled.12");
    map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    map.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
    map.put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
    map.put("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
    map.put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
    map.put("potm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
    map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    map.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
    map.put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
    map.put("xltm", "application/vnd.ms-excel.template.macroEnabled.12");
    map.put("doc", "application/msword");
    map.put("ppt", "application/vnd.ms-powerpoint");
    map.put("xls", "application/vnd.ms-excel");
    map.put("json", "applicaiton/json");
    map.put("csv", "text/csv");
    map.put("txt", "text/plain");
    map.put("pdf", "application/pdf");
    map.put("tiff", "image/tiff");
    map.put("tif", "image/tiff");
    map.put("png", "image/png");
    map.put("jpg", "image/jpeg");
    map.put("gif", "image/gif");
    map.put("bmp", "image/bmp");
    map.put("zip", "application/zip");
    extensionToMimeType = new HashMap<>(map);
  }

  public static String mimeTypeByFileExtension(String extension) {
    return extensionToMimeType.getOrDefault(extension, "application/octet-stream");
  }

}

package com.avos.avoscloud;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;

public class LogUtil {
  // this class only for debug @avos
  public static class avlog {
    public static boolean showAVLog = true;

    public static void i(String text) {
      if (showAVLog) {
        log.i(text);
      }
    }

    public static void i(Object o) {
      if (showAVLog) {
        log.i("" + o);
      }
    }

    public static void d(String text) {
      if (showAVLog) {
        log.d(text);
      }
    }

    public static void e(String text) {
      if (showAVLog) {
        log.e(text);
      }
    }
  }

  public static class log {
    private static final Logger logger = LogManager.getLogger(AVOSCloud.class);
    public static final boolean show = true;
    public static final String Tag = "===AVOS Cloud===";
    public static String Cname = "";
    public static String Mname = "";


    private static boolean shouldShow(int tag_level) {
      return show && (tag_level > 0);
    }

    protected static void getTrace() {
      StackTraceElement caller = new Throwable().fillInStackTrace().getStackTrace()[2];
      String className = new StringBuilder().append(caller.getClassName()).toString();
      className = className.substring(className.lastIndexOf(".") + 1);

      Cname = className;
      Mname =
          new StringBuilder().append(caller.getMethodName() + "->" + caller.getLineNumber() + ": ")
              .toString();
    }

    // ================================================================================
    // Verbose
    // ================================================================================

    public static void v(String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_VERBOSE))
        return;

      getTrace();
      if (null == text) {
        text = "null";
      }

      logger.info(Mname + text);
    }

    // ================================================================================
    // Debug
    // ================================================================================

    public static void d(String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_DEBUG))
        return;

      getTrace();
      if (null == text) {
        text = "null";
      }

      logger.debug(Cname + "->" + Mname + text);
    }

    public static void d(Map<String, Object> o) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_DEBUG))
        return;
      String text = "";
      getTrace();
      if (null == o) {
        text = "null";
      } else {
        try {
          text = JSON.toJSONString(o);
        } catch (Exception e) {

        }
      }

      logger.debug(Cname + "->" + Mname + text);
    }

    public static void d(int text) {
      d("" + text);
    }

    public static void d(float text) {
      d("" + text);
    }

    public static void d(double text) {
      d("" + text);
    }

    public static void d() {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_DEBUG))
        return;

      getTrace();
      logger.debug(Tag + "->" + Mname + "");
    }

    public static void d(String Tag, String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_DEBUG))
        return;

      getTrace();
      logger.debug(Cname + "->" + Mname + text);
    }

    public static void d(String text, Exception e) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;
      String tmp = Cname + "->" + Mname + text + ":" + e.toString();
      logger.debug(tmp);
      e.printStackTrace();
    }

    // ================================================================================
    // Info
    // ================================================================================

    public static void i(String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_INFO))
        return;

      getTrace();
      if (null == text) {
        text = "null";
      }

      logger.info(Mname + text);
    }

    // ================================================================================
    // Warning
    // ================================================================================

    public static void w(String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_WARNING))
        return;

      getTrace();
      if (null == text) {
        text = "null";
      }

      logger.warn(Mname + text);
    }

    // ================================================================================
    // Error
    // ================================================================================

    public static void e(String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;

      getTrace();
      if (null == text) {
        text = "null";
      }

      logger.error(Cname + "->" + Mname + text);
    }

    public static void e() {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;
      getTrace();
      logger.error(Cname + "->" + Mname + "");
    }

    public static void e(String text, Exception e) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;
      String tmp = text + Mname + "err:" + e.toString();
      logger.error(tmp);
      e.printStackTrace();
    }

    public static void e(String Tag, String text) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;
      getTrace();
      logger.error(Cname + "->" + Mname + text);
    }

    public static void e(String Tag, String text, Exception e) {
      if (!shouldShow(AVOSCloud.LOG_LEVEL_ERROR))
        return;
      getTrace();
      logger.error(Cname + "->" + Mname + text + " err:" + e.toString());
    }
  }
}

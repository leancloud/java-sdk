package com.avos.avoscloud;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User: summer Date: 13-4-16 Time: AM10:43
 */
abstract class AVUploader {
  protected final AVFile parseFile;
  protected SaveCallback saveCallback;
  protected ProgressCallback progressCallback;
  protected long totalSize;
  private volatile boolean cancelled = false;
  private volatile boolean complete = false;
  private volatile Future future;

  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
  private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
  private static final int MAX_POOL_SIZE = CPU_COUNT * 2 + 1;
  private static final long KEEP_ALIVE_TIME = 1L;

  private static ThreadPoolExecutor executor;

  static {
    executor = new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>());
  }



  protected static final int defaultFileKeyLength = 40;

  protected AVUploader(AVFile parseFile, SaveCallback saveCallback,
      ProgressCallback progressCallback) {
    this.parseFile = parseFile;
    this.saveCallback = saveCallback;
    this.progressCallback = progressCallback;
    cancelled = false;
    complete = false;
  }

  abstract AVException doWork();

  // put
  public void execute() {
    Runnable task = new Runnable() {
      @Override
      public void run() {
        AVException exception = doWork();
        if (!cancelled) {
          complete = true;
          onPostExecute(exception);
        } else {
          onPostExecute(AVErrorUtils.createException(AVException.UNKNOWN,
              "Uploading file task is canceled."));
        }
      }
    };

    future = executor.submit(task);
  }

  // @Override
  protected AVException doInBackground(Void... arg0) {
    return doWork();
  }

  // @Override
  protected void onProgressUpdate(Integer progress) {
    if (progressCallback != null) progressCallback.internalDone(progress, null);
  }

  // @Override
  protected void onPostExecute(AVException result) {
    if (saveCallback != null) {
      saveCallback.internalDone(result);
    }
  }

  // @Override
  protected void onCancelled() {
    // super.onCancelled();
    LogUtil.log.d("upload cancel");
  }


  // ignore interrupt so far.
  public boolean cancel(boolean interrupt) {
    if (cancelled || complete) {
      return false;
    }
    cancelled = true;
    if (interrupt) {
      interruptImmediately();
    } else {
      if (future != null) {
        future.cancel(false);
      }
    }
    onCancelled();
    return true;
  }

  public void interruptImmediately() {
    if (future != null) {
      // Interrupts worker thread.
      future.cancel(true);
    }
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void publishProgress(int percentage) {
    onProgressUpdate(percentage);
  }
}

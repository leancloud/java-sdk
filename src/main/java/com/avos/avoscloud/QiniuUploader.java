package com.avos.avoscloud;

import org.json.JSONException;
import org.json.JSONObject;

import com.alibaba.fastjson.JSON;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.avos.avoscloud.okhttp.Call;
import com.avos.avoscloud.okhttp.MediaType;
import com.avos.avoscloud.okhttp.Request;
import com.avos.avoscloud.okhttp.RequestBody;
import com.avos.avoscloud.okhttp.Response;

/**
 * User: summer,dennis Date: 13-4-15 Time: PM4:12
 */
class QiniuUploader extends HttpClientUploader {
  private String bucket;
  private String token;
  private String key;
  private String hash;
  private String objectId;
  private String url;
  private String[] uploadFileCtx;
  private int blockCount;

  private static final String QINIU_HOST = "http://upload.qiniu.com";
  private static final String QINIU_CREATE_BLOCK_EP = QINIU_HOST + "/mkblk/%d";
  private static final String QINIU_BRICK_UPLOAD_EP = QINIU_HOST + "/bput/%s/%d";
  private static final String QINIU_MKFILE_EP = QINIU_HOST + "/mkfile/%d/key/%s";
  private static final int WIFI_CHUNK_SIZE = 256 * 1024;
  private static final int BLOCK_SIZE = 1024 * 1024 * 4;
  private static final int NONWIFI_CHUNK_SIZE = 64 * 1024;

  private static final int DEFAULT_RETRY_TIMES = 6;
  static final int PROGRESS_GET_TOKEN = 10;
  static final int PROGRESS_UPLOAD_FILE = 90;
  private static final int PROGRESS_COMPLETE = 100;

  private volatile Call mergeFileRequestCall;
  private volatile Future[] tasks;

  QiniuUploader(AVFile parseFile, SaveCallback saveCallback, ProgressCallback progressCallback) {
    super(parseFile, saveCallback, progressCallback);
  }

  int uploadChunkSize = WIFI_CHUNK_SIZE;
  static final ExecutorService fileUploadExecutor = Executors.newFixedThreadPool(10);

  @Override
  AVException doWork() {
    parseFileKey();
    if (AVOSCloud.isDebugLogEnabled()) {
      LogUtil.avlog.d("uploading with chunk size:" + uploadChunkSize);
    }
    return uploadWithBlocks();
  }

  private void parseFileKey() {
    key = AVUtils.getRandomString(defaultFileKeyLength);
    int idx = 0;
    if (parseFile.getName() != null) {
      idx = parseFile.getName().lastIndexOf(".");
    }
    // try to add post fix.
    if (idx > 0) {
      String postFix = parseFile.getName().substring(idx);
      key += postFix;
    }
  }

  private Request.Builder addAuthHeader(Request.Builder builder) throws Exception {
    if (token != null) {
      builder.addHeader("Authorization", "UpToken " + token);
    }
    return builder;
  }

  private AVException uploadWithBlocks() {

    try {
      byte[] bytes = parseFile.getLocalFileData();
      if (bytes == null) {
        return new AVException("File doesn't exist", new FileNotFoundException());
      }
      // 1.通过服务器申请文件存储地址
      AVException getBucketException = fetchUploadBucket();
      if (getBucketException != null) {
        return getBucketException;
      }
      publishProgress(PROGRESS_GET_TOKEN);
      blockCount = (bytes.length / BLOCK_SIZE) + (bytes.length % BLOCK_SIZE > 0 ? 1 : 0);
      uploadFileCtx = new String[blockCount];

      // 2.按照分片进行上传
      QiniuBlockResponseData respBlockData = null;
      CountDownLatch latch = new CountDownLatch(blockCount);
      tasks = new Future[blockCount];
      synchronized (tasks) {
        for (int blockOffset = 0; blockOffset < blockCount; blockOffset++) {
          tasks[blockOffset] =
              fileUploadExecutor.submit(new FileBlockUploadTask(bytes, blockOffset, latch,
                  uploadChunkSize, uploadFileCtx, this));
        }
      }
      latch.await();
      if (AVExceptionHolder.exists()) {
        for (Future task : tasks) {
          if (!task.isDone()) {
            task.cancel(true);
          }
        }

        throw AVExceptionHolder.remove();
      }
      // 3 merge文件
      QiniuMKFileResponseData mkfileResp = makeFile(bytes.length, key, DEFAULT_RETRY_TIMES);

      if (!isCancelled()) {
        // qiniu's status code is 200, but should be 201 like parse..
        if (mkfileResp == null || !mkfileResp.key.equals(key)) {
          destroyFileObject();
          return AVErrorUtils.createException(AVException.OTHER_CAUSE, "upload file failure");
        } else {
          parseFile.handleUploadedResponse(objectId, objectId, url);
          publishProgress(PROGRESS_COMPLETE);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      destroyFileObject();
      return new AVException(e);
    }
    return null;
  }

  private static class QiniuBlockResponseData {
    public String ctx;
    public long crc32;
    public int offset;
    public String host;
    public String checksum;
  }

  private static class QiniuMKFileResponseData {
    public String key;
    public String hash;
  }

  private QiniuMKFileResponseData makeFile(int dataSize, String key, int retry) throws Exception {
    try {
      String endPoint = String.format(QINIU_MKFILE_EP, dataSize, AVUtils.Base64Encode(key));
      List<String> list = new LinkedList<String>();
      Collections.addAll(list, uploadFileCtx);
      final String joinedFileCtx = AVUtils.joinCollection(list, ",");
      Request.Builder builder = new Request.Builder();
      builder.url(endPoint);

      builder = builder.post(RequestBody.create(MediaType.parse("text"), joinedFileCtx));
      builder = addAuthHeader(builder);
      mergeFileRequestCall = getOKHttpClient().newCall(builder.build());
      return parseQiniuResponse(mergeFileRequestCall.execute(), QiniuMKFileResponseData.class);
    } catch (Exception e) {
      if (retry-- > 0) {
        return makeFile(dataSize, key, retry);
      } else {
        LogUtil.log.e("Exception during file upload", e);
      }
    }
    return null;
  }

  private static <T> T parseQiniuResponse(Response resp, Class<T> clazz) throws Exception {

    int code = resp.code();
    String phrase = resp.message();

    String h = resp.header("X-Log");

    if (code == 401) {
      throw new Exception("unauthorized to create Qiniu Block");
    }
    String responseData = AVUtils.stringFromBytes(resp.body().bytes());
    try {
      if (code / 100 == 2) {
        T data = JSON.parseObject(responseData, clazz);
        return data;
      }
    } catch (Exception e) {
    }

    if (responseData.length() > 0) {
      throw new Exception(code + ":" + responseData);
    }
    if (!AVUtils.isBlankString(h)) {
      throw new Exception(h);
    }
    throw new Exception(phrase);
  }

  private void destroyFileObject() {
    if (!AVUtils.isBlankString(this.objectId)) {
      try {
        AVObject fileObject = AVObject.createWithoutData("_File", objectId);
        fileObject.delete();
      } catch (Exception e) {
        // ignore
      }
    }
  }



  // ================================================================================
  // Private Methods
  // ================================================================================

  private AVException handleGetBucketResponse(String responseStr, AVException exception) {
    if (exception != null)
      return exception;
    try {
      JSONObject jsonObject = new JSONObject(responseStr);
      bucket = jsonObject.getString("bucket");
      objectId = jsonObject.getString("objectId");
      token = jsonObject.getString("token");
      if (AVUtils.isBlankString(token)) {
        return new AVException(AVException.OTHER_CAUSE, "No token return for qiniu upload");
      }
      url = jsonObject.getString("url");
    } catch (JSONException e) {
      return new AVException(e);
    }

    return null;
  }

  private String getGetBucketParameters() {
    Map<String, Object> parameters = new HashMap<String, Object>(3);
    parameters.put("key", key);
    parameters.put("name", parseFile.getName());
    parameters.put("mime_type", parseFile.mimeType());
    parameters.put("metaData", parseFile.getMetaData());
    parameters.put("__type", AVFile.className());
    if (parseFile.getACL() != null) {
      parameters.putAll(AVUtils.getParsedMap(parseFile.getACL().getACLMap()));
    }

    return AVUtils.restfulServerData(parameters);
  }

  // http://192.168.1.25:2271/1/qiniu
  protected String getUploadPath() {
    return "qiniu";
  }

  protected AVException fetchUploadBucket() {
    final AVException[] exceptionWhenGetBucket = new AVException[1];
    if (!isCancelled()) {
      PaasClient.storageInstance().postObject(getUploadPath(), getGetBucketParameters(), true,
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              exceptionWhenGetBucket[0] = handleGetBucketResponse(content, e);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              exceptionWhenGetBucket[0] = AVErrorUtils.createException(error, content);
            }
          });
      if (exceptionWhenGetBucket[0] != null) {
        destroyFileObject();
        return exceptionWhenGetBucket[0];
      }
    }
    return null;
  }

  private static class FileBlockUploadTask implements Runnable {
    private byte[] bytes;
    private int blockOffset;
    CountDownLatch latch;
    final int uploadChunkSize;
    String[] uploadFileCtx;
    QiniuUploader parent;

    public FileBlockUploadTask(byte[] bytes, int blockOffset, CountDownLatch latch,
        int uploadChunkSize, String[] uploadFileCtx, QiniuUploader parent) {
      this.bytes = bytes;
      this.blockOffset = blockOffset;
      this.latch = latch;
      this.uploadChunkSize = uploadChunkSize;
      this.uploadFileCtx = uploadFileCtx;
      this.parent = parent;
    }

    public void run() {
      QiniuBlockResponseData respBlockData;
      // 1.创建一个block,并且会上传第一个block的第一个chunk的数据
      int currentBlockSize = getCurrentBlockSize(bytes, blockOffset);
      respBlockData = createBlockInQiniu(blockOffset, currentBlockSize, DEFAULT_RETRY_TIMES, bytes);
      // 2.分片上传
      if (respBlockData != null) {
        respBlockData =
            putFileBlocksToQiniu(blockOffset, bytes, respBlockData, DEFAULT_RETRY_TIMES);
      }
      if (respBlockData != null) {
        uploadFileCtx[blockOffset] = respBlockData.ctx;
      } else {
        AVExceptionHolder.add(new AVException(AVException.OTHER_CAUSE, "Upload File failure"));
        long count = latch.getCount();
        for (; count > 0; count--) {
          latch.countDown();
        }
      }
      latch.countDown();
    }

    private QiniuBlockResponseData createBlockInQiniu(final int blockOffset, int blockSize,
        int retry, final byte[] data) {
      try {
        if (AVOSCloud.isDebugLogEnabled()) {
          LogUtil.avlog.d("try to mkblk");
        }
        String endPoint = String.format(QINIU_CREATE_BLOCK_EP, blockSize);
        Request.Builder builder = new Request.Builder();
        builder.url(endPoint);

        final int nextChunkSize = getNextChunkSize(blockOffset, data);

        RequestBody requestBody =
            RequestBody.create(MediaType.parse(parent.parseFile.mimeType()), data, blockOffset,
                nextChunkSize);

        builder = builder.post(requestBody);
        builder = parent.addAuthHeader(builder);
        return parseQiniuResponse(getOKHttpClient().newCall(builder.build()).execute(),
            QiniuBlockResponseData.class);
      } catch (Exception e) {
        e.printStackTrace();
        if (retry-- > 0) {
          return createBlockInQiniu(blockOffset, blockSize, retry, data);
        } else {
          LogUtil.log.e("Exception during file upload", e);
        }
      }
      return null;
    }


    private QiniuBlockResponseData putFileBlocksToQiniu(final int blockOffset, final byte[] data,
        QiniuBlockResponseData lastChunk, int retry) {
      int currentBlockLength = getCurrentBlockSize(data, blockOffset);
      int remainingBlockLength = currentBlockLength - lastChunk.offset;

      if (remainingBlockLength > 0 && lastChunk.offset > 0) {
        try {
          String endPoint = String.format(QINIU_BRICK_UPLOAD_EP, lastChunk.ctx, lastChunk.offset);
          Request.Builder builder = new Request.Builder();
          builder.url(endPoint);
          builder.addHeader("Content-Type", "application/octet-stream");

          final QiniuBlockResponseData chunkData = lastChunk;
          final int nextChunkSize =
              remainingBlockLength > uploadChunkSize ? uploadChunkSize : remainingBlockLength;

          RequestBody requestBody =
              RequestBody.create(MediaType.parse(parent.parseFile.mimeType()), data, blockOffset
                  * BLOCK_SIZE + chunkData.offset, nextChunkSize);

          builder = builder.post(requestBody);
          builder = parent.addAuthHeader(builder);
          QiniuBlockResponseData respData =
              parseQiniuResponse(getOKHttpClient().newCall(builder.build()).execute(),
                  QiniuBlockResponseData.class);
          if (respData != null) {
            if (respData.offset < currentBlockLength) {
              return putFileBlocksToQiniu(blockOffset, data, respData, DEFAULT_RETRY_TIMES);
            } else {
              return respData;
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          if (retry-- > 0) {
            return putFileBlocksToQiniu(blockOffset, data, lastChunk, retry);
          } else {
            LogUtil.log.e("Exception during file upload", e);
          }
        }
      } else {
        // 这个应该是遇到多余的一个block里面的数据只够本block的第一个chunk塞，这部分数据已经在mkblk的上传过了，所以直接返回原来的resp就可以了
        return lastChunk;
      }
      return null;
    }


    private int getCurrentBlockSize(byte[] bytes, int blockOffset) {
      return (bytes.length - blockOffset * BLOCK_SIZE) > BLOCK_SIZE ? BLOCK_SIZE
          : (bytes.length - blockOffset * BLOCK_SIZE);
    }

    private int getNextChunkSize(int blockOffset, byte[] data) {
      return ((data.length - blockOffset * BLOCK_SIZE) > uploadChunkSize) ? uploadChunkSize
          : (data.length - blockOffset * BLOCK_SIZE);
    }
  }

  @Override
  public void interruptImmediately() {
    super.interruptImmediately();

    if (tasks != null && tasks.length > 0) {
      synchronized (tasks) {
        for (int index = 0; index < tasks.length; index++) {
          Future task = tasks[index];
          if (task != null && !task.isDone() && !task.isCancelled()) {
            task.cancel(true);
          }
        }
      }
    }

    if (mergeFileRequestCall != null) {
      mergeFileRequestCall.cancel();
    }
  }
}

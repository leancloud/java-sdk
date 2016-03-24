package com.avos.avoscloud;

import com.avos.avoscloud.signature.AES;
import com.avos.avoscloud.signature.Base64Encoder;
import com.avos.avoscloud.okhttp.Call;
import com.avos.avoscloud.okhttp.MediaType;
import com.avos.avoscloud.okhttp.MultipartBuilder;
import com.avos.avoscloud.okhttp.OkHttpClient;
import com.avos.avoscloud.okhttp.Request;
import com.avos.avoscloud.okhttp.RequestBody;
import com.avos.avoscloud.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by summer on 13-5-27.
 */
public class S3Uploader extends HttpClientUploader {
  private String access;
  private String secret;

  private String objectId;
  private String url;

  private String uuid;
  private static final String bucket = "avos-cloud";
  private static final String S3BasePath = "https://s3.amazonaws.com/avos-cloud";
  private volatile Call call;

  S3Uploader(AVFile parseFile, SaveCallback saveCallback, ProgressCallback progressCallback) {
    super(parseFile, saveCallback, progressCallback);
  }

  @Override
  AVException doWork() {
    uuid = UUID.randomUUID().toString().toLowerCase();
    int idx = parseFile.getName().indexOf(".");
    // try to add post fix.
    if (idx > 0) {
      String postFix = parseFile.getName().substring(idx);
      uuid += postFix;
    }
    final OkHttpClient httpClient = getOKHttpClient();
    Response response = null;
    String serverResponse = null;
    try {
      byte[] bytes = parseFile.getLocalFileData();

      // get access/secret
      final AVException[] exceptionWhenGetBucket = new AVException[1];
      PaasClient.storageInstance().getObject("s3", null, true, null, new GenericObjectCallback() {
        @Override
        public void onSuccess(String content, AVException e) {
          exceptionWhenGetBucket[0] = handleGetKeyResponse(content);
        }

        @Override
        public void onFailure(Throwable error, String content) {
          exceptionWhenGetBucket[0] = AVErrorUtils.createException(error, content);
        }
      });
      if (exceptionWhenGetBucket[0] != null) return exceptionWhenGetBucket[0];
      // END of get access/secret

      // change name to unique for url
      String path = "android/" + uuid;
      url = getS3Link(path);

      // post to urulu
      PaasClient.storageInstance().postObject("files", getParametersForUrulu(), true,
          new GenericObjectCallback() {
            @Override
            public void onSuccess(String content, AVException e) {
              exceptionWhenGetBucket[0] = handlePostResponse(content);
            }

            @Override
            public void onFailure(Throwable error, String content) {
              exceptionWhenGetBucket[0] = AVErrorUtils.createException(error, content);
            }
          });
      if (exceptionWhenGetBucket[0] != null) return exceptionWhenGetBucket[0];

      // upload to s3
      Request.Builder builder = new Request.Builder();
      builder.url("http://" + bucket + ".s3.amazonaws.com/");

      // ================================================================================
      // setup multi part
      // ================================================================================

      Charset charset = Charset.forName("UTF-8");
      // support file for future

      RequestBody requestBody = new MultipartBuilder()
          .addFormDataPart("acl", null,
              RequestBody.create(MediaType.parse("text/plain"), "public-read".getBytes(charset)))
          .addFormDataPart("key", null,
              RequestBody.create(MediaType.parse("text/plain"), path.getBytes(charset)))
          .addFormDataPart("file", uuid,
              RequestBody.create(MediaType.parse("application/octet-stream"), bytes))
          .build();



      totalSize = requestBody.contentLength();
      builder.post(requestBody);
      // content type
      String contentType = requestBody.contentType().toString();
      String dateString = RFC822FormatStringFromDate(new Date());
      builder.addHeader("Authorization", authorization("POST", contentType, dateString));
      builder.addHeader("Date", dateString);
      builder.addHeader("Content-Type", contentType);

      // Send it
      call = httpClient.newCall(builder.build());

      if (!isCancelled()) {
        response = call.execute();
        // The 204 status code implies no response is needed
        if (response.code() != 204) {
          serverResponse = AVUtils.stringFromBytes(response.body().bytes());
          LogUtil.avlog.e(serverResponse);
          return AVErrorUtils.createException(AVException.OTHER_CAUSE, "upload file failure");
        } else {
          parseFile.handleUploadedResponse(objectId, objectId, url);
        }
      }
    } catch (Exception e) {
      return new AVException(e.getCause());
    }
    return null;
  }

  // ================================================================================
  // Private methods
  // ================================================================================

  private AVException handleGetKeyResponse(String responseStr) {
    try {
      JSONObject jsonObject = new JSONObject(responseStr);

      AES aes = new AES();
      access = aes.decrypt(jsonObject.getString("access_key"));
      secret = aes.decrypt(jsonObject.getString("access_token"));

    } catch (Exception e) {
      return new AVException(e);
    }

    return null;
  }

  private AVException handlePostResponse(String responseStr) {
    try {
      JSONObject jsonObject = new JSONObject(responseStr);
      objectId = jsonObject.getString("objectId");

    } catch (JSONException e) {
      return new AVException(e);
    }

    return null;
  }

  private String getParametersForUrulu() {
    Map<String, Object> parameters = new HashMap<String, Object>(3);
    parameters.put("key", "android/" + uuid);
    parameters.put("name", parseFile.getName());
    parameters.put("mime_type", parseFile.mimeType());
    parameters.put("metaData", parseFile.getMetaData());
    parameters.put("__type", AVFile.className());
    parameters.put("url", url);
    if (parseFile.getACL() != null) {
      parameters.putAll(AVUtils.getParsedMap(parseFile.getACL().getACLMap()));
    }
    return AVUtils.restfulServerData(parameters);
  }

  // like alpacino.jpg, test/someother/alpacino.jpg
  private String getS3Link(String filePathOrName) {
    return S3BasePath + "/" + filePathOrName;
  }

  // ================================================================================
  // Signature
  // ================================================================================

  // This method creates S3 signature for a given String.
  private String md5WithHmac(String data) throws Exception {
    javax.crypto.Mac mac = Mac.getInstance("HmacSHA1");
    byte[] keyBytes = secret.getBytes("UTF8");
    javax.crypto.spec.SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
    mac.init(signingKey);

    // Signed String must be BASE64 encoded.
    byte[] signBytes = mac.doFinal(data.getBytes("UTF8"));
    String signature = Base64Encoder.encode(signBytes);
    return signature;
  }

  private String authorization(String httpVerb, String contentType, String dateString)
      throws Exception {
    return "AWS" + " " + access + ":" + signature(httpVerb, contentType, dateString);
  }

  private String signature(String httpVerb, String contentType, String dateString)
      throws Exception {
    String canonicalizedResource = "/" + bucket + "/";
    String stringToSign = httpVerb + "\n" // POST
        + "\n" // CONTENT-MD5
        + contentType + "\n" + dateString + "\n" // date
        + "" // CanonicalizedAmzHeaders
        + canonicalizedResource;
    return md5WithHmac(stringToSign);
  }

  private String RFC822FormatStringFromDate(Date date) {
    SimpleDateFormat simpleDateFormat =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return simpleDateFormat.format(date);
  }

  @Override
  public void interruptImmediately() {
    super.interruptImmediately();
    if (call != null) {
      call.cancel();
    }
  }
}

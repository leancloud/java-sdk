package com.avos.avoscloud;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;
import com.avos.avoscloud.AVOSCloud.StorageType;
import com.avos.avoscloud.utils.MimeTypeMap;
import com.avos.avoscloud.utils.UrlValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * AVFile is a local representation of a file that is saved to the AVOSCloud cloud.
 * </p>
 * <p>
 * The workflow is to construct a AVFile with data and optionally a filename. Then save it and set
 * it as a field on a AVObject.
 * </p>
 * Example:
 * 
 * <pre>
 * AVFile file = new AVFile(&quot;hello&quot;.getBytes());
 * file.save();
 * AVObject object = new AVObject(&quot;TestObject&quot;);
 * object.put(&quot;file&quot;, file);
 * object.save();
 * </pre>
 */
@JSONType(ignores = {"fileObject","name"})
public final class AVFile {

  /**
   * 需要上传但是未上传的 dirty 会标记为 true
   */
  private boolean dirty;

  private String name;

  /**
   * AVFile 中的具体文件的来源有三种，一种是用户传过来的本地文件，一种是用户传过来的 byte[]，一种是通过 Url 下载来的 此变量存储对应的“通过 Url 下载来的”
   */
  private String url;

  /**
   * 此变量存储对应的“用户传过来的本地文件”
   */
  private String localPath;

  private byte[] data;

  transient private AVUploader uploader;
  // metadata for file,added by dennis<xzhuang@avos.com>,2013-09-06
  private final HashMap<String, Object> metaData = new HashMap<String, Object>();
  private static String defaultMimeType = "application/octet-stream";
  private static final String FILE_SUM_KEY = "_checksum";
  static final String FILE_NAME_KEY = "_name";
  private String objectId;
  private AVObject fileObject;
  private String bucket;
  private static final String ELDERMETADATAKEYFORIOSFIX = "metadata";
  private AVACL acl;

  public AVFile() {
    super();
    if (PaasClient.storageInstance().getDefaultACL() != null) {
      acl = new AVACL(PaasClient.storageInstance().getDefaultACL());
    }
  }

  AVObject getFileObject() {
    if (fileObject == null && !AVUtils.isBlankString(objectId)) {
      fileObject = AVObject.createWithoutData("_File", objectId);
    }
    return fileObject;
  }

  /**
   * 创建一个基于网络文件的AVFile对象
   * 
   * @param name 文件名
   * @param url 网络文件的url
   * @param metaData 网络文件的元信息，可以为空
   */
  public AVFile(String name, String url, Map<String, Object> metaData) {
    this();
    this.name = name;
    this.url = url;
    if (metaData != null) {
      this.metaData.putAll(metaData);
    }
    this.metaData.put("__source", "external");
  }


  /**
   * Creates a new file from a byte array and a name. Giving a name with a proper file extension
   * (e.g. ".png") is ideal because it allows AVOSCloud to deduce the content type of the file and
   * set appropriate HTTP headers when it is fetched.
   * 
   * @param name The file's name, ideally with extension.
   * @param data The file's data
   */
  public AVFile(String name, byte[] data) {
    this();

    this.dirty = true;
    this.name = name;
    if (null != data) {
      String md5 = AVUtils.computeMD5(data);
      this.metaData.put(FILE_SUM_KEY, md5);
      this.metaData.put("size", data.length);
    } else {
      this.metaData.put("size", 0);
    }
    this.data = data;

    AVUser currentUser = AVUser.getCurrentUser();
    this.metaData.put("owner", currentUser != null ? currentUser.getObjectId() : "");
    this.metaData.put(FILE_NAME_KEY, name);
  }

  /**
   * @hide For internal use only.
   */
  protected AVFile(String name, String url) {
    super();

    this.dirty = false;
    this.name = name;
    this.url = url;
  }

  /**
   * Returns the file object Id.
   * 
   * @return The file object id.
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * Set the file objectId.
   * 
   * @param objectId file object id.
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * Retrieve a AVFile object by object id from AVOSCloud.If the file is not found,it will throw
   * java.io.FileNotFoundException.
   * 
   * @since 2.0.2
   * @param objectId
   * @return
   * @throws AVException ,FileNotFoundException
   */
  public static AVFile withObjectId(String objectId) throws AVException, FileNotFoundException {
    AVQuery<AVObject> query = new AVQuery<AVObject>("_File");
    AVObject object = query.get(objectId);
    if (object != null && !AVUtils.isBlankString(object.getObjectId())) {
      AVFile file = createFileFromAVObject(object);
      return file;
    } else {
      throw new FileNotFoundException("Could not find file object by id:" + objectId);
    }
  }

  /**
   * Construct a AVFile from AVObject.
   * 
   * @param obj The parse object.
   * @return The parse file object.
   * @since 2.0.2
   */
  public static AVFile withAVObject(AVObject obj) {
    if (obj != null && !AVUtils.isBlankString(obj.getObjectId())) {
      AVFile file = createFileFromAVObject(obj);
      return file;
    } else {
      throw new IllegalArgumentException("Invalid AVObject.");
    }
  }

  private static AVFile createFileFromAVObject(AVObject object) {
    AVFile file = new AVFile(object.getObjectId(), object.getString("url"));
    if (object.getMap(ELDERMETADATAKEYFORIOSFIX) != null
        && !object.getMap(ELDERMETADATAKEYFORIOSFIX).isEmpty()) {
      file.metaData.putAll(object.getMap(ELDERMETADATAKEYFORIOSFIX));
    }
    if (object.getMap("metaData") != null) {
      file.metaData.putAll(object.getMap("metaData"));
    }
    file.setObjectId(object.getObjectId());
    file.fileObject = object;
    file.setBucket((String) object.get("bucket"));
    if (!file.metaData.containsKey(FILE_NAME_KEY)) {
      file.metaData.put(FILE_NAME_KEY, object.getString("name"));
    }
    return file;
  }

  /**
   * Creates a new file from local file path. Giving a name with a proper file extension (e.g.
   * ".png") is ideal because it allows AVOSCloud to deduce the content type of the file and set
   * appropriate HTTP headers when it is fetched.
   * 
   * @since 2.0.2
   * @param name The file's name, ideally with extension.
   * @param absoluteLocalFilePath The file's absolute path.
   */
  public static AVFile withAbsoluteLocalPath(String name, String absoluteLocalFilePath)
      throws FileNotFoundException {
    return withFile(name, new File(absoluteLocalFilePath));
  }

  /**
   * Creates a new file from java.io.File object.
   * 
   * @since 2.0.2
   * @param name The file's name, ideally with extension.
   * @param file The file object.
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static AVFile withFile(String name, File file) throws FileNotFoundException {
    if (file == null)
      throw new IllegalArgumentException("null file object.");
    if (!file.exists() || !file.isFile()) {
      throw new FileNotFoundException();
    }
    AVFile avFile = new AVFile();
    avFile.setLocalPath(file.getAbsolutePath());
    avFile.setName(name);

    avFile.dirty = true;
    avFile.name = name;
    byte[] data = AVUtils.readContentBytesFromFile(file);
    if (null != data) {
      avFile.metaData.put(FILE_SUM_KEY, AVUtils.computeMD5(data));
      avFile.metaData.put("size", file.length());
    } else {
      avFile.metaData.put("size", 0);
    }
    AVUser currentUser = AVUser.getCurrentUser();
    avFile.metaData.put("owner", currentUser != null ? currentUser.getObjectId() : "");
    avFile.metaData.put(FILE_NAME_KEY, name);
    return avFile;
  }

  /**
   * Returns the file's metadata map.
   * 
   * @return The file's metadata map.
   * @since 1.3.4
   */
  public HashMap<String, Object> getMetaData() {
    return metaData;
  }

  /**
   * Added meta data to file.
   * 
   * @param key The meta data's key.
   * @param val The meta data's value.
   * @return The old metadata value.
   * @since 1.3.4
   */
  public Object addMetaData(String key, Object val) {
    return metaData.put(key, val);
  }

  /**
   * Returns the metadata value by key.
   * 
   * @param key The metadata key
   * @return The value.
   * @since 1.3.4
   */
  public Object getMetaData(String key) {
    return this.metaData.get(key);
  }

  /**
   * Returns the file size in bytes.
   * 
   * @return File size in bytes
   * @since 1.3.4
   */
  public int getSize() {
    Number size = (Number) getMetaData("size");
    if (size != null)
      return size.intValue();
    else
      return -1;
  }

  /**
   * Returns the file's owner
   * 
   * @return File's owner
   * @since 1.3.4
   */
  public String getOwnerObjectId() {
    return (String) getMetaData("owner");
  }

  /**
   * Remove file meta data.
   * 
   * @param key The meta data's key
   * @return The metadata value.
   * @since 1.3.4
   */
  public Object removeMetaData(String key) {
    return metaData.remove(key);
  }

  /**
   * Clear file metadata.
   */
  public void clearMetaData() {
    this.metaData.clear();
  }

  /**
   * The filename. Before save is called, this is just the filename given by the user (if any).
   * After save is called, that name gets prefixed with a unique identifier.
   * 
   * @return The file's name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * 
   * @return
   */
  public String getOriginalName() {
    return (String) metaData.get(FILE_NAME_KEY);
  }

  /**
   * 
   * @param name
   */
  protected void setName(String name) {
    this.name = name;
  }

  public static String getMimeType(String url) {
    String type = defaultMimeType;
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (extension != null) {
      MimeTypeMap mime = MimeTypeMap.getSingleton();
      type = mime.getMimeTypeFromExtension(extension);
    }
    if (type == null) {
      type = defaultMimeType;
    }
    return type;
  }

  /**
   * Whether the file still needs to be saved.
   * 
   * @return Whether the file needs to be saved.
   */
  public boolean isDirty() {
    return this.dirty;
  }

  /**
   * This returns the url of the file. It's only available after you save or after you get the file
   * from a AVObject.
   * 
   * @return The url of the file.
   */
  public String getUrl() {
    return url;
  }

  private static final String THUMBNAIL_FMT = "?imageView/%d/w/%d/h/%d/q/%d/format/%s";

  /**
   * Returns a thumbnail image url using QiNiu endpoints.
   * 
   * @param scaleToFit
   * @param width
   * @param height
   * @see #getThumbnailUrl(boolean, int, int, int, String)
   * @return
   */
  public String getThumbnailUrl(boolean scaleToFit, int width, int height) {
    return this.getThumbnailUrl(scaleToFit, width, height, 100, "png");
  }

  /**
   * 返回缩略图URl 这个服务仅仅适用于保存在Qiniu的图片
   * 
   * @param scaleToFit Whether to scale the image
   * @param width The thumbnail image's width
   * @param height The thumbnail image'height
   * @param quality The thumbnail image quality in 1 - 100.
   * @param fmt The thumbnail image format such as 'jpg','gif','png','tif' etc.
   * @return
   */
  public String getThumbnailUrl(boolean scaleToFit, int width, int height, int quality, String fmt) {
    if (AVOSCloud.getStorageType() != StorageType.StorageTypeQiniu) {
      throw new UnsupportedOperationException("We only support this method for qiniu storage.");
    }
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Invalid width or height.");
    }
    if (quality < 1 || quality > 100) {
      throw new IllegalArgumentException("Invalid quality,valid range is 0-100.");
    }
    if (fmt == null || AVUtils.isBlankString(fmt.trim())) {
      fmt = "png";
    }
    int mode = scaleToFit ? 2 : 1;
    String resultUrl =
        this.getUrl() + String.format(THUMBNAIL_FMT, mode, width, height, quality, fmt);
    return resultUrl;
  }

  void setUrl(String url) {
    this.url = url;
  }

  void setLocalPath(String localPath) {
    this.localPath = localPath;
  }

  /**
   * Saves the file to the AVOSCloud cloud synchronously.
   * 
   * @throws AVException
   */
  public void save() throws AVException {
    cancelUploadIfNeed();

    uploader = this.getUploader(null, null);

    AVException exception = uploader.doWork();
    if (exception != null)
      throw exception;
  }

  private void cancelUploadIfNeed() {
    if (uploader != null)
      uploader.cancel(true);
  }

  // ================================================================================
  // Private fields
  // ================================================================================

  /**
   * need call this if upload success
   * 
   * @param uniqueName
   * @param url
   */
  void handleUploadedResponse(String objectId, String uniqueName, String url) {
    this.dirty = false;
    this.objectId = objectId;
    this.fileObject = AVObject.createWithoutData("_File", objectId);
    this.name = uniqueName;
    this.url = url;
  }

  /**
   * @see AVObject#delete()
   * @throws AVException
   * @since 1.3.4
   */
  public void delete() throws AVException {
    if (getFileObject() != null)
      getFileObject().delete();
    else
      throw AVErrorUtils.createException(AVException.FILE_DELETE_ERROR,
          "File object is not exists.");
  }

  String mimeType() {
    if (!AVUtils.isBlankString(name)) {
      return getMimeType(name);
    } else if (!AVUtils.isBlankString(url)) {
      return getMimeType(url);
    }
    return defaultMimeType;
  }

  static String className() {
    return "File";
  }

  protected AVUploader getUploader(final SaveCallback saveCallback,
      final ProgressCallback progressCallback) throws AVException {
    if (this.objectId == null && !AVUtils.isBlankString(url)) {
      if (UrlValidator.getInstance().isValid(url))
        return new UrlDirectlyUploader(this, saveCallback, progressCallback);
      else {
        throw new AVException(AVException.INVALID_FILE_URL, "Invalid File URL");
      }
    }
    switch (AVOSCloud.getStorageType()) {
      case StorageTypeQiniu:
        return new QiniuUploader(this, saveCallback, progressCallback);
      case StorageTypeS3:
        return new S3Uploader(this, saveCallback, progressCallback);
      default:
        LogUtil.log.e();
        break;
    }
    return null;
  }

  public String getBucket() {
    return this.bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  /**
   * 此函数对应的是获得“用户传过来的本地文件”的具体内容
   * 
   * @return
   */
  @JSONField(serialize = false)
  protected byte[] getLocalFileData() {
    if (!AVUtils.isBlankString(localPath)) {
      return AVUtils.readContentBytesFromFile(new File(localPath));
    } else if (data != null && data.length > 0) {
      return data;
    }
    return null;
  }


  /**
   * 获取AVFile的ACL
   * 
   * @since 2.6.9
   */
  public AVACL getACL() {
    return acl;
  }

  public void setACL(AVACL acl) {
    this.acl = acl;
  }

  protected JSONObject toJSONObject() {
    JSONObject object = new JSONObject();
    Map<String, Object> data = AVUtils.mapFromFile(this);
    data.put("url", url);
    return object;
  }
}

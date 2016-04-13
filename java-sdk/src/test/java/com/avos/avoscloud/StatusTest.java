package com.avos.avoscloud;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import junit.framework.TestCase;

public class StatusTest extends TestCase {
  static private int max = 20;

  @Override
  public void setUp() {
    TestApp.init();
  }

  private boolean contains(List<AVStatus> list, AVStatus object) {
    for (AVStatus item : list) {
      if (object.getObjectId().equals(item.getObjectId())) {
        return true;
      }
    }
    return false;
  }

  public void testSimpleStatusPost() throws Exception {
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();

    Map<String, Object> data = new HashMap<String, Object>();
    data.put("images", "some new text");
    data.put("messages", "your link to the text");
    final AVStatus status = AVStatus.createStatusWithData(data);
    status.send();
  }

  public void testStatusPost() throws Exception {

    AVObject statusObject = new AVObject("StatusTestPointer");
    statusObject.put("value", 123);
    statusObject.save();

    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max) + System.currentTimeMillis());
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max) + System.currentTimeMillis());
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);


    // publish status to a's follower, the b
    AVUser.logIn(userA.getUsername(), passwordA);
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("text", "some new text");
    data.put("link", "your link to the text");
    final AVStatus status = AVStatus.createStatusWithData(data);
    status.put("pointer", statusObject);
    status.send();

    // b should be able to query
    // publish another one,
    final AVStatus yas = AVStatus.createStatus("aaa", "bbbb");
    AVStatus.sendPrivateStatus(yas, userB.getObjectId());

    // get status of a
    AVUser.logIn(userA.getUsername(), passwordA);
    Thread.sleep(5 * 1000);

    AVStatusQuery statusQuery = AVStatus.statusQuery(AVUser.getCurrentUser());
    statusQuery.setLimit(100);
    List<AVStatus> parseObjects = statusQuery.find();
    if (parseObjects.size() > 0) {
      assertTrue(contains(parseObjects, status));
    }
  }

  public void testStatusPostWithQuery() throws Exception {

    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    // two followers
    final AVUser userFollower = new AVUser();
    userFollower.setUsername(AVUtils.getRandomString(max));
    String passwordFollower = AVUtils.getRandomString(max);
    userFollower.setPassword(passwordFollower);
    userFollower.signUp();
    userFollower.follow(userObjectId);

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max));
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);


    // publish status to a's follower, the b and only b.
    AVUser.logIn(userA.getUsername(), passwordA);
    AVQuery query = AVUser.getQuery();
    query.whereEqualTo("objectId", userB.getObjectId());
    final AVStatus status = AVStatus.createStatus("aaa", "bbbb");
    status.setQuery(query);
    status.send();

    // only b is able to query
    // get status of a
    AVUser.logIn(userB.getUsername(), passwordB);
    Thread.sleep(10 * 1000);
    AVStatusQuery statusQuery = AVStatus.statusQuery(AVUser.getCurrentUser());
    statusQuery.setLimit(100);
    List<AVStatus> parseObjects = statusQuery.find();

    if (parseObjects.size() > 0) {
      assertTrue(contains(parseObjects, status));
    }

    // follower should not receive the status.
    AVUser.logIn(userFollower.getUsername(), passwordFollower);
    statusQuery = AVStatus.statusQuery(AVUser.getCurrentUser());
    statusQuery.setLimit(100);
    parseObjects = statusQuery.find();
    if (parseObjects.size() > 0) {
      assertTrue(contains(parseObjects, status));
    }
  }

  public void testStatusPostAndGet() throws Exception {

    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();

    // publish status to a's follower, the b
    AVUser.logIn(userA.getUsername(), passwordA);
    final String message = "status message";
    final String image = "status image url";
    final AVStatus status = AVStatus.createStatus(image, message);
    status.send();
    Thread.sleep(1000 * 5);
    // query the status now.
    AVStatus statusObject = AVStatus.getStatusWithId(status.getObjectId());
    assertEquals(statusObject.getCreatedAt(), status.getCreatedAt());
    assertEquals(statusObject.getObjectId(), status.getObjectId());
    assertEquals(statusObject.getMessage(), message);
    assertEquals(statusObject.getImageUrl(), image);
    assertTrue(statusObject.getSource() instanceof AVUser);
  }

  public void testStatusDelete() throws Exception {
    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();

    final AVStatus status = AVStatus.createStatus("image", "to my followers.");
    status.send();

    Thread.sleep(100);
    // remove it
    AVStatus.deleteStatusWithID(status.getObjectId());

    AVStatusQuery statusQuery = AVStatus.statusQuery(AVUser.getCurrentUser());
    statusQuery.setLimit(100);
    statusQuery.whereEqualTo("objectId", status.getObjectId());
    List<AVStatus> parseObjects = statusQuery.find();
    assertTrue(parseObjects.isEmpty());
  }

  public void testStatusInbox() throws Exception {
    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    String usernameA = AVUtils.getRandomString(max);
    String passwordA = AVUtils.getRandomString(max);
    userA.setUsername(usernameA);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max));
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);

    // publish status to a's follower, the b
    AVUser.logIn(usernameA, passwordA);
    final AVStatus status = AVStatus.createStatus("aaa", "bbbb");
    status.send();

    // b should be able to query
    AVUser.logOut();
    AVUser.logIn(userB.getUsername(), passwordB);

    // async, so have to make sure the status are available.
    Thread.sleep(1000 * 5);
    AVStatusQuery statusQuery =
        AVStatus.inboxQuery(AVUser.getCurrentUser(), AVStatus.INBOX_TYPE.TIMELINE.toString());
    statusQuery.setLimit(10);
    statusQuery.addDescendingOrder("createdAt");
    List<AVStatus> parseObjects = statusQuery.find();
    assertTrue(contains(parseObjects, status));

    AVStatus.getStatusWithId(status.getObjectId());

    AVUser.logOut();
  }

  public void testStatusIterate() throws Exception {
    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max));
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userA.getObjectId());

    // publish status to a's follower, the b
    AVUser.logIn(userA.getUsername(), passwordA);
    final int count = 10;
    final int round = 3;
    for (int i = 0; i < round * count; ++i) {
      String message = String.format("message %d", i);
      String image = String.format("image %d", i);
      final int value = i;
      final AVStatus s = AVStatus.createStatus(image, message);
      s.send();
    }

    // b should be able to query
    AVUser.logIn(userB.getUsername(), passwordB);
    Thread.sleep(5 * 1000);
    AVStatusQuery query =
        AVStatus.inboxQuery(AVUser.getCurrentUser(), AVStatus.INBOX_TYPE.TIMELINE.toString());
    query.setLimit(30);
    query.orderByDescending("createdAt");
    List<AVStatus> parseObjects = query.find();
    if (parseObjects.size() > 0) {
      for (AVStatus object : parseObjects) {
        String messageNumStr = object.getMessage().split(" ")[1];
        String imageNumStr = object.getImageUrl().split(" ")[1];
        assertEquals(messageNumStr, imageNumStr);
      }
    }
  }

  public void testStatusPostWithSimpleQuery() throws Exception {

    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();

    AVStatus status = AVStatus.createStatus("test image", "test message");
    AVQuery query = AVQuery.getQuery("_Follower");
    query.selectKeys(Arrays.asList("follower"));
    query.whereEqualTo("user", userA);
    status.setQuery(query);
    status.send();
  }


  // for sample code.
  public void testPostWithNullQuery() throws Exception {
    AVUser.logIn("zengzhu", "12345678");
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("text", "we have new website, take a look!");
    data.put("link", "http://avoscloud.com");
    AVStatus status = AVStatus.createStatusWithData(data);
    status.setInboxType("system");

    status.send();

    AVStatusQuery query = AVStatus.statusQuery(AVUser.getCurrentUser());
    query.setInboxType("system");
    query.setLimit(50);
    List<AVStatus> result = query.find();
    assertNotNull(result);
    assertTrue(result.size() > 0);
    if (result.size() > 0) {
      assertEquals("system", result.get(0).getInboxType());
    }
  }

  public void testStatusQuery() throws Exception {

    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    userA.setUsername(AVUtils.getRandomString(max));
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max));
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);


    // publish status to a's follower, the b
    AVUser.logIn(userA.getUsername(), passwordA);
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("text", "some new text");
    data.put("link", "your link to the text");
    final AVStatus status = AVStatus.createStatusWithData(data);
    status.send();

    // b should be able to query
    // publish another one,
    final AVStatus yas = AVStatus.createStatus("try text", "this is a status message");
    AVStatus.sendPrivateStatus(yas, userB.getObjectId());

    // get status of a
    AVUser.logIn(userA.getUsername(), passwordA);
    Thread.sleep(5 * 1000);
    LogUtil.avlog.d("current userId:" + AVUser.getCurrentUser().getObjectId());
    LogUtil.avlog.d("target status:" + yas.getObjectId());
    AVStatusQuery query = AVStatus.statusQuery(AVUser.getCurrentUser());
    query.setLimit(100);
    query.setSinceId(0);
    List<AVStatus> parseObjects = query.find();
    if (parseObjects.size() > 0) {
      assertTrue(contains(parseObjects, yas));
    }
    // AVUser.logIn(userB.getUsername(), passwordB);
    query.setInboxType(AVStatus.INBOX_TYPE.PRIVATE.toString());
    parseObjects = query.find();
    if (parseObjects.size() > 0) {
      for (AVStatus s : parseObjects) {
        LogUtil.avlog.d(s.getInboxType() + ":" + s.getObjectId());
      }
    }
    AVStatusQuery inboxQuery = AVStatus.inboxQuery(userB, AVStatus.INBOX_TYPE.PRIVATE.toString());
    parseObjects = inboxQuery.find();
    assertTrue(contains(parseObjects, yas));
    assertTrue(parseObjects.get(0).getMessageId() > 0);
  }

  public void testFollowAttributes() throws Exception {
    // setup follow relation at first. b follows a
    final AVUser userA = new AVUser();
    String userAUsername = AVUtils.getRandomString(max) + System.currentTimeMillis();
    userA.setUsername(userAUsername);
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max) + System.currentTimeMillis());
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("item", "Great Dadin");
    attributes.put("count", 12);
    userB.follow(userObjectId, attributes);
    AVQuery<AVUser> query = userB.followeeQuery(AVUser.class);
    query.whereEqualTo("count", 12);
    query.include("followee");
    List<AVUser> results = query.find();
    assertEquals(1, results.size());
    assertEquals(userA.getUsername(), results.get(0).getUsername());
  }

  public void testDeleteInboxStatus() throws Exception {
    final AVUser userA = new AVUser();
    String userAUsername = AVUtils.getRandomString(max) + System.currentTimeMillis();
    userA.setUsername(userAUsername);
    String passwordA = AVUtils.getRandomString(max);
    userA.setPassword(passwordA);
    userA.signUp();
    String userObjectId = userA.getObjectId();

    final AVUser userB = new AVUser();
    userB.setUsername(AVUtils.getRandomString(max) + System.currentTimeMillis());
    String passwordB = AVUtils.getRandomString(max);
    userB.setPassword(passwordB);
    userB.signUp();
    userB.follow(userObjectId);

    AVUser.logIn(userAUsername, passwordA);
    Map<String, Object> data = new HashMap<String, Object>();
    String speicalString = AVUtils.getRandomString(15);
    data.put("text", speicalString);
    data.put("link", "http://avoscloud.com");

    AVStatus status = AVStatus.createStatusWithData(data);
    status.setInboxType("system");
    status.send();
    Thread.sleep(5000);
    AVStatusQuery query = AVStatus.inboxQuery(userB, "system");
    query.whereEqualTo("text", speicalString);
    AVStatus result = query.getFirst();

    AVStatus.deleteInboxStatus(result.getMessageId(), "system", userB);
  }

  // A--->B--->C;
  public void testAVFriendShipQuery() throws Exception {
    String prefix = "lbt05";
    String userAUsername = prefix + AVUtils.getRandomString(10) + System.currentTimeMillis();
    String userBUsername = prefix + AVUtils.getRandomString(10) + System.currentTimeMillis();
    String userCUserName = prefix + AVUtils.getRandomString(10) + System.currentTimeMillis();

    String userAPassword = AVUtils.getRandomString(8);
    String userBPassword = AVUtils.getRandomString(8);
    String userCPassword = AVUtils.getRandomString(8);

    AVUser userC = new AVUser();
    userC.setUsername(userCUserName);
    userC.setPassword(userCPassword);
    userC.signUp();

    AVUser userB = new AVUser();
    userB.setUsername(userBUsername);
    userB.setPassword(userBPassword);
    userB.signUp();

    userB.follow(userC.getObjectId());

    AVUser userA = new AVUser();
    userA.setUsername(userAUsername);
    userA.setPassword(userAPassword);
    userA.signUp();
    userA.follow(userB.getObjectId());

    userB = AVUser.logIn(userBUsername, userBPassword);

    List<AVUser> followees = userB.followeeQuery(AVUser.class).find();
    assertEquals(1, followees.size());
    assertEquals(userC.getObjectId(), followees.get(0).getObjectId());
    List<AVUser> followers = userB.followerQuery(AVUser.class).find();
    assertEquals(1, followers.size());
    assertEquals(userA.getObjectId(), followers.get(0).getObjectId());
  }

  public void testNestedStatus() throws Exception {
    AVUser.logIn("zengzhu", "12345678");

    AVStatus firstStatus = AVStatus.createStatus(null, "test message");
    firstStatus.send();

    AVObject statusTestObject = new AVObject("statusTestObject");
    statusTestObject.put("int", 123);
    statusTestObject.put("status", firstStatus);

    statusTestObject.save();
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("avObject", statusTestObject);

    final AVStatus status = AVStatus.createStatusWithData(data);
    status.send();

    AVQuery<AVObject> query = new AVQuery<AVObject>("statusTestObject");
    query.include("status");
    AVObject result = query.getFirst();
    AVStatus includedStatus = result.getAVObject("status", AVStatus.class);
    assertNotNull(includedStatus.getSource());
    assertNotNull(includedStatus.getMessage());
    assertNotNull(includedStatus.getInboxType());

    String str =
        JSON.toJSONString(includedStatus, ObjectValueFilter.instance,
            SerializerFeature.WriteClassName, SerializerFeature.DisableCircularReferenceDetect);
    AVStatus parsedStatus = JSON.parseObject(str, AVStatus.class);
    assertEquals("test message", parsedStatus.getMessage());
    assertTrue(!AVUtils.isBlankString(parsedStatus.getObjectId()));
    assertEquals("default", parsedStatus.getInboxType());
  }
}

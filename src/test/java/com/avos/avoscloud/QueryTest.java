package com.avos.avoscloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.avos.avoscloud.data.Armor;

import junit.framework.TestCase;

public class QueryTest extends TestCase {
  static String tableName = "GameScoreFromAndroid";
  static String commetTable = "androidComment";
  static String postTable = "androidPost";
  static String addTable = "androidAdd";

  @Override
  public void setUp() {
    TestApp.init();
  }

  public void testBasicQuery() throws Exception {
    AVQuery query = new AVQuery(tableName);
    assertTrue(query.count() > 0);
  }

  public void testGetInBackgroundQuery() throws Exception {
    AVObject gameScore = new AVObject(tableName);
    Random r = new Random();
    final int targetValue = r.nextInt();
    gameScore.put("score", targetValue);
    final String targetString = "Sean Plott" + targetValue;
    gameScore.put("playerName", targetString);
    gameScore.put("date", new Date());

    byte[] data = {1, 2, 3, 4, 5};
    gameScore.put("byteArray", data);

    gameScore.save();
    assertTrue(!gameScore.getObjectId().isEmpty());

    AVQuery<AVObject> query = new AVQuery<AVObject>(tableName);
    AVObject object = query.get(gameScore.getObjectId());
    assertEquals(gameScore.getObjectId(), object.objectId);
  }

  public void testAVQueryOrSubqueries() throws Exception {
    final String key = "score";
    final int min = 0;
    final int max = 6000000;
    AVQuery lotsOfWins = new AVQuery(tableName);
    lotsOfWins.whereGreaterThan(key, max);

    AVQuery fewWins = new AVQuery(tableName);
    fewWins.whereLessThan(key, min);

    List list = new ArrayList<AVQuery>();
    list.add(lotsOfWins);
    list.add(fewWins);
    AVQuery<AVObject> query = AVQuery.or(list);
    query.orderByDescending(key);
    query.addDescendingOrder("createdAt");
    List<AVObject> parseObjects = query.find();
    int temp = Integer.MAX_VALUE;
    Date tempDate = new Date(System.currentTimeMillis() + 3000);
    for (AVObject item : parseObjects) {
      int value = item.getInt(key);
      assertTrue(temp > value);
      temp = value;
      tempDate = item.getCreatedAt();
      assertTrue(value >= max || value <= min);
    }
  }

  public void testAVQueryEndsWith() throws Exception {
    final String key = "playerName";
    final String suffix = "7";
    AVQuery query = new AVQuery(tableName);
    query.whereEndsWith(key, suffix);
    List<AVObject> parseObjects = query.find();

    for (AVObject item : parseObjects) {
      String value = item.getString(key);
      assertTrue(value.lastIndexOf(suffix) == value.length() - suffix.length());
    }
  }

  public void testAVQueryStartsWith() throws Exception {
    final String key = "playerName";
    final String prefix = "Sean Plott17";
    AVQuery query = new AVQuery(tableName);
    query.whereStartsWith(key, prefix);
    List<AVObject> parseObjects = query.find();
    for (AVObject item : parseObjects) {
      String value = item.getString(key);
      assertTrue(value.indexOf(prefix) == 0);
    }
  }

  public void testAVQueryGreaterThan() throws Exception {
    final String key = "score";
    final int value = 18887000;
    AVQuery query = new AVQuery(tableName);
    query.whereGreaterThan(key, value);
    List<AVObject> parseObjects = query.find();
    for (AVObject item : parseObjects) {
      double result = item.getDouble(key);
      assertTrue(result > value);
    }
  }

  public void testAVQueryLessThan() throws Exception {
    final String key = "score";
    final int value = 0;
    AVQuery query = new AVQuery(tableName);
    query.whereLessThan(key, value);
    List<AVObject> parseObjects = query.find();
    for (AVObject item : parseObjects) {
      double result = item.getDouble(key);
      assertTrue(result < value);
    }
  }

  public void testConstraintQuery() throws Exception {
    AVObject person1 = new AVObject("Person");
    person1.put("gender", "Female");
    person1.put("name", "Cake");
    person1.save();
    assertFalse(person1.getObjectId().isEmpty());


    AVObject something1 = new AVObject("Something");
    something1.put("belongTo", "Cake");
    something1.put("city", "ChangDe");
    something1.save();
    assertFalse(something1.getObjectId().isEmpty());

    AVQuery q1 = new AVQuery("Person");
    q1.whereEqualTo("gender", "Female");


    AVQuery q2 = new AVQuery("Something");
    q2.whereMatchesKeyInQuery("belongTo", "name", q1);
    List<AVObject> list = q2.find();
    for (AVObject obj : list) {
      String value = obj.getString("belongTo");
      assertTrue(value.equals("Cake"));
    }

    AVQuery q3 = new AVQuery("Something");
    q3.whereDoesNotMatchKeyInQuery("belongTo", "Cake", q1);
    list = q3.find();
    for (AVObject obj : list) {
      String value = obj.getString("belongTo");
      assertTrue(value.equals("Cake"));
    }
  }

  public void testRelationalQuery() throws Exception {
    {
      AVObject comment1 = new AVObject(commetTable);
      AVObject post1 = new AVObject(postTable);
      AVObject add = new AVObject(addTable);

      post1.put("content", "This is a content");
      comment1.put("post", post1);
      comment1.put("add", add);

      post1.save();
      add.save();
      comment1.save();
      assertFalse(post1.getObjectId().isEmpty());
      assertFalse(add.getObjectId().isEmpty());
      assertFalse(comment1.getObjectId().isEmpty());
    }

    AVObject comment1 = new AVObject(commetTable);
    AVObject post1 = new AVObject(postTable);
    AVObject add = new AVObject(addTable);

    comment1.put("add", add);
    comment1.put("post", post1);

    post1.save();
    add.save();
    comment1.save();


    {
      AVQuery q1 = new AVQuery(commetTable);
      q1.whereEqualTo("post", post1);
      AVObject result = q1.getFirst();
      assertTrue(comment1.getObjectId().equals(result.getObjectId()));
    }


    {
      AVQuery q2 = new AVQuery(postTable);
      q2.whereExists("content");


      AVQuery q3 = new AVQuery(commetTable);
      q3.whereMatchesQuery("post", q2);

      List<AVObject> results = q3.find();
      assertTrue(results.size() > 0);

      for (AVObject obj : results) {
        AVObject post = (AVObject) obj.get("post");
        // Assert.assertTrue(post != null);
      }
    }


    {
      AVQuery q2 = new AVQuery(postTable);
      q2.whereExists("content");
      AVQuery q3 = new AVQuery(commetTable);
      q3.whereDoesNotMatchQuery("post", q2);

      List<AVObject> list = q3.find();
      assertTrue(list.size() > 0);

      for (AVObject obj : list) {
        AVObject post = (AVObject) obj.get("post");
        // Assert.assertTrue(post != null);
        // Assert.assertTrue(post.get("content") != null);
      }
    }
  }

  public void testNearGeoPoint() throws Exception {
    String className = "placeObject";
    String field = "location";

    AVObject placeObject = new AVObject(className);
    AVGeoPoint point = new AVGeoPoint(40.0, -30.0);
    placeObject.put(field, point);
    placeObject.save();

    // 1
    {
      AVGeoPoint userGeoPoint = new AVGeoPoint(41.0, -31.0);
      AVQuery query = new AVQuery(className);
      query.whereNear(field, userGeoPoint);
      query.setLimit(10);

      final List<AVObject> result = new ArrayList<AVObject>();
      result.addAll(query.find());
      assertTrue(result.size() > 0);
      AVObject first = result.get(0);
      AVGeoPoint p = first.getAVGeoPoint(field);
      LogUtil.log.d(String.format("1 nearGeoPoint %f %f", p.getLatitude(), p.getLongitude()));
    }

    // 2
    {
      AVGeoPoint userGeoPoint = new AVGeoPoint(41, -30.0);
      AVQuery query = new AVQuery(className);
      query.whereWithinMiles(field, userGeoPoint, 100, 1);
      query.setLimit(10);

      final List<AVObject> result = new ArrayList<AVObject>();
      result.addAll(query.find());
      assertTrue(result.size() > 0);
      AVObject first = result.get(0);
      AVGeoPoint p = first.getAVGeoPoint(field);
      LogUtil.log.d(String.format("2 withinMiles %f %f", p.getLatitude(), p.getLongitude()));
    }

    // 3
    {
      AVGeoPoint userGeoPoint = new AVGeoPoint(41, -30.0);
      AVQuery query = new AVQuery(className);
      query.whereWithinKilometers(field, userGeoPoint, 3000, 1);
      query.setLimit(10);

      final List<AVObject> result = new ArrayList<AVObject>();
      result.addAll(query.find());
      assertTrue(result.size() > 0);
      AVObject first = result.get(0);
      AVGeoPoint p = first.getAVGeoPoint(field);
      LogUtil.log.d(String.format("3 withinKilometers %f %f", p.getLatitude(), p.getLongitude()));
    }

    // 4
    {
      AVGeoPoint userGeoPoint = new AVGeoPoint(41, -30.0);
      AVQuery query = new AVQuery(className);
      query.whereWithinRadians(field, userGeoPoint, 3000, 0);
      query.setLimit(10);

      final List<AVObject> result = new ArrayList<AVObject>();
      result.addAll(query.find());
      assertTrue(result.size() > 0);
      AVObject first = result.get(0);
      AVGeoPoint p = first.getAVGeoPoint(field);
      LogUtil.log.d(String.format("4 withinRadians %f %f", p.getLatitude(), p.getLongitude()));
    }

    // 5
    {
      AVGeoPoint swOfSF = new AVGeoPoint(35, -62.526398);
      AVGeoPoint neOfSF = new AVGeoPoint(52.822802, -20.373962);
      AVQuery query = new AVQuery(className);
      query.setLimit(10);
      query.whereWithinGeoBox(field, swOfSF, neOfSF);

      final List<AVObject> result = new ArrayList<AVObject>();
      result.addAll(query.find());
      assertTrue(result.size() > 0);
      AVObject first = result.get(0);
      AVGeoPoint p = first.getAVGeoPoint(field);
      LogUtil.log.d(String.format("5 withinGeoBoxFromSouthwest %f %f", p.getLatitude(),
          p.getLongitude()));
    }
  }

  public void testZZZInclude() throws Exception {
    AVQuery query = new AVQuery(commetTable);
    query.orderByDescending("createdAt");
    query.setLimit(10);
    query.include("post");
    query.include("add");
    List<AVObject> commentList = query.find();
    assertTrue(commentList.size() > 0);
    for (AVObject comment : commentList) {
      // This does not require a network access.
      AVObject post = comment.getAVObject("post");
      assertFalse(post.getObjectId().isEmpty());

      AVObject byhand = comment.getAVObject("add");
      assertFalse(byhand.getObjectId().isEmpty());
    }
  }

  public void testSelectKeys() throws Exception {

    final String table = "keysTestAndroid";
    AVObject object = new AVObject(table);
    object.put("a", "a");
    object.put("b", "b");
    object.put("c", "c");
    object.put("d", "d");
    object.save();

    AVQuery query = new AVQuery(table);
    query.selectKeys(Arrays.asList("b", "c"));
    AVObject result = query.getFirst();

    String value = (String) result.get("a");
    assertTrue("invalid value", value == null);

    value = (String) result.get("b");
    assertTrue("invalid value", value.equalsIgnoreCase("b"));

    value = (String) result.get("c");
    assertTrue("invalid value", value.equalsIgnoreCase("c"));
    result.delete();
  }

  public void testEmptyQuery() throws Exception {
    AVQuery query = new AVQuery("emptyTest");
    List<AVObject> parseObjects = query.find();
    assertTrue("invalid object array", parseObjects.isEmpty());
  }

  public void testAddWhereQuery() throws Exception {
    AVQuery query = new AVQuery("AddWhereQueryTable");
    query.whereGreaterThan("value", 1);
    query.whereLessThan("value", 3);
    query.whereEqualTo("value", 2);
    query.whereExists("objectId");

    AVQuery query2 = new AVQuery("AddWhereQueryTable");
    query2.whereEqualTo("value", 4);
    List<AVQuery<AVObject>> orQueryList = new ArrayList<AVQuery<AVObject>>();
    orQueryList.add(query);
    orQueryList.add(query2);

    AVQuery orQuery = AVQuery.or(orQueryList);
    assertEquals(2, orQuery.find().size());
  }

  public void testAddWhereQueryWithoutEquals() throws Exception {
    AVQuery query = new AVQuery("AddWhereQueryTable");
    query.whereGreaterThan("value", 1);
    query.whereLessThan("value", 3);
    query.whereExists("objectId");

    AVQuery query2 = new AVQuery("AddWhereQueryTable");
    query2.whereEqualTo("value", 4);
    List<AVQuery<AVObject>> orQueryList = new ArrayList<AVQuery<AVObject>>();
    orQueryList.add(query);
    orQueryList.add(query2);

    AVQuery orQuery = AVQuery.or(orQueryList);
    assertEquals(2, orQuery.find().size());
  }

  public void testJavaObjectTest() throws Exception {
    AVQuery query = new AVQuery(tableName);
    Calendar time = Calendar.getInstance();
    time.setTime(new Date());
    time.add(Calendar.DATE, -6);
    query.whereGreaterThan("createdAt", time.getTime());
    List<AVObject> result = query.find();
    assertTrue(result.size() > 0);
  }

  public void testCloudQuery() throws Exception {
    AVCloudQueryResult result = AVQuery.doCloudQuery("select * from ObjectUnitTestArmor");
    assertTrue(result.getResults().size() > 0);
    assertEquals("ObjectUnitTestArmor", result.getResults().get(0).getClassName());
    result = AVQuery.doCloudQuery("select count(*) from ObjectUnitTestArmor", Armor.class);
    assertTrue(result.getCount() > 0);
  }

  public void testCloudQueryWithVal() throws Exception {
    AVCloudQueryResult result =
        AVQuery.doCloudQuery("select * from ObjectUnitTestArmor where durability = ?", Armor.class,
            100);
    assertTrue(result.getResults().size() > 0);
    assertEquals("ObjectUnitTestArmor", result.getResults().get(0).getClassName());
  }

  public void testAVObjectArrayQuery() throws Exception {
    AVObject i = new Armor();
    AVObject j = new Armor();
    i.put("durability", 100);
    j.put("durability", 200);

    i.save();
    j.save();

    AVObject data = new AVObject("ObjectArrayTest");
    data.add("data", i);
    data.add("data", j);
    data.save();


    AVQuery<AVObject> query = new AVQuery<AVObject>("ObjectArrayTest");
    query.whereEqualTo("objectId", data.getObjectId());
    query.include("data");
    List<AVObject> result = query.find();
    for (AVObject item : result) {
      List<Armor> armors = item.getList("data", Armor.class);
      assertEquals(2, armors.size());
      assertEquals(100, armors.get(0).getDurability());
      assertEquals(200, armors.get(1).getDurability());
    }
  }

  public void testWhereMatchesKeyInQuery() throws Exception {
    AVUser.logIn("zengzhu", "12345678");
    String gudongObjectId = AVUser.getCurrentUser().getObjectId();
    AVQuery<AVObject> query = new AVQuery("TestMatchesKeyInQuery");
    AVQuery<AVUser> followeeQuery = AVUser.followerQuery(gudongObjectId, AVUser.class);
    List<AVUser> beforeSaveResult = followeeQuery.find();
    AVUser newUser = new AVUser();
    newUser.setUsername(AVUtils.getRandomString(10) + System.currentTimeMillis());
    newUser.setPassword(AVUtils.getRandomString(10));
    newUser.signUp();
    AVUser.getCurrentUser().follow(gudongObjectId);
    AVObject object = new AVObject("TestMatchesKeyInQuery");
    object.put("user", AVUser.getCurrentUser());
    int randomValue = new Random().nextInt();
    object.put("value", randomValue);
    object.save();

    query.whereMatchesKeyInQuery("user", "follower", followeeQuery);
    List<AVObject> result = query.find();
    assertTrue(beforeSaveResult.size() + 1 >= result.size());
    assertTrue(result.size() > 0);
  }

  public void testWhereMatchesWithModifier() throws Exception {
    AVObject stringMatchObject = new AVObject("StringTest");
    stringMatchObject.put("str",
        AVUtils.getRandomString(5) + "lBtjaVA" + AVUtils.getRandomString(5));
    stringMatchObject.save();
    AVQuery<AVObject> query = new AVQuery<AVObject>("StringTest");
    query.whereMatches("str", ".*lbtjava.*", "-i");
    List<AVObject> reuslt = query.find();
    for (AVObject o : reuslt) {
      assertTrue(o.getString("str").toLowerCase().contains("lbtjava"));
    }
  }


  public void testQueryAnd() throws Exception {
    AVObject firstObject = new AVObject("AndQueryTest");
    firstObject.put("int", 1);
    firstObject.put("str", "value");
    firstObject.save();
    AVObject secondObject = new AVObject("AndQueryTest");
    secondObject.put("int", 2);
    secondObject.put("value", "$%^");
    secondObject.save();
    AVQuery firstQuery = new AVQuery("AndQueryTest");
    firstQuery.whereEqualTo("int", 1);
    AVQuery secondQuery = new AVQuery("AndQueryTest");
    secondQuery.whereNotEqualTo("int", 2);
    // secondQuery.whereEqualTo("str", "value");

    List<AVQuery<AVObject>> queries = new LinkedList<AVQuery<AVObject>>();
    queries.add(firstQuery);
    queries.add(secondQuery);
    AVQuery<AVObject> andQuery = AVQuery.and(queries);
    List<AVObject> parseObjects = andQuery.find();
    assertFalse(parseObjects.isEmpty());
    for (AVObject object : parseObjects) {
      assertEquals(1, object.getInt("int"));
      assertEquals("value", object.getString("str"));
    }
  }

  public void testQerySkip() throws Exception {
    String personalTable = "Person";
    AVObject person1 = new AVObject(personalTable);
    person1.put("gender", "Female");
    person1.put("name", "person1");
    person1.save();

    AVObject person2 = new AVObject(personalTable);
    person2.put("gender", "Female");
    person2.put("name", "person2");
    person2.save();

    AVObject person3 = new AVObject(personalTable);
    person3.put("gender", "Female");
    person3.put("name", "person3");
    person3.save();

    AVQuery<AVObject> query1 = new AVQuery<AVObject>(personalTable);
    query1.setSkip(2);
    query1.setLimit(1);
    query1.orderByDescending("createdAt");
    List<AVObject> parseObjects = query1.find();
    assertNotNull(parseObjects);
    assertEquals(parseObjects.get(0).getString("name"), "person1");

    query1.setSkip(0);
    parseObjects = query1.find();
    assertNotNull(parseObjects);
    assertEquals(parseObjects.get(0).getString("name"), "person3");
  }
}

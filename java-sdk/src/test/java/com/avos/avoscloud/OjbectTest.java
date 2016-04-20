package com.avos.avoscloud;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.avos.avoscloud.data.Armor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class OjbectTest extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public OjbectTest(String testName) {
    super(testName);
  }

  @Override
  public void setUp() {
    TestApp.init();
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(OjbectTest.class);
  }

  static private String testTableName = "GameScoreFromAndroid";
  static private double bigObjectValue = 123;

  public void testBasicPutGet() throws Exception {
    AVObject gameScore = new AVObject(testTableName);
    final int targetValue = 1337;
    gameScore.put("score", targetValue);
    int value = gameScore.getInt("score");
    assertTrue(value == targetValue);

    final String targetString = "Sean Plott";
    gameScore.put("playerName", targetString);
    String stringValue = gameScore.getString("playerName");
    assertTrue(stringValue == targetString);
  }

  public void testDatePutGet() {
    AVObject gameScore = new AVObject(testTableName);
    Date date = new Date();
    gameScore.put("date", date);
    Date result = gameScore.getDate("date");
    String a = date.toString();
    String b = result.toString();
    assertTrue(a.compareToIgnoreCase(b) == 0);
  }

  public void testObjectQueryConcurrency() {
    final AVObject gameScore = new AVObject(testTableName);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failCount = new AtomicInteger(0);
    final int targetValue = 1337;
    gameScore.put("score", targetValue);

    final Armor armor = new Armor();
    armor.setDisplayName("Golden Shit");
    armor.setBroken(false);

    final AVObject androidTestObject = new AVObject("AndroidTestSaveObject");
    androidTestObject.put("age", "999");
    androidTestObject.put("email", "hahah@hh.com");
    androidTestObject.put("name", "lbt05");
    try {
      gameScore.save();
      armor.save();
      androidTestObject.save();
      ExecutorService threadPool = Executors.newFixedThreadPool(5);
      List<Future<Void>> futures = new LinkedList<Future<Void>>();
      List<AVObject> objects = new LinkedList<AVObject>();

      for (int i = 0; i < 25; i++) {
        final int d = i;
        Future<Void> future = threadPool.submit(new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            switch (d % 3) {
              case 0:
                AVQuery query = AVQuery.getQuery(gameScore.getClassName());
                try {
                  query.get(gameScore.getObjectId());
                  successCount.incrementAndGet();
                } catch (AVException e) {
                  failCount.incrementAndGet();
                }
                return null;
              case 1:
                query = AVQuery.getQuery(Armor.class);
                try {
                  query.get(armor.getObjectId());
                  successCount.incrementAndGet();
                } catch (AVException e) {
                  failCount.incrementAndGet();
                }
                return null;
              case 2:
                query = AVQuery.getQuery(androidTestObject.getClassName());
                try {
                  query.get(androidTestObject.getObjectId());
                  successCount.incrementAndGet();
                } catch (AVException e) {
                  failCount.incrementAndGet();
                }
                return null;
              default:
                return null;
            }
          }
        });
        futures.add(future);
      }
      Thread.sleep(1000);
      for (Future<Void> future : futures) {
        future.get();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    int count = successCount.intValue() + failCount.intValue();
    // check local cache, no cache.
    LogUtil.log.d("errorTimes:" + failCount.intValue());
    assertEquals(25, count);
  }

  public void testObjectDataTypeASave() throws Exception {
    final int intValue = 10;
    Number number = new Double(bigObjectValue);
    final String string = String.format("this is a value for test: %d", number.intValue());
    final Date date = new Date();
    final byte[] data = string.getBytes("UTF-8");
    final List<String> array = new LinkedList<String>();
    for (int i = 0; i < 10; ++i) {
      array.add(Integer.toString(i));
    }
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("string", string);
    map.put("number", number);
    map.put("null", null);

    JSON json = (JSON) JSON.toJSON(map);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("1", 1);
    jsonObject.put("2", 2);

    org.json.JSONObject jsonJsonObje = new org.json.JSONObject();
    jsonJsonObje.put("1", 12);

    AVObject object = new AVObject("androidBigObject");
    object.put("myNumber", new Double(123));
    object.put("myLong", new Long(123456));
    object.put("myfloat", 12.30f);
    object.put("myInt", 123);
    object.put("myShort", (short) 127);
    object.put("myString", string);
    object.put("myDate", date);
    object.put("myData", data);
    object.put("myArray", array);
    object.put("myDictionary", map);
    object.put("myNull", null);
    object.put("intValue", intValue);
    object.put("myJson", json);
    object.put("myJsonObject", jsonObject);
    object.put("orgjsonObject", jsonJsonObje);
    object.put("myObject", new Object() {
      String a = "java";
      String b = "test";

      public String getB() {
        return b;
      }

      @Override
      public String toString() {
        return "fuck";
      }
    });

    System.out.println(JSON.toJSONString(new Object() {
      String a = "java";
      String b = "test";

      public String getB() {
        return b;
      }

      @Override
      public String toString() {
        return "fuck";
      }
    }));

    object.save();
    assertTrue(!object.getObjectId().isEmpty());

    AVQuery<AVObject> query = new AVQuery<AVObject>("androidBigObject");
    object = query.get(object.getObjectId());
    assertTrue(object.getInt("intValue") == intValue);

    String resultString = object.getString("myString");
    assertTrue(resultString.compareTo(string) == 0);

    Date resultDate = object.getDate("myDate");
    assertTrue(resultDate.compareTo(date) == 0);

    System.out.println(object.getDouble("myfloat"));

    ArrayList resultArray = (ArrayList) object.getList("myArray");
    org.json.JSONObject jo = object.getJSONObject("orgjsonObject");
    try {
      assertEquals(12, jo.getInt("1"));
    } catch (JSONException e1) {
      e1.printStackTrace();
    }
    assertTrue(resultArray.size() == array.size());
    for (int i = 0; i < resultArray.size(); ++i) {
      assertTrue(resultArray.get(i).equals(array.get(i)));
    }
  }

  public void testObjectSave1() throws Exception {
    AVObject gameScore = new AVObject(testTableName);
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
  }

  public void testObjectSave2FetchAndSave() throws Exception {
    AVQuery query = new AVQuery(testTableName);
    AVObject verifyObject = new AVObject(testTableName);
    verifyObject.setObjectId(query.getFirst().getObjectId());
    verifyObject.fetch();
    assertTrue(verifyObject.getCreatedAt() != null);

    verifyObject.put("score", 1234);
    verifyObject.save();
  }

  public void testSaveSync() throws Exception {
    Number number = new Double(bigObjectValue);
    String string = String.format("this is a value for test: %d", number.intValue());
    Date date = new Date();
    byte[] data = string.getBytes("UTF-8");
    List array = new ArrayList();
    for (int i = 0; i < 10; ++i) {
      array.add(i);
    }
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("string", string);
    map.put("number", number);
    map.put("null", null);

    AVObject object = new AVObject("BigObject");
    object.put("myNumber", bigObjectValue);
    object.put("myString", string);
    object.put("myDate", date);
    object.put("myData", data);
    object.put("myArray", array);
    object.put("myDictionary", map);
    object.put("myNull", null);

    object.save();
    assertTrue(!object.getObjectId().isEmpty());
  }


  public void testObjectDataTypeBFetch() throws Exception {
    AVQuery query = new AVQuery("BigObject");
    query.setLimit(5);
    List<AVObject> objects = query.find();
    for (AVObject object : objects) {
      double value = object.getDouble("myNumber");
      assertTrue(value == bigObjectValue);
    }
  }

  public void testPointerSave() throws Exception {
    AVObject person = new AVObject("TestPerson1");
    person.put("name", "summber");
    person.save();

    AVObject post = new AVObject("TestPost1");
    post.put("who", person);
    post.save();
    assertEquals(person.getObjectId(), AVObject.createWithoutData("TestPost1", post.getObjectId())
        .fetch("who").getAVObject("who").getObjectId());
  }

  public void testSaveAndDelete() throws Exception {

    AVObject person = new AVObject("TestPerson1");
    person.put("name", "summber");
    person.save();
    final String id = person.getObjectId();

    person.delete();

    final AVQuery query = new AVQuery("TestPerson1");
    try {
      query.get(id);
    } catch (AVException e) {
      assertNotNull(e);
      assertEquals(AVException.OBJECT_NOT_FOUND, e.getCode());
    }
  }

  public void testIncrement() throws Exception {
    final String valueTag = "value";
    final int intValue = 1;
    Number value = new Integer(intValue);
    AVObject person = new AVObject("TestIncream");
    person.put("name", "summber");
    person.put(valueTag, value);
    person.save();
    person.increment(valueTag);
    person.save();
    int result = person.getInt(valueTag);
    assertTrue(result == intValue + 1);
    person.save();
  }

  public void testIncrementAndFetch() throws Exception {
    final String valueTag = "number";
    final int intValue = 0;
    Number value = new Integer(intValue);
    final AVObject person = new AVObject("TestIncream");
    person.setFetchWhenSave(true);
    person.put("name", "summber");
    person.put(valueTag, value);
    int result = person.getInt(valueTag);
    assertTrue(result == intValue);
    person.save();

    // query it and save again
    AVQuery query = new AVQuery("TestIncream");
    AVObject samePerson = query.get(person.getObjectId());
    samePerson.increment(valueTag, -2);
    samePerson.save();

    // Increment the old one.
    person.increment(valueTag, -3);

    person.save();

    int v = person.getInt(valueTag);
    assertTrue(v == intValue - 5);
    if (person.getInt(valueTag) != intValue - 5)
      throw new RuntimeException();

    person.save();

    v = person.getInt(valueTag);
    person.increment(valueTag, 5);
    person.increment(valueTag, -3);
    person.increment(valueTag, -10);
    samePerson.increment(valueTag, -2);
    samePerson.save();
    person.save();
    int x = person.getInt(valueTag);
    assertTrue(x == v - 10);
  }

  public void testObjectAdd() throws Exception {
    AVObject container = new AVObject("container");
    for (int i = 0; i < 5; ++i) {
      AVObject item = new AVObject("item");
      item.put("index", i);
      item.save();
      container.add("list", item);
    }
    container.save();

    AVQuery query = new AVQuery("container");
    query.include("list");
    AVObject result = query.get(container.getObjectId());
    List<AVObject> items = result.getList("list");
    int value = 0;
    for (AVObject item : items) {
      assertFalse(item.getObjectId().isEmpty());
      assertEquals(value++, item.getInt("index"));
    }
  }

  public void testObjectRemoveAll() throws Exception {
    final String tableName = "removeAllTestTable";
    AVObject table = new AVObject(tableName);
    final String key = "mykey";
    List<String> values = new ArrayList<String>();
    values.add("a");
    values.add("b");
    values.add("c");
    values.add("d");
    values.add("e");
    values.add("f");
    for (String s : values) {
      table.add(key, s);
    }
    table.save();
    values.remove("f");
    table.removeAll(key, values);
    table.save();

    AVQuery query = new AVQuery(tableName);
    AVObject object = query.get(table.getObjectId());
    JSONArray array = object.getJSONArray(key);
    assertTrue(array.length() == 1);
  }

  private List<AVObject> getObjectList(final String table) throws AVException {
    AVQuery query = new AVQuery(table);
    query.setLimit(10);
    List<AVObject> list = query.find();
    return list;
  }

  public void testSyncFetch() throws Exception {
    AVObject object = new AVObject(testTableName);
    List<AVObject> list = getObjectList(testTableName);
    object.setObjectId(list.get(0).getObjectId());
    object.fetch();
    assertTrue(object.getUpdatedAt() != null);
  }

  public void testSyncRefresh() throws Exception {
    AVObject object = new AVObject(testTableName);
    List<AVObject> list = getObjectList(testTableName);
    object.setObjectId(list.get(0).getObjectId());
    object.refresh();
    assertTrue(object.getUpdatedAt() != null);
  }

  public void testFetchAllIfNeededInBackground() throws Exception {
    final List<String> idList = new ArrayList<String>();
    List<AVObject> list = getObjectList(testTableName);
    for (int i = 0; i < list.size() && i < 5; ++i) {
      idList.add(list.get(i).getObjectId());
    }

    String tableName = testTableName;
    List<AVObject> objects = new ArrayList<AVObject>();
    for (int i = 0; i < idList.size(); ++i) {
      AVObject object = new AVObject(tableName);
      object.setObjectId(idList.get(i));
      objects.add(object);
    }

    AVObject.fetchAllIfNeeded(objects);
    assertTrue(objects.size() == idList.size());
  }

  public void testSaveAllSubClass() throws Exception {
    List<Armor> objectList = new LinkedList<Armor>();
    for (int i = 10; i > 0; i--) {
      Armor armor = new Armor();
      armor.setDisplayName(AVUtils.getRandomString(10));
      armor.setBroken(false);
      objectList.add(armor);
    }
    AVObject.saveAll(objectList);
    for (Armor armor : objectList) {
      assertEquals(false, armor.getBoolean("broken"));
    }
  }

  public void testJSONLibrary() throws Exception {
    AVObject o = new AVObject("JSONTest");
    com.alibaba.fastjson.JSONObject fastjsonObject = new com.alibaba.fastjson.JSONObject();
    fastjsonObject.put("shit", 1);
    o.put("fastjsonObject", fastjsonObject);
    com.alibaba.fastjson.JSONArray fastjsonArray = new com.alibaba.fastjson.JSONArray();
    fastjsonArray.add(fastjsonObject);
    fastjsonArray.add(fastjsonObject);
    o.put("fastjsonArray", fastjsonArray);

    org.json.JSONObject jsonObject = new org.json.JSONObject();
    jsonObject.put("golendShit", 12);
    o.put("jsonObject", jsonObject);

    org.json.JSONArray jsonArray = new org.json.JSONArray();
    jsonArray.put(jsonObject);
    jsonArray.put(jsonObject);
    o.put("jsonArray", jsonArray);
    o.save();

    AVObject x = new AVObject("JSONTest");
    x.setObjectId(o.getObjectId());
    AVObject object = x.fetch();
    jsonObject = object.getJSONObject("jsonObject");
    jsonArray = object.getJSONArray("jsonArray");
    try {
      assertEquals(12, jsonObject.getInt("golendShit"));
      org.json.JSONObject object1 = new org.json.JSONObject(jsonArray.get(0).toString());
      assertEquals(12, object1.getInt("golendShit"));
    } catch (JSONException e1) {
      fail();
    }
  }

  public void testAVObjectSerialize() throws Exception {
    AVObject childObject = new AVObject("child");
    childObject.put("value", "shit");
    childObject.save();
    childObject.put("int", 123);
    String str = childObject.toString();
    LogUtil.avlog.d(str);
    AVObject parsedObject = AVObject.parseAVObject(str);
    assertEquals(123, parsedObject.getInt("int"));

    AVObject parentObject = new AVObject("Parent");
    parentObject.put("child", childObject);
    String parentStr = parentObject.toString();
    AVObject parsedParent = AVObject.parseAVObject(parentStr);
    parsedParent.save();
    parsedParent = AVObject.parseAVObject(parsedParent.toString());
    parsedObject = parsedParent.getAVObject("child");
    assertEquals(123, parsedObject.getInt("int"));
  }

  public void testUnicodeAttribute() throws Exception {
    AVObject childObject = new AVObject("child");
    childObject.put("str", "\\u0001\\u0080\\u0002");
    String str = childObject.toString();
    AVObject object = AVObject.parseAVObject(str);
    assertEquals("\\u0001\\u0080\\u0002", object.getString("str"));
  }

  public void testGeoPointRemove() throws Exception {
    AVObject o = new AVObject("GeoTest");
    AVGeoPoint p = new AVGeoPoint(1, 1);
    o.put("location", p);
    o.save();
    o.remove("location");
    o.save();

    AVQuery query = new AVQuery<AVObject>("GeoTest");
    AVObject result = query.get(o.getObjectId());
    assertFalse(result.containsKey("location"));
  }

  public void testObjectSaveWithOption() throws Exception {
    AVObject object = new AVObject("ObjectSaveWithQuery");
    object.put("int", 1);
    object.save();

    object.put("int", 2);
    AVQuery query = new AVQuery("ObjectSaveWithQuery");
    query.whereEqualTo("int", 2);
    AVSaveOption option = new AVSaveOption();
    option.query(query);
    try {
      object.save(option);
    } catch (AVException e) {
      assertEquals(305, e.getCode());
    }

    query.whereEqualTo("int", 1);
    object.save();
  }

}

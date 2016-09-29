package com.avos.avoscloud;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

public class USEndpointTest extends TestCase {

  @Override
  public void setUp() {
    TestApp.initForUS();
    // AVCloud.setProductionMode(false);
  }

  static private String testTableName = "GameScoreFromAndroid";
  static private double bigObjectValue = 123;

  public void testBasicObjectSave() throws Exception {
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
}

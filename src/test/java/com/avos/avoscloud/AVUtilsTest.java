package com.avos.avoscloud;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wli on 2017/2/28.
 */
public class AVUtilsTest extends TestCase {

    @Override
    public void setUp() {
        TestApp.init();
    }

    public void testGetParsedObject() throws Exception {
        AVObject person = new AVObject("TestPerson1");
        person.put("name", "summber");
        person.save();

        AVObject post = new AVObject("TestPost1");
        post.put("who", person);
        post.save();

        AVQuery<AVObject> query = new AVQuery<AVObject>("TestPost1");
        query.whereEqualTo("objectId", post.getObjectId());
        query.include("who.name");
        List<AVObject> resultList = query.find();

        Assert.assertTrue(null != resultList && resultList.size() == 1);
        Assert.assertTrue(null != resultList.get(0).getAVObject("who"));
        Assert.assertTrue("summber".equals(resultList.get(0).getAVObject("who").getString("name")));

        Object object = AVUtils.getParsedObject(resultList, true, false, true, true, true);
        Assert.assertTrue(null != object);

        HashMap<String, Object> dataMap = (HashMap<String, Object>)((ArrayList<Object>)object).get(0);
        Assert.assertTrue(dataMap.containsKey("createdAt"));
        Assert.assertTrue(dataMap.containsKey("className"));
        Assert.assertEquals("TestPost1", dataMap.get("className"));
        Assert.assertEquals("Object", dataMap.get("__type"));
        Assert.assertTrue(dataMap.containsKey("who"));

        HashMap<String, Object> whoMap = (HashMap<String, Object>)dataMap.get("who");
        Assert.assertTrue(whoMap.containsKey("name"));
        Assert.assertEquals("Pointer", whoMap.get("__type"));
        Assert.assertEquals("summber", whoMap.get("name"));
    }
}

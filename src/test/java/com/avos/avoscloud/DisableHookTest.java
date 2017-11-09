package com.avos.avoscloud;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DisableHookTest extends TestCase {

  public DisableHookTest(String testName) {
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
    return new TestSuite(DisableHookTest.class);
  }

  public void testSaveHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.save();
    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertEquals(1, object.getInt("byAfterSave"));
  }

  public void testSaveHook_disableBeforeHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.disableBeforeHook();
    object.save();
    object.fetch();
    assertNull(object.get("byBeforeSave"));
    assertEquals(1, object.getInt("byAfterSave"));
  }

  public void testSaveHook_disableAfterHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.disableAfterHook();
    object.save();
    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertNull(object.get("byAfterSave"));
  }

  public void testUpdateHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.save();
    object.put("title", "something");
    object.save();
    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertEquals(1, object.getInt("byAfterSave"));
    assertEquals(1, object.getInt("byBeforeUpdate"));
    assertEquals(1, object.getInt("byAfterUpdate"));
  }

  public void testUpdateHook_disableBeforeHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.save();
    object.put("title", "something");
    object.disableBeforeHook();
    object.save();
    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertEquals(1, object.getInt("byAfterSave"));
    assertNull(object.get("byBeforeUpdate"));
    assertEquals(1, object.getInt("byAfterUpdate"));
  }

  public void testUpdateHook_disableAfterHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.save();
    object.put("title", "something");
    object.disableAfterHook();
    object.save();
    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertEquals(1, object.getInt("byAfterSave"));
    assertEquals(1, object.getInt("byBeforeUpdate"));
    assertNull(object.get("byAfterUpdate"));
  }

  public void testHook_haveChildren_disableHook() throws AVException {
    AVObject child1 = new AVObject("IgnoreHookTest");
    child1.put("title", "child1");
    child1.disableBeforeHook();

    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.disableAfterHook();

    object.put("child1", child1);

    object.save();

    object.fetch();
    assertEquals(1, object.getInt("byBeforeSave"));
    assertNull(object.get("byAfterSave"));

    child1.fetch();
    assertNull(child1.get("byBeforeSave"));
    assertEquals(1, child1.getInt("byAfterSave"));

    child1.put("title", "child1 something");
    object.put("child1", child1);

    AVObject child2 = new AVObject("IgnoreHookTest");
    child2.put("title", "child2");
    child2.disableAfterHook();

    object.put("child2", child2);

    object.save();

    object.fetch();
    assertEquals(1, object.getInt("byBeforeUpdate"));
    assertNull(object.get("byAfterUpdate"));

    child1.fetch();
    assertNull(child1.get("byBeforeUpdate"));
    assertEquals(1, child1.getInt("byAfterUpdate"));

    child2.fetch();
    assertEquals(1, child2.getInt("byBeforeSave"));
    assertNull(child2.get("byAfterSave"));
  }

  public void testDeleteHook_disableBeforeHook() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "test");
    object.save();
    object.fetch();
    object.put("title", "something");
    object.disableBeforeHook();
    object.delete();
  }

  public void testDeleteHook_deleteAll() throws AVException {
    AVObject object = new AVObject("IgnoreHookTest");
    object.put("title", "object1");
    object.save();

    try {
      AVObject.deleteAll(Arrays.asList(object));
      fail("should throw 'Cloud Code vaildation failed' exception.");
    } catch (AVException e) {
      assertTrue(e.getMessage()
          .startsWith("Cloud Code validation failed. Error detail : Error from beforeDelete"));
    }

    AVObject object1 = new AVObject("IgnoreHookTest");
    object1.put("title", "object1");
    object1.ignoreHook(AVObject.Hook.beforeDelete);
    object1.save();

    AVObject object2 = new AVObject("IgnoreHookTest");
    object2.put("title", "object2");
    object2.disableBeforeHook();
    object2.save();

    AVObject object3 = new AVObject("IgnoreHookTest");
    object3.put("title", "object2");
    object3.disableBeforeHook();
    object3.save();

    AVObject.deleteAll(Arrays.asList(object1, object2, object3));
  }

}

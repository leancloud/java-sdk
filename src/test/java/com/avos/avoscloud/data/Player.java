package com.avos.avoscloud.data;

import java.util.List;

import com.avos.avoscloud.AVClassName;
import com.avos.avoscloud.AVObject;

@AVClassName("player")
public class Player extends AVObject {

  public void setArmors(List<Armor> armors) {
    this.put("armors", armors);
  }

  public List<Armor> getArmors() {
    return this.getList("armors", Armor.class);
  }

  public void addArmor(Armor armor) {
    this.addUnique("armors", armor);
  }
}

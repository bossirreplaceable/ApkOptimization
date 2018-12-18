package com.dongnao.ricky.androlib.res.data;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResPackage
{
  private final String mName;
  private Map<Integer, String> mSpecNamesReplace = new LinkedHashMap();
  private HashSet<String> mSpecNamesBlock = new HashSet();
  private boolean mCanProguard = false;

  public ResPackage(int id, String name)
  {
    this.mName = name;
  }

  public void setCanProguard(boolean set) {
    this.mCanProguard = set;
  }

  public boolean isCanProguard() {
    return this.mCanProguard;
  }

  public boolean hasSpecRepplace(String resID) {
    return this.mSpecNamesReplace.containsKey(resID);
  }

  public String getSpecRepplace(int resID) {
    return ((String)this.mSpecNamesReplace.get(Integer.valueOf(resID)));
  }

  public void putSpecNamesReplace(int resID, String value)
  {
    this.mSpecNamesReplace.put(Integer.valueOf(resID), value);
  }

  public void putSpecNamesblock(String value) {
    this.mSpecNamesBlock.add(value);
  }

  public HashSet<String> getSpecNamesBlock()
  {
    return this.mSpecNamesBlock;
  }

  public String getName()
  {
    return this.mName;
  }

  public String toString()
  {
    return this.mName;
  }
}
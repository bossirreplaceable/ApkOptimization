package com.dongnao.ricky.androlib.res.data;

import java.util.HashSet;

import com.dongnao.ricky.androlib.AndrolibException;

public final class ResType
{
  private final String mName;
  private final ResPackage mPackage;
  private HashSet<String> specNames;

  public ResType(String name, ResPackage package_)
  {
    this.mName = name;
    this.mPackage = package_;
    this.specNames = new HashSet();
  }

  public String getName() {
    return this.mName;
  }

  public void putSpecProguardName(String name) throws AndrolibException {
    if (this.specNames.contains(name)) {
      throw new AndrolibException(String.format(
        "spec proguard name duplicate in a singal type %s, spec name: %s\n known issue: if you write a whilte list R.drawable.ab, and you have a png named ab.png, these may cost duplicate of ab\n", new Object[] { 
        getName(), name }));
    }

    this.specNames.add(name);
  }

  public String toString()
  {
    return this.mName;
  }
}
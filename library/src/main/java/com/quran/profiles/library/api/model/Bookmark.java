package com.quran.profiles.library.api.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Bookmark object
 * Created by ahmedre on 5/18/14.
 */
public class Bookmark {
  Integer id;
  String name;
  boolean isDefault;
  Date createdAt;
  Date updatedAt;
  String etag;
  Pointer pointer;

  public Bookmark() {
  }

  public Bookmark(String name, Pointer pointer) {
    this.name = name;
    this.pointer = pointer;
  }

  public Bookmark(Pointer pointer) {
    this.pointer = pointer;
  }

  public int getId() {
    return this.id;
  }

  public Pointer getPointer() {
    return this.pointer;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEtag() {
    return this.etag;
  }

  public Date getCreatedDate() {
    return this.createdAt;
  }

  public Date getLastUpdatedDate() {
    return this.updatedAt;
  }

  public boolean isDefault() {
    return this.isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  public Map<String, String> getParameters() {
    final Map<String, String> map = new HashMap<String, String>();
    map.put("name", this.name);
    map.put("page", this.pointer.page == null ? null :
        String.valueOf(this.pointer.page));
    map.put("chapter", this.pointer.chapter == null ? null :
        String.valueOf(this.pointer.chapter));
    map.put("verse", this.pointer.verse == null ? null :
        String.valueOf(this.pointer.verse));
    map.put("is_default", String.valueOf(isDefault));
    map.put("etag", this.etag);
    return map;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("");
    if (name != null) {
      builder.append(name).append(" - ");
    }
    if (pointer != null) {
      builder.append(pointer.toString());
    }
    return builder.toString();
  }
}

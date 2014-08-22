package com.quran.profiles.library.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
  Integer[] tagIds;
  List<String> tags;
  Tag[] newTags;

  public Bookmark() {
  }

  public Bookmark(String name, Pointer pointer) {
    this.name = name;
    this.pointer = pointer;
  }

  public Bookmark(Pointer pointer) {
    this.pointer = pointer;
  }

  public Integer getId() {
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

  public Integer[] getTagIds() {
    return this.tagIds;
  }

  public void setTagIds(Integer[] tagIds) {
    this.tagIds = tagIds;
  }

  public void addTag(String name) {
    if (this.tags == null) {
      this.tags = new ArrayList<String>();
    }
    this.tags.add(name);
  }

  public boolean isUpdateOf(Bookmark serverBookmark) {
    return this.updatedAt != null &&
        !this.updatedAt.equals(serverBookmark.getLastUpdatedDate());
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

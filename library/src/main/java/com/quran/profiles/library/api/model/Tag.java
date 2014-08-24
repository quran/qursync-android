package com.quran.profiles.library.api.model;

import java.util.Date;

/**
 * Tag object
 * Created by ahmedre on 7/5/14.
 */
public class Tag {
  Integer id;
  String name;
  Date updatedAt;
  String etag;
  String color;
  Integer[] bookmarkIds;

  public Tag() {
  }

  public Tag(String name) {
    this.name = name;
  }

  public Integer getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public String getEtag() {
    return this.etag;
  }

  public Date getLastUpdatedDate() {
    return this.updatedAt;
  }

  public boolean isUpdateOf(Tag serverTag) {
    return this.updatedAt != null &&
        !this.updatedAt.equals(serverTag.getLastUpdatedDate());
  }
}

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
}

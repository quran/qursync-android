package com.quran.profiles.library.api.model;

import java.util.Date;

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
  Pointer pointer;
  // TODO: etag / json hint?

  public Bookmark() {
  }

  public Bookmark(Pointer pointer) {
    this.pointer = pointer;
  }

  public int getId() {
    return this.id;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("");
    if (name != null) {
      builder.append(name).append(" - ");
    }
    builder.append(pointer.toString());
    return builder.toString();
  }
}

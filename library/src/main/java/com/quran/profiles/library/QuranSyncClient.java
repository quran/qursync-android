package com.quran.profiles.library;

import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Tag;

import java.util.List;
import java.util.Set;

public interface QuranSyncClient {
  public List<Bookmark> getBookmarks();
  public Set<Integer> getDeletedBookmarkIds();
  public List<Tag> getTags();
  public Set<Integer> getDeletedTagIds();
}

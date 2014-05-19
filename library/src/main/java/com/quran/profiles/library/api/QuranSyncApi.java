package com.quran.profiles.library.api;

import com.quran.profiles.library.api.model.Bookmark;

import java.util.List;

import retrofit.Callback;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * QuranSync Retrofit Api
 * Created by ahmedre on 5/18/14.
 */
public interface QuranSyncApi {

  @GET("/bookmarks")
  void getBookmarks(Callback<List<Bookmark>> callback);

  @POST("/bookmarks")
  void addPageBookmark(@Query("page") Integer page,
      Callback<Bookmark> callback);

  @DELETE("/bookmarks/{bookmarkId}")
  void deleteBookmark(@Path("bookmarkId") int bookmarkId,
      Callback<Bookmark> callback);
}

package com.quran.profiles.library.api;

import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Tag;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * QuranSync Retrofit Api
 * Created by ahmedre on 5/18/14.
 */
public interface QuranSyncApi {

  @GET("/bookmarks")
  void getBookmarks(Callback<List<Bookmark>> callback);

  @POST("/bookmarks")
  void addBookmark(@Body Bookmark params, Callback<Bookmark> callback);

  @PUT("/bookmarks/{bookmarkId}")
  void updateBookmark(@Path("bookmarkId") int bookmarkId,
      @Body Bookmark params, Callback<Bookmark> callback);

  @DELETE("/bookmarks/{bookmarkId}")
  void deleteBookmark(@Path("bookmarkId") int bookmarkId,
      Callback<Bookmark> callback);

  @GET("/tags")
  void getTags(Callback<List<Tag>> callback);

  @POST("/tags")
  void addTag(@Body Tag tag);

  @DELETE("/tags/{tagId}")
  void deleteTag(@Path("tagId") int tagId, Callback<Tag> callback);
}

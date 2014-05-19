package com.quran.profiles.library.api;

import com.quran.profiles.library.api.model.Bookmark;

import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.http.DELETE;
import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
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

  @FormUrlEncoded
  @POST("/bookmarks")
  void addBookmark(@FieldMap Map<String, String> params,
      Callback<Bookmark> callback);

  @FormUrlEncoded
  @PUT("/bookmarks/{bookmarkId}")
  void updateBookmark(@Path("bookmarkId") int bookmarkId,
      @FieldMap Map<String, String> params,
      Callback<Bookmark> callback);

  @DELETE("/bookmarks/{bookmarkId}")
  void deleteBookmark(@Path("bookmarkId") int bookmarkId,
      Callback<Bookmark> callback);
}

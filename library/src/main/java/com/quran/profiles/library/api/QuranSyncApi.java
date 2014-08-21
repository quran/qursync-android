package com.quran.profiles.library.api;

import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Tag;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import rx.Observable;

/**
 * QuranSync Retrofit Api
 * Created by ahmedre on 5/18/14.
 */
public interface QuranSyncApi {

  @GET("/bookmarks")
  Observable<List<Bookmark>> getBookmarks();

  @POST("/bookmarks")
  Observable<Bookmark> addBookmark(@Body Bookmark params);

  @PUT("/bookmarks/{bookmarkId}")
  Observable<Bookmark> updateBookmark(@Path("bookmarkId") int bookmarkId,
      @Body Bookmark params);

  @DELETE("/bookmarks/{bookmarkId}")
  Observable<Bookmark> deleteBookmark(@Path("bookmarkId") int bookmarkId);

  @GET("/tags")
  Observable<List<Tag>> getTags();

  @POST("/tags")
  Observable<Tag> addTag(@Body Tag tag);

  @DELETE("/tags/{tagId}")
  Observable<Tag> deleteTag(@Path("tagId") int tagId);
}

package com.quran.profiles.library;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.quran.profiles.library.api.QuranSyncApi;
import com.quran.profiles.library.api.QuranSyncScribeApi;
import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Pointer;
import com.quran.profiles.library.api.model.Tag;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthConstants;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Api wrapper for QuranSync
 * Created by ahmedre on 5/18/14.
 */
public class QuranSync {

  public static final String API_BASE = "http://profiles.quran.com/api/v1/";
  public static final int MSG_TOKEN_RECEIVED = 1;

  private static final String TAG = QuranSync.class.getName();

  private static QuranSync sInstance;
  private volatile Token mToken;
  private OAuthService mService;
  private QuranSyncApi mApi;
  private final Object mLock = new Object();

  public static synchronized QuranSync getInstance(
      String apiKey, String apiSecret, String callback) {
    if (sInstance == null) {
      sInstance = new QuranSync(apiKey, apiSecret, callback);
    }
    return sInstance;
  }

  private QuranSync(String apiKey, String apiSecret, String callback) {
    mService = new ServiceBuilder()
        .provider(QuranSyncScribeApi.class)
        .apiKey(apiKey)
        .apiSecret(apiSecret)
        .callback(callback)
        .build();

    Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_BASE)
        .setConverter(new GsonConverter(gson))
        // uncomment this to test with a proxy
        // .setClient(new ProxyHttpClient())
        .setRequestInterceptor(new RequestInterceptor() {
          @Override
          public void intercept(RequestFacade request) {
            request.addQueryParam(
                OAuthConstants.ACCESS_TOKEN, mToken.getToken());
          }
        }).build();
    mApi = restAdapter.create(QuranSyncApi.class);
  }

  public void setToken(String token) {
    synchronized (mLock) {
      mToken = new Token(token, "");
    }
  }

  public void clearToken() {
    synchronized (mLock) {
      mToken = null;
    }
  }

  public boolean isAuthorized() {
    synchronized (mLock) {
      return mToken != null;
    }
  }

  public String getAuthorizationUrl() {
    return mService.getAuthorizationUrl(null);
  }

  public void getAccessToken(final String code, final Handler callback) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        final Token token = mService.getAccessToken(null, new Verifier(code));
        synchronized (mLock) {
          mToken = token;
        }

        if (callback != null) {
          final Message message =
              callback.obtainMessage(MSG_TOKEN_RECEIVED, token.getToken());
          callback.sendMessage(message);
        }
      }
    }).start();
  }

  public Observable<SyncResult> sync(final QuranSyncClient client) {
    Log.d(TAG, "sync");

    // setup a map of bookmark ids to bookmarks (for existing bookmarks)
    final List<Bookmark> clientBookmarks = client.getBookmarks();
    final SparseArray<Bookmark> localBookmarksMap = new SparseArray<>();
    for (Bookmark bookmark : clientBookmarks) {
      final Integer id = bookmark.getId();
      if (id != null) {
        localBookmarksMap.put(id, bookmark);
      }
    }

    // setup a map of tag ids to tags (for existing tags)
    final List<Tag> clientTags = client.getTags();
    final SparseArray<Tag> localTagsMap = new SparseArray<>();
    for (Tag tag : clientTags) {
      final Integer id = tag.getId();
      if (id != null) {
        localTagsMap.put(id, tag);
      }
    }

    // list of deleted tags
    final Set<Integer> deletedTagIds = client.getDeletedTagIds();
    final Set<Integer> deletedBookmarkIds = client.getDeletedBookmarkIds();

    // bookmarks related api requests
    final Observable<List<Bookmark>> bookmarksObservable =
        mApi.getBookmarks()
            .flatMap(new Func1<List<Bookmark>, Observable<Bookmark>>() {
              @Override
              public Observable<Bookmark> call(List<Bookmark> bookmarkList) {
                Log.d(TAG, "got result back, processing...");
                final Set<Pointer> serverBookmarks = new HashSet<>();
                for (Bookmark b : bookmarkList) {
                  serverBookmarks.add(b.getPointer());
                }

                final Observable<Bookmark> additions =
                    Observable.from(client.getBookmarks())
                        .filter(new Func1<Bookmark, Boolean>() {
                          @Override
                          public Boolean call(Bookmark b) {
                            return b.getId() == null &&
                                !serverBookmarks.contains(b.getPointer());
                          }
                        });

                // we will return an observable of the server bookmarks
                // combined with any new client bookmarks
                return Observable.from(bookmarkList)
                    .mergeWith(additions);
              }
            })
            .flatMap(new Func1<Bookmark, Observable<Bookmark>>() {
              @Override
              public Observable<Bookmark> call(Bookmark b) {
                Log.d(TAG, "considering: " + b.toString());
                final Integer id = b.getId();
                if (id == null) {
                  // this is a new client bookmark, add it to server
                  Log.d(TAG, "adding bookmark: " + b.toString());
                  return mApi.addBookmark(b);
                } else if (deletedBookmarkIds.contains(id)) {
                  // we explicitly deleted this on the client, so delete
                  Log.d(TAG, "deleting bookmark: " + b.toString());
                  return mApi.deleteBookmark(id);
                } else {
                  // check if we had this bookmark already
                  final Bookmark localBookmark = localBookmarksMap.get(id);
                  if (localBookmark != null && localBookmark.isUpdateOf(b)) {
                    // we had the bookmark, but it changed...
                    Log.d(TAG, "updating bookmark: " + b.toString());
                    return mApi.updateBookmark(id, localBookmark);
                  } else {
                    // bookmark is unchanged (or new on server)
                    return Observable.from(b);
                  }
                }
              }
            })
            // filter out the null bookmarks emitted by delete
            .filter(new Func1<Bookmark, Boolean>() {
              @Override
              public Boolean call(Bookmark bookmark) {
                return bookmark != null;
              }
            })
            .toList();


    // tags related api requests
    final Observable<List<Tag>> tagsObservable =
        mApi.getTags()
        .flatMap(new Func1<List<Tag>, Observable<Tag>>() {
          @Override
          public Observable<Tag> call(List<Tag> tags) {
            // we will return an observable containing the server tags
            // combined with the new client tags
            return Observable.from(tags)
                .mergeWith(Observable.from(client.getTags())
                    .filter(new Func1<Tag, Boolean>() {
                      @Override
                      public Boolean call(Tag tag) {
                        return tag.getId() == null;
                      }
                    })
                );
          }
        })
        .flatMap(new Func1<Tag, Observable<Tag>>() {
          @Override
          public Observable<Tag> call(Tag tag) {
            final Integer id = tag.getId();
            if (id == null) {
              // this is a new tag
              return mApi.addTag(tag);
            } else if (deletedTagIds.contains(id)) {
              // we explicitly deleted this tag
              return mApi.deleteTag(id);
            } else {
              final Tag clientTag = localTagsMap.get(id);
              if (clientTag != null && clientTag.isUpdateOf(tag)) {
                // we have this tag, but we changed it
                return mApi.updateTag(id, clientTag);
              } else {
                // tag is unchanged (or new on server)
                return Observable.from(tag);
              }
            }
          }
        })
        // filter out the null tags emitted by delete
        .filter(new Func1<Tag, Boolean>() {
          @Override
          public Boolean call(Tag tag) {
            return tag != null;
          }
        }).toList();

    // run the bookmarks requests after tags is done
    return tagsObservable.flatMap(new Func1<List<Tag>, Observable<SyncResult>>() {
      @Override
      public Observable<SyncResult> call(List<Tag> tags) {
        return Observable.zip(bookmarksObservable, Observable.just(tags),
            new Func2<List<Bookmark>, List<Tag>, SyncResult>() {
              @Override
              public SyncResult call(List<Bookmark> bookmarks, List<Tag> tags) {
                final SyncResult result = new SyncResult();
                result.bookmarks = bookmarks;
                result.tags = tags;
                return result;
              }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
      }
    });
  }
}

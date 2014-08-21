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

  public Observable<SyncResult> sync(
      final List<Bookmark> bookmarks,
      final SparseArray<Bookmark> updates,
      final SparseArray<Bookmark> deletions) {
    Log.d(TAG, "sync");

    /**
     * explanation
     * we return the combined result of bookmarks and tags (both
     * of which are defined below).
     * note that bookmarks and tags should run in parallel.
     *
     * bookmarks
     *   1. get an array containing the server bookmarks
     *   2. add to the above array any local bookmarks that don't
     *      yet exist on the server.
     *   3. for each item in the array, either:
     *      a. make an api call to add
     *      b. make an api call to delete
     *      c. make an api call to update
     *   4. remove any null bookmarks and return as a list.
     *
     *   note that these api calls happen in parallel (minus the
     *   first call, which is intentionally blocking any other work).
     *
     * tags
     *   currently just gets a list of tags from the server.
     *
     *
     * current TODO
     * 1. switch parameters to an interface with a set of methods,
     *    since we'll eventually need more than the current params
     * 2. implement tags - in simplest case, we should do something
     *    similar to bookmarks.
     * 3. currently, if server has a bookmark for page 5, and a fresh
     *    client adds this bookmark before syncing, sync will keep the
     *    server's and ignore the clients. is this correct, or should
     *    the client win in this case (ie become an update?)
     * 4. speaking of updates, what is the right thing to do if the
     *    server etag is different than the client etag? do we drop
     *    the client change?
     * 5. edge cases - ex, client says update bookmark 3, server no
     *    longer has bookmark 3 - what happens? currently, we ignore it.
     * 6. error handling
     * 7. document the code inline
     *
     * we currently return an observable, which the activity subscribes
     * to. perhaps it makes more sense to offer a version which takes a
     * callback instead, which we call once we are done (so developers
     * don't have to learn rxjava unless they want to).
     */

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
                    Observable.from(bookmarks)
                        .filter(new Func1<Bookmark, Boolean>() {
                          @Override
                          public Boolean call(Bookmark bookmark) {
                            return bookmark.getId() == null &&
                                !serverBookmarks.contains(bookmark.getPointer());
                          }
                        });
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
                  Log.d(TAG, "adding bookmark: " + b.toString());
                  return mApi.addBookmark(b);
                } else if (deletions.get(id, null) != null) {
                  Log.d(TAG, "deleting bookmark: " + b.toString());
                  return mApi.deleteBookmark(id);
                } else if (updates.get(id, null) != null) {
                  Log.d(TAG, "updating bookmark: " + b.toString());
                  return mApi.updateBookmark(id, updates.get(id));
                } else {
                  return Observable.from(b);
                }
              }
            })
            .filter(new Func1<Bookmark, Boolean>() {
              @Override
              public Boolean call(Bookmark bookmark) {
                return bookmark != null;
              }
            })
            .toList();

    final Observable<List<Tag>> tagsObservable =
        mApi.getTags();

    return Observable.zip(bookmarksObservable, tagsObservable,
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
}

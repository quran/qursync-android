package com.quran.profiles.sample.app;

import com.quran.profiles.library.QuranSync;
import com.quran.profiles.library.api.QuranSyncApi;
import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Pointer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends Activity {
  private static final String TAG =
      "com.quran.profiles.sample.app.MainActivity";

  private static final String API_KEY = Constants.API_KEY;
  private static final String API_SECRET = Constants.API_SECRET;
  private static final String CALLBACK = Constants.CALLBACK;

  private static final String PREF_TOKEN = "pref_token";
  private static TokenHandler sHandler = new TokenHandler();

  private Button mButton;
  private Button mAddButton;
  private QuranSync mQuranSync;
  private SharedPreferences mPrefs;
  private ListView mListView;
  private List<Bookmark> mBookmarks;

  static class TokenHandler extends Handler {
    private WeakReference<MainActivity> mActivityReference;

    public void setActivity(MainActivity activity) {
      mActivityReference = new WeakReference<MainActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
      if (mActivityReference != null) {
        final MainActivity activity = mActivityReference.get();
        if (activity != null) {
          activity.onAuthorized((String) msg.obj);
        }
        super.handleMessage(msg);
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mQuranSync = QuranSync.getInstance(API_KEY, API_SECRET, CALLBACK);
    mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    final String tokenString = mPrefs.getString(PREF_TOKEN, null);
    if (tokenString != null) {
      mQuranSync.setToken(tokenString);
    }

    setContentView(R.layout.main);
    mButton = (Button) findViewById(R.id.authorize);
    mAddButton = (Button) findViewById(R.id.add_bookmark);
    mButton.setOnClickListener(mOnClickListener);
    mAddButton.setOnClickListener(mOnClickListener);

    mListView = (ListView) findViewById(R.id.bookmarks);
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mBookmarks != null && position < mBookmarks.size()) {
          final Bookmark b = mBookmarks.get(position);
          if (TextUtils.isEmpty(b.getName()) && b.getPointer() != null) {
            b.setName("bookmark " + System.currentTimeMillis());
            updateBookmark(b);
          } else {
            deleteBookmark(mBookmarks.get(position).getId());
          }
        }
      }
    });

    final boolean loggedIn = mQuranSync.isAuthorized();
    mButton.setText(!loggedIn ?
        R.string.authorize : R.string.logout);
    mAddButton.setEnabled(loggedIn);
    handleIntent(getIntent());
  }

  @Override
  protected void onStart() {
    super.onStart();
    sHandler.setActivity(this);
  }

  @Override
  protected void onStop() {
    sHandler.setActivity(null);
    super.onStop();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.d(TAG, "onNewIntent()");
    handleIntent(intent);
  }

  private View.OnClickListener mOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.authorize: {
          if (!mQuranSync.isAuthorized()) {
            authorize();
          } else {
            mQuranSync.clearToken();
            mButton.setText(R.string.authorize);
            mAddButton.setEnabled(false);
          }
          break;
        }
        case R.id.add_bookmark: {
          final int page = 1 + (int)(Math.random() * 604);
          final Bookmark bookmark = new Bookmark(Pointer.fromPage(page));
          addBookmark(bookmark);
          break;
        }
      }
    }
  };

  private void handleIntent(Intent intent) {
    Log.d(TAG, "handleIntent()");
    final Uri uri = intent.getData();
    if (uri != null) {
      Log.d(TAG, "got uri: " + uri);
      final String code = uri.getQueryParameter("code");
      mQuranSync.getAccessToken(code, sHandler);
    }

    if (mQuranSync.isAuthorized()) {
      requestBookmarks();
    }
  }

  private void authorize() {
    final String url = mQuranSync.getAuthorizationUrl();
    final Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(i);
  }

  private void onAuthorized(String token) {
    mButton.setText(R.string.logout);
    mPrefs.edit()
        .putString(PREF_TOKEN, token).commit();
    mAddButton.setEnabled(true);
    requestBookmarks();
  }

  private void requestBookmarks() {
    final QuranSyncApi api = mQuranSync.getApi();
    api.getBookmarks(new Callback<List<Bookmark>>() {
      @Override
      public void success(List<Bookmark> bookmarks, Response response) {
        Log.d(TAG, "successfully requested bookmarks!");
        mBookmarks = bookmarks;
        mListView.setAdapter(new ArrayAdapter<Bookmark>(
            MainActivity.this, android.R.layout.simple_list_item_1,
            mBookmarks));
      }

      @Override
      public void failure(RetrofitError retrofitError) {
        Log.e(TAG, "error requesting bookmarks", retrofitError);
      }
    });
  }

  private void addBookmark(Bookmark bookmark) {
    final QuranSyncApi api = mQuranSync.getApi();
    api.addBookmark(bookmark, new Callback<Bookmark>() {
      @Override
      public void success(Bookmark bookmark, Response response) {
        Log.d(TAG, "successfully added bookmark!");
        requestBookmarks();
      }

      @Override
      public void failure(RetrofitError retrofitError) {
        Log.e(TAG, "error adding bookmark", retrofitError);
      }
    });
  }

  private void deleteBookmark(int id) {
    final QuranSyncApi api = mQuranSync.getApi();
    api.deleteBookmark(id, new Callback<Bookmark>() {
      @Override
      public void success(Bookmark bookmark, Response response) {
        Log.d(TAG, "successfully deleted bookmark");
        requestBookmarks();
      }

      @Override
      public void failure(RetrofitError retrofitError) {
        Log.e(TAG, "error deleting bookmark", retrofitError);
      }
    });
  }

  private void updateBookmark(Bookmark bookmark) {
    final QuranSyncApi api = mQuranSync.getApi();
    api.updateBookmark(bookmark.getId(), bookmark, new Callback<Bookmark>() {
      @Override
      public void success(Bookmark bookmark, Response response) {
        Log.d(TAG, "successfully updated bookmark!");
        requestBookmarks();
      }

      @Override
      public void failure(RetrofitError retrofitError) {
        Log.e(TAG, "error updating bookmark", retrofitError);
      }
    });
  }
}

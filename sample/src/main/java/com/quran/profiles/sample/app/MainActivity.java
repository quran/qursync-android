package com.quran.profiles.sample.app;

import com.quran.profiles.library.QuranSync;
import com.quran.profiles.library.SyncResult;
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
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;


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
  private Button mSyncButton;
  private QuranSync mQuranSync;
  private SharedPreferences mPrefs;
  private List<Bookmark> mBookmarks;
  private BookmarksAdapter mAdapter;
  private SparseArray<Bookmark> mUpdates;
  private SparseArray<Bookmark> mDeletions;
  private Subscription mSubscription;

  static class TokenHandler extends Handler {
    private WeakReference<MainActivity> mActivityReference;

    public void setActivity(MainActivity activity) {
      mActivityReference = new WeakReference<>(activity);
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
    mSyncButton = (Button) findViewById(R.id.sync);
    mButton.setOnClickListener(mOnClickListener);
    mAddButton.setOnClickListener(mOnClickListener);
    mSyncButton.setOnClickListener(mOnClickListener);

    final ListView listView = (ListView) findViewById(R.id.bookmarks);
    mAdapter = new BookmarksAdapter();
    listView.setAdapter(mAdapter);

    mBookmarks = new ArrayList<>();
    mDeletions = new SparseArray<>();
    mUpdates = new SparseArray<>();

    final boolean loggedIn = mQuranSync.isAuthorized();
    mButton.setText(!loggedIn ?
        R.string.authorize : R.string.logout);
    mAddButton.setEnabled(loggedIn);
    mSyncButton.setEnabled(loggedIn);
    handleIntent(getIntent());
  }

  @Override
  protected void onDestroy() {
    if (mSubscription != null) {
      mSubscription.unsubscribe();
    }
    super.onDestroy();
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
            mSyncButton.setEnabled(false);
          }
          break;
        }
        case R.id.add_bookmark: {
          final int page = 1 + (int)(Math.random() * 604);
          final Bookmark bookmark = new Bookmark(Pointer.fromPage(page));
          addBookmark(bookmark);
          break;
        }
        case R.id.sync: {
          syncBookmarks();
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
      syncBookmarks();
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
    mSyncButton.setEnabled(true);
    syncBookmarks();
  }

  private void syncBookmarks() {
    mAddButton.setEnabled(false);
    mSyncButton.setEnabled(false);

    mSubscription = AndroidObservable.bindActivity(this,
        mQuranSync.sync(mBookmarks, mUpdates, mDeletions))
        .subscribe(new Action1<SyncResult>() {
          @Override
          public void call(SyncResult result) {
            mUpdates.clear();
            mDeletions.clear();
            mBookmarks = result.bookmarks;
            mAdapter.notifyDataSetChanged();

            mAddButton.setEnabled(true);
            mSyncButton.setEnabled(true);
            Toast.makeText(MainActivity.this,
                getString(R.string.sync_done), Toast.LENGTH_SHORT).show();
            mSubscription = null;
          }
        });
  }

  private void addBookmark(Bookmark bookmark) {
    mBookmarks.add(bookmark);
    mAdapter.notifyDataSetChanged();
  }

  private void deleteBookmark(Bookmark bookmark) {
    if (bookmark.getId() != null) {
      mDeletions.put(bookmark.getId(), bookmark);
    }
    mBookmarks.remove(bookmark);
    mAdapter.notifyDataSetChanged();
  }

  private class BookmarksAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    public BookmarksAdapter() {
      mInflater = LayoutInflater.from(MainActivity.this);
    }

    @Override
    public int getCount() {
      return mBookmarks == null ? 0 : mBookmarks.size();
    }

    @Override
    public Object getItem(int position) {
      return mBookmarks.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder vh;
      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.bookmark_item, parent, false);
        vh = new ViewHolder();
        vh.title = (TextView) convertView.findViewById(R.id.title);
        vh.delete = (ImageButton) convertView.findViewById(R.id.delete);
        convertView.setTag(vh);
      }
      vh = (ViewHolder) convertView.getTag();

      final Bookmark bookmark = (Bookmark) getItem(position);
      vh.title.setText(bookmark.toString());
      vh.delete.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          deleteBookmark(bookmark);
        }
      });
      return convertView;
    }
  }

  public static class ViewHolder {
    public TextView title;
    public ImageButton delete;
  }
}

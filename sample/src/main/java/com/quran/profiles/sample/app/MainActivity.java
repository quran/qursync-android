package com.quran.profiles.sample.app;

import com.quran.profiles.library.QuranSync;
import com.quran.profiles.library.QuranSyncClient;
import com.quran.profiles.library.SyncResult;
import com.quran.profiles.library.api.model.Bookmark;
import com.quran.profiles.library.api.model.Pointer;
import com.quran.profiles.library.api.model.Tag;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;


public class MainActivity extends FragmentActivity {
  private static final String TAG =
      "com.quran.profiles.sample.app.MainActivity";
  private static final String TAG_TAGS_FRAGMENT = "TAG_TAGS_FRAGMENT";

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
  private List<Tag> mTags;
  private Bookmark mEditingBookmark;
  private BookmarksAdapter mAdapter;
  private Set<Integer> mDeletions;
  private Set<Integer> mTagDeletions;
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
    mDeletions = new HashSet<>();
    mTagDeletions = new HashSet<>();
    mTags = new ArrayList<>();

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

  public List<Tag> getTags() {
    return mTags;
  }

  public List<String> getCurrentBookmarkTags() {
    if (mEditingBookmark == null) {
      return new ArrayList<>();
    }
    return getTagsFor(mEditingBookmark);
  }

  private List<String> getTagsFor(Bookmark b) {
    List<String> tags = b.getTags();
    if (tags != null) {
      return tags;
    }
    tags = new ArrayList<>();

    final Integer[] ids = b.getTagIds();
    if (ids != null) {
      final Map<Integer, Tag> tagHash = new HashMap<>();
      for (Tag t : mTags) {
        tagHash.put(t.getId(), t);
      }

      for (Integer id : ids) {
        final Tag tag = tagHash.get(id);
        if (tag != null) {
          tags.add(tag.getName());
        }
      }
    }
    return tags;
  }

  public void saveTags(List<Tag> tags,
      List<String> bookmarkTags,
      List<Integer> tagDeletions) {
    mTags = tags;
    if (mEditingBookmark != null) {
      mEditingBookmark.setTags(bookmarkTags);
    }

    for (Integer id : tagDeletions) {
      mTagDeletions.add(id);
    }
    closeTagDialog();
    mAdapter.notifyDataSetChanged();
  }

  public void showTagsFragment(Bookmark b) {
    mEditingBookmark = b;
    final FragmentManager fm = getSupportFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final Fragment prev = fm.findFragmentByTag(TAG_TAGS_FRAGMENT);
    if (prev != null) {
      ft.remove(prev);
    }

    final TagsFragment fragment = new TagsFragment();
    fragment.show(ft, TAG_TAGS_FRAGMENT);
  }

  public void closeTagDialog() {
    mEditingBookmark = null;

    final FragmentManager fm = getSupportFragmentManager();
    final FragmentTransaction ft = fm.beginTransaction();
    final Fragment prev = fm.findFragmentByTag(TAG_TAGS_FRAGMENT);
    if (prev != null) {
      ft.remove(prev);
    }
    ft.commit();
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

  public static class SyncClient implements QuranSyncClient {
    private List<Bookmark> mBookmarks;
    private Set<Integer> mDeletions;
    private List<Tag> mTags;
    private Set<Integer> mDeletedTagIds;

    public SyncClient(List<Bookmark> bookmarks, Set<Integer> deletedIds,
        List<Tag> tags, Set<Integer> deletedTagIds) {
      mBookmarks = bookmarks;
      mDeletions = deletedIds;
      mTags = tags;
      mDeletedTagIds = deletedTagIds;
    }

    @Override
    public List<Bookmark> getBookmarks() {
      return mBookmarks;
    }

    @Override
    public Set<Integer> getDeletedBookmarkIds() {
      return mDeletions;
    }

    @Override
    public List<Tag> getTags() {
      return mTags;
    }

    @Override
    public Set<Integer> getDeletedTagIds() {
      return mDeletedTagIds;
    }
  }

  private void syncBookmarks() {
    mAddButton.setEnabled(false);
    mSyncButton.setEnabled(false);

    final SyncClient client = new SyncClient(
        mBookmarks, mDeletions, mTags, mTagDeletions);
    mSubscription = AndroidObservable.bindActivity(this,
        mQuranSync.sync(client))
        .subscribe(new Action1<SyncResult>() {
          @Override
          public void call(SyncResult result) {
            mDeletions.clear();
            mBookmarks = result.bookmarks;
            mTags = result.tags;
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
      mDeletions.add(bookmark.getId());
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
        vh.tags = (TextView) convertView.findViewById(R.id.tags);
        vh.title = (TextView) convertView.findViewById(R.id.title);
        vh.tag = (ImageButton) convertView.findViewById(R.id.tag);
        vh.delete = (ImageButton) convertView.findViewById(R.id.delete);
        convertView.setTag(vh);
      }
      vh = (ViewHolder) convertView.getTag();

      final Bookmark bookmark = (Bookmark) getItem(position);
      vh.title.setText(bookmark.toString());
      final List<String> tags = getTagsFor(bookmark);
      vh.tags.setText(TextUtils.join(", ", tags));

      vh.tag.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          showTagsFragment(bookmark);
        }
      });
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
    public TextView tags;
    public TextView title;
    public ImageButton tag;
    public ImageButton delete;
  }
}

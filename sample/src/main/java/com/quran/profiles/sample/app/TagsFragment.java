package com.quran.profiles.sample.app;

import com.quran.profiles.library.api.model.Tag;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagsFragment extends DialogFragment {
  private EditText mTagEntry;
  private TagsAdapter mAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.tags_dialog, container, false);
    final ListView list = (ListView) view.findViewById(R.id.list_view);
    mAdapter = new TagsAdapter(getActivity());
    list.setAdapter(mAdapter);

    mTagEntry = (EditText) view.findViewById(R.id.text);
    final Button addTag = (Button) view.findViewById(R.id.add_tag);
    addTag.setOnClickListener(mOnClickListener);

    final Button doneButton = (Button) view.findViewById(R.id.done);
    doneButton.setOnClickListener(mOnClickListener);
    return view;
  }

  private View.OnClickListener mOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.add_tag: {
          mAdapter.addTag(mTagEntry.getText().toString());
          mTagEntry.setText("");
          break;
        }
        case R.id.done: {
          mAdapter.done();
        }
      }
    }
  };

  private class TagsAdapter extends BaseAdapter {
    private List<Tag> mTags;
    private List<Boolean> mSelectedStates;
    private List<Integer> mDeletedTagIds;
    private LayoutInflater mInflater;

    public TagsAdapter(Context context) {
      mInflater = LayoutInflater.from(context);
      if (context instanceof MainActivity) {
        final MainActivity activity = (MainActivity) context;
        mTags = activity.getTags();
        List<String> selectedTags = activity.getCurrentBookmarkTags();

        final Set<String> selectedMap = new HashSet<>();
        for (String tagName : selectedTags) {
          selectedMap.add(tagName);
        }

        mSelectedStates = new ArrayList<>();
        for (Tag tag : mTags) {
          mSelectedStates.add(selectedMap.contains(tag.getName()));
        }
        mDeletedTagIds = new ArrayList<>();
      }
    }

    @Override
    public int getCount() {
      return mTags == null ? 0 : mTags.size();
    }

    @Override
    public Object getItem(int position) {
      return mTags.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(final int position,
        View convertView, ViewGroup parent) {
      ViewHolder vh;
      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.tag_item, parent, false);
        vh = new ViewHolder();
        vh.title = (CheckBox) convertView.findViewById(R.id.title);
        vh.delete = (ImageButton) convertView.findViewById(R.id.delete);
        convertView.setTag(vh);
      }
      vh = (ViewHolder) convertView.getTag();
      vh.title.setChecked(mSelectedStates.get(position));

      final Tag tag = mTags.get(position);
      vh.title.setText(tag.getName());
      vh.title.setOnCheckedChangeListener(
          new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(
            CompoundButton buttonView, boolean isChecked) {
          mSelectedStates.remove(position);
          mSelectedStates.add(position, isChecked);
        }
      });
      vh.delete.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final Integer id = tag.getId();
          if (id != null) {
            mDeletedTagIds.add(id);
          }
          mTags.remove(position);
          mSelectedStates.remove(position);
          notifyDataSetChanged();
        }
      });
      return convertView;
    }

    public void addTag(String text) {
      final Tag t = new Tag(text);
      mTags.add(t);
      mSelectedStates.add(true);
      notifyDataSetChanged();
    }

    public void done() {
      final Activity activity = getActivity();
      if (activity instanceof MainActivity) {
        final List<String> bookmarkTags = new ArrayList<>();
        final int size = mSelectedStates.size();
        for (int i=0; i<size; i++) {
          if (mSelectedStates.get(i)) {
            bookmarkTags.add(mTags.get(i).getName());
          }
        }
        ((MainActivity) activity).saveTags(
            mTags, bookmarkTags, mDeletedTagIds);
      }
    }
  }

  public static class ViewHolder {
    public CheckBox title;
    public ImageButton delete;
  }
}

package com.devbrackets.android.exomediademo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.devbrackets.android.exomediademo.R;

import java.util.ArrayList;
import java.util.List;

public class StartupListAdapter extends BaseAdapter {
    public static final int INDEX_AUDIO_PLAYBACK = 0;
    public static final int INDEX_VIDEO_PLAYBACK = 1;

    private List<String> examplePages;
    private LayoutInflater inflater;

    public StartupListAdapter(Context context) {
        examplePages = new ArrayList<>();
        examplePages.add(INDEX_AUDIO_PLAYBACK, "Audio Playback");
        examplePages.add(INDEX_VIDEO_PLAYBACK, "Video Playback");

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return examplePages.size();
    }

    @Override
    public Object getItem(int position) {
        return examplePages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.simple_text_item, null);

            holder = new ViewHolder();
            holder.text = convertView.findViewById(R.id.simple_text_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(examplePages.get(position));
        return convertView;
    }


    private static class ViewHolder {
        TextView text;
    }
}

package com.devbrackets.android.exomediademo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.helper.VideoItems;

import java.util.List;

public class VideoSelectionListAdapter extends BaseAdapter {

    private List<VideoItems.VideoItem> items;
    private LayoutInflater inflater;

    public VideoSelectionListAdapter(Context context) {
        items = VideoItems.getItems();

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
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
            holder.text = (TextView) convertView.findViewById(R.id.simple_text_text_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(items.get(position).getTitle());
        return convertView;
    }

    private static class ViewHolder {
        TextView text;
    }
}
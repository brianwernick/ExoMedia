package com.devbrackets.android.exomediademo.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.data.Samples;

import java.util.List;

public class SampleListAdapter extends BaseAdapter {

    private List<Samples.Sample> samples;
    private LayoutInflater inflater;

    public SampleListAdapter(@NonNull Context context, @NonNull List<Samples.Sample> samples) {
        this.samples = samples;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return samples.size();
    }

    @Override
    public Object getItem(int position) {
        return samples.get(position);
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

        holder.text.setText(samples.get(position).getTitle());
        return convertView;
    }

    private static class ViewHolder {
        TextView text;
    }
}
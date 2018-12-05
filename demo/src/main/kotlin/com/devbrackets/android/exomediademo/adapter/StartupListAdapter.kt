package com.devbrackets.android.exomediademo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.devbrackets.android.exomediademo.R
import java.util.*

class StartupListAdapter(context: Context) : BaseAdapter() {
    companion object {
        const val INDEX_AUDIO_PLAYBACK = 0
        const val INDEX_VIDEO_PLAYBACK = 1
    }

    private val examplePages: MutableList<String>
    private val inflater: LayoutInflater

    init {
        examplePages = ArrayList()
        examplePages.add(INDEX_AUDIO_PLAYBACK, "Audio Playback")
        examplePages.add(INDEX_VIDEO_PLAYBACK, "Video Playback")

        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return examplePages.size
    }

    override fun getItem(position: Int): Any {
        return examplePages[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.simple_text_item, null)

            holder = ViewHolder()
            holder.text = convertView!!.findViewById(R.id.simple_text_text_view)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.text!!.text = examplePages[position]
        return convertView
    }


    private class ViewHolder {
        internal var text: TextView? = null
    }
}

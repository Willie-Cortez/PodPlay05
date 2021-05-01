package com.cortez.willie.podplay05.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cortez.willie.podplay05.R
import com.cortez.willie.podplay05.utilities.DateUtils
import com.cortez.willie.podplay05.utilities.HtmlUtils
import com.cortez.willie.podplay05.viewmodel.PodcastViewModel

class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private val episodeListAdapterListener: EpisodeListAdapterListener) :
    RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    class ViewHolder(
        v: View, private val episodeListAdapterListener: EpisodeListAdapterListener) :
        RecyclerView.ViewHolder(v) {

        init {
            v.setOnClickListener {
                episodeViewData?.let {
                    episodeListAdapterListener.onSelectedEpisode(it)
                }
            }
        }

        var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        val titleTextView: TextView = v.findViewById(R.id.titleView)
        val descTextView: TextView = v.findViewById(R.id.descView)
        val durationTextView: TextView = v.findViewById(R.id.durationView)
        val releaseDateTextView: TextView = v.findViewById(R.id.releaseDateView)
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): EpisodeListAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.episode_item, parent, false),
            episodeListAdapterListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]

        holder.episodeViewData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text =  HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let { DateUtils.dateToShortDate(it) }
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }
}
package com.cortez.willie.podplay05.repository


import android.arch.lifecycle.LiveData
import com.cortez.willie.podplay05.db.PodcastDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.cortez.willie.podplay05.model.Episode
import com.cortez.willie.podplay05.model.Podcast
import com.cortez.willie.podplay05.service.FeedService
import com.cortez.willie.podplay05.service.RssFeedResponse
import com.cortez.willie.podplay05.utilities.DateUtils

class PodcastRepo(
    private var feedService: FeedService,
    private var podcastDao: PodcastDao
) {

    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {

        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {

                feedService.getFeed(feedUrl) { feedResponse ->

                    if (feedResponse == null) {
                        GlobalScope.launch(Dispatchers.Main) {
                            callback(null)
                        }
                    } else {
                        val podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                        GlobalScope.launch(Dispatchers.Main) {
                            callback(podcast)
                        }
                    }
                }
            }
        }

    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String, imageUrl: String, rssResponse:
        RssFeedResponse
    ): Podcast? {

        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "") rssResponse.summary else rssResponse.description

        return Podcast(
            null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items)
        )
    }

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {

        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()

        val podcasts = podcastDao.loadPodcastsStatic()

        var processCount = podcasts.count()
        for (podcast in podcasts) {
            getNewEpisodes(podcast, { newEpisodes ->

                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(PodcastUpdateInfo(podcast.feedUrl, podcast.feedTitle, newEpisodes.count()))
                }
                processCount--

                if (processCount == 0) {
                    callback(updatedPodcasts)
                }
            })
        }
    }

    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    private fun getNewEpisodes(localPodcast: Podcast, callBack: (List<Episode>) -> Unit) {

        feedService.getFeed(localPodcast.feedUrl, { response ->

            if (response != null) {
                val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, response)
                remotePodcast?.let {

                    val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)

                    val newEpisodes = remotePodcast.episodes.filter { episode ->
                        localEpisodes.find { episode.guid == it.guid } == null
                    }

                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        })
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo (val feedUrl: String, val name: String, val newCount: Int)
}
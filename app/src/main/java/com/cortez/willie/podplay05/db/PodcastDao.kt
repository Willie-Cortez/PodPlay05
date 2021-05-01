package com.cortez.willie.podplay05.db

import android.arch.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import com.cortez.willie.podplay05.model.Episode
import com.cortez.willie.podplay05.model.Podcast
import androidx.room.Query

@Dao
interface PodcastDao {
    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    suspend fun loadEpisodes(podcastId: Long): List<Episode>

    @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
    suspend fun loadPodcast(url: String): Podcast?

    @Insert(onConflict = REPLACE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertEpisode(episode: Episode): Long

    @Delete
    suspend fun deletePodcast(podcast: Podcast)
}
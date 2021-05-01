package com.cortez.willie.podplay05.repository

import com.cortez.willie.podplay05.service.ItunesService
import com.cortez.willie.podplay05.service.PodcastResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ItunesRepo(private val itunesService: ItunesService) {

    fun searchByTerm(term: String, callBack: (List<PodcastResponse.ItunesPodcast>?) -> Unit) {

        val podcastCall = itunesService.searchPodcastByTerm(term)

        podcastCall.enqueue(object : Callback<PodcastResponse> {
            override fun onFailure(call: Call<PodcastResponse>?, t: Throwable?) {
                callBack(null)
            }

            override fun onResponse(call: Call<PodcastResponse>?, response: Response<PodcastResponse>?) {
                val body = response?.body()
                callBack(body?.results)
            }
        })
    }
}
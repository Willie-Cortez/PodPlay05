package com.cortez.willie.podplay05.ui

import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.method.ScrollingMovementMethod
import android.view.*
import com.bumptech.glide.Glide
import com.cortez.willie.podplay05.R
import com.cortez.willie.podplay05.adapter.EpisodeListAdapter
import com.cortez.willie.podplay05.adapter.EpisodeListAdapter.EpisodeListAdapterListener
import com.cortez.willie.podplay05.service.PodplayMediaService
import com.cortez.willie.podplay05.viewmodel.PodcastViewModel

class PodcastDetailsFragment : Fragment(), EpisodeListAdapterListener {

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: OnPodcastDetailsListener? = null
    private var menuItem: MenuItem? = null
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                startPlaying(episodeViewData)
            }
        } else {
            startPlaying(episodeViewData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setupViewModel()
        initMediaBrowser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupControls()
        updateControls()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_details, menu)
        menuItem = menu?.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {

                    if (podcastViewModel.activePodcastViewData?.subscribed == true) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnPodcastDetailsListener")
        }
    }

    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
        } else {
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
    }

    private fun setupControls() {
        feedDescTextView.movementMethod = ScrollingMovementMethod()
        episodeRecyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager
        val dividerItemDecoration = android.support.v7.widget.DividerItemDecoration(
            episodeRecyclerView.context,
            layoutManager.orientation)
        episodeRecyclerView.addItemDecoration(dividerItemDecoration)
        episodeListAdapter =
            EpisodeListAdapter(
                podcastViewModel.activePodcastViewData?.episodes,
                this
            )
        episodeRecyclerView.adapter = episodeListAdapter
    }
    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView.text = viewData.feedTitle
        feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl).into(feedImageView)
        }
    }
    private fun updateMenuItem() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title = if (viewData.subscribed) getString(R.string.unsubscribe)
        else getString(R.string.subscribe)
    }
    private fun setupViewModel() {
        activity?.let { activity ->
            podcastViewModel = ViewModelProviders.of(activity).get(PodcastViewModel::class.java)
        }
    }
    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }
    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(),
            null
        )
    }
    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()

        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
        }
        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // Disable transport controls
        }
        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            // Fatal error handling
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            println("metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            println("state changed to $state")
        }
    }
    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
    }
}
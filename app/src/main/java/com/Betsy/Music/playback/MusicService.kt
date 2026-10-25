/*
 * Noctune
 * Copyright (C) 2026 Betsy
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Based on InnerTune and OpenTune (GPL-3.0). Modified by Betsy, 2026.
 * Portions remain under the copyright of their original authors.
 */

@file:Suppress("DEPRECATION")

package com.betsy.music.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.betsy.audius.compat.AudiusApi
import com.betsy.audius.compat.models.SongItem
import com.betsy.audius.compat.models.WatchEndpoint
import com.betsy.music.MainActivity
import com.betsy.music.R
import com.betsy.music.constants.AudioNormalizationKey
import com.betsy.music.constants.AudioQualityKey
import com.betsy.music.constants.AutoLoadMoreKey
import com.betsy.music.constants.AutoSkipNextOnErrorKey
import com.betsy.music.constants.DisableLoadMoreWhenRepeatAllKey
import com.betsy.music.constants.HideExplicitKey
import com.betsy.music.constants.HistoryDuration
import com.betsy.music.constants.MediaSessionConstants.CommandToggleLike
import com.betsy.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.betsy.music.constants.MediaSessionConstants.CommandToggleShuffle
import com.betsy.music.constants.PauseListenHistoryKey
import com.betsy.music.constants.PersistentQueueKey
import com.betsy.music.constants.PlayerVolumeKey
import com.betsy.music.constants.RepeatModeKey
import com.betsy.music.constants.ShowLyricsKey
import com.betsy.music.constants.SimilarContent
import com.betsy.music.constants.SkipSilenceKey
import com.betsy.music.db.musicDatabase
import com.betsy.music.db.entities.Event
import com.betsy.music.db.entities.FormatEntity
import com.betsy.music.db.entities.LyricsEntity
import com.betsy.music.db.entities.RelatedSongMap
import com.betsy.music.di.DownloadCache
import com.betsy.music.di.PlayerCache
import com.betsy.music.extensions.SilentHandler
import com.betsy.music.extensions.collect
import com.betsy.music.extensions.collectLatest
import com.betsy.music.extensions.currentMetadata
import com.betsy.music.extensions.findNextMediaItemById
import com.betsy.music.extensions.mediaItems
import com.betsy.music.extensions.metadata
import com.betsy.music.extensions.toMediaItem
import com.betsy.music.extensions.toQueue
import com.betsy.music.lyrics.LyricsHelper
import com.betsy.music.models.PersistPlayerState
import com.betsy.music.models.PersistQueue
import com.betsy.music.models.toMediaMetadata
import com.betsy.music.playback.queues.EmptyQueue
import com.betsy.music.playback.queues.Queue
import com.betsy.music.playback.queues.StreamingQueue
import com.betsy.music.playback.queues.filterExplicit
import com.betsy.music.utils.AccountManager
import com.betsy.music.utils.CoilBitmapLoader
import com.betsy.music.utils.GuestManager
import com.betsy.music.utils.NetworkConnectivityObserver
import com.betsy.music.utils.AudiusPlayerUtils
import com.betsy.music.utils.dataStore
import com.betsy.music.utils.enumPreference
import com.betsy.music.utils.get
import com.betsy.music.utils.reportException
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class musicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: musicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false

    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = musicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.betsy.music.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.betsy.music.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                val id = mediaMetadata?.id
                if (id != null) database.song(id) else kotlinx.coroutines.flow.flowOf(null)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var lastPlaybackSpeed = 1.0f

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var consecutivePlaybackErr = 0

    private var controllerFuture: ListenableFuture<MediaController>? = null

    
    
    
    
    private val trackingUrlCache = HashMap<String, String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        scope.launch(Dispatchers.IO) {
            GuestManager.getOrCreateGuestId(dataStore)
            AccountManager.migrateFromLegacyIfNeeded(dataStore)
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.ic_notification)
                },
        )
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@musicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@musicService))
                }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        val sessionToken = SessionToken(this, ComponentName(this, musicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture!!.addListener(
            {
                runCatching { controllerFuture?.get() }
            },
            MoreExecutors.directExecutor()
        )

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        playerVolume.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { _ ->
            updateNotification()
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            setupLoudnessEnhancer()
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false,
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                scope.launch {
                    delay(1000)
                    player.repeatMode = playerState.repeatMode
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }
                }
                scope.launch {
                    delay(1300)
                    if (player.currentMediaItem != null) {
                        currentMediaMetadata.value = player.currentMetadata
                    }
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = false
                if (player.isPlaying) player.pause()
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) player.pause()
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) player.volume = (playerVolume.value * 0.2f)
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true
                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    fun hasAudioFocusForPlayback(): Boolean = hasAudioFocus

    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex
        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }
        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: AudiusPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.durationSeconds ?: AudiusPlayerUtils.durationFor(mediaId).getOrNull())
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                AudiusApi.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = AudiusApi.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach { insert(it) }
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach { insert(it) }
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        Log.d(TAG, "▶ playQueue() called | queue=${queue::class.simpleName} | playWhenReady=$playWhenReady | preloadItem=${queue.preloadItem?.id}")
        if (queue.preloadItem != null) {
            val preload = queue.preloadItem!!.toMediaItem()
            player.setMediaItem(preload)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            Log.d(TAG, "  initialStatus: items=${initialStatus.items.size} mediaItemIndex=${initialStatus.mediaItemIndex} position=${initialStatus.position}")
            if (initialStatus.items.isEmpty()) {
                Log.w(TAG, "  initialStatus.items is EMPTY – nothing to play!")
                return@launch
            }
            if (queue.preloadItem != null) {
                player.addMediaItems(
                    0,
                    initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                )
                player.addMediaItems(
                    initialStatus.items.subList(
                        initialStatus.mediaItemIndex + 1,
                        initialStatus.items.size
                    )
                )
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0,
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }
        scope.launch(SilentHandler) {
            val radioQueue = StreamingQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            AudiusApi.album(albumId).onSuccess {
                getAutomix(it.album.playlistId)
            }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                AudiusApi.next(WatchEndpoint(playlistId = playlistId)).onSuccess {
                    AudiusApi.next(WatchEndpoint(playlistId = it.endpoint.playlistId)).onSuccess {
                        automixItems.value = it.items.map { song -> song.toMediaItem() }
                    }
                }
            }
        }
    }

    fun addToQueueAutomix(item: MediaItem, position: Int) {
        automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(item: MediaItem, position: Int) {
        automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }
        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled
        player.addMediaItems(insertIndex, items)
        player.prepare()
        if (shuffleEnabled) {
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }
                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse()
                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }
                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLike())
            }
        }
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }
        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }
                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }
                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }
                    val loudnessDb = format?.loudnessDb
                    withContext(Dispatchers.Main) {
                        if (loudnessDb != null) {
                            val targetGain = (-loudnessDb * 100).toInt()
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                            } catch (e: Exception) {
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Log.w(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastPlaybackSpeed = -1.0f
        setupLoudnessEnhancer()
        consecutivePlaybackErr = 0
        Log.d(TAG, "⏭ onMediaItemTransition | mediaId=${mediaItem?.mediaId} | isLocal=${mediaItem?.mediaId?.startsWith("device_")}")
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        val stateStr = when (playbackState) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN($playbackState)"
        }
        Log.d(TAG, "🎵 onPlaybackStateChanged → $stateStr | mediaId=${player.currentMediaItem?.mediaId} | isLocal=${player.currentMediaItem?.mediaId?.startsWith("device_")}")
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }
        if (playbackState == Player.STATE_ENDED) {
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    currentMediaMetadata.value = null
                }
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) setupLoudnessEnhancer()
        scope.launch {
            delay(300)
            updateNotification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) openAudioEffectSession()
            } else {
                closeAudioEffectSession()
                if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    abandonAudioFocus()
                }
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            scope.launch {
                delay(200)
                updateNotification()
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            if (player.mediaItemCount == 0) return
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        val currentItem = player.currentMediaItem
        Log.e(TAG, "════════════════════════════════════════")
        Log.e(TAG, "❌ onPlayerError | code=${error.errorCode} (${error.errorCodeName})")
        Log.e(TAG, "   message      : ${error.message}")
        Log.e(TAG, "   cause        : ${error.cause?.javaClass?.simpleName} – ${error.cause?.message}")
        Log.e(TAG, "   rootCause    : ${error.cause?.cause?.javaClass?.simpleName} – ${error.cause?.cause?.message}")
        Log.e(TAG, "   currentItem  : mediaId=${currentItem?.mediaId}")
        Log.e(TAG, "   uri          : ${currentItem?.localConfiguration?.uri}")
        Log.e(TAG, "   customKey    : ${currentItem?.localConfiguration?.customCacheKey}")
        Log.e(TAG, "   isLocal      : ${currentItem?.mediaId?.startsWith("device_")}")
        Log.e(TAG, "   networkConn  : ${isNetworkConnected.value}")
        Log.e(TAG, "════════════════════════════════════════")
        val isConnectionError = (error.cause?.cause is PlaybackException) &&
                (error.cause?.cause as PlaybackException).errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        if (!isNetworkConnected.value || isConnectionError) {
            waitOnNetworkError()
            return
        }
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) skipOnError() else stopOnError()
    }

       
                                                                                                 
                                                                                             
                                                              
       
    private fun isFullyInPlayerCache(mediaId: String, position: Long): Boolean {
        val length = ContentMetadata.getContentLength(playerCache.getContentMetadata(mediaId))
        if (length <= 0L) return false
        val start = position.coerceAtLeast(0L)
        return start < length && playerCache.isCached(mediaId, start, length - start)
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(
                                OkHttpClient.Builder().proxy(AudiusApi.proxy).build(),
                            ),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaIdFromKey = dataSpec.key
            val uriString = dataSpec.uri.toString()

            Log.d(TAG, "🔍 DataSource resolve | key=$mediaIdFromKey | uri=$uriString | position=${dataSpec.position} | length=${dataSpec.length}")

            val isLocalItem =
                (mediaIdFromKey != null && mediaIdFromKey.startsWith("device_")) ||
                        uriString.startsWith("content://media/")

            if (isLocalItem) {
                Log.d(TAG, "📂 LOCAL item detected → passing dataSpec as-is to ContentDataSource | uri=$uriString | key=$mediaIdFromKey")
                return@Factory dataSpec
            }

            val mediaId = mediaIdFromKey
                ?: run {
                    Log.e(TAG, "❌ dataSpec.key is NULL for non-local item (uri=$uriString)")
                    error("No media id in dataSpec.key for non-local item (uri=$uriString)")
                }

            if (downloadCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1) ||
                isFullyInPlayerCache(mediaId, dataSpec.position)
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }
            try {
                val playbackData = runBlocking(Dispatchers.IO) {
                    AudiusPlayerUtils.playerResponseForPlayback(
                        mediaId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    )
                }.getOrElse { throwable ->
                    when (throwable) {
                        is PlaybackException -> throw throwable
                        is ConnectException, is UnknownHostException -> throw PlaybackException(
                            getString(R.string.error_no_internet), throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        )
                        is SocketTimeoutException -> throw PlaybackException(
                            getString(R.string.error_timeout), throwable,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        )
                        else -> throw PlaybackException(
                            getString(R.string.error_unknown), throwable,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                }
                val format = playbackData.format
                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType,
                            codecs = format.codecs,
                            bitrate = format.bitrate,
                            sampleRate = format.sampleRate,
                            contentLength = format.contentLength,
                            loudnessDb = null,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }
                val streamUrl = playbackData.streamUrl
                songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)

                
                
                
                

                
                
                
                return@Factory dataSpec.withUri(streamUrl.toUri())
            } catch (e: Exception) {
                Timber.tag("Audius").e(e, "Error al resolver el stream de Audius")
                throw e
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            DefaultExtractorsFactory(),
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink.Builder(this@musicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        if (playbackStats.totalPlayTimeMs >= (
                    dataStore[HistoryDuration]?.times(1000f) ?: 30000f
                    ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            
            
            
            
            
            scope.launch(Dispatchers.IO) {
                
                val existingSong = database.song(mediaItem.mediaId).first()
                if (existingSong == null) {
                    val meta = withContext(Dispatchers.Main) {
                        player.findNextMediaItemById(mediaItem.mediaId)?.metadata
                    }
                    if (meta != null) {
                        database.query { insert(meta) }
                        Log.d(TAG, "📝 Inserted local song into DB: ${mediaItem.mediaId}")
                    } else {
                        Log.w(TAG, "📝 No metadata for ${mediaItem.mediaId}, skipping Event")
                        return@launch
                    }
                }

                
                database.query {
                    incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                    try {
                        insert(
                            Event(
                                songId = mediaItem.mediaId,
                                timestamp = LocalDateTime.now(),
                                playTime = playbackStats.totalPlayTimeMs,
                            ),
                        )
                        Log.d(TAG, "📝 Event inserted for ${mediaItem.mediaId}")
                    } catch (e: SQLException) {
                        Log.e(TAG, "❌ Event insert FAILED for ${mediaItem.mediaId}: ${e.message}", e)
                    }
                }
            }

            
            
            
            
            val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")
            if (!dataStore.get(PauseRemoteListenHistoryKey, false)) {
                val mediaId = mediaItem.mediaId
                
                if (!mediaId.startsWith("device_")) {
                    CoroutineScope(Dispatchers.IO).launch {
                        
                    }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }
        try {
            val persistQueue = PersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                position = if (player.currentPosition >= 0) player.currentPosition else 0,
            )
            val persistAutomix = PersistQueue(
                title = "automix",
                items = automixItems.value.mapNotNull { it.metadata },
                mediaItemIndex = 0,
                position = 0,
            )
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = player.playWhenReady,
                playbackState = player.playbackState
            )
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos -> oos.writeObject(persistQueue) }
                }
            }.onFailure { Log.e(TAG, "Error saving queue to disk", it); reportException(it) }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos -> oos.writeObject(persistAutomix) }
                }
            }.onFailure { Log.e(TAG, "Error saving automix to disk", it); reportException(it) }
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos -> oos.writeObject(playerState) }
                }
            }.onFailure { Log.e(TAG, "Error saving player state to disk", it); reportException(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        controllerFuture?.cancel(false)
        controllerFuture = null
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class musicBinder : Binder() {
        val service: musicService
            get() = this@musicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"

        private const val MAX_GAIN_MB = 800
        private const val MIN_GAIN_MB = -800
        private const val TAG = "musicService"
    }
}
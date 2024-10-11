package ru.madbrains.smartyard.ui.main.intercom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.TimeZone
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.madbrains.data.DataModule
import ru.madbrains.domain.model.response.CameraCctvItemItem
import ru.madbrains.domain.model.response.Cctv
import ru.madbrains.domain.model.response.PlaceItemItem
import ru.madbrains.smartyard.DiskCache
import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.AddressViewModel
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVDetailFragment
import ru.madbrains.smartyard.ui.main.address.cctv_video.CCTVViewModel
import ru.madbrains.smartyard.ui.main.burger.cityCameras.CityCameraFragment
import ru.madbrains.smartyard.ui.main.intercom.IntercomComposableFragment.Companion.OPEN_EVENT_LOG
import ru.madbrains.smartyard.ui.main.intercom.IntercomComposableFragment.Companion.RESET_CODE_DOOR
import ru.madbrains.smartyard.ui.main.intercom.IntercomComposableFragment.Companion.SHARE_OPEN_URL
import ru.madbrains.smartyard.ui.main.settings.accessAddress.AccessAddressFragment
import ru.madbrains.smartyard.ui.player.ExoPlayerViewModel.Companion.dateTimeFormatForExif
import ru.madbrains.smartyard.ui.readExifData
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


enum class DiffExifTime(val char: String) {
    DAY("Д"),
    HOUR("Ч"),
    MINUTE("М"),
    SECONDS("C")
}

data class BurgerMenuItem(
    val name: String,
    val icon: Int? = null,
)

data class CacheImage(
    val bitmap: Bitmap,
    val date: String?,
)


@Composable
fun MainIntercom(
    mCCTVViewModel: CCTVViewModel = viewModel(),
    mViewModel: AddressViewModel = viewModel(),
    onNavigate: (Fragment, Bundle?) -> Unit,
) {
    val viewModelList = remember { mCCTVViewModel }
    val intercomViewModel = remember { mViewModel }

    Recycler(viewModelList, intercomViewModel) { fragment, bundle ->
        onNavigate(fragment, bundle)
    }
}


@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Recycler(
    mCCTVViewModel: CCTVViewModel,
    mViewModel: AddressViewModel,
    onNavigate: (Fragment, Bundle?) -> Unit,
) {
    val camerasList = mViewModel.camerasList.observeAsState()
    val placesList = mViewModel.placesList.observeAsState()
    val isLoading = mViewModel.isLoading.observeAsState()
    val isPlaying = mViewModel.playingVideoId.observeAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pagerState by remember { mutableStateOf<PagerState?>(null) }

    fun refresh() = coroutineScope.launch {
        refreshing = true
        mViewModel.refresh()
        delay(1500)
        refreshing = false
    }

    val state = rememberPullRefreshState(refreshing = refreshing, onRefresh = ::refresh)
    if (placesList.value == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlaceSpiner(Modifier.size(40.dp))
        }
    }

    Column(
        modifier = Modifier
            .pullRefresh(state)
            .verticalScroll(scrollState),
    ) {
        placesList.value?.let { list ->
            CameraItem(
                data = camerasList.value,
                onNavigate = { fragment, bundle ->
                    onNavigate(fragment, bundle)
                },
                choseCamera = { id ->
                    val cItem = camerasList.value?.firstOrNull { cl -> cl.id == id }
                    if (cItem != null && !cItem.isCityCctv) {
                        mCCTVViewModel.chooseCameraById(cItem.id)
                        onNavigate(CCTVDetailFragment(), null)
                    } else {
                        val bundle = Bundle()
                        bundle.putInt("id", id)
                        onNavigate(CityCameraFragment(), bundle)
                    }
                }
            )

            list.forEachIndexed { index, item ->
                Spacer(modifier = Modifier.height(10.dp))
                if (item.cctv.count() > 1) {
                    Pager(item.cctv.count()) { pagerIndex, state ->
                        pagerState = state
                        IntercomItemComposable(
                            id = item.cctv[pagerIndex].id,
                            isPlaying = isPlaying.value == item.cctv[pagerIndex].id,
                            data = item,
                            isLoading = if (isLoading.value?.first == item.flatId) isLoading.value?.second
                                ?: false else false,
                            index = pagerIndex,
                            onNavigate = { fragment, bundle ->
                                onNavigate(fragment, bundle)
                            },
                            choseCamera = { id ->
                                camerasList.value?.let {
                                    mCCTVViewModel.chooseCameraById(id)
                                    onNavigate(CCTVDetailFragment(), null)
                                }
                            },
                            share = {
                                mViewModel.generateOpenUrl(
                                    item.houseId,
                                    item.flatNumber,
                                    item.domophoneId.toLong()
                                )
                            },
                            openDoor = {
                                mViewModel.openDoor(item.domophoneId.toLong(), item.doorId)
                            },
                            playVideoId = { videoId ->
                                mViewModel.setPlayingId(videoId)
                            },
                            fullScreen = {
                                val bundle = Bundle()
                                bundle.putLong("domophoneId", item.domophoneId.toLong())
                                bundle.putInt("doorId", item.doorId)
                                bundle.putString("title", list[index].cctv[pagerIndex].name)
                                bundle.putString("name", list[index].cctv[pagerIndex].name)
                                bundle.putString(
                                    "uriHls",
                                    list[index].cctv[pagerIndex].videoUrl
                                )
                                onNavigate(ExoPlayerIntercomWebView(), bundle)
                            }
                        )
                    }
                } else {
                    item.cctv.firstOrNull()?.let {
                        IntercomItemComposable(
                            data = item,
                            id = it.id,
                            isPlaying = isPlaying.value == it.id,
                            index = 0,
                            onNavigate = { fragment, bundle ->
                                onNavigate(fragment, bundle)
                            },
                            playVideoId = { videoId ->
                                mViewModel.setPlayingId(videoId)
                            },
                            choseCamera = { id ->
                                camerasList.value?.let {
                                    mCCTVViewModel.chooseCameraById(id)
                                    onNavigate(CCTVDetailFragment(), null)
                                }
                            },
                            share = {
                                mViewModel.generateOpenUrl(
                                    item.houseId,
                                    item.flatNumber,
                                    item.domophoneId.toLong()
                                )
                            },
                            openDoor = {
                                mViewModel.openDoor(item.domophoneId.toLong(), item.doorId)
                            },
                            fullScreen = {
                                val bundle = Bundle()
                                bundle.putLong("domophoneId", item.domophoneId.toLong())
                                bundle.putInt("doorId", item.doorId)
                                bundle.putString("title", it.name)
                                bundle.putString("name", it.name)
                                bundle.putString("uriHls", it.videoUrl)
                                onNavigate(ExoPlayerIntercomWebView(), bundle)
                            },
                            isLoading = if (isLoading.value?.first == item.flatId) isLoading.value?.second
                                ?: false else false,
                        )
                    }
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .pullRefresh(state)
    ) {
        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.CenterHorizontally))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pager(count: Int, composable: @Composable (Int, PagerState) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { count })
    val coroutineScope = rememberCoroutineScope()
    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fill,
        pageSpacing = 4.dp
    ) { page ->
        composable(page, pagerState)
    }
    pagerState.let {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(it.pageCount) { iteration ->
                val color = if (it.currentPage == iteration) Color.Red else Color.LightGray
                Box(modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(8.dp)
                    .clickable {
                        coroutineScope.launch {
                            it.scrollToPage(iteration)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraItem(
    data: List<CameraCctvItemItem>?,
    onNavigate: (Fragment, Bundle?) -> Unit,
    choseCamera: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(all = 8.dp)
    ) {
        CardHeader(
            share = {},
            onNavigate = { fragment, bundle ->
                onNavigate(fragment, bundle)
            },
            isLoading = false
        )
        Column(
            modifier = Modifier.height(169.dp)
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                CamList(data) {
                    choseCamera(it)
                }
            }
        }
    }
}

@Composable
private fun IntercomItemComposable(
    data: PlaceItemItem,
    index: Int,
    id: Int,
    isLoading: Boolean,
    choseCamera: (Int) -> Unit,
    onNavigate: (Fragment, Bundle?) -> Unit,
    share: () -> Unit,
    openDoor: () -> Unit,
    fullScreen: () -> Unit,
    playVideoId: (Int?) -> Unit,
    isPlaying: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(all = 8.dp)
    ) {
        CardHeader(data,
            onNavigate = { fragment, bundle ->
                onNavigate(fragment, bundle)
            }, share = {
                share()
            },
            isLoading = isLoading
        )
        Column(
            modifier = Modifier
                .aspectRatio(16f / 9f)
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Box(contentAlignment = Alignment.BottomStart) {
                    ShowPreview(
                        data = data.cctv[index],
                        urlPreview = data.cctv[index].screenShotPreview(DataModule.URL),
                        id = id,
                        openDoor = {
                            openDoor()
                        },
                        choseCamera = { id ->
                            choseCamera(id)
                        },
                        fullScreen = {
                            fullScreen()
                        },
                        playVideoId = { videoId ->
                            playVideoId(videoId)
                        },
                        isPlaying = isPlaying
                    )
                }
                Text(
                    text = data.cctv[index].name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, blurRadius = 7f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val mediaItems = arrayListOf<MediaItem>()
    url.let {
        mediaItems.add(
            MediaItem.Builder()
                .setUri(it)
                .setMediaId(it)
                .setTag(it)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setDisplayTitle("Test Name Display")
                        .build()
                ).build()
        )
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            this.setMediaItems(mediaItems)
            this.prepare()
            this.playWhenReady = true
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Timber.e(error, "EXOPLAYER LISTNER")
                }
            })
        }
    }

    ConstraintLayout {
        val (title, videoPlayer) = createRefs()
        DisposableEffect(
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .clip(RoundedCornerShape(10.dp))
                    .constrainAs(videoPlayer) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        this.useController = false
                        layoutParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams
                                    .MATCH_PARENT,
                                ViewGroup.LayoutParams
                                    .MATCH_PARENT
                            )
                    }
                }
            )
        ) {
            onDispose {
                exoPlayer.release()
            }
        }
    }
}

private fun downloadFileWithExif(context: Context, url: String): CacheImage? {
    return try {
        val file = Glide.with(context)
            .asFile()
            .load(url)
            .timeout(3000)
            .skipMemoryCache(true)
            .signature(ObjectKey(Date()))
            .submit()
            .get()

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val date = readExifData(file)
        Timber.d("EXIFDASDASD date:$date url:$url")
        CacheImage(bitmap, date)
    } catch (e: Exception) {
        Timber.e(e, "downloadImageWithExif() EXCETPION")
        null
    }
}


@Composable
private fun lifeCycle(lifeCycleOwner: LifecycleOwner, id: Int): Boolean {
    var isPause by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        lifeCycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                isPause = true
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                isPause = false
            }
        })
    }
    return isPause
}


@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun ShowPreview(
    data: Cctv,
    urlPreview: String,
    choseCamera: (Int) -> Unit,
    openDoor: () -> Unit,
    fullScreen: () -> Unit,
    playVideoId: (Int?) -> Unit,
    id: Int,
    isPlaying: Boolean,
) {
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    var expanded by remember { mutableStateOf(false) }
    var isPause by remember { mutableStateOf(false) }
    var counter by remember { mutableStateOf(0) }

    val cache = remember { DiskCache.getInstance(context) }
    val imageCache = remember { cache.get(data.id.toString()) }
    var bitmapImage by remember { mutableStateOf<Bitmap?>(imageCache) }
    var lastUpdateDate by remember { mutableStateOf("") }
    var lastUpdateString by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf<Timer?>(null) }
    var lastUpdateTimer by remember { mutableStateOf<Timer?>(null) }
    val coroutineScope = rememberCoroutineScope()
    isPause = lifeCycle(lifeCycleOwner = lifeCycleOwner, data.id)


    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isPlaying) {
            VideoPlayer(url = data.videoUrl)
        } else {
            if (bitmapImage == null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Gray)
                        .fillMaxSize()
                        .clickable { expanded = !expanded },
                ) {
                    PlaceSpiner()
                }
            } else {
                bitmapImage?.let {
                    Box {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { expanded = !expanded },
                        )
                        if (lastUpdateDate.isNotEmpty() && !isPause) {
                            DisposableEffect(Unit) {
                                lastUpdateTimer = Timer()
                                lastUpdateTimer?.let {
                                    startTimer(it, 100L, 1000L) {
                                        val lastUpd = lastUpdateImageStr(lastUpdateDate)
                                        lastUpdateString = lastUpd.ifEmpty { "0 C" }
                                    }
                                }

                                onDispose {
                                    onDisposeTimer(lastUpdateTimer) {
                                        lastUpdateTimer = null
                                    }
                                }
                            }

                            Text(
                                text = lastUpdateString,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                textAlign = TextAlign.Right,
                                color = Color.White,
                                style = TextStyle(
                                    shadow = Shadow(color = Color.Black, blurRadius = 7f),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            onDisposeTimer(lastUpdateTimer) {
                                lastUpdateTimer = null
                            }
                        }
                        Text(text = "$id", color = Color.Red)
                    }
                }
            }
        }
        PlayerController(
            openDoor = { openDoor() },
            playVideo = { playVideoId(if (it) id else null) },
            isPlaying = isPlaying,
            fullScreen = { fullScreen() }
        )
    }

    if (!isPause && counter < 2) {
        DisposableEffect(Unit) {
            if (timer == null) {
                timer = Timer()
                timer?.let {
                    startTimer(it, 0L, 6000L) {
                        val cacheImage = downloadFileWithExif(context = context, url = urlPreview)
                        lastUpdateDate = cacheImage?.date ?: ""
                        bitmapImage = cacheImage?.bitmap
                        if (bitmapImage == null) {
                            counter++
                        } else {
                            bitmapImage?.let { bitmap ->
                                coroutineScope.launch {
                                    cache.put(data.id.toString(), bitmap)
                                }
                            }
                        }
                    }
                }
            }

            onDispose {
                onDisposeTimer(timer, lastUpdateTimer) {
                    timer = null
                    lastUpdateTimer = null
                }
            }
        }
    } else {
        onDisposeTimer(timer, lastUpdateTimer) {
            timer = null
            lastUpdateTimer = null
        }
    }

    if (expanded) {
        choseCamera(data.id)
        expanded = false
    }
}

@Composable
private fun PlayerController(
    openDoor: () -> Unit,
    fullScreen: () -> Unit,
    isPlaying: Boolean,
    playVideo: (Boolean) -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }

    if (isOpen) {
        LaunchedEffect(Unit) {
            openDoor()
            delay(3000)
            isOpen = false
        }
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 3.dp)
            .fillMaxWidth()
    ) {
        IconButton(
            onClick = {
                playVideo(!isPlaying)
            },
            modifier = Modifier
                .background(Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ca_circle),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .alpha(0.3f)
            )
            Icon(
                modifier = Modifier.size(35.dp),
                painter = if (isPlaying) painterResource(id = R.drawable.ca_pause) else painterResource(
                    id = R.drawable.ca_play
                ),
                contentDescription = null,
                tint = Color.White,
            )
        }

        IconButton(
            enabled = !isOpen,
            onClick = {
                isOpen = !isOpen
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ca_circle),
                contentDescription = null,
                modifier = Modifier
                    .width(70.dp)
                    .height(70.dp)
                    .alpha(0.3f)
            )
            Icon(
                painter = painterResource(id = R.drawable.unlocktemp),
                modifier = Modifier
                    .width(35.dp)
                    .height(35.dp),
                contentDescription = null,
                tint = if (!isOpen) Color.White else Color.Green
            )

        }
        IconButton(
            onClick = {
                fullScreen()
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ca_circle),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .alpha(0.3f)
            )
            Icon(
                modifier = Modifier.scale(1.0f),
                painter = painterResource(id = R.drawable.ca_full_screen),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}


@Composable
private fun CardHeader(
    data: PlaceItemItem? = null,
    share: () -> Unit,
    onNavigate: (Fragment, Bundle?) -> Unit?,
    isLoading: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 5.dp, bottom = 6.dp, top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (data == null) {
                    R.drawable.ic_video_camera
                } else {
                    when (data.icon) {
                        "barrier" -> R.drawable.ic_barrier
                        "gate" -> R.drawable.ic_gates
                        "wicket" -> R.drawable.ic_wicket
                        "entrance" -> R.drawable.ic_porch
                        else -> R.drawable.ic_porch
                    }
                }
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 5.dp)
                )
                if (icon != R.drawable.ic_barrier) {
                    Column(
                        modifier = Modifier.padding(end = 7.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = if (isLoading) Alignment.CenterHorizontally else Alignment.Start
                    ) {
                        val name = data?.doorCode ?: "Камеры"
                        if (!isLoading) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.subtitle1
                            )
                        } else {
                            PlaceSpiner()
                        }
                        val text = if (data == null) "Онлайн | Архив" else "Код открытия"
                        Text(
                            text = text.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                }
                if (data != null) {
                    val id = "${data.flatNumber}${data.domophoneId}"
                    Share(id) {
                        share()
                    }
                }
            }
            Box {
                BurgerButton(data,
                    onNavigate = { fragment, bundle ->
                        onNavigate(fragment, bundle)
                    },
                    share = {
                        share()
                    }
                )
            }
        }
    }
}

private fun shareAccess(context: Context, link: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, link)
        type = "text/plain"
    }
    context.startActivity(
        Intent.createChooser(
            sendIntent,
            null
        )
    )
}

@Composable
private fun Share(id: String, share: () -> Unit) {
    val context = LocalContext.current
    BroadCastManager(context, id)
    Box(modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false, radius = 15.dp)
        ) {
            share()
        }) {
        Image(
            painter = painterResource(id = R.drawable.ic_share_intercom),
            contentDescription = null
        )
    }
}

@Composable
fun BroadCastManager(context: Context, id: String) {
    var expanded by remember { mutableStateOf(false) }
    val broadCastMsg = remember { mutableStateOf("") }

    val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val message = intent.getStringExtra("message")
            val idIntent = intent.getStringExtra("id")
            if (message != null && idIntent == id) {
                broadCastMsg.value = message
                expanded = true
            }
        }
    }
    if (expanded) {
        shareAccess(LocalContext.current, broadCastMsg.value)
        expanded = false
    }
    LocalBroadcastManager.getInstance(context).registerReceiver(
        broadcastReceiver, IntentFilter(SHARE_OPEN_URL)
    )
}


@Composable
private fun ItemBurger(burgerMenuItems: BurgerMenuItem, onAction: @Composable () -> Unit) {
    var actionClick by remember { mutableStateOf(false) }
    DropdownMenuItem(onClick = {
        actionClick = !actionClick
    }) {
        if (burgerMenuItems.icon != null) {
            Icon(
                painter = painterResource(id = burgerMenuItems.icon),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .padding(start = 0.dp, end = 9.dp)
                    .size(20.dp),
            )
        }
        Text(text = burgerMenuItems.name, style = MaterialTheme.typography.body1)
    }
    if (actionClick) {
        actionClick = false
        onAction()
    }
}

@Composable
private fun BurgerButton(
    data: PlaceItemItem? = null,
    onNavigate: (Fragment, Bundle?) -> Unit,
    share: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val eventItem = BurgerMenuItem("Список событий", R.drawable.list_events)
    val resetCodeDoorItem = BurgerMenuItem("Обновить код открытия", R.drawable.refresh_door_code)
    val settingsFaceIdItem = BurgerMenuItem("Настроить FaceID", R.drawable.faceid_settings)
    val shareOpenItem = BurgerMenuItem("Поделиться доступом", R.drawable.share_address)
    val cameraItem = BurgerMenuItem("Все камеры в архиве", null)

    Icon(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 15.dp)
            ) {
                expanded = !expanded
            }
            .size(16.dp),
        painter = painterResource(id = R.drawable.ic_camera_burger),
        contentDescription = null,
        tint = MaterialTheme.colors.primary
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        if (data != null) {
            ItemBurger(burgerMenuItems = eventItem) {
                val intent = Intent(OPEN_EVENT_LOG)
                intent.putExtra("houseId", data.houseId)
                intent.putExtra("address", data.address)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                expanded = false

            }
            if (data.icon != "barrier") {
                ItemBurger(burgerMenuItems = resetCodeDoorItem) {
                    val intent = Intent(RESET_CODE_DOOR)
                    intent.putExtra("flatId", data.flatId)
                    intent.putExtra("domophoneId", data.domophoneId.toLong())
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    expanded = false
                }
            }
            ItemBurger(burgerMenuItems = settingsFaceIdItem) {
                val bundleSettings = Bundle()
                bundleSettings.putString("address", "${data.address} квартира ${data.flatNumber}")
                bundleSettings.putString("clientId", data.clientId.toString())
                bundleSettings.putInt("flatId", data.flatId)
                bundleSettings.putBoolean("hasGates", false)
                bundleSettings.putBoolean("contractOwner", data.contractOwner == "t")
                if (!bundleSettings.isEmpty) {
                    onNavigate(AccessAddressFragment(), bundleSettings)
                }
                expanded = false
            }
            ItemBurger(burgerMenuItems = shareOpenItem) {
                share()
                val id = "${data.flatNumber}${data.domophoneId}"
                BroadCastManager(context = context, id = id)
                expanded = false
            }
        } else {
            ItemBurger(burgerMenuItems = cameraItem) {
                onNavigate(CCTVDetailFragment(), null)
                expanded = false
            }
        }

    }
}

@Composable
private fun CamList(itemCam: List<CameraCctvItemItem>?, choseCamera: (Int) -> Unit) {
    val ic = remember(key1 = itemCam) { itemCam }
    val modifier = Modifier
        .width(225.dp)
        .fillMaxHeight()

    if (ic != null) {
        LazyRow {
            item {
                ItemCam(data = ic.first(), modifier) {
                    choseCamera(it)
                }
            }
            itemsIndexed(ic, key = { _, item -> item.id }) { index, _ ->
                Column {
                    if (index % 2 != 0) {
                        ItemCam(ic[index]) {
                            choseCamera(it)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        if (index + 1 in ic.indices) {
                            ItemCam(ic[index + 1]) {
                                choseCamera(it)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(225.dp)
                    .fillMaxHeight()
                    .background(Color.Gray)
            ) {
                PlaceSpiner()
            }
            Spacer(modifier = Modifier.width(2.dp))
            Column {
                repeat(2) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(140.dp)
                            .height(85.dp)
                            .background(Color.Gray)
                    ) {
                        PlaceSpiner()
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun ItemCam(
    data: CameraCctvItemItem,
    modifier: Modifier? = null,
    choseCamera: (Int) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current
    var isPause by remember { mutableStateOf(false) }
    isPause = lifeCycle(lifeCycleOwner = lifecycle, data.id)
    val context = LocalContext.current
    val cache = remember { DiskCache.getInstance(context) }
    val imageCache = cache.get(data.id.toString())
    var expanded by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf<Timer?>(null) }
    var bitmapImage by remember { mutableStateOf<Bitmap?>(imageCache) }
    var counter by remember { mutableStateOf(0) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var lastUpdateDate by remember { mutableStateOf("") }
    var lastUpdateString by remember { mutableStateOf("") }
    var lastUpdateTimer by remember { mutableStateOf<Timer?>(null) }


    if (expanded) {
        expanded = false
        choseCamera(data.id)
    }

    val mod = modifier ?: Modifier
        .width(140.dp)
        .height(85.dp)
    Column(modifier = mod.onGloballyPositioned { coordinates = it }) {
        if (bitmapImage == null) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Gray)
                    .fillMaxSize()
                    .clickable { expanded = !expanded }
            ) {
                PlaceSpiner()
            }
        } else {
            bitmapImage?.let {
                Box {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { expanded = !expanded },
                        contentScale = ContentScale.FillBounds,
                    )
                    if (lastUpdateDate.isNotEmpty() && !isPause) {
                        DisposableEffect(Unit) {
                            coroutineScope.launch {
                                if (lastUpdateTimer == null) {
                                    lastUpdateTimer = Timer()
                                    lastUpdateTimer?.let {
                                        startTimer(it, 100L, 1000L) {
                                            val lastUpd = lastUpdateImageStr(lastUpdateDate)
                                            lastUpdateString = lastUpd.ifEmpty { "0 C" }
                                        }
                                    }
                                }
                            }
                            onDispose {
                                onDisposeTimer(lastUpdateTimer) {
                                    lastUpdateTimer = null
                                }
                            }
                        }
                        Text(
                            text = lastUpdateString,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            textAlign = TextAlign.Right,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, blurRadius = 7f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        onDisposeTimer(lastUpdateTimer) {
                            lastUpdateTimer = null
                        }
                    }
                }
            }
        }
    }
    Timber.d("AA ItemCam ID::${data.id} | isPause:$isPause")
    if (!isPause) {
        DisposableEffect(key1 = Unit) {
            coroutineScope.launch {
                if (timer == null) {
                    timer = Timer()
                    timer?.let {
                        startTimer(it, 0, 6000) {
                            val screenShotUrl = data.screenShotPreview(DataModule.URL)
                            val cacheImage = downloadFileWithExif(
                                context = context,
                                url = screenShotUrl
                            )
                            lastUpdateDate = cacheImage?.date ?: ""
                            bitmapImage = cacheImage?.bitmap
                            bitmapImage?.let {
                                coroutineScope.launch {
                                    cache.put(data.id.toString(), it)
                                }
                            }
                        }
                    }
                }
            }

            onDispose {
                onDisposeTimer(timer, lastUpdateTimer) {
                    Timber.d("AA ItemCam onDispose() ID::${data.id} | isPause:$isPause")
                    timer = null
                    lastUpdateTimer = null
                }
            }
        }
    } else {
        onDisposeTimer(timer, lastUpdateTimer) {
            Timber.d("BBBBBB ItemCam if (isPause == $isPause) ItemCam onDispose() ID::${data.id} | isPause:$isPause")
            timer = null
            lastUpdateTimer = null
        }
    }
}


private fun startTimer(timer: Timer, delay: Long = 0, period: Long, onRun: () -> Unit) {
    timer.apply {
        schedule(object : TimerTask() {
            override fun run() {
                onRun()
            }
        }, delay, period)
    }
}

private fun onDisposeTimer(vararg timers: Timer?, onDispose: () -> Unit) {
    timers.forEach { timer ->
        timer?.apply {
            cancel()
            purge()
        }
    }
    onDispose()
}

private fun lastUpdateImageStr(lastUpdateDate: String): String {
    if (lastUpdateDate.isEmpty()) return ""
    var str: String = ""
    str = diffExifTime(lastUpdateDate)
    return str.trim()
}


private fun diffExifTime(to: String, from: String? = null, maxCountStr: Int = 1): String {
    val currentDate = Date()
    val dateTimeParts = to.split(" ", ":")

    val year = dateTimeParts[0].toInt()
    val month = dateTimeParts[1].toInt() - 1
    val day = dateTimeParts[2].toInt()
    val hour = dateTimeParts[3].toInt()
    val minute = dateTimeParts[4].toInt()
    val second = dateTimeParts[5].toInt()
    val dateTime = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), Locale("ru"))

    dateTime.set(year, month, day, hour, minute, second)
    val secondDiff = (currentDate.time / 1000) - (dateTime.timeInMillis / 1000)

    if (secondDiff < 0) {
        return "0 C"
    }
    if (secondDiff < 60) {
        return "$secondDiff C"
    }
    if (secondDiff < 3600) {
        return "${secondDiff / 60} М"
    }
    if (secondDiff < 86400) {
        return "${secondDiff / 3600} Ч"
    }
    return "${secondDiff / 86400} Д"

//    val timeUnit = TimeUnit.MILLISECONDS
//    val d = timeUnit.toDays(diffMills)
//    val h = timeUnit.toHours(diffMills) % 24
//    val m = timeUnit.toMinutes(diffMills) % 60
//    val s = timeUnit.toSeconds(diffMills) % 60
//
//    val arr = arrayOf(
//        DiffExifTime.DAY to d,
//        DiffExifTime.HOUR to h,
//        DiffExifTime.MINUTE to m,
//        DiffExifTime.SECONDS to s
//    )
//
//
//
//    var str: String = ""
//    arr.forEach { (type, time) ->
//        val splitListCount = str.trim().split(" ").count() / maxCountStr
//        if (time >= 365) {
//            str = ">1 Г"
//            return str
//        } else if (time in 1 until 365 && splitListCount <= maxCountStr) {
//            str += "$time ${type.char} "
//        } else return@forEach
//    }
//
//    return str.trim()
}

@Composable
private fun PlaceSpiner(modifier: Modifier? = null) {
    CircularProgressIndicator(
        modifier = modifier ?: Modifier.size(23.dp),
        color = MaterialTheme.colors.primary,
        strokeWidth = if (modifier == null) 2.dp else 4.dp
    )
}

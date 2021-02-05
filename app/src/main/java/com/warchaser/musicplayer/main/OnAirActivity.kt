package com.warchaser.musicplayer.main

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.SeekBar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.warchaser.musicplayer.R
import com.warchaser.musicplayer.display.DisplayActivity
import com.warchaser.musicplayer.global.BaseActivity
import com.warchaser.musicplayer.tools.*
import com.warchaser.musicplayer.tools.MediaControllerService.MyBinder
import com.warchaser.musicplayer.view.ConfirmDeleteDialog
import com.warchaser.musicplayer.view.OnAirListMenu
import kotlinx.android.synthetic.main.activity_main.*

/**
 * 主Activity
 * */
class OnAirActivity : BaseActivity(), View.OnClickListener{

    /**
     * Serve for SlideBar to locate the index of music.
     */
    private var mMusicListTmps : ArrayList<String>? = null

    /**
     * Service binder
     */
    private var mMyBinder : MyBinder? = null

    /**
     * Adapter
     * */
    private var mAdapter : SongsAdapter? = null

    private var mIsBound : Boolean = false

    private var mPath : String? = null

    private var mIsFromExternal : Boolean = false

    private var mMenuPopupWindow : OnAirListMenu? = null

    private var mConfirmDeleteDialog : ConfirmDeleteDialog? = null

    private var mLayoutManager : LinearLayoutManager? = null

    private var mObserverBuilder : UIObserverBuilder? = null

    private var mIsObserverEnable : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getList()
        playExternal()
        initComponent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            getIntent().data = data
            getIntent().putExtra("isFromExternal", true)
        }
        playExternal()
    }

    override fun onPause() {
        super.onPause()
        mIsObserverEnable = false
    }

    override fun onResume() {
        super.onResume()
        mMyBinder?.run {
            mBtnState.setBackgroundResource(if (getIsPlaying()) R.mipmap.pausedetail else R.mipmap.run)
        }

        mIsObserverEnable = true

        mMyBinder?.run {
            notifyActivity()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if(id == R.id.action_exit){
            destroyWholeApp()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyServiceBinder()
        mObserverBuilder?.let {
            CallObserver.instance.removeSingleObserver(it)
        }
    }

    private fun initComponent(){
        mAdapter = SongsAdapter(this)
        mSeekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMyBinder?.changeProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        //Initialize SeekBar's thumb
        val width = ImageUtil.dp2PX(this, resources.getDimension(R.dimen.seek_bar_thumb_width))
        val thumbDrawable = ImageUtil.getNewDrawable(this, R.mipmap.thumb, width)
        mSeekBarProgress.thumb = thumbDrawable

        if(MusicList.isListNotEmpty()){
            val bean = MusicList.getCurrentMusic()
            mTvBottomTitle.text = bean.title
            mTvBottomArtist.text = bean.artist
            refreshBottomThumb(bean.albumId)
        }

        mAdapter?.setOnItemClickDelegate(object : SongsAdapter.OnItemClickDelegate {
            override fun onItemClick(position: Int, bean: MusicInfo) {
                onSongsItemClick(position)
            }

            override fun onMenuClick(position: Int, bean: MusicInfo) {
                showSongListMenu(bean, position)
            }
        })

        mLayoutManager = LinearLayoutManager(this)
        mListViewSongs.layoutManager = mLayoutManager
        mListViewSongs.itemAnimator = null
        mListViewSongs.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        mAdapter?.setHasStableIds(true)
        mListViewSongs.adapter = mAdapter

        mMenuPopupWindow = OnAirListMenu(this)
        mMenuPopupWindow?.setOnMenuOptionsSelectedListener(object : OnAirListMenu.OnMenuOptionsSelectedListener{
            override fun onDeleteSelected(songTitle: String, position: Int, currentUri: String?, isDeleted: Boolean) {
                if (isDeleted) {
                    showConfirmDeleteDialog(songTitle, position, currentUri)
                } else {
                    CommonUtils.showShortToast(R.string.hint_file_can_not_be_deleted)
                }
            }
        })

        //SlideBar部分，tvFloatLetter+SlideBar，有渐变的动画效果
        val alp = AlphaAnimation(1f, 0f)
        alp.duration = 1500
        mTvFloatLetter.animation = alp
        alp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                mTvFloatLetter.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })

        mSlideBar.setOnLetterTouchChangeListener(object : SlideBar.OnLetterTouchListener {
            override fun onLetterTouchChange(isTouched: Boolean, s: String) {
                val bean = MusicInfo()
                bean.pinyinInitial = s
                mTvFloatLetter.text = s
                if (isTouched) {
                    mTvFloatLetter.visibility = View.VISIBLE
                } else {
                    mTvFloatLetter.startAnimation(alp)
                }

                val index = mMusicListTmps?.indexOf(bean.pinyinInitial)
                index?.run {
                    scrollRecyclerView2Position(this)
                }
            }
        })

        mObserverBuilder = registerUIObserver {
            notifySeekBar2Update {
                onNotifySeekBar2Update(it)
            }

            notify2Play {
                onNotify2Play(it)
            }

            stopServiceAndExit {
                destroyWholeApp()
            }

            setEnable {
                mIsObserverEnable
            }
        }.apply { CallObserver.instance.registerObserver(this) }

        mLyBtnState.setOnClickListener(this)
        mBtnState.setOnClickListener(this)
        mLyBtnNext.setOnClickListener(this)
        mBtnNext.setOnClickListener(this)
        mBottomBar.setOnClickListener(this)
    }

    /**
     * 初始化各个List
     * */
    private fun getList(){
        mMusicListTmps = ArrayList()
        val size = MusicList.size()
        for(i in 0 until size){
            mMusicListTmps!! += MusicList.getMusicWithPosition(i).pinyinInitial!!
        }
    }

    /**
     * 处理外存传过来的路径，以播放
     * */
    private fun playExternal(){
        var isFileFound = false
        val size = MusicList.size()
        //从splash得到传过来的绝对路径
        val uri = intent.data
        mIsFromExternal = intent.getBooleanExtra("isFromExternal", false)
        uri?.run {
            if("content" == uri.scheme){
                val filPathColumns : Array<String> = arrayOf(MediaStore.Images.Media.DATA)
                val cursor : Cursor?
                try {
                    cursor = contentResolver.query(uri, filPathColumns, null, null, null)
                    cursor?.run {
                        cursor.moveToFirst()
                        val columnIndex = cursor.getColumnIndex(filPathColumns[0])
                        mPath = cursor.getString(columnIndex)
                        close()
                    }
                } catch (e: Exception) {
                    NLog.printStackTrace(TAG, e)
                } catch (e: Error){
                    NLog.printStackTrace(TAG, e)
                }
            } else {
                mPath = uri.path
            }
        }

        mPath?.run {
            for(i in 0 until size){
                if(MusicList.getMusicWithPosition(i).url == this){
                    MusicList.setCurrentMusic(i)
                    isFileFound = true
                }
            }

            if(!isFileFound){
                getMetaData(this)
                updateDataBase(this)
                MusicList.setCurrentMusic(MusicList.getLastPosition())
            }
        }

        if(!getBinderState()){
            connect2MediaService()
        }

        mMyBinder?.run {
            mPath?.run {
                //这里在点击notification时，会有从新播放的bug
                startPlay(MusicList.getCurrentMusicInt(), 0)
            }

            if(getIsPlaying()){
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            } else {
                mBtnState.setBackgroundResource(R.mipmap.run)
            }
        }

        mPath = null
    }

    /**
     * 获取数据库中没有的文件的各种信息，并显示在列表的尾部
     * */
    private fun getMetaData(path: String){
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        //对于无法通过文件获取到title，album，artist的文件，可以用别的方法解决。
        val musicInfo = MusicInfo()
        musicInfo.title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) + ""
        musicInfo.album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) + ""
        musicInfo.artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) + ""
        musicInfo.duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt()
        musicInfo.url = path
        MusicList.add(musicInfo)
    }

    /**
     * 更新数据库
     * */
    private fun updateDataBase(path: String){
        MediaScannerConnection.scanFile(this, arrayOf(path), null, object : MediaScannerConnection.OnScanCompletedListener {
            override fun onScanCompleted(path: String?, uri: Uri?) {
                CommonUtils.showLongToast(R.string.database_sync_completed)
            }
        })
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.mLyBtnState, R.id.mBtnState -> play()
            R.id.mLyBtnNext, R.id.mBtnNext -> playNext()
            R.id.mBottomBar -> {
                if (MusicList.isListNotEmpty()) {
                    Intent(this, DisplayActivity::class.java).apply {
                        val bean = MusicList.getCurrentMusic()
                        putExtra("albumId", bean.albumId)
                        val sdkVersion = Build.VERSION.SDK_INT
                        if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
                            val p1 = Pair.create(mBottomBarDisc as View, "cover")
                            val p2 = Pair.create(mBtnState as View, "btn_state")
                            val p3 = Pair.create(mBtnNext as View, "btn_next")
                            val p4 = Pair.create(mSeekBarProgress as View, "seek_bar")
                            startActivity(this, ActivityOptions.makeSceneTransitionAnimation(this@OnAirActivity, p1, p2, p3, p4).toBundle())
                        } else {
                            startActivity(this)
                        }
                    }

                }
            }
        }
    }

    private fun connect2MediaService(){
        Intent(this, MediaControllerService::class.java).apply {
            mIsBound = applicationContext.bindService(this, mServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun play(){
        mMyBinder?.run {
            if(getIsPlaying()){
                stopPlay()
                mBtnState.setBackgroundResource(R.mipmap.run)
            } else {
                startPlay(MusicList.getCurrentMusicInt(), MusicList.getCurrentPosition())
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            }
        }
    }

    private fun playNext(){
        mMyBinder?.run {
            playNext()
            if(getIsPlaying()){
                mBtnState.setBackgroundResource(R.mipmap.pausedetail)
            }
        }
    }

    private fun refreshBottomThumb(albumId: Long){
        mBottomBarDisc.setImageBitmap(CoverLoader.instance.loadBottomThumb(albumId))
    }

    private fun getBinderState() : Boolean{
        return mMyBinder != null
    }

    private fun destroyServiceBinder(){
        mMyBinder?.run {
            if(mIsBound){
                applicationContext.unbindService(mServiceConnection)
                mMyBinder = null
                mIsBound = false
            }
        }
    }

    private fun showConfirmDeleteDialog(songTitle: String, selectedPosition: Int, currentUri: String?){
        if(mConfirmDeleteDialog == null){
            mConfirmDeleteDialog = ConfirmDeleteDialog(this)
            mConfirmDeleteDialog!!.setOnConfirmClickListener(object : ConfirmDeleteDialog.OnConfirmListener {
                override fun onConfirmClick(selectedPosition: Int, isDeleted: Boolean) {
                    if (isDeleted) {
                        MusicList.remove(selectedPosition)
                        if (selectedPosition == MusicList.getCurrentMusicInt()) {
                            mMyBinder?.playNextOnDelete()
                        }

                        mAdapter?.notifyDataSetChanged()
                    }

                    CommonUtils.showShortToast(if (isDeleted) R.string.delete_single_file_successfully else R.string.hint_file_can_not_be_deleted)
                }
            })
        }

        mConfirmDeleteDialog?.run {
            if(!isShowing){
                show(songTitle, currentUri, selectedPosition)
            }
        }
    }

    private fun dismissConfirmDeleteDialog(){
        mConfirmDeleteDialog?.run {
            if(isShowing){
                dismiss()
            }
        }
    }

    fun destroyWholeApp(){
        finish()
        destroyServiceBinder()
        stopService(Intent(this, MediaControllerService::class.java))
    }

    override fun clearMember() {
        super.clearMember()
        mMusicListTmps?.run {
            clear()
        }
        mMusicListTmps = null

        mAdapter = null
        mPath = null
        mObserverBuilder = null

        mMenuPopupWindow?.run {
            dismiss()
        }
        mMenuPopupWindow = null
        mLayoutManager = null

        dismissConfirmDeleteDialog()
        mConfirmDeleteDialog = null
    }

    private fun onSongsItemClick(position: Int){
        MusicList.setCurrentMusic(position)
        mMyBinder?.run {
            startPlay(MusicList.getCurrentMusicInt(), MusicList.FIRST_POSITION)
            mBtnState.setBackgroundResource(if (getIsPlaying()) R.mipmap.pausedetail else R.mipmap.run)
        }
    }

    private fun showSongListMenu(bean: MusicInfo?, position: Int){
        if(bean == null){
            return
        }

        mMenuPopupWindow?.run {
            show(bean.title!!, bean.url, position)
        }
    }

    private fun scrollRecyclerView2Position(position: Int){
        mLayoutManager?.run {
            scrollToPositionWithOffset(position, 0)
        }
    }

    private fun onNotifySeekBar2Update(intent: Intent?){
        intent?.apply {
            when(intent.action){
                MediaControllerService.ACTION_UPDATE_PROGRESS -> {
                    val progress = intent.getIntExtra(MediaControllerService.ACTION_UPDATE_PROGRESS, 0)
                    if (progress > 0) {
                        MusicList.setCurrentPosition(progress)
                        val seekBarProgress = MusicList.getCurrentPosition() / 1000
                        mSeekBarProgress.progress = seekBarProgress
                    }
                }
                MediaControllerService.ACTION_UPDATE_CURRENT_MUSIC -> {
                    val position = intent.getIntExtra(MediaControllerService.ACTION_UPDATE_CURRENT_MUSIC, 0)
                    mAdapter?.notifyItemsChanged(position)
                    if (MusicList.isListNotEmpty()) {
                        val bean = MusicList.getCurrentMusic()
                        refreshBottomThumb(bean.albumId)
                        mTvBottomTitle.text = FormatHelper.formatTitle(bean.title!!, 35)
                        mTvBottomArtist.text = bean.artist
                    }
                }
                MediaControllerService.ACTION_UPDATE_DURATION -> {
                    MusicList.setCurrentMusicMax(intent.getIntExtra(MediaControllerService.ACTION_UPDATE_DURATION, 0))
                    mSeekBarProgress.max = MusicList.getCurrentMusicMax() / 1000
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    mMyBinder?.run {
                        if (getIsPlaying()) {
                            stopPlay()
                            mBtnState.setBackgroundResource(R.mipmap.run)
                        }
                    }
                }
            }
        }
    }

    private fun onNotify2Play(repeatTime: Int){
        when(repeatTime){
            MediaControllerService.SINGLE_CLICK -> play()
            MediaControllerService.DOUBLE_CLICK -> playNext()
        }
    }

    /**
     * 绑定服务
     */
    private val mServiceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mMyBinder = service as MyBinder
            //判断外部（外存）传来的路径是否为空，不为空就在绑定成功之后立即播放
            mMyBinder?.run {
                mPath?.run {
                    if(mIsFromExternal){
                        startPlay(MusicList.getCurrentMusicInt(), 0)
                        mBtnState.setBackgroundResource(R.mipmap.pausedetail)
                    } else {
                        play()
                    }
                }

                notifyProgress()

                if(getIsPlaying()){
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail)
                } else {
                    mBtnState.setBackgroundResource(R.mipmap.run)
                }
            }

        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

}
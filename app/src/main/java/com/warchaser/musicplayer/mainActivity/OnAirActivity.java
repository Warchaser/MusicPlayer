package com.warchaser.musicplayer.mainActivity;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.displayActivity.DisplayActivity;
import com.warchaser.musicplayer.globalInfo.BaseActivity;
import com.warchaser.musicplayer.tools.CallObserver;
import com.warchaser.musicplayer.tools.CommonUtils;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.ImageUtil;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;
import com.warchaser.musicplayer.tools.MyService;
import com.warchaser.musicplayer.tools.MyService.MyBinder;
import com.warchaser.musicplayer.tools.UIObserver;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * MainActivity extends ActionBarActivity implements View.OnClickListener
 * Show the main-activity with title.
 * This activity contains ActionBar(Title), ListView, SlideBar(Me-Class) and LinerLayout.
 * This java-document contains inner-static classes.
 * */


public class OnAirActivity extends BaseActivity implements View.OnClickListener
{

    /**
     * Serve for SlideBar to locate the index of music.
     * */
    private List<String> mMusicListTmps;

    /**
     * Service binder
     * */
    public MyBinder mMyBinder;

    //Save the position of list on pause
    /**
     * View to convert.
     * */
    private View mView4SeekListView;

    /**
     * Index of current position of ListView.
     * */
    private int mIndex4SeekListView;

    /**
     * Top of the position of ListView.
     * */
    private int mTop4SeekListView;
    //Save the position of list on pause

    /**
     * Layout BottomBar
     * */
    @BindView(R.id.bottomBar)
    LinearLayout mLyBottomBar;

    /**
     * Layout Playing State
     * */
    @BindView(R.id.lyBtnState)
    LinearLayout mLyBtnState;

    /**
     * Layout Button Play Next
     * */
    @BindView(R.id.lyBtnNext)
    LinearLayout mLyBtnNext;

    private SongsAdapter mAdapter;

    /**
     * TextView, Music Title on BottomBar
     * */
    @BindView(R.id.bottomBarTvTitle)
    TextView mTvBottomTitle;

    /**
     * TextView, Music Artist on BottomBar
     * */
    @BindView(R.id.bottomBarTvArtist)
    TextView mTvBottomArtist;

    /**
     * ImageView, Music Cover on BottomBar
     * */
    @BindView(R.id.bottomBar_disc)
    ImageView mBottomBarDisc;

    /**
     * ListView, Songs List
     * */
    @BindView(R.id.listView)
    ListView mListViewSongs;

    /**
     * SeekBar, Playing Progress
     * */
    @BindView(R.id.progress)
    SeekBar mSeekBarProgress;

    /**
     * Button, Playing State
     * */
    @BindView(R.id.btnState)
    Button mBtnState;

    /**
     * Button, Play Next
     * */
    @BindView(R.id.btnNext)
    Button mBtnNext;

    /**
     * SlideBar, A IndexBar on Right;
     * Allow Users to Drag
     * */
    @BindView(R.id.slideBar)
    SlideBar mSlideBar;

    /**
     * TextView, A Floating Text, Which is Showing he Current Index.
     * */
    @BindView(R.id.tvFloatLetter)
    TextView mTvFloatLetter;

    private boolean mIsBind = false;

    private UIUpdateObserver mObserver;

    private String mPath = null;

    private boolean mIsFromExternal = false;

    private Unbinder mUnbinder;

    /**
     * 绑定服务
     * */
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            mMyBinder = (MyBinder) iBinder;
            //判断外部（外存）传来的路径是否为空，不为空就在绑定成功之后立即播放
            if(null != mPath)
            {
                if(mIsFromExternal){
                    mMyBinder.startPlay(MusicList.iCurrentMusic, 0);
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                } else {
                    play();
                }
            }

            mMyBinder.notifyProgress();

            if(mMyBinder.getIsPlaying())
            {
                mBtnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else
            {
                mBtnState.setBackgroundResource(R.mipmap.run);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {

        }
    };

    private void connectToMyService()
    {
        Intent intent = new Intent(this,MyService.class);
        mIsBind = this.getApplicationContext().bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUnbinder = ButterKnife.bind(this);

        getList();
        playExternal();
        initComponent();
    }

    //初始化各个List
    private void getList()
    {
        mMusicListTmps = new ArrayList<>();
        int musicInfoListSize = MusicList.musicInfoList.size();
        for(int i = 0;i < musicInfoListSize; i++)
        {
//            musicListTmps.add(musicInfoList.get(i).getPinyinInitial().toUpperCase());//用于除英文以外的版本
            mMusicListTmps.add(MusicList.musicInfoList.get(i).getPinyinInitial());//用于英文版本（英文名开头歌曲多的）
        }
    }

    //处理外存传过来的路径，以播放
    private void playExternal()
    {
        boolean isFileFound = false;
        int musicInfoListSize = MusicList.musicInfoList.size();
        //从splash得到传过来的绝对路径
        Uri uri = getIntent().getData();
        mIsFromExternal = getIntent().getBooleanExtra("isFromExternal", false);

        if(uri != null)
        {
            if("content".equals(uri.getScheme()))
            {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor;
                try
                {
                    cursor = getContentResolver().query(uri,
                            filePathColumn, null, null, null);
                    if(cursor != null)
                    {
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        mPath = cursor.getString(columnIndex);
                        cursor.close();
                    }
                }
                catch (Exception | Error e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                mPath = uri.getPath();
            }
        }

        //从外部传歌曲名
        if(mPath != null)
        {
            for(int i = 0;i < musicInfoListSize;i++)
            {
                if(MusicList.musicInfoList.get(i).getUrl().equals(mPath))
                {
                    MusicList.iCurrentMusic = i;
                    isFileFound = true;
                }
            }

            if(!isFileFound)
            {
                getMetaData(mPath);
                updateDataBase(mPath);
                MusicList.iCurrentMusic = MusicList.musicInfoList.size() - 1;
            }
        }

        //绑定服务
        if(!getBinderStatute())
        {
            connectToMyService();
        }
        else
        {
            if(mPath != null)
            {
                //这里在点击notification时，会有从新播放的bug
                mMyBinder.startPlay(MusicList.iCurrentMusic, 0);
            }

            if (mMyBinder.getIsPlaying())
            {
                mBtnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else
            {
                mBtnState.setBackgroundResource(R.mipmap.run);
            }
        }

        mPath = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().setData(intent.getData());
        getIntent().putExtra("isFromExternal", true);
        playExternal();
    }

    //更新数据库
    private void updateDataBase(String filename)
    {
        MediaScannerConnection.scanFile(this,
                new String[]{filename}, null,
                new MediaScannerConnection.OnScanCompletedListener()
                {
                    public void onScanCompleted(String path, Uri uri)
                    {
                        CommonUtils.showShortToast(R.string.database_sync_completed);
                    }
                });
    }


    //获取数据库中没有的文件的各种信息，并显示在列表的尾部
    private void getMetaData(String filePath)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath);
        //对于无法通过文件获取到title，album，artist的文件，可以用别的方法解决。
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) + "");
        musicInfo.setAlbum(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) + "");
        musicInfo.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) + "");
        musicInfo.setDuration(Integer.parseInt(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        musicInfo.setUrl(filePath);
        MusicList.musicInfoList.add(musicInfo);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(getBinderStatute() && mIsBind)
        {
            this.getApplicationContext().unbindService(mServiceConnection);
            mMyBinder = null;
        }

        if(mUnbinder != null)
        {
            mUnbinder.unbind();
        }

        mUnbinder = null;

        CallObserver.removeSingleObserver(mObserver);
        clearOnDestroy();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mIndex4SeekListView = mListViewSongs.getFirstVisiblePosition();
        mTop4SeekListView = (mView4SeekListView == null) ? 0 : mView4SeekListView.getTop();
        if(mObserver != null)
        {
            mObserver.setObserverEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(getBinderStatute()){
            if(mMyBinder.getIsPlaying())
            {
                mBtnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else
            {
                mBtnState.setBackgroundResource(R.mipmap.run);
            }
        }

        mListViewSongs.setSelectionFromTop(mIndex4SeekListView, mTop4SeekListView);
        if(mObserver != null)
        {
            mObserver.setObserverEnabled(true);
        }

        if(getBinderStatute())
        {
            mMyBinder.notifyActivity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_exit)
        {
            finish();
            if(getBinderStatute() && mIsBind)
            {
                this.getApplicationContext().unbindService(mServiceConnection);
                mMyBinder = null;
            }
            stopService(new Intent(this, MyService.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.bottomBar, R.id.lyBtnState, R.id.btnState, R.id.lyBtnNext, R.id.btnNext})
    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.lyBtnState:
            case R.id.btnState:
                play();
                break;

            case R.id.lyBtnNext:
            case R.id.btnNext:
                playNext();
                break;

            case R.id.bottomBar:
                Intent intent = new Intent(this,DisplayActivity.class);
                MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
                intent.putExtra("uri", bean.getUriWithCoverPic());
                int sdk = android.os.Build.VERSION.SDK_INT;
                if(sdk >= Build.VERSION_CODES.LOLLIPOP)
                {
                    Pair p1 = Pair.create(mBottomBarDisc,"cover");
                    Pair p2 = Pair.create(mBtnState, "btn_state");
                    Pair p3 = Pair.create(mBtnNext, "btn_next");
                    Pair p4 = Pair.create(mSeekBarProgress, "seek_bar");
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, p1,p2,p3,p4).toBundle());
                }
                else
                {
                    startActivity(intent);
                }

                break;
        }
    }

    private void playNext()
    {
        mMyBinder.playNext();
        if(mMyBinder.getIsPlaying())
        {
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void clearOnDestroy()
    {
        if(mMusicListTmps != null)
        {
            mMusicListTmps.clear();
            mMusicListTmps = null;
        }

        mView4SeekListView = null;

        mLyBottomBar = null;

        mLyBtnState = null;
        mLyBtnNext = null;

        mAdapter = null;

        mTvBottomTitle = null;
        mTvBottomArtist = null;
        mBottomBarDisc = null;
        mListViewSongs = null;
        mSeekBarProgress = null;
        mBtnState = null;
        mBtnNext = null;

        mPath = null;
        mSlideBar = null;
        mTvFloatLetter = null;
        mObserver = null;
    }

    private void play(){

        if(!getBinderStatute())
        {
            return;
        }

        if(mMyBinder.getIsPlaying())
        {
            mMyBinder.stopPlay();
            mBtnState.setBackgroundResource(R.mipmap.run);
        }
        else
        {
            mMyBinder.startPlay(MusicList.iCurrentMusic,MusicList.iCurrentPosition);
            mBtnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void initComponent()
    {
        mAdapter = new SongsAdapter(this);

        mSeekBarProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                if(b)
                {
                    mMyBinder.changeProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        if(MusicList.musicInfoList.size() != 0)
        {
            MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
            mTvBottomTitle.setText(bean.getTitle());
            mTvBottomArtist.setText(bean.getArtist());
            ImageUtil.setBottomBarDisc(this, bean.getUriWithCoverPic(), R.dimen.bottom_bar_disc_width_and_height, mBottomBarDisc, R.mipmap.disc, true);
        }

        mListViewSongs.setAdapter(mAdapter);
        mListViewSongs.setFocusable(true);

        mListViewSongs.setOnItemClickListener(new OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                MusicList.iCurrentMusic = i;
                mMyBinder.startPlay(MusicList.iCurrentMusic, 0);
                if (mMyBinder.getIsPlaying())
                {
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                else
                {
                    mBtnState.setBackgroundResource(R.mipmap.run);
                }

                mAdapter.notifyDataSetChanged();

            }
        });

        //SlideBar部分，tvFloatLetter+SlideBar，有渐变的动画效果
        final AlphaAnimation alp = new AlphaAnimation(1.0f,0.0f);
        alp.setDuration(1500);
        mTvFloatLetter.setAnimation(alp);

        alp.setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                mTvFloatLetter.setVisibility(View.GONE);
            }
        });

        mSlideBar.setOnLetterTouchChangeListener(new SlideBar.OnLetterTouchChangeListener()
        {
            @Override
            public void onLetterTouchChange(boolean isTouched, String s)
            {
                MusicInfo bean = new MusicInfo();
                bean.setPinyinInitial(s);
                mTvFloatLetter.setText(s);
                if (isTouched)
                {
                    mTvFloatLetter.setVisibility(View.VISIBLE);
                }
                else
                {
                    mTvFloatLetter.startAnimation(alp);
                }

                int index = mMusicListTmps.indexOf(bean.getPinyinInitial());
                mListViewSongs.setSelection(index);
            }
        });

        mView4SeekListView = mListViewSongs.getChildAt(0);

        mObserver = new UIUpdateObserver();
        CallObserver.setObserver(mObserver);
    }
    
    /**
     * return binder's statutes.
     * @retrun binder is not null.
     * */
    private boolean getBinderStatute(){
        return mMyBinder != null;
    }

    private class UIUpdateObserver implements UIObserver
    {
        private boolean mIsEnabled;

        @Override
        public void notifySeekBar2Update(Intent intent)
        {
            String sAction = intent.getAction();
            if(MyService.ACTION_UPDATE_PROGRESS.equals(sAction))
            {
                int iProgress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS,0);
                if(iProgress > 0)
                {
                    MusicList.iCurrentPosition = iProgress;
                    mSeekBarProgress.setProgress(MusicList.iCurrentPosition / 1000);
                }
            }
            else if(MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(sAction))
            {
                MusicList.iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC,0);
                if(MusicList.musicInfoList.size() != 0)
                {
                    MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
                    ImageUtil.setBottomBarDisc(OnAirActivity.this, bean.getUriWithCoverPic(), R.dimen.bottom_bar_disc_width_and_height, mBottomBarDisc, R.mipmap.disc, true);
                    mTvBottomTitle.setText(FormatHelper.formatTitle(bean.getTitle(), 35));
                    mTvBottomArtist.setText(bean.getArtist());
                }
                mListViewSongs.setSelection(MusicList.iCurrentMusic);
                mAdapter.notifyDataSetChanged();

            }
            else if(MyService.ACTION_UPDATE_DURATION.equals(sAction))
            {
                MusicList.iCurrentMax = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION,0);
                mSeekBarProgress.setMax(MusicList.iCurrentMax / 1000);
            }
            else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(sAction))
            {
                play();
            }
        }

        @Override
        public void notify2Play(int repeatTime)
        {
            switch (repeatTime)
            {
                case 1:
                    play();
                    break;
                case 2:
                    playNext();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void setObserverEnabled(boolean enabled)
        {
            this.mIsEnabled = enabled;
        }

        @Override
        public boolean getObserverEnabled()
        {
            return mIsEnabled;
        }
    }
}

package com.warchaser.musicplayer.mainActivity;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
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
import android.widget.Toast;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.displayActivity.DisplayActivity;
import com.warchaser.musicplayer.globalInfo.BaseActivity;
import com.warchaser.musicplayer.tools.CallObserver;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.ImageUtil;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;
import com.warchaser.musicplayer.tools.MyService;
import com.warchaser.musicplayer.tools.MyService.MyBinder;
import com.warchaser.musicplayer.tools.UIObserver;

import java.util.ArrayList;
import java.util.List;

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
    public MyBinder mMyBinder;////
    /************Service part**************/

    /**Save the position of list on pause**/
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
    /**Save the position of list on pause**/


    private LinearLayout mLyBottomBar;

    private LinearLayout mLyBtnState;
    private LinearLayout mLyBtnNext;

    private SongsAdapter mAdapter;

    private TextView mTvBottomTitle;
    private TextView mTvBottomArtist;
    private ImageView mBottomBarDisc;
    private ListView mListViewSongs;
    private SeekBar mSeekBarProgress;
    private Button mBtnState;///
    private Button mBtnNext;

    private String mPath = null;
    //SlideBar部分
    private SlideBar mSlideBar;
    private TextView mTvFloatLetter;
    //SlideBar部分

    private boolean mIsBind = false;

    private UIUpdateObserver mObserver;

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
                play();
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

        getList();
        playExternal();
        initComponent();

        //取得电话管理服务
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        if(telephonyManager != null){
            //注册监听对象，对电话的来电状态进行监听
            telephonyManager.listen(new TelListener(), PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    //初始化各个List
    private void getList(){

        MusicList.instance(getContentResolver());

        mMusicListTmps = new ArrayList<String>();
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

        if(uri != null)
        {
            if("content".equals(uri.getScheme()))
            {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = null;
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
        if(mMyBinder == null)
        {
            connectToMyService();
        }
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
                        Toast.makeText(OnAirActivity.this,"同步完成", Toast.LENGTH_SHORT).show();
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
        if(mMyBinder != null && mIsBind)
        {
            this.getApplicationContext().unbindService(mServiceConnection);
            mMyBinder = null;
        }

        CallObserver.removeSingleObserver(mObserver);
        clearOnDestroy();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        startActivity(intent);
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

        if(mMyBinder != null){
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

        if(mMyBinder != null)
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
            if(mMyBinder != null && mIsBind)
            {
                this.getApplicationContext().unbindService(mServiceConnection);
                mMyBinder = null;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()){
            case R.id.lyBtnState:
            case R.id.btnState:
                play();
                break;

            case R.id.lyBtnNext:
            case R.id.btnNext:
                mMyBinder.playNext();
                if(mMyBinder.getIsPlaying())
                {
                    mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.bottomBar:
                Intent intent = new Intent(OnAirActivity.this,DisplayActivity.class);
                MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
                intent.putExtra("uri", bean.getUriWithCoverPic());
                int sdk = android.os.Build.VERSION.SDK_INT;
                if(sdk >= Build.VERSION_CODES.LOLLIPOP){
                    Pair p1 = Pair.create(mBottomBarDisc,"cover");
                    Pair p2 = Pair.create(mBtnState, "btn_state");
                    Pair p3 = Pair.create(mBtnNext, "btn_next");
                    Pair p4 = Pair.create(mSeekBarProgress, "seek_bar");
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this, p1,p2,p3,p4).toBundle());
                } else {
                    startActivity(intent);
                }

                break;
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

        mSeekBarProgress = (SeekBar) findViewById(R.id.progress);
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

        mTvBottomTitle = (TextView) findViewById(R.id.bottomBarTvTitle);
        mTvBottomArtist = (TextView) findViewById(R.id.bottomBarTvArtist);
        mBottomBarDisc = (ImageView) findViewById(R.id.bottomBar_disc);

        if(MusicList.musicInfoList.size() != 0)
        {
            MusicInfo bean = MusicList.musicInfoList.get(MusicList.iCurrentMusic);
            mTvBottomTitle.setText(bean.getTitle());
            mTvBottomArtist.setText(bean.getArtist());
            ImageUtil.setBottomBarDisc(this, bean.getUriWithCoverPic(), R.dimen.bottom_bar_disc_width_and_height, mBottomBarDisc, R.mipmap.disc, true);
        }


        mBtnState = (Button) findViewById(R.id.btnState);
        mBtnState.setOnClickListener(this);

        mBtnNext = (Button) findViewById(R.id.btnNext);
        mBtnNext.setOnClickListener(this);

        mLyBottomBar = (LinearLayout) findViewById(R.id.bottomBar);
        mLyBottomBar.setOnClickListener(this);

        mLyBtnState = (LinearLayout) findViewById(R.id.lyBtnState);
        mLyBtnState.setOnClickListener(this);

        mLyBtnNext = (LinearLayout) findViewById(R.id.lyBtnNext);
        mLyBtnNext.setOnClickListener(this);

        mListViewSongs = (ListView) findViewById(R.id.listView);
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
        mTvFloatLetter = (TextView) findViewById(R.id.tvFloatLetter);
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

        mSlideBar = (SlideBar) findViewById(R.id.slideBar);
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
        public void notify2Play()
        {
            play();
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

    private final class TelListener extends PhoneStateListener
    {
        public void onCallStateChanged(int state, String incomingNumber)
        {
            super.onCallStateChanged(state, incomingNumber);
            //来电状态
            if(state == TelephonyManager.CALL_STATE_RINGING)
            {
                if(mMyBinder != null)
                {
                    if(mMyBinder.getIsPlaying())
                    {
                        mMyBinder.stopPlay();
                        mBtnState.setBackgroundResource(R.mipmap.run);
                    }
                }
            }
            else if(state == TelephonyManager.CALL_STATE_IDLE)
            {
                //挂断状态(即非来电状态)
                if(mMyBinder != null)
                {
                    if(!mMyBinder.getIsPlaying())
                    {
                        mMyBinder.startPlay(MusicList.iCurrentMusic,MusicList.iCurrentPosition);
                        mBtnState.setBackgroundResource(R.mipmap.pausedetail);
                    }
                }
            }
        }
    }



}

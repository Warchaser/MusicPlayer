package com.warchaser.musicplayer.mainActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ant.liao.GifView;
import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.displayActivity.DisplayActivity;
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


public class OnAirActivity extends ActionBarActivity implements View.OnClickListener{

    /**
     * List of information of music.
     * */
    public static List<MusicInfo> musicInfoList;

    /**
     * Serve for SlideBar to locate the index of music.
     * */
    private List<String> musicListTmps;

    /**
     * Service binder
     * */
    public MyBinder myBinder;////
    /************Service part**************/

    /*********Current music part***********/
    /**
     * The music which is playing.
     * */
    public static int iCurrentMusic;

    /**
     *The position of the music is playing.
     * */
    public static int iCurrentPosition;

    /**
     * The length of each music;
     * */
    private int iCurrentMax;
    /*********Current music part***********/

    /**Save the position of list on pause**/
    /**
     * View to convert.
     * */
    private View v;

    /**
     * Index of current position of ListView.
     * */
    private int index;

    /**
     * Top of the position of ListView.
     * */
    private int top;
    /**Save the position of list on pause**/


    private LinearLayout displayLayout;

    private LinearLayout lyBtnState;
    private LinearLayout lyBtnNext;

    final MyListViewAdapter adapter = new MyListViewAdapter();

    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView mBottomBarDisc;
    private ListView lvSongs;
    private SeekBar SeekProgress;
    private Button btnState;///
    private Button btnNext;

    private String path = null;
    private boolean FileFound = false;
    //SlideBar部分
    private SlideBar mSlideBar;
    private TextView tvFloatLetter;
    private MusicInfo beanTmps = null;
    //SlideBar部分

    private boolean mIsBind = false;

    private UIUpdateObserver mObserver;

    /**
     * 绑定服务
     * */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            myBinder = (MyBinder) iBinder;
            //判断外部（外存）传来的路径是否为空，不为空就在绑定成功之后立即播放
            if(null != path){
                play();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void connectToMyService(){
        Intent intent = new Intent(this,MyService.class);
        mIsBind = this.getApplicationContext().bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //show menu-key on the bottom virtual bar
//        getWindow().setFlags(0x08000000, 0x08000000);

        getList();
        playExternal();
        initComponent();

        //取得电话管理服务
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        //注册监听对象，对电话的来电状态进行监听
        telephonyManager.listen(new TelListener(), PhoneStateListener.LISTEN_CALL_STATE);
    }

    //初始化各个List
    private void getList(){

        MusicList musicList = MusicList.instance(getContentResolver());
        musicInfoList = musicList.getMusicList();

        musicListTmps = new ArrayList<String>();
        int musicInfoListSize = musicInfoList.size();
        for(int i = 0;i < musicInfoListSize;i++){
//            musicListTmps.add(musicInfoList.get(i).getPinyinInitial().toUpperCase());//用于除英文以外的版本
            musicListTmps.add(musicInfoList.get(i).getPinyinInitial());//用于英文版本（英文名开头歌曲多的）
        }

        beanTmps = new MusicInfo();
    }

    //处理外存传过来的路径，以播放
    private void playExternal(){
        int musicInfoListSize = musicInfoList.size();
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
                        path = cursor.getString(columnIndex);
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
                path = uri.getPath();
            }
        }

        //从外部传歌曲名
        if(path != null){
            for(int i = 0;i < musicInfoListSize;i++){
                if(musicInfoList.get(i).getUrl().equals(path)){
                    iCurrentMusic = i;
                    FileFound = true;
                }
            }
            if(!FileFound){
                getMetaData(path);
                updateDataBase(path);
                iCurrentMusic = musicInfoList.size() - 1;
            }
        }
        //绑定服务
        if(myBinder == null){
            connectToMyService();
        }
    }

    //更新数据库
    private void updateDataBase(String filename){
        MediaScannerConnection.scanFile(this,
                new String[]{filename}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Toast.makeText(OnAirActivity.this,"同步完成", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    //获取数据库中没有的文件的各种信息，并显示在列表的尾部
    private void getMetaData(String filePath) {
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
        musicInfoList.add(musicInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myBinder != null && mIsBind){
            this.getApplicationContext().unbindService(serviceConnection);
            myBinder = null;
        }

        CallObserver.removeSingleObserver(mObserver);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        startActivity(intent);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        index = lvSongs.getFirstVisiblePosition();
        top = (v == null) ? 0 : v.getTop();
        if(mObserver != null)
        {
            mObserver.setObserverEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(myBinder != null){
            if(myBinder.getIsPlaying()){
                btnState.setBackgroundResource(R.mipmap.pausedetail);
            }
            else {
                btnState.setBackgroundResource(R.mipmap.run);
            }
            myBinder.notifyActivity();
        }

        lvSongs.setSelectionFromTop(index,top);
        if(mObserver != null)
        {
            mObserver.setObserverEnabled(true);
        }

        if(myBinder != null)
        {
            myBinder.rebindObeserverOnResume();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        if(id == R.id.action_exit){
            finish();
            if(myBinder != null && mIsBind){
                this.getApplicationContext().unbindService(serviceConnection);
                myBinder = null;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.lyBtnState:
            case R.id.btnState:
                play();
                break;

            case R.id.lyBtnNext:
            case R.id.btnNext:
                myBinder.playNext();
                if(myBinder.getIsPlaying()){
                    btnState.setBackgroundResource(R.mipmap.pausedetail);
                }
                break;

            case R.id.bottomBar:
                Intent intent = new Intent(OnAirActivity.this,DisplayActivity.class);
                intent.putExtra(DisplayActivity.MUSIC_LENGTH, iCurrentMax);
                intent.putExtra(DisplayActivity.CURRENT_MUSIC, iCurrentMusic);
                intent.putExtra(DisplayActivity.CURRENT_POSITION, iCurrentPosition);
                startActivity(intent);
                break;
        }
    }

    private void play(){
        if(myBinder.getIsPlaying()){
            myBinder.stopPlay();
            btnState.setBackgroundResource(R.mipmap.run);
        }else{
            myBinder.startPlay(iCurrentMusic,iCurrentPosition);
            btnState.setBackgroundResource(R.mipmap.pausedetail);
        }
    }

    private void initComponent(){
        SeekProgress = (SeekBar) findViewById(R.id.progress);
        SeekProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    myBinder.changeProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        tvBottomTitle = (TextView) findViewById(R.id.bottomBarTvTitle);
        tvBottomArtist = (TextView) findViewById(R.id.bottomBarTvArtist);
        mBottomBarDisc = (ImageView) findViewById(R.id.bottomBar_disc);

        if(musicInfoList.size() != 0)
        {
            MusicInfo bean = musicInfoList.get(iCurrentMusic);
            tvBottomTitle.setText(bean.getTitle());
            tvBottomArtist.setText(bean.getArtist());
            setBottomBarDisc(OnAirActivity.this, bean.getUriWithCoverPic());
        }


        btnState = (Button) findViewById(R.id.btnState);
        btnState.setOnClickListener(this);

        btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        displayLayout = (LinearLayout) findViewById(R.id.bottomBar);
        displayLayout.setOnClickListener(this);

        lyBtnState = (LinearLayout) findViewById(R.id.lyBtnState);
        lyBtnState.setOnClickListener(this);

        lyBtnNext = (LinearLayout) findViewById(R.id.lyBtnNext);
        lyBtnNext.setOnClickListener(this);

        lvSongs = (ListView) findViewById(R.id.listView);
        lvSongs.setAdapter(adapter);
        lvSongs.setFocusable(true);

        lvSongs.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                iCurrentMusic = i;
                myBinder.startPlay(iCurrentMusic, 0);
                if (myBinder.getIsPlaying()) {
                    btnState.setBackgroundResource(R.mipmap.pausedetail);
                } else {
                    btnState.setBackgroundResource(R.mipmap.run);
                }

                adapter.notifyDataSetChanged();

            }
        });

        //SlideBar部分，tvFloatLetter+SlideBar，有渐变的动画效果
        tvFloatLetter = (TextView) findViewById(R.id.tvFloatLetter);
        final AlphaAnimation alp = new AlphaAnimation(1.0f,0.0f);
        alp.setDuration(1500);
        tvFloatLetter.setAnimation(alp);

        alp.setAnimationListener(new Animation.AnimationListener(){

            public void onAnimationStart(Animation animation){}
            public void onAnimationRepeat(Animation animation){}
            public void onAnimationEnd(Animation animation){

                tvFloatLetter.setVisibility(View.GONE);
            }
        });

        mSlideBar = (SlideBar) findViewById(R.id.slideBar);
        mSlideBar.setOnLetterTouchChangeListener(new SlideBar.OnLetterTouchChangeListener() {
            @Override
            public void onLetterTouchChange(boolean isTouched, String s) {

                beanTmps.setPinyinInitial(s);
                tvFloatLetter.setText(s);
                if (isTouched)
                    tvFloatLetter.setVisibility(View.VISIBLE);
                else
                    tvFloatLetter.startAnimation(alp);

                int index = musicListTmps.indexOf(beanTmps.getPinyinInitial());
                lvSongs.setSelection(index);
            }
        });

        v = lvSongs.getChildAt(0);

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
            if(MyService.ACTION_UPDATE_PROGRESS.equals(sAction)){
                int iProgress = intent.getIntExtra(MyService.ACTION_UPDATE_PROGRESS,0);
                if(iProgress > 0){
                    iCurrentPosition = iProgress;
                    SeekProgress.setProgress(iCurrentPosition / 1000);
                }
            }
            else
            if(MyService.ACTION_UPDATE_CURRENT_MUSIC.equals(sAction)){
                iCurrentMusic = intent.getIntExtra(MyService.ACTION_UPDATE_CURRENT_MUSIC,0);
                if(musicInfoList.size() != 0)
                {
                    MusicInfo bean = musicInfoList.get(iCurrentMusic);
                    setBottomBarDisc(OnAirActivity.this, bean.getUriWithCoverPic());
                    tvBottomTitle.setText(FormatHelper.formatTitle(bean.getTitle(), 35));
                    tvBottomArtist.setText(bean.getArtist());
                }
                lvSongs.setSelection(iCurrentMusic);
                adapter.notifyDataSetChanged();

            }
            else
            if(MyService.ACTION_UPDATE_DURATION.equals(sAction)){
                iCurrentMax = intent.getIntExtra(MyService.ACTION_UPDATE_DURATION,0);
                int iMax = iCurrentMax / 1000;
                SeekProgress.setMax(iMax);
            }

            else
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(sAction)) {
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

    private void setBottomBarDisc(Context context, String uri)
    {
        Drawable drawable;
        if(!TextUtils.isEmpty(uri))
        {
            drawable = ImageUtil.getCoverDrawableFromMusicFile(uri, OnAirActivity.this, getResources().getDimension(R.dimen.bottom_bar_disc_width_and_height));
            if(drawable == null)
            {
                drawable = ImageUtil.getDrawableFromRes(context, R.mipmap.disc);
            }
        }
        else
        {
            drawable = ImageUtil.getDrawableFromRes(context, R.mipmap.disc);
        }

        ImageUtil.setBackground(mBottomBarDisc, drawable);
    }

    private final class TelListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if(state == TelephonyManager.CALL_STATE_RINGING){//来电状态
                if(myBinder != null){
                    if(myBinder.getIsPlaying()){
                        myBinder.stopPlay();
                        btnState.setBackgroundResource(R.mipmap.run);
                    }
                }
            }
            else
            if(state == TelephonyManager.CALL_STATE_IDLE){//挂断状态(即非来电状态)
                if(myBinder != null){
                    if(!myBinder.getIsPlaying()){
                        myBinder.startPlay(iCurrentMusic,iCurrentPosition);
                        btnState.setBackgroundResource(R.mipmap.pausedetail);
                    }
                }
            }
        }
    }

    public class MyListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return musicInfoList.size();
        }

        @Override
        public Object getItem(int i) {
            return musicInfoList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return musicInfoList.get(i).getId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolderItem viewHolder;
            if(null == view){
                viewHolder = new ViewHolderItem();
                view = LayoutInflater.from(OnAirActivity.this).inflate(R.layout.item, null);
                viewHolder.tvItemTitle = (TextView) view.findViewById(R.id.tvItemTitle);
                viewHolder.tvItemDuration = (TextView) view.findViewById(R.id.tvItemDuration);
                viewHolder.gfGo = (GifView) view.findViewById(R.id.gfGo);
                viewHolder.gfGo.setGifImage(R.mipmap.ani);
                viewHolder.gfGo.setGifImageType(GifView.GifImageType.COVER);

                view.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolderItem) view.getTag();
            }

            viewHolder.tvItemTitle.setText(musicInfoList.get(i).getTitle());
            viewHolder.tvItemDuration.setText(FormatHelper.formatDuration(musicInfoList.get(i).getDuration()));

            viewHolder.tvItemTitle.setTextColor(Color.argb(255,0,0,0));
            viewHolder.tvItemDuration.setTextColor(Color.argb(255,0,0,0));

            viewHolder.gfGo.setVisibility(View.GONE);

            if(i == iCurrentMusic){
                viewHolder.tvItemTitle.setTextColor(Color.RED);
                viewHolder.tvItemDuration.setTextColor(Color.RED);
                viewHolder.gfGo.setVisibility(View.VISIBLE);
//                viewHolder.gfGo.showAnimation();
//                viewHolder.gfGo.showCover();
            }

            return view;
        }
    }

    public class ViewHolderItem{
        public TextView tvItemTitle;
        public TextView tvItemDuration;
        public GifView gfGo;
    }

}

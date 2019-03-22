package com.warchaser.musicplayer.mainActivity;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ant.liao.GifView;
import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Wucn on 2017/12/20.
 */

public class SongsAdapter extends BaseAdapter {

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    private OnItemClickDelegate mOnItemClickDelegate;

    SongsAdapter(Context context) {
        mContext = context;
        mOnItemClickListener = new OnItemClickListener();
    }

    @Override
    public int getCount() {
        return MusicList.musicInfoList.size();
    }

    @Override
    public Object getItem(int i) {
        return MusicList.musicInfoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return MusicList.musicInfoList.get(i).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolderItem viewHolder;
        if (null == view) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item, viewGroup, false);
            viewHolder = new ViewHolderItem(view);
            viewHolder.gfGo.setGifImage(R.mipmap.ani);
            viewHolder.gfGo.setGifImageType(GifView.GifImageType.COVER);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem) view.getTag();
        }

        viewHolder.tvItemTitle.setText(MusicList.musicInfoList.get(position).getTitle());
        viewHolder.tvItemDuration.setText(FormatHelper.formatDuration(MusicList.musicInfoList.get(position).getDuration()));

        viewHolder.tvItemTitle.setTextColor(Color.argb(255, 0, 0, 0));
        viewHolder.tvItemDuration.setTextColor(Color.argb(255, 0, 0, 0));

        viewHolder.gfGo.setVisibility(View.GONE);

        viewHolder.mLyRoot.setTag(position);
        viewHolder.mLyRoot.setOnClickListener(mOnItemClickListener);

        viewHolder.mBtnMenu.setTag(position);
        viewHolder.mBtnMenu.setOnClickListener(mOnItemClickListener);

        if (position == MusicList.iCurrentMusic) {
            viewHolder.tvItemTitle.setTextColor(Color.RED);
            viewHolder.tvItemDuration.setTextColor(Color.RED);
            viewHolder.gfGo.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public void setOnItemClickDelegate(OnItemClickDelegate delegate){
        this.mOnItemClickDelegate = delegate;
    }

    private class OnItemClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {

            if(mOnItemClickDelegate == null){
                return;
            }

            final int position = (int)v.getTag();
            final MusicInfo bean = MusicList.musicInfoList.get(position);
            switch (v.getId()){
                case R.id.mLyRoot:
                    mOnItemClickDelegate.onItemClick(position, bean);
                    break;
                case R.id.mBtnMenu:
                    mOnItemClickDelegate.onMenuClick(position, bean);
                    break;
                default:
                    break;
            }
        }
    }

    public interface OnItemClickDelegate{

        void onItemClick(int position, MusicInfo bean);

        void onMenuClick(int position, MusicInfo bean);
    }

    class ViewHolderItem {
        /**
         * TextView, Music Title
         */
        @BindView(R.id.tvItemTitle)
        TextView tvItemTitle;

        /**
         * TextView, Music Duration
         */
        @BindView(R.id.tvItemDuration)
        TextView tvItemDuration;

        /**
         * GifView, Tag a Gif on Current Music which is Playing
         */
        @BindView(R.id.gfGo)
        GifView gfGo;

        /**
         * 右侧菜单按钮
         * */
        @BindView(R.id.mBtnMenu)
        Button mBtnMenu;

        @BindView(R.id.mLyRoot)
        RelativeLayout mLyRoot;

        ViewHolderItem(View view) {
            ButterKnife.bind(this, view);
        }
    }
}



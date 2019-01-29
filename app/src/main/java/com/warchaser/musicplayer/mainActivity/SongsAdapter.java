package com.warchaser.musicplayer.mainActivity;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ant.liao.GifView;
import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.MusicList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Wucn on 2017/12/20.
 */

public class SongsAdapter extends BaseAdapter {

    private Context mContext;

    SongsAdapter(Context context) {
        mContext = context;
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
    public View getView(int i, View view, ViewGroup viewGroup) {
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

        viewHolder.tvItemTitle.setText(MusicList.musicInfoList.get(i).getTitle());
        viewHolder.tvItemDuration.setText(FormatHelper.formatDuration(MusicList.musicInfoList.get(i).getDuration()));

        viewHolder.tvItemTitle.setTextColor(Color.argb(255, 0, 0, 0));
        viewHolder.tvItemDuration.setTextColor(Color.argb(255, 0, 0, 0));

        viewHolder.gfGo.setVisibility(View.GONE);

        if (i == MusicList.iCurrentMusic) {
            viewHolder.tvItemTitle.setTextColor(Color.RED);
            viewHolder.tvItemDuration.setTextColor(Color.RED);
            viewHolder.gfGo.setVisibility(View.VISIBLE);
        }

        return view;
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

        ViewHolderItem(View view) {
            ButterKnife.bind(this, view);
        }
    }
}



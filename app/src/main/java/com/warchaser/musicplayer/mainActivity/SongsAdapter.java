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

/**
 * Created by Wucn on 2017/12/20.
 *
 */

public class SongsAdapter extends BaseAdapter
{

    private Context mContext;

    public SongsAdapter(Context context){
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
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        ViewHolderItem viewHolder;
        if(null == view)
        {
            viewHolder = new ViewHolderItem();
            view = LayoutInflater.from(mContext).inflate(R.layout.item, null);
            viewHolder.tvItemTitle = (TextView) view.findViewById(R.id.tvItemTitle);
            viewHolder.tvItemDuration = (TextView) view.findViewById(R.id.tvItemDuration);
            viewHolder.gfGo = (GifView) view.findViewById(R.id.gfGo);
            viewHolder.gfGo.setGifImage(R.mipmap.ani);
            viewHolder.gfGo.setGifImageType(GifView.GifImageType.COVER);

            view.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolderItem) view.getTag();
        }

        viewHolder.tvItemTitle.setText(MusicList.musicInfoList.get(i).getTitle());
        viewHolder.tvItemDuration.setText(FormatHelper.formatDuration(MusicList.musicInfoList.get(i).getDuration()));

        viewHolder.tvItemTitle.setTextColor(Color.argb(255,0,0,0));
        viewHolder.tvItemDuration.setTextColor(Color.argb(255,0,0,0));

        viewHolder.gfGo.setVisibility(View.GONE);

        if(i == MusicList.iCurrentMusic)
        {
            viewHolder.tvItemTitle.setTextColor(Color.RED);
            viewHolder.tvItemDuration.setTextColor(Color.RED);
            viewHolder.gfGo.setVisibility(View.VISIBLE);
//                viewHolder.gfGo.showAnimation();
//                viewHolder.gfGo.showCover();
        }

        return view;
    }

    private class ViewHolderItem
    {
        private TextView tvItemTitle;
        private TextView tvItemDuration;
        private GifView gfGo;
    }
}



package com.warchaser.musicplayer.mainActivity;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.tools.FormatHelper;
import com.warchaser.musicplayer.tools.MusicInfo;
import com.warchaser.musicplayer.tools.MusicList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Wucn on 2017/12/20.
 */

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolderItem> {

    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    private OnItemClickDelegate mOnItemClickDelegate;

    private int mCurrentPosition = MusicList.getCurrentMusicInt();

    SongsAdapter(Context context) {
        mContext = context;
        mOnItemClickListener = new OnItemClickListener();
    }

    @NonNull
    @Override
    public ViewHolderItem onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolderItem(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderItem viewHolderItem, int position) {

        MusicInfo bean = MusicList.getMusicWithPosition(position);

        Glide.with(mContext).asGif().load(R.mipmap.moving_music).into(viewHolderItem.gfGo);

        viewHolderItem.tvItemTitle.setText(bean.getTitle());
        viewHolderItem.tvItemDuration.setText(FormatHelper.formatDuration(bean.getDuration()));

        viewHolderItem.tvItemTitle.setTextColor(Color.argb(255, 0, 0, 0));
        viewHolderItem.tvItemDuration.setTextColor(Color.argb(255, 0, 0, 0));

        viewHolderItem.gfGo.setVisibility(View.INVISIBLE);

        viewHolderItem.mLyRoot.setTag(position);
        viewHolderItem.mLyRoot.setOnClickListener(mOnItemClickListener);

        viewHolderItem.mBtnMenu.setTag(position);
        viewHolderItem.mBtnMenu.setOnClickListener(mOnItemClickListener);

        if (position == MusicList.getCurrentMusicInt()) {
            viewHolderItem.tvItemTitle.setTextColor(Color.RED);
            viewHolderItem.tvItemDuration.setTextColor(Color.RED);
            viewHolderItem.gfGo.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return MusicList.size();
    }

    /**
     * 配合setHasStableIds(true)使用
     * */
    @Override
    public long getItemId(int position) {
        return position;
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
            final MusicInfo bean = MusicList.getMusicWithPosition(position);
            switch (v.getId()){
                case R.id.mLyRoot:
                    notifyItemChanged(mCurrentPosition);
                    notifyItemChanged(position);
                    mCurrentPosition = position;
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

    class ViewHolderItem extends RecyclerView.ViewHolder{
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
        ImageView gfGo;

        /**
         * 右侧菜单按钮
         * */
        @BindView(R.id.mBtnMenu)
        Button mBtnMenu;

        @BindView(R.id.mLyRoot)
        RelativeLayout mLyRoot;

        ViewHolderItem(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}



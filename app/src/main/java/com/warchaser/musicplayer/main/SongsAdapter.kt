package com.warchaser.musicplayer.main

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.warchaser.musicplayer.R
import com.warchaser.musicplayer.tools.FormatHelper
import com.warchaser.musicplayer.tools.MusicInfo
import com.warchaser.musicplayer.tools.MusicList
import kotlinx.android.synthetic.main.item.view.*

class SongsAdapter(context: Context) : RecyclerView.Adapter<SongsAdapter.ViewHolderItem>() {

    private val mContext : Context = context

    private var mOnItemClickListener : OnItemClickListener? = null

    private var mOnItemClickDelegate : OnItemClickDelegate? = null

    private var mCurrentPosition : Int = MusicList.getCurrentMusicInt()

    init {
        mOnItemClickListener = OnItemClickListener()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        return ViewHolderItem(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int, payloads: MutableList<Any>) {
        val isFullRefresh = payloads.isEmpty()

        val itemView = holder.itemView

        if(isFullRefresh){
            val bean = MusicList.getMusicWithPosition(position)
            Glide.with(mContext).asGif().load(R.mipmap.moving_music).into(itemView.mIvGo)

            itemView.mTvTitle.text = bean.title
            itemView.mTvDuration.text = FormatHelper.formatDuration(bean.duration)

            itemView.mTvTitle.setTextColor(Color.BLACK)
            itemView.mTvDuration.setTextColor(Color.BLACK)

            itemView.mIvGo.visibility = View.INVISIBLE

            itemView.mLyRoot.tag = position
            itemView.mLyRoot.setOnClickListener(mOnItemClickListener)

            itemView.mBtnMenu.tag = position
            itemView.mBtnMenu.setOnClickListener(mOnItemClickListener)

        }

        if(position == MusicList.getCurrentMusicInt()){
            itemView.mTvTitle.setTextColor(Color.RED)
            itemView.mTvDuration.setTextColor(Color.RED)
            itemView.mIvGo.visibility = View.VISIBLE
        } else {
            itemView.mTvTitle.setTextColor(Color.BLACK)
            itemView.mTvDuration.setTextColor(Color.BLACK)
            itemView.mIvGo.visibility = View.INVISIBLE
        }
    }

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
    }

    override fun getItemCount(): Int {
        return MusicList.size()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setOnItemClickDelegate(delegate: OnItemClickDelegate){
        mOnItemClickDelegate = delegate
    }

    fun notifyItemsChanged(position: Int){
        notifyItemChanged(mCurrentPosition, "local_refresh")
        notifyItemChanged(position, "local_refresh");
        mCurrentPosition = position
    }

    private inner class OnItemClickListener : View.OnClickListener{
        override fun onClick(v: View?) {

            mOnItemClickDelegate?.run {
                val position = v?.tag as Int
                val bean = MusicList.getMusicWithPosition(position)
                when(v.id){
                    R.id.mLyRoot -> {
                        notifyItemsChanged(position)
                        onItemClick(position, bean)
                    }
                    R.id.mBtnMenu -> {
                        onMenuClick(position, bean)
                    }
                }
            }
        }
    }

    interface OnItemClickDelegate{
        fun onItemClick(position: Int, bean : MusicInfo)

        fun onMenuClick(position: Int, bean : MusicInfo)
    }

    class ViewHolderItem(view : View) : RecyclerView.ViewHolder(view)
}
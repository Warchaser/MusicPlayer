package com.warchaser.musicplayer.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.warchaser.musicplayer.R
import kotlinx.android.synthetic.main.popup_on_air_menu.*
import java.io.File

class OnAirListMenu(context: Context) : AlertDialog(context){

    private val mContext : Context = context

    private var mCurrentUri : String? = ""
    private var mSelectedMusicPosition : Int = -1

    private var mOnMenuOptionsSelectedListener : OnMenuOptionsSelectedListener? = null

    init {
        show()
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize()
    }

    private fun initialize(){
        val view = LayoutInflater.from(mContext).inflate(R.layout.popup_on_air_menu, null)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        setContentView(view)

        val heightTitleLayout = mLyTitle.measuredHeight
        val heightOptionsLayout = mLyOptions.measuredHeight

        val params = window?.attributes
        params?.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = heightTitleLayout + heightOptionsLayout

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.attributes = params
        window?.setWindowAnimations(R.style.popup_window_style)
        setCanceledOnTouchOutside(true)

        mBtnClose.setOnClickListener { dismiss() }

        mTvOptionDelete.setOnClickListener {
            if (isParamsAvailable()) {
                mOnMenuOptionsSelectedListener?.run {
                    onDeleteSelected(
                            mTvSongTitle.text.toString(),
                            mSelectedMusicPosition,
                            mCurrentUri,
                            File(mCurrentUri).exists()
                    )
                }
                resetParams()
            }

            dismiss()
        }
    }

    private fun isParamsAvailable() : Boolean{
        return !TextUtils.isEmpty(mCurrentUri) && mSelectedMusicPosition > -1
    }

    private fun resetParams(){
        mCurrentUri = ""
        mSelectedMusicPosition = -1
    }

    fun show(songTitle: String, uri: String?, selectedMusicPosition: Int){
        mTvSongTitle.text = songTitle
        mCurrentUri = uri
        mSelectedMusicPosition = selectedMusicPosition
        show()
    }

    fun setOnMenuOptionsSelectedListener(listener: OnMenuOptionsSelectedListener){
        mOnMenuOptionsSelectedListener = listener
    }

    interface OnMenuOptionsSelectedListener{
        fun onDeleteSelected(songTitle: String, position: Int, currentUri: String?, isDeleted: Boolean)
    }
}
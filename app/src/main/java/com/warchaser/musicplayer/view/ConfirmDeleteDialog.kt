package com.warchaser.musicplayer.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.warchaser.musicplayer.R
import com.warchaser.musicplayer.tools.MusicList.Companion.deleteSingleMusicFile
import kotlinx.android.synthetic.main.dialog_confirm.*

class ConfirmDeleteDialog(context: Context) : AlertDialog(context) {

    private val mContext : Context = context

    private var mCurrentUri : String? = ""
    private var mSelectionMusicPosition : Int = -1

    private var mOnConfirmListener : OnConfirmListener? = null

    init {
        show()
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize()
    }

    private fun initialize(){
        setContentView(LayoutInflater.from(mContext).inflate(R.layout.dialog_confirm, null))

        val params = window?.attributes
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.attributes = params

        mBtnCancel.setOnClickListener { dismiss() }

        mBtnConfirm.setOnClickListener {
            mOnConfirmListener?.run {
                onConfirmClick(mSelectionMusicPosition, deleteSingleMusicFile(mCurrentUri))
            }

            resetParams()
            dismiss()
        }
    }

    fun show(songTitle : String, uri : String?, selectionMusicPosition : Int){
        mTvMessage.text = String.format(mContext.resources.getString(R.string.format_dialog_message), songTitle)
        mCurrentUri = uri
        mSelectionMusicPosition = selectionMusicPosition
        show()
    }

    private fun resetParams(){
        mCurrentUri = ""
        mSelectionMusicPosition = -1
    }

    fun setOnConfirmClickListener(listener : OnConfirmListener){
        mOnConfirmListener = listener
    }

    interface  OnConfirmListener{
        fun onConfirmClick(selectedPosition : Int, isDeleted : Boolean)
    }

}
package com.warchaser.musicplayer.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.tools.MusicList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class ConfirmDeleteDialog extends AlertDialog {

    private Context mContext;

    private String mCurrentUri;

    private int mSelectedMusicPosition = -1;

    private TextView mTvMessage;
    private OnConfirmListener mOnConfirmListener;

    public ConfirmDeleteDialog(@NonNull Context context) {
        super(context);
        this.mContext = context;

        show();
        dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize();
    }

    private void initialize(){
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_confirm, null);

        setContentView(view);

        mTvMessage = findViewById(R.id.mTvMessage);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setAttributes(params);

        view.findViewById(R.id.mBtnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.mBtnConfirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mOnConfirmListener != null){
                    mOnConfirmListener.onConfirmClick(mSelectedMusicPosition, MusicList.deleteSingleMusicFile(mCurrentUri));
                }

                resetParams();

                dismiss();
            }
        });
    }

    private void resetParams(){
        mCurrentUri = "";
        mSelectedMusicPosition = -1;
    }

    public void show(String songTitle, String uri, int selectedMusicPosition){
        mTvMessage.setText(String.format(mContext.getResources().getString(R.string.format_dialog_message), songTitle));
        mCurrentUri = uri;
        mSelectedMusicPosition = selectedMusicPosition;
        show();
    }

    public void setOnConfirmClickListener(OnConfirmListener listener){
        mOnConfirmListener = listener;
    }

    public interface OnConfirmListener{
        void onConfirmClick(int selectedPosition, boolean isDeleted);
    }
}

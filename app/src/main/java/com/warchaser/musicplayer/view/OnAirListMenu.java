package com.warchaser.musicplayer.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.warchaser.musicplayer.R;
import com.warchaser.musicplayer.tools.MusicList;

public class OnAirListMenu extends AlertDialog {

    private Context mContext;

    private TextView mTvSongTitle;

    private String mCurrentUri;

    private int mSelectedMusicPosition = -1;

    public OnAirListMenu(Context context){
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

        View view = LayoutInflater.from(mContext).inflate(R.layout.popup_on_air_menu, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        setContentView(view);

        mTvSongTitle = findViewById(R.id.mTvSongTitle);

        int heightTitleLayout = view.findViewById(R.id.mLyTitle).getMeasuredHeight();
        int heightOptionsLayout = view.findViewById(R.id.mLyOptions).getMeasuredHeight();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = heightTitleLayout + heightOptionsLayout;

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setAttributes(params);
        getWindow().setWindowAnimations(R.style.popup_window_style);
        setCanceledOnTouchOutside(true);

        view.findViewById(R.id.mBtnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.mTvOptionDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isParamsAvailable()){
                    MusicList.deleteSingleMusicFile(mCurrentUri);
                    resetParams();
                }

                dismiss();
            }
        });
    }

    private boolean isParamsAvailable(){
        return !TextUtils.isEmpty(mCurrentUri) && mSelectedMusicPosition > -1;
    }

    private void resetParams(){
        mCurrentUri = "";
        mSelectedMusicPosition = -1;
    }

    public void show(String songTitle, String uri, int selectedMusicPosition){
        mTvSongTitle.setText(songTitle);
        mCurrentUri = uri;
        mSelectedMusicPosition = selectedMusicPosition;
        show();
    }

    @Override
    public void dismiss() {
        super.dismiss();

    }
}

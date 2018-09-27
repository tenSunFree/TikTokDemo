package com.home.tiktokdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.home.tiktokdemo.widget.MarqueeTextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by xiongxingxing on 16/12/3.
 */

public class RoomFragment extends Fragment implements View.OnClickListener {

    private TextView authorNameTextView, authorContentTextView;
    private MarqueeTextView songNameMTextView;
    private FrameLayout rotateFrameLayout;
    private RelativeLayout room_view;
    private ImageView playImageView;
    private OnVideoControlListener onVideoControlListener;
    private CircleImageView avatarCircleImageView, avatarCircleImageView2;

    public static RoomFragment newInstance() {
        Bundle args = new Bundle();
        RoomFragment fragment = new RoomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        initializationView(view);
    }

    private void initializationView(View view) {
        room_view = view.findViewById(R.id.room_view);
        room_view.setOnClickListener(this);
        playImageView = view.findViewById(R.id.playImageView);
        playImageView.setAlpha(70);
        playImageView.setVisibility(View.INVISIBLE);
        playImageView.setOnClickListener(this);
        rotateFrameLayout = view.findViewById(R.id.rotateFrameLayout);
        avatarCircleImageView = view.findViewById(R.id.avatarCircleImageView);
        authorNameTextView = view.findViewById(R.id.authorNameTextView);
        authorContentTextView = view.findViewById(R.id.authorContentTextView);
        songNameMTextView = view.findViewById(R.id.songNameMTextView);
        avatarCircleImageView2 = view.findViewById(R.id.avatarCircleImageView2);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.room_view:
                onVideoControlListener.onPause(playImageView);
                break;
            case R.id.playImageView:
                onVideoControlListener.onStart(playImageView);
                break;
        }
    }

    public interface OnVideoControlListener {
        void onStart(ImageView imageView);

        void onPause(ImageView imageView);
    }

    public void setOnItemClickListener(OnVideoControlListener onVideoControlListener) {
        this.onVideoControlListener = onVideoControlListener;
    }

    /**
     * 隱藏播放按鈕
     */
    public void invisibleImageView() {
        if (playImageView != null) {
            playImageView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 將圖片呈現轉360度的動畫效果
     */
    public void rotateView() {
        if (rotateFrameLayout != null) {
            rotateFrameLayout.setRotation(0);
            rotateFrameLayout.animate()
                    .rotationBy(36000)
                    .setDuration(100000)
                    .start();
        }
    }

    public void setAuthorName(String string) {
        authorNameTextView.setText(string);
    }

    public void setAuthorContent(String string) {
        authorContentTextView.setText(string);
    }

    public void setSongName(String string) {
        songNameMTextView.setText(string);
    }

    public void setAuthorAvatar(int id) {
        avatarCircleImageView.setImageResource(id);
    }

    public void setAuthorAvatar2(int id) {
        avatarCircleImageView2.setImageResource(id);
    }
}


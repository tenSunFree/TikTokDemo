package com.home.tiktokdemo;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.home.tiktokdemo.utils.Utils;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.pili.pldroid.player.widget.PLVideoView;

import java.util.ArrayList;
import java.util.HashMap;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class MainActivity extends AppCompatActivity
        implements PLMediaPlayer.OnPreparedListener, PLMediaPlayer.OnCompletionListener,
        View.OnClickListener {

    private static final int MESSAGE_ID_RECONNECTING = 0x01;
    private boolean mIsActivityPaused = true;
    private PLVideoTextureView mVideoView;
    private Toast mToast = null;
    private String mVideoPath = null;
    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what != MESSAGE_ID_RECONNECTING) {
                return;
            }
            if (mIsActivityPaused || !Utils.isLiveStreamingAvailable()) {
                finish();
                return;
            }
            if (!Utils.isNetworkAvailable(MainActivity.this)) {
                sendReconnectMessage();
                return;
            }
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.start();
        }
    };
    private VerticalViewPager mViewPager;
    private RelativeLayout mRoomContainer;
    private PagerAdapter mPagerAdapter;
    private int mCurrentItem;
    private int isLiveStreaming = 1;
    private AVOptions options;
    private FrameLayout mFragmentContainer;
    private ArrayList<String> mVideoUrls = new ArrayList<>();
    private Subscription mSubscription = Subscriptions.empty();
    private FragmentManager mFragmentManager;
    private int mRoomId = -1;
    private RoomFragment mRoomFragment;
    private boolean mInit = false;
    private boolean isFirst = true;
    private MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    private ImageView searchImageView;
    private View separationLineView1, separationLineView2, separationLineView3, separationLineView4;
    private String[] authorName, authorContent, songName;
    private int[] authorAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreenDisplayAndStatusBarIsNotHidden();
        setContentView(R.layout.activity_room);

        initializationData();
        initializationView();
        initAVOptions();
        generateUrls();
        mFragmentManager = getSupportFragmentManager();
        mPagerAdapter = new PagerAdapter();
        initializationPLVideoTextureView();
        initializationVerticalViewPager();
        initializationRoomFragment();
        searchImageView.setOnClickListener(this);
    }

    private void initializationData() {
        authorName = new String[]{
                "\u0040小海鷗", "\u0040小貓頭鷹", "\u0040大兔子",};
        authorContent = new String[]{
                "#深海 #飛翔", "#洞穴 #肥兔", "#草原 #蝴蝶",};
        songName = new String[]{
                "原聲 - 小海鷗 - 小海鷗    原聲 - 小海鷗 - 小海鷗    原聲 - 小海鷗 - 小海鷗",
                "原聲 - 小貓頭鷹 - 小貓頭鷹    原聲 - 小貓頭鷹 - 小貓頭鷹    原聲 - 小貓頭鷹 - 小貓頭鷹",
                "原聲 - 大兔子 - 大兔子    原聲 - 大兔子 - 大兔子    原聲 - 大兔子 - 大兔子",};
        authorAvatar = new int[]{R.drawable.icon_right_avatar1, R.drawable.icon_right_avatar2, R.drawable.icon_right_avatar3};
    }

    private void initializationRoomFragment() {
        mRoomFragment = RoomFragment.newInstance();
        mRoomFragment.setOnItemClickListener(new RoomFragment.OnVideoControlListener() {

            /** 開始播放, 並隱藏按鈕 */
            @Override
            public void onStart(ImageView imageView) {
                mVideoView.start();
                imageView.setVisibility(View.INVISIBLE);
            }

            /** 開始播放與暫停播放, 並顯示與隱藏按鈕 */
            @Override
            public void onPause(ImageView imageView) {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    mVideoView.start();
                    imageView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void initializationVerticalViewPager() {

        /** 上下翻頁時, 同步更新mCurrentItem */
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mCurrentItem = position;
            }
        });

        mViewPager.setPageTransformer(false, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {
                ViewGroup viewGroup = (ViewGroup) page;

                if ((position < 0 && viewGroup.getId() != mCurrentItem)) {
                    View roomContainer = viewGroup.findViewById(R.id.room_container);
                    if (roomContainer != null && roomContainer.getParent() != null && roomContainer.getParent() instanceof ViewGroup) {
                        ((ViewGroup) (roomContainer.getParent())).removeView(roomContainer);
                    }
                }

                /** 满足此种条件, 表明需要加载直播视频以及聊天室 */
                if (viewGroup.getId() == mCurrentItem && position == 0 && mCurrentItem != mRoomId) {
                    changeSeparationLineColor();
                    if (mRoomContainer.getParent() != null && mRoomContainer.getParent() instanceof ViewGroup) {
                        ((ViewGroup) (mRoomContainer.getParent())).removeView(mRoomContainer);
                    }
                    loadVideoAndChatRoom(viewGroup, mCurrentItem);
                }
            }
        });

        mViewPager.setAdapter(mPagerAdapter);
    }

    private void initializationPLVideoTextureView() {
        mVideoView.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_PAVED_PARENT);                // 設定視訊預覽模式
        mVideoView.setAVOptions(options);
    }

    /**
     * Android全屏显示时, 状态栏显示在最顶层, 不隐藏
     */
    private void fullScreenDisplayAndStatusBarIsNotHidden() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void initializationView() {
        mViewPager = (VerticalViewPager) findViewById(R.id.view_pager);
        mRoomContainer = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.view_room_container, null);
        mFragmentContainer = (FrameLayout) mRoomContainer.findViewById(R.id.fragment_container);
        mVideoView = (PLVideoTextureView) mRoomContainer.findViewById(R.id.texture_view);
        separationLineView1 = findViewById(R.id.separationLineView1);
        separationLineView2 = findViewById(R.id.separationLineView2);
        separationLineView3 = findViewById(R.id.separationLineView3);
        separationLineView4 = findViewById(R.id.separationLineView4);
        searchImageView = (ImageView) findViewById(R.id.searchImageView);
    }

    /**
     * 初始化播放片源網址
     */
    private void generateUrls() {
        mVideoUrls.add("http://vjs.zencdn.net/v/oceans.mp4");
        mVideoUrls.add("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        mVideoUrls.add("http://www.w3school.com.cn/example/html5/mov_bbb.mp4");
    }

    private void initAVOptions() {
        options = new AVOptions();

        /** the unit of timeout is ms */
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);

        /** Some optimization with buffering mechanism when be set to 1 */
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, isLiveStreaming);
        options.setInteger(AVOptions.KEY_DELAY_OPTIMIZATION, 1);

        /** 1 -> hw codec enable, 0 -> disable [recommended] */
        int codec = 0;
        options.setInteger(AVOptions.KEY_MEDIACODEC, codec);

        /**whether start play automatically after prepared, default value is 1 */
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);
    }

    /**
     * @param viewGroup
     * @param currentItem
     */
    private void loadVideoAndChatRoom(ViewGroup viewGroup, int currentItem) {

        /** 聊天室的fragment只加载一次, 以后复用 */
        if (!mInit) {
            mFragmentManager.beginTransaction().add(mFragmentContainer.getId(), mRoomFragment).commitAllowingStateLoss();
            mInit = true;
        }

        loadVideo(currentItem);
        viewGroup.addView(mRoomContainer);
        mRoomId = currentItem;

        /** 切換影片時, 如果有顯示播放按鈕 就把它隱藏 */
        if (isFirst == true) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRoomFragment.invisibleImageView();
                    isFirst = false;
                }
            }, 50);
        } else {
            mRoomFragment.invisibleImageView();
        }
    }

    private void loadVideo(int position) {
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(this);
        mVideoView.stopPlayback();
        mVideoView.setVideoPath(mVideoUrls.get(position));
        mVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mToast = null;
        mVideoView.pause();
        mIsActivityPaused = true;
        mRoomFragment.invisibleImageView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        mVideoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
        mSubscription.unsubscribe();
    }

    private void showToastTips(final String tips) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(MainActivity.this, tips, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    private void sendReconnectMessage() {
        showToastTips("正在重连...");
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_ID_RECONNECTING), 500);
    }

    /**
     * 當影片播放完, 自動重新播放
     */
    @Override
    public void onCompletion(PLMediaPlayer plMediaPlayer) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadVideo(mCurrentItem);
            }
        }, 100);
    }

    /**
     * 當影片加載完成時, 刷新旋轉的動畫
     */
    @Override
    public void onPrepared(PLMediaPlayer plMediaPlayer) {
        mRoomFragment.rotateView();
        mRoomFragment.setAuthorAvatar(authorAvatar[mCurrentItem]);
        mRoomFragment.setAuthorAvatar2(authorAvatar[mCurrentItem]);
        mRoomFragment.setAuthorName(authorName[mCurrentItem]);
        mRoomFragment.setAuthorContent(authorContent[mCurrentItem]);
        mRoomFragment.setSongName(songName[mCurrentItem]);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchImageView:
                Toast.makeText(this, "Click searchImageView", Toast.LENGTH_SHORT).show();
                changeSeparationLineColor();
                break;
        }
    }

    class PagerAdapter extends android.support.v4.view.PagerAdapter {

        @Override
        public int getCount() {
            return mVideoUrls.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.view_room_item, null);
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(mVideoUrls.get(position), new HashMap());
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime((long) (1000), MediaMetadataRetriever.OPTION_PREVIOUS_SYNC);
            ImageView previewImageView = view.findViewById(R.id.previewImageView);
            previewImageView.setImageBitmap(bitmap);
            view.setId(position);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(container.findViewById(position));
        }
    }

    /**
     * 分隔線的顏色動畫
     */
    public void changeSeparationLineColor() {
        separationLineView1.setBackgroundColor(getColor(R.color.colorSLineChange));
        separationLineView2.setBackgroundColor(getColor(R.color.colorSLineChange));
        new Handler().postDelayed(new Runnable() {
            public void run() {
                separationLineView3.setBackgroundColor(getColor(R.color.colorSLineChange));
                separationLineView4.setBackgroundColor(getColor(R.color.colorSLineChange));
            }
        }, 100);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                separationLineView1.setBackgroundColor(getColor(R.color.colorSLineDeneral));
                separationLineView2.setBackgroundColor(getColor(R.color.colorSLineDeneral));
                separationLineView3.setBackgroundColor(getColor(R.color.colorSLineDeneral));
                separationLineView4.setBackgroundColor(getColor(R.color.colorSLineDeneral));
            }
        }, 200);
    }
}
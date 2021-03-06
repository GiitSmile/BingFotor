package com.wallpaper.bingfotor.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.flaviofaria.kenburnsview.RandomTransitionGenerator;
import com.victor.loading.rotate.RotateLoading;
import com.wallpaper.bingfotor.BingFotorApplication;
import com.wallpaper.bingfotor.R;
import com.wallpaper.bingfotor.model.entity.Bean;
import com.wallpaper.bingfotor.presenter.IBingPresenter;
import com.wallpaper.bingfotor.presenter.impl.IBingPresenterImpl;
import com.wallpaper.bingfotor.service.NetworkStateService;
import com.wallpaper.bingfotor.utils.DateUtils;
import com.wallpaper.bingfotor.utils.GlideUtils;
import com.wallpaper.bingfotor.utils.NetWorkUtils;
import com.wallpaper.bingfotor.utils.ScreenUtils;
import com.wallpaper.bingfotor.view.IBingView;
import com.wallpaper.bingfotor.view.PromptDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, IBingView, View.OnLongClickListener {
    private Typeface TEXT_TYPE;
    @BindView(R.id.bing_bg)
    KenBurnsView bing_bg;
    @BindView(R.id.day)
    TextView day;
    @BindView(R.id.month)
    TextView month;
    @BindView(R.id.week)
    TextView week;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.copyright)
    TextView copyright;
    @BindView(R.id.rotateloading)
    RotateLoading rotateLoading;
    private Context context;
    Receiver receiver;

    List<String> IMAGES;
    RandomTransitionGenerator generator;
    private boolean isPause = false;
    private IBingPresenter bingPresenter;

    public static Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Toast.makeText(BingFotorApplication.getInstance(), "开始下载", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(BingFotorApplication.getInstance(), "下载完成", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(BingFotorApplication.getInstance(), "网络出错", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initWidget();
    }

    private void initWidget() {
        context = MainActivity.this;
        Intent i = new Intent(context, NetworkStateService.class);
        startService(i);
        generator = new RandomTransitionGenerator(5000, new DecelerateInterpolator());
        bing_bg.setTransitionGenerator(generator);
        IMAGES = new ArrayList<>();
        // 加载自定义字体
        try {
            TEXT_TYPE = Typeface.createFromAsset(getAssets(), "HelveticaNeueLTPro-ThEx.otf");
        } catch (Exception e) {
            TEXT_TYPE = null;
        }
        if (TEXT_TYPE != null) {
            month.setTypeface(TEXT_TYPE);
            week.setTypeface(TEXT_TYPE);
            day.setTypeface(TEXT_TYPE);
            title.setTypeface(TEXT_TYPE);
            month.setText(DateUtils.covertMonth(DateUtils.month()));
            week.setText(DateUtils.convertWeek(DateUtils.week()));
            day.setText(DateUtils.day() + "");
        }


        bing_bg.setOnClickListener(this);
        bing_bg.setOnLongClickListener(this);

        bingPresenter = new IBingPresenterImpl(this);
        bing_bg.setClickable(false);
        bingPresenter.getUrlInfo(IMAGES);

        receiver=new Receiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.communication.NOT_INFO");
        registerReceiver(receiver,filter);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bing_bg:
                if (!isPause) {
                    bing_bg.pause();
                    isPause = true;
                } else {
                    bing_bg.resume();
                    isPause = false;
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bing_bg.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bing_bg.resume();
    }

    @Override
    public void showPic(List<Bean.ImagesBean> posts) {
        GlideUtils.getInstance().loadImage(context, bing_bg, IMAGES.get(0), true);
        title.setText(posts.get(0).getCopyright().substring(0, posts.get(0).getCopyright().indexOf("(")));
        copyright.setText(posts.get(0).getCopyright().substring(posts.get(0).getCopyright().indexOf("("), posts.get(0).getCopyright().indexOf(")") + 1));
        bing_bg.setClickable(true);
    }

    @Override
    public void showLoading() {
        rotateLoading.start();
        bing_bg.setClickable(false);
    }

    @Override
    public void hideLoading() {
        rotateLoading.stop();
        bing_bg.setClickable(true);
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.bing_bg:
                if (NetWorkUtils.isNetworkConnected(MainActivity.this) && bing_bg.isClickable()) {
                    showMeTheDialog(MainActivity.this);
                }
                break;
        }
        return true;
    }

    private void showMeTheDialog(final Context context) {
        PromptDialog.show((Activity) context, "是否下载当前图片", new PromptDialog.OnConfirmListener() {
            @Override
            public void onConfirmClick() {
                if (NetWorkUtils.isNetworkConnected(context)) {
                    new DownloadThread().execute();
                }
            }
        });
    }



    class DownloadThread extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            UIHandler.sendEmptyMessage(0);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try{
                ScreenUtils.saveBitmapToJpg(MainActivity.this, ScreenUtils.getBitmap(IMAGES.get(0)));
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            UIHandler.sendEmptyMessage(1);
        }
    }

    //
    class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            UIHandler.sendEmptyMessage(2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}

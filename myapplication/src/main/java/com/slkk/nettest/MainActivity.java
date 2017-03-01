package com.slkk.nettest;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "testnet";
    private TextView tv_type, tv_now_speed, tv_ave_speed;
    private Button btn;
    private ImageView needle;
    private Info info;
    private byte[] imageBytes;
    private boolean flag;
    private int last_degree = 0, cur_degree;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            if (msg.what == 0x123) {
                tv_now_speed.setText(msg.arg1 + "KB/S");
                tv_ave_speed.setText(msg.arg2 + "KB/S");
                startAnimation(msg.arg1);
            }
            if (msg.what == 0x100) {
                tv_now_speed.setText("0KB/S");
                startAnimation(0);
                btn.setText("开始测试");
                btn.setEnabled(true);
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = new Info();

        tv_type = (TextView) findViewById(R.id.connection_type);
        tv_now_speed = (TextView) findViewById(R.id.now_speed);
        tv_ave_speed = (TextView) findViewById(R.id.ave_speed);
        needle = (ImageView) findViewById(R.id.needle);
        btn = (Button) findViewById(R.id.start_btn);


        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                tv_type.setText(networkInfo.getTypeName());


                btn.setText("测试中");
                btn.setEnabled(false);
                info.hadfinishByte = 0;
                info.speed = 0;
                info.totalByte = 1024;
                new DownloadThread().start();
                new GetInfoThread().start();
            }
        });
    }


    class DownloadThread extends Thread {

        @Override
        public void run() {
            // TODO Auto-generated method stub
//            String url_string = "http://172.16.2.202:8089/Test2/test.rar";
            String url_string = "http://tds.ott.cp31.ott.cibntv.net/youku_downpage/cibn_cCIBN_YouKu_for_v5.1.0_B2017_02_27.apk";

            String path = "file";
            long start_time, cur_time;
            URL url;
            URLConnection connection;
            InputStream iStream = null;
            String fileName = "test.rar";
            OutputStream outputStream = null;
            try {
                url = new URL(url_string);
                connection = url.openConnection();

                String SDCard = Environment.getExternalStorageDirectory() + "";
                String pathName = SDCard + "/" + path + "/" + fileName;//文件存储路径

                info.totalByte = connection.getContentLength();
                File file = new File(pathName);
                if (file.exists()) {
                    file.delete();
                    Log.i(TAG, "run: file exitst");
                }
                String dir = SDCard + "/" + path;
                new File(dir).mkdir();//新建文件夹
                file.createNewFile();//新建文件
                iStream = connection.getInputStream();
                outputStream = new FileOutputStream(file);
                start_time = System.currentTimeMillis();
                byte[] buffer = new byte[4 * 1024];
                while (iStream.read() != -1 && flag) {
                    outputStream.write(buffer);
                    info.hadfinishByte++;
                    cur_time = System.currentTimeMillis();
                    if (cur_time - start_time == 0) {
                        info.speed = 1000;
                    } else {
                        info.speed = info.hadfinishByte / (cur_time - start_time) * 1000;
                    }
                }
                outputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    iStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class GetInfoThread extends Thread {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            double sum, counter;
            int cur_speed, ave_speed;
            try {
                sum = 0;
                counter = 0;
                while (info.hadfinishByte < info.totalByte && flag) {
                    Thread.sleep(1000);

                    sum += info.speed;
                    counter++;
                    cur_speed = (int) info.speed;
                    ave_speed = (int) (sum / counter);
                    Log.e("Test", "cur_speed:" + info.speed / 1024 + "KB/S ave_speed:" + ave_speed / 1024);
                    Message msg = new Message();
                    msg.arg1 = ((int) info.speed / 1024);
                    msg.arg2 = ((int) ave_speed / 1024);
                    msg.what = 0x123;
                    handler.sendMessage(msg);
                }
                if (info.hadfinishByte == info.totalByte && flag) {
                    handler.sendEmptyMessage(0x100);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        flag = false;
        super.onBackPressed();
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        flag = true;
        super.onResume();
    }

    private void startAnimation(int cur_speed) {
        cur_degree = getDegree(cur_speed);

        RotateAnimation rotateAnimation = new RotateAnimation(last_degree, cur_degree, Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setDuration(1000);
        last_degree = cur_degree;
        needle.startAnimation(rotateAnimation);
    }

    private int getDegree(double cur_speed) {
        int ret = 0;
        if (cur_speed >= 0 && cur_speed <= 512) {
            ret = (int) (15.0 * cur_speed / 128.0);
        } else if (cur_speed >= 512 && cur_speed <= 1024) {
            ret = (int) (60 + 15.0 * cur_speed / 256.0);
        } else if (cur_speed >= 1024 && cur_speed <= 10 * 1024) {
            ret = (int) (90 + 15.0 * cur_speed / 1024.0);
        } else {
            ret = 180;
        }
        return ret;
    }

}

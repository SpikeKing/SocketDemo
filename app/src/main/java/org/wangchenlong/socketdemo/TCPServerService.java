package org.wangchenlong.socketdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP的服务端
 * <p/>
 * Created by wangchenlong on 16/6/16.
 */
public class TCPServerService extends Service {
    private static final String TAG = "DEBUG-WCL: " + TCPServerService.class.getSimpleName();
    private boolean mIsServiceDestroyed = false;
    private String[] mDefinedMessages = new String[]{
            "我是Spike, 哈哈, 你们的好朋友!",
            "请问你叫什么名字?",
            "塞班是个好地方, 非常适合度假.",
            "要记得关注我哦, 共同学习共同进步.",
            "啊呀呀, 心情不好的时候要编程, 心情好的时候也要编程"
    };

    @Override public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();
    }

    @Override public void onDestroy() {
        mIsServiceDestroyed = true;
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private class TcpServer implements Runnable {
        @Override public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8644);
            } catch (IOException e) {
                Log.e(TAG, "建立链接失败, 端口:8644");
                e.printStackTrace();
            }

            while (!mIsServiceDestroyed) {
                try {
                    if (serverSocket != null) {
                        final Socket client = serverSocket.accept();
                        Log.e(TAG, "接收链接");
                        new Thread() {
                            @Override public void run() {
                                try {
                                    responseClient(client);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void responseClient(Socket client) throws IOException {

    }

}

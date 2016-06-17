package org.wangchenlong.socketdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * TCP的服务端
 * <p/>
 * Created by wangchenlong on 16/6/16.
 */
public class ServerService extends Service {
    private static final String TAG = "DEBUG-WCL: " + ServerService.class.getSimpleName();

    private boolean mIsServiceDestroyed = false;
    private String[] mDefinedMessages = new String[]{
            "我是Spike, 哈哈, 你们的好朋友!",
            "请问你叫什么名字?",
            "塞班是个好地方, 非常适合度假.",
            "要记得关注我哦, 共同学习共同进步.",
            "啊呀呀, 心情不好的时候要编程, 心情好的时候也要编程"
    };

    public static final int PORT = 8644;

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
                serverSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                Log.e(TAG, "建立链接失败, 端口:" + PORT);
                e.printStackTrace();
                return; // 链接建立失败直接返回
            }

            while (!mIsServiceDestroyed) {
                try {
                    final Socket client = serverSocket.accept();
                    Log.e(TAG, "接收数据");
                    new Thread() {
                        @Override public void run() {
                            try {
                                responseClient(client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 接收客户端的消息
     *
     * @param client 客户端
     * @throws IOException
     */
    private void responseClient(Socket client) throws IOException {
        // 接收客户端消息
        BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));

        // 向客户端发送消息
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(client.getOutputStream())), true);

        out.println("欢迎欢迎, 我是Spike!");
        while (!mIsServiceDestroyed) {
            String str = in.readLine();
            Log.e(TAG, "信息来自: " + str);
            if (str == null) {
                break;
            }

            int i = new Random().nextInt(mDefinedMessages.length);
            String msg = mDefinedMessages[i];
            out.println(msg);
            Log.e(TAG, "发送信息: " + msg);
        }

        System.out.println("客户端退出");

        // 关闭通信
        close(out);
        close(in);
        client.close();
    }

    /**
     * 关闭
     * @param closeable 关闭
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

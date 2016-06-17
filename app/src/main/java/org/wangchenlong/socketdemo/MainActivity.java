package org.wangchenlong.socketdemo;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();

    private TextView mTvContent; // 显示聊天内容
    private EditText mEtMessage; // 输入发送数据
    private Button mBSend; // 发送数据

    private PrintWriter mPrintWriter; // 向服务端发送消息
    private Socket mClientSocket; // 客户端的Socket

    private static final int MESSAGE_RECEIVE_NEW_MSG = 0;
    private static final int MESSAGE_SOCKET_CONNECTED = 1;

    // 处理
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_RECEIVE_NEW_MSG:
                    mTvContent.setText(
                            String.valueOf(mTvContent.getText().toString() + msg.obj));
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    mBSend.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvContent = (TextView) findViewById(R.id.main_tv_content);
        mEtMessage = (EditText) findViewById(R.id.main_et_edit_text);
        mBSend = (Button) findViewById(R.id.main_b_send);

        Intent intent = new Intent(this, ServerService.class);
        startService(intent);

        new Thread(new Runnable() {
            @Override public void run() {
                connectTCPServer();
            }
        }).start();
    }

    @Override protected void onDestroy() {
        if (mClientSocket != null) {
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    /**
     * 连接TCP服务器
     */
    private void connectTCPServer() {
        Socket socket = null;

        // 不停重试直到连接成功为止
        while (socket == null) {
            try {
                socket = new Socket("localhost", ServerService.PORT);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.e(TAG, "服务器连接成功");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                Log.e(TAG, "连接TCP服务失败, 重试...");
            }
        }

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing()) {
                String msg = br.readLine();
                Log.e(TAG, "收到信息: " + msg);
                if (msg != null) {
                    String time = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(System.currentTimeMillis());
                    String showedMsg = "server " + time + ":" + msg + "\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG, showedMsg)
                            .sendToTarget();
                }
            }
            Log.e(TAG, "退出");
            ServerService.close(mPrintWriter);
            ServerService.close(br);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息的回调事件
     *
     * @param view 视图
     */
    public void sendMessage(View view) {
        String msg = mEtMessage.getText().toString();
        if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
            mPrintWriter.println(msg);
            mEtMessage.setText("");
            String time = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(System.currentTimeMillis());
            String showedMsg = "self " + time + ":" + msg + "\n";
            mTvContent.setText(String.valueOf(mTvContent.getText() + showedMsg));
        }
    }
}

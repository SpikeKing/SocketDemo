# 使用 Socket 处理跨进程的实时聊天

> 欢迎Follow我的GitHub: https://github.com/SpikeKing

**Socket**是套接字, 网络通信经常使用的方法, 分为TCP和UDP两种模式, 需要网络权限, 当然也可以应用于跨进程通信. 本文通过一个简易的聊天程序, 熟悉**Socket**的使用方法.

本文源码的GitHub[下载地址](https://github.com/SpikeKing/SocketDemo)

逻辑: 客户端向服务端发送数据, 服务端收到后返回客户端数据.

---

## Server

Socket处理属于网络请求, 需要在其他线程中使用, 不能应用于主线程. 

``` java
new Thread(new TcpServer()).start();
```

TCP服务的Socket链接. 设置Socket的端口号``ServerSocket(PORT)``, 不断循环的接收数据``serverSocket.accept()``, 在``responseClient()``方法处理数据.

``` java
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
```

> ``mIsServiceDestroyed``用于判断服务器是否存活, 防止内存泄露.

处理Socket数据, 使用``BufferedReader``读取数据, 使用``PrintWriter ``写入数据, 循环检测, 结束时关闭缓存和Socket.

``` java
private void responseClient(Socket client) throws IOException {
    // 接收客户端消息
    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

    // 向客户端发送消息
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
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
```

服务器使用单独线程, 模拟跨进程通信.

``` xml
<service
    android:name=".TCPServerService"
    android:process=":remote"/>
```

需要申请网络权限, 连网和访问网络状态.

``` xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

---

## Client

客户端, 向服务器发送数据, 并接收服务器返回的数据.

启动服务, 连接TCP服务器.

``` java
Intent intent = new Intent(this, TCPServerService.class);
startService(intent);

new Thread(new Runnable() {
    @Override public void run() {
        connectTCPServer();
    }
}).start();
```

尝试连接服务器, 每隔1秒进行重试, 并初始化发送缓存``PrintWriter ``.

``` java
Socket socket = null;

// 不停重试直到连接成功为止
while (socket == null) {
    try {
        socket = new Socket("localhost", TCPServerService.PORT);
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
```

成功后, 循环调用, 监听``BufferedReader``, 是否有数据返回.

``` java
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
```

Handler处理数据, 分为连接成功和获取数据两种情况.

``` java
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
```

点击按钮发送数据, 直接在PrintWriter中写入, 即可.

``` java
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
```

当我们向服务端发送数据时, 就会获取服务端的返回, 模拟聊天效果.

---

效果

![效果](https://raw.githubusercontent.com/SpikeKing/SocketDemo/master/articles/socket-demo.png)

Socket作为经典的网络通信方式, 有很多应用, 也可以实现跨进程通信, 希望能熟练掌握.

OK, that's all! Enjoy it!

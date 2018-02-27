package com.github.faucamp.simplertmp.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.github.faucamp.simplertmp.RtmpPublisher;
import com.github.faucamp.simplertmp.amf.AmfMap;
import com.github.faucamp.simplertmp.amf.AmfNull;
import com.github.faucamp.simplertmp.amf.AmfNumber;
import com.github.faucamp.simplertmp.amf.AmfObject;
import com.github.faucamp.simplertmp.amf.AmfString;
import com.github.faucamp.simplertmp.packets.Abort;
import com.github.faucamp.simplertmp.packets.Data;
import com.github.faucamp.simplertmp.packets.Handshake;
import com.github.faucamp.simplertmp.packets.Command;
import com.github.faucamp.simplertmp.packets.Audio;
import com.github.faucamp.simplertmp.packets.SetPeerBandwidth;
import com.github.faucamp.simplertmp.packets.Video;
import com.github.faucamp.simplertmp.packets.UserControl;
import com.github.faucamp.simplertmp.packets.RtmpPacket;
import com.github.faucamp.simplertmp.packets.WindowAckSize;
import java.security.MessageDigest;
import android.util.Base64;

/**
 * Main RTMP connection implementation class
 *
 * @author francois, leoma
 */
public class RtmpConnection implements RtmpPublisher {

    private static final String TAG = "RtmpConnection";
    private static final Pattern rtmpUrlPattern = Pattern.compile("^rtmp://([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");
    private static final Pattern rtmpUrlPattern_auth = Pattern.compile("^rtmp://([a-zA-Z0-9\\.\\-]+(\\:[a-zA-Z0-9\\.&amp;%\\$\\-]+)*@)?([^/:]+)(:(\\d+))*/([^/]+)(/(.*))*$");
    //Pattern.compile("^rtmp://?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?((\w)+:(\w)+@)([^/:]+)(:(\d+))*/([^/]+)(/(.*))*$");
    private RtmpHandler mHandler;
    private int port;
    private String host;
    private String user;
    private String pswd;
    private String appName;
    private String streamName;
    private String publishType;
    private String swfUrl;
    private String tcUrl;
    private String pageUrl;
    private Socket socket;
    private String srsServerInfo = "";
    private String socketExceptionCause = "";
    private RtmpSessionInfo rtmpSessionInfo;
    private RtmpDecoder rtmpDecoder;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private Thread rxPacketHandler;
    private volatile boolean connected = false;
    private volatile boolean publishPermitted = false;
    private final Object connectingLock = new Object();
    private final Object publishLock = new Object();
    private AtomicInteger videoFrameCacheNumber = new AtomicInteger(0);
    private int currentStreamId = 0;
    private int transactionIdCounter = 0;
    private AmfString serverIpAddr;
    private AmfNumber serverPid;
    private AmfNumber serverId;
    private int videoWidth;
    private int videoHeight;
    private int videoFrameCount;
    private int videoDataLength;
    private int audioFrameCount;
    private int audioDataLength;
    private long videoLastTimeMillis;
    private long audioLastTimeMillis;
    private String auth_params = "";
    private String auth_mod = "";
    private String auth_salt = "";
    private String auth_challenge = "";
    private String auth_opaque = "";
    private String auth_nonce = "";

    public void ResetAuthParams(){
        auth_params = "";
        auth_mod = "";
        auth_challenge = "";
        auth_salt = "";
        auth_opaque = "";
        auth_nonce = "";
    }

    public RtmpConnection(RtmpHandler handler) {
        mHandler = handler;
    }

    private void handshake(InputStream in, OutputStream out) throws IOException {
        Handshake handshake = new Handshake();
        handshake.writeC0(out);
        handshake.writeC1(out); // Write C1 without waiting for S0
        out.flush();
        handshake.readS0(in);
        handshake.readS1(in);
        handshake.writeC2(out);
        handshake.readS2(in);
    }

    /**
     * 去掉url中的路径，留下请求参数部分
     * @param strURL url地址
     * @return url请求参数部分
     */
    private static String TruncateUrlPage(String strURL)
    {
        String strAllParam=null;
        String[] arrSplit=null;

        strURL=strURL.trim();

        arrSplit=strURL.split("[?]");
        if(strURL.length()>1)
        {
            if(arrSplit.length>1)
            {
                if(arrSplit[1]!=null)
                {
                    strAllParam=arrSplit[1];
                }
            }
        }

        return strAllParam;
    }
    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     * @param URL  url地址
     * @return  url请求参数部分
     */
    public static Map<String, String> URLRequest(String URL)
    {
        Map<String, String> mapRequest = new HashMap<String, String>();

        String[] arrSplit=null;

        String strUrlParam=TruncateUrlPage(URL);
        if(strUrlParam==null)
        {
            return mapRequest;
        }
        //每个键值为一组 www.2cto.com
        arrSplit=strUrlParam.split("[&]");
        for(String strSplit:arrSplit)
        {
            String[] arrSplitEqual=null;
            arrSplitEqual= strSplit.split("[=]", 2);

            //解析出键值
            if(arrSplitEqual.length>1)
            {
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);

            }
            else
            {
                if(arrSplitEqual[0]!="")
                {
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }


    private void handle_invoke_error(String description)
    {
        //description=>"authmod=adobe", "authmod=llnw", "?reason=needauth", "salt", "opaque", "challenge", "nonce"
        //[ AccessManager.Reject ] : [ code=403 need auth; authmod=adobe ] :
        if (description.contains("code=403 need auth")) {
            if (description.contains("authmod=adobe")) {
                auth_mod="adobe";
            } else if (description.contains("authmod=llnw")) {
                auth_mod="llnw";
            }
        }
        String tag = "?reason=needauth";
        if (description.contains(tag)) {
            String str = description.substring(description.indexOf(tag) );

            Map<String, String > mapRequest  = URLRequest(str);
            auth_salt = mapRequest.get("salt");
            auth_challenge = mapRequest.get("challenge");
            auth_opaque = mapRequest.get("opaque");
            auth_nonce = mapRequest.get("nonce");
        }
    }

    static char hexdigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    /**
     * 把byte[]数组转换成十六进制字符串表示形式
     * @param tmp    要转换的byte[]
     * @return 十六进制字符串表示形式
     */

    private static String byteToHexString(byte[] tmp) {
        String s;
        // 用字节表示就是 16 个字节
        char str[] = new char[16 * 2]; // 每个字节用 16 进制表示的话，使用两个字符，
        // 所以表示成 16 进制需要 32 个字符
        int k = 0; // 表示转换结果中对应的字符位置
        for (int i = 0; i < 16; i++) { // 从第一个字节开始，对 MD5 的每一个字节
            // 转换成 16 进制字符的转换
            byte byte0 = tmp[i]; // 取第 i 个字节
            str[k++] = hexdigits[byte0 >>> 4 & 0xf]; // 取字节中高 4 位的数字转换,
            // >>> 为逻辑右移，将符号位一起右移
            str[k++] = hexdigits[byte0 & 0xf]; // 取字节中低 4 位的数字转换
        }
        s = new String(str); // 换后的结果转换为字符串
        return s;
    }

    static String do_adobe_auth(String user, String password, String salt,
                                String opaque, String challenge) throws NoSuchAlgorithmException
    {
        if (salt == "" || challenge == "") {
            String auth_params = String.format(
                    "?authmod=%s&user=%s",
                    "adobe", user);

            return auth_params;
        }
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Random random = new Random();
        String challenge2 = String.format( "%08x", random.nextInt());
        //challenge2 = "bcb7448d";
        md5.update(user.getBytes());
        md5.update(salt.getBytes());
        md5.update(password.getBytes());

        byte[] m = md5.digest();//加密
        String hashstr = Base64.encodeToString(m, Base64.NO_WRAP);
        md5.reset();

        md5.update(hashstr.getBytes());
        if (opaque.length()>0) {
            md5.update(opaque.getBytes());
        } else if (challenge.length()>0) {
            md5.update(challenge.getBytes());
        }
        md5.update(challenge2.getBytes());
        m = md5.digest();//加密
        hashstr = Base64.encodeToString(m, Base64.NO_WRAP);



        String auth_params = String.format(
                "?authmod=%s&user=%s&challenge=%s&response=%s",
                "adobe", user, challenge2, hashstr);

        if (opaque.length()>0)
            auth_params += String.format("&opaque=%s", opaque);
        return auth_params;
    }


    private String do_llnw_auth(String  user, String password, String app, String  nonce) throws NoSuchAlgorithmException
    {
        if (nonce == ""
                ) {
            String auth_params = String.format(
                    "?authmod=%s&user=%s",
                    "llnw", user);

            return auth_params;
        }
        String realm = new String("live");
        String method = new String("publish");
        String qop = new String("auth");
        String nc = new String("00000001");
        String cnonce;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Random random = new Random();
        cnonce = String.format( "%08x", random.nextInt());
        md5.update(user.getBytes());
        md5.update(":".getBytes());
        md5.update(realm.getBytes());
        md5.update(":".getBytes());
        md5.update(password.getBytes());
        byte[] m = md5.digest();//加密
        String hashstr1 = byteToHexString(m);

        md5.reset();
        md5.update(method.getBytes());
        md5.update(":/".getBytes());
        md5.update(app.getBytes());
        if (app.indexOf('/')>=0) {
            md5.update("/_definst_".getBytes());
        }
        m = md5.digest();//加密
        String hashstr2 = byteToHexString(m);

        md5.reset();
        md5.update(hashstr1.getBytes());
        md5.update(":".getBytes());
        if (nonce.length()>0) {
            md5.update(nonce.getBytes());
        }
        md5.update(":".getBytes());
        md5.update(nc.getBytes());
        md5.update(":".getBytes());
        md5.update(cnonce.getBytes());
        md5.update(":".getBytes());
        md5.update(qop.getBytes());
        md5.update(":".getBytes());
        md5.update(hashstr2.getBytes());
        m = md5.digest();//加密
        hashstr1 = byteToHexString(m);

        String auth_params = String.format(
                "?authmod=%s&user=%s&nonce=%s&cnonce=%s&nc=%s&response=%s",
                "llnw", user, nonce, cnonce, nc, hashstr1);

        return auth_params;
    }

    @Override
    public boolean connect(String url) {

        Matcher matcher_auth = rtmpUrlPattern_auth.matcher(url);
        Matcher matcher = rtmpUrlPattern.matcher(url);
        auth_params = "";

        if (matcher_auth.matches()) {

            swfUrl = "";
            pageUrl = "";
            user = matcher_auth.group(1);
            pswd = matcher_auth.group(2);
            if (url.indexOf('@')>=0) {
                tcUrl = url.substring(0, 7) + url.substring(url.indexOf('@') + 1, url.lastIndexOf('/'));
            } else {
                tcUrl = url.substring(0, url.lastIndexOf('/'));
            }
            if (pswd != null) {
                user = user.substring(0, user.length() - pswd.length()-1);
                pswd = pswd.substring(1);
            } else {
                pswd = "";
                if (user == null)
                    user = "";
            }
            host = matcher_auth.group(1+2);
            String portStr = matcher_auth.group(3+2);
            port = portStr != null ? Integer.parseInt(portStr) : 1935;
            appName = matcher_auth.group(4+2);
            streamName = matcher_auth.group(6+2);
            try {
                if (user != "" && pswd != "") {
                    if (auth_mod == "adobe") {
                        auth_params = do_adobe_auth(user, pswd, auth_salt, auth_opaque, auth_challenge);
                    } else if (auth_mod == "llnw") {
                        auth_params = do_llnw_auth(user, pswd, appName, auth_nonce);
                    }
                }
            } catch (Exception e) {
            }
        } else if (matcher.matches()) {
            tcUrl = url.substring(0, url.lastIndexOf('/'));
            swfUrl = "";
            pageUrl = "";
            user = "";
            pswd = "";
            host = matcher.group(1);
            String portStr = matcher.group(3);
            port = portStr != null ? Integer.parseInt(portStr) : 1935;
            appName = matcher.group(4);
            streamName = matcher.group(6);
        } else {
            mHandler.notifyRtmpIllegalArgumentException(new IllegalArgumentException(
                    "Invalid RTMP URL. Must be in format: rtmp://[user:pswd@]host[:port]/application[/streamName]"));
            return false;
        }

        // socket connection
        Log.d(TAG, "connect() called. " + " User: " + user + ", Pswd: " + pswd + ", Host: " + host + ", port: " + port + ", appName: " + appName + ", publishPath: " + streamName);
        rtmpSessionInfo = new RtmpSessionInfo();
        rtmpDecoder = new RtmpDecoder(rtmpSessionInfo);
        socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        try {
            socket.connect(socketAddress, 3000);
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            Log.d(TAG, "connect(): socket connection established, doing handhake...");
            handshake(inputStream, outputStream);
            Log.d(TAG, "connect(): handshake done");
        } catch (IOException e) {
            e.printStackTrace();
            mHandler.notifyRtmpIOException(e);
            return false;
        }

        // Start the "main" handling thread
        rxPacketHandler = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "starting main rx handler loop");
                    handleRxPacketLoop();
                } catch (IOException ex) {
                    Logger.getLogger(RtmpConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        rxPacketHandler.start();

        return rtmpConnect();
    }

    private boolean rtmpConnect() {
        if (connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Already connected to RTMP server"));
            return false;
        }

        // Mark session timestamp of all chunk stream information on connection.
        ChunkStreamInfo.markSessionTimestampTx();

        Log.d(TAG, "rtmpConnect(): Building 'connect' invoke packet");
        ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
        Command invoke = new Command("connect", ++transactionIdCounter, chunkStreamInfo);
        invoke.getHeader().setMessageStreamId(0);
        AmfObject args = new AmfObject();
        args.setProperty("app", appName + auth_params);
        args.setProperty("type", "nonprivate");
        args.setProperty("flashVer", "FMLE/3.0 (compatible; Lavf57.41.100)"); // Flash player OS: Linux, version: 11.2.202.233
        args.setProperty("tcUrl", tcUrl + auth_params);
/*
        args.setProperty("swfUrl", swfUrl);
        args.setProperty("fpad", false);
        args.setProperty("capabilities", 239);
        args.setProperty("audioCodecs", 3575);
        args.setProperty("videoCodecs", 252);
        args.setProperty("videoFunction", 1);
        args.setProperty("pageUrl", pageUrl);
        args.setProperty("objectEncoding", 0);
  */
        invoke.addData(args);
        sendRtmpPacket(invoke);
        mHandler.notifyRtmpConnecting("Connecting");

        synchronized (connectingLock) {
            try {
                connectingLock.wait(5000);
            } catch (InterruptedException ex) {
                // do nothing
            }
        }
        if (!connected) {
            shutdown();
        }
        return connected;
    }

    @Override
    public boolean publish(String type) {
        if (type == null) {
            mHandler.notifyRtmpIllegalArgumentException(new IllegalArgumentException("No publish type specified"));
            return false;
        }
        publishType = type;
        return createStream();
    }

    private boolean createStream() {
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return false;
        }
        if (currentStreamId != 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Current stream object has existed"));
            return false;
        }

        Log.d(TAG, "createStream(): Sending releaseStream command...");
        // transactionId == 2
        Command releaseStream = new Command("releaseStream", ++transactionIdCounter);
        releaseStream.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
        releaseStream.addData(new AmfNull());  // command object: null for "createStream"
        releaseStream.addData(streamName);  // command object: null for "releaseStream"
        sendRtmpPacket(releaseStream);

        Log.d(TAG, "createStream(): Sending FCPublish command...");
        // transactionId == 3
        Command FCPublish = new Command("FCPublish", ++transactionIdCounter);
        FCPublish.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
        FCPublish.addData(new AmfNull());  // command object: null for "FCPublish"
        FCPublish.addData(streamName);
        sendRtmpPacket(FCPublish);

        Log.d(TAG, "createStream(): Sending createStream command...");
        ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_OVER_CONNECTION);
        // transactionId == 4
        Command createStream = new Command("createStream", ++transactionIdCounter, chunkStreamInfo);
        createStream.addData(new AmfNull());  // command object: null for "createStream"
        sendRtmpPacket(createStream);

        // Waiting for "NetStream.Publish.Start" response.
        synchronized (publishLock) {
            try {
                publishLock.wait(5000);
            } catch (InterruptedException ex) {
                // do nothing
            }
        }
        if (publishPermitted) {
            mHandler.notifyRtmpConnected("Connected" + srsServerInfo);
        } else {
            shutdown();
        }
        return publishPermitted;
    }

    private void fmlePublish() {
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return;
        }
        if (currentStreamId == 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("No current stream object exists"));
            return;
        }

        Log.d(TAG, "fmlePublish(): Sending publish command...");
        // transactionId == 0
        Command publish = new Command("publish", 0);
        publish.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
        publish.getHeader().setMessageStreamId(currentStreamId);
        publish.addData(new AmfNull());  // command object: null for "publish"
        publish.addData(streamName);
        publish.addData(publishType);
        sendRtmpPacket(publish);
    }

    private void onMetaData() {
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return;
        }
        if (currentStreamId == 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("No current stream object exists"));
            return;
        }

        Log.d(TAG, "onMetaData(): Sending empty onMetaData...");
        Data metadata = new Data("@setDataFrame");
        metadata.getHeader().setMessageStreamId(currentStreamId);
        metadata.addData("onMetaData");
        AmfMap ecmaArray = new AmfMap();
        ecmaArray.setProperty("duration", 0);
        ecmaArray.setProperty("width", videoWidth);
        ecmaArray.setProperty("height", videoHeight);
        ecmaArray.setProperty("videodatarate", 0);
        ecmaArray.setProperty("framerate", 0);
        ecmaArray.setProperty("audiodatarate", 0);
        ecmaArray.setProperty("audiosamplerate", 44100);
        ecmaArray.setProperty("audiosamplesize", 16);
        ecmaArray.setProperty("stereo", true);
        ecmaArray.setProperty("filesize", 0);
        metadata.addData(ecmaArray);
        sendRtmpPacket(metadata);
    }

    @Override
    public void close() {
        if (socket != null) {
            closeStream();
        }
        shutdown();
    }

    private void closeStream() {
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return;
        }
        if (currentStreamId == 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("No current stream object exists"));
            return;
        }
        if (!publishPermitted) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not get _result(Netstream.Publish.Start)"));
            return;
        }
        Log.d(TAG, "closeStream(): setting current stream ID to 0");
        Command closeStream = new Command("closeStream", 0);
        closeStream.getHeader().setChunkStreamId(ChunkStreamInfo.RTMP_CID_OVER_STREAM);
        closeStream.getHeader().setMessageStreamId(currentStreamId);
        closeStream.addData(new AmfNull());
        sendRtmpPacket(closeStream);
        mHandler.notifyRtmpStopped();
    }

    private void shutdown() {
        if (socket != null) {
            try {
                // It will raise EOFException in handleRxPacketThread
                socket.shutdownInput();
                // It will raise SocketException in sendRtmpPacket
                socket.shutdownOutput();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // shutdown rxPacketHandler
            if (rxPacketHandler != null) {
                rxPacketHandler.interrupt();
                try {
                    rxPacketHandler.join();
                } catch (InterruptedException ie) {
                    rxPacketHandler.interrupt();
                }
                rxPacketHandler = null;
            }

            // shutdown socket as well as its input and output stream
            try {
                socket.close();
                Log.d(TAG, "socket closed");
            } catch (IOException ex) {
                Log.e(TAG, "shutdown(): failed to close socket", ex);
            }

            mHandler.notifyRtmpDisconnected();
        }

        reset();
    }

    private void reset() {
        connected = false;
        publishPermitted = false;
        tcUrl = null;
        swfUrl = null;
        pageUrl = null;
        appName = null;
        streamName = null;
        publishType = null;
        currentStreamId = 0;
        transactionIdCounter = 0;
        videoFrameCacheNumber.set(0);
        socketExceptionCause = "";
        serverIpAddr = null;
        serverPid = null;
        serverId = null;
        socket = null;
        rtmpSessionInfo = null;
        rtmpDecoder = null;
    }

    @Override
    public void publishAudioData(byte[] data, int dts) {
        if (data == null || data.length == 0 || dts < 0) {
            mHandler.notifyRtmpIllegalArgumentException(new IllegalArgumentException("Invalid Audio Data"));
            return;
        }
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return;
        }
        if (currentStreamId == 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("No current stream object exists"));
            return;
        }
        if (!publishPermitted) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not get _result(Netstream.Publish.Start)"));
            return;
        }
        Audio audio = new Audio();
        audio.setData(data);
        audio.getHeader().setAbsoluteTimestamp(dts);
        audio.getHeader().setMessageStreamId(currentStreamId);
        sendRtmpPacket(audio);
        calcAudioBitrate(audio.getHeader().getPacketLength());
        mHandler.notifyRtmpAudioStreaming();
    }

    @Override
    public void publishVideoData(byte[] data, int dts) {
        if (data == null || data.length == 0 || dts < 0) {
            mHandler.notifyRtmpIllegalArgumentException(new IllegalArgumentException("Invalid Video Data"));
            return;
        }
        if (!connected) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not connected to RTMP server"));
            return;
        }
        if (currentStreamId == 0) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("No current stream object exists"));
            return;
        }
        if (!publishPermitted) {
            mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Not get _result(Netstream.Publish.Start)"));
            return;
        }
        Video video = new Video();
        video.setData(data);
        video.getHeader().setAbsoluteTimestamp(dts);
        video.getHeader().setMessageStreamId(currentStreamId);
        sendRtmpPacket(video);
        videoFrameCacheNumber.decrementAndGet();
        calcVideoFpsAndBitrate(video.getHeader().getPacketLength());
        mHandler.notifyRtmpVideoStreaming();
    }

    private void calcVideoFpsAndBitrate(int length) {
        videoDataLength += length;
        if (videoFrameCount == 0) {
            videoLastTimeMillis = System.nanoTime() / 1000000;
            videoFrameCount++;
        } else {
            if (++videoFrameCount >= 48) {
                long diffTimeMillis = System.nanoTime() / 1000000 - videoLastTimeMillis;
                mHandler.notifyRtmpVideoFpsChanged((double) videoFrameCount * 1000 / diffTimeMillis);
                mHandler.notifyRtmpVideoBitrateChanged((double) videoDataLength * 8 * 1000 / diffTimeMillis);
                videoFrameCount = 0;
                videoDataLength = 0;
            }
        }
    }

    private void calcAudioBitrate(int length) {
        audioDataLength += length;
        if (audioFrameCount == 0) {
            audioLastTimeMillis = System.nanoTime() / 1000000;
            audioFrameCount++;
        } else {
            if (++audioFrameCount >= 48) {
                long diffTimeMillis = System.nanoTime() / 1000000 - audioLastTimeMillis;
                mHandler.notifyRtmpAudioBitrateChanged((double) audioDataLength * 8 * 1000 / diffTimeMillis);
                audioFrameCount = 0;
                audioDataLength = 0;
            }
        }
    }

    private void sendRtmpPacket(RtmpPacket rtmpPacket) {
        try {
            ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(rtmpPacket.getHeader().getChunkStreamId());
            chunkStreamInfo.setPrevHeaderTx(rtmpPacket.getHeader());
            if (!(rtmpPacket instanceof Video || rtmpPacket instanceof Audio)) {
                rtmpPacket.getHeader().setAbsoluteTimestamp((int) chunkStreamInfo.markAbsoluteTimestampTx());
            }
            rtmpPacket.writeTo(outputStream, rtmpSessionInfo.getTxChunkSize(), chunkStreamInfo);
            Log.d(TAG, "wrote packet: " + rtmpPacket + ", size: " + rtmpPacket.getHeader().getPacketLength());
            if (rtmpPacket instanceof Command) {
                rtmpSessionInfo.addInvokedCommand(((Command) rtmpPacket).getTransactionId(), ((Command) rtmpPacket).getCommandName());
            }
            outputStream.flush();
        } catch (SocketException se) {
            // Since there are still remaining AV frame in the cache, we set a flag to guarantee the
            // socket exception only issue one time.
            if (!socketExceptionCause.contentEquals(se.getMessage())) {
                socketExceptionCause = se.getMessage();
                Log.e(TAG, "Caught SocketException during write loop, shutting down: " + se.getMessage());
                mHandler.notifyRtmpSocketException(se);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Caught IOException during write loop, shutting down: " + ioe.getMessage());
            mHandler.notifyRtmpIOException(ioe);
        }
    }

    private void handleRxPacketLoop() throws IOException {
        // Handle all queued received RTMP packets
        while (!Thread.interrupted()) {
            try {
                // It will be blocked when no data in input stream buffer
                RtmpPacket rtmpPacket = rtmpDecoder.readPacket(inputStream);
                if (rtmpPacket != null) {
                    Log.d(TAG, "handleRxPacketLoop(): RTMP rx packet message type: " + rtmpPacket.getHeader().getMessageType());
                    switch (rtmpPacket.getHeader().getMessageType()) {
                        case ABORT:
                            rtmpSessionInfo.getChunkStreamInfo(((Abort) rtmpPacket).getChunkStreamId()).clearStoredChunks();
                            break;
                        case USER_CONTROL_MESSAGE:
                            UserControl user = (UserControl) rtmpPacket;
                            switch (user.getType()) {
                                case STREAM_BEGIN:
                                    if (currentStreamId != user.getFirstEventData()) {
                                        mHandler.notifyRtmpIllegalStateException(new IllegalStateException("Current stream ID error!"));
                                    }
                                    break;
                                case PING_REQUEST:
                                    ChunkStreamInfo channelInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
                                    Log.d(TAG, "handleRxPacketLoop(): Sending PONG reply..");
                                    UserControl pong = new UserControl(user, channelInfo);
                                    sendRtmpPacket(pong);
                                    break;
                                case STREAM_EOF:
                                    Log.i(TAG, "handleRxPacketLoop(): Stream EOF reached, closing RTMP writer...");
                                    break;
                                default:
                                    // Ignore...
                                    break;
                            }
                            break;
                        case WINDOW_ACKNOWLEDGEMENT_SIZE:
                            WindowAckSize windowAckSize = (WindowAckSize) rtmpPacket;
                            int size = windowAckSize.getAcknowledgementWindowSize();
                            Log.d(TAG, "handleRxPacketLoop(): Setting acknowledgement window size: " + size);
                            rtmpSessionInfo.setAcknowledgmentWindowSize(size);
                            break;
                        case SET_PEER_BANDWIDTH:
                            SetPeerBandwidth bw = (SetPeerBandwidth) rtmpPacket;
                            rtmpSessionInfo.setAcknowledgmentWindowSize(bw.getAcknowledgementWindowSize());
                            int acknowledgementWindowsize = rtmpSessionInfo.getAcknowledgementWindowSize();
                            ChunkStreamInfo chunkStreamInfo = rtmpSessionInfo.getChunkStreamInfo(ChunkStreamInfo.RTMP_CID_PROTOCOL_CONTROL);
                            Log.d(TAG, "handleRxPacketLoop(): Send acknowledgement window size: " + acknowledgementWindowsize);
                            sendRtmpPacket(new WindowAckSize(acknowledgementWindowsize, chunkStreamInfo));
                            // Set socket option
                            socket.setSendBufferSize(acknowledgementWindowsize);
                            break;
                        case COMMAND_AMF0:
                            handleRxInvoke((Command) rtmpPacket);
                            break;
                        case SET_CHUNK_SIZE:
                            //zorro : TODO
                            Log.w(TAG, "handleRxPacketLoop(): SET_CHUNK_SIZE");
                            break;
                        default:
                            Log.w(TAG, "handleRxPacketLoop(): Not handling unimplemented/unknown packet of type: " + rtmpPacket.getHeader().getMessageType());
                            break;
                    }
                }
            } catch (EOFException eof) {
                Thread.currentThread().interrupt();
            } catch (SocketException se) {
                Log.e(TAG, "Caught SocketException while reading/decoding packet, shutting down: " + se.getMessage());
                mHandler.notifyRtmpSocketException(se);
            } catch (IOException ioe) {
                Log.e(TAG, "Caught exception while reading/decoding packet, shutting down: " + ioe.getMessage());
                mHandler.notifyRtmpIOException(ioe);
            }
        }
    }

    private void handleRxInvoke(Command invoke) throws IOException {
        String commandName = invoke.getCommandName();

        if (commandName.equals("_result")) {
            // This is the result of one of the methods invoked by us
            String method = rtmpSessionInfo.takeInvokedCommand(invoke.getTransactionId());

            Log.d(TAG, "handleRxInvoke: Got result for invoked method: " + method);
            if ("connect".equals(method)) {
                // Capture server ip/pid/id information if any
                srsServerInfo = onSrsServerInfo(invoke);
                // We can now send createStream commands
                connected = true;
                synchronized (connectingLock) {
                    connectingLock.notifyAll();
                }
            } else if ("createStream".contains(method)) {
                // Get stream id
                currentStreamId = (int) ((AmfNumber) invoke.getData().get(1)).getValue();
                Log.d(TAG, "handleRxInvoke(): Stream ID to publish: " + currentStreamId);
                if (streamName != null && publishType != null) {
                    fmlePublish();
                }
            } else if ("releaseStream".contains(method)) {
                Log.d(TAG, "handleRxInvoke(): 'releaseStream'");
            } else if ("FCPublish".contains(method)) {
                Log.d(TAG, "handleRxInvoke(): 'FCPublish'");
            } else {
                Log.w(TAG, "handleRxInvoke(): '_result' message received for unknown method: " + method);
            }
        } else if (commandName.equals("_error")) {
            String level = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("level")).getValue();
            String code = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("code")).getValue();
            String description = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("description")).getValue();
            Log.d(TAG, "handleRxInvoke(): handle_invoke_error " +level + ", " + code + ", "+ description);
            handle_invoke_error(description);
            synchronized (connectingLock) {
                connectingLock.notifyAll();
            }
        } else if (commandName.equals("onBWDone")) {
            Log.d(TAG, "handleRxInvoke(): 'onBWDone'");
        } else if (commandName.equals("onFCPublish")) {
            Log.d(TAG, "handleRxInvoke(): 'onFCPublish'");
        } else if (commandName.equals("onStatus")) {
            String code = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("code")).getValue();
//            String description = ((AmfString) ((AmfObject) invoke.getData().get(1)).getProperty("description")).getValue();
            Log.d(TAG, "handleRxInvoke(): onStatus " + code);
            if (code.equals("NetStream.Publish.Start")) {
                onMetaData();
                // We can now publish AV data
                publishPermitted = true;
                synchronized (publishLock) {
                    publishLock.notifyAll();
                }
            }
        } else {
            Log.e(TAG, "handleRxInvoke(): Unknown/unhandled server invoke: " + invoke);
        }
    }

    private void LogData(AmfObject objData, String item) {
        if ( (objData).getProperty(item) instanceof AmfString) {
            AmfString it = (AmfString) (objData).getProperty(item);
            Log.e(TAG, "onSrsServerInfo(): "+it+" " + it.getValue());
        } else if ( (objData).getProperty(item) instanceof AmfNumber) {
            AmfNumber it = (AmfNumber) (objData).getProperty(item);
            Log.e(TAG, "onSrsServerInfo(): "+item+" " + it.getValue());
        }
    }
    private String onSrsServerInfo(Command invoke) {
        // SRS server special information
        AmfObject objData = (AmfObject) invoke.getData().get(1);

        //add by zorro
        AmfObject objData0 = (AmfObject) invoke.getData().get(0);
        LogData(objData0, "fmsVer");
        LogData(objData, "level");
        LogData(objData, "code");
        LogData(objData, "description");
        LogData(objData, "clientid");
        LogData(objData, "objectEncoding");
        //add end

        if ((objData).getProperty("data") instanceof AmfObject) {
            objData = ((AmfObject) objData.getProperty("data"));
            serverIpAddr = (AmfString) objData.getProperty("srs_server_ip");
            serverPid = (AmfNumber) objData.getProperty("srs_pid");
            serverId = (AmfNumber) objData.getProperty("srs_id");
        }
        String info = "";
        info += serverIpAddr == null ? "" : " ip: " + serverIpAddr.getValue();
        info += serverPid == null ? "" : " pid: " + (int) serverPid.getValue();
        info += serverId == null ? "" : " id: " + (int) serverId.getValue();
        return info;
    }

    @Override
    public AtomicInteger getVideoFrameCacheNumber() {
        return videoFrameCacheNumber;
    }

    @Override
    public final String getServerIpAddr() {
        return serverIpAddr == null ? null : serverIpAddr.getValue();
    }

    @Override
    public final int getServerPid() {
        return serverPid == null ? 0 : (int) serverPid.getValue();
    }

    @Override
    public final int getServerId() {
        return serverId == null ? 0 : (int) serverId.getValue();
    }

    @Override
    public void setVideoResolution(int width, int height) {
        videoWidth = width;
        videoHeight = height;
    }
}

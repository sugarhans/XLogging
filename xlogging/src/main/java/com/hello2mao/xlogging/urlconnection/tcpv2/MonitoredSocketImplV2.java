package com.hello2mao.xlogging.urlconnection.tcpv2;


import android.text.TextUtils;
import android.webkit.URLUtil;

import com.hello2mao.xlogging.urlconnection.MonitoredSocketInterface;
import com.hello2mao.xlogging.urlconnection.NetworkMonitor;
import com.hello2mao.xlogging.urlconnection.NetworkTransactionState;
import com.hello2mao.xlogging.urlconnection.UrlBuilder;
import com.hello2mao.xlogging.urlconnection.iov2.HttpRequestParsingOutputStreamV2;
import com.hello2mao.xlogging.urlconnection.iov2.HttpResponseParsingInputStreamV2;
import com.hello2mao.xlogging.urlconnection.tracing.ConnectSocketData;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.util.LinkedList;
import java.util.Queue;


public class MonitoredSocketImplV2 extends SocketImpl implements MonitoredSocketInterface {

    private static final AgentLog LOG = AgentLogManager.getAgentLog();
    private static final int ACCEPT_IDX = 0;
    private static final int AVAILABLE_IDX = 1;
    private static final int BIND_IDX = 2;
    private static final int CLOSE_IDX = 3;
    private static final int CONNECT_INET_ADDRESS_IDX = 4;
    private static final int CONNECT_SOCKET_ADDRESS_IDX = 5;
    private static final int CONNECT_STRING_INT_IDX = 6;
    private static final int CREATE_IDX = 7;
    private static final int GET_FILE_DESCRIPTOR_IDX = 8;
    private static final int GET_INET_ADDRESS_IDX = 9;
    private static final int GET_INPUT_STREAM_IDX = 10;
    private static final int GET_LOCAL_PORT_IDX = 11;
    private static final int GET_OUTPUT_STREAM_IDX = 12;
    private static final int GET_PORT_IDX = 13;
    private static final int LISTEN_IDX = 14;
    private static final int SEND_URGENT_DATA_IDX = 15;
    private static final int SET_PERFORMANCE_PREFERENCES_IDX = 16;
    private static final int SHUTDOWN_INPUT_IDX = 17;
    private static final int SHUTDOWN_OUTPUT_IDX = 18;
    private static final int SUPPORTS_URGENT_DATA_IDX = 19;
    private static final int NUM_METHODS = 20;

    // F
    private SocketImpl delegate;
    private static Field addressField;
    private static Field fdField;
    private static Field localportField;
    private static Field portField;
    private static Method[] methods = new Method[NUM_METHODS];
    private HttpResponseParsingInputStreamV2 inputStream;
    private HttpRequestParsingOutputStreamV2 outputStream;
    private final Queue<NetworkTransactionState> queue;

    //String C
    private String ipAddress;
    // int B
    private int connectTime;

    static {
        try {
            addressField = SocketImpl.class.getDeclaredField("address");
            fdField = SocketImpl.class.getDeclaredField("fd");
            localportField = SocketImpl.class.getDeclaredField("localport");
            portField = SocketImpl.class.getDeclaredField("port");
            ReflectionUtil.setAccessible(addressField, new AccessibleObject[] { fdField,
                    localportField, portField });
            methods[0] = SocketImpl.class.getDeclaredMethod("accept", SocketImpl.class);
            methods[1] = SocketImpl.class.getDeclaredMethod("available");
            methods[2] = SocketImpl.class.getDeclaredMethod("bind", InetAddress.class, Integer.TYPE);
            methods[3] = SocketImpl.class.getDeclaredMethod("close");
            methods[4] = SocketImpl.class.getDeclaredMethod("connect", InetAddress.class, Integer.TYPE);
            methods[5] = SocketImpl.class.getDeclaredMethod("connect", SocketAddress.class, Integer.TYPE);
            methods[6] = SocketImpl.class.getDeclaredMethod("connect", String.class, Integer.TYPE);
            methods[7] = SocketImpl.class.getDeclaredMethod("create", Boolean.TYPE);
            methods[8] = SocketImpl.class.getDeclaredMethod("getFileDescriptor");
            methods[9] = SocketImpl.class.getDeclaredMethod("getInetAddress");
            methods[10] = SocketImpl.class.getDeclaredMethod("getInputStream");
            methods[11] = SocketImpl.class.getDeclaredMethod("getLocalPort");
            methods[12] = SocketImpl.class.getDeclaredMethod("getOutputStream");
            methods[13] = SocketImpl.class.getDeclaredMethod("getPort");
            methods[14] = SocketImpl.class.getDeclaredMethod("listen", Integer.TYPE);
            methods[15] = SocketImpl.class.getDeclaredMethod("sendUrgentData", Integer.TYPE);
            methods[16] = SocketImpl.class.getDeclaredMethod("setPerformancePreferences",
                    Integer.TYPE, Integer.TYPE, Integer.TYPE);
            methods[17] = SocketImpl.class.getDeclaredMethod("shutdownInput");
            methods[18] = SocketImpl.class.getDeclaredMethod("shutdownOutput");
            methods[19] = SocketImpl.class.getDeclaredMethod("supportsUrgentData");
            ReflectionUtil.setAccessible(methods);
        } catch (Exception e) {
            LOG.error("Caught error while MonitoredSocketImplV2 static field: ", e);
            AgentHealth.noticeException(e);
        }
    }

    public MonitoredSocketImplV2(SocketImpl socketImpl) {
        this.ipAddress = "";
        this.queue = new LinkedList<>();
        if (socketImpl == null) {
            throw new NullPointerException("delegate was null");
        }
        this.delegate = socketImpl;
        syncFromDelegate();
    }

    private NetworkTransactionState createNetworkTransactionState(final boolean b) {
        LOG.debug("MonitorSocketImplV2 createNewStats");
        final NetworkTransactionState nbsTransactionState = new NetworkTransactionState();
        nbsTransactionState.setAddress((this.ipAddress == null) ? "" : this.ipAddress);
        nbsTransactionState.setPort(this.port);
        if (this.port == 443) {
            nbsTransactionState.setScheme(UrlBuilder.Scheme.HTTPS);
        }
        else {
            nbsTransactionState.setScheme(UrlBuilder.Scheme.HTTP);
        }
        nbsTransactionState.setCarrier(Agent.getActiveNetworkCarrier());
        nbsTransactionState.setTcpHandShakeTime(this.connectTime);
        return nbsTransactionState;
    }

    private void syncFromDelegate() {
        try {
            this.address = (InetAddress) addressField.get(delegate);
            this.fd = (FileDescriptor) fdField.get(delegate);
            this.localport = localportField.getInt(delegate);
            this.port = portField.getInt(delegate);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.error("Caught error while syncFromDelegate: ", e);
            AgentHealth.noticeException(e);
        }
    }

    private void syncToDelegate() {
        try {
            addressField.set(delegate, address);
            fdField.set(delegate, fd);
            localportField.setInt(delegate, localport);
            portField.setInt(delegate, port);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeThrowsIOException(int index, Object[] params) throws IOException {
        try {
            return invoke(index, params);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T invokeNoThrow(int index, Object[] params) {
        try {
            return (T) invoke(index, params);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过反射调用代理的方法
     *
     * @param index index
     * @param params params
     * @param <T> T
     * @return T
     * @throws Exception Exception
     */
    private <T> T invoke(int index, Object[] params) throws Exception {
        syncToDelegate();
        try {
            return (T) methods[index].invoke(delegate, params);
        } catch (Exception e) {
            LOG.error("Caught error while MonitoredSocketImplV2 invoke: ", e);
            AgentHealth.noticeException(e);
        } finally {
            syncFromDelegate();
        }
        return null;
    }

    @Override
    protected void create(boolean stream) throws IOException {
        invokeThrowsIOException(CREATE_IDX, new Object[] { stream });
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        long currentTimeMillis = System.currentTimeMillis();
        invokeThrowsIOException(CONNECT_STRING_INT_IDX, new Object[] { host, port});
        // FIXME:
        connectTime = (int)(System.currentTimeMillis() - currentTimeMillis);
        if (port == 443 && !TextUtils.isEmpty(host)) {
            final ConnectSocketData connectSocketData = new ConnectSocketData();
            connectSocketData.setSocketAddress(host);
            connectSocketData.setHost(host);
            connectSocketData.setPort(port);
            connectSocketData.setConnectTime(connectTime);
            NetworkMonitor.addConnectSocket(connectSocketData);
        }
        LOG.error("Unexpected MonitoredSocketImplV2: connectTime-1=" + (System.currentTimeMillis() - currentTimeMillis));
    }

    @Override
    protected void connect(InetAddress inetAddress, int port) throws IOException {
        // FIXME:
        LOG.error("Unexpected MonitoredSocketImplV2: connectTime-2");
        invokeThrowsIOException(CONNECT_INET_ADDRESS_IDX, new Object[] { inetAddress, port});
    }

    @Override
    protected void connect(SocketAddress socketAddress, int timeout) throws IOException {
        try {
            String host = "";
            if (socketAddress instanceof InetSocketAddress) {
                // inetSocketAddress="ip.taobao.com/42.120.226.92:80" URLConnection/OkHttp3
                // inetSocketAddress="/42.120.226.92:80" HttpClient
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                host = URLUtil.getHost(inetSocketAddress);
                this.ipAddress = URLUtil.getIpAddress(inetSocketAddress);
            }
            long currentTimeMillis = System.currentTimeMillis();
            invokeThrowsIOException(CONNECT_SOCKET_ADDRESS_IDX, new Object[] { socketAddress, timeout});
            this.connectTime = (int) (System.currentTimeMillis() - currentTimeMillis);
            // FIXME:经过简单测试，URLConnection、OkHttp3、httpClient都是connectTime-3
            LOG.debug("MonitoredSocketImplV2: connectTime-3=" + connectTime + ", ipAddress="
                    + ipAddress + ", host=" + host);
            if (this.port == 443) {
                NetworkMonitor.addConnectSocketInfo(ipAddress, host, this.connectTime);
                // TODO:check this
                LOG.debug("MonitoredSocketImplV2: https");
            }
        } catch (IOException e) {
            // TODO:采集异常
            throw  e;
        }
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        invokeThrowsIOException(BIND_IDX, new Object[] { host, port});
    }

    @Override
    protected void listen(int backlog) throws IOException {
        invokeThrowsIOException(LISTEN_IDX, new Object[] { backlog});
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        invokeThrowsIOException(ACCEPT_IDX, new Object[] { s});
    }

    private InputStream unsafeInstrumentInputStream(InputStream inputStream) {
        if (inputStream != null) {
            if ((this.inputStream != null) && (this.inputStream.isDelegateSame(inputStream))) {
                LOG.debug("MonitoredSocketImplV2: unsafeInstrumentInputStream DelegateSame");
                inputStream = this.inputStream;
            } else {
                LOG.debug("MonitoredSocketImplV2: new HttpResponseParsingInputStreamV2");
                this.inputStream = new HttpResponseParsingInputStreamV2(this, inputStream);
                inputStream = this.inputStream;
            }
        }
        return inputStream;
    }

    private OutputStream unsafeInstrumentOutputStream(OutputStream outputStream) {
        if (outputStream != null) {
            if (this.outputStream != null && this.outputStream.isOutputStreamSame(outputStream)) {
                LOG.debug("MonitoredSocketImplV2: unsafeInstrumentOutputStream DelegateSame");
                outputStream = this.outputStream;
            } else {
                LOG.debug("MonitoredSocketImplV2: new HttpRequestParsingOutputStreamV2");
                this.outputStream = new HttpRequestParsingOutputStreamV2(this, outputStream);
                outputStream = this.outputStream;
            }
        }
        return outputStream;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        LOG.debug("v2 getInputStream..");
        InputStream inputStream = (InputStream) invokeThrowsIOException(GET_INPUT_STREAM_IDX, new Object[0]);
        return unsafeInstrumentInputStream(inputStream);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        LOG.debug("v2 getOutputStream..");
        OutputStream outputStream = (OutputStream) invokeThrowsIOException(GET_OUTPUT_STREAM_IDX, new Object[0]);
        try {
            outputStream = unsafeInstrumentOutputStream(outputStream);
        } catch (ThreadDeath threadDeath) {
            throw threadDeath;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return outputStream;
    }

    @Override
    protected int available() throws IOException {
        Integer localInteger = (Integer) invokeThrowsIOException(AVAILABLE_IDX, new Object[0]);
        if (localInteger == null) {
            throw new RuntimeException("Received a null Integer");
        }
        return localInteger;
    }

    @Override
    protected void close() throws IOException {
        LOG.debug("MonitoredSocketImplV2 close");
        invokeThrowsIOException(CLOSE_IDX, new Object[0]);
        if (inputStream != null) {
            inputStream.notifySocketClosing();
        }
    }

    @Override
    public void shutdownInput() throws IOException {
        invokeThrowsIOException(SHUTDOWN_INPUT_IDX, new Object[0]);
    }

    @Override
    public void shutdownOutput() throws IOException {
        invokeThrowsIOException(SHUTDOWN_OUTPUT_IDX, new Object[0]);
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        return invokeNoThrow(GET_FILE_DESCRIPTOR_IDX, new Object[0]);
    }

    @Override
    public InetAddress getInetAddress() {
        return invokeNoThrow(GET_INET_ADDRESS_IDX, new Object[0]);
    }

    @Override
    public int getPort() {
        return invokeNoThrow(GET_PORT_IDX, new Object[0]);
    }

    @Override
    public int getLocalPort() {
        return invokeNoThrow(GET_LOCAL_PORT_IDX, new Object[0]);
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        invokeThrowsIOException(SEND_URGENT_DATA_IDX, new Object[] { data});
    }

    @Override
    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
        invokeNoThrow(SET_PERFORMANCE_PREFERENCES_IDX, new Object[] { connectionTime, latency, bandwidth });
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        delegate.setOption(optID, value);
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return delegate.getOption(optID);
    }

    @Override
    public boolean supportsUrgentData() {
        return (Boolean) invokeNoThrow(SUPPORTS_URGENT_DATA_IDX, new Object[0]);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }


    @Override
    public NetworkTransactionState createNetworkTransactionState() {
        return createNetworkTransactionState(true);
    }

    @Override
    public void enqueueNetworkTransactionState(NetworkTransactionState networkTransactionState) {
        synchronized (queue) {
            queue.add(networkTransactionState);
        }
    }

    @Override
    public NetworkTransactionState dequeueNetworkTransactionState() {
        synchronized (queue) {
            return queue.poll();
        }
    }
}
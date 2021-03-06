package com.hello2mao.xlogging.internal;

import com.hello2mao.xlogging.TransactionData;

/**
 * record transaction state
 */
public class TransactionState {

    // Basic Info
    private String host;
    private String ip;
    private String scheme;
    private String protocol;
    private int port;
    private String pathAndQuery;
    private String requestMethod;
    private int statusCode;
    private long bytesSent;
    private long bytesReceived;

    // Timing
    // DNS查询->TCP建连->SSL握手->请求->响应->接收
    // (1)DNS查询时间=dnsLookupEndTime-dnsLookupStartTime
    // (2)TCP建连时间=tcpConnectEndTime-tcpConnectStartTime
    // (3)SSL握手时间=sslHandshakeEndTime-sslHandshakeStartTime
    // (4)请求时间=requestEndTime-requestStartTime
    // (5)响应时间=responseStartTime-requestEndTime
    // (6)首包时间=请求时间+响应时间
    // (7)接收时间=responseEndTime-responseStartTime
    private long dnsLookupStartTime;
    private long dnsLookupEndTime;
    private long tcpConnectStartTime;
    private long tcpConnectEndTime;
    private long sslHandshakeStartTime;
    private long sslHandshakeEndTime;
    private long requestStartTime;
    private long requestEndTime;
    private long responseStartTime;
    private long responseEndTime;

    // Optional
    private String exception;
    private boolean socketReuse;

    // Other
    private State state;

    /**
     * HTTP(S) transaction state
     */
    private enum State {
        READY,
        SENT,
        COMPLETE
    }

    public TransactionState() {
        // Basic Info
        this.host = "";
        this.ip = "";
        this.scheme = "";
        this.protocol = "";
        this.port = -1;
        this.pathAndQuery = "";
        this.requestMethod = "";
        this.statusCode = -1;
        this.bytesSent = -1;
        this.bytesReceived = -1;
        // Timing
        this.dnsLookupStartTime = -1L;
        this.dnsLookupEndTime = -1L;
        this.tcpConnectStartTime = -1L;
        this.tcpConnectEndTime = -1L;
        this.sslHandshakeStartTime = -1L;
        this.sslHandshakeEndTime = -1L;
        this.requestStartTime = -1L;
        this.requestEndTime = -1L;
        this.responseStartTime = -1L;
        this.responseEndTime = -1L;
        // Optional
        this.exception = "";
        this.socketReuse = false;
        // Other
        this.state = State.READY;
    }

    public TransactionState(TransactionState transactionState) {
        this();
        // Basic Info
        this.host = transactionState.getHost();
        this.ip = transactionState.getIp();
        this.scheme = transactionState.getScheme();
        this.protocol = transactionState.getProtocol();
        this.port = transactionState.getPort();
        this.pathAndQuery = transactionState.getPathAndQuery();
        this.requestMethod = transactionState.getRequestMethod();
        this.statusCode = transactionState.getStatusCode();
        this.bytesSent = transactionState.getBytesSent();
        this.bytesReceived = transactionState.getBytesReceived();
        // Timing
        this.dnsLookupStartTime = transactionState.getDnsLookupStartTime();
        this.dnsLookupEndTime = transactionState.getDnsLookupEndTime();
        this.tcpConnectStartTime = transactionState.getTcpConnectStartTime();
        this.tcpConnectEndTime = transactionState.getTcpConnectEndTime();
        this.sslHandshakeStartTime = transactionState.getSslHandshakeStartTime();
        this.sslHandshakeEndTime = transactionState.getSslHandshakeEndTime();
        this.requestStartTime = transactionState.getRequestStartTime();
        this.requestEndTime = transactionState.getRequestEndTime();
        this.responseStartTime = transactionState.getResponseStartTime();
        this.responseEndTime = transactionState.getResponseEndTime();
        // Optional
        this.exception = transactionState.getException();
        this.socketReuse = transactionState.isSocketReuse();
        // Other
        this.state = transactionState.getState();
    }

    public TransactionData toTransactionData() {
        TransactionData transactionData = new TransactionData();
        transactionData.setHost(host);
        transactionData.setIp(ip);
        transactionData.setScheme(scheme);
        transactionData.setProtocol(protocol);
        transactionData.setPort(port);
        transactionData.setPathAndQuery(pathAndQuery);
        transactionData.setRequestMethod(requestMethod);
        transactionData.setStatusCode(statusCode);
        transactionData.setBytesSent(bytesSent);
        transactionData.setBytesReceived(bytesReceived);
        if ((tcpConnectEndTime != -1L) && (tcpConnectStartTime != -1L)) {
            transactionData.setTcpConnectTime(tcpConnectEndTime - tcpConnectStartTime);
        } else {
            transactionData.setTcpConnectTime(-1L);
        }
        if ((sslHandshakeEndTime != -1L) && (sslHandshakeStartTime != -1L)) {
            transactionData.setSslHandshakeTime(sslHandshakeEndTime - sslHandshakeStartTime);
        } else {
            transactionData.setSslHandshakeTime(-1L);
        }
        if ((requestEndTime != -1L) && (requestStartTime != -1L)) {
            transactionData.setRequestTime(requestEndTime - requestStartTime);
        } else {
            transactionData.setRequestTime(-1L);
        }
        if ((responseEndTime != -1L) && (requestEndTime != -1L)) {
            transactionData.setResponseTime(responseEndTime - requestEndTime);
        } else {
            transactionData.setResponseTime(-1L);
        }
        if ((responseStartTime != -1L) && (requestEndTime != -1L)) {
            transactionData.setFirstPackageTime(responseStartTime - requestEndTime);
        } else {
            transactionData.setFirstPackageTime(-1L);
        }
        transactionData.setException(exception);
        transactionData.setSocketReuse(socketReuse);
        return transactionData;
    }

    public void endTransaction() {
        if (!isComplete()) {
            this.state = State.COMPLETE;
            this.responseEndTime = System.currentTimeMillis();
        }
    }

    public boolean isComplete() {
        return state.ordinal() >= State.COMPLETE.ordinal();
    }

    public boolean isSent() {
        return state.ordinal() >= State.SENT.ordinal();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPathAndQuery() {
        return pathAndQuery;
    }

    public void setPathAndQuery(String pathAndQuery) {
        this.pathAndQuery = pathAndQuery;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getDnsLookupStartTime() {
        return dnsLookupStartTime;
    }

    public void setDnsLookupStartTime(long dnsLookupStartTime) {
        this.dnsLookupStartTime = dnsLookupStartTime;
    }

    public long getDnsLookupEndTime() {
        return dnsLookupEndTime;
    }

    public void setDnsLookupEndTime(long dnsLookupEndTime) {
        this.dnsLookupEndTime = dnsLookupEndTime;
    }

    public long getTcpConnectStartTime() {
        return tcpConnectStartTime;
    }

    public void setTcpConnectStartTime(long tcpConnectStartTime) {
        this.tcpConnectStartTime = tcpConnectStartTime;
    }

    public long getTcpConnectEndTime() {
        return tcpConnectEndTime;
    }

    public void setTcpConnectEndTime(long tcpConnectEndTime) {
        this.tcpConnectEndTime = tcpConnectEndTime;
    }

    public long getSslHandshakeStartTime() {
        return sslHandshakeStartTime;
    }

    public void setSslHandshakeStartTime(long sslHandshakeStartTime) {
        this.sslHandshakeStartTime = sslHandshakeStartTime;
    }

    public long getSslHandshakeEndTime() {
        return sslHandshakeEndTime;
    }

    public void setSslHandshakeEndTime(long sslHandshakeEndTime) {
        this.sslHandshakeEndTime = sslHandshakeEndTime;
    }

    public long getRequestStartTime() {
        return requestStartTime;
    }

    public void setRequestStartTime(long requestStartTime) {
        this.requestStartTime = requestStartTime;
    }

    public long getRequestEndTime() {
        return requestEndTime;
    }

    public void setRequestEndTime(long requestEndTime) {
        this.requestEndTime = requestEndTime;
    }

    public long getResponseStartTime() {
        return responseStartTime;
    }

    public void setResponseStartTime(long responseStartTime) {
        this.responseStartTime = responseStartTime;
    }

    public long getResponseEndTime() {
        return responseEndTime;
    }

    public void setResponseEndTime(long responseEndTime) {
        this.responseEndTime = responseEndTime;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public boolean isSocketReuse() {
        return socketReuse;
    }

    public void setSocketReuse(boolean socketReuse) {
        this.socketReuse = socketReuse;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}

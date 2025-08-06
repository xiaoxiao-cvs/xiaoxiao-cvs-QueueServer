package com.github.queueserver.forge.http;

import com.github.queueserver.forge.QueueForgePlugin;
import com.github.queueserver.forge.http.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.*;
import okhttp3.MediaType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP代理客户端
 * 处理与代理服务器的HTTP通信
 */
public class ProxyHttpClient {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String authToken;
    
    public ProxyHttpClient(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.baseUrl = plugin.getConfigManager().getProxyServerUrl();
        this.authToken = plugin.getConfigManager().getProxyServerToken();
        
        // 创建HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new AuthInterceptor(authToken))
                .addInterceptor(new LoggingInterceptor(logger))
                .build();
        
        // 创建JSON处理器
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        
        logger.info("HTTP代理客户端已初始化，目标地址: " + baseUrl);
    }
    
    /**
     * 发送心跳
     */
    public CompletableFuture<Boolean> sendHeartbeat() {
        HeartbeatRequest heartbeat = new HeartbeatRequest();
        heartbeat.setServerName("queue-server"); // 固定服务器名称
        heartbeat.setOnlinePlayers(plugin.getServer().getOnlinePlayers().size());
        heartbeat.setMaxPlayers(plugin.getServer().getMaxPlayers());
        heartbeat.setTps(getCurrentTPS());
        heartbeat.setTimestamp(System.currentTimeMillis());
        
        return sendRequest("/api/heartbeat", heartbeat, HeartbeatResponse.class)
                .thenApply(response -> response != null && response.isSuccess());
    }
    
    /**
     * 通知服务器就绪
     */
    public CompletableFuture<Boolean> notifyServerReady() {
        ServerReadyRequest request = new ServerReadyRequest();
        request.setServerName("queue-server"); // 固定服务器名称
        request.setReady(true);
        request.setTimestamp(System.currentTimeMillis());
        
        return sendRequest("/api/server/ready", request, ServerReadyResponse.class)
                .thenApply(response -> response != null && response.isSuccess());
    }
    
    /**
     * 获取服务器状态
     */
    public CompletableFuture<ServerStatusResponse> getServerStatus() {
        return sendGetRequest("/api/server/status", ServerStatusResponse.class);
    }
    
    /**
     * 传送玩家
     */
    public CompletableFuture<Boolean> transferPlayer(UUID playerId, String playerName) {
        PlayerTransferRequest request = new PlayerTransferRequest();
        request.setPlayerId(playerId.toString());
        request.setPlayerName(playerName);
        request.setSourceServer("queue-server"); // 固定源服务器名称
        request.setTargetServer("game"); // 目标服务器名称
        request.setTimestamp(System.currentTimeMillis());
        
        return sendRequest("/api/player/transfer", request, PlayerTransferResponse.class)
                .thenApply(response -> response != null && response.isSuccess());
    }
    
    /**
     * 获取玩家队列信息
     */
    public CompletableFuture<QueueInfoResponse> getQueueInfo(UUID playerId) {
        String url = "/api/queue/info?playerId=" + playerId.toString();
        return sendGetRequest(url, QueueInfoResponse.class);
    }
    
    /**
     * 添加玩家到队列
     */
    public CompletableFuture<Boolean> addPlayerToQueue(UUID playerId, String playerName, boolean isVip) {
        QueueAddRequest request = new QueueAddRequest();
        request.setPlayerId(playerId.toString());
        request.setPlayerName(playerName);
        request.setVip(isVip);
        request.setTimestamp(System.currentTimeMillis());
        
        return sendRequest("/api/queue/add", request, QueueAddResponse.class)
                .thenApply(response -> response != null && response.isSuccess());
    }
    
    /**
     * 从队列移除玩家
     */
    public CompletableFuture<Boolean> removePlayerFromQueue(UUID playerId) {
        QueueRemoveRequest request = new QueueRemoveRequest();
        request.setPlayerId(playerId.toString());
        request.setTimestamp(System.currentTimeMillis());
        
        return sendRequest("/api/queue/remove", request, QueueRemoveResponse.class)
                .thenApply(response -> response != null && response.isSuccess());
    }
    
    /**
     * 获取队列统计信息
     */
    public CompletableFuture<QueueStatsResponse> getQueueStats() {
        return sendGetRequest("/api/queue/stats", QueueStatsResponse.class);
    }
    
    /**
     * 发送POST请求
     */
    private <T, R> CompletableFuture<R> sendRequest(String endpoint, T requestData, Class<R> responseClass) {
        CompletableFuture<R> future = new CompletableFuture<>();
        
        try {
            String jsonBody = gson.toJson(requestData);
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            Request request = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(body)
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.log(Level.WARNING, "HTTP请求失败: " + endpoint, e);
                    future.complete(null);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            String jsonResponse = responseBody.string();
                            R result = gson.fromJson(jsonResponse, responseClass);
                            future.complete(result);
                        } else {
                            logger.warning("HTTP请求失败: " + endpoint + ", 状态码: " + response.code());
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "解析HTTP响应失败: " + endpoint, e);
                        future.complete(null);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "构建HTTP请求失败: " + endpoint, e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * 发送GET请求
     */
    private <R> CompletableFuture<R> sendGetRequest(String endpoint, Class<R> responseClass) {
        CompletableFuture<R> future = new CompletableFuture<>();
        
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .get()
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.log(Level.WARNING, "HTTP GET请求失败: " + endpoint, e);
                    future.complete(null);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            String jsonResponse = responseBody.string();
                            R result = gson.fromJson(jsonResponse, responseClass);
                            future.complete(result);
                        } else {
                            logger.warning("HTTP GET请求失败: " + endpoint + ", 状态码: " + response.code());
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "解析HTTP GET响应失败: " + endpoint, e);
                        future.complete(null);
                    }
                }
            });
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "构建HTTP GET请求失败: " + endpoint, e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * 获取当前TPS
     */
    private double getCurrentTPS() {
        try {
            // 尝试获取Paper TPS
            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            // 如果不支持，返回默认值
            return 20.0;
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void shutdown() {
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("HTTP代理客户端已关闭");
        } catch (Exception e) {
            logger.log(Level.WARNING, "关闭HTTP客户端时发生错误", e);
        }
    }
    
    /**
     * 认证拦截器
     */
    private static class AuthInterceptor implements Interceptor {
        private final String token;
        
        public AuthInterceptor(String token) {
            this.token = token;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .header("User-Agent", "QueueForgePlugin/2.0.0")
                    .header("Content-Type", "application/json")
                    .build();
            return chain.proceed(authenticatedRequest);
        }
    }
    
    /**
     * 日志拦截器
     */
    private static class LoggingInterceptor implements Interceptor {
        private final Logger logger;
        
        public LoggingInterceptor(Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            
            Response response = chain.proceed(request);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            if (logger != null) {
                logger.info(String.format("HTTP %s %s -> %d (%dms)", 
                    request.method(), 
                    request.url().encodedPath(), 
                    response.code(), 
                    duration));
            }
            
            return response;
        }
    }
}

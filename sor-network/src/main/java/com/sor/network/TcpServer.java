package com.sor.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Netty TCP 服务器
 * 
 * 特性：
 * - 支持 NIO/EPOLL 传输
 * - 基于长度的帧解码器（避免粘包）
 * - 零拷贝优化
 */
public class TcpServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(TcpServer.class);
    
    // Boss 事件循环组（接受连接）
    private final EventLoopGroup bossGroup;
    
    // Worker 事件循环组（处理 I/O）
    private final EventLoopGroup workerGroup;
    
    // 服务器端口
    private final int port;
    
    // 是否使用 EPOLL（Linux 下性能更好）
    private final boolean useEpoll;
    
    // 服务器通道
    private Channel serverChannel;
    
    /**
     * 构造函数
     */
    public TcpServer(int port) {
        this.port = port;
        this.useEpoll = Epoll.isAvailable();
        
        if (useEpoll) {
            LOG.info("Using EPOLL transport (Linux native)");
            this.bossGroup = new EpollEventLoopGroup(1);
            this.workerGroup = new EpollEventLoopGroup();
        } else {
            LOG.info("Using NIO transport");
            this.bossGroup = new NioEventLoopGroup(1);
            this.workerGroup = new NioEventLoopGroup();
        }
    }
    
    /**
     * 启动服务器
     */
    public void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        bootstrap.group(bossGroup, workerGroup)
                .channel(useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 添加长度字段解码器（解决 TCP 粘包）
                        // 最大帧长度：1MB
                        // 长度字段偏移：0
                        // 长度字段长度：4 字节（int）
                        // 调整值：4（跳过长度字段本身）
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                            1048576, 0, 4, 0, 4));
                        
                        // 添加长度字段编码器
                        pipeline.addLast(new LengthFieldPrepender(4));
                        
                        // 添加业务处理器
                        pipeline.addLast(new TcpServerHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_RCVBUF, 65536)
                .childOption(ChannelOption.SO_SNDBUF, 65536);
        
        // 绑定端口
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
        this.serverChannel = future.channel();
        
        LOG.info("TCP Server started on port {}", port);
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        LOG.info("Stopping TCP Server...");
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        
        LOG.info("TCP Server stopped");
    }
}

package com.sor.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 服务器处理器
 * 
 * 处理客户端连接、消息和断开
 */
public class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    private static final Logger LOG = LoggerFactory.getLogger(TcpServerHandler.class);
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOG.info("Client connected: {}", ctx.channel().remoteAddress());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // 读取消息（零拷贝）
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        
        // TODO: 解析 FIX 协议或其他二进制协议
        // TODO: 提交到 Disruptor 处理
        
        LOG.debug("Received {} bytes from {}", data.length, ctx.channel().remoteAddress());
        
        // 响应（可选）
        // ctx.writeAndFlush(response);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("Client disconnected: {}", ctx.channel().remoteAddress());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Exception caught from {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}

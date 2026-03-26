package com.sor.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIX 协议编解码器
 * 
 * FIX (Financial Information eXchange) 协议是金融交易行业标准
 * 格式：tag=value 对，SOH 字符（ASCII 1）分隔
 * 示例：8=FIX.4.2|9=123|35=D|...|10=123|
 */
public class FixCodec {
    
    private static final Logger LOG = LoggerFactory.getLogger(FixCodec.class);
    
    // SOH 字符（Start of Header，ASCII 码 1）
    private static final char SOH = '\u0001';
    
    // FIX 版本
    private static final String FIX_VERSION = "FIX.4.2";
    
    /**
     * 编码 FIX 消息
     * 
     * @param fields tag=value 字段数组
     * @return FIX 消息字符串
     */
    public String encode(String[][] fields) {
        StringBuilder sb = new StringBuilder();
        
        // 添加标准头
        sb.append("8=").append(FIX_VERSION).append(SOH);
        sb.append("9=").append(calculateBodyLength(fields)).append(SOH);
        
        // 添加字段
        for (String[] field : fields) {
            if (field.length == 2) {
                sb.append(field[0]).append('=').append(field[1]).append(SOH);
            }
        }
        
        // 添加校验和（简化版，实际需要计算前面所有字节的和）
        sb.append("10=").append(calculateChecksum(sb.toString())).append(SOH);
        
        return sb.toString();
    }
    
    /**
     * 解码 FIX 消息
     * 
     * @param message FIX 消息字符串
     * @return tag=value Map
     */
    public java.util.Map<String, String> decode(String message) {
        java.util.Map<String, String> fields = new java.util.HashMap<>();
        
        // 按 SOH 分割
        String[] pairs = message.split(String.valueOf(SOH));
        
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            
            int eqIndex = pair.indexOf('=');
            if (eqIndex > 0) {
                String tag = pair.substring(0, eqIndex);
                String value = pair.substring(eqIndex + 1);
                fields.put(tag, value);
            }
        }
        
        return fields;
    }
    
    /**
     * 计算消息体长度
     */
    private int calculateBodyLength(String[][] fields) {
        int length = 0;
        for (String[] field : fields) {
            if (field.length == 2) {
                // tag=value+SOH
                length += field[0].length() + 1 + field[1].length() + 1;
            }
        }
        // 加上校验和字段长度
        length += 7; // "10=xxx|"
        return length;
    }
    
    /**
     * 计算校验和（简化版）
     */
    private String calculateChecksum(String message) {
        int sum = 0;
        for (char c : message.toCharArray()) {
            sum += c;
        }
        return String.format("%03d", sum % 256);
    }
    
    /**
     * 创建新订单消息（FIX 35=D）
     */
    public String createNewOrder(String orderId, String symbol, String side, 
                                  double price, int quantity) {
        String[][] fields = new String[][] {
            {"35", "D"},           // MsgType=NewOrderSingle
            {"11", orderId},       // ClOrdID
            {"55", symbol},        // Symbol
            {"54", side},          // Side (1=Buy, 2=Sell)
            {"40", "2"},           // OrdType=Limit
            {"44", String.valueOf(price)},  // Price
            {"38", String.valueOf(quantity)}, // OrderQty
            {"60", getCurrentTime()} // TransactTime
        };
        return encode(fields);
    }
    
    /**
     * 获取当前时间（FIX 格式：YYYYMMDD-HH:MM:SS）
     */
    private String getCurrentTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
        return now.format(formatter);
    }
}

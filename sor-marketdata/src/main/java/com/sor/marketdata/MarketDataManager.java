package com.sor.marketdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 市场数据管理器
 * 
 * 管理多个交易品种的订单簿和行情数据
 */
public class MarketDataManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(MarketDataManager.class);
    
    // 订单簿缓存（按交易品种）
    private final Map<String, OrderBook> orderBooks;
    
    // 默认深度
    private static final int DEFAULT_DEPTH = 10;
    
    /**
     * 构造函数
     */
    public MarketDataManager() {
        this.orderBooks = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取或创建订单簿
     */
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, s -> {
            OrderBook book = new OrderBook(s, DEFAULT_DEPTH);
            LOG.info("Created order book for {}", s);
            return book;
        });
    }
    
    /**
     * 更新订单簿（买单）
     */
    public void updateBid(String symbol, double price, double quantity) {
        OrderBook book = getOrderBook(symbol);
        if (quantity == 0) {
            book.removeBid(price);
            LOG.debug("Removed bid {}@{}", price, symbol);
        } else {
            book.addBid(price, quantity);
            LOG.trace("Updated bid {}@{}: {}", symbol, price, quantity);
        }
    }
    
    /**
     * 更新订单簿（卖单）
     */
    public void updateAsk(String symbol, double price, double quantity) {
        OrderBook book = getOrderBook(symbol);
        if (quantity == 0) {
            book.removeAsk(price);
            LOG.debug("Removed ask {}@{}", price, symbol);
        } else {
            book.addAsk(price, quantity);
            LOG.trace("Updated ask {}@{}: {}", symbol, price, quantity);
        }
    }
    
    /**
     * 更新最新成交价
     */
    public void updateLastPrice(String symbol, double price) {
        OrderBook book = getOrderBook(symbol);
        book.setLastPrice(price);
        LOG.trace("Updated last price {}@{}", symbol, price);
    }
    
    /**
     * 获取最优买价
     */
    public double getBestBid(String symbol) {
        OrderBook book = getOrderBook(symbol);
        return book.getBestBid();
    }
    
    /**
     * 获取最优卖价
     */
    public double getBestAsk(String symbol) {
        OrderBook book = getOrderBook(symbol);
        return book.getBestAsk();
    }
    
    /**
     * 获取中间价
     */
    public double getMidPrice(String symbol) {
        OrderBook book = getOrderBook(symbol);
        return book.getMidPrice();
    }
    
    /**
     * 获取买卖价差
     */
    public double getSpread(String symbol) {
        OrderBook book = getOrderBook(symbol);
        return book.getSpread();
    }
    
    /**
     * 获取所有交易品种
     */
    public String[] getSymbols() {
        return orderBooks.keySet().toArray(new String[0]);
    }
    
    /**
     * 清除订单簿
     */
    public void clearOrderBook(String symbol) {
        OrderBook book = orderBooks.get(symbol);
        if (book != null) {
            book.clear();
            LOG.info("Cleared order book for {}", symbol);
        }
    }
    
    /**
     * 打印订单簿快照
     */
    public void printOrderBookSnapshot(String symbol) {
        OrderBook book = getOrderBook(symbol);
        LOG.info("Order Book Snapshot for {}:\n{}", symbol, book);
    }
}

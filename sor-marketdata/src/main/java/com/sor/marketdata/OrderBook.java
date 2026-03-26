package com.sor.marketdata;

import java.util.Arrays;

/**
 * 订单簿（Order Book）
 * 
 * 管理买卖盘口数据，支持价格优先、时间优先的撮合逻辑
 * 
 * 特性：
 * - 使用数组存储（避免集合类创建开销）
 * - 预分配容量（减少扩容）
 * - 价格水平聚合
 */
public class OrderBook {
    
    // 交易品种
    private final String symbol;
    
    // 买盘（ bids: 高价在前）
    private final PriceLevel[] bids;
    
    // 卖盘（asks: 低价在前）
    private final PriceLevel[] asks;
    
    // 买盘数量
    private int bidCount;
    
    // 卖盘数量
    private int askCount;
    
    // 最新成交价
    private volatile double lastPrice;
    
    // 更新时间戳（纳秒）
    private volatile long timestamp;
    
    /**
     * 构造函数
     * @param symbol 交易品种
     * @param depth 深度（默认 10 档）
     */
    public OrderBook(String symbol, int depth) {
        this.symbol = symbol;
        this.bids = new PriceLevel[depth];
        this.asks = new PriceLevel[depth];
        this.bidCount = 0;
        this.askCount = 0;
        this.lastPrice = 0.0;
        this.timestamp = System.nanoTime();
        
        // 初始化价格水平
        for (int i = 0; i < depth; i++) {
            bids[i] = new PriceLevel();
            asks[i] = new PriceLevel();
        }
    }
    
    /**
     * 添加买单
     * @param price 价格
     * @param quantity 数量
     */
    public synchronized void addBid(double price, double quantity) {
        // 查找是否已存在该价格
        for (int i = 0; i < bidCount; i++) {
            if (bids[i].getPrice() == price) {
                bids[i].addQuantity(quantity);
                return;
            }
        }
        
        // 插入新价格（保持降序）
        if (bidCount < bids.length) {
            // 找到插入位置
            int insertPos = bidCount;
            for (int i = 0; i < bidCount; i++) {
                if (price > bids[i].getPrice()) {
                    insertPos = i;
                    break;
                }
            }
            
            // 向后移动
            for (int i = bidCount; i > insertPos; i--) {
                bids[i] = bids[i - 1];
            }
            
            // 插入
            bids[insertPos].set(price, quantity);
            bidCount++;
        }
        
        updateTimestamp();
    }
    
    /**
     * 添加卖单
     * @param price 价格
     * @param quantity 数量
     */
    public synchronized void addAsk(double price, double quantity) {
        // 查找是否已存在该价格
        for (int i = 0; i < askCount; i++) {
            if (asks[i].getPrice() == price) {
                asks[i].addQuantity(quantity);
                return;
            }
        }
        
        // 插入新价格（保持升序）
        if (askCount < asks.length) {
            // 找到插入位置
            int insertPos = askCount;
            for (int i = 0; i < askCount; i++) {
                if (price < asks[i].getPrice()) {
                    insertPos = i;
                    break;
                }
            }
            
            // 向后移动
            for (int i = askCount; i > insertPos; i--) {
                asks[i] = asks[i - 1];
            }
            
            // 插入
            asks[insertPos].set(price, quantity);
            askCount++;
        }
        
        updateTimestamp();
    }
    
    /**
     * 移除买单
     * @param price 价格
     */
    public synchronized void removeBid(double price) {
        for (int i = 0; i < bidCount; i++) {
            if (bids[i].getPrice() == price) {
                // 向前移动
                for (int j = i; j < bidCount - 1; j++) {
                    // 复制值而不是引用
                    bids[j].set(bids[j + 1].getPrice(), bids[j + 1].getQuantity());
                }
                // 清除最后一个
                bids[bidCount - 1].clear();
                bidCount--;
                updateTimestamp();
                return;
            }
        }
    }
    
    /**
     * 移除卖单
     * @param price 价格
     */
    public synchronized void removeAsk(double price) {
        for (int i = 0; i < askCount; i++) {
            if (asks[i].getPrice() == price) {
                // 向前移动
                for (int j = i; j < askCount - 1; j++) {
                    // 复制值而不是引用
                    asks[j].set(asks[j + 1].getPrice(), asks[j + 1].getQuantity());
                }
                // 清除最后一个
                asks[askCount - 1].clear();
                askCount--;
                updateTimestamp();
                return;
            }
        }
    }
    
    /**
     * 更新成交量
     * @param price 价格
     * @param quantity 数量
     * @param isBid 是否为买单
     */
    public synchronized void updateQuantity(double price, double quantity, boolean isBid) {
        PriceLevel[] levels = isBid ? bids : asks;
        int count = isBid ? bidCount : askCount;
        
        for (int i = 0; i < count; i++) {
            if (levels[i].getPrice() == price) {
                levels[i].setQuantity(quantity);
                updateTimestamp();
                return;
            }
        }
    }
    
    /**
     * 获取最优买价
     */
    public synchronized double getBestBid() {
        return bidCount > 0 ? bids[0].getPrice() : 0.0;
    }
    
    /**
     * 获取最优卖价
     */
    public synchronized double getBestAsk() {
        return askCount > 0 ? asks[0].getPrice() : 0.0;
    }
    
    /**
     * 获取买卖价差
     */
    public synchronized double getSpread() {
        if (bidCount > 0 && askCount > 0) {
            return asks[0].getPrice() - bids[0].getPrice();
        }
        return 0.0;
    }
    
    /**
     * 获取中间价
     */
    public synchronized double getMidPrice() {
        if (bidCount > 0 && askCount > 0) {
            return (bids[0].getPrice() + asks[0].getPrice()) / 2.0;
        }
        return lastPrice;
    }
    
    /**
     * 设置最新成交价
     */
    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
        updateTimestamp();
    }
    
    /**
     * 获取最新成交价
     */
    public double getLastPrice() {
        return lastPrice;
    }
    
    /**
     * 获取时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取买盘深度
     */
    public int getBidCount() {
        return bidCount;
    }
    
    /**
     * 获取卖盘深度
     */
    public int getAskCount() {
        return askCount;
    }
    
    /**
     * 获取交易品种
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * 清空订单簿
     */
    public synchronized void clear() {
        bidCount = 0;
        askCount = 0;
        lastPrice = 0.0;
        updateTimestamp();
    }
    
    /**
     * 更新时间戳
     */
    private void updateTimestamp() {
        this.timestamp = System.nanoTime();
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder("OrderBook{").append(symbol).append("\n");
        sb.append("Bids:\n");
        for (int i = 0; i < Math.min(5, bidCount); i++) {
            sb.append("  ").append(bids[i]).append("\n");
        }
        sb.append("Asks:\n");
        for (int i = 0; i < Math.min(5, askCount); i++) {
            sb.append("  ").append(asks[i]).append("\n");
        }
        sb.append("Spread: ").append(getSpread()).append("\n");
        sb.append("Mid: ").append(getMidPrice()).append("}");
        return sb.toString();
    }
}

package com.sor.marketdata;

/**
 * 价格水平（Price Level）
 * 
 * 表示订单簿中某一价格的所有订单总和
 */
public class PriceLevel {
    
    // 价格
    private double price;
    
    // 总数量
    private double quantity;
    
    // 订单数量
    private int orderCount;
    
    /**
     * 构造函数
     */
    public PriceLevel() {
        this.price = 0.0;
        this.quantity = 0.0;
        this.orderCount = 0;
    }
    
    /**
     * 设置价格水平
     */
    public void set(double price, double quantity) {
        this.price = price;
        this.quantity = quantity;
        this.orderCount = 1;
    }
    
    /**
     * 添加数量
     */
    public void addQuantity(double quantity) {
        this.quantity += quantity;
        this.orderCount++;
    }
    
    /**
     * 减少数量
     */
    public void reduceQuantity(double quantity) {
        this.quantity = Math.max(0.0, this.quantity - quantity);
        if (this.quantity == 0.0) {
            this.orderCount = 0;
        }
    }
    
    /**
     * 设置数量
     */
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    /**
     * 清空
     */
    public void clear() {
        this.price = 0.0;
        this.quantity = 0.0;
        this.orderCount = 0;
    }
    
    // Getter 方法
    
    public double getPrice() {
        return price;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public int getOrderCount() {
        return orderCount;
    }
    
    @Override
    public String toString() {
        return String.format("Price{%.2f, Qty=%.0f, Orders=%d}", price, quantity, orderCount);
    }
}

package com.wzh;

import lombok.Getter;
import lombok.Setter;

/**
 * 行程
 *  一个行程单中包含多单行程
 * @author wangfl
 * @date 2024/1/17
 */
@Setter
@Getter
public class Journey {
    /**
     * 序号
     */
    private int index;

    /**
     * 车型
     */
    private String vehicleType;

    /**
     * 上车时间
     */
    private String startTime;

    /**
     * 城市
     */
    private String city;

    /**
     * 起点
     */
    private String startPosition;

    /**
     * 终点
     */
    private String endPosition;

    /**
     * 里程
     */
    private Double mileage;

    /**
     * 金额
     */
    private double money;
}

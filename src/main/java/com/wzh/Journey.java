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
public abstract class Journey {
    /**
     * 序号
     */
    private int index;
    protected abstract String getIndexTitle();

    /**
     * 车型
     */
    private String vehicleType;
    protected abstract String getVehicleTypeTitle();

    /**
     * 上车时间
     */
    private String startTime;
    protected abstract String getStartTimeTitle();

    /**
     * 城市
     */
    private String city;
    protected abstract String getCityTitle();

    /**
     * 起点
     */
    private String startPosition;
    protected abstract String getStartPositionTitle();

    /**
     * 终点
     */
    private String endPosition;
    protected abstract String getEndPositionTitle();

    /**
     * 里程
     */
    private Double mileage;
    protected abstract String getMileageTitle();

    /**
     * 金额
     */
    private double money;
    protected abstract String getMoneyTitle();
}

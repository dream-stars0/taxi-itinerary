package com.wzh.impl;

import com.wzh.Journey;

/**
 * @author wangfl
 * @date 2024/1/24
 */
public class GaodeJourney extends Journey {
    @Override
    protected String getIndexTitle() {
        return "序号";
    }

    @Override
    protected String getVehicleTypeTitle() {
        return "车型";
    }

    @Override
    protected String getStartTimeTitle() {
        return "上车时间";
    }

    @Override
    protected String getCityTitle() {
        return "城市";
    }

    @Override
    protected String getStartPositionTitle() {
        return "起点";
    }

    @Override
    protected String getEndPositionTitle() {
        return "终点";
    }

    @Override
    protected String getMileageTitle() {
        return null;
    }

    @Override
    protected String getMoneyTitle() {
        return "金额(元)";
    }
}

/*
 * (C) Copyright 2016 Ymatou (http://www.ymatou.com/). All rights reserved.
 */
package com.ymatou.payment.integration;

import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;

/**
 * Disconf配置信息读取
 * 
 * @author qianmin 2016年5月9日 上午10:42:18
 *
 */
@Component
@DisconfFile(fileName = "integration.properties")
public class IntegrationConfig {

    private String wxUnifiedOrderUrl; // 微信统一下单url
    private String wxRefundQueryUrl; // 微信退款查询url
    private String wxOrderQueryUrl; // 微信查询订单url
    private String wxJsapiCertPath; // 微信Jsapi(1278350701)对应的证书路径
    private String wxJsapiCertPass; // 微信Jsapi(1278350701)对应的证书密码
    private String wxJsapiMchId; // 微信Jsapi的商户号(1234079001)
    private String wxAppCertPath; // 微信APP(1234079001)对应的证书路径
    private String wxAppCertPass; // 微信APP(1234079001)对应的证书密码
    private String wxAppMchId; // 微信App的商户号(1234079001)
    private String aliPayBaseUrl; // 支付宝防钓鱼时间戳url
    private String ymtUserServiceUrl; // 用户服务url
    private String ymtNotifyRefundUrl; // 通知退款url
    private String ymtNotifytradingeventUrl; // 通知用户交易信息url
    private String ymtPaymentBaseUrl; // ymt.payment.baseurl

    private String openMock;
    private String aliPayBaseUrlMock; // 支付宝网关url(mock)
    private String wxUnifiedOrderUrlMock; // 微信统一下单url(mock)
    private String wxRefundQueryUrlMock; // 微信退款查询url(mock)
    private String wxOrderQueryUrlMock; // 微信查询订单url(mock)
    private String ymtUserServiceUrlMock; // 用户服务url(mock)
    private String ymtNotifyRefundUrlMock; // 通知退款url(mock)
    private String ymtNotifytradingeventUrlMock; // 通知用户交易信息url(mock)


    public String getWxOrderQueryUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getWxOrderQueryUrlMock();
        } else {
            return getWxOrderQueryUrl();
        }
    }

    @DisconfFileItem(name = "wx.orderquery.url")
    public String getWxOrderQueryUrl() {
        return wxOrderQueryUrl;
    }

    public void setWxOrderQueryUrl(String wxOrderQueryUrl) {
        this.wxOrderQueryUrl = wxOrderQueryUrl;
    }

    @DisconfFileItem(name = "wx.orderquery.url.mock")
    public String getWxOrderQueryUrlMock() {
        return wxOrderQueryUrlMock;
    }

    public void setWxOrderQueryUrlMock(String wxOrderQueryUrlMock) {
        this.wxOrderQueryUrlMock = wxOrderQueryUrlMock;
    }

    public String getYmtNotifyRefundUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getYmtNotifyRefundUrlMock();
        } else {
            return getYmtNotifyRefundUrl();
        }
    }

    public String getYmtNotifytradingeventUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getYmtNotifytradingeventUrlMock();
        } else {
            return getYmtNotifytradingeventUrl();
        }
    }

    @DisconfFileItem(name = "ymt.notifyrefund.url")
    public String getYmtNotifyRefundUrl() {
        return ymtNotifyRefundUrl;
    }

    public void setYmtNotifyRefundUrl(String ymtNotifyRefundUrl) {
        this.ymtNotifyRefundUrl = ymtNotifyRefundUrl;
    }

    @DisconfFileItem(name = "ymt.notifytradingevent.url")
    public String getYmtNotifytradingeventUrl() {
        return ymtNotifytradingeventUrl;
    }

    public void setYmtNotifytradingeventUrl(String ymtNotifytradingeventUrl) {
        this.ymtNotifytradingeventUrl = ymtNotifytradingeventUrl;
    }

    @DisconfFileItem(name = "ymt.notifyrefund.url.mock")
    public String getYmtNotifyRefundUrlMock() {
        return ymtNotifyRefundUrlMock;
    }

    public void setYmtNotifyRefundUrlMock(String ymtNotifyRefundUrlMock) {
        this.ymtNotifyRefundUrlMock = ymtNotifyRefundUrlMock;
    }

    @DisconfFileItem(name = "ymt.notifytradingevent.url.mock")
    public String getYmtNotifytradingeventUrlMock() {
        return ymtNotifytradingeventUrlMock;
    }

    public void setYmtNotifytradingeventUrlMock(String ymtNotifytradingeventUrlMock) {
        this.ymtNotifytradingeventUrlMock = ymtNotifytradingeventUrlMock;
    }

    @DisconfFileItem(name = "wx.unifiedorder.url")
    public String getWxUnifiedOrderUrl() {
        return wxUnifiedOrderUrl;
    }

    public void setWxUnifiedOrderUrl(String wxUnifiedOrderUrl) {
        this.wxUnifiedOrderUrl = wxUnifiedOrderUrl;
    }

    @DisconfFileItem(name = "wx.refundquery.url")
    public String getWxRefundQueryUrl() {
        return wxRefundQueryUrl;
    }

    public void setWxRefundQueryUrl(String wxRefundQueryUrl) {
        this.wxRefundQueryUrl = wxRefundQueryUrl;
    }

    @DisconfFileItem(name = "wx.jsapi.certpath")
    public String getWxJsapiCertPath() {
        return wxJsapiCertPath;
    }

    public void setWxJsapiCertPath(String wxJsapiCertPath) {
        this.wxJsapiCertPath = wxJsapiCertPath;
    }

    @DisconfFileItem(name = "wx.app.certpath")
    public String getWxAppCertPath() {
        return wxAppCertPath;
    }

    public void setWxAppCertPath(String wxAppCertPath) {
        this.wxAppCertPath = wxAppCertPath;
    }

    @DisconfFileItem(name = "ali.base.url")
    public String getAliPayBaseUrl() {
        return aliPayBaseUrl;
    }

    public void setAliPayBaseUrl(String aliPayBaseUrl) {
        this.aliPayBaseUrl = aliPayBaseUrl;
    }

    @DisconfFileItem(name = "ymt.userservice.url")
    public String getYmtUserServiceUrl() {
        return ymtUserServiceUrl;
    }

    public void setYmtUserServiceUrl(String ymtUserServiceUrl) {
        this.ymtUserServiceUrl = ymtUserServiceUrl;
    }

    @DisconfFileItem(name = "wx.jsapi.certpass")
    public String getWxJsapiCertPass() {
        return wxJsapiCertPass;
    }

    public void setWxJsapiCertPass(String wxJsapiCertPass) {
        this.wxJsapiCertPass = wxJsapiCertPass;
    }

    @DisconfFileItem(name = "wx.app.certpass")
    public String getWxAppCertPass() {
        return wxAppCertPass;
    }

    public void setWxAppCertPass(String wxAppCertPass) {
        this.wxAppCertPass = wxAppCertPass;
    }

    @DisconfFileItem(name = "wx.jsapi.mchid")
    public String getWxJsapiMchId() {
        return wxJsapiMchId;
    }

    public void setWxJsapiMchId(String wxJsapiMchId) {
        this.wxJsapiMchId = wxJsapiMchId;
    }

    @DisconfFileItem(name = "wx.app.mchid")
    public String getWxAppMchId() {
        return wxAppMchId;
    }

    public void setWxAppMchId(String wxAppMchId) {
        this.wxAppMchId = wxAppMchId;
    }

    @DisconfFileItem(name = "ali.base.url.mock")
    public String getAliPayBaseUrlMock() {
        return aliPayBaseUrlMock;
    }

    public void setAliPayBaseUrlMock(String aliPayBaseUrlMock) {
        this.aliPayBaseUrlMock = aliPayBaseUrlMock;
    }

    @DisconfFileItem(name = "wx.unifiedorder.url.mock")
    public String getWxUnifiedOrderUrlMock() {
        return wxUnifiedOrderUrlMock;
    }

    public void setWxUnifiedOrderUrlMock(String wxUnifiedOrderUrlMock) {
        this.wxUnifiedOrderUrlMock = wxUnifiedOrderUrlMock;
    }

    @DisconfFileItem(name = "wx.refundquery.url.mock")
    public String getWxRefundQueryUrlMock() {
        return wxRefundQueryUrlMock;
    }

    public void setWxRefundQueryUrlMock(String wxRefundQueryUrlMock) {
        this.wxRefundQueryUrlMock = wxRefundQueryUrlMock;
    }

    @DisconfFileItem(name = "ymt.userservice.url.mock")
    public String getYmtUserServiceUrlMock() {
        return ymtUserServiceUrlMock;
    }

    public void setYmtUserServiceUrlMock(String ymtUserServiceUrlMock) {
        this.ymtUserServiceUrlMock = ymtUserServiceUrlMock;
    }

    public String getWxRefundQueryUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getWxRefundQueryUrlMock();
        } else {
            return getWxRefundQueryUrl();
        }
    }

    public String getWxUnifiedOrderUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getWxUnifiedOrderUrlMock();
        } else {
            return getWxUnifiedOrderUrl();
        }
    }

    public String getYmtUserServiceUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getYmtUserServiceUrlMock();
        } else {
            return getYmtUserServiceUrl();
        }
    }

    public String getAliPayBaseUrl(HashMap<String, String> header) {
        if (isMock(header)) {
            return getAliPayBaseUrlMock();
        } else {
            return getAliPayBaseUrl();
        }
    }

    @DisconfFileItem(name = "open.mock")
    public String getOpenMock() {
        return openMock;
    }

    public void setOpenMock(String openMock) {
        this.openMock = openMock;
    }

    public boolean isMock(HashMap<String, String> header) {
        return header != null
                && "1".equals(header.get("Mock"))
                && "true".equals(getOpenMock());
    }

    /**
     * @return the ymtPaymentBaseUrl
     */
    @DisconfFileItem(name = "ymt.payment.baseurl")
    public String getYmtPaymentBaseUrl() {
        return ymtPaymentBaseUrl;
    }

    /**
     * @param ymtPaymentBaseUrl the ymtPaymentBaseUrl to set
     */
    public void setYmtPaymentBaseUrl(String ymtPaymentBaseUrl) {
        this.ymtPaymentBaseUrl = ymtPaymentBaseUrl;
    }
}

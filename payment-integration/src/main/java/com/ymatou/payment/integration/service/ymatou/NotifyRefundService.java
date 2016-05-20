/*
 * (C) Copyright 2016 Ymatou (http://www.ymatou.com/). All rights reserved.
 */
package com.ymatou.payment.integration.service.ymatou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ymatou.payment.integration.IntegrationConfig;
import com.ymatou.payment.integration.common.HttpClientUtil;

/**
 * 通知退款服务
 * 
 * @author qianmin 2016年5月10日 下午4:13:29
 *
 */
@Component
public class NotifyRefundService implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(NotifyRefundService.class);

    private CloseableHttpAsyncClient httpClient;

    @Autowired
    private IntegrationConfig integrationConfig;

    /**
     * 通知退款
     * 
     * @param paymentId
     * @param traceId
     * @param header
     * @return
     * @throws Exception
     */
    public void doService(String paymentId, String traceId, HashMap<String, String> header) throws Exception {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("PaymentId", paymentId));
            params.add(new BasicNameValuePair("TraceId", traceId));
            String result = HttpClientUtil.sendPost(integrationConfig.getYmtNotifyRefundUrl(header),
                    params, header, httpClient);
            if (!"OK".equalsIgnoreCase(result)) {
                logger.info("refund compensate call failed on {0},{1}", paymentId, result);
            }
        } catch (Exception e) {
            logger.error("refund compensate call failed on {0}", paymentId, e);
            throw e;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
    }
}

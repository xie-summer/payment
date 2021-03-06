/*
 * (C) Copyright 2016 Ymatou (http://www.ymatou.com/). All rights reserved.
 */
package com.ymatou.payment.domain.refund.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ymatou.payment.facade.constants.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.ymatou.payment.domain.channel.service.AcquireRefundService;
import com.ymatou.payment.domain.channel.service.RefundQueryService;
import com.ymatou.payment.domain.channel.service.acquirerefund.RefundServiceFactory;
import com.ymatou.payment.domain.channel.service.refundquery.RefundQueryServiceFactory;
import com.ymatou.payment.domain.pay.model.BussinessOrder;
import com.ymatou.payment.domain.pay.model.Payment;
import com.ymatou.payment.domain.refund.repository.RefundPository;
import com.ymatou.payment.infrastructure.Money;
import com.ymatou.payment.infrastructure.db.mapper.AccountingLogMapper;
import com.ymatou.payment.infrastructure.db.model.AccountingLogPo;
import com.ymatou.payment.infrastructure.db.model.PaymentPo;
import com.ymatou.payment.infrastructure.db.model.RefundRequestPo;
import com.ymatou.payment.integration.model.AccountingItem;
import com.ymatou.payment.integration.model.AccountingRequest;
import com.ymatou.payment.integration.model.AccountingResponse;
import com.ymatou.payment.integration.model.RefundCallbackRequest;
import com.ymatou.payment.integration.service.ymatou.AccountService;
import com.ymatou.payment.integration.service.ymatou.RefundCallbackService;

/**
 * 
 * @author qianmin 2016年6月8日 上午10:48:21
 *
 */
@Component
public class RefundJobServiceImpl implements RefundJobService {

    private static final Logger logger = LoggerFactory.getLogger(RefundJobServiceImpl.class);

    // 原路退回成功
    private static final int REFUND_SUCCESS_OPT_TYPE = 10;

    // 原路退回失败
    private static final int REFUND_FAIL_OPT_TYPE = 20;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountingLogMapper accountingLogMapper;

    @Autowired
    private RefundPository refundPository;

    @Autowired
    private RefundServiceFactory refundServiceFactory;

    @Autowired
    private RefundQueryServiceFactory refundQueryServiceFactory;

    @Autowired
    private RefundCallbackService refundCallbackService;

    @Override
    public boolean isContinueExecute(RefundRequestPo refundRequest) {
        // refundRequest=null;approveStatus=0;refundStatus=4,5,6,-2;softDelete;不再执行退款
        boolean isContinue = false;
        if (refundRequest == null) {
            logger.info("RefundRequest not exist.");
        } else if (refundRequest.getApproveStatus().equals(ApproveStatusEnum.NOT_APPROVED.getCode())
                || refundRequest.getSoftDeleteFlag()) {
            logger.info("RefundRequest can not be excuted. ApproveStatus:{}, SoftDeleteFlag:{}",
                    refundRequest.getApproveStatus(), refundRequest.getSoftDeleteFlag());
        } else if (refundRequest.getRefundStatus().equals(RefundStatusEnum.COMPLETE_FAILED.getCode())
                || refundRequest.getRefundStatus().compareTo((RefundStatusEnum.COMPLETE_SUCCESS.getCode())) >= 0) {
            logger.info("RefundRequest completed. RefundStatus: {}", refundRequest.getRefundStatus());
        } else {
            isContinue = true;
        }

        return isContinue;
    }

    @Override
    public void updateRetryCount(Integer refundId) {
        refundPository.updateRetryCount(refundId);
    }

    @Override
    public RefundRequestPo getRefundRequestByRefundId(Integer refundId) {
        RefundRequestPo refundRequest = refundPository.getRefundRequestByRefundId(refundId);
        logger.info("the refund will be excuted. {}", JSONObject.toJSONString(refundRequest));
        return refundRequest;
    }

    @Override
    public RefundStatusEnum submitRefund(RefundRequestPo refundRequest, Payment payment,
            HashMap<String, String> header) {
        String refundBatchNo = StringUtils.isBlank(refundRequest.getRefundBatchNo())
                ? refundPository.generateRefundBatchNo() : refundRequest.getRefundBatchNo();
        refundRequest.setRefundBatchNo(refundBatchNo);
        refundPository.updateRefundBatchNoByRefundId(refundBatchNo, refundRequest.getRefundId()); // 提交第三方退款前，添加RefundBatchNo

        PayTypeEnum payType = payment.getPayType();
        AcquireRefundService refundService = refundServiceFactory.getInstanceByPayType(payType);
        return refundService.notifyRefund(refundRequest, payment, header);
    }

    @Override
    public RefundStatusEnum queryRefund(RefundRequestPo refundRequest, Payment payment,
            HashMap<String, String> header) {
        RefundQueryService refundQueryService = refundQueryServiceFactory.getInstanceByPayType(payment.getPayType());
        RefundStatusEnum refundStatus = refundQueryService.queryRefund(refundRequest, payment, header);

        return refundStatus;
    }

    @Override
    public AccountingStatusEnum dedcutBalance(Payment payment, BussinessOrder bussinessOrder,
            RefundRequestPo refundRequest, HashMap<String, String> header) {
        AccountingResponse accountingResponse = dedcutBalanceOnce(payment, bussinessOrder, refundRequest, header);
        if (AccountingStatusEnum.UNKNOW.equals(accountingResponse.getAccountingStatus())) { // 发送异常重试
            accountingResponse = dedcutBalanceOnce(payment, bussinessOrder, refundRequest, header);
        }

        if (AccountingResponse.BALANCE_LIMIT.equals(accountingResponse.getStatusCode())) {
            logger.error("dedcut user balance failed, stop refund. RefundId: {}", refundRequest.getRefundId());
            refundPository.softDeleteRefundRequest(refundRequest.getRefundId()); // 逻辑删除refundRequest
        }

        return accountingResponse.getAccountingStatus();
    }

    private AccountingResponse dedcutBalanceOnce(Payment payment, BussinessOrder bussinessOrder,
            RefundRequestPo refundRequest, HashMap<String, String> header) {
        AccountingRequest request = generateRequest(refundRequest, payment, bussinessOrder);
        AccountingResponse response = accountService.accounting(request, header);
        saveAccoutingLog(bussinessOrder, refundRequest, response, request);
        logger.info("accounting result. StatusCode:{}, Message:{}", response.getStatusCode(),
                response.getMessage());

        return response;
    }

    private void saveAccoutingLog(BussinessOrder bussinessOrder, RefundRequestPo refundRequest,
            AccountingResponse response, AccountingRequest accountingRequest) {
        AccountingLogPo log = new AccountingLogPo();
        log.setCreatedTime(new Date());
        log.setAccoutingAmt(new Money(accountingRequest.getAccountingItems().get(0).getAmount()).getAmount());
        log.setAccountingType("Refund");
        log.setUserId((long) bussinessOrder.getUserId().intValue());
        log.setBizNo(String.valueOf(refundRequest.getRefundId()));
        log.setMemo("快速退款");
        log.setRespCode(response.getStatusCode());
        log.setRespMsg(response.getMessage());
        log.setStatus(response.isAccoutingSuccess()); // 成功为1，失败为0
        accountingLogMapper.insertSelective(log);

        refundPository.updateRefundRequestAccoutingStatus(refundRequest.getRefundId(), log.getStatus());
    }

    private AccountingRequest generateRequest(RefundRequestPo refundRequest, Payment payment,
            BussinessOrder bussinessOrder) {
        List<AccountingItem> itemList = new ArrayList<>();
        AccountingItem item = new AccountingItem();
        item.setUserId(bussinessOrder.getUserId());
        item.setCurrencyType(CurrencyTypeEnum.CNY.code());

        // 由于存在优惠金额，当招行支付 140 ，优惠10，码头余额入账150 发生快速退款
        // 此时支付网关 应该退用户 140， 码头余额出账 150
        // 如果发生部分的快速退款 130， 码头余额出账 (130 / 140) * 10 + 130
        BigDecimal refundAmount = refundRequest.getRefundAmount();
        BigDecimal actualAmount = payment.getActualPayPrice().getAmount();
        BigDecimal discountAmount = payment.getDiscountAmt().getAmount();
        BigDecimal accountAmount = refundAmount.divide(actualAmount, MathContext.DECIMAL64).multiply(discountAmount)
                .add(refundAmount).setScale(2, RoundingMode.DOWN);

        item.setAmount(accountAmount.toString());
        item.setAccountOperateType(AccountOperateTypeEnum.Fundout.code());
        item.setAccountType(AccountTypeEnum.RmbAccount.code());
        item.setAccountingDate(new Date());
        item.setBizCode("300017"); // 快速退款
        item.setBizNo(String.valueOf(refundRequest.getRefundId()));
        item.setOriginalNo(bussinessOrder.getOrderId());
        item.setMemo("快速退款");
        itemList.add(item);

        AccountingRequest request = new AccountingRequest();
        request.setAccountingItems(itemList);
        request.setAppId("payment.ymatou.com");
        return request;
    }

    public void updateRefundRequestAndPayment(RefundRequestPo refundRequest, Payment payment,
            RefundStatusEnum refundStatus) {
        // 赋值需要更新的字段 退款单
        RefundRequestPo refundRequestPo = new RefundRequestPo();
        refundRequestPo.setRefundTime(new Date());
        refundRequestPo.setRefundId(refundRequest.getRefundId());
        refundRequestPo.setRefundBatchNo(refundRequest.getRefundBatchNo());
        refundRequestPo.setRefundStatus(refundStatus.getCode());

        // 赋值需要更新的字段 支付单
        PaymentPo paymentPo = new PaymentPo();
        paymentPo.setPaymentId(payment.getPaymentId());

        if (RefundStatusEnum.THIRDPART_REFUND_SUCCESS.equals(refundStatus)) { // 更新退款完成金额
            BigDecimal refundAmt = payment.getCompletedRefundAmt() == null ? refundRequest.getRefundAmount()
                    : refundRequest.getRefundAmount().add(payment.getCompletedRefundAmt());
            paymentPo.setPayStatus(PayStatusEnum.Refunded.getIndex());
            paymentPo.setCompletedRefundAmt(refundAmt);

        } else if (RefundStatusEnum.COMPLETE_FAILED.equals(refundStatus)) { // 更新退款申请金额
            BigDecimal refundAmt = payment.getRefundAmt().subtract(refundRequest.getRefundAmount());
            paymentPo.setRefundAmt(refundAmt);

        } else if(RefundStatusEnum.INIT.equals(refundStatus)){ // 更新退款批次号, 只有支付宝渠道的退款需要更新退款批次号
            ChannelTypeEnum channelType = PayTypeEnum.getChannelType(refundRequest.getPayType());
            if(ChannelTypeEnum.AliPay.equals(channelType))
            {
                updateRefundBatchNo(refundRequestPo);
            }
        }

        refundPository.updateRefundRequestAndPayment(refundRequestPo, paymentPo);
    }

    /**
     * 更新退款批次号（只有支付宝对退款批次号有时间上的要求）
     *
     * 如果跨天还需要把退款批次号设置为当天，避免造成无法提交
     * is_success=T&result_details=201704070000978622^2017040721001004570278175064^88.00^SELLER_BALANCE_NOT_ENOUGH^false^null
     * is_success=T&result_details=201704129000978622^2017040721001004570278175064^88.00^SUCCESS^false^null
     * @param refundRequest
     */
    private void updateRefundBatchNo(RefundRequestPo refundRequest){
        SimpleDateFormat simpleDateFormat= new SimpleDateFormat("yyyyMMdd");
        String today = simpleDateFormat.format(new Date());

        // 如果退款批次号的前八位不是当天，则需要将退款批次号更换为当天
        if(!today.equals(StringUtils.left(refundRequest.getRefundBatchNo(), 8))){
            // + "9" 的目的是避免和当天的退款单号发生重复
            String newRefundBatchNo = today + "9" + StringUtils.substring(refundRequest.getRefundBatchNo(), 9);
            refundRequest.setRefundBatchNo(newRefundBatchNo);
        }
    }

    @Override
    public boolean callbackTradingSystem(RefundRequestPo refundRequest, Payment payment, Boolean refundSuccess,
            HashMap<String, String> header) {
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setActualRefundAmount(refundRequest.getRefundAmount());
        request.setAuditor(refundRequest.getApprovedUser());
        request.setOptType(Boolean.TRUE.equals(refundSuccess) ? REFUND_SUCCESS_OPT_TYPE : REFUND_FAIL_OPT_TYPE);
        request.setOrderID(Long.parseLong(refundRequest.getOrderId()));

        // 避免出现NULL的现象
        request.setPassAuditTime(refundRequest.getRefundTime() == null ? new Date() : refundRequest.getRefundTime());

        request.setRequiredRefundAmount(refundRequest.getRefundAmount());
        request.setThirdPartyName(PayTypeEnum.getThirdPartyName(payment.getPayType()));
        request.setThirdPartyTradingNo(refundRequest.getInstPaymentId());
        request.setTradeNo(refundRequest.getTradeNo());
        request.setIsFastRefund(refundRequest.getApproveStatus().equals(ApproveStatusEnum.FAST_REFUND.getCode()));
        boolean isNewSystem = refundRequest.getTraceId().length() == 24; // Java版交易系统RequestNo长度
        if (isNewSystem) {
            request.setRequestNo(refundRequest.getTraceId());
        }

        try {
            return refundCallbackService.doService(request, isNewSystem, header);
        } catch (IOException e) {
            logger.error("refund notify to trade service failed on {}", refundRequest.getRefundBatchNo());
            return false;
        }
    }

    @Override
    public void updateRefundRequestToCompletedSuccess(RefundRequestPo refundRequest) {
        RefundRequestPo refundRequestPo = new RefundRequestPo();
        refundRequestPo.setRefundTime(new Date());
        refundRequestPo.setRefundBatchNo(refundRequest.getRefundBatchNo());
        refundRequestPo.setRefundStatus(RefundStatusEnum.COMPLETE_SUCCESS.getCode());

        refundPository.updateRefundRequest(refundRequestPo);
    }

    @Override
    public void updateRefundRequestToReturnToTrading(RefundRequestPo refundRequest) {
        RefundRequestPo refundRequestPo = new RefundRequestPo();
        refundRequestPo.setRefundTime(new Date());
        refundRequestPo.setRefundBatchNo(refundRequest.getRefundBatchNo());
        refundRequestPo.setRefundStatus(RefundStatusEnum.RETURN_TRANSACTION.getCode());

        refundPository.updateRefundRequest(refundRequestPo);
    }
}

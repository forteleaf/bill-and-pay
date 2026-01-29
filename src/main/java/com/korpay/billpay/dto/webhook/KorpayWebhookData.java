package com.korpay.billpay.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KorpayWebhookData {

    private String tid;
    private String otid;
    private String mid;
    private String catId;
    private String connCd;
    private String ediNo;

    private String ordNo;
    private Long amt;
    private Long remainAmt;
    private String payMethod;
    private String goodsName;

    private String cardNo;
    private String appNo;
    private String quota;
    private String appCardCd;
    private String acqCardCd;
    private String fnNm;

    private String ordNm;
    private String buyerId;

    private String cancelYN;
    private String appDtm;
    private String ccDnt;

    private String notiDnt;
    private String tPhone;
    private String usePointAmt;
    private String vacntNo;
    private String socHpNo;
    private String charSet;
    private String hashStr;
    private String ediDate;
    private String resultCd;
    private String cashCrctFlg;
}

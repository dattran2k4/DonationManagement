package com.chiaseyeuthuong.service;

public interface MailService {

    void sendVerificationCodeMail(String to);

    void sendVerificationCodeMailAsync(String to);

    boolean verifyLookupCode(String email, String code);

    void sendDonationThankYouMailAsync(String to, String donorName, String memoCode, String targetTitle, String amountText);
}

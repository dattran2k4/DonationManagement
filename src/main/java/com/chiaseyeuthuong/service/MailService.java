package com.chiaseyeuthuong.service;

public interface MailService {

    String generateVerificationCode();

    void sendVerificationCodeMail(String to);

    void sendVerificationCodeMailAsync(String to);

    boolean verifyLookupCode(String email, String code);
}

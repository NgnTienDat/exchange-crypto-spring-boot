package com.ntd.exchange_crypto.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${app.mail.host}")
    private String mailHost;

    @Value("${app.mail.port}")
    private int mailPort;

    @Value("${app.mail.username}")
    private String mailUsername;

    @Value("${app.mail.password}")
    private String mailPassword;


    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);

        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

//        mailSender.setHost("smtp.gmail.com"); // Change to your SMTP server
//        mailSender.setPort(587);
//
//        mailSender.setUsername("dat.nt334@gmail.com");
//        mailSender.setPassword("wwdp fmvg pcdi qlfw");


        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true"); // Remove in production

        return mailSender;
    }
}
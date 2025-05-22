import logger.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author BingoCAT
 * @author Xu_Huawei
 */
public class EmailVerificationUtils {

    // 全局配置变量
    private static final String FROM_EMAIL = "wanght2024@mail.sustech.edu.cn";
    private static final String PASSWORD = "5113683xyz@A";
    private static final String HOST = "smtp.exmail.qq.com";
    private static final int PORT = 465;
    private static final int CODE_LENGTH = 6; // 验证码长度
    private static final long EXPIRATION_TIME = 10 * 60 * 1000; // 10分钟有效期

    // 存储验证码及其过期时间 (邮箱 -> [验证码, 过期时间])
    private static final Map<String, Object[]> verificationCodes = new ConcurrentHashMap<>();

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @throws MessagingException 邮件发送异常
     */
    public static void sendVerificationCode(String toEmail)
    {
        // 生成验证码
        String code = generateVerificationCode();
        String subject = "[KlotskiPuzzle] Verification Code";
        String content = buildVerificationEmailContent(code);

        // 发送邮件
        sendEmail(toEmail, subject, content);

        // 存储验证码及过期时间
        long expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
        verificationCodes.put(toEmail, new Object[]{code, expirationTime});
    }

    /**
     * 验证用户输入的验证码
     * @param toEmail 用户邮箱
     * @param inputCode 用户输入的验证码
     * @return 验证结果
     */
    public static boolean verifyCode(String toEmail, String inputCode)
    {
        Object[] codeInfo = verificationCodes.get(toEmail);
        if (codeInfo == null)
        {
            return false;
        }

        String storedCode = (String) codeInfo[0];
        long expirationTime = (Long) codeInfo[1];

        // 检查是否过期
        if (System.currentTimeMillis() > expirationTime)
        {
            verificationCodes.remove(toEmail);
            return false;
        }

        // 验证验证码
        return storedCode.equals(inputCode);
    }

    /**
     * 生成随机验证码
     */
    private static String generateVerificationCode()
    {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++)
        {
         code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 构建包含验证码的邮件内容
     */
    private static String buildVerificationEmailContent(String code)
    {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;\">" +
                "<h2 style=\"color: #333;\">Verification Code</h2>" +
                "<p style=\"font-size: 16px;\">尊敬的用户，您好：</p>" +
                "<p style=\"font-size: 16px;\">您正在进行身份验证，验证码为：</p>" +
                "<div style=\"background-color: #f5f5f5; padding: 15px; text-align: center; margin: 20px 0;\">" +
                "<span style=\"font-size: 24px; font-weight: bold; color: #e74c3c;\">" + code + "</span>" +
                "</div>" +
                "<p style=\"font-size: 14px; color: #666;\">该验证码10分钟内有效，请及时完成验证。</p>" +
                "<p style=\"font-size: 14px; color: #666;\">如果您未请求此验证码，请忽略此邮件。</p>" +
                "</div>";
    }

    /**
     * 发送邮件方法
     */
    private static void sendEmail(String toEmail, String subject, String content)
    {
        // 配置邮件服务器属性
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", PORT);
        properties.put("mail.smtp.ssl.enable", "true");  // 启用SSL


        // 创建认证信息
        Authenticator auth = new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        };

        // 创建会话
        Session session = Session.getInstance(properties, auth);

        try
        {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
        }
        catch (MessagingException e)
        {
            verificationCodes.remove(toEmail); // 发送失败时清除验证码
            Logger.warning("Failed to send verification email to " + toEmail);
        }
    }
    private static final ExecutorService emailExecutor = Executors.newSingleThreadExecutor();

    public static void sendVerificationCodeAsync(String toEmail) {
        emailExecutor.submit(() -> {
            try {
                sendVerificationCode(toEmail);
                Logger.info("Verification email sent successfully to " + toEmail);
            } catch (Exception e) {
                Logger.error("Failed to send verification email: " + e.getMessage());
            }
        });
    }
}    
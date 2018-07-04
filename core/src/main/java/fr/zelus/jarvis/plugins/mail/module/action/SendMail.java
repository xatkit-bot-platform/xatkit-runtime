package fr.zelus.jarvis.plugins.mail.module.action;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.plugins.mail.module.MailModule;
import fr.zelus.jarvis.utils.MessageUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

public class SendMail extends JarvisAction<MailModule> {

    private String to;
    private String title;
    private String body;

    public SendMail(MailModule containingModule, JarvisContext context, String to, String title, String body) {
        super(containingModule, context);
        this.to = to;
        this.title = title;
        this.body = body;
    }

    @Override
    public Object call() {
        try {
            MimeMessage email = createEmail(to, "jarvis.bot.dev@gmail.com", title, MessageUtils.fillContextValues
                    (body, this.context));
            sendMessage(this.module.getService(), "me", email);
        } catch (MessagingException | IOException e) {
            String errorMessage = MessageFormat.format("An error occurred when sending the email to {0}", to);
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        return null;
    }

    private Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws MessagingException,
            IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();
        return message;
    }

    public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws
            MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}

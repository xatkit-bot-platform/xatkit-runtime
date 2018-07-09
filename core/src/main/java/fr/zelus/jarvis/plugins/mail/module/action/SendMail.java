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

/**
 * A {@link JarvisAction} that sends an email to the provided {@code address} with the given {@code body} and {@code
 * title}.
 * <p>
 * This class relies on the {@link MailModule}'s {@link Gmail} service to connect to the corresponding Gmail account
 * and send emails.
 *
 * @see MailModule
 */
public class SendMail extends JarvisAction<MailModule> {

    /**
     * The address to send the email to.
     */
    private String to;

    /**
     * The title of the email to send.
     */
    private String title;

    /**
     * The content of the email to send.
     */
    private String body;

    /**
     * Constructs a new {@link SendMail} with the provided {@code containingModule}, {@code context}, {@code to},
     * {@code title}, and {@code body}.
     *
     * @param containingModule the {@link MailModule} containing this action
     * @param context          the {@link JarvisContext} associated to this action
     * @param to               the address to send the email to
     * @param title            the title of the email to send
     * @param body             the content of the email to send
     * @throws NullPointerException if the provided {@code containingModule} or {@code context} is {@code null}
     */
    public SendMail(MailModule containingModule, JarvisContext context, String to, String title, String body) {
        super(containingModule, context);
        this.to = to;
        this.title = title;
        this.body = body;
    }

    /**
     * Sends an email to the provided {@code to} address with the given {@code title} and {@code body}.
     * <p>
     * This method relies on the containing {@link MailModule}'s {@link Gmail} service to authenticate the bot and
     * send emails.
     *
     * @return {@code null}
     */
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

    /**
     * Creates a {@link MimeMessage} from the given {@code to}, {@code from}, {@code subject}, and {@code bodyText}.
     *
     * @param to       the address to send the email to
     * @param from     the bot address
     * @param subject  the title of the email to send
     * @param bodyText the content of the email to send
     * @return the created {@link MimeMessage}
     * @throws MessagingException if an error occurred when building the {@link MimeMessage}
     */
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

    /**
     * Creates a {@link Gmail} compatible {@link Message} from the provided {@code emailContent}.
     *
     * @param emailContent the {@link MimeMessage} to construct a {@link Gmail} {@link Message} from
     * @return the created {@link Message}
     * @throws MessagingException if an error occurred when building the {@link Message}
     * @throws IOException        if an error occurred when building the {@link Message}
     */
    public static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Sends the provided {@code emailContent} using the given {@link Gmail} service.
     *
     * @param service      the {@link Gmail} service used to send the email
     * @param userId       the identifier used to connect to the {@link Gmail} service
     * @param emailContent the {@link MimeMessage} to send
     * @return the send {@link Message}
     * @throws MessagingException if an error occurred when sending the email
     * @throws IOException        if an error occurred when sending the email
     */
    private Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws MessagingException,
            IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();
        return message;
    }

}

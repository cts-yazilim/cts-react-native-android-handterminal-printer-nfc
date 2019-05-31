package com.handterminalprinternfc;

import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import android.os.Environment;

public class GMailSender extends javax.mail.Authenticator {

	private String mailhost = "smtp.yandex.com";

	private String user;

	private String password;

	private Session session;

	static {

		Security.addProvider(new JSSEProvider());

	}

	public GMailSender(String user, String password) {

		this.user = user;

		this.password = password;

		Properties props = new Properties();

		props.setProperty("mail.transport.protocol", "smtps");

		props.setProperty("mail.host", mailhost);

		props.put("mail.smtp.auth", "true");

		props.put("mail.smtp.port", "465");

		props.put("mail.smtp.socketFactory.port", "465");

		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

		props.put("mail.smtp.socketFactory.fallback", "false");

		props.setProperty("mail.smtp.quitwait", "false");

		session = Session.getDefaultInstance(props, this);

	}

	protected PasswordAuthentication getPasswordAuthentication() {

		return new PasswordAuthentication(user, password);

	}

	public synchronized void sendMail(String subject, String body, String file,

			String sender, String recipients) throws Exception {

		MimeMessage message = new MimeMessage(session);

		DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));

		message.setSender(new InternetAddress(sender));

		message.setSubject(subject);
		message.setText(body);
		message.setDataHandler(handler);

		if (recipients.indexOf(',') > 0)
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
		else
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));

		Multipart multipart = new MimeMultipart();
		MimeBodyPart subjectBody = new MimeBodyPart();
		subjectBody.setText(body);
		subjectBody.setContent(body, "text/html");

		multipart.addBodyPart(subjectBody);

		if (file != "") {
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart = new MimeBodyPart();
			String fileName = "backup.zip";
			DataSource source = new FileDataSource(file);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(fileName);
			multipart.addBodyPart(messageBodyPart);
		}

		message.setContent(multipart);

		System.out.println("Sending");
		Transport.send(message);
		System.out.println("OK");

		// Message message = new MimeMessage(session);
		// //message.setFrom(new InternetAddress("selcuk.aksar@cts.com.tr"));
		// message.setRecipient(Message.RecipientType.TO, new
		// InternetAddress(recipients));
		// message.setSubject("Testing Subject");
		// message.setText("PFA");
		//
		// MimeBodyPart messageBodyPart = new MimeBodyPart();

		// Multipart multipart = new MimeMultipart();
		//
		// messageBodyPart = new MimeBodyPart();
		// String file = Environment.getExternalStorageDirectory() +
		// "/database_copy.db";
		// String fileName = "database_copy";
		// DataSource source = new FileDataSource(file);
		// messageBodyPart.setDataHandler(new DataHandler(source));
		// messageBodyPart.setFileName(fileName);
		// multipart.addBodyPart(messageBodyPart);
		//
		// message.setContent(multipart);
		//
		// System.out.println("Sending");
		//
		// Transport.send(message);
		//
		// System.out.println("Done");

		// MimeMessage message = new MimeMessage(session);
		//
		// DataHandler handler = new DataHandler(new
		// ByteArrayDataSource(body.getBytes(), "text/plain"));
		//
		// message.setSender(new InternetAddress(sender));
		//
		// message.setSubject(subject);
		//
		// message.setDataHandler(handler);
		//
		//
		//
		// if (recipients.indexOf(',') > 0)
		//
		// message.setRecipients(Message.RecipientType.TO,
		// InternetAddress.parse(recipients));
		//
		// else
		//
		// message.setRecipient(Message.RecipientType.TO, new
		// InternetAddress(recipients));
		//
		//
		//
		// Transport.send(message);

	}

}
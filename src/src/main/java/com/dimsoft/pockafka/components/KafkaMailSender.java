package com.dimsoft.pockafka.components;

import com.dimsoft.pockafka.beans.Mail;
import com.dimsoft.pockafka.utils.Topics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class KafkaMailSender {

	private final Logger LOG = LoggerFactory.getLogger(KafkaMailSender.class);
	private KafkaTemplate<String, Mail> mailKafkaTemplate;

	@Autowired
	public KafkaMailSender(KafkaTemplate<String, Mail> mailKafkaTemplate) {
		this.mailKafkaTemplate = mailKafkaTemplate;
	}

	public void sendMail(Mail mail, String topicName) {
		LOG.info("Sending mail {} to kafka in topic {} !", mail, topicName);
		LOG.info("--------------------------------");

		mailKafkaTemplate.send(topicName, mail);
	}

    public void sendMail(Mail mail) {
		this.sendMail(mail, Topics.MAIL_TOPIC);
	}

	public void sendMailWithCallback(Mail mail, String topicName) {
		LOG.info("Sending mail {} to kafka in topic {}, with callback !", mail, topicName);
		LOG.info("---------------------------------");

		ListenableFuture<SendResult<String, Mail>> future = mailKafkaTemplate.send(topicName, mail);

		future.addCallback(new ListenableFutureCallback<SendResult<String, Mail>>() {
			@Override
			public void onSuccess(SendResult<String, Mail> result) {
				LOG.info("Success Callback : [{}] delivered with offset - {}", mail,
						result.getRecordMetadata().offset());
			}

			@Override
			public void onFailure(Throwable ex) {
				LOG.warn("Failure Callback: Unable to deliver message [{}]. {}", mail, ex.getMessage());
			}
		});
	}

    public void sendMailWithCallback(Mail mail) {
        this.sendMailWithCallback(mail, Topics.MAIL_TOPIC);
    }
}
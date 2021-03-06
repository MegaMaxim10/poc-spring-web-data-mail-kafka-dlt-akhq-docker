package com.dimsoft.pockafka.components;

import com.dimsoft.pockafka.beans.Mail;
import com.dimsoft.pockafka.repository.MailRepository;
import com.dimsoft.pockafka.services.MailService;
import com.dimsoft.pockafka.utils.Topics;

import java.lang.Exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class KafkaMailProcessor {
    private final Logger LOG = LoggerFactory.getLogger(KafkaMailProcessor.class);
    @Autowired
    private MailService mailService;
    @Autowired
    private MailRepository mailRepository;

    @RetryableTopic(
      attempts = "3",
      backoff = @Backoff(delay = 100000, multiplier = 2.0),
      autoCreateTopics = "true",
	  kafkaTemplate = "mailKafkaTemplate",
      topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = Topics.MAIL_TOPIC, groupId = Topics.MAIL_GROUP, containerFactory = "mailKafkaListenerContainerFactory")
	public void mailListener(Mail mail) throws Exception {
		LOG.info("Kafka mail listener [{}]", mail);
        LOG.info("Spring will now try to send the email [{}]", mail);
		try {
			mailService.sendSimpleMail(mail);
		} catch(Exception e) {
			LOG.info("An exception was caught");
			e.printStackTrace();
			throw new Exception(e);
		}
        LOG.info("Spring will now try to update the previous email status to 1");
        mail.setMailStatus((short)1);
        mailRepository.save(mail);
	}

    @DltHandler
    public void dlt(Mail mail, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        LOG.info("Received mail {} from topic {} in DLT.", mail, topic);
    }
}

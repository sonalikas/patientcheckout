package com.sonalika.aws.lambda.s3sns;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.slf4j.Logger;

import java.util.List;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PatientCheckoutLambda {
	
	private static final String PATIENT_CHECKOUT_TOPIC = System.getenv("PATIENT_CHECKOUT_TOPIC");
	private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final AmazonSNS sns = AmazonSNSClientBuilder.defaultClient();
	
	public void handler(S3Event event, Context context) {
		//LambdaLogger logger = context.getLogger();
		
		org.slf4j.Logger logger = LoggerFactory.getLogger(PatientCheckoutLambda.class);
		event.getRecords().forEach(record -> {
			S3ObjectInputStream s3inputStream = s3
					.getObject(record.getS3().getBucket().getName(), record.getS3().getObject().getKey())
					.getObjectContent();
			try {
				logger.info("Reading data from s3");
				List<PatientCheckoutEvent> patientCheckoutEvents = Arrays
						.asList(objectMapper.readValue(s3inputStream, PatientCheckoutEvent[].class));
				System.out.println(patientCheckoutEvents);
				s3inputStream.close();
				logger.info("Message being publish to sns");
				publishMessageToSNS(patientCheckoutEvents);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			catch(JsonMappingException e) {
//				logger.error("Exception is:", e);
//				throw RuntimeException ();
//			}
//			catch(JsonMappingException e) {
//				StringWriter stringWriter = new StringWriter();
//				e.printStackTrace(new PrintWriter(new StringWriter()));
//				logger.log(stringWriter.toString());
//			}
		});
	}
	private void publishMessageToSNS(List<PatientCheckoutEvent> patientCheckoutEvents) {
		patientCheckoutEvents.forEach(checkoutEvent->{
			try {
				sns.publish(PATIENT_CHECKOUT_TOPIC,
						objectMapper.writeValueAsString(checkoutEvent));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
}

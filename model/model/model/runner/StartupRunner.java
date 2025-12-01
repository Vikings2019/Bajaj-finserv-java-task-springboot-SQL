package com.example.bajaj.runner;

import com.example.bajaj.model.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class);

    @Value("${hiring.api.generate.url}")
    private String generateUrl;

    @Value("${hiring.api.submit.url}")
    private String submitUrl;

    @Value("${hiring.payload.name}")
    private String name;

    @Value("${hiring.payload.regNo}")
    private String regNo;

    @Value("${hiring.payload.email}")
    private String email;

    @Value("${hiring.final.query:}")
    private String finalQuery;

    @Value("${hiring.useBearer:true}")
    private boolean useBearer;

    private final RestTemplate restTemplate;

    @Autowired
    public StartupRunner(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Starting hiring flow: sending generateWebhook request to {}", generateUrl);

            GenerateWebhookRequest payload = new GenerateWebhookRequest(name, regNo, email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GenerateWebhookRequest> request = new HttpEntity<>(payload, headers);
            ResponseEntity<GenerateWebhookResponse> resp = restTemplate.postForEntity(generateUrl, request, GenerateWebhookResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                GenerateWebhookResponse body = resp.getBody();
                logger.info("Received response: {}", body);

                // If finalQuery is not provided, show webhook and return
                if (finalQuery == null || finalQuery.trim().isEmpty() || finalQuery.contains("PASTE_FINAL_SQL_HERE")) {
                    logger.warn("No finalQuery found in application.properties (hiring.final.query). Please paste your final SQL into application.properties before running to submit automatically.");
                    logger.info("Webhook URL returned by server: {}", body.getWebhook());
                    return;
                }

                SubmitRequest submit = new SubmitRequest(finalQuery.trim());

                HttpHeaders submitHeaders = new HttpHeaders();
                submitHeaders.setContentType(MediaType.APPLICATION_JSON);

                String token = body.getAccessToken();
                if (token != null && !token.trim().isEmpty()) {
                    String headerValue = token;
                    if (useBearer) headerValue = "Bearer " + token;
                    submitHeaders.set("Authorization", headerValue);
                } else {
                    logger.warn("No accessToken returned; continuing without Authorization header.");
                }

                HttpEntity<SubmitRequest> submitEntity = new HttpEntity<>(submit, submitHeaders);

                // Use the webhook URL from response if provided; otherwise use configured submitUrl
                String target = (body.getWebhook() != null && !body.getWebhook().trim().isEmpty()) ? body.getWebhook() : submitUrl;
                logger.info("Submitting final query to: {}", target);

                ResponseEntity<String> submitResp = restTemplate.postForEntity(target, submitEntity, String.class);
                logger.info("Submission response: {} - {}", submitResp.getStatusCode(), submitResp.getBody());
            } else {
                logger.error("generateWebhook failed: status={}, body={}", resp.getStatusCode(), resp.getBody());
            }

        } catch (Exception ex) {
            logger.error("Exception during startup hiring flow", ex);
        }
    }
}

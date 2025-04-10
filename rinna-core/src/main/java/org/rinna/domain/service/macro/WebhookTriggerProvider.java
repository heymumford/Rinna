package org.rinna.domain.service.macro;

import java.util.Map;

import org.rinna.domain.model.macro.MacroTrigger;
import org.rinna.domain.model.macro.TriggerEvent;
import org.rinna.domain.model.macro.TriggerType;

/**
 * Specialized trigger provider for webhook triggers.
 */
public interface WebhookTriggerProvider extends TriggerProvider {
    
    /**
     * Creates a TriggerEvent from a webhook request.
     *
     * @param path the webhook path
     * @param headers the request headers
     * @param body the request body
     * @param sourceIp the source IP address
     * @return a new TriggerEvent representing the webhook request
     */
    TriggerEvent createTriggerEvent(String path, Map<String, String> headers, String body, String sourceIp);
    
    /**
     * Validates authentication for a webhook request.
     *
     * @param trigger the macro trigger
     * @param headers the request headers
     * @param body the request body
     * @param sourceIp the source IP address
     * @return true if the request is authenticated
     */
    boolean validateAuthentication(MacroTrigger trigger, Map<String, String> headers, String body, String sourceIp);
    
    /**
     * Creates a JSON webhook trigger provider.
     *
     * @return a new WebhookTriggerProvider for JSON webhooks
     */
    static WebhookTriggerProvider jsonProvider() {
        return new WebhookTriggerProvider() {
            @Override
            public TriggerType getProvidedType() {
                return TriggerType.WEBHOOK_JSON;
            }
            
            @Override
            public boolean matches(TriggerEvent event, MacroTrigger trigger) {
                if (event == null || trigger == null || !trigger.isWebhookTrigger()) {
                    return false;
                }
                
                // Check if the event is a webhook event
                if (!event.getType().isWebhookTrigger()) {
                    return false;
                }
                
                // Get the webhook path from the event
                String path = (String) event.getPayloadValue("path");
                
                // Get the expected path from the trigger
                String expectedPath = (String) trigger.getConfigValue("path");
                
                // Check if the paths match
                if (path == null || expectedPath == null || !path.equals(expectedPath)) {
                    return false;
                }
                
                // If the trigger has a condition, delegate to it
                if (trigger.hasCondition()) {
                    // In a real implementation, this would evaluate the condition
                    // against the event payload
                    return true;
                }
                
                return true;
            }
            
            @Override
            public TriggerEvent createTriggerEvent(String path, Map<String, String> headers, 
                    String body, String sourceIp) {
                // Create a new trigger event
                TriggerEvent event = new TriggerEvent(TriggerType.WEBHOOK_JSON, sourceIp);
                
                // Add the webhook request details to the payload
                event.setPayloadValue("path", path);
                event.setPayloadValue("headers", headers);
                event.setPayloadValue("body", body);
                
                return event;
            }
            
            @Override
            public boolean validateAuthentication(MacroTrigger trigger, Map<String, String> headers, 
                    String body, String sourceIp) {
                // No trigger means no authentication
                if (trigger == null || trigger.getWebhookConfig() == null) {
                    return false;
                }
                
                // Check if IP filtering is enabled
                String ipAllowList = trigger.getWebhookConfig().getAuth().getIpAllowList();
                if (ipAllowList != null && !ipAllowList.isEmpty()) {
                    boolean ipAllowed = false;
                    
                    // Split the allow list by commas
                    String[] allowedIps = ipAllowList.split(",");
                    for (String allowedIp : allowedIps) {
                        allowedIp = allowedIp.trim();
                        
                        // Check if the IP matches
                        // In a real implementation, this would handle CIDR notation
                        if (allowedIp.equals(sourceIp)) {
                            ipAllowed = true;
                            break;
                        }
                    }
                    
                    if (!ipAllowed) {
                        return false;
                    }
                }
                
                // Check the authentication method
                switch (trigger.getWebhookConfig().getAuth().getMethod()) {
                    case NONE:
                        return true;
                        
                    case API_KEY:
                        // Get the header name and expected key
                        String headerName = trigger.getWebhookConfig().getAuth().getHeaderName();
                        String expectedKey = trigger.getWebhookConfig().getAuth().getSecretKey();
                        
                        // Check if the header is present and matches the expected key
                        String actualKey = headers.get(headerName);
                        return actualKey != null && actualKey.equals(expectedKey);
                        
                    case HMAC_SHA256:
                        // Get the header name and expected signature
                        String signatureHeader = trigger.getWebhookConfig().getAuth().getHeaderName();
                        String secretKey = trigger.getWebhookConfig().getAuth().getSecretKey();
                        
                        // Get the actual signature
                        String signature = headers.get(signatureHeader);
                        
                        // In a real implementation, this would compute the expected signature
                        // using HMAC-SHA256 and compare it with the actual signature
                        return signature != null && !signature.isEmpty();
                        
                    case BASIC_AUTH:
                        // Get the Authorization header
                        String authHeader = headers.get("Authorization");
                        
                        // In a real implementation, this would decode the basic auth header
                        // and compare the username and password
                        return authHeader != null && authHeader.startsWith("Basic ");
                        
                    case BEARER_TOKEN:
                        // Get the Authorization header
                        String bearerHeader = headers.get("Authorization");
                        String expectedToken = trigger.getWebhookConfig().getAuth().getSecretKey();
                        
                        // Check if the header is present and matches the expected token
                        return bearerHeader != null && 
                               bearerHeader.startsWith("Bearer ") && 
                               bearerHeader.substring(7).equals(expectedToken);
                        
                    case CUSTOM_HEADER:
                        // Get the custom header name and expected value
                        String customHeader = trigger.getWebhookConfig().getAuth().getHeaderName();
                        String expectedValue = trigger.getWebhookConfig().getAuth().getSecretKey();
                        
                        // Check if the header is present and matches the expected value
                        String actualValue = headers.get(customHeader);
                        return actualValue != null && actualValue.equals(expectedValue);
                        
                    default:
                        return false;
                }
            }
        };
    }
}
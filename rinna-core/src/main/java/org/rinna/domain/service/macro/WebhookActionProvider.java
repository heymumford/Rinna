package org.rinna.domain.service.macro;

import org.rinna.domain.model.macro.ActionResult;
import org.rinna.domain.model.macro.ActionType;
import org.rinna.domain.model.macro.ExecutionContext;
import org.rinna.domain.model.macro.MacroAction;

/**
 * Specialized action provider for webhook actions.
 */
public interface WebhookActionProvider extends ActionProvider {
    
    /**
     * Validates that the webhook URL is acceptable.
     *
     * @param url the webhook URL to validate
     * @return true if the URL is acceptable
     */
    boolean validateWebhookUrl(String url);
    
    /**
     * Gets the headers for a webhook call.
     *
     * @param action the macro action
     * @param context the execution context
     * @return the headers for the webhook call
     */
    java.util.Map<String, String> getWebhookHeaders(MacroAction action, ExecutionContext context);
    
    /**
     * Gets the body for a webhook call.
     *
     * @param action the macro action
     * @param context the execution context
     * @return the body for the webhook call
     */
    String getWebhookBody(MacroAction action, ExecutionContext context);
    
    /**
     * Gets the HTTP method for a webhook call.
     *
     * @param action the macro action
     * @return the HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    String getWebhookMethod(MacroAction action);
    
    /**
     * Creates a JSON webhook action provider.
     *
     * @param webhookService the webhook service to use
     * @return a new WebhookActionProvider for JSON webhooks
     */
    static WebhookActionProvider jsonProvider(WebhookService webhookService) {
        return new WebhookActionProvider() {
            @Override
            public ActionType getProvidedType() {
                return ActionType.SEND_WEBHOOK_JSON;
            }
            
            @Override
            public ActionResult execute(MacroAction action, ExecutionContext context) {
                String url = (String) action.getConfigValue("url");
                java.util.Map<String, String> headers = getWebhookHeaders(action, context);
                String body = getWebhookBody(action, context);
                String method = getWebhookMethod(action);
                
                long startTime = System.currentTimeMillis();
                WebhookService.WebhookResponse response = webhookService.sendOutgoingWebhook(
                        url, method, headers, body, action.getWebhookConfig());
                long duration = System.currentTimeMillis() - startTime;
                
                if (response.isSuccessful()) {
                    ActionResult result = ActionResult.success(action.getType(), duration);
                    result.setResultValue("statusCode", response.getStatusCode());
                    result.setResultValue("responseBody", response.getBody());
                    result.setResultValue("responseTime", response.getResponseTimeMs());
                    return result;
                } else {
                    return ActionResult.failure(action.getType(), 
                            "Webhook call failed: " + response.getErrorMessage(), duration);
                }
            }
            
            @Override
            public boolean validateWebhookUrl(String url) {
                return url != null && (url.startsWith("https://") || url.startsWith("http://"));
            }
            
            @Override
            public java.util.Map<String, String> getWebhookHeaders(MacroAction action, ExecutionContext context) {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("User-Agent", "Rinna-Macro-Automation");
                
                // Add any custom headers from the action configuration
                Object customHeaders = action.getConfigValue("headers");
                if (customHeaders instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> headerMap = (java.util.Map<String, Object>) customHeaders;
                    for (java.util.Map.Entry<String, Object> entry : headerMap.entrySet()) {
                        if (entry.getValue() != null) {
                            headers.put(entry.getKey(), entry.getValue().toString());
                        }
                    }
                }
                
                return headers;
            }
            
            @Override
            public String getWebhookBody(MacroAction action, ExecutionContext context) {
                // Use the "payload" configuration value as the body
                Object payload = action.getConfigValue("payload");
                if (payload instanceof String) {
                    return (String) payload;
                } else if (payload instanceof java.util.Map) {
                    // Convert the map to a JSON string (simplified approach)
                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> payloadMap = (java.util.Map<String, Object>) payload;
                    boolean first = true;
                    for (java.util.Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                        if (!first) {
                            sb.append(",");
                        }
                        first = false;
                        
                        sb.append("\"").append(entry.getKey()).append("\":");
                        if (entry.getValue() instanceof String) {
                            sb.append("\"").append(entry.getValue()).append("\"");
                        } else if (entry.getValue() == null) {
                            sb.append("null");
                        } else {
                            sb.append(entry.getValue());
                        }
                    }
                    
                    sb.append("}");
                    return sb.toString();
                } else {
                    return "{}";
                }
            }
            
            @Override
            public String getWebhookMethod(MacroAction action) {
                Object method = action.getConfigValue("method");
                if (method instanceof String) {
                    return (String) method;
                }
                return "POST"; // Default to POST for JSON webhooks
            }
        };
    }
}
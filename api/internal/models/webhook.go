/*
 * Webhook configuration models for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package models

import (
	"encoding/json"
	"time"
)

// WebhookProvider represents a supported webhook source
type WebhookProvider string

const (
	// WebhookProviderGitHub represents GitHub webhooks
	WebhookProviderGitHub WebhookProvider = "github"
	
	// WebhookProviderGitLab represents GitLab webhooks
	WebhookProviderGitLab WebhookProvider = "gitlab"
	
	// WebhookProviderBitbucket represents Bitbucket webhooks
	WebhookProviderBitbucket WebhookProvider = "bitbucket"
	
	// WebhookProviderCustom represents custom webhooks
	WebhookProviderCustom WebhookProvider = "custom"
)

// WebhookSignatureMethod represents a signature verification method
type WebhookSignatureMethod string

const (
	// WebhookSignatureSHA256 represents HMAC-SHA256 signature method
	WebhookSignatureSHA256 WebhookSignatureMethod = "sha256"
	
	// WebhookSignatureSHA1 represents HMAC-SHA1 signature method
	WebhookSignatureSHA1 WebhookSignatureMethod = "sha1"
	
	// WebhookSignatureToken represents direct token comparison
	WebhookSignatureToken WebhookSignatureMethod = "token"
)

// WebhookSecurityLevel represents a security strictness level
type WebhookSecurityLevel string

const (
	// WebhookSecurityLevelStrict enforces all security checks
	WebhookSecurityLevelStrict WebhookSecurityLevel = "strict"
	
	// WebhookSecurityLevelStandard uses reasonable security defaults
	WebhookSecurityLevelStandard WebhookSecurityLevel = "standard"
	
	// WebhookSecurityLevelRelaxed relaxes some security checks for compatibility
	WebhookSecurityLevelRelaxed WebhookSecurityLevel = "relaxed"
)

// WebhookConfig represents webhook configuration for a project
type WebhookConfig struct {
	ID                 string               `json:"id"`
	ProjectID          string               `json:"projectId"`
	Provider           WebhookProvider      `json:"provider"`
	URL                string               `json:"url"`
	Secret             string               `json:"secret,omitempty"` // Omitted in responses
	SignatureMethod    WebhookSignatureMethod `json:"signatureMethod"`
	SecurityLevel      WebhookSecurityLevel   `json:"securityLevel"`
	Events             []string             `json:"events"`
	Headers            map[string]string    `json:"headers,omitempty"`
	Description        string               `json:"description"`
	CreatedAt          time.Time            `json:"createdAt"`
	UpdatedAt          time.Time            `json:"updatedAt"`
	LastDelivery       *time.Time           `json:"lastDelivery,omitempty"`
	Active             bool                 `json:"active"`
	RecentDeliveries   int                  `json:"recentDeliveries"`
	SuccessfulDeliveries int                `json:"successfulDeliveries"`
	FailedDeliveries   int                  `json:"failedDeliveries"`
	SSLVerification    bool                 `json:"sslVerification"`
	IPWhitelist        []string             `json:"ipWhitelist,omitempty"`
	CustomProperties   map[string]string    `json:"customProperties,omitempty"`
}

// WebhookConfigCreateRequest represents a request to create a webhook configuration
type WebhookConfigCreateRequest struct {
	ProjectID       string               `json:"projectId"`
	Provider        WebhookProvider      `json:"provider"`
	URL             string               `json:"url,omitempty"` // Not needed for inbound webhooks
	Secret          string               `json:"secret"`        // Optional, generated if not provided
	SignatureMethod WebhookSignatureMethod `json:"signatureMethod,omitempty"`
	SecurityLevel   WebhookSecurityLevel   `json:"securityLevel,omitempty"`
	Events          []string             `json:"events"`
	Headers         map[string]string    `json:"headers,omitempty"`
	Description     string               `json:"description"`
	Active          bool                 `json:"active"`
	SSLVerification bool                 `json:"sslVerification"`
	IPWhitelist     []string             `json:"ipWhitelist,omitempty"`
}

// WebhookConfigUpdateRequest represents a request to update a webhook configuration
type WebhookConfigUpdateRequest struct {
	URL             *string               `json:"url,omitempty"`
	Secret          *string               `json:"secret,omitempty"`
	SignatureMethod *WebhookSignatureMethod `json:"signatureMethod,omitempty"`
	SecurityLevel   *WebhookSecurityLevel   `json:"securityLevel,omitempty"`
	Events          []string              `json:"events,omitempty"`
	Headers         map[string]string     `json:"headers,omitempty"`
	Description     *string               `json:"description,omitempty"`
	Active          *bool                 `json:"active,omitempty"`
	SSLVerification *bool                 `json:"sslVerification,omitempty"`
	IPWhitelist     []string              `json:"ipWhitelist,omitempty"`
}

// WebhookEvent represents a webhook event received by the system
type WebhookEvent struct {
	ID           string           `json:"id"`
	WebhookID    string           `json:"webhookId"`
	ProjectID    string           `json:"projectId"`
	Provider     WebhookProvider  `json:"provider"`
	EventType    string           `json:"eventType"`
	RequestID    string           `json:"requestID"`
	ReceivedAt   time.Time        `json:"receivedAt"`
	ProcessedAt  time.Time        `json:"processedAt"`
	ProcessingTime int64          `json:"processingTime"` // in milliseconds
	Status       string           `json:"status"`
	StatusCode   int              `json:"statusCode"`
	Headers      map[string]string `json:"headers"`
	IPAddress    string           `json:"ipAddress"`
	PayloadSize  int              `json:"payloadSize"`
	Payload      json.RawMessage  `json:"payload,omitempty"` // Omitted in list responses
	Result       json.RawMessage  `json:"result,omitempty"`  // The result of processing
	ErrorMessage string           `json:"errorMessage,omitempty"`
}

// WebhookEventListResponse represents a list of webhook events
type WebhookEventListResponse struct {
	Items      []WebhookEvent `json:"items"`
	TotalItems int            `json:"totalItems"`
	PageSize   int            `json:"pageSize"`
	Page       int            `json:"page"`
}

// WebhookDelivery represents a webhook delivery from the system to an external URL
type WebhookDelivery struct {
	ID            string          `json:"id"`
	WebhookID     string          `json:"webhookId"`
	URL           string          `json:"url"`
	Method        string          `json:"method"`
	Headers       map[string]string `json:"headers"`
	PayloadSize   int             `json:"payloadSize"`
	Payload       json.RawMessage `json:"payload,omitempty"` // Omitted in list responses
	Status        string          `json:"status"`
	StatusCode    int             `json:"statusCode,omitempty"`
	Error         string          `json:"error,omitempty"`
	StartTime     time.Time       `json:"startTime"`
	EndTime       time.Time       `json:"endTime"`
	Duration      int64           `json:"duration"` // in milliseconds
	Attempts      int             `json:"attempts"`
	NextRetryTime *time.Time      `json:"nextRetryTime,omitempty"`
}

// WebhookSecretRotationRequest represents a request to rotate a webhook secret
type WebhookSecretRotationRequest struct {
	WebhookID    string  `json:"webhookId"`
	NewSecret    *string `json:"newSecret,omitempty"` // If not provided, a secure random secret will be generated
	GracePeriod  int     `json:"gracePeriod,omitempty"` // Grace period in hours
}

// WebhookSecretRotationResponse represents the response to a webhook secret rotation
type WebhookSecretRotationResponse struct {
	WebhookID     string    `json:"webhookId"`
	NewSecret     string    `json:"newSecret"`
	OldSecret     string    `json:"oldSecret"`
	GracePeriod   int       `json:"gracePeriod"` // Grace period in hours
	RotatedAt     time.Time `json:"rotatedAt"`
	GraceUntil    time.Time `json:"graceUntil"`
	Instructions  string    `json:"instructions"`
}

// WebhookListResponse represents a list of webhook configurations
type WebhookListResponse struct {
	Items      []WebhookConfig `json:"items"`
	TotalItems int             `json:"totalItems"`
	PageSize   int             `json:"pageSize"`
	Page       int             `json:"page"`
}

// WebhookRegistrationResponse represents a response to a webhook registration
type WebhookRegistrationResponse struct {
	WebhookConfig WebhookConfig `json:"webhookConfig"`
	Secret        string        `json:"secret,omitempty"` // Only included in initial registration
	URL           string        `json:"url"`
	Instructions  string        `json:"instructions,omitempty"`
}

// WebhookStatusResponse represents a webhook status check response
type WebhookStatusResponse struct {
	WebhookID           string    `json:"webhookId"`
	Status              string    `json:"status"`
	LastDelivery        *time.Time `json:"lastDelivery,omitempty"`
	RecentDeliveries    int       `json:"recentDeliveries"`
	SuccessfulDeliveries int      `json:"successfulDeliveries"`
	FailedDeliveries    int       `json:"failedDeliveries"`
	IsActive            bool      `json:"isActive"`
	VerificationResult  string    `json:"verificationResult,omitempty"`
}
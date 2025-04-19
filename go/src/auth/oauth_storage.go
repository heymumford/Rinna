/*
 * File-based storage for OAuth tokens
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

// FileTokenStorage is a file-based implementation of OAuthTokenStorage
type FileTokenStorage struct {
	baseDir     string         // Base directory for token storage
	encryptKey  []byte         // Key for token encryption
	useEncrypt  bool           // Whether to encrypt tokens
	mu          sync.RWMutex   // Mutex for file operations
}

// NewFileTokenStorage creates a new file-based token storage
func NewFileTokenStorage(baseDir string, encryptionKey string) (*FileTokenStorage, error) {
	// Create directory if it doesn't exist
	if err := os.MkdirAll(baseDir, 0700); err != nil {
		return nil, fmt.Errorf("failed to create token directory: %w", err)
	}
	
	storage := &FileTokenStorage{
		baseDir: baseDir,
	}
	
	// Set up encryption if a key is provided
	if encryptionKey != "" {
		// Use a SHA-256 hash of the key for AES-256
		hasher := sha256.New()
		hasher.Write([]byte(encryptionKey))
		storage.encryptKey = hasher.Sum(nil)
		storage.useEncrypt = true
	}
	
	return storage, nil
}

// SaveToken implements OAuthTokenStorage
func (s *FileTokenStorage) SaveToken(token *OAuthToken) error {
	if token == nil {
		return errors.New("token cannot be nil")
	}
	
	// Generate the file path
	filePath, err := s.getTokenFilePath(token.Provider, token.ProjectID, token.UserID)
	if err != nil {
		return err
	}
	
	// Serialize the token
	tokenData, err := json.Marshal(token)
	if err != nil {
		return fmt.Errorf("failed to marshal token: %w", err)
	}
	
	// Encrypt the token if encryption is enabled
	if s.useEncrypt {
		tokenData, err = s.encrypt(tokenData)
		if err != nil {
			return fmt.Errorf("failed to encrypt token: %w", err)
		}
	}
	
	// Create the directory if it doesn't exist
	dir := filepath.Dir(filePath)
	if err := os.MkdirAll(dir, 0700); err != nil {
		return fmt.Errorf("failed to create directory: %w", err)
	}
	
	// Write atomically
	s.mu.Lock()
	defer s.mu.Unlock()
	
	// First write to a temp file
	tempFile := filePath + ".tmp"
	if err := ioutil.WriteFile(tempFile, tokenData, 0600); err != nil {
		return fmt.Errorf("failed to write token to temp file: %w", err)
	}
	
	// Then rename to the final file
	if err := os.Rename(tempFile, filePath); err != nil {
		// Try to clean up the temp file
		os.Remove(tempFile)
		return fmt.Errorf("failed to save token file: %w", err)
	}
	
	return nil
}

// LoadToken implements OAuthTokenStorage
func (s *FileTokenStorage) LoadToken(provider OAuthProvider, projectID, userID string) (*OAuthToken, error) {
	// Generate the file path
	filePath, err := s.getTokenFilePath(provider, projectID, userID)
	if err != nil {
		return nil, err
	}
	
	// Check if the file exists
	s.mu.RLock()
	defer s.mu.RUnlock()
	
	tokenData, err := ioutil.ReadFile(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, errors.New("token not found")
		}
		return nil, fmt.Errorf("failed to read token file: %w", err)
	}
	
	// Decrypt the token if encryption is enabled
	if s.useEncrypt {
		tokenData, err = s.decrypt(tokenData)
		if err != nil {
			return nil, fmt.Errorf("failed to decrypt token: %w", err)
		}
	}
	
	// Deserialize the token
	var token OAuthToken
	if err := json.Unmarshal(tokenData, &token); err != nil {
		return nil, fmt.Errorf("failed to unmarshal token: %w", err)
	}
	
	return &token, nil
}

// DeleteToken implements OAuthTokenStorage
func (s *FileTokenStorage) DeleteToken(provider OAuthProvider, projectID, userID string) error {
	// Generate the file path
	filePath, err := s.getTokenFilePath(provider, projectID, userID)
	if err != nil {
		return err
	}
	
	// Delete the file
	s.mu.Lock()
	defer s.mu.Unlock()
	
	err = os.Remove(filePath)
	if err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("failed to delete token file: %w", err)
	}
	
	return nil
}

// ListTokens implements OAuthTokenStorage
func (s *FileTokenStorage) ListTokens(provider OAuthProvider, projectID string) ([]*OAuthToken, error) {
	// Generate the directory path
	dirPath := filepath.Join(s.baseDir, string(provider), safePath(projectID))
	
	// Check if the directory exists
	s.mu.RLock()
	defer s.mu.RUnlock()
	
	_, err := os.Stat(dirPath)
	if err != nil {
		if os.IsNotExist(err) {
			return []*OAuthToken{}, nil
		}
		return nil, fmt.Errorf("failed to access token directory: %w", err)
	}
	
	// List all token files
	files, err := ioutil.ReadDir(dirPath)
	if err != nil {
		return nil, fmt.Errorf("failed to list token files: %w", err)
	}
	
	// Load each token
	var tokens []*OAuthToken
	for _, file := range files {
		if file.IsDir() || strings.HasSuffix(file.Name(), ".tmp") {
			continue
		}
		
		// Extract the user ID from the filename
		userID := strings.TrimSuffix(file.Name(), ".json")
		userID = strings.TrimSuffix(userID, ".enc") // If encrypted
		
		// Load the token
		token, err := s.LoadToken(provider, projectID, userID)
		if err == nil && token != nil {
			tokens = append(tokens, token)
		}
	}
	
	return tokens, nil
}

// getTokenFilePath generates the file path for a token
func (s *FileTokenStorage) getTokenFilePath(provider OAuthProvider, projectID, userID string) (string, error) {
	if provider == "" {
		return "", errors.New("provider cannot be empty")
	}
	if projectID == "" {
		return "", errors.New("project ID cannot be empty")
	}
	if userID == "" {
		return "", errors.New("user ID cannot be empty")
	}
	
	// Make sure the path components are safe
	providerPath := safePath(string(provider))
	projectIDPath := safePath(projectID)
	userIDPath := safePath(userID)
	
	// Generate the file path
	extension := ".json"
	if s.useEncrypt {
		extension = ".enc"
	}
	
	return filepath.Join(s.baseDir, providerPath, projectIDPath, userIDPath+extension), nil
}

// encrypt encrypts data using AES-GCM
func (s *FileTokenStorage) encrypt(data []byte) ([]byte, error) {
	block, err := aes.NewCipher(s.encryptKey)
	if err != nil {
		return nil, err
	}
	
	// Generate a nonce
	nonce := make([]byte, 12)
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}
	
	// Create the AES-GCM mode
	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	
	// Encrypt the data
	ciphertext := aesgcm.Seal(nil, nonce, data, nil)
	
	// Prepend the nonce
	result := make([]byte, len(nonce)+len(ciphertext))
	copy(result, nonce)
	copy(result[len(nonce):], ciphertext)
	
	return result, nil
}

// decrypt decrypts data using AES-GCM
func (s *FileTokenStorage) decrypt(data []byte) ([]byte, error) {
	if len(data) < 13 { // 12 byte nonce + at least 1 byte ciphertext
		return nil, errors.New("encrypted data too short")
	}
	
	block, err := aes.NewCipher(s.encryptKey)
	if err != nil {
		return nil, err
	}
	
	// Create the AES-GCM mode
	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}
	
	// Extract the nonce
	nonce := data[:12]
	ciphertext := data[12:]
	
	// Decrypt the data
	plaintext, err := aesgcm.Open(nil, nonce, ciphertext, nil)
	if err != nil {
		return nil, err
	}
	
	return plaintext, nil
}

// safePath makes a string safe for use in a file path
func safePath(s string) string {
	// Replace unsafe characters with underscores
	unsafe := []string{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"}
	result := s
	for _, char := range unsafe {
		result = strings.ReplaceAll(result, char, "_")
	}
	return result
}

// CleanupExpiredTokens removes tokens that have expired
func (s *FileTokenStorage) CleanupExpiredTokens() error {
	now := time.Now()
	
	// Walk the token directory
	return filepath.Walk(s.baseDir, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		
		// Skip directories
		if info.IsDir() {
			return nil
		}
		
		// Skip temp files
		if strings.HasSuffix(path, ".tmp") {
			return nil
		}
		
		// Skip non-token files
		if !strings.HasSuffix(path, ".json") && !strings.HasSuffix(path, ".enc") {
			return nil
		}
		
		// Try to load and check the token
		s.mu.RLock()
		tokenData, err := ioutil.ReadFile(path)
		s.mu.RUnlock()
		
		if err != nil {
			// Skip files we can't read
			return nil
		}
		
		// Decrypt if needed
		if s.useEncrypt && strings.HasSuffix(path, ".enc") {
			tokenData, err = s.decrypt(tokenData)
			if err != nil {
				// Skip files we can't decrypt
				return nil
			}
		}
		
		// Parse the token
		var token OAuthToken
		if err := json.Unmarshal(tokenData, &token); err != nil {
			// Skip files we can't parse
			return nil
		}
		
		// Check if the token has expired
		if !token.Expiry.IsZero() && token.Expiry.Before(now) {
			// Delete the expired token
			s.mu.Lock()
			os.Remove(path)
			s.mu.Unlock()
		}
		
		return nil
	})
}
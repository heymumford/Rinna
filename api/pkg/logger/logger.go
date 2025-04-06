/*
 * Standardized logging package for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package logger

import (
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"
)

// LogLevel defines the severity of log messages
type LogLevel int

const (
	// Trace is used for very fine-grained debugging information
	TraceLevel LogLevel = iota
	// Debug is used for development and troubleshooting information
	DebugLevel
	// Info is used for standard operational information
	InfoLevel
	// Warn is used for non-critical issues that need attention
	WarnLevel
	// Error is used for errors that impact functionality
	ErrorLevel
	// Fatal is used for critical errors that prevent startup or require immediate shutdown
	FatalLevel
)

// String converts a LogLevel to its string representation
func (l LogLevel) String() string {
	switch l {
	case TraceLevel:
		return "TRACE"
	case DebugLevel:
		return "DEBUG"
	case InfoLevel:
		return "INFO"
	case WarnLevel:
		return "WARN"
	case ErrorLevel:
		return "ERROR"
	case FatalLevel:
		return "FATAL"
	default:
		return "UNKNOWN"
	}
}

// Config represents the logger configuration
type Config struct {
	Level      LogLevel
	TimeFormat string
	LogFile    string
	ShowCaller bool
}

// DefaultConfig returns the default logger configuration
func DefaultConfig() Config {
	return Config{
		Level:      InfoLevel,
		TimeFormat: time.RFC3339,
		LogFile:    "",
		ShowCaller: true,
	}
}

// Logger represents the standard Rinna logger
type Logger struct {
	config    Config
	mu        sync.Mutex
	writer    io.Writer
	prefix    string
	fields    map[string]interface{}
	logFile   *os.File
	stdLogger *log.Logger
}

// New creates a new Logger with default configuration
func New() *Logger {
	return NewWithConfig(DefaultConfig())
}

// NewWithConfig creates a new Logger with the specified configuration
func NewWithConfig(config Config) *Logger {
	var writer io.Writer = os.Stdout
	var logFile *os.File
	
	// If a log file is specified, open it and use a MultiWriter
	if config.LogFile != "" {
		dir := filepath.Dir(config.LogFile)
		if err := os.MkdirAll(dir, 0755); err != nil {
			log.Printf("Failed to create log directory %s: %v", dir, err)
		}
		
		file, err := os.OpenFile(config.LogFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
		if err != nil {
			log.Printf("Failed to open log file %s: %v", config.LogFile, err)
		} else {
			logFile = file
			writer = io.MultiWriter(os.Stdout, file)
		}
	}
	
	return &Logger{
		config:    config,
		writer:    writer,
		fields:    make(map[string]interface{}),
		logFile:   logFile,
		stdLogger: log.New(writer, "", 0),
	}
}

// Close closes the logger and any open files
func (l *Logger) Close() error {
	l.mu.Lock()
	defer l.mu.Unlock()
	
	if l.logFile != nil {
		err := l.logFile.Close()
		l.logFile = nil
		return err
	}
	return nil
}

// WithField returns a new Logger with the field added to log context
func (l *Logger) WithField(key string, value interface{}) *Logger {
	newLogger := &Logger{
		config:    l.config,
		writer:    l.writer,
		fields:    make(map[string]interface{}),
		logFile:   l.logFile,
		stdLogger: l.stdLogger,
	}
	
	// Copy existing fields
	for k, v := range l.fields {
		newLogger.fields[k] = v
	}
	
	// Add the new field
	newLogger.fields[key] = value
	return newLogger
}

// WithFields returns a new Logger with multiple fields added to log context
func (l *Logger) WithFields(fields map[string]interface{}) *Logger {
	newLogger := &Logger{
		config:    l.config,
		writer:    l.writer,
		fields:    make(map[string]interface{}),
		logFile:   l.logFile,
		stdLogger: l.stdLogger,
	}
	
	// Copy existing fields
	for k, v := range l.fields {
		newLogger.fields[k] = v
	}
	
	// Add the new fields
	for k, v := range fields {
		newLogger.fields[k] = v
	}
	
	return newLogger
}

// WithPrefix returns a new Logger with the specified prefix
func (l *Logger) WithPrefix(prefix string) *Logger {
	newLogger := &Logger{
		config:    l.config,
		writer:    l.writer,
		prefix:    prefix,
		fields:    make(map[string]interface{}),
		logFile:   l.logFile,
		stdLogger: l.stdLogger,
	}
	
	// Copy existing fields
	for k, v := range l.fields {
		newLogger.fields[k] = v
	}
	
	return newLogger
}

// formatFields formats the logger's fields as a string
func (l *Logger) formatFields() string {
	if len(l.fields) == 0 {
		return ""
	}
	
	var parts []string
	for k, v := range l.fields {
		parts = append(parts, fmt.Sprintf("%s=%v", k, v))
	}
	
	return " " + strings.Join(parts, " ")
}

// getCaller returns the file and line number of the caller
func getCaller() string {
	_, file, line, ok := runtime.Caller(3) // Skip the getCaller, log, and public logging method
	if !ok {
		return "unknown:0"
	}
	
	// Use just the base filename, not the full path
	return fmt.Sprintf("%s:%d", filepath.Base(file), line)
}

// log logs a message at the specified level
func (l *Logger) log(level LogLevel, message string, args ...interface{}) {
	if level < l.config.Level {
		return
	}
	
	l.mu.Lock()
	defer l.mu.Unlock()
	
	// Format the message if args are provided
	if len(args) > 0 {
		message = fmt.Sprintf(message, args...)
	}
	
	// Build the log message
	timestamp := time.Now().Format(l.config.TimeFormat)
	levelStr := level.String()
	
	var builder strings.Builder
	
	// Format: timestamp [level] [prefix] message fields [caller]
	builder.WriteString(timestamp)
	builder.WriteString(" [")
	builder.WriteString(levelStr)
	builder.WriteString("]")
	
	if l.prefix != "" {
		builder.WriteString(" [")
		builder.WriteString(l.prefix)
		builder.WriteString("]")
	}
	
	builder.WriteString(" ")
	builder.WriteString(message)
	
	// Add fields
	fields := l.formatFields()
	if fields != "" {
		builder.WriteString(fields)
	}
	
	// Add caller info if enabled
	if l.config.ShowCaller {
		builder.WriteString(" [")
		builder.WriteString(getCaller())
		builder.WriteString("]")
	}
	
	// Log the message
	l.stdLogger.Println(builder.String())
	
	// Fatal logs also exit the program
	if level == FatalLevel {
		os.Exit(1)
	}
}

// Trace logs a message at trace level
func (l *Logger) Trace(message string, args ...interface{}) {
	l.log(TraceLevel, message, args...)
}

// Debug logs a message at debug level
func (l *Logger) Debug(message string, args ...interface{}) {
	l.log(DebugLevel, message, args...)
}

// Info logs a message at info level
func (l *Logger) Info(message string, args ...interface{}) {
	l.log(InfoLevel, message, args...)
}

// Warn logs a message at warning level
func (l *Logger) Warn(message string, args ...interface{}) {
	l.log(WarnLevel, message, args...)
}

// Error logs a message at error level
func (l *Logger) Error(message string, args ...interface{}) {
	l.log(ErrorLevel, message, args...)
}

// Fatal logs a message at fatal level and exits the program
func (l *Logger) Fatal(message string, args ...interface{}) {
	l.log(FatalLevel, message, args...)
}

// Default logger instance
var std = New()

// GetLogger returns the default logger
func GetLogger() *Logger {
	return std
}

// SetLogger sets the default logger
func SetLogger(logger *Logger) {
	std = logger
}

// Configure configures the default logger
func Configure(config Config) {
	std = NewWithConfig(config)
}

// SetLevel sets the log level for the default logger
func SetLevel(level LogLevel) {
	std.config.Level = level
}

// Close closes the default logger
func Close() error {
	return std.Close()
}

// Global functions that use the default logger

// Trace logs a message at trace level
func Trace(message string, args ...interface{}) {
	std.Trace(message, args...)
}

// Debug logs a message at debug level
func Debug(message string, args ...interface{}) {
	std.Debug(message, args...)
}

// Info logs a message at info level
func Info(message string, args ...interface{}) {
	std.Info(message, args...)
}

// Warn logs a message at warning level
func Warn(message string, args ...interface{}) {
	std.Warn(message, args...)
}

// Error logs a message at error level
func Error(message string, args ...interface{}) {
	std.Error(message, args...)
}

// Fatal logs a message at fatal level and exits the program
func Fatal(message string, args ...interface{}) {
	std.Fatal(message, args...)
}

// WithField returns a new logger with the field added
func WithField(key string, value interface{}) *Logger {
	return std.WithField(key, value)
}

// WithFields returns a new logger with the fields added
func WithFields(fields map[string]interface{}) *Logger {
	return std.WithFields(fields)
}

// WithPrefix returns a new logger with the specified prefix
func WithPrefix(prefix string) *Logger {
	return std.WithPrefix(prefix)
}

// Field creates a structured log field with key and value
// This is a utility function for creating fields in a more readable way
func Field(key string, value interface{}) map[string]interface{} {
	return map[string]interface{}{key: value}
}
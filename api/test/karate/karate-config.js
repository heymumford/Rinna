function fn() {
  // Environment variable for environment selection, defaults to 'dev'
  var env = karate.env || 'dev';
  karate.log('karate.env = ', env);

  // Base configuration for all environments
  var config = {
    baseUrl: 'http://localhost:8080/',
    apiVersion: 'v1',
    timeoutMs: 15000,
    // Mock authentication function - in real implementation, this would call auth.feature
    getAuthToken: function() {
      // In real implementation: return karate.call('classpath:auth.feature').token;
      return 'sample-jwt-token';
    }
  };

  // Environment-specific configurations
  if (env === 'dev') {
    // Development configuration
    config.baseUrl = 'http://localhost:8080/';
    config.logEnabled = true;
  } else if (env === 'qa') {
    // QA configuration
    config.baseUrl = 'https://qa-api.rinna.org/';
    config.logEnabled = true;
  } else if (env === 'staging') {
    // Staging configuration
    config.baseUrl = 'https://staging-api.rinna.org/';
    config.logEnabled = false;
    // Would likely have additional configurations for staging
  } else if (env === 'prod') {
    // Production configuration
    config.baseUrl = 'https://api.rinna.org/';
    config.logEnabled = false;
    config.timeoutMs = 30000; // Longer timeout for production
  }

  // Logging configuration
  karate.configure('logPrettyRequest', config.logEnabled);
  karate.configure('logPrettyResponse', config.logEnabled);
  
  // Connection timeouts
  karate.configure('connectTimeout', config.timeoutMs);
  karate.configure('readTimeout', config.timeoutMs);

  return config;
}
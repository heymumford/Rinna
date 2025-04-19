# Multi-stage build for Python services in Rinna
# Development/Testing version with full tooling

# -----------------------------------------------------------------------------
# BASE STAGE - Common dependencies and setup
# -----------------------------------------------------------------------------
FROM python:3.13-slim AS base

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONPATH=/app

WORKDIR /app

# Install system dependencies 
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    # Dependencies for WeasyPrint and other libraries
    libpango-1.0-0 \
    libpangoft2-1.0-0 \
    libpangocairo-1.0-0 \
    libcairo2 \
    libgdk-pixbuf2.0-0 \
    libffi-dev \
    poppler-utils \
    # Clean up
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better layer caching
COPY requirements.txt requirements-core.txt ./

# -----------------------------------------------------------------------------
# BUILDER STAGE - Install dependencies and build packages
# -----------------------------------------------------------------------------
FROM base AS builder

# Install Python dependencies
RUN pip install --no-cache-dir --user -r requirements.txt

# Copy Python package and build
COPY python/ /app/python/

# Build packages
RUN cd /app/python && SETUP_PACKAGE=lucidchart-py pip install --no-cache-dir --user -e . && \
    cd /app/python && SETUP_PACKAGE=rinna pip install --no-cache-dir --user -e .

# -----------------------------------------------------------------------------
# TEST STAGE - For running tests
# -----------------------------------------------------------------------------
FROM base AS test

# Create a non-root user for running tests
ARG USER_ID=1001
ARG GROUP_ID=1001
RUN groupadd -g $GROUP_ID appuser && \
    useradd -u $USER_ID -g appuser -m -s /bin/bash appuser

# Create directories for test outputs
RUN mkdir -p /app/test-output /app/coverage /app/logs && \
    chown -R appuser:appuser /app

# Copy dependencies from builder stage
COPY --from=builder /root/.local /home/appuser/.local
ENV PATH=/home/appuser/.local/bin:$PATH

# Copy Python package 
COPY python/ /app/python/

# Create reports directory
RUN mkdir -p /app/python/rinna/reports/templates/css && \
    chown -R appuser:appuser /app/python/rinna/reports

# Copy test scripts
COPY bin/test-python-pyramid.sh /app/bin/
RUN chmod +x /app/bin/test-python-pyramid.sh

# Set user for running tests
USER appuser

# Set entrypoint for test container
ENTRYPOINT ["/app/bin/test-python-pyramid.sh", "--isolated", "--system-python"]
CMD ["all"]

# -----------------------------------------------------------------------------
# DEVELOPMENT STAGE - For local development
# -----------------------------------------------------------------------------
FROM test AS development

USER root

# Install development tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    vim \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Install development Python packages
RUN pip install --no-cache-dir ipython debugpy jupyter

# Reset to appuser for safety
USER appuser

# Set entrypoint for development container
ENTRYPOINT ["/bin/bash"]
#!/bin/bash

# Create directories for all components
mkdir -p docs/antora/modules/ROOT/assets/images
mkdir -p java/docs/antora/modules/ROOT/assets/images
mkdir -p python/docs/antora/modules/ROOT/assets/images
mkdir -p go/docs/antora/modules/ROOT/assets/images

# Copy logo files
cp "creative/assets/PNG Logo Files/Transparent Logo.png" docs/antora/modules/ROOT/assets/images/rinna-logo.png
cp "creative/assets/PNG Logo Files/Original Logo Symbol.png" docs/antora/modules/ROOT/assets/images/rinna-symbol.png
cp "creative/assets/Favicon/Wordpress Transparent.png" docs/antora/modules/ROOT/assets/images/rinna-favicon.png

# Copy to component directories
cp docs/antora/modules/ROOT/assets/images/rinna-logo.png java/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-symbol.png java/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-favicon.png java/docs/antora/modules/ROOT/assets/images/

cp docs/antora/modules/ROOT/assets/images/rinna-logo.png python/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-symbol.png python/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-favicon.png python/docs/antora/modules/ROOT/assets/images/

cp docs/antora/modules/ROOT/assets/images/rinna-logo.png go/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-symbol.png go/docs/antora/modules/ROOT/assets/images/
cp docs/antora/modules/ROOT/assets/images/rinna-favicon.png go/docs/antora/modules/ROOT/assets/images/

echo "Branding assets copied successfully!"
#!/bin/bash

# Create directories for all components
mkdir -p docs/antora/modules/ROOT/assets/images
mkdir -p java/docs/antora/modules/ROOT/assets/images
mkdir -p python/docs/antora/modules/ROOT/assets/images
mkdir -p go/docs/antora/modules/ROOT/assets/images
mkdir -p docs/supplemental-ui/img

# Copy logo files to supplemental UI (don't use symlinks)
cp "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" docs/supplemental-ui/img/rinna-logo.png
cp "$(pwd)/creative/assets/PNG Logo Files/Original Logo Symbol.png" docs/supplemental-ui/img/rinna-symbol.png
cp "$(pwd)/creative/assets/Favicon/Wordpress Transparent.png" docs/supplemental-ui/img/rinna-favicon.png

# Create symbolic links to logo files in module directories
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" docs/antora/modules/ROOT/assets/images/rinna-logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" docs/antora/modules/ROOT/assets/images/rinna_logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Original Logo Symbol.png" docs/antora/modules/ROOT/assets/images/rinna-symbol.png
ln -sf "$(pwd)/creative/assets/Favicon/Wordpress Transparent.png" docs/antora/modules/ROOT/assets/images/rinna-favicon.png

# Create symbolic links to component directories
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" java/docs/antora/modules/ROOT/assets/images/rinna-logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" java/docs/antora/modules/ROOT/assets/images/rinna_logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Original Logo Symbol.png" java/docs/antora/modules/ROOT/assets/images/rinna-symbol.png
ln -sf "$(pwd)/creative/assets/Favicon/Wordpress Transparent.png" java/docs/antora/modules/ROOT/assets/images/rinna-favicon.png

ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" python/docs/antora/modules/ROOT/assets/images/rinna-logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" python/docs/antora/modules/ROOT/assets/images/rinna_logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Original Logo Symbol.png" python/docs/antora/modules/ROOT/assets/images/rinna-symbol.png
ln -sf "$(pwd)/creative/assets/Favicon/Wordpress Transparent.png" python/docs/antora/modules/ROOT/assets/images/rinna-favicon.png

ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" go/docs/antora/modules/ROOT/assets/images/rinna-logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Transparent Logo.png" go/docs/antora/modules/ROOT/assets/images/rinna_logo.png
ln -sf "$(pwd)/creative/assets/PNG Logo Files/Original Logo Symbol.png" go/docs/antora/modules/ROOT/assets/images/rinna-symbol.png
ln -sf "$(pwd)/creative/assets/Favicon/Wordpress Transparent.png" go/docs/antora/modules/ROOT/assets/images/rinna-favicon.png

echo "Branding assets linked successfully!"

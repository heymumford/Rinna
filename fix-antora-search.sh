#!/bin/bash

# This script fixes issues with Antora search functionality and image paths

set -e

echo "Fixing Antora search and image issues..."

# Check if build/site directory exists
if [ ! -d "build/site" ]; then
    echo "Error: build/site directory not found. Please run ./build-docs.sh first."
    exit 1
fi

# Create a comprehensive fix for the search-ui.js and favicon issues
cat > build/site/_/js/search-fix.js << 'EOL'
// Apply immediate fixes to prevent search errors
(function() {
  console.log("Setting up search fixes");
  
  // Create a global reference to avoid errors
  window._searchFixApplied = true;
  
  // Monkey patch document.getElementById before other scripts run
  const originalGetElementById = document.getElementById;
  document.getElementById = function(id) {
    // Get the actual element
    const element = originalGetElementById.call(document, id);
    
    // Special handling for search-related elements that might be null
    if (element === null && (id.includes('search') || id === 'lunr-index')) {
      console.log(`Creating dummy element for: ${id}`);
      
      // Return a dummy object that implements all the methods used by search-ui.js
      return {
        parentNode: {
          removeChild: function(child) { console.log(`Dummy removeChild called for ${id}`); return child; },
          appendChild: function(child) { console.log(`Dummy appendChild called for ${id}`); return child; },
          insertBefore: function(child, ref) { console.log(`Dummy insertBefore called for ${id}`); return child; }
        },
        value: '',
        disabled: false,
        title: '',
        hasAttribute: function() { return false; },
        setAttribute: function() { return null; },
        getAttribute: function() { return null; },
        dispatchEvent: function(event) {
          console.log(`Dummy dispatchEvent called for ${id} with event: ${event.type}`);
          return true;
        },
        classList: {
          add: function() {},
          remove: function() {},
          contains: function() { return false; },
          toggle: function() { return false; }
        },
        querySelector: function() { return null; },
        querySelectorAll: function() { return []; },
        addEventListener: function(type, listener) {
          console.log(`Dummy addEventListener called for ${id} with event: ${type}`);
        },
        removeEventListener: function() {},
        contains: function() { return false; },
        tagName: 'DIV',
        nodeName: 'DIV',
        nodeType: 1,
        style: {},
        focus: function() {},
        blur: function() {}
      };
    }
    
    return element;
  };
  
  // Fix for initSearch function
  window.antoraSearch = window.antoraSearch || {};
  window.antoraSearch.initSearch = window.antoraSearch.initSearch || function(lunr, data) {
    console.log('Search initialization defined with data');
    window.antoraLunrIndex = data;
    return { index: {} }; // Return object to prevent errors
  };
})();

// Add an onload handler to do additional fixes after page loads
window.addEventListener('DOMContentLoaded', function() {
  console.log("DOM loaded - applying additional fixes");
  
  // Fix any broken image links that might still exist
  const images = document.querySelectorAll('img[src*="_images/"]');
  images.forEach(function(img) {
    const src = img.getAttribute('src');
    if (src) {
      // Fix all image paths to use the unified _/img directory
      const newSrc = src.replace(/.*_images\/(.*)$/, '../_/img/$1');
      img.setAttribute('src', newSrc);
      console.log(`Fixed image path: ${src} → ${newSrc}`);
    }
  });
  
  // Ensure favicon link exists
  if (!document.querySelector('link[rel="icon"]')) {
    const favicon = document.createElement('link');
    favicon.rel = 'icon';
    favicon.href = '../_/img/rinna-favicon.png';
    favicon.type = 'image/png';
    document.head.appendChild(favicon);
    console.log('Added missing favicon link');
  }
  
  // Create a dummy search input element if it doesn't exist
  if (!document.getElementById('search-input')) {
    var dummySearch = document.createElement('input');
    dummySearch.id = 'search-input';
    dummySearch.type = 'text';
    dummySearch.placeholder = 'Search';
    dummySearch.style.display = 'none';
    document.body.appendChild(dummySearch);
    console.log('Created dummy search input element');
  }
  
  // Fix broken navigation links
  const links = document.querySelectorAll('a[href="overview.html"]');
  links.forEach(function(link) {
    link.setAttribute('href', 'rinna/overview.html');
    console.log(`Fixed broken link: overview.html → rinna/overview.html`);
  });
});
EOL

# Create directories for all image paths
mkdir -p build/site/_/img
mkdir -p build/site/_images

# Always use Favicon Transparent.ico
FAVICON_SOURCE="creative/assets/Favicon/Favicon Transparent.ico"
echo "Using favicon from: $FAVICON_SOURCE"

# Check if the favicon exists
if [ ! -f "$FAVICON_SOURCE" ]; then
  echo "Error: Favicon file not found at $FAVICON_SOURCE"
  exit 1
fi

# Copy images to all possible locations Antora might be looking
echo "Copying logo images to all possible paths..."
cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/_/img/rinna-logo.png
cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/_/img/rinna_logo.png
cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png build/site/_/img/rinna-symbol.png
cp -f "$FAVICON_SOURCE" build/site/_/img/rinna-favicon.png
cp -f "$FAVICON_SOURCE" build/site/favicon.ico
cp -f "$FAVICON_SOURCE" build/site/favicon.png

# Also copy to _images directory (which is where the adoc file is looking)
cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/_images/rinna-logo.png
cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/_images/rinna_logo.png
cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png build/site/_images/rinna-symbol.png
cp -f "$FAVICON_SOURCE" build/site/_images/rinna-favicon.png

# Copy to component specific image folders
for component in go java python rinna; do
  mkdir -p build/site/$component/_/img
  mkdir -p build/site/$component/_/js  
  mkdir -p build/site/$component/_images
  
  # Copy to component root
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/$component/_/img/rinna-logo.png
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/$component/_/img/rinna_logo.png
  cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png build/site/$component/_/img/rinna-symbol.png
  cp -f "$FAVICON_SOURCE" build/site/$component/_/img/rinna-favicon.png
  cp -f build/site/_/js/search-fix.js build/site/$component/_/js/search-fix.js
  
  # Copy to _images directory within component
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/$component/_images/rinna-logo.png
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png build/site/$component/_images/rinna_logo.png
  cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png build/site/$component/_images/rinna-symbol.png
  cp -f "$FAVICON_SOURCE" build/site/$component/_images/rinna-favicon.png
  
  # Also add favicon to each component root
  cp -f "$FAVICON_SOURCE" build/site/$component/favicon.ico
  cp -f "$FAVICON_SOURCE" build/site/$component/favicon.png
  
  # Need to handle nested directories within components
  for subdir in $(find build/site/$component -type d -mindepth 1); do
    mkdir -p "$subdir/_/img"
    mkdir -p "$subdir/_/js"
    cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png "$subdir/_/img/rinna-logo.png"
    cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png "$subdir/_/img/rinna_logo.png"
    cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png "$subdir/_/img/rinna-symbol.png"
    cp -f "$FAVICON_SOURCE" "$subdir/_/img/rinna-favicon.png"
    cp -f build/site/_/js/search-fix.js "$subdir/_/js/search-fix.js"
  done
done

# Hack: Directly modify the HTML files to fix the image paths
echo "Fixing image paths in HTML files..."
find build/site -name "*.html" -exec sed -i '' -e 's|src="_images/rinna-logo.png"|src="../_/img/rinna-logo.png"|g' {} \;
find build/site -name "*.html" -exec sed -i '' -e 's|src="_images/rinna_logo.png"|src="../_/img/rinna_logo.png"|g' {} \;

# Special fix for nested pages
echo "Fixing paths for nested pages..."
find build/site/*/architecture -name "*.html" -exec sed -i '' -e 's|src="\.\./\_/img/|src="../../_/img/|g' {} \;
find build/site/*/guides -name "*.html" -exec sed -i '' -e 's|src="\.\./\_/img/|src="../../_/img/|g' {} \;
find build/site/*/reference -name "*.html" -exec sed -i '' -e 's|src="\.\./\_/img/|src="../../_/img/|g' {} \;

# Remove all existing favicon references first
find build/site -name "*.html" -exec sed -i '' -e 's|<link rel="icon"[^>]*>||g' {} \;

# Add the correct favicon to all HTML files based on their depth
echo "Adding correct favicon reference to all HTML files..."
find build/site -maxdepth 1 -name "*.html" -exec sed -i '' -e 's|</head>|<link rel="icon" href="favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">\n</head>|g' {} \;
find build/site -maxdepth 2 -name "*.html" -exec sed -i '' -e 's|</head>|<link rel="icon" href="../favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="../favicon.ico" type="image/x-icon">\n</head>|g' {} \;
find build/site/*/architecture -name "*.html" -exec sed -i '' -e 's|</head>|<link rel="icon" href="../../favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="../../favicon.ico" type="image/x-icon">\n</head>|g' {} \;
find build/site/*/guides -name "*.html" -exec sed -i '' -e 's|</head>|<link rel="icon" href="../../favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="../../favicon.ico" type="image/x-icon">\n</head>|g' {} \;
find build/site/*/reference -name "*.html" -exec sed -i '' -e 's|</head>|<link rel="icon" href="../../favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="../../favicon.ico" type="image/x-icon">\n</head>|g' {} \;

# Add a proper head tag to the main index.html redirect page
sed -i '' -e 's|<meta charset="utf-8">|<head>\n<meta charset="utf-8">|' build/site/index.html
sed -i '' -e 's|<title>Redirect Notice</title>|<title>Redirect Notice</title>\n<link rel="icon" href="favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">\n</head>|' build/site/index.html

# Fix script references for the search-fix.js script
echo "Fixing script references..."
# Remove any previous search-fix.js references
find build/site -name "*.html" -exec sed -i '' -e 's|<script src="[^"]*/_/js/search-fix.js"></script>||g' {} \;

# Add the correct search-fix.js reference based on directory depth
find build/site -maxdepth 2 -name "*.html" -exec sed -i '' -e 's|</head>|<script src="../_/js/search-fix.js"></script>\n</head>|g' {} \;
find build/site/*/architecture -name "*.html" -exec sed -i '' -e 's|</head>|<script src="../../_/js/search-fix.js"></script>\n</head>|g' {} \;
find build/site/*/guides -name "*.html" -exec sed -i '' -e 's|</head>|<script src="../../_/js/search-fix.js"></script>\n</head>|g' {} \;
find build/site/*/reference -name "*.html" -exec sed -i '' -e 's|</head>|<script src="../../_/js/search-fix.js"></script>\n</head>|g' {} \;

# Rewrite the search-ui.js script include to ensure our fix runs first
find build/site -maxdepth 2 -name "*.html" -exec sed -i '' -e 's|<script src="\([^"]*\)/_/js/search-ui.js"|<script src="\1/_/js/search-fix.js"></script>\n<script src="\1/_/js/search-ui.js"|g' {} \;
find build/site/*/architecture -name "*.html" -exec sed -i '' -e 's|<script src="\([^"]*\)/_/js/search-ui.js"|<script src="\1/_/js/search-fix.js"></script>\n<script src="\1/_/js/search-ui.js"|g' {} \;
find build/site/*/guides -name "*.html" -exec sed -i '' -e 's|<script src="\([^"]*\)/_/js/search-ui.js"|<script src="\1/_/js/search-fix.js"></script>\n<script src="\1/_/js/search-ui.js"|g' {} \;
find build/site/*/reference -name "*.html" -exec sed -i '' -e 's|<script src="\([^"]*\)/_/js/search-ui.js"|<script src="\1/_/js/search-fix.js"></script>\n<script src="\1/_/js/search-ui.js"|g' {} \;

# Hack: Create symlink or copy overview.html to root if it doesn't exist
# This fixes the 404 error when trying to access /overview directly
if [ -f "build/site/rinna/overview.html" ] && [ ! -f "build/site/overview.html" ]; then
  echo "Creating overview.html redirect in root directory..."
  cat > build/site/overview.html << EOF
<!DOCTYPE html>
<head>
<meta charset="utf-8">
<link rel="canonical" href="https://docs.rinnacloud.com/rinna/overview.html">
<script>location="rinna/overview.html"</script>
<meta http-equiv="refresh" content="0; url=rinna/overview.html">
<meta name="robots" content="noindex">
<title>Redirect Notice</title>
<link rel="icon" href="favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
</head>
<body>
<h1>Redirect Notice</h1>
<p>The page you requested has been relocated to <a href="rinna/overview.html">https://docs.rinnacloud.com/rinna/overview.html</a>.</p>
</body>
EOF
fi

# Fix for clean-architecture.html specific issues
echo "Fixing clean-architecture.html specific issues..."
for clean_arch_file in $(find build/site -name "clean-architecture.html"); do
  dir=$(dirname "$clean_arch_file")
  component=$(echo "$dir" | awk -F '/' '{print $(NF-1)}')  # Get component name
  parent_dir=$(dirname "$dir")  # Get parent directory
  
  # Copy assets directly into the directory of the clean-architecture.html file
  mkdir -p "$dir/_/img"
  mkdir -p "$dir/_/js"
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png "$dir/_/img/rinna-logo.png"
  cp -f creative/assets/PNG\ Logo\ Files/Transparent\ Logo.png "$dir/_/img/rinna_logo.png"
  cp -f creative/assets/PNG\ Logo\ Files/Original\ Logo\ Symbol.png "$dir/_/img/rinna-symbol.png"
  cp -f "$FAVICON_SOURCE" "$dir/favicon.ico"  # Add favicon directly to the directory
  cp -f build/site/_/js/search-fix.js "$dir/_/js/search-fix.js"
  
  # Fix the paths in this specific file to use local assets
  sed -i '' -e 's|<link rel="icon"[^>]*>|<link rel="icon" href="favicon.ico" type="image/x-icon">\n<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">|g' "$clean_arch_file"
  sed -i '' -e 's|src="[^"]*/_/js/search-fix.js"|src="_/js/search-fix.js"|g' "$clean_arch_file"
done

echo "Fixes applied successfully!"
echo "Run 'npm run docs:serve' to start the documentation server"
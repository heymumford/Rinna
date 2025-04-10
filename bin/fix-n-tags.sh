#\!/bin/bash

# Find all pom.xml files
find . -name "pom.xml" -type f | while read -r file; do
  # Replace <n> with <name> using sed
  sed -i 's/<n>/<name>/g; s/<\/n>/<\/name>/g' "$file"
  echo "Fixed $file"
done

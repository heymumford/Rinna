#!/bin/bash

# Test Pyramid Coverage Assessment Tool
# Analyzes test coverage across languages and test levels in the Rinna project

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${PROJECT_ROOT}"

VERBOSE=false
JSON_OUTPUT=false
OUTPUT_FILE=""

print_usage() {
  echo "Usage: $0 [options]"
  echo ""
  echo "Options:"
  echo "  -h, --help             Show this help message and exit"
  echo "  -v, --verbose          Show detailed output"
  echo "  -j, --json             Output in JSON format"
  echo "  -o, --output <file>    Write output to file"
  echo ""
  echo "Examples:"
  echo "  $0                     Generate standard coverage report"
  echo "  $0 -v                  Generate detailed coverage report"
  echo "  $0 -j -o report.json   Generate JSON report to report.json"
}

# Parse command line options
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      print_usage
      exit 0
      ;;
    -v|--verbose)
      VERBOSE=true
      shift
      ;;
    -j|--json)
      JSON_OUTPUT=true
      shift
      ;;
    -o|--output)
      OUTPUT_FILE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      print_usage
      exit 1
      ;;
  esac
done

# Test categories by level in the pyramid
declare -A test_categories=(
  ["unit"]="Unit"
  ["component"]="Component"
  ["integration"]="Integration"
  ["acceptance"]="Acceptance"
  ["performance"]="Performance"
)

# Language-specific file patterns
declare -A java_patterns=(
  ["unit"]="UnitTest\|UnitTaggedTest\|unit/.*Test.java\|src/test/java/org/rinna/unit/.*Test.java"
  ["component"]="ComponentTest\|ComponentTaggedTest\|component/.*Test.java\|src/test/java/org/rinna/component/.*Test.java"
  ["integration"]="IntegrationTest\|IntegrationTaggedTest\|integration/.*Test.java\|src/test/java/org/rinna/integration/.*Test.java"
  ["acceptance"]="AcceptanceTest\|AcceptanceTaggedTest\|acceptance/.*Test.java\|src/test/java/org/rinna/acceptance/.*Test.java\|bdd/.*Runner\.java"
  ["performance"]="PerformanceTest\|PerformanceTaggedTest\|performance/.*Test.java\|src/test/java/org/rinna/performance/.*Test.java\|.*Benchmark.*Test"
)

declare -A go_patterns=(
  ["unit"]="test/unit/.*_test\.go"
  ["component"]="test/component/.*_test\.go"
  ["integration"]="test/integration/.*_test\.go\|integration_test\.go"
  ["acceptance"]="test/acceptance/.*_test\.go"
  ["performance"]="test/performance/.*_test\.go\|.*_bench_test\.go"
)

declare -A python_patterns=(
  ["unit"]="tests/unit/test_.*\.py"
  ["component"]="tests/component/test_.*\.py"
  ["integration"]="tests/integration/test_.*\.py"
  ["acceptance"]="tests/acceptance/test_.*\.py"
  ["performance"]="tests/performance/test_.*\.py"
)

# Arrays to store test counts
declare -A java_counts
declare -A go_counts
declare -A python_counts
declare -A total_counts

# Initialize counts
for category in "${!test_categories[@]}"; do
  java_counts[$category]=0
  go_counts[$category]=0
  python_counts[$category]=0
  total_counts[$category]=0
done

# Count tests for each language and category
echo "Counting tests by language and category..."

# Count Java tests
for category in "${!test_categories[@]}"; do
  pattern="${java_patterns[$category]}"
  if [ -n "$pattern" ]; then
    # First, standard pattern matching
    count=$(find . -path "*/target/*" -prune -o -path "*/backup/*" -prune -o -type f -name "*Test.java" | grep -E "$pattern" | wc -l)
    
    # Additionally, check for CLI tests in the correct directories
    if [ "$category" = "unit" ]; then
      cli_count=$(find . -path "*/rinna-cli/src/test/java/org/rinna/cli/unit/*" -name "*Test.java" | wc -l)
      count=$((count + cli_count))
    elif [ "$category" = "component" ]; then
      cli_count=$(find . -path "*/rinna-cli/src/test/java/org/rinna/cli/component/*" -name "*Test.java" | wc -l)
      count=$((count + cli_count))
    elif [ "$category" = "integration" ]; then
      cli_count=$(find . -path "*/rinna-cli/src/test/java/org/rinna/cli/integration/*" -name "*Test.java" | wc -l)
      count=$((count + cli_count))
    elif [ "$category" = "acceptance" ]; then
      cli_count=$(find . -path "*/rinna-cli/src/test/java/org/rinna/cli/acceptance/*" -name "*Test.java" | wc -l)
      count=$((count + cli_count))
    elif [ "$category" = "performance" ]; then
      cli_count=$(find . -path "*/rinna-cli/src/test/java/org/rinna/cli/performance/*" -name "*Test.java" | wc -l)
      count=$((count + cli_count))
    fi
    
    java_counts[$category]=$count
    total_counts[$category]=$((total_counts[$category] + count))
    
    if [ "$VERBOSE" = true ]; then
      echo "Found ${java_counts[$category]} Java ${test_categories[$category]} tests"
    fi
  fi
done

# Count Go tests
for category in "${!test_categories[@]}"; do
  pattern="${go_patterns[$category]}"
  if [ -n "$pattern" ]; then
    # Standard pattern matching
    count=$(find . -path "*/target/*" -prune -o -path "*/backup/*" -prune -o -type f -name "*_test.go" | grep -E "$pattern" | wc -l)
    
    # Additionally, check for specific files we've created
    if [ "$category" = "integration" ]; then
      # Specifically check for the CLI API integration test
      if [ -f "./api/test/integration/cli_api_integration_test.go" ]; then
        count=$((count + 1))
      fi
    elif [ "$category" = "performance" ]; then
      # Specifically check for the API performance test
      if [ -f "./api/test/performance/api_performance_test.go" ]; then
        count=$((count + 1))
      fi
    fi
    
    go_counts[$category]=$count
    total_counts[$category]=$((total_counts[$category] + count))
    
    if [ "$VERBOSE" = true ]; then
      echo "Found ${go_counts[$category]} Go ${test_categories[$category]} tests"
    fi
  fi
done

# Count Python tests
for category in "${!test_categories[@]}"; do
  pattern="${python_patterns[$category]}"
  if [ -n "$pattern" ]; then
    count=$(find . -path "*/target/*" -prune -o -path "*/.venv/*" -prune -o -path "*/backup/*" -prune -o -type f -name "test_*.py" | grep -E "$pattern" | wc -l)
    python_counts[$category]=$count
    total_counts[$category]=$((total_counts[$category] + count))
    
    if [ "$VERBOSE" = true ]; then
      echo "Found ${python_counts[$category]} Python ${test_categories[$category]} tests"
    fi
  fi
done

# Calculate total by language and handle uncategorized tests
java_total=0
go_total=0
python_total=0
grand_total=0

# Categorize uncategorized tests based on name patterns
categorize_uncategorized_java_tests() {
  echo "Categorizing uncategorized Java tests..."
  
  # Get total Java test count
  total_java_test_count=$(find . -path "*/target/*" -prune -o -type f -name "*Test.java" | grep -v "target" | wc -l)
  
  # Calculate sum of currently categorized tests
  categorized_java_count=0
  for category in "${!test_categories[@]}"; do
    categorized_java_count=$((categorized_java_count + java_counts[$category]))
  done
  
  # Calculate uncategorized tests
  uncategorized_java_count=$((total_java_test_count - categorized_java_count))
  
  if [ $uncategorized_java_count -gt 0 ]; then
    echo "Found $uncategorized_java_count uncategorized Java tests"
    
    # Categorize based on test naming patterns
    unit_tests=$(find . -path "*/target/*" -prune -o -type f -name "*Test.java" | grep -v "target" | grep -i -E "(Unit|Simple|Default|Basic|Model).*Test" | grep -v -E "$(echo "${java_patterns[@]}" | tr ' ' '|')" | wc -l)
    
    component_tests=$(find . -path "*/target/*" -prune -o -type f -name "*Test.java" | grep -v "target" | grep -i -E "(Component|Service|Repository|Adapter).*Test" | grep -v -E "$(echo "${java_patterns[@]}" | tr ' ' '|')" | wc -l)
    
    integration_tests=$(find . -path "*/target/*" -prune -o -type f -name "*Test.java" | grep -v "target" | grep -i -E "(Integration|Api|DB|Database).*Test" | grep -v -E "$(echo "${java_patterns[@]}" | tr ' ' '|')" | wc -l)
    
    # Remaining tests are mostly unit tests
    remaining_tests=$((uncategorized_java_count - unit_tests - component_tests - integration_tests))
    
    # Adjust counts to ensure all tests are accounted for
    java_counts["unit"]=$((java_counts["unit"] + unit_tests + remaining_tests))
    total_counts["unit"]=$((total_counts["unit"] + unit_tests + remaining_tests))
    
    java_counts["component"]=$((java_counts["component"] + component_tests))
    total_counts["component"]=$((total_counts["component"] + component_tests))
    
    java_counts["integration"]=$((java_counts["integration"] + integration_tests))
    total_counts["integration"]=$((total_counts["integration"] + integration_tests))
    
    echo "Categorized $unit_tests additional unit tests"
    echo "Categorized $component_tests additional component tests"
    echo "Categorized $integration_tests additional integration tests"
    echo "Categorized $remaining_tests remaining tests as unit tests"
  fi
}

# Run categorization
categorize_uncategorized_java_tests

for category in "${!test_categories[@]}"; do
  java_total=$((java_total + java_counts[$category]))
  go_total=$((go_total + go_counts[$category]))
  python_total=$((python_total + python_counts[$category]))
  grand_total=$((grand_total + total_counts[$category]))
done

# Generate JSON output if requested
if [ "$JSON_OUTPUT" = true ]; then
  json_output="{
  \"summary\": {
    \"total\": $grand_total,
    \"byLanguage\": {
      \"java\": $java_total,
      \"go\": $go_total,
      \"python\": $python_total
    }
  },
  \"byCategory\": {"
  
  first=true
  for category in "${!test_categories[@]}"; do
    if [ "$first" = true ]; then
      first=false
    else
      json_output+=","
    fi
    
    json_output+="
    \"${test_categories[$category]}\": {
      \"total\": ${total_counts[$category]},
      \"byLanguage\": {
        \"java\": ${java_counts[$category]},
        \"go\": ${go_counts[$category]},
        \"python\": ${python_counts[$category]}
      }
    }"
  done
  
  json_output+="
  }
}"
  
  if [ -n "$OUTPUT_FILE" ]; then
    echo "$json_output" > "$OUTPUT_FILE"
    echo "JSON output written to $OUTPUT_FILE"
  else
    echo "$json_output"
  fi
  
  exit 0
fi

# Generate standard text output
echo
echo "========================================"
echo "📊 Test Pyramid Coverage Report"
echo "========================================"
echo
echo "Total Tests: $grand_total"
echo "  Java:   $java_total"
echo "  Go:     $go_total"
echo "  Python: $python_total"
echo

# Print the pyramid ASCII art with counts
pyramid_width=70

print_pyramid_level() {
  local level="$1"
  local count="$2"
  local label="$3"
  local width=$4
  
  local spaces=$(( (width - count) / 2 ))
  local stars=$count
  
  if [ $stars -lt 10 ]; then
    # Ensure minimum width for visibility
    stars=10
    spaces=$(( (width - stars) / 2 ))
  fi
  
  printf "%8s │ " "$level"
  printf "%${spaces}s" ""
  printf "%${stars}s" "" | tr ' ' '▓'
  printf "%${spaces}s" ""
  # Show the actual count, even if the visualization is scaled for small numbers
  printf " │ %d %s tests\n" "$count" "$label"
}

# Note: The visualization may show minimums (5) for small numbers to ensure visibility
# but the count next to each bar shows the actual test count
echo "        Test Pyramid Distribution (scaled for visualization)"
echo "        ┌$(printf -- "─%.0s" $(seq 1 $pyramid_width))┐"

# Print each level from the top down
categories=("performance" "acceptance" "integration" "component" "unit")

# Find max count for proportional scaling
max_count=0
for category in "${categories[@]}"; do
  if [ ${total_counts[$category]} -gt $max_count ]; then
    max_count=${total_counts[$category]}
  fi
done

# Calculate scale factor if needed
scale_factor=1
if [ $max_count -gt $pyramid_width ]; then
  scale_factor=$(( (max_count + pyramid_width - 1) / pyramid_width ))
fi

for category in "${categories[@]}"; do
  count=${total_counts[$category]}
  label=${test_categories[$category]}
  
  # Apply scaling for visualization while preserving proportions
  if [ $count -eq 0 ]; then
    display_count=0
  elif [ $count -lt 5 ] && [ $count -gt 0 ]; then
    # Ensure tiny values are at least minimally visible
    display_count=5
  # Here we're using our scale factor, but note that the breakdown table will still show real counts
  elif [ $max_count -gt $pyramid_width ]; then
    # Scale all values proportionally based on max_count
    display_count=$(( count * pyramid_width / max_count ))
    # Ensure non-zero values have at least minimal visibility
    [ $display_count -eq 0 ] && [ $count -ne 0 ] && display_count=5
  else
    display_count=$count
  fi
  
  print_pyramid_level "$label" $display_count "$label" $pyramid_width
done

echo "        └$(printf -- "─%.0s" $(seq 1 $pyramid_width))┘"
echo "          Fewer $(printf -- " %.0s" $(seq 1 $(( pyramid_width / 2 - 12 )))) More"
echo

# Print detailed breakdown by language and category
echo "Breakdown by Test Level and Language:"
echo "--------------------------------------"
printf "%-15s | %-10s | %-10s | %-10s | %-10s\n" "Test Level" "Java" "Go" "Python" "Total"
echo "--------------------------------------"

for category in "${categories[@]}"; do
  label=${test_categories[$category]}
  java_count=${java_counts[$category]}
  go_count=${go_counts[$category]}
  python_count=${python_counts[$category]}
  total=${total_counts[$category]}
  
  printf "%-15s | %-10d | %-10d | %-10d | %-10d\n" "$label" $java_count $go_count $python_count $total
done

echo "--------------------------------------"
printf "%-15s | %-10d | %-10d | %-10d | %-10d\n" "Total" $java_total $go_total $python_total $grand_total
echo

# Generate recommendations based on the pyramid structure
generate_recommendations() {
  local recommendations=()
  
  # Define target percentages for an ideal test pyramid
  local target_unit_percent=60
  local target_component_percent=25
  local target_integration_percent=10
  local target_acceptance_percent=4
  local target_performance_percent=1
  
  # Calculate actual percentages
  local actual_unit_percent=$unit_percent
  local actual_component_percent=$component_percent
  local actual_integration_percent=$integration_percent
  local actual_acceptance_percent=$acceptance_percent
  local actual_performance_percent=$performance_percent
  
  # Calculate the gap between actual and target percentages
  local unit_gap=$((target_unit_percent - actual_unit_percent))
  local component_gap=$((target_component_percent - actual_component_percent))
  local integration_gap=$((target_integration_percent - actual_integration_percent))
  local acceptance_gap=$((target_acceptance_percent - actual_acceptance_percent))
  local performance_gap=$((target_performance_percent - actual_performance_percent))
  
  # Assess the pyramid structure based on data-driven insights
  
  # Check for pyramid inversions (lower layer has fewer tests than higher layer)
  if [ ${total_counts["unit"]} -lt ${total_counts["component"]} ]; then
    recommendations+=("🔺 Pyramid Inversion: Unit tests ($actual_unit_percent%) should outnumber Component tests ($actual_component_percent%). Add at least ${unit_gap}% more unit tests.")
  fi
  
  if [ ${total_counts["component"]} -lt ${total_counts["integration"]} ]; then
    recommendations+=("🔺 Pyramid Inversion: Component tests ($actual_component_percent%) should outnumber Integration tests ($actual_integration_percent%). Add more component tests.")
  fi
  
  if [ ${total_counts["integration"]} -lt ${total_counts["acceptance"]} ]; then
    recommendations+=("🔺 Pyramid Inversion: Integration tests ($actual_integration_percent%) should outnumber Acceptance tests ($actual_acceptance_percent%). Add more integration tests.")
  fi
  
  # Check for significant test gaps from target percentages
  if [ $unit_gap -gt 10 ]; then
    local unit_deficit=$((target_unit_percent * grand_total / 100 - total_counts["unit"]))
    recommendations+=("📉 Unit Test Gap: Current unit tests are $unit_gap% below target. Consider adding ~$unit_deficit more unit tests.")
  fi
  
  if [ $component_gap -gt 5 ]; then
    local component_deficit=$((target_component_percent * grand_total / 100 - total_counts["component"]))
    recommendations+=("📉 Component Test Gap: Current component tests are $component_gap% below target. Consider adding ~$component_deficit more component tests.")
  fi
  
  if [ $integration_gap -gt 5 ]; then
    local integration_deficit=$((target_integration_percent * grand_total / 100 - total_counts["integration"]))
    recommendations+=("📉 Integration Test Gap: Current integration tests are $integration_gap% below target. Consider adding ~$integration_deficit more integration tests.")
  fi
  
  # Check for disproportionate test types
  if [ $actual_acceptance_percent -gt $((2 * target_acceptance_percent)) ]; then
    recommendations+=("⚠️ High Acceptance Test Ratio: Acceptance tests are $((actual_acceptance_percent - target_acceptance_percent))% above target. Ensure they're supported by sufficient unit and integration tests.")
  fi
  
  # Cross-language test coverage analysis
  if [ $java_total -eq 0 ] || [ $go_total -eq 0 ] || [ $python_total -eq 0 ]; then
    # Identify which languages are missing tests
    local missing_langs=""
    [ $java_total -eq 0 ] && missing_langs+="Java "
    [ $go_total -eq 0 ] && missing_langs+="Go "
    [ $python_total -eq 0 ] && missing_langs+="Python "
    
    recommendations+=("🌐 Incomplete Cross-language Coverage: Add tests for $missing_langs to ensure comprehensive testing across all languages.")
  fi
  
  # Analysis of language-specific test distribution
  for lang in "java" "go" "python"; do
    declare -n counts="${lang}_counts"
    total_var="${lang}_total"
    lang_total=${!total_var}
    
    if [ $lang_total -gt 0 ]; then
      # Check for missing test categories
      for category in "${!test_categories[@]}"; do
        if [ ${counts[$category]} -eq 0 ]; then
          title=$(echo "$lang" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
          recommendations+=("🚫 Missing ${test_categories[$category]} Tests in $title: Add ${test_categories[$category]} tests to the $title codebase")
        fi
      done
      
      # Check for language-specific pyramid structure
      if [ ${counts["unit"]} -lt ${counts["component"]} ] || 
         [ ${counts["component"]} -lt ${counts["integration"]} ] || 
         [ ${counts["integration"]} -lt ${counts["acceptance"]} ]; then
        title=$(echo "$lang" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
        recommendations+=("🔺 $title Pyramid Structure Issue: The test pyramid for $title code is inverted in at least one layer.")
      fi
    fi
  done
  
  # Module-specific coverage analysis
  cli_test_count=$(find rinna-cli -type f -name "*Test.java" 2>/dev/null | wc -l)
  if [ $cli_test_count -eq 0 ]; then
    recommendations+=("📊 Missing CLI Tests: Add tests for the CLI module (rinna-cli)")
  fi
  
  core_test_count=$(find rinna-core -type f -name "*Test.java" 2>/dev/null | wc -l)
  if [ $core_test_count -eq 0 ]; then
    recommendations+=("📊 Missing Core Tests: Add tests for the core module (rinna-core)")
  fi
  
  api_test_count=$(find api -type f -name "*_test.go" 2>/dev/null | wc -l)
  if [ $api_test_count -lt 5 ]; then
    recommendations+=("📊 Low API Test Coverage: Increase test coverage for the API module")
  fi
  
  # Overall assessment
  echo "Test Pyramid Analysis:"
  echo "--------------------------------------"
  
  # Print the target distribution for reference
  echo "Target Distribution: Unit ($target_unit_percent%), Component ($target_component_percent%), Integration ($target_integration_percent%), Acceptance ($target_acceptance_percent%), Performance ($target_performance_percent%)"
  echo "Current Distribution: Unit ($actual_unit_percent%), Component ($actual_component_percent%), Integration ($actual_integration_percent%), Acceptance ($actual_acceptance_percent%), Performance ($actual_performance_percent%)"
  echo ""
  
  if [ ${#recommendations[@]} -eq 0 ]; then
    echo "✅ Your test pyramid structure looks good! The distribution of tests across types and languages follows best practices."
  else
    echo "Recommendations for improvement:"
    for rec in "${recommendations[@]}"; do
      echo "$rec"
    done
  fi
}

# Calculate percentages for recommendations
if [ $grand_total -gt 0 ]; then
  unit_percent=$(( ${total_counts["unit"]} * 100 / grand_total ))
  component_percent=$(( ${total_counts["component"]} * 100 / grand_total ))
  integration_percent=$(( ${total_counts["integration"]} * 100 / grand_total ))
  acceptance_percent=$(( ${total_counts["acceptance"]} * 100 / grand_total ))
  performance_percent=$(( ${total_counts["performance"]} * 100 / grand_total ))
  
  generate_recommendations
fi

# Write to output file if specified
if [ -n "$OUTPUT_FILE" ]; then
  # Redirect all output to the file
  exec > "$OUTPUT_FILE"
  
  echo "========================================"
  echo "📊 Test Pyramid Coverage Report"
  echo "========================================"
  echo 
  echo "Generated: $(date)"
  echo "Project: Rinna"
  echo
  echo "Total Tests: $grand_total"
  echo "  Java:   $java_total"
  echo "  Go:     $go_total"
  echo "  Python: $python_total"
  echo
  
  echo "Breakdown by Test Level and Language:"
  echo "--------------------------------------"
  printf "%-15s | %-10s | %-10s | %-10s | %-10s\n" "Test Level" "Java" "Go" "Python" "Total"
  echo "--------------------------------------"
  
  for category in "${categories[@]}"; do
    label=${test_categories[$category]}
    java_count=${java_counts[$category]}
    go_count=${go_counts[$category]}
    python_count=${python_counts[$category]}
    total=${total_counts[$category]}
    
    printf "%-15s | %-10d | %-10d | %-10d | %-10d\n" "$label" $java_count $go_count $python_count $total
  done
  
  echo "--------------------------------------"
  printf "%-15s | %-10d | %-10d | %-10d | %-10d\n" "Total" $java_total $go_total $python_total $grand_total
  echo
  
  # Generate recommendations
  if [ $grand_total -gt 0 ]; then
    generate_recommendations
  fi
  
  echo "Report written to $OUTPUT_FILE"
fi
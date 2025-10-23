#!/usr/bin/env bash


# Check if Micronaut CLI is available
if ! command -v mn >/dev/null 2>&1; then
  echo "❌ Error: Micronaut CLI (mn) not found."
  echo "Install it with: sdk install micronaut"
  exit 1
fi

# Check arguments
if [ -z "$1" ]; then
  echo "Usage: $0 <keyword> [starter] [--exact]"
  echo
  echo "Examples:"
  echo "  $0 data"
  echo "  $0 data create-cli-app"
  echo "  $0 data create-app --exact"
  exit 1
fi

QUERY=$1
SELECTED_STARTER=$2
EXACT=0

# Detect if --exact flag was passed
if [ "$2" == "--exact" ] || [ "$3" == "--exact" ]; then
  EXACT=1
fi

# Known starters
STARTERS=(
  "create-app"
  "create-cli-app"
  "create-function-app"
  "create-grpc-app"
  "create-messaging-app"
)

STARTER_PROVIDED=0
# Check if a specific starter was provided
if [ -n "$SELECTED_STARTER" ] && [ "$SELECTED_STARTER" != "--exact" ]; then
  if ! printf '%s\n' "${STARTERS[@]}" | grep -qx "$SELECTED_STARTER"; then
    echo "Unknown starter: '$SELECTED_STARTER'"
    echo "Available starters are: ${STARTERS[*]}"
    exit 1
  fi
  STARTERS=("$SELECTED_STARTER")
  STARTER_PROVIDED=1
fi

FOUND=0

if [ $STARTER_PROVIDED -eq 1 ]; then
  echo "🔍 Searching for feature '${QUERY}' in '${SELECTED_STARTER}' starter..."
else
  echo "🔍 Searching for feature '${QUERY}' in Micronaut starters..."
fi

echo

# Loop through starters
for starter in "${STARTERS[@]}"; do
  FEATURES=$(mn $starter --list-features 2>/dev/null)
  if [ -z "$FEATURES" ]; then
    echo " No features found for '$starter'."
    continue
  fi

  if [ $EXACT -eq 1 ]; then
    MATCHES=$(echo "$FEATURES" | grep -iE "^[[:space:]]*$QUERY[[:space:]]" )
  else
    MATCHES=$(echo "$FEATURES" | grep -i "$QUERY")
  fi

  if [ -n "$MATCHES" ]; then
    FOUND=1
    echo "$starter:"
    echo "$MATCHES"
    echo
  fi
done

# Results
if [ $FOUND -eq 0 ]; then
  echo "No features matching '${QUERY}' found in selected starters."
fi

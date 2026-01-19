#!/bin/bash

# Script to create an admin user in the database
# Usage: ./scripts/create-admin.sh [email] [password] [name]
# Example: ./scripts/create-admin.sh admin@lebvest.com admin123 "Admin User"

set -e

EMAIL="${1:-admin@lebvest.com}"
PASSWORD="${2:-admin123}"
NAME="${3:-Admin User}"

echo "========================================="
echo "Creating Admin User"
echo "========================================="
echo "Email: $EMAIL"
echo "Name: $NAME"
echo "Profile: ${SPRING_PROFILES_ACTIVE:-dev}"
echo ""

# Check if we're in the project root
if [ ! -f "pom.xml" ]; then
    echo "Error: Must run from project root directory"
    exit 1
fi

# Set profile to dev if not set
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

# Check if Maven wrapper exists
if [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
else
    echo "Error: Maven not found. Please install Maven or use the Maven wrapper."
    exit 1
fi

# Build if needed (check for jar or if target doesn't exist)
if [ ! -f "target/lebvest-0.0.1-SNAPSHOT.jar" ]; then
    echo "Building application..."
    $MVN_CMD clean package -DskipTests -q
    echo ""
fi

# Run the application with the admin creation arguments
echo "Running application to create admin user..."
java -jar target/lebvest-0.0.1-SNAPSHOT.jar \
    --app.create-admin=true \
    "$EMAIL" "$PASSWORD" "$NAME" \
    --spring.main.web-application-type=none

echo ""
echo "========================================="
echo "Admin user creation completed!"
echo "========================================="


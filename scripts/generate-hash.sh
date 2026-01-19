#!/bin/bash
# Quick script to generate BCrypt hash for admin123

cd "$(dirname "$0")/.." || exit 1

./mvnw exec:java -Dexec.mainClass="com.lebvest.util.PasswordHashGenerator" -Dexec.args="admin123" -q 2>&1 | grep "BCrypt Hash" | cut -d: -f2 | tr -d ' '


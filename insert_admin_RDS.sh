#!/bin/bash
# Script to insert admin user into RDS database
# This explicitly uses the RDS endpoint to avoid inserting into localhost

echo "=========================================="
echo "Inserting Admin User into RDS Database"
echo "=========================================="
echo ""
echo "RDS Endpoint: lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com"
echo "Database: lebvest"
echo ""

mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      lebvest < "$(dirname "$0")/insert_admin.sql"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Admin user inserted successfully into RDS!"
    echo ""
    echo "Verifying insertion..."
    mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
          -P 3306 \
          -u admin \
          -p \
          lebvest -e "SELECT u.id, u.name, u.email, u.enabled, GROUP_CONCAT(ur.role) as roles FROM users u LEFT JOIN user_roles ur ON u.id = ur.user_id WHERE u.email = 'bousleimengeorgio139@gmail.com' GROUP BY u.id;"
else
    echo ""
    echo "❌ Error inserting admin user. Check the error message above."
fi

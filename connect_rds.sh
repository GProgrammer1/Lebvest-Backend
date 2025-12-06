#!/bin/bash
# Quick script to connect to RDS MySQL database
# Usage: ./connect_rds.sh

RDS_HOST="lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com"
RDS_PORT="3306"
RDS_DB="lebvest"
RDS_USER="admin"

echo "=========================================="
echo "Connecting to RDS MySQL Database"
echo "=========================================="
echo ""
echo "Host: $RDS_HOST"
echo "Port: $RDS_PORT"
echo "Database: $RDS_DB"
echo "Username: $RDS_USER"
echo ""
echo "You will be prompted for the password."
echo "Check your .env file for DB_PASSWORD"
echo ""
echo "=========================================="
echo ""

mysql -h "$RDS_HOST" \
      -P "$RDS_PORT" \
      -u "$RDS_USER" \
      -p \
      "$RDS_DB"

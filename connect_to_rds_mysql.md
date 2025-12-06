# How to Connect to RDS MySQL Database via Command Line

## RDS Connection Details

- **Host**: `lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com`
- **Port**: `3306` (default MySQL port)
- **Database**: `lebvest`
- **Username**: `admin` (or check your `.env` file for `DB_USERNAME`)
- **Password**: Check your `.env` file for `DB_PASSWORD`

## Prerequisites

Make sure you have MySQL client installed on your system:

```bash
# Check if MySQL client is installed
mysql --version

# If not installed, install it:
# On Fedora/RHEL:
sudo dnf install mysql

# On Ubuntu/Debian:
sudo apt-get install mysql-client

# On macOS:
brew install mysql-client
```

## Connection Methods

### Method 1: Interactive MySQL Command Line

```bash
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      lebvest
```

You will be prompted to enter the password. Enter the password from your `.env` file (`DB_PASSWORD`).

### Method 2: With Password in Command (Less Secure)

```bash
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -pYOUR_PASSWORD_HERE \
      lebvest
```

**Note**: No space after `-p` when including password directly.

### Method 3: Using Environment Variable

```bash
# Set password as environment variable
export MYSQL_PWD="your_password_here"

# Connect (password will be read from environment)
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      lebvest
```

### Method 4: Execute SQL File Directly

```bash
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      lebvest < /path/to/your/query.sql
```

## Example: Running the Verification Queries

Once connected, you can run the SQL queries we created:

```bash
# Connect to RDS
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      lebvest

# Then inside MySQL prompt, run:
source /home/georgio/Documents/Projects/Lebvest/lebvest-backend/check_fully_verified_companies.sql;
source /home/georgio/Documents/Projects/Lebvest/lebvest-backend/check_half_completed_companies.sql;
```

Or execute directly from command line:

```bash
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      lebvest < /home/georgio/Documents/Projects/Lebvest/lebvest-backend/check_fully_verified_companies.sql
```

## Quick Reference Commands

Once connected to MySQL, useful commands:

```sql
-- Show all databases
SHOW DATABASES;

-- Use a specific database
USE lebvest;

-- Show all tables
SHOW TABLES;

-- Show table structure
DESCRIBE companies;
DESCRIBE company_verification_documents;

-- Exit MySQL
EXIT;
-- or
QUIT;
```

## Troubleshooting

### Connection Timeout
- Check your security group settings in AWS RDS console
- Ensure your IP is allowed in the security group
- Verify the RDS instance is running

### Access Denied
- Double-check username and password
- Verify the user has proper permissions
- Check if password has special characters that need escaping

### SSL Connection Issues
If you encounter SSL errors, you can disable SSL (not recommended for production):

```bash
mysql -h lebvest.cc5w6c8uk08j.us-east-1.rds.amazonaws.com \
      -P 3306 \
      -u admin \
      -p \
      --ssl-mode=DISABLED \
      lebvest
```

## Security Notes

⚠️ **Important Security Considerations:**

1. Never commit passwords to version control
2. Use environment variables or `.env` files for credentials
3. Prefer interactive password prompts over command-line passwords
4. Use SSL connections in production (default for RDS)
5. Limit database access to necessary IP addresses only

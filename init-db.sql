CREATE DATABASE IF NOT EXISTS merchant_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS transaction_db;
CREATE DATABASE IF NOT EXISTS fraud_db;
CREATE DATABASE IF NOT EXISTS settlement_db;
CREATE DATABASE IF NOT EXISTS notification_db;

-- Grant permissions
GRANT ALL PRIVILEGES ON merchant_db.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON transaction_db.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON fraud_db.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON settlement_db.* TO 'payment_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'payment_user'@'%';
FLUSH PRIVILEGES;

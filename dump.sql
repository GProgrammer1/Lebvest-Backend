/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19  Distrib 10.11.11-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: lebvest
-- ------------------------------------------------------
-- Server version	10.11.11-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_notifications`
--

DROP TABLE IF EXISTS `admin_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_notifications` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_accepted` bit(1) DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('APP_STAT_UPDATE','PROJECT_PROPOSAL','SIGNUP_REQUEST') DEFAULT NULL,
  `admin_id` bigint(20) NOT NULL,
  `request_id` bigint(20) DEFAULT NULL,
  `company_id` bigint(20) DEFAULT NULL,
  `investment_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm1ffj3g1eh6tuctp155uy3k9w` (`request_id`),
  KEY `FKr96abhhnpnthfk7p652l4m3gn` (`admin_id`),
  KEY `FK89tycfx37ms7ucg84efjk7foj` (`company_id`),
  KEY `FKa35fal4d22cv9omdb9g3g66ln` (`investment_id`),
  CONSTRAINT `FK89tycfx37ms7ucg84efjk7foj` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`),
  CONSTRAINT `FKa35fal4d22cv9omdb9g3g66ln` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`),
  CONSTRAINT `FKgspp6wmcfkdooe9aejesos3x2` FOREIGN KEY (`request_id`) REFERENCES `company_signup_requests` (`id`),
  CONSTRAINT `FKr96abhhnpnthfk7p652l4m3gn` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_notifications`
--

LOCK TABLES `admin_notifications` WRITE;
/*!40000 ALTER TABLE `admin_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin_notifications_seq`
--

DROP TABLE IF EXISTS `admin_notifications_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_notifications_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_notifications_seq`
--

LOCK TABLES `admin_notifications_seq` WRITE;
/*!40000 ALTER TABLE `admin_notifications_seq` DISABLE KEYS */;
INSERT INTO `admin_notifications_seq` VALUES
(151);
/*!40000 ALTER TABLE `admin_notifications_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `companies`
--

DROP TABLE IF EXISTS `companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies` (
  `id` bigint(20) NOT NULL,
  `description` longtext DEFAULT NULL,
  `founded_year` int(11) NOT NULL,
  `location` varchar(512) DEFAULT NULL,
  `logo` varchar(512) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `sector` tinyint(4) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `city` varchar(255) DEFAULT NULL,
  `custom_sector` varchar(255) DEFAULT NULL,
  `governorate` varchar(255) DEFAULT NULL,
  `phone_number` varchar(50) DEFAULT NULL,
  `website` varchar(512) DEFAULT NULL,
  `status` enum('APPROVED','FULLY_VERIFIED','PENDING','PENDING_DOCS','REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK50ygfritln653mnfhxucoy8up` (`name`),
  UNIQUE KEY `UK5xg6ed73n32iai9psir68pia9` (`user_id`),
  CONSTRAINT `FK9l5d0fem75e59uwf9upwuf9du` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `companies`
--

LOCK TABLES `companies` WRITE;
/*!40000 ALTER TABLE `companies` DISABLE KEYS */;
/*!40000 ALTER TABLE `companies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `companies_seq`
--

DROP TABLE IF EXISTS `companies_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `companies_seq`
--

LOCK TABLES `companies_seq` WRITE;
/*!40000 ALTER TABLE `companies_seq` DISABLE KEYS */;
INSERT INTO `companies_seq` VALUES
(451);
/*!40000 ALTER TABLE `companies_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_bank_statements`
--

DROP TABLE IF EXISTS `company_bank_statements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_bank_statements` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FKok3slsggktai623luxne4r2qt` (`verification_docs_id`),
  CONSTRAINT `FKok3slsggktai623luxne4r2qt` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_bank_statements`
--

LOCK TABLES `company_bank_statements` WRITE;
/*!40000 ALTER TABLE `company_bank_statements` DISABLE KEYS */;
INSERT INTO `company_bank_statements` VALUES
(1,'uploads/companies/353/documents/1764531040113_Murex.png');
/*!40000 ALTER TABLE `company_bank_statements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_director_ids`
--

DROP TABLE IF EXISTS `company_director_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_director_ids` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FKakj41e8qqs70hm3mgx8opnahh` (`verification_docs_id`),
  CONSTRAINT `FKakj41e8qqs70hm3mgx8opnahh` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_director_ids`
--

LOCK TABLES `company_director_ids` WRITE;
/*!40000 ALTER TABLE `company_director_ids` DISABLE KEYS */;
INSERT INTO `company_director_ids` VALUES
(1,'uploads/companies/353/documents/1764531004102_structured-cabling-1.jpg');
/*!40000 ALTER TABLE `company_director_ids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_documents`
--

DROP TABLE IF EXISTS `company_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_documents` (
  `company_id` bigint(20) NOT NULL,
  `document` varchar(255) DEFAULT NULL,
  KEY `FK28n811hlbfv9ql27ujm80si1x` (`company_id`),
  CONSTRAINT `FK28n811hlbfv9ql27ujm80si1x` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_documents`
--

LOCK TABLES `company_documents` WRITE;
/*!40000 ALTER TABLE `company_documents` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_financial_statements`
--

DROP TABLE IF EXISTS `company_financial_statements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_financial_statements` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FKa9vbiojmpdive3fv53pg80suj` (`verification_docs_id`),
  CONSTRAINT `FKa9vbiojmpdive3fv53pg80suj` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_financial_statements`
--

LOCK TABLES `company_financial_statements` WRITE;
/*!40000 ALTER TABLE `company_financial_statements` DISABLE KEYS */;
INSERT INTO `company_financial_statements` VALUES
(1,'uploads/companies/353/documents/1764531032123_webdev.jpg');
/*!40000 ALTER TABLE `company_financial_statements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_financials`
--

DROP TABLE IF EXISTS `company_financials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_financials` (
  `id` bigint(20) NOT NULL,
  `expenses` decimal(15,2) NOT NULL,
  `profit` decimal(15,2) NOT NULL,
  `revenue` decimal(15,2) NOT NULL,
  `year` int(11) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cf_company` (`company_id`),
  CONSTRAINT `FKe87crhi35qhkjgg7aormw3oby` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_financials`
--

LOCK TABLES `company_financials` WRITE;
/*!40000 ALTER TABLE `company_financials` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_financials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_financials_seq`
--

DROP TABLE IF EXISTS `company_financials_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_financials_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_financials_seq`
--

LOCK TABLES `company_financials_seq` WRITE;
/*!40000 ALTER TABLE `company_financials_seq` DISABLE KEYS */;
INSERT INTO `company_financials_seq` VALUES
(101);
/*!40000 ALTER TABLE `company_financials_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_investors`
--

DROP TABLE IF EXISTS `company_investors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_investors` (
  `id` bigint(20) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `invested_at` date NOT NULL,
  `company_id` bigint(20) NOT NULL,
  `investor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ci_company` (`company_id`),
  KEY `FKqek6ncb9hogfkj58a9tyhtfpn` (`investor_id`),
  CONSTRAINT `FKqek6ncb9hogfkj58a9tyhtfpn` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`),
  CONSTRAINT `FKsr7gwp7fx1gk49737vd3iqj3s` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_investors`
--

LOCK TABLES `company_investors` WRITE;
/*!40000 ALTER TABLE `company_investors` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_investors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_investors_seq`
--

DROP TABLE IF EXISTS `company_investors_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_investors_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_investors_seq`
--

LOCK TABLES `company_investors_seq` WRITE;
/*!40000 ALTER TABLE `company_investors_seq` DISABLE KEYS */;
INSERT INTO `company_investors_seq` VALUES
(1);
/*!40000 ALTER TABLE `company_investors_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_management_accounts`
--

DROP TABLE IF EXISTS `company_management_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_management_accounts` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FK2ta25312yg2xcs5y2kmgs7wsx` (`verification_docs_id`),
  CONSTRAINT `FK2ta25312yg2xcs5y2kmgs7wsx` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_management_accounts`
--

LOCK TABLES `company_management_accounts` WRITE;
/*!40000 ALTER TABLE `company_management_accounts` DISABLE KEYS */;
INSERT INTO `company_management_accounts` VALUES
(1,'uploads/companies/353/documents/1764531037461_machine-learning.jpg');
/*!40000 ALTER TABLE `company_management_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_notifications`
--

DROP TABLE IF EXISTS `company_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_notifications` (
  `id` varchar(36) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `message` tinytext NOT NULL,
  `notified_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('ADMIN_MESSAGE','FUNDING_MILESTONE','INVESTOR_INQUIRY','INVESTOR_REQUEST') NOT NULL,
  `company_id` bigint(20) NOT NULL,
  `related_investment_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_company_notifs` (`company_id`),
  KEY `FKpik4shgvtw7d5y2v01qx3mdii` (`related_investment_id`),
  CONSTRAINT `FKma13xndn80b9c44qaagrmp3u4` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`),
  CONSTRAINT `FKpik4shgvtw7d5y2v01qx3mdii` FOREIGN KEY (`related_investment_id`) REFERENCES `investments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_notifications`
--

LOCK TABLES `company_notifications` WRITE;
/*!40000 ALTER TABLE `company_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_signatory_ids`
--

DROP TABLE IF EXISTS `company_signatory_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_signatory_ids` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FKkjdmivjtloj5lxlle56yfvkvj` (`verification_docs_id`),
  CONSTRAINT `FKkjdmivjtloj5lxlle56yfvkvj` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_signatory_ids`
--

LOCK TABLES `company_signatory_ids` WRITE;
/*!40000 ALTER TABLE `company_signatory_ids` DISABLE KEYS */;
INSERT INTO `company_signatory_ids` VALUES
(1,'uploads/companies/353/documents/1764531007936_machine-learning.jpg'),
(1,'uploads/companies/353/documents/1764531023136_1594918982837479-bel1-t-1024x616.jpg');
/*!40000 ALTER TABLE `company_signatory_ids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_signup_requests`
--

DROP TABLE IF EXISTS `company_signup_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_signup_requests` (
  `id` bigint(20) NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` longtext DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `founded_year` int(11) NOT NULL,
  `location` varchar(512) DEFAULT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `request_id` binary(16) NOT NULL,
  `request_status` tinyint(4) NOT NULL,
  `sector` tinyint(4) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `custom_sector` varchar(255) DEFAULT NULL,
  `governorate` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) NOT NULL,
  `website` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtpfrmejvar19f0nm5nmjni123` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_signup_requests`
--

LOCK TABLES `company_signup_requests` WRITE;
/*!40000 ALTER TABLE `company_signup_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_signup_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_signup_requests_seq`
--

DROP TABLE IF EXISTS `company_signup_requests_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_signup_requests_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_signup_requests_seq`
--

LOCK TABLES `company_signup_requests_seq` WRITE;
/*!40000 ALTER TABLE `company_signup_requests_seq` DISABLE KEYS */;
INSERT INTO `company_signup_requests_seq` VALUES
(201);
/*!40000 ALTER TABLE `company_signup_requests_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_social_media`
--

DROP TABLE IF EXISTS `company_social_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_social_media` (
  `facebook` varchar(255) DEFAULT NULL,
  `instagram` varchar(255) DEFAULT NULL,
  `linkedin` varchar(255) DEFAULT NULL,
  `twitter` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `company_id` bigint(20) NOT NULL,
  PRIMARY KEY (`company_id`),
  CONSTRAINT `FK6tiao6m7vr4yd238tp24waj9p` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_social_media`
--

LOCK TABLES `company_social_media` WRITE;
/*!40000 ALTER TABLE `company_social_media` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_social_media` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_team_members`
--

DROP TABLE IF EXISTS `company_team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_team_members` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bio` tinytext NOT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ctm_company` (`company_id`),
  CONSTRAINT `FKj30lugqvu6hx3p4fv6k9hglsd` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_team_members`
--

LOCK TABLES `company_team_members` WRITE;
/*!40000 ALTER TABLE `company_team_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `company_team_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_ubo_ids`
--

DROP TABLE IF EXISTS `company_ubo_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_ubo_ids` (
  `verification_docs_id` bigint(20) NOT NULL,
  `document_path` varchar(255) DEFAULT NULL,
  KEY `FKl1u48uov1oc2d6ey7ejfli2ly` (`verification_docs_id`),
  CONSTRAINT `FKl1u48uov1oc2d6ey7ejfli2ly` FOREIGN KEY (`verification_docs_id`) REFERENCES `company_verification_documents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_ubo_ids`
--

LOCK TABLES `company_ubo_ids` WRITE;
/*!40000 ALTER TABLE `company_ubo_ids` DISABLE KEYS */;
INSERT INTO `company_ubo_ids` VALUES
(1,'uploads/companies/353/documents/1764531001845_structured-cabling-1.jpg');
/*!40000 ALTER TABLE `company_ubo_ids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_verification_documents`
--

DROP TABLE IF EXISTS `company_verification_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_verification_documents` (
  `id` bigint(20) NOT NULL,
  `articles_of_association` varchar(512) DEFAULT NULL,
  `bank_account_confirmation` varchar(512) DEFAULT NULL,
  `board_resolution` varchar(512) DEFAULT NULL,
  `certificate_of_incorporation` varchar(512) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_approved` bit(1) NOT NULL,
  `pep_sanctions_declaration` varchar(512) DEFAULT NULL,
  `proof_of_registered_address` varchar(512) DEFAULT NULL,
  `shareholder_structure` varchar(512) DEFAULT NULL,
  `source_of_funds_declaration` varchar(512) DEFAULT NULL,
  `tax_registration_certificate` varchar(512) DEFAULT NULL,
  `company_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs3yyhccfmalhxv08oucwxvvqh` (`company_id`),
  CONSTRAINT `FKfy2vnfaav29e7w4uwitqglqh7` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_verification_documents`
--

LOCK TABLES `company_verification_documents` WRITE;
/*!40000 ALTER TABLE `company_verification_documents` DISABLE KEYS */;
INSERT INTO `company_verification_documents` VALUES
(1,'uploads/companies/353/documents/1764530985113_software-engineering.jpg','uploads/companies/353/documents/1764531029896_software-engineering.jpg','uploads/companies/353/documents/1764531010499_webdev.jpg','uploads/companies/353/documents/1764530982522_software-engineering.jpg','2025-11-30 19:30:44.000000','\0','uploads/companies/353/documents/1764531013513_structured-cabling-1.jpg','uploads/companies/353/documents/1764530991765_software-engineering.jpg','uploads/companies/353/documents/1764530999593_software-engineering.jpg','uploads/companies/353/documents/1764531042728_structured-cabling-1.jpg','uploads/companies/353/documents/1764530989572_structured-cabling-1.jpg',353);
/*!40000 ALTER TABLE `company_verification_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `company_verification_documents_seq`
--

DROP TABLE IF EXISTS `company_verification_documents_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_verification_documents_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `company_verification_documents_seq`
--

LOCK TABLES `company_verification_documents_seq` WRITE;
/*!40000 ALTER TABLE `company_verification_documents_seq` DISABLE KEYS */;
INSERT INTO `company_verification_documents_seq` VALUES
(51);
/*!40000 ALTER TABLE `company_verification_documents_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forgot_pass_tokens`
--

DROP TABLE IF EXISTS `forgot_pass_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `forgot_pass_tokens` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKil7sxette9mrs7q7d3ssxw75k` (`user_id`),
  CONSTRAINT `FKil7sxette9mrs7q7d3ssxw75k` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forgot_pass_tokens`
--

LOCK TABLES `forgot_pass_tokens` WRITE;
/*!40000 ALTER TABLE `forgot_pass_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `forgot_pass_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `forgot_pass_tokens_seq`
--

DROP TABLE IF EXISTS `forgot_pass_tokens_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `forgot_pass_tokens_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `forgot_pass_tokens_seq`
--

LOCK TABLES `forgot_pass_tokens_seq` WRITE;
/*!40000 ALTER TABLE `forgot_pass_tokens_seq` DISABLE KEYS */;
INSERT INTO `forgot_pass_tokens_seq` VALUES
(1);
/*!40000 ALTER TABLE `forgot_pass_tokens_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `funding_history`
--

DROP TABLE IF EXISTS `funding_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `funding_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `date` date NOT NULL,
  `round` varchar(100) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fh_company` (`company_id`),
  CONSTRAINT `FKb9tk79ixcer5xp3yqcp83nisd` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `funding_history`
--

LOCK TABLES `funding_history` WRITE;
/*!40000 ALTER TABLE `funding_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `funding_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `funding_history_investors`
--

DROP TABLE IF EXISTS `funding_history_investors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `funding_history_investors` (
  `funding_history_id` bigint(20) NOT NULL,
  `investor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`funding_history_id`,`investor_id`),
  KEY `FK96odrwdsuhd7crsgm0odsmat2` (`investor_id`),
  CONSTRAINT `FK96odrwdsuhd7crsgm0odsmat2` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`),
  CONSTRAINT `FKondkvrixpuo8jy5g3xe7xv1ve` FOREIGN KEY (`funding_history_id`) REFERENCES `funding_history` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `funding_history_investors`
--

LOCK TABLES `funding_history_investors` WRITE;
/*!40000 ALTER TABLE `funding_history_investors` DISABLE KEYS */;
/*!40000 ALTER TABLE `funding_history_investors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_ai_predictions`
--

DROP TABLE IF EXISTS `investment_ai_predictions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_ai_predictions` (
  `investment_id` bigint(20) NOT NULL,
  `confidence_score` decimal(5,2) NOT NULL,
  `profit_prediction` decimal(5,2) NOT NULL,
  `risk_assessment` varchar(255) NOT NULL,
  PRIMARY KEY (`investment_id`),
  CONSTRAINT `FKl9ov0uqwgiw2paejmkv7yvojv` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_ai_predictions`
--

LOCK TABLES `investment_ai_predictions` WRITE;
/*!40000 ALTER TABLE `investment_ai_predictions` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_ai_predictions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_documents`
--

DROP TABLE IF EXISTS `investment_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_documents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `type` varchar(100) NOT NULL,
  `url` varchar(512) NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_idoc_investment` (`investment_id`),
  CONSTRAINT `FKheocstsjtn5grr28h6p8sjrso` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_documents`
--

LOCK TABLES `investment_documents` WRITE;
/*!40000 ALTER TABLE `investment_documents` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_financials`
--

DROP TABLE IF EXISTS `investment_financials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_financials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `expenses` decimal(15,2) NOT NULL,
  `profit` decimal(15,2) NOT NULL,
  `revenue` decimal(15,2) NOT NULL,
  `year` int(11) NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ifin_investment` (`investment_id`),
  CONSTRAINT `FKgve74sxgqs39n7ktpp0no4go9` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_financials`
--

LOCK TABLES `investment_financials` WRITE;
/*!40000 ALTER TABLE `investment_financials` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_financials` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_highlights`
--

DROP TABLE IF EXISTS `investment_highlights`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_highlights` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `highlight` tinytext NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ih_investment` (`investment_id`),
  CONSTRAINT `FK8jvyx88yhviuv5efwk2nhlxbd` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_highlights`
--

LOCK TABLES `investment_highlights` WRITE;
/*!40000 ALTER TABLE `investment_highlights` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_highlights` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_team_members`
--

DROP TABLE IF EXISTS `investment_team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_team_members` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bio` tinytext NOT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_itm_investment` (`investment_id`),
  CONSTRAINT `FKloxbgbbnu7tmivch90ph0r6fu` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_team_members`
--

LOCK TABLES `investment_team_members` WRITE;
/*!40000 ALTER TABLE `investment_team_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_team_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investment_updates`
--

DROP TABLE IF EXISTS `investment_updates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investment_updates` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` tinytext NOT NULL,
  `title` varchar(255) NOT NULL,
  `update_date` date NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iup_investment` (`investment_id`),
  CONSTRAINT `FK7tx09ai2g21owy2mefddue7xh` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investment_updates`
--

LOCK TABLES `investment_updates` WRITE;
/*!40000 ALTER TABLE `investment_updates` DISABLE KEYS */;
/*!40000 ALTER TABLE `investment_updates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investments`
--

DROP TABLE IF EXISTS `investments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investments` (
  `id` bigint(20) NOT NULL,
  `category` enum('AGRICULTURE','EDUCATION','ENERGY','GOVERNMENT_BONDS','HEALTHCARE','PERSONAL_PROJECT','REAL_ESTATE','RETAIL','SME','STARTUP','TECHNOLOGY','TOURISM') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deadline` date NOT NULL,
  `description` longtext DEFAULT NULL,
  `duration_months` int(11) NOT NULL,
  `expected_return` decimal(5,2) NOT NULL,
  `funding_stage` varchar(255) DEFAULT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `investment_type` enum('CROWDFUNDING','DEBT','EQUITY') NOT NULL,
  `location` enum('AKKAR','BAALBEK_HERMEL','BEIRUT','BEKAA','MOUNT_LEBANON','NABATIEH','NORTH','SOUTH') NOT NULL,
  `min_investment` decimal(15,2) NOT NULL,
  `raised_amount` decimal(15,2) NOT NULL,
  `risk_level` enum('HIGH','LOW','MEDIUM') NOT NULL,
  `target_amount` decimal(15,2) NOT NULL,
  `title` varchar(255) NOT NULL,
  `company_id` bigint(20) NOT NULL,
  `status` enum('APPROVED','DRAFT','PENDING_REVIEW','REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inv_company` (`company_id`),
  KEY `idx_inv_category` (`category`),
  CONSTRAINT `FKbon43rtapg0v46cl97r3129vp` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investments`
--

LOCK TABLES `investments` WRITE;
/*!40000 ALTER TABLE `investments` DISABLE KEYS */;
/*!40000 ALTER TABLE `investments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investments_seq`
--

DROP TABLE IF EXISTS `investments_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investments_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investments_seq`
--

LOCK TABLES `investments_seq` WRITE;
/*!40000 ALTER TABLE `investments_seq` DISABLE KEYS */;
INSERT INTO `investments_seq` VALUES
(151);
/*!40000 ALTER TABLE `investments_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_goals`
--

DROP TABLE IF EXISTS `investor_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_goals` (
  `id` bigint(20) NOT NULL,
  `current_amount` decimal(15,2) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `target_amount` decimal(15,2) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `investor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_goals_investor` (`investor_id`),
  CONSTRAINT `FKk162sd9yk22eq79bk6ycf5x91` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_goals`
--

LOCK TABLES `investor_goals` WRITE;
/*!40000 ALTER TABLE `investor_goals` DISABLE KEYS */;
/*!40000 ALTER TABLE `investor_goals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_goals_seq`
--

DROP TABLE IF EXISTS `investor_goals_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_goals_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_goals_seq`
--

LOCK TABLES `investor_goals_seq` WRITE;
/*!40000 ALTER TABLE `investor_goals_seq` DISABLE KEYS */;
INSERT INTO `investor_goals_seq` VALUES
(151);
/*!40000 ALTER TABLE `investor_goals_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_investments`
--

DROP TABLE IF EXISTS `investor_investments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_investments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `current_value` decimal(15,2) NOT NULL,
  `invested_at` date NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  `investor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ii_investor` (`investor_id`),
  KEY `idx_ii_investment` (`investment_id`),
  CONSTRAINT `FKbag36i5mleyou9m2x8a0cdx6e` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKnu2iin25qtker5vc1xq54w9uj` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_investments`
--

LOCK TABLES `investor_investments` WRITE;
/*!40000 ALTER TABLE `investor_investments` DISABLE KEYS */;
/*!40000 ALTER TABLE `investor_investments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_notifications`
--

DROP TABLE IF EXISTS `investor_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_notifications` (
  `id` bigint(20) NOT NULL,
  `investor_notification_type` tinyint(4) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(255) NOT NULL,
  `notified_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `investor_id` bigint(20) NOT NULL,
  `related_investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKteudqhjkxkc1prp5493coyxm2` (`investor_id`),
  UNIQUE KEY `UKn3oddmurioidjmxer835lbgu6` (`related_investment_id`),
  CONSTRAINT `FK4d2fohskib6vct2d5fj7lmlc` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKp49mwmqc2lh7c8oxwhv0pq4da` FOREIGN KEY (`related_investment_id`) REFERENCES `investments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_notifications`
--

LOCK TABLES `investor_notifications` WRITE;
/*!40000 ALTER TABLE `investor_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `investor_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_notifications_seq`
--

DROP TABLE IF EXISTS `investor_notifications_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_notifications_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_notifications_seq`
--

LOCK TABLES `investor_notifications_seq` WRITE;
/*!40000 ALTER TABLE `investor_notifications_seq` DISABLE KEYS */;
INSERT INTO `investor_notifications_seq` VALUES
(1);
/*!40000 ALTER TABLE `investor_notifications_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_pref_categories`
--

DROP TABLE IF EXISTS `investor_pref_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_pref_categories` (
  `investor_id` bigint(20) NOT NULL,
  `category` enum('AGRICULTURE','EDUCATION','ENERGY','GOVERNMENT_BONDS','HEALTHCARE','PERSONAL_PROJECT','REAL_ESTATE','RETAIL','SME','STARTUP','TECHNOLOGY','TOURISM') NOT NULL,
  PRIMARY KEY (`investor_id`,`category`),
  CONSTRAINT `FK90nxpt4oymq96gvt27ih30ore` FOREIGN KEY (`investor_id`) REFERENCES `investor_preferences` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_pref_categories`
--

LOCK TABLES `investor_pref_categories` WRITE;
/*!40000 ALTER TABLE `investor_pref_categories` DISABLE KEYS */;
INSERT INTO `investor_pref_categories` VALUES
(1,'PERSONAL_PROJECT'),
(2,'GOVERNMENT_BONDS'),
(2,'TECHNOLOGY'),
(3,'AGRICULTURE'),
(3,'EDUCATION'),
(3,'GOVERNMENT_BONDS'),
(3,'TOURISM'),
(52,'GOVERNMENT_BONDS'),
(102,'PERSONAL_PROJECT');
/*!40000 ALTER TABLE `investor_pref_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_pref_locations`
--

DROP TABLE IF EXISTS `investor_pref_locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_pref_locations` (
  `investor_id` bigint(20) NOT NULL,
  `location` enum('AKKAR','BAALBEK_HERMEL','BEIRUT','BEKAA','MOUNT_LEBANON','NABATIEH','NORTH','SOUTH') NOT NULL,
  PRIMARY KEY (`investor_id`,`location`),
  CONSTRAINT `FKt8evprkrxlm0e88mspb0470sk` FOREIGN KEY (`investor_id`) REFERENCES `investor_preferences` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_pref_locations`
--

LOCK TABLES `investor_pref_locations` WRITE;
/*!40000 ALTER TABLE `investor_pref_locations` DISABLE KEYS */;
INSERT INTO `investor_pref_locations` VALUES
(1,'BEIRUT'),
(1,'BEKAA'),
(1,'NORTH'),
(2,'BEIRUT'),
(3,'BEKAA'),
(3,'NORTH'),
(52,'BEIRUT'),
(102,'BEKAA'),
(102,'SOUTH');
/*!40000 ALTER TABLE `investor_pref_locations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_pref_risk_levels`
--

DROP TABLE IF EXISTS `investor_pref_risk_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_pref_risk_levels` (
  `investor_id` bigint(20) NOT NULL,
  `risk_level` enum('HIGH','LOW','MEDIUM') NOT NULL,
  PRIMARY KEY (`investor_id`,`risk_level`),
  CONSTRAINT `FK1ts7wnxah6my51nkeyo6elbol` FOREIGN KEY (`investor_id`) REFERENCES `investor_preferences` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_pref_risk_levels`
--

LOCK TABLES `investor_pref_risk_levels` WRITE;
/*!40000 ALTER TABLE `investor_pref_risk_levels` DISABLE KEYS */;
INSERT INTO `investor_pref_risk_levels` VALUES
(1,'LOW'),
(2,'LOW'),
(3,'LOW'),
(52,'HIGH'),
(102,'MEDIUM');
/*!40000 ALTER TABLE `investor_pref_risk_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_preferences`
--

DROP TABLE IF EXISTS `investor_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_preferences` (
  `investor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`investor_id`),
  CONSTRAINT `FKn5v2krvrw8x9k7gcjokxwv9wm` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_preferences`
--

LOCK TABLES `investor_preferences` WRITE;
/*!40000 ALTER TABLE `investor_preferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `investor_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investor_watchlist`
--

DROP TABLE IF EXISTS `investor_watchlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investor_watchlist` (
  `investor_id` bigint(20) NOT NULL,
  `investment_id` bigint(20) NOT NULL,
  PRIMARY KEY (`investor_id`,`investment_id`),
  KEY `FK1mrnrvah7he670kw94yr23q8e` (`investment_id`),
  CONSTRAINT `FK1mrnrvah7he670kw94yr23q8e` FOREIGN KEY (`investment_id`) REFERENCES `investments` (`id`),
  CONSTRAINT `FK2pt0voy034kjqhtp3t2wbb0q1` FOREIGN KEY (`investor_id`) REFERENCES `investors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investor_watchlist`
--

LOCK TABLES `investor_watchlist` WRITE;
/*!40000 ALTER TABLE `investor_watchlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `investor_watchlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investors`
--

DROP TABLE IF EXISTS `investors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investors` (
  `id` bigint(20) NOT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `portfolio_value` decimal(15,2) NOT NULL,
  `total_invested` decimal(15,2) NOT NULL,
  `total_returns` decimal(15,2) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt59ldm66889tm58ygharvn0bi` (`user_id`),
  CONSTRAINT `FKsr2yc2gls41w7vejv4wd1dkiw` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investors`
--

LOCK TABLES `investors` WRITE;
/*!40000 ALTER TABLE `investors` DISABLE KEYS */;
/*!40000 ALTER TABLE `investors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `investors_seq`
--

DROP TABLE IF EXISTS `investors_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `investors_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `investors_seq`
--

LOCK TABLES `investors_seq` WRITE;
/*!40000 ALTER TABLE `investors_seq` DISABLE KEYS */;
INSERT INTO `investors_seq` VALUES
(201);
/*!40000 ALTER TABLE `investors_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `requesting_company_documents`
--

DROP TABLE IF EXISTS `requesting_company_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `requesting_company_documents` (
  `signup_request` bigint(20) NOT NULL,
  `documents` varchar(255) DEFAULT NULL,
  KEY `FKfuuj5sb29hycpku2eyj3tio1s` (`signup_request`),
  CONSTRAINT `FKfuuj5sb29hycpku2eyj3tio1s` FOREIGN KEY (`signup_request`) REFERENCES `company_signup_requests` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `requesting_company_documents`
--

LOCK TABLES `requesting_company_documents` WRITE;
/*!40000 ALTER TABLE `requesting_company_documents` DISABLE KEYS */;
INSERT INTO `requesting_company_documents` VALUES
(52,'uploads/pending/9a277ed8-79ce-4826-ae2d-d80da54d5e7f/1764523957981_software-engineering.jpg'),
(52,'uploads/pending/9a277ed8-79ce-4826-ae2d-d80da54d5e7f/1764523957981_software-engineering.jpg'),
(102,'uploads/pending/962026c0-2c9a-4f29-b282-f29287784362/1764526602622_structured-cabling-1.jpg'),
(102,'uploads/pending/962026c0-2c9a-4f29-b282-f29287784362/1764526602623_webdev.jpg'),
(103,'uploads/pending/5d62da56-6996-4923-9689-815309126213/1764530875139_structured-cabling-1.jpg'),
(103,'uploads/pending/5d62da56-6996-4923-9689-815309126213/1764530875139_software-engineering.jpg');
/*!40000 ALTER TABLE `requesting_company_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint(20) NOT NULL,
  `role` enum('ADMIN','COMPANY','INVESTOR') NOT NULL,
  PRIMARY KEY (`user_id`,`role`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES
(1,'ADMIN');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `locked` bit(1) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(254) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES
(1,'2025-12-05 14:11:13.000000','bousleimengeorgio139@gmail.com','','\0','Admin User','$2a$10$rYU86.5TBx8bD1amb0smUOTIkcdf7dbLYCa7upu6ZRHpVdWlQXL.a');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_seq`
--

DROP TABLE IF EXISTS `users_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_seq`
--

LOCK TABLES `users_seq` WRITE;
/*!40000 ALTER TABLE `users_seq` DISABLE KEYS */;
INSERT INTO `users_seq` VALUES
(551);
/*!40000 ALTER TABLE `users_seq` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-05 18:33:46

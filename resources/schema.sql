-- MySQL dump 10.13  Distrib 5.7.15, for osx10.12 (x86_64)
--
-- Host: localhost    Database: repl_electric_samples
-- ------------------------------------------------------
-- Server version	5.7.15

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `notes_fine`
--

DROP TABLE IF EXISTS `notes_fine`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notes_fine` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `sample_id` int(32) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `collection` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `onset` float DEFAULT NULL,
  `offset` float DEFAULT NULL,
  `length` float DEFAULT NULL,
  `midi` int(11) DEFAULT NULL,
  `note` varchar(6) DEFAULT NULL,
  `octave` int(11) DEFAULT NULL,
  `perc` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `samples`
--

DROP TABLE IF EXISTS `samples`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `samples` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `guid` varchar(64) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `collection` varchar(255) DEFAULT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `length` float DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `subtype` varchar(255) DEFAULT NULL,
  `note` varchar(6) DEFAULT NULL,
  `octave` varchar(6) DEFAULT NULL,
  `bpm` int(8) DEFAULT NULL,
  `rough_note` varchar(6) DEFAULT NULL,
  `note1` varchar(6) DEFAULT NULL,
  `note2` varchar(6) DEFAULT NULL,
  `note3` varchar(6) DEFAULT NULL,
  `note4` varchar(6) DEFAULT NULL,
  `mean_amplitude` float DEFAULT NULL,
  `max_amplitude` float DEFAULT NULL,
  `min_amplitude` float DEFAULT NULL,
  `rms_amplitude` float DEFAULT NULL,
  `volume_adjustment` float DEFAULT NULL,
  `rms_delta` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55021 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-10-15 18:40:59

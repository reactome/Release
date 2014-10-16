-- MySQL dump 10.13  Distrib 5.1.63, for debian-linux-gnu (x86_64)
--
-- Host: reactomecurator.oicr.on.ca    Database: gk_central
-- ------------------------------------------------------
-- Server version	5.0.51a-24+lenny5-log

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
-- Not dumping tablespaces as no INFORMATION_SCHEMA.FILES table on this server
--

--
-- Table structure for table `AbstractModifiedResidue`
--

DROP TABLE IF EXISTS `AbstractModifiedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `AbstractModifiedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  `referenceSequence` int(10) unsigned default NULL,
  `referenceSequence_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceSequence` (`referenceSequence`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Affiliation`
--

DROP TABLE IF EXISTS `Affiliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Affiliation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `address` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `address` (`address`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Affiliation_2_name`
--

DROP TABLE IF EXISTS `Affiliation_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Affiliation_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BlackBoxEvent`
--

DROP TABLE IF EXISTS `BlackBoxEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `BlackBoxEvent` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `templateEvent` int(10) unsigned default NULL,
  `templateEvent_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `templateEvent` (`templateEvent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BlackBoxEvent_2_hasEvent`
--

DROP TABLE IF EXISTS `BlackBoxEvent_2_hasEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `BlackBoxEvent_2_hasEvent` (
  `DB_ID` int(10) unsigned default NULL,
  `hasEvent_rank` int(10) unsigned default NULL,
  `hasEvent` int(10) unsigned default NULL,
  `hasEvent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasEvent` (`hasEvent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Book`
--

DROP TABLE IF EXISTS `Book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Book` (
  `DB_ID` int(10) unsigned NOT NULL,
  `ISBN` text,
  `chapterTitle` text,
  `pages` text,
  `publisher` int(10) unsigned default NULL,
  `publisher_class` varchar(64) default NULL,
  `year` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `ISBN` (`ISBN`(10)),
  KEY `chapterTitle` (`chapterTitle`(10)),
  KEY `pages` (`pages`(10)),
  KEY `publisher` (`publisher`),
  KEY `year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Book_2_chapterAuthors`
--

DROP TABLE IF EXISTS `Book_2_chapterAuthors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Book_2_chapterAuthors` (
  `DB_ID` int(10) unsigned default NULL,
  `chapterAuthors_rank` int(10) unsigned default NULL,
  `chapterAuthors` int(10) unsigned default NULL,
  `chapterAuthors_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `chapterAuthors` (`chapterAuthors`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CandidateSet`
--

DROP TABLE IF EXISTS `CandidateSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CandidateSet` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CandidateSet_2_hasCandidate`
--

DROP TABLE IF EXISTS `CandidateSet_2_hasCandidate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CandidateSet_2_hasCandidate` (
  `DB_ID` int(10) unsigned default NULL,
  `hasCandidate_rank` int(10) unsigned default NULL,
  `hasCandidate` int(10) unsigned default NULL,
  `hasCandidate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasCandidate` (`hasCandidate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CatalystActivity`
--

DROP TABLE IF EXISTS `CatalystActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CatalystActivity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `activity` int(10) unsigned default NULL,
  `activity_class` varchar(64) default NULL,
  `physicalEntity` int(10) unsigned default NULL,
  `physicalEntity_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `activity` (`activity`),
  KEY `physicalEntity` (`physicalEntity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CatalystActivity_2_activeUnit`
--

DROP TABLE IF EXISTS `CatalystActivity_2_activeUnit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CatalystActivity_2_activeUnit` (
  `DB_ID` int(10) unsigned default NULL,
  `activeUnit_rank` int(10) unsigned default NULL,
  `activeUnit` int(10) unsigned default NULL,
  `activeUnit_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `activeUnit` (`activeUnit`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CatalystActivity_2_literatureReference`
--

DROP TABLE IF EXISTS `CatalystActivity_2_literatureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CatalystActivity_2_literatureReference` (
  `DB_ID` int(10) unsigned default NULL,
  `literatureReference_rank` int(10) unsigned default NULL,
  `literatureReference` int(10) unsigned default NULL,
  `literatureReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `literatureReference` (`literatureReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CellType`
--

DROP TABLE IF EXISTS `CellType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CellType` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Compartment`
--

DROP TABLE IF EXISTS `Compartment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Compartment` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Complex`
--

DROP TABLE IF EXISTS `Complex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Complex` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `isChimeric` enum('TRUE','FALSE') default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `isChimeric` (`isChimeric`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Complex_2_entityOnOtherCell`
--

DROP TABLE IF EXISTS `Complex_2_entityOnOtherCell`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Complex_2_entityOnOtherCell` (
  `DB_ID` int(10) unsigned default NULL,
  `entityOnOtherCell_rank` int(10) unsigned default NULL,
  `entityOnOtherCell` int(10) unsigned default NULL,
  `entityOnOtherCell_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `entityOnOtherCell` (`entityOnOtherCell`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Complex_2_hasComponent`
--

DROP TABLE IF EXISTS `Complex_2_hasComponent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Complex_2_hasComponent` (
  `DB_ID` int(10) unsigned default NULL,
  `hasComponent_rank` int(10) unsigned default NULL,
  `hasComponent` int(10) unsigned default NULL,
  `hasComponent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasComponent` (`hasComponent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Complex_2_includedLocation`
--

DROP TABLE IF EXISTS `Complex_2_includedLocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Complex_2_includedLocation` (
  `DB_ID` int(10) unsigned default NULL,
  `includedLocation_rank` int(10) unsigned default NULL,
  `includedLocation` int(10) unsigned default NULL,
  `includedLocation_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `includedLocation` (`includedLocation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Complex_2_species`
--

DROP TABLE IF EXISTS `Complex_2_species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Complex_2_species` (
  `DB_ID` int(10) unsigned default NULL,
  `species_rank` int(10) unsigned default NULL,
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `species` (`species`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConcurrentEventSet`
--

DROP TABLE IF EXISTS `ConcurrentEventSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConcurrentEventSet` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConcurrentEventSet_2_concurrentEvents`
--

DROP TABLE IF EXISTS `ConcurrentEventSet_2_concurrentEvents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConcurrentEventSet_2_concurrentEvents` (
  `DB_ID` int(10) unsigned default NULL,
  `concurrentEvents_rank` int(10) unsigned default NULL,
  `concurrentEvents` int(10) unsigned default NULL,
  `concurrentEvents_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `concurrentEvents` (`concurrentEvents`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ConcurrentEventSet_2_focusEntity`
--

DROP TABLE IF EXISTS `ConcurrentEventSet_2_focusEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ConcurrentEventSet_2_focusEntity` (
  `DB_ID` int(10) unsigned default NULL,
  `focusEntity_rank` int(10) unsigned default NULL,
  `focusEntity` int(10) unsigned default NULL,
  `focusEntity_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `focusEntity` (`focusEntity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ControlledVocabulary`
--

DROP TABLE IF EXISTS `ControlledVocabulary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ControlledVocabulary` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `definition` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ControlledVocabulary_2_name`
--

DROP TABLE IF EXISTS `ControlledVocabulary_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ControlledVocabulary_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CrosslinkedResidue`
--

DROP TABLE IF EXISTS `CrosslinkedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CrosslinkedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  `modification` int(10) unsigned default NULL,
  `modification_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `modification` (`modification`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CrosslinkedResidue_2_secondCoordinate`
--

DROP TABLE IF EXISTS `CrosslinkedResidue_2_secondCoordinate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CrosslinkedResidue_2_secondCoordinate` (
  `DB_ID` int(10) unsigned default NULL,
  `secondCoordinate_rank` int(10) unsigned default NULL,
  `secondCoordinate` int(10) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `secondCoordinate` (`secondCoordinate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DataModel`
--

DROP TABLE IF EXISTS `DataModel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DataModel` (
  `thing` varchar(255) NOT NULL,
  `thing_class` enum('SchemaClass','SchemaClassAttribute','Schema') default NULL,
  `property_name` varchar(255) NOT NULL,
  `property_value` text,
  `property_value_type` enum('INTEGER','SYMBOL','STRING','INSTANCE','SchemaClass','SchemaClassAttribute') default NULL,
  `property_value_rank` int(10) unsigned default '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseIdentifier`
--

DROP TABLE IF EXISTS `DatabaseIdentifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseIdentifier` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `identifier` varchar(50) default NULL,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `identifier` (`identifier`),
  KEY `referenceDatabase` (`referenceDatabase`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseIdentifier_2_crossReference`
--

DROP TABLE IF EXISTS `DatabaseIdentifier_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseIdentifier_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseObject`
--

DROP TABLE IF EXISTS `DatabaseObject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseObject` (
  `DB_ID` int(10) NOT NULL auto_increment,
  `_Protege_id` varchar(255) default NULL,
  `__is_ghost` enum('TRUE') default NULL,
  `_class` varchar(64) default NULL,
  `_displayName` text,
  `_timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `created` int(10) unsigned default NULL,
  `created_class` varchar(64) default NULL,
  `stableIdentifier` int(10) unsigned default NULL,
  `stableIdentifier_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `_Protege_id` (`_Protege_id`),
  KEY `__is_ghost` (`__is_ghost`),
  KEY `_class` (`_class`),
  KEY `_timestamp` (`_timestamp`),
  KEY `created` (`created`),
  KEY `_displayName` (`_displayName`(10)),
  KEY `stableIdentifier` (`stableIdentifier`)
) ENGINE=InnoDB AUTO_INCREMENT=5626753 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DatabaseObject_2_modified`
--

DROP TABLE IF EXISTS `DatabaseObject_2_modified`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DatabaseObject_2_modified` (
  `DB_ID` int(10) unsigned default NULL,
  `modified_rank` int(10) unsigned default NULL,
  `modified` int(10) unsigned default NULL,
  `modified_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `modified` (`modified`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DefinedSet`
--

DROP TABLE IF EXISTS `DefinedSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DefinedSet` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DeletedControlledVocabulary`
--

DROP TABLE IF EXISTS `DeletedControlledVocabulary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DeletedControlledVocabulary` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Depolymerisation`
--

DROP TABLE IF EXISTS `Depolymerisation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Depolymerisation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Disease`
--

DROP TABLE IF EXISTS `Disease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Disease` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Edge`
--

DROP TABLE IF EXISTS `Edge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Edge` (
  `DB_ID` int(10) unsigned NOT NULL,
  `edgeType` int(10) default NULL,
  `pathwayDiagram` int(10) unsigned default NULL,
  `pathwayDiagram_class` varchar(64) default NULL,
  `pointCoordinates` text,
  `sourceVertex` int(10) unsigned default NULL,
  `sourceVertex_class` varchar(64) default NULL,
  `targetVertex` int(10) unsigned default NULL,
  `targetVertex_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `edgeType` (`edgeType`),
  KEY `pathwayDiagram` (`pathwayDiagram`),
  KEY `pointCoordinates` (`pointCoordinates`(10)),
  KEY `sourceVertex` (`sourceVertex`),
  KEY `targetVertex` (`targetVertex`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityCompartment`
--

DROP TABLE IF EXISTS `EntityCompartment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityCompartment` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityFunctionalStatus`
--

DROP TABLE IF EXISTS `EntityFunctionalStatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityFunctionalStatus` (
  `DB_ID` int(10) unsigned NOT NULL,
  `physicalEntity` int(10) unsigned default NULL,
  `physicalEntity_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `physicalEntity` (`physicalEntity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityFunctionalStatus_2_functionalStatus`
--

DROP TABLE IF EXISTS `EntityFunctionalStatus_2_functionalStatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityFunctionalStatus_2_functionalStatus` (
  `DB_ID` int(10) unsigned default NULL,
  `functionalStatus_rank` int(10) unsigned default NULL,
  `functionalStatus` int(10) unsigned default NULL,
  `functionalStatus_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `functionalStatus` (`functionalStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntitySet`
--

DROP TABLE IF EXISTS `EntitySet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntitySet` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntitySet_2_hasMember`
--

DROP TABLE IF EXISTS `EntitySet_2_hasMember`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntitySet_2_hasMember` (
  `DB_ID` int(10) unsigned default NULL,
  `hasMember_rank` int(10) unsigned default NULL,
  `hasMember` int(10) unsigned default NULL,
  `hasMember_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasMember` (`hasMember`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntitySet_2_species`
--

DROP TABLE IF EXISTS `EntitySet_2_species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntitySet_2_species` (
  `DB_ID` int(10) unsigned default NULL,
  `species_rank` int(10) unsigned default NULL,
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `species` (`species`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityVertex`
--

DROP TABLE IF EXISTS `EntityVertex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityVertex` (
  `DB_ID` int(10) unsigned NOT NULL,
  `containedInEntityVertex` int(10) unsigned default NULL,
  `containedInEntityVertex_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `containedInEntityVertex` (`containedInEntityVertex`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityWithAccessionedSequence`
--

DROP TABLE IF EXISTS `EntityWithAccessionedSequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityWithAccessionedSequence` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `endCoordinate` int(10) default NULL,
  `referenceEntity` int(10) unsigned default NULL,
  `referenceEntity_class` varchar(64) default NULL,
  `startCoordinate` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `endCoordinate` (`endCoordinate`),
  KEY `referenceEntity` (`referenceEntity`),
  KEY `startCoordinate` (`startCoordinate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EntityWithAccessionedSequence_2_hasModifiedResidue`
--

DROP TABLE IF EXISTS `EntityWithAccessionedSequence_2_hasModifiedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EntityWithAccessionedSequence_2_hasModifiedResidue` (
  `DB_ID` int(10) unsigned default NULL,
  `hasModifiedResidue_rank` int(10) unsigned default NULL,
  `hasModifiedResidue` int(10) unsigned default NULL,
  `hasModifiedResidue_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasModifiedResidue` (`hasModifiedResidue`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event`
--

DROP TABLE IF EXISTS `Event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `definition` text,
  `evidenceType` int(10) unsigned default NULL,
  `evidenceType_class` varchar(64) default NULL,
  `goBiologicalProcess` int(10) unsigned default NULL,
  `goBiologicalProcess_class` varchar(64) default NULL,
  `releaseDate` date default NULL,
  `_doRelease` enum('TRUE','FALSE') default NULL,
  `releaseStatus` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `evidenceType` (`evidenceType`),
  KEY `goBiologicalProcess` (`goBiologicalProcess`),
  KEY `definition` (`definition`(10)),
  KEY `releaseDate` (`releaseDate`),
  KEY `_doRelease` (`_doRelease`),
  KEY `releaseStatus` (`releaseStatus`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_authored`
--

DROP TABLE IF EXISTS `Event_2_authored`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_authored` (
  `DB_ID` int(10) unsigned default NULL,
  `authored_rank` int(10) unsigned default NULL,
  `authored` int(10) unsigned default NULL,
  `authored_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `authored` (`authored`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_compartment`
--

DROP TABLE IF EXISTS `Event_2_compartment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_compartment` (
  `DB_ID` int(10) unsigned default NULL,
  `compartment_rank` int(10) unsigned default NULL,
  `compartment` int(10) unsigned default NULL,
  `compartment_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `compartment` (`compartment`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_crossReference`
--

DROP TABLE IF EXISTS `Event_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_disease`
--

DROP TABLE IF EXISTS `Event_2_disease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_disease` (
  `DB_ID` int(10) unsigned default NULL,
  `disease_rank` int(10) unsigned default NULL,
  `disease` int(10) unsigned default NULL,
  `disease_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `disease` (`disease`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_edited`
--

DROP TABLE IF EXISTS `Event_2_edited`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_edited` (
  `DB_ID` int(10) unsigned default NULL,
  `edited_rank` int(10) unsigned default NULL,
  `edited` int(10) unsigned default NULL,
  `edited_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `edited` (`edited`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_figure`
--

DROP TABLE IF EXISTS `Event_2_figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_figure` (
  `DB_ID` int(10) unsigned default NULL,
  `figure_rank` int(10) unsigned default NULL,
  `figure` int(10) unsigned default NULL,
  `figure_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `figure` (`figure`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_inferredFrom`
--

DROP TABLE IF EXISTS `Event_2_inferredFrom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_inferredFrom` (
  `DB_ID` int(10) unsigned default NULL,
  `inferredFrom_rank` int(10) unsigned default NULL,
  `inferredFrom` int(10) unsigned default NULL,
  `inferredFrom_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `inferredFrom` (`inferredFrom`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_literatureReference`
--

DROP TABLE IF EXISTS `Event_2_literatureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_literatureReference` (
  `DB_ID` int(10) unsigned default NULL,
  `literatureReference_rank` int(10) unsigned default NULL,
  `literatureReference` int(10) unsigned default NULL,
  `literatureReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `literatureReference` (`literatureReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_name`
--

DROP TABLE IF EXISTS `Event_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_orthologousEvent`
--

DROP TABLE IF EXISTS `Event_2_orthologousEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_orthologousEvent` (
  `DB_ID` int(10) unsigned default NULL,
  `orthologousEvent_rank` int(10) unsigned default NULL,
  `orthologousEvent` int(10) unsigned default NULL,
  `orthologousEvent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `orthologousEvent` (`orthologousEvent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_precedingEvent`
--

DROP TABLE IF EXISTS `Event_2_precedingEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_precedingEvent` (
  `DB_ID` int(10) unsigned default NULL,
  `precedingEvent_rank` int(10) unsigned default NULL,
  `precedingEvent` int(10) unsigned default NULL,
  `precedingEvent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `precedingEvent` (`precedingEvent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_relatedSpecies`
--

DROP TABLE IF EXISTS `Event_2_relatedSpecies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_relatedSpecies` (
  `DB_ID` int(10) unsigned default NULL,
  `relatedSpecies_rank` int(10) unsigned default NULL,
  `relatedSpecies` int(10) unsigned default NULL,
  `relatedSpecies_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `relatedSpecies` (`relatedSpecies`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_reviewed`
--

DROP TABLE IF EXISTS `Event_2_reviewed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_reviewed` (
  `DB_ID` int(10) unsigned default NULL,
  `reviewed_rank` int(10) unsigned default NULL,
  `reviewed` int(10) unsigned default NULL,
  `reviewed_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `reviewed` (`reviewed`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_revised`
--

DROP TABLE IF EXISTS `Event_2_revised`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_revised` (
  `DB_ID` int(10) unsigned default NULL,
  `revised_rank` int(10) unsigned default NULL,
  `revised` int(10) unsigned default NULL,
  `revised_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `revised` (`revised`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_species`
--

DROP TABLE IF EXISTS `Event_2_species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_species` (
  `DB_ID` int(10) unsigned default NULL,
  `species_rank` int(10) unsigned default NULL,
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `species` (`species`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Event_2_summation`
--

DROP TABLE IF EXISTS `Event_2_summation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Event_2_summation` (
  `DB_ID` int(10) unsigned default NULL,
  `summation_rank` int(10) unsigned default NULL,
  `summation` int(10) unsigned default NULL,
  `summation_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `summation` (`summation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EvidenceType`
--

DROP TABLE IF EXISTS `EvidenceType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EvidenceType` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `definition` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EvidenceType_2_instanceOf`
--

DROP TABLE IF EXISTS `EvidenceType_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EvidenceType_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `EvidenceType_2_name`
--

DROP TABLE IF EXISTS `EvidenceType_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EvidenceType_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ExternalOntology`
--

DROP TABLE IF EXISTS `ExternalOntology`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ExternalOntology` (
  `DB_ID` int(10) unsigned NOT NULL,
  `definition` text,
  `identifier` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `definition` (`definition`(10)),
  KEY `identifier` (`identifier`(10)),
  KEY `referenceDatabase` (`referenceDatabase`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ExternalOntology_2_instanceOf`
--

DROP TABLE IF EXISTS `ExternalOntology_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ExternalOntology_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ExternalOntology_2_name`
--

DROP TABLE IF EXISTS `ExternalOntology_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ExternalOntology_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ExternalOntology_2_synonym`
--

DROP TABLE IF EXISTS `ExternalOntology_2_synonym`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ExternalOntology_2_synonym` (
  `DB_ID` int(10) unsigned default NULL,
  `synonym_rank` int(10) unsigned default NULL,
  `synonym` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `synonym` (`synonym`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FailedReaction`
--

DROP TABLE IF EXISTS `FailedReaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FailedReaction` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Figure`
--

DROP TABLE IF EXISTS `Figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Figure` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `url` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `url` (`url`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FragmentDeletionModification`
--

DROP TABLE IF EXISTS `FragmentDeletionModification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FragmentDeletionModification` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FragmentInsertionModification`
--

DROP TABLE IF EXISTS `FragmentInsertionModification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FragmentInsertionModification` (
  `DB_ID` int(10) unsigned NOT NULL,
  `coordinate` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `coordinate` (`coordinate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FragmentModification`
--

DROP TABLE IF EXISTS `FragmentModification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FragmentModification` (
  `DB_ID` int(10) unsigned NOT NULL,
  `endPositionInReferenceSequence` int(10) default NULL,
  `startPositionInReferenceSequence` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `endPositionInReferenceSequence` (`endPositionInReferenceSequence`),
  KEY `startPositionInReferenceSequence` (`startPositionInReferenceSequence`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FragmentReplacedModification`
--

DROP TABLE IF EXISTS `FragmentReplacedModification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FragmentReplacedModification` (
  `DB_ID` int(10) unsigned NOT NULL,
  `alteredAminoAcidFragment` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `alteredAminoAcidFragment` (`alteredAminoAcidFragment`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FrontPage`
--

DROP TABLE IF EXISTS `FrontPage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FrontPage` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FrontPage_2_frontPageItem`
--

DROP TABLE IF EXISTS `FrontPage_2_frontPageItem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FrontPage_2_frontPageItem` (
  `DB_ID` int(10) unsigned default NULL,
  `frontPageItem_rank` int(10) unsigned default NULL,
  `frontPageItem` int(10) unsigned default NULL,
  `frontPageItem_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `frontPageItem` (`frontPageItem`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FunctionalStatus`
--

DROP TABLE IF EXISTS `FunctionalStatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionalStatus` (
  `DB_ID` int(10) unsigned NOT NULL,
  `functionalStatusType` int(10) unsigned default NULL,
  `structuralVariant` int(10) unsigned default NULL,
  `structuralVariant_class` varchar(64) default NULL,
  `functionalStatusType_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `structuralVariant` (`structuralVariant`),
  KEY `functionalStatusType` (`functionalStatusType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FunctionalStatusType`
--

DROP TABLE IF EXISTS `FunctionalStatusType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionalStatusType` (
  `DB_ID` int(10) unsigned NOT NULL,
  `definition` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FunctionalStatusType_2_name`
--

DROP TABLE IF EXISTS `FunctionalStatusType_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `FunctionalStatusType_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `accession` text,
  `definition` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceDatabase` (`referenceDatabase`),
  KEY `accession` (`accession`(10)),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_componentOf`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_componentOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_componentOf` (
  `DB_ID` int(10) unsigned default NULL,
  `componentOf_rank` int(10) unsigned default NULL,
  `componentOf` int(10) unsigned default NULL,
  `componentOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `componentOf` (`componentOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_hasPart`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_hasPart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_hasPart` (
  `DB_ID` int(10) unsigned default NULL,
  `hasPart_rank` int(10) unsigned default NULL,
  `hasPart` int(10) unsigned default NULL,
  `hasPart_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasPart` (`hasPart`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_instanceOf`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_name`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_negativelyRegulate`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_negativelyRegulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_negativelyRegulate` (
  `DB_ID` int(10) unsigned default NULL,
  `negativelyRegulate_rank` int(10) unsigned default NULL,
  `negativelyRegulate` int(10) unsigned default NULL,
  `negativelyRegulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `negativelyRegulate` (`negativelyRegulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_positivelyRegulate`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_positivelyRegulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_positivelyRegulate` (
  `DB_ID` int(10) unsigned default NULL,
  `positivelyRegulate_rank` int(10) unsigned default NULL,
  `positivelyRegulate` int(10) unsigned default NULL,
  `positivelyRegulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `positivelyRegulate` (`positivelyRegulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_BiologicalProcess_2_regulate`
--

DROP TABLE IF EXISTS `GO_BiologicalProcess_2_regulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_BiologicalProcess_2_regulate` (
  `DB_ID` int(10) unsigned default NULL,
  `regulate_rank` int(10) unsigned default NULL,
  `regulate` int(10) unsigned default NULL,
  `regulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `regulate` (`regulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_CellularComponent`
--

DROP TABLE IF EXISTS `GO_CellularComponent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_CellularComponent` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `accession` text,
  `definition` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceDatabase` (`referenceDatabase`),
  KEY `accession` (`accession`(10)),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_CellularComponent_2_componentOf`
--

DROP TABLE IF EXISTS `GO_CellularComponent_2_componentOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_CellularComponent_2_componentOf` (
  `DB_ID` int(10) unsigned default NULL,
  `componentOf_rank` int(10) unsigned default NULL,
  `componentOf` int(10) unsigned default NULL,
  `componentOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `componentOf` (`componentOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_CellularComponent_2_hasPart`
--

DROP TABLE IF EXISTS `GO_CellularComponent_2_hasPart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_CellularComponent_2_hasPart` (
  `DB_ID` int(10) unsigned default NULL,
  `hasPart_rank` int(10) unsigned default NULL,
  `hasPart` int(10) unsigned default NULL,
  `hasPart_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasPart` (`hasPart`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_CellularComponent_2_instanceOf`
--

DROP TABLE IF EXISTS `GO_CellularComponent_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_CellularComponent_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_CellularComponent_2_name`
--

DROP TABLE IF EXISTS `GO_CellularComponent_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_CellularComponent_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction`
--

DROP TABLE IF EXISTS `GO_MolecularFunction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `accession` text,
  `definition` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceDatabase` (`referenceDatabase`),
  KEY `accession` (`accession`(10)),
  KEY `definition` (`definition`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_componentOf`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_componentOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_componentOf` (
  `DB_ID` int(10) unsigned default NULL,
  `componentOf_rank` int(10) unsigned default NULL,
  `componentOf` int(10) unsigned default NULL,
  `componentOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `componentOf` (`componentOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_ecNumber`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_ecNumber`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_ecNumber` (
  `DB_ID` int(10) unsigned default NULL,
  `ecNumber_rank` int(10) unsigned default NULL,
  `ecNumber` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `ecNumber` (`ecNumber`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_hasPart`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_hasPart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_hasPart` (
  `DB_ID` int(10) unsigned default NULL,
  `hasPart_rank` int(10) unsigned default NULL,
  `hasPart` int(10) unsigned default NULL,
  `hasPart_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasPart` (`hasPart`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_instanceOf`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_name`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_negativelyRegulate`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_negativelyRegulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_negativelyRegulate` (
  `DB_ID` int(10) unsigned default NULL,
  `negativelyRegulate_rank` int(10) unsigned default NULL,
  `negativelyRegulate` int(10) unsigned default NULL,
  `negativelyRegulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `negativelyRegulate` (`negativelyRegulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_positivelyRegulate`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_positivelyRegulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_positivelyRegulate` (
  `DB_ID` int(10) unsigned default NULL,
  `positivelyRegulate_rank` int(10) unsigned default NULL,
  `positivelyRegulate` int(10) unsigned default NULL,
  `positivelyRegulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `positivelyRegulate` (`positivelyRegulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GO_MolecularFunction_2_regulate`
--

DROP TABLE IF EXISTS `GO_MolecularFunction_2_regulate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GO_MolecularFunction_2_regulate` (
  `DB_ID` int(10) unsigned default NULL,
  `regulate_rank` int(10) unsigned default NULL,
  `regulate` int(10) unsigned default NULL,
  `regulate_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `regulate` (`regulate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GeneticallyModifiedResidue`
--

DROP TABLE IF EXISTS `GeneticallyModifiedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GeneticallyModifiedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GenomeEncodedEntity`
--

DROP TABLE IF EXISTS `GenomeEncodedEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GenomeEncodedEntity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `species` (`species`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GroupModifiedResidue`
--

DROP TABLE IF EXISTS `GroupModifiedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `GroupModifiedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  `modification` int(10) unsigned default NULL,
  `modification_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `modification` (`modification`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InstanceEdit`
--

DROP TABLE IF EXISTS `InstanceEdit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InstanceEdit` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `_applyToAllEditedInstances` text,
  `dateTime` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `note` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `dateTime` (`dateTime`),
  KEY `_applyToAllEditedInstances` (`_applyToAllEditedInstances`(10)),
  KEY `note` (`note`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InstanceEdit_2_author`
--

DROP TABLE IF EXISTS `InstanceEdit_2_author`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InstanceEdit_2_author` (
  `DB_ID` int(10) unsigned default NULL,
  `author_rank` int(10) unsigned default NULL,
  `author` int(10) unsigned default NULL,
  `author_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `author` (`author`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InterChainCrosslinkedResidue`
--

DROP TABLE IF EXISTS `InterChainCrosslinkedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InterChainCrosslinkedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InterChainCrosslinkedResidue_2_equivalentTo`
--

DROP TABLE IF EXISTS `InterChainCrosslinkedResidue_2_equivalentTo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InterChainCrosslinkedResidue_2_equivalentTo` (
  `DB_ID` int(10) unsigned default NULL,
  `equivalentTo_rank` int(10) unsigned default NULL,
  `equivalentTo` int(10) unsigned default NULL,
  `equivalentTo_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `equivalentTo` (`equivalentTo`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InterChainCrosslinkedResidue_2_secondReferenceSequence`
--

DROP TABLE IF EXISTS `InterChainCrosslinkedResidue_2_secondReferenceSequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `InterChainCrosslinkedResidue_2_secondReferenceSequence` (
  `DB_ID` int(10) unsigned default NULL,
  `secondReferenceSequence_rank` int(10) unsigned default NULL,
  `secondReferenceSequence` int(10) unsigned default NULL,
  `secondReferenceSequence_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `secondReferenceSequence` (`secondReferenceSequence`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `IntraChainCrosslinkedResidue`
--

DROP TABLE IF EXISTS `IntraChainCrosslinkedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `IntraChainCrosslinkedResidue` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `LiteratureReference`
--

DROP TABLE IF EXISTS `LiteratureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `LiteratureReference` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `journal` varchar(255) default NULL,
  `pages` text,
  `pubMedIdentifier` int(10) default NULL,
  `volume` int(10) default NULL,
  `year` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `journal` (`journal`),
  KEY `pubMedIdentifier` (`pubMedIdentifier`),
  KEY `volume` (`volume`),
  KEY `year` (`year`),
  KEY `pages` (`pages`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ModifiedResidue`
--

DROP TABLE IF EXISTS `ModifiedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ModifiedResidue` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `NegativeRegulation`
--

DROP TABLE IF EXISTS `NegativeRegulation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NegativeRegulation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Ontology`
--

DROP TABLE IF EXISTS `Ontology`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Ontology` (
  `ontology` longblob
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `OpenSet`
--

DROP TABLE IF EXISTS `OpenSet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `OpenSet` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `referenceEntity` int(10) unsigned default NULL,
  `referenceEntity_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceEntity` (`referenceEntity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `OtherEntity`
--

DROP TABLE IF EXISTS `OtherEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `OtherEntity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Pathway`
--

DROP TABLE IF EXISTS `Pathway`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Pathway` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `doi` varchar(64) default NULL,
  `isCanonical` enum('TRUE','FALSE') default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `doi` (`doi`),
  KEY `isCanonical` (`isCanonical`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PathwayCoordinates`
--

DROP TABLE IF EXISTS `PathwayCoordinates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PathwayCoordinates` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `locatedEvent` int(10) unsigned default NULL,
  `locatedEvent_class` varchar(64) default NULL,
  `maxX` int(10) default NULL,
  `maxY` int(10) default NULL,
  `minX` int(10) default NULL,
  `minY` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `locatedEvent` (`locatedEvent`),
  KEY `maxX` (`maxX`),
  KEY `maxY` (`maxY`),
  KEY `minX` (`minX`),
  KEY `minY` (`minY`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PathwayDiagram`
--

DROP TABLE IF EXISTS `PathwayDiagram`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PathwayDiagram` (
  `DB_ID` int(10) unsigned NOT NULL,
  `height` int(10) default NULL,
  `storedATXML` longblob,
  `width` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `height` (`height`),
  KEY `width` (`width`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PathwayDiagramItem`
--

DROP TABLE IF EXISTS `PathwayDiagramItem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PathwayDiagramItem` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PathwayDiagram_2_representedPathway`
--

DROP TABLE IF EXISTS `PathwayDiagram_2_representedPathway`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PathwayDiagram_2_representedPathway` (
  `DB_ID` int(10) unsigned default NULL,
  `representedPathway_rank` int(10) unsigned default NULL,
  `representedPathway` int(10) unsigned default NULL,
  `representedPathway_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `representedPathway` (`representedPathway`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PathwayVertex`
--

DROP TABLE IF EXISTS `PathwayVertex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PathwayVertex` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Pathway_2_hasEvent`
--

DROP TABLE IF EXISTS `Pathway_2_hasEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Pathway_2_hasEvent` (
  `DB_ID` int(10) unsigned default NULL,
  `hasEvent_rank` int(10) unsigned default NULL,
  `hasEvent` int(10) unsigned default NULL,
  `hasEvent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `hasEvent` (`hasEvent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Person`
--

DROP TABLE IF EXISTS `Person`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Person` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `eMailAddress` varchar(255) default NULL,
  `firstname` text,
  `initial` varchar(10) default NULL,
  `surname` varchar(255) default NULL,
  `project` text,
  `url` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `eMailAddress` (`eMailAddress`),
  KEY `initial` (`initial`),
  KEY `surname` (`surname`),
  KEY `firstname` (`firstname`(10)),
  KEY `project` (`project`(10)),
  KEY `url` (`url`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Person_2_affiliation`
--

DROP TABLE IF EXISTS `Person_2_affiliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Person_2_affiliation` (
  `DB_ID` int(10) unsigned default NULL,
  `affiliation_rank` int(10) unsigned default NULL,
  `affiliation` int(10) unsigned default NULL,
  `affiliation_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `affiliation` (`affiliation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Person_2_crossReference`
--

DROP TABLE IF EXISTS `Person_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Person_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Person_2_figure`
--

DROP TABLE IF EXISTS `Person_2_figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Person_2_figure` (
  `DB_ID` int(10) unsigned default NULL,
  `figure_rank` int(10) unsigned default NULL,
  `figure` int(10) unsigned default NULL,
  `figure_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `figure` (`figure`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity`
--

DROP TABLE IF EXISTS `PhysicalEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `definition` text,
  `goCellularComponent` int(10) unsigned default NULL,
  `goCellularComponent_class` varchar(64) default NULL,
  `authored` int(10) unsigned default NULL,
  `authored_class` varchar(64) default NULL,
  `cellType` int(10) unsigned default NULL,
  `cellType_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `goCellularComponent` (`goCellularComponent`),
  KEY `definition` (`definition`(10)),
  KEY `authored` (`authored`),
  KEY `cellType` (`cellType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_compartment`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_compartment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_compartment` (
  `DB_ID` int(10) unsigned default NULL,
  `compartment_rank` int(10) unsigned default NULL,
  `compartment` int(10) unsigned default NULL,
  `compartment_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `compartment` (`compartment`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_crossReference`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_disease`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_disease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_disease` (
  `DB_ID` int(10) unsigned default NULL,
  `disease_rank` int(10) unsigned default NULL,
  `disease` int(10) unsigned default NULL,
  `disease_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `disease` (`disease`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_edited`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_edited`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_edited` (
  `DB_ID` int(10) unsigned default NULL,
  `edited_rank` int(10) unsigned default NULL,
  `edited` int(10) unsigned default NULL,
  `edited_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `edited` (`edited`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_figure`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_figure` (
  `DB_ID` int(10) unsigned default NULL,
  `figure_rank` int(10) unsigned default NULL,
  `figure` int(10) unsigned default NULL,
  `figure_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `figure` (`figure`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_inferredFrom`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_inferredFrom`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_inferredFrom` (
  `DB_ID` int(10) unsigned default NULL,
  `inferredFrom_rank` int(10) unsigned default NULL,
  `inferredFrom` int(10) unsigned default NULL,
  `inferredFrom_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `inferredFrom` (`inferredFrom`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_inferredTo`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_inferredTo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_inferredTo` (
  `DB_ID` int(10) unsigned default NULL,
  `inferredTo_rank` int(10) unsigned default NULL,
  `inferredTo` int(10) unsigned default NULL,
  `inferredTo_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `inferredTo` (`inferredTo`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_literatureReference`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_literatureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_literatureReference` (
  `DB_ID` int(10) unsigned default NULL,
  `literatureReference_rank` int(10) unsigned default NULL,
  `literatureReference` int(10) unsigned default NULL,
  `literatureReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `literatureReference` (`literatureReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_name`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_reviewed`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_reviewed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_reviewed` (
  `DB_ID` int(10) unsigned default NULL,
  `reviewed_rank` int(10) unsigned default NULL,
  `reviewed` int(10) unsigned default NULL,
  `reviewed_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `reviewed` (`reviewed`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_revised`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_revised`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_revised` (
  `DB_ID` int(10) unsigned default NULL,
  `revised_rank` int(10) unsigned default NULL,
  `revised` int(10) unsigned default NULL,
  `revised_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `revised` (`revised`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PhysicalEntity_2_summation`
--

DROP TABLE IF EXISTS `PhysicalEntity_2_summation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PhysicalEntity_2_summation` (
  `DB_ID` int(10) unsigned default NULL,
  `summation_rank` int(10) unsigned default NULL,
  `summation` int(10) unsigned default NULL,
  `summation_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `summation` (`summation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Polymer`
--

DROP TABLE IF EXISTS `Polymer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Polymer` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `maxUnitCount` int(10) default NULL,
  `minUnitCount` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `maxUnitCount` (`maxUnitCount`),
  KEY `minUnitCount` (`minUnitCount`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Polymer_2_repeatedUnit`
--

DROP TABLE IF EXISTS `Polymer_2_repeatedUnit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Polymer_2_repeatedUnit` (
  `DB_ID` int(10) unsigned default NULL,
  `repeatedUnit_rank` int(10) unsigned default NULL,
  `repeatedUnit` int(10) unsigned default NULL,
  `repeatedUnit_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `repeatedUnit` (`repeatedUnit`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Polymer_2_species`
--

DROP TABLE IF EXISTS `Polymer_2_species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Polymer_2_species` (
  `DB_ID` int(10) unsigned default NULL,
  `species_rank` int(10) unsigned default NULL,
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `species` (`species`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Polymerisation`
--

DROP TABLE IF EXISTS `Polymerisation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Polymerisation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PositiveRegulation`
--

DROP TABLE IF EXISTS `PositiveRegulation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PositiveRegulation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PsiMod`
--

DROP TABLE IF EXISTS `PsiMod`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PsiMod` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Publication`
--

DROP TABLE IF EXISTS `Publication`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Publication` (
  `DB_ID` int(10) unsigned NOT NULL,
  `title` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `title` (`title`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Publication_2_author`
--

DROP TABLE IF EXISTS `Publication_2_author`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Publication_2_author` (
  `DB_ID` int(10) unsigned default NULL,
  `author_rank` int(10) unsigned default NULL,
  `author` int(10) unsigned default NULL,
  `author_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `author` (`author`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Reaction`
--

DROP TABLE IF EXISTS `Reaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Reaction` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `reverseReaction` int(10) unsigned default NULL,
  `reverseReaction_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `reverseReaction` (`reverseReaction`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionCoordinates`
--

DROP TABLE IF EXISTS `ReactionCoordinates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionCoordinates` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `locatedEvent` int(10) unsigned default NULL,
  `locatedEvent_class` varchar(64) default NULL,
  `locationContext` int(10) unsigned default NULL,
  `locationContext_class` varchar(64) default NULL,
  `sourceX` int(10) default NULL,
  `sourceY` int(10) default NULL,
  `targetX` int(10) default NULL,
  `targetY` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `locatedEvent` (`locatedEvent`),
  KEY `locationContext` (`locationContext`),
  KEY `sourceX` (`sourceX`),
  KEY `sourceY` (`sourceY`),
  KEY `targetX` (`targetX`),
  KEY `targetY` (`targetY`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionVertex`
--

DROP TABLE IF EXISTS `ReactionVertex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionVertex` (
  `DB_ID` int(10) unsigned NOT NULL,
  `pointCoordinates` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `pointCoordinates` (`pointCoordinates`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent`
--

DROP TABLE IF EXISTS `ReactionlikeEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `isChimeric` enum('TRUE','FALSE') default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `isChimeric` (`isChimeric`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_catalystActivity`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_catalystActivity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_catalystActivity` (
  `DB_ID` int(10) unsigned default NULL,
  `catalystActivity_rank` int(10) unsigned default NULL,
  `catalystActivity` int(10) unsigned default NULL,
  `catalystActivity_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `catalystActivity` (`catalystActivity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_entityFunctionalStatus`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_entityFunctionalStatus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_entityFunctionalStatus` (
  `DB_ID` int(10) unsigned default NULL,
  `entityFunctionalStatus_rank` int(10) unsigned default NULL,
  `entityFunctionalStatus` int(10) unsigned default NULL,
  `entityFunctionalStatus_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `entityFunctionalStatus` (`entityFunctionalStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_entityOnOtherCell`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_entityOnOtherCell`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_entityOnOtherCell` (
  `DB_ID` int(10) unsigned default NULL,
  `entityOnOtherCell_rank` int(10) unsigned default NULL,
  `entityOnOtherCell` int(10) unsigned default NULL,
  `entityOnOtherCell_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `entityOnOtherCell` (`entityOnOtherCell`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_input`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_input`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_input` (
  `DB_ID` int(10) unsigned default NULL,
  `input_rank` int(10) unsigned default NULL,
  `input` int(10) unsigned default NULL,
  `input_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `input` (`input`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_normalReaction`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_normalReaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_normalReaction` (
  `DB_ID` int(10) unsigned default NULL,
  `normalReaction_rank` int(10) unsigned default NULL,
  `normalReaction` int(10) unsigned default NULL,
  `normalReaction_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `normalReaction` (`normalReaction`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_output`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_output`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_output` (
  `DB_ID` int(10) unsigned default NULL,
  `output_rank` int(10) unsigned default NULL,
  `output` int(10) unsigned default NULL,
  `output_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `output` (`output`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReactionlikeEvent_2_requiredInputComponent`
--

DROP TABLE IF EXISTS `ReactionlikeEvent_2_requiredInputComponent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReactionlikeEvent_2_requiredInputComponent` (
  `DB_ID` int(10) unsigned default NULL,
  `requiredInputComponent_rank` int(10) unsigned default NULL,
  `requiredInputComponent` int(10) unsigned default NULL,
  `requiredInputComponent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `requiredInputComponent` (`requiredInputComponent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceDNASequence`
--

DROP TABLE IF EXISTS `ReferenceDNASequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceDNASequence` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceDatabase`
--

DROP TABLE IF EXISTS `ReferenceDatabase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceDatabase` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `accessUrl` text,
  `url` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `url` (`url`(10)),
  KEY `accessUrl` (`accessUrl`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceDatabase_2_name`
--

DROP TABLE IF EXISTS `ReferenceDatabase_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceDatabase_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceEntity`
--

DROP TABLE IF EXISTS `ReferenceEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceEntity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `identifier` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `referenceDatabase` (`referenceDatabase`),
  KEY `identifier` (`identifier`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceEntity_2_crossReference`
--

DROP TABLE IF EXISTS `ReferenceEntity_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceEntity_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceEntity_2_name`
--

DROP TABLE IF EXISTS `ReferenceEntity_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceEntity_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceEntity_2_otherIdentifier`
--

DROP TABLE IF EXISTS `ReferenceEntity_2_otherIdentifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceEntity_2_otherIdentifier` (
  `DB_ID` int(10) unsigned default NULL,
  `otherIdentifier_rank` int(10) unsigned default NULL,
  `otherIdentifier` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `otherIdentifier` (`otherIdentifier`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceGeneProduct`
--

DROP TABLE IF EXISTS `ReferenceGeneProduct`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceGeneProduct` (
  `DB_ID` int(10) unsigned NOT NULL,
  `_chainChangeLog` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `_chainChangeLog` (`_chainChangeLog`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceGeneProduct_2_chain`
--

DROP TABLE IF EXISTS `ReferenceGeneProduct_2_chain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceGeneProduct_2_chain` (
  `DB_ID` int(10) unsigned default NULL,
  `chain_rank` int(10) unsigned default NULL,
  `chain` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `chain` (`chain`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceGeneProduct_2_referenceGene`
--

DROP TABLE IF EXISTS `ReferenceGeneProduct_2_referenceGene`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceGeneProduct_2_referenceGene` (
  `DB_ID` int(10) unsigned default NULL,
  `referenceGene_rank` int(10) unsigned default NULL,
  `referenceGene` int(10) unsigned default NULL,
  `referenceGene_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `referenceGene` (`referenceGene`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceGeneProduct_2_referenceTranscript`
--

DROP TABLE IF EXISTS `ReferenceGeneProduct_2_referenceTranscript`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceGeneProduct_2_referenceTranscript` (
  `DB_ID` int(10) unsigned default NULL,
  `referenceTranscript_rank` int(10) unsigned default NULL,
  `referenceTranscript` int(10) unsigned default NULL,
  `referenceTranscript_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `referenceTranscript` (`referenceTranscript`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceGroup`
--

DROP TABLE IF EXISTS `ReferenceGroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceGroup` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `atomicConnectivity` text,
  `formula` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `atomicConnectivity` (`atomicConnectivity`(10)),
  KEY `formula` (`formula`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceIsoform`
--

DROP TABLE IF EXISTS `ReferenceIsoform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceIsoform` (
  `DB_ID` int(10) unsigned NOT NULL,
  `variantIdentifier` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `variantIdentifier` (`variantIdentifier`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceIsoform_2_isoformParent`
--

DROP TABLE IF EXISTS `ReferenceIsoform_2_isoformParent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceIsoform_2_isoformParent` (
  `DB_ID` int(10) unsigned default NULL,
  `isoformParent_rank` int(10) unsigned default NULL,
  `isoformParent` int(10) unsigned default NULL,
  `isoformParent_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `isoformParent` (`isoformParent`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceMolecule`
--

DROP TABLE IF EXISTS `ReferenceMolecule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceMolecule` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `atomicConnectivity` text,
  `formula` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `atomicConnectivity` (`atomicConnectivity`(10)),
  KEY `formula` (`formula`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceRNASequence`
--

DROP TABLE IF EXISTS `ReferenceRNASequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceRNASequence` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceRNASequence_2_referenceGene`
--

DROP TABLE IF EXISTS `ReferenceRNASequence_2_referenceGene`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceRNASequence_2_referenceGene` (
  `DB_ID` int(10) unsigned default NULL,
  `referenceGene_rank` int(10) unsigned default NULL,
  `referenceGene` int(10) unsigned default NULL,
  `referenceGene_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `referenceGene` (`referenceGene`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence`
--

DROP TABLE IF EXISTS `ReferenceSequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  `sequenceLength` int(10) default NULL,
  `isSequenceChanged` text,
  `checksum` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `species` (`species`),
  KEY `sequenceLength` (`sequenceLength`),
  KEY `isSequenceChanged` (`isSequenceChanged`(10)),
  KEY `checksum` (`checksum`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence_2_comment`
--

DROP TABLE IF EXISTS `ReferenceSequence_2_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence_2_comment` (
  `DB_ID` int(10) unsigned default NULL,
  `comment_rank` int(10) unsigned default NULL,
  `comment` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `comment` (`comment`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence_2_description`
--

DROP TABLE IF EXISTS `ReferenceSequence_2_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence_2_description` (
  `DB_ID` int(10) unsigned default NULL,
  `description_rank` int(10) unsigned default NULL,
  `description` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `description` (`description`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence_2_geneName`
--

DROP TABLE IF EXISTS `ReferenceSequence_2_geneName`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence_2_geneName` (
  `DB_ID` int(10) unsigned default NULL,
  `geneName_rank` int(10) unsigned default NULL,
  `geneName` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `geneName` (`geneName`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence_2_keyword`
--

DROP TABLE IF EXISTS `ReferenceSequence_2_keyword`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence_2_keyword` (
  `DB_ID` int(10) unsigned default NULL,
  `keyword_rank` int(10) unsigned default NULL,
  `keyword` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `keyword` (`keyword`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReferenceSequence_2_secondaryIdentifier`
--

DROP TABLE IF EXISTS `ReferenceSequence_2_secondaryIdentifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReferenceSequence_2_secondaryIdentifier` (
  `DB_ID` int(10) unsigned default NULL,
  `secondaryIdentifier_rank` int(10) unsigned default NULL,
  `secondaryIdentifier` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `secondaryIdentifier` (`secondaryIdentifier`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation`
--

DROP TABLE IF EXISTS `Regulation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `regulatedEntity` int(10) unsigned default NULL,
  `regulatedEntity_class` varchar(64) default NULL,
  `regulationType` int(10) unsigned default NULL,
  `regulationType_class` varchar(64) default NULL,
  `regulator` int(10) unsigned default NULL,
  `regulator_class` varchar(64) default NULL,
  `releaseDate` date default NULL,
  `authored` int(10) unsigned default NULL,
  `authored_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `regulatedEntity` (`regulatedEntity`),
  KEY `regulationType` (`regulationType`),
  KEY `regulator` (`regulator`),
  KEY `releaseDate` (`releaseDate`),
  KEY `authored` (`authored`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RegulationType`
--

DROP TABLE IF EXISTS `RegulationType`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RegulationType` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RegulationType_2_instanceOf`
--

DROP TABLE IF EXISTS `RegulationType_2_instanceOf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RegulationType_2_instanceOf` (
  `DB_ID` int(10) unsigned default NULL,
  `instanceOf_rank` int(10) unsigned default NULL,
  `instanceOf` int(10) unsigned default NULL,
  `instanceOf_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `instanceOf` (`instanceOf`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RegulationType_2_name`
--

DROP TABLE IF EXISTS `RegulationType_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RegulationType_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` varchar(255) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_containedInPathway`
--

DROP TABLE IF EXISTS `Regulation_2_containedInPathway`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_containedInPathway` (
  `DB_ID` int(10) unsigned default NULL,
  `containedInPathway_rank` int(10) unsigned default NULL,
  `containedInPathway` int(10) unsigned default NULL,
  `containedInPathway_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `containedInPathway` (`containedInPathway`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_edited`
--

DROP TABLE IF EXISTS `Regulation_2_edited`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_edited` (
  `DB_ID` int(10) unsigned default NULL,
  `edited_rank` int(10) unsigned default NULL,
  `edited` int(10) unsigned default NULL,
  `edited_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `edited` (`edited`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_figure`
--

DROP TABLE IF EXISTS `Regulation_2_figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_figure` (
  `DB_ID` int(10) unsigned default NULL,
  `figure_rank` int(10) unsigned default NULL,
  `figure` int(10) unsigned default NULL,
  `figure_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `figure` (`figure`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_literatureReference`
--

DROP TABLE IF EXISTS `Regulation_2_literatureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_literatureReference` (
  `DB_ID` int(10) unsigned default NULL,
  `literatureReference_rank` int(10) unsigned default NULL,
  `literatureReference` int(10) unsigned default NULL,
  `literatureReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `literatureReference` (`literatureReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_name`
--

DROP TABLE IF EXISTS `Regulation_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_reviewed`
--

DROP TABLE IF EXISTS `Regulation_2_reviewed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_reviewed` (
  `DB_ID` int(10) unsigned default NULL,
  `reviewed_rank` int(10) unsigned default NULL,
  `reviewed` int(10) unsigned default NULL,
  `reviewed_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `reviewed` (`reviewed`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_revised`
--

DROP TABLE IF EXISTS `Regulation_2_revised`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_revised` (
  `DB_ID` int(10) unsigned default NULL,
  `revised_rank` int(10) unsigned default NULL,
  `revised` int(10) unsigned default NULL,
  `revised_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `revised` (`revised`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Regulation_2_summation`
--

DROP TABLE IF EXISTS `Regulation_2_summation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Regulation_2_summation` (
  `DB_ID` int(10) unsigned default NULL,
  `summation_rank` int(10) unsigned default NULL,
  `summation` int(10) unsigned default NULL,
  `summation_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `summation` (`summation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReplacedResidue`
--

DROP TABLE IF EXISTS `ReplacedResidue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReplacedResidue` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `coordinate` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `coordinate` (`coordinate`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ReplacedResidue_2_psiMod`
--

DROP TABLE IF EXISTS `ReplacedResidue_2_psiMod`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ReplacedResidue_2_psiMod` (
  `DB_ID` int(10) unsigned default NULL,
  `psiMod_rank` int(10) unsigned default NULL,
  `psiMod` int(10) unsigned default NULL,
  `psiMod_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `psiMod` (`psiMod`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Requirement`
--

DROP TABLE IF EXISTS `Requirement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Requirement` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SequenceOntology`
--

DROP TABLE IF EXISTS `SequenceOntology`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SequenceOntology` (
  `DB_ID` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SimpleEntity`
--

DROP TABLE IF EXISTS `SimpleEntity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SimpleEntity` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  `referenceEntity` int(10) unsigned default NULL,
  `referenceEntity_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `species` (`species`),
  KEY `referenceEntity` (`referenceEntity`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Species`
--

DROP TABLE IF EXISTS `Species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Species` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`DB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Species_2_figure`
--

DROP TABLE IF EXISTS `Species_2_figure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Species_2_figure` (
  `DB_ID` int(10) unsigned default NULL,
  `figure_rank` int(10) unsigned default NULL,
  `figure` int(10) unsigned default NULL,
  `figure_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `figure` (`figure`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StableIdentifier`
--

DROP TABLE IF EXISTS `StableIdentifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `StableIdentifier` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `identifier` text,
  `identifierVersion` text,
  `referenceDatabase` int(10) unsigned default NULL,
  `referenceDatabase_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `identifier` (`identifier`(10)),
  KEY `identifierVersion` (`identifierVersion`(10)),
  KEY `referenceDatabase` (`referenceDatabase`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Summation`
--

DROP TABLE IF EXISTS `Summation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Summation` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `text` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `text` (`text`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Summation_2_literatureReference`
--

DROP TABLE IF EXISTS `Summation_2_literatureReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Summation_2_literatureReference` (
  `DB_ID` int(10) unsigned default NULL,
  `literatureReference_rank` int(10) unsigned default NULL,
  `literatureReference` int(10) unsigned default NULL,
  `literatureReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `literatureReference` (`literatureReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Taxon`
--

DROP TABLE IF EXISTS `Taxon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Taxon` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `superTaxon` int(10) unsigned default NULL,
  `superTaxon_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `superTaxon` (`superTaxon`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Taxon_2_crossReference`
--

DROP TABLE IF EXISTS `Taxon_2_crossReference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Taxon_2_crossReference` (
  `DB_ID` int(10) unsigned default NULL,
  `crossReference_rank` int(10) unsigned default NULL,
  `crossReference` int(10) unsigned default NULL,
  `crossReference_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `crossReference` (`crossReference`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Taxon_2_name`
--

DROP TABLE IF EXISTS `Taxon_2_name`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Taxon_2_name` (
  `DB_ID` int(10) unsigned default NULL,
  `name_rank` int(10) unsigned default NULL,
  `name` varchar(255) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TranslationalModification`
--

DROP TABLE IF EXISTS `TranslationalModification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TranslationalModification` (
  `DB_ID` int(10) unsigned NOT NULL,
  `coordinate` int(10) default NULL,
  `psiMod` int(10) unsigned default NULL,
  `psiMod_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `coordinate` (`coordinate`),
  KEY `psiMod` (`psiMod`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `URL`
--

DROP TABLE IF EXISTS `URL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `URL` (
  `DB_ID` int(10) unsigned NOT NULL,
  `uniformResourceLocator` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `uniformResourceLocator` (`uniformResourceLocator`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Vertex`
--

DROP TABLE IF EXISTS `Vertex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Vertex` (
  `DB_ID` int(10) unsigned NOT NULL,
  `height` int(10) default NULL,
  `pathwayDiagram` int(10) unsigned default NULL,
  `pathwayDiagram_class` varchar(64) default NULL,
  `representedInstance` int(10) unsigned default NULL,
  `representedInstance_class` varchar(64) default NULL,
  `width` int(10) default NULL,
  `x` int(10) default NULL,
  `y` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `height` (`height`),
  KEY `pathwayDiagram` (`pathwayDiagram`),
  KEY `representedInstance` (`representedInstance`),
  KEY `width` (`width`),
  KEY `x` (`x`),
  KEY `y` (`y`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `VertexSearchableTerm`
--

DROP TABLE IF EXISTS `VertexSearchableTerm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VertexSearchableTerm` (
  `DB_ID` int(10) unsigned NOT NULL,
  `providerCount` int(10) default NULL,
  `searchableTerm` varchar(255) default NULL,
  `species` int(10) unsigned default NULL,
  `species_class` varchar(64) default NULL,
  `vertexCount` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `providerCount` (`providerCount`),
  KEY `searchableTerm` (`searchableTerm`),
  KEY `species` (`species`),
  KEY `vertexCount` (`vertexCount`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `VertexSearchableTerm_2_termProvider`
--

DROP TABLE IF EXISTS `VertexSearchableTerm_2_termProvider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VertexSearchableTerm_2_termProvider` (
  `DB_ID` int(10) unsigned default NULL,
  `termProvider_rank` int(10) unsigned default NULL,
  `termProvider` int(10) unsigned default NULL,
  `termProvider_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `termProvider` (`termProvider`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `VertexSearchableTerm_2_vertex`
--

DROP TABLE IF EXISTS `VertexSearchableTerm_2_vertex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `VertexSearchableTerm_2_vertex` (
  `DB_ID` int(10) unsigned default NULL,
  `vertex_rank` int(10) unsigned default NULL,
  `vertex` int(10) unsigned default NULL,
  `vertex_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `vertex` (`vertex`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_AttributeValueBeforeChange`
--

DROP TABLE IF EXISTS `_AttributeValueBeforeChange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_AttributeValueBeforeChange` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `changedAttribute` text,
  PRIMARY KEY  (`DB_ID`),
  KEY `changedAttribute` (`changedAttribute`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_AttributeValueBeforeChange_2_previousValue`
--

DROP TABLE IF EXISTS `_AttributeValueBeforeChange_2_previousValue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_AttributeValueBeforeChange_2_previousValue` (
  `DB_ID` int(10) unsigned default NULL,
  `previousValue_rank` int(10) unsigned default NULL,
  `previousValue` text,
  KEY `DB_ID` (`DB_ID`),
  KEY `previousValue` (`previousValue`(10))
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_Deleted`
--

DROP TABLE IF EXISTS `_Deleted`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_Deleted` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `curatorComment` text,
  `reason` int(10) unsigned default NULL,
  `reason_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `curatorComment` (`curatorComment`(10)),
  KEY `reason` (`reason`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_Deleted_2_deletedInstanceDB_ID`
--

DROP TABLE IF EXISTS `_Deleted_2_deletedInstanceDB_ID`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_Deleted_2_deletedInstanceDB_ID` (
  `DB_ID` int(10) unsigned default NULL,
  `deletedInstanceDB_ID_rank` int(10) unsigned default NULL,
  `deletedInstanceDB_ID` int(10) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `deletedInstanceDB_ID` (`deletedInstanceDB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_Deleted_2_replacementInstances`
--

DROP TABLE IF EXISTS `_Deleted_2_replacementInstances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_Deleted_2_replacementInstances` (
  `DB_ID` int(10) unsigned default NULL,
  `replacementInstances_rank` int(10) unsigned default NULL,
  `replacementInstances` int(10) unsigned default NULL,
  `replacementInstances_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `replacementInstances` (`replacementInstances`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_InstanceBeforeChange`
--

DROP TABLE IF EXISTS `_InstanceBeforeChange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_InstanceBeforeChange` (
  `DB_ID` int(10) unsigned NOT NULL default '0',
  `changedInstanceDB_ID` int(10) default NULL,
  `instanceEdit` int(10) unsigned default NULL,
  `instanceEdit_class` varchar(64) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `changedInstanceDB_ID` (`changedInstanceDB_ID`),
  KEY `instanceEdit` (`instanceEdit`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_InstanceBeforeChange_2_attributeValuesBeforeChange`
--

DROP TABLE IF EXISTS `_InstanceBeforeChange_2_attributeValuesBeforeChange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_InstanceBeforeChange_2_attributeValuesBeforeChange` (
  `DB_ID` int(10) unsigned default NULL,
  `attributeValuesBeforeChange_rank` int(10) unsigned default NULL,
  `attributeValuesBeforeChange` int(10) unsigned default NULL,
  `attributeValuesBeforeChange_class` varchar(64) default NULL,
  KEY `DB_ID` (`DB_ID`),
  KEY `attributeValuesBeforeChange` (`attributeValuesBeforeChange`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `_Release`
--

DROP TABLE IF EXISTS `_Release`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `_Release` (
  `DB_ID` int(10) unsigned NOT NULL,
  `releaseDate` text,
  `releaseNumber` int(10) default NULL,
  PRIMARY KEY  (`DB_ID`),
  KEY `releaseDate` (`releaseDate`(10)),
  KEY `releaseNumber` (`releaseNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-10-10 18:19:36

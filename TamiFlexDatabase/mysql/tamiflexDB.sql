SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `tamiflexDB` ;
USE `tamiflexDB`;

-- -----------------------------------------------------
-- Table `tamiflexDB`.`CallTypes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`CallTypes` (
  `idCallTypes` INT NOT NULL AUTO_INCREMENT ,
  `type` VARCHAR(45) NULL ,
  PRIMARY KEY (`idCallTypes`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `tamiflexDB`.`ClassID`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`ClassID` (
  `idClassID` INT NOT NULL AUTO_INCREMENT ,
  `name` VARCHAR(1000) NULL ,
  `version` BIGINT NULL ,
  PRIMARY KEY (`idClassID`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `tamiflexDB`.`Locations`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`Locations` (
  `idLocations` INT NOT NULL AUTO_INCREMENT ,
  `classid` INT NULL ,
  `method` VARCHAR(1000) NULL ,
  `line` INT NULL ,
  `calltypeid` INT NULL ,
  PRIMARY KEY (`idLocations`) ,
  INDEX `idClassID` (`classid` ASC) ,
  INDEX `idCallTypes` (`calltypeid` ASC) ,
  CONSTRAINT `idClassID`
    FOREIGN KEY (`classid` )
    REFERENCES `tamiflexDB`.`ClassID` (`idClassID` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `idCallTypes`
    FOREIGN KEY (`calltypeid` )
    REFERENCES `tamiflexDB`.`CallTypes` (`idCallTypes` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `tamiflexDB`.`Calls`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`Calls` (
  `idCalls` INT NOT NULL AUTO_INCREMENT ,
  `locationid` INT NULL ,
  `target` VARCHAR(1000) NULL ,
  `thread` VARCHAR(100) NULL ,
  PRIMARY KEY (`idCalls`) ,
  INDEX `idLocations` (`locationid` ASC) ,
  CONSTRAINT `idLocations`
    FOREIGN KEY (`locationid` )
    REFERENCES `tamiflexDB`.`Locations` (`idLocations` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `tamiflexDB`.`Runs`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`Runs` (
  `idRuns` INT NOT NULL AUTO_INCREMENT ,
  `host` VARCHAR(300) NULL ,
  `time` TIMESTAMP NULL ,
  PRIMARY KEY (`idRuns`) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `tamiflexDB`.`RunToCall`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `tamiflexDB`.`RunToCall` (
  `idRunToCall` INT NOT NULL AUTO_INCREMENT ,
  `RunID` INT NULL ,
  `CallID` INT NULL ,
  PRIMARY KEY (`idRunToCall`) ,
  INDEX `idRuns` (`RunID` ASC) ,
  INDEX `idCalls` (`CallID` ASC) ,
  CONSTRAINT `idRuns`
    FOREIGN KEY (`RunID` )
    REFERENCES `tamiflexDB`.`Runs` (`idRuns` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `idCalls`
    FOREIGN KEY (`CallID` )
    REFERENCES `tamiflexDB`.`Calls` (`idCalls` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- -----------------------------------------------------
-- Data for table `tamiflexDB`.`CallTypes`
-- -----------------------------------------------------
SET AUTOCOMMIT=0;
USE `tamiflexDB`;
INSERT INTO `CallTypes` (`idCallTypes`, `type`) VALUES (NULL, 'Class.forName');
INSERT INTO `CallTypes` (`idCallTypes`, `type`) VALUES (NULL, 'Class.newInstance');
INSERT INTO `CallTypes` (`idCallTypes`, `type`) VALUES (NULL, 'Method.invoke');
INSERT INTO `CallTypes` (`idCallTypes`, `type`) VALUES (NULL, 'Constructor.newInstance');

COMMIT;

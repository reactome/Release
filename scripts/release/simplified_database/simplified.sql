CREATE TABLE Pathway (
    id INT(32) PRIMARY KEY,
    displayName VARCHAR(255) NOT NULL,
    species VARCHAR(255) NOT NULL,
    stableId VARCHAR(32) NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE ReactionLikeEvent (
    id INT(32) PRIMARY KEY,
    displayName VARCHAR(255) NOT NULL,
    species VARCHAR(255) NOT NULL,
    class VARCHAR(255) NOT NULL,
    stableId VARCHAR(32) NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE PhysicalEntity (
    id INT(32) PRIMARY KEY,
    displayName VARCHAR(255) NOT NULL,
    species VARCHAR(255) NULL,
    class VARCHAR(255) NOT NULL,
    stableId VARCHAR(32) NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE ReactionLikeEvent_To_PhysicalEntity (
    reactionLikeEventId INT(32),
    physicalEntityId INT(32),
    PRIMARY KEY (reactionLikeEventId, physicalEntityId),
    FOREIGN KEY (reactionLikeEventId) REFERENCES ReactionLikeEvent (id),
    FOREIGN KEY (physicalEntityId) REFERENCES PhysicalEntity (id)
) ENGINE=InnoDB;

CREATE TABLE Pathway_To_ReactionLikeEvent (
    pathwayId INT(32),
    reactionLikeEventId INT(32),
    PRIMARY KEY (pathwayId, reactionLikeEventId),
    FOREIGN KEY (pathwayId) REFERENCES Pathway (id),
    FOREIGN KEY (reactionLikeEventId) REFERENCES ReactionLikeEvent (id)
) ENGINE=InnoDB;

CREATE TABLE Id_To_ExternalIdentifier (
    id INT(32),
    referenceDatabase VARCHAR(255),
    externalIdentifier VARCHAR(32),
    description VARCHAR(255) NULL,
    PRIMARY KEY (id, referenceDatabase, externalIdentifier),
    FOREIGN KEY (id) REFERENCES Pathway (id),
    FOREIGN KEY (id) REFERENCES ReactionLikeEvent (id),
    FOREIGN KEY (id) REFERENCES PhysicalEntity (id)
) ENGINE=InnoDB;

CREATE TABLE PathwayHierarchy (
    pathwayId INT(32),
    childPathwayId INT(32),
    PRIMARY KEY (pathwayId, childPathwayId),
    FOREIGN KEY (pathwayId) REFERENCES Pathway (id)
) ENGINE=InnoDB;

CREATE TABLE PhysicalEntityHierarchy (
    physicalEntityId INT(32),
    childPhysicalEntityId INT(32),
    PRIMARY KEY (physicalEntityId, childPhysicalEntityId),
    FOREIGN KEY (physicalEntityId) REFERENCES PhysicalEntity (id)
) ENGINE=InnoDB;

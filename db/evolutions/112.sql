# ---!Ups

CREATE TABLE contractual_clauses (
	id BIGSERIAL PRIMARY KEY,
	name TEXT UNIQUE NOT NULL,
	description TEXT,
	begin_date DATE NOT NULL default NOW(),
	end_date DATE,
	version INT DEFAULT 0
);
	
CREATE TABLE contractual_clauses_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	name TEXT,
	description TEXT,
	begin_date DATE,
	end_date DATE,
	PRIMARY KEY (id, _revision, _revision_type)
);
	
CREATE TABLE contractual_clauses_group_absence_types (
	contractual_clauses_id BIGINT NOT NULL REFERENCES contractual_clauses(id),
	group_absence_types_id BIGINT NOT NULL REFERENCES group_absence_types(id)
);

CREATE TABLE contractual_clauses_group_absence_types_history (
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	contractual_clauses_id BIGINT NOT NULL REFERENCES contractual_clauses(id),
	group_absence_types_id BIGINT NOT NULL REFERENCES group_absence_types(id),    
    PRIMARY KEY (contractual_clauses_id, group_absence_types_id, _revision, _revision_type)
);

CREATE TABLE contractual_references (
	id BIGSERIAL PRIMARY KEY,
	name TEXT UNIQUE NOT NULL,
	url TEXT,
	filename TEXT,
	file TEXT,
	version INT DEFAULT 0
);

CREATE TABLE contractual_references_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	name TEXT,
	url TEXT,
	filename TEXT,
	file TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE contractual_clauses_contractual_references (
	contractual_clauses_id BIGINT NOT NULL REFERENCES contractual_clauses(id),
	contractual_references_id BIGINT NOT NULL REFERENCES contractual_references(id)
);

CREATE TABLE contractual_clauses_contractual_references_history (
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	contractual_clauses_id BIGINT NOT NULL REFERENCES contractual_clauses(id),
	contractual_references_id BIGINT NOT NULL REFERENCES contractual_references(id),  
    PRIMARY KEY (contractual_clauses_id, contractual_references_id, _revision, _revision_type)
);

ALTER TABLE absence_types ADD COLUMN documentation TEXT;
ALTER TABLE absence_types_history ADD COLUMN documentation TEXT;

# ---!Downs

ALTER TABLE absence_types_history DROP COLUMN documentation;
ALTER TABLE absence_types DROP COLUMN documentation;

DROP TABLE contractual_clause_group_absence_types_history;
DROP TABLE contractual_clause_contractual_references;
DROP TABLE contractual_clause_group_absence_types_history;
DROP TABLE contractual_clause_group_absence_types;
DROP TABLE contractual_clauses_history;
DROP TABLE contractual_clauses;

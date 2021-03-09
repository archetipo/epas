# --- !Ups

CREATE TABLE information_requests(
	id BIGSERIAL PRIMARY KEY,
	person_id BIGINT REFERENCES persons(id),
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	version INT DEFAULT 0
);

CREATE TABLE information_requests_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	person_id BIGINT,
	information_type TEXT,
	office_head_approved TIMESTAMP WITHOUT TIME ZONE,
	office_head_approval_required BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE information_request_events(
	id BIGSERIAL PRIMARY KEY,
	information_request_id BIGINT REFERENCES information_requests(id),
	owner_id BIGINT REFERENCES users(id),
	description TEXT,
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);

CREATE TABLE information_request_events_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	information_request_id BIGINT,
	owner_id BIGINT REFERENCES users(id),
	description TEXT,
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	PRIMARY KEY (id, _revision, _revision_type)
);


CREATE TABLE telework_requests(
	id BIGSERIAL PRIMARY KEY,	
	context TEXT,
	year INTEGER,
	month INTEGER,
	version INT DEFAULT 0,
	FOREIGN KEY (id) REFERENCES information_requests (id));	

CREATE TABLE telework_requests_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	context TEXT,
  	year INTEGER,
	month INTEGER,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE illness_requests(
	id BIGSERIAL PRIMARY KEY,
	begin_date DATE,
	end_date DATE,
	name TEXT,
	version INT DEFAULT 0,
	FOREIGN KEY (id) REFERENCES information_requests (id));	

CREATE TABLE illness_requests_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	begin_date DATE,
	end_date DATE,
	name TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE service_requests(
	id BIGSERIAL PRIMARY KEY,
	day DATE,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	reason TEXT,
	version INT DEFAULT 0,
	FOREIGN KEY (id) REFERENCES information_requests (id));
	

CREATE TABLE service_requests_history(
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
	day DATE,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	reason TEXT,
	PRIMARY KEY (id, _revision, _revision_type)
);




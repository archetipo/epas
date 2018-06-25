# ---!Ups

CREATE TABLE absence_requests (
	id BIGSERIAL PRIMARY KEY,
	type TEXT,
	person_id BIGINT NOT NULL REFERENCES persons(id),
	start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	description TEXT,
	attachment TEXT,
	manager_approved DATE,
	administrative_approved DATE,
	office_head_approved DATE,
	manager_approval_required BOOLEAN DEFAULT TRUE,
	administrative_approval_required BOOLEAN DEFAULT TRUE,
	office_head_approval_required BOOLEAN DEFAULT TRUE,
	created_at TIMESTAMP WITHOUT TIME ZONE,
	updated_at TIMESTAMP WITHOUT TIME ZONE,
	version INT DEFAULT 0
);
	
CREATE TABLE absence_requests_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
  	type TEXT,
	person_id BIGINT,
	start_at TIMESTAMP WITHOUT TIME ZONE,
	end_to TIMESTAMP WITHOUT TIME ZONE,
	description TEXT,
	attachment TEXT,
	manager_approved DATE,
	administrative_approved DATE,
	office_head_approved DATE,
	manager_approval_required BOOLEAN,
	administrative_approval_required BOOLEAN,
	office_head_approval_required BOOLEAN,
	PRIMARY KEY (id, _revision, _revision_type)
);

CREATE TABLE absence_request_events (
	id BIGSERIAL PRIMARY KEY,
	absence_request_id BIGINT NOT NULL REFERENCES absence_requests(id),
	owner_id BIGINT NOT NULL REFERENCES users(id),
	description TEXT,	
	event_type TEXT,
	created_at TIMESTAMP WITHOUT TIME ZONE, 
	version INT DEFAULT 0
);


# ---!Downs

DROP TABLE absence_request_events;
DROP TABLE absence_requests_history;
DROP TABLE absence_requests;

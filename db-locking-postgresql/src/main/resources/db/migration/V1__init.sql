CREATE TABLE accounts
(
  id             UUID PRIMARY KEY,
  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
  version        INTEGER NOT NULL DEFAULT 0,
  name           VARCHAR
);

CREATE TABLE operations
(
  id             UUID PRIMARY KEY,
  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
  description    VARCHAR NOT NULL,
  account_id     UUID NOT NULL,
  version        INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX ON operations (account_id);

CREATE TABLE entity_without_version
(
  id             SERIAL PRIMARY KEY,
  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
  description    VARCHAR NOT NULL
);

CREATE TABLE entity_with_version
(
  id             SERIAL PRIMARY KEY,
  created        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
  description    VARCHAR NOT NULL,
  version        INTEGER NOT NULL DEFAULT 0
);

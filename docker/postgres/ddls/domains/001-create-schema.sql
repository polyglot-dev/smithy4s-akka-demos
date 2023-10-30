
CREATE TABLE IF NOT EXISTS person(
      id BIGSERIAL PRIMARY KEY,
      name VARCHAR,
      town VARCHAR,
      address json
);

create unique index person_name_uindex
    on person (name);

CREATE TABLE IF NOT EXISTS advertiser(
      id BIGSERIAL PRIMARY KEY,
      name VARCHAR not null
);

CREATE TABLE IF NOT EXISTS campaign(
      id BIGSERIAL PRIMARY KEY,
      name VARCHAR not null,
      advertiser_id BIGINT not null,
      CONSTRAINT fk_advertiser
          FOREIGN KEY(advertiser_id)
              REFERENCES advertiser(id)
              ON DELETE CASCADE
);

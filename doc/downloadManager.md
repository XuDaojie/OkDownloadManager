SQL
===

## Create Table
``` sql
CREATE TABLE t_download_manager(
       allow_write Boolean,
       id integer NOT NULL,
       last_modify_timestamp integer,
       local_filename varchar,
       local_uri varchar,
       media_type varchar,
       media_provider_uri varchar,
       reason varchar,
       status integer,
       title varchar,
       total_size_bytes integer,
       uri varchar,
       PRIMARY KEY(id)
)
```

## Insert
```sql
INSERT INTO t_download_manager
VALUES
       (
              1,  -- allow_write
              123457, -- id
              1213213313,  -- timeline
              'c://dd.apk',
              'c://dd.apk',
              'media_type',
              'media_provider_uri',
              'reason',
              1, -- status
              'title',
              1024, -- total_size_bytes
              'uri'
       )
```

## Update
``` sql
UPDATE t_download_manager
SET status = 2,
 total_size_bytes = 2048
WHERE
    id = 123455
```
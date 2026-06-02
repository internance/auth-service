CREATE TABLE users (
    id         UUID         NOT NULL,
    username   VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);

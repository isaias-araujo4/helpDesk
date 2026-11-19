CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    description VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permissions ENUM(
        'ASSUME_TICKET','CANCEL_TICKET','CLOSE_TICKET','COMMENT_TICKET',
        'CREATE_TICKET','MANAGE_USERS','REOPEN_TICKET','RESOLVE_TICKET',
        'VIEW_AUDIT_LOGS','VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS'
    ),
    CONSTRAINT uk_role_permissions UNIQUE (role_id, permissions),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    active BIT NOT NULL,
    created_by VARCHAR(255),
    created_on DATE,
    deleted_by VARCHAR(255),
    deleted_on DATE,
    department ENUM('HR','IT','MARKETING','PURCHASING') NOT NULL,
    email VARCHAR(50) NOT NULL,
    first_name VARCHAR(25) NOT NULL,
    last_name VARCHAR(25) NOT NULL,
    must_change_password BIT NOT NULL,
    password VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    updated_on DATE,
    role_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_name_department UNIQUE (first_name, last_name, department),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE user_permissions (
    user_id BIGINT NOT NULL,
    permission ENUM(
        'ASSUME_TICKET','CANCEL_TICKET','CLOSE_TICKET','COMMENT_TICKET',
        'CREATE_TICKET','MANAGE_USERS','REOPEN_TICKET','RESOLVE_TICKET',
        'VIEW_AUDIT_LOGS','VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS'
    ),
    CONSTRAINT uk_user_permissions UNIQUE (user_id, permission),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    closed_at DATETIME(6),
    created_on DATETIME(6),
    description TEXT NOT NULL,
    resolved_at DATETIME(6),
    status ENUM('CANCELLED','CLOSED','IN_PROGRESS','PENDING','RESOLVED') NOT NULL,
    theme ENUM('ACCESS','HARDWARE','NETWORK','OTHER','SOFTWARE') NOT NULL,
    title VARCHAR(100) NOT NULL,
    customer_id BIGINT NOT NULL,
    technician_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_tickets_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_technician FOREIGN KEY (technician_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

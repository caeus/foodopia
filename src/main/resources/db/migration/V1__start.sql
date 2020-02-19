create table customers
(
    email varchar not null primary key,
    name  varchar not null,
    hashpw varchar not null
);

create table searches
(
    id   varchar   not null primary key,
    data jsonb     not null,
    at   timestamp not null,
    by   varchar   not null references customers(email)
);
create index searches_by_idx on searches(by);
create index searches_at_idx on searches(at desc);
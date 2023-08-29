create table if not exists users (
    first_name varchar(20) not null,
    last_name varchar(30),
    gender enum('Male', 'Female') not null,
    dob date not null,
    email varchar(100) not null primary key,
    mobile varchar(20)
);

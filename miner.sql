alter session set "_ORACLE_SCRIPT" = true;

create user miner identified by wjsansrk;

grant connect, resource to miner;

create table SIGN(
ID varchar(20) NOT NULL,
PASSWORD varchar(20) NOT NULL,
primary key(ID)
);
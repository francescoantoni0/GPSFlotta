CREATE DATABASE Flotta;
USE Flotta;
CREATE TABLE Posizioni
(
    veicolo     VARCHAR(8) NOT NULL,
    data        DATE       NOT NULL,
    ora         TIME       NOT NULL,
    latitudine  FLOAT,
    longitudine FLOAT,
    PRIMARY KEY (veicolo, data, ora)
);
CREATE DATABASE IF NOT EXISTS moviedb
    DEFAULT CHARACTER SET utf8mb4;

USE moviedb;

CREATE TABLE IF NOT EXISTS movies (
                                      id       VARCHAR(10)  NOT NULL,
    title    VARCHAR(100) NOT NULL,
    year     INTEGER      NOT NULL,
    director VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS stars (
                                     id         VARCHAR(10)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    birth_year INTEGER,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS stars_in_movies (
                                               star_id  VARCHAR(10) NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    FOREIGN KEY (star_id)  REFERENCES stars(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
    );

CREATE TABLE IF NOT EXISTS genres (
                                      id   INTEGER     NOT NULL AUTO_INCREMENT,
                                      name VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS genres_in_movies (
                                                genre_id INTEGER     NOT NULL,
                                                movie_id VARCHAR(10) NOT NULL,
    FOREIGN KEY (genre_id) REFERENCES genres(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
    );

CREATE TABLE IF NOT EXISTS credit_cards (
                                            id         VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name  VARCHAR(50) NOT NULL,
    expiration DATE        NOT NULL,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS customers (
                                         id             INTEGER      NOT NULL AUTO_INCREMENT,
                                         first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    credit_card_id VARCHAR(20)  NOT NULL,
    address        VARCHAR(200) NOT NULL,
    email          VARCHAR(50)  NOT NULL,
    password       VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id)
    );

CREATE TABLE IF NOT EXISTS sales (
                                     id          INTEGER     NOT NULL AUTO_INCREMENT,
                                     customer_id INTEGER     NOT NULL,
                                     movie_id    VARCHAR(10) NOT NULL,
    sale_date   DATE        NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (movie_id)    REFERENCES movies(id)
    );

CREATE TABLE IF NOT EXISTS ratings (
                                       movie_id   VARCHAR(10) NOT NULL,
    rating     FLOAT       NOT NULL,
    vote_count INTEGER     NOT NULL,
    FOREIGN KEY (movie_id) REFERENCES movies(id)
    );

-- CREATE TABLE IF NOT EXISTS movies (
--     id       VARCHAR(10)  NOT NULL,
--     title    VARCHAR(100) NOT NULL,
--     year     INTEGER      NOT NULL,
--     director VARCHAR(100) NOT NULL,
--     price    DECIMAL(6,2),
--     PRIMARY KEY (id)
-- );
--
-- CREATE TABLE IF NOT EXISTS sales (
--      id          INTEGER     NOT NULL AUTO_INCREMENT,
--      customer_id INTEGER     NOT NULL,
--      movie_id    VARCHAR(10) NOT NULL,
--      sale_date   DATE        NOT NULL,
--      quantity    INTEGER     NOT NULL DEFAULT 1,
--      PRIMARY KEY (id),
--      FOREIGN KEY (customer_id) REFERENCES customers(id),
--      FOREIGN KEY (movie_id)    REFERENCES movies(id)
-- );

COMMIT;
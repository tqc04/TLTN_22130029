-- V2__Alter_Favorites_UserId_To_Varchar.sql
-- Align favorites.user_id column type with JPA entity (String / VARCHAR)
-- Previous schema used INT for user_id which caused:
-- "Incorrect integer value: 'a1' for column 'user_id'"

ALTER TABLE favorites
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL;



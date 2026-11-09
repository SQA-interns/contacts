-- Sort key of the city column of the Contact list (R-007-03, BR-05); written with the Contact.
ALTER TABLE contact ADD COLUMN city_norm varchar(255);
UPDATE contact SET city_norm = lower(city) WHERE city IS NOT NULL;

drop table if exists words;
CREATE TABLE `words` (
  id int(10) unsigned NOT NULL AUTO_INCREMENT primary key,
  word varchar(128) NOT NULL,
  meaning mediumtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8

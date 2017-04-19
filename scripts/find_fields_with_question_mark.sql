DROP PROCEDURE IF EXISTS find_text_with_qmark;
DELIMITER //

CREATE PROCEDURE find_text_with_qmark(in tbl_name char(64), in col_name char(64))
BEGIN
	-- Drop previous table.
	set @drop_command = concat('drop temporary table if exists ',tbl_name,'_',col_name,'_text_for_qa;');
	prepare drop_stmt from @drop_command;
	execute drop_stmt;
	deallocate prepare drop_stmt;

	-- Populate the temporary table with the results of the query
	IF tbl_name = 'DatabaseObject' THEN
		-- DatabaseObject is special: need to get extra columns and also info on the related InstanceEdit
		set @create_text_for_qa = CONCAT('CREATE temporary table if not exists ',tbl_name,'_',col_name,'_text_for_qa AS
		select ',tbl_name,'.DB_ID AS DB_ID, ',tbl_name,'.',col_name,' AS TEXT_VAL, _class AS CLASS, DatabaseObject.created as Created_ID,
		Person.DB_ID as person_db_id, Person.eMailAddress, Person.firstname, Person.initial, Person.surname, Person.project
			from Person
			inner join InstanceEdit_2_author on Person.DB_ID = InstanceEdit_2_author.author
			inner join InstanceEdit on InstanceEdit_2_author.DB_ID = InstanceEdit.DB_ID
			right join DatabaseObject on InstanceEdit.DB_ID = DatabaseObject.created
			where ',tbl_name,'.',col_name,'
			regexp \'.*\\\\?[^ ]+.*\'
				and ',tbl_name,'.',col_name,' not like \'%(?-%\'
				and ',tbl_name,'.',col_name,' not like \'%-?)%\'
				and _class not in (\'EntityWithAccessionedSequence\',\'URL\')
			order by Person.project asc, Person.firstname asc, Person.surname asc, DatabaseObject._class ASC; ');
		select CONCAT('CHECKING FOR ?-LITERALS IN: ',tbl_name,'.',col_name) AS MESSAGE;

	ELSE
		set @create_text_for_qa = CONCAT('CREATE temporary table if not exists ',tbl_name,'_',col_name,'_text_for_qa AS
		select ',tbl_name,'.DB_ID AS DB_ID, ',tbl_name,'.',col_name,' AS TEXT_VAL, \'',tbl_name,'\' AS CLASS
			from ',tbl_name,'
			where ',tbl_name,'.',col_name,'
			regexp \'.*\\\\?[^ ]+.*\'
			and ',tbl_name,'.',col_name,' not like \'%(?-%\'
			and ',tbl_name,'.',col_name,' not like \'%-?)%\'; ');
		select CONCAT('CHECKING FOR ?-LITERALS IN: ',tbl_name,'.',col_name) AS MESSAGE;
	END IF;

	-- execute the statement
	prepare create_text_for_qa_stmt from @create_text_for_qa;
	execute create_text_for_qa_stmt;
	deallocate prepare create_text_for_qa_stmt;

	set @query_tbl = concat('select * from ',tbl_name,'_',col_name,'_text_for_qa ;');
	prepare query_stmt from @query_tbl;
	execute query_stmt;
	deallocate prepare query_stmt;

	-- select * from text_for_qa;
	-- drop temporary table text_for_qa;
END //
DELIMITER ;

SET @proc_call = concat('CALL find_text_with_qmark(\'DatabaseObject\', \'_displayName\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Person\', \'surname\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Person\', \'firstname\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Person\', \'initial\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Publication\', \'title\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Summation\', \'text\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL find_text_with_qmark(\'Affiliation\', \'address\');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

-- Invoke this script as: 'SET @run_update = true; \.generic_fix_chars_proc.sql'
-- select 'Character and Collation database variables, before alter to UTF8' as message;
-- show variables like 'character%';
-- show variables like 'collation%';
--
-- -- ensure that the database is using utf8 character set and collation
-- -- NOTE: MySQL does not allow ALTER DATABASE to be called from a prepared statement, so
-- -- these statements cannot be parameterized. This will need to be executed outside this
-- -- script.
-- ALTER DATABASE gk_current default character set utf8;
-- ALTER DATABASE gk_current default collate utf8_general_ci;
--
-- select 'Character and Collation database variables, AFTER alter to UTF8' as message;
-- show variables like 'character%';
-- show variables like 'collation%';

set autocommit = false;
DROP PROCEDURE IF EXISTS fix_chars_in_table_col;
DELIMITER //
-- This procedure will map invalid character sequences to valid ones. It operates on a single column in a single table.
-- This is used for situations where UTF8 characters were converted and
-- saved in a non-UTF8 format, such as Ã¨ which should be converted to è.
-- Parameters:
-- tbl_name: The name of the table to update.
-- col_name: The column in the table to update.
-- update_source: Update the source data? A boolean: set to true if you REALLY want to update, set to false if you just want the report.
CREATE PROCEDURE fix_chars_in_table_col(in tbl_name char(64), in col_name char(64), in update_source int)
BEGIN
	select concat('Fixing characters for column ',col_name,' in table ',tbl_name) as message;
	drop temporary table if exists special_chars;
	create temporary table if not EXISTS special_chars
	(
		special_char VARCHAR(10),
		replacement_char varchar(100),
        INDEX(special_char)
	) character set 'utf8' collate 'utf8_unicode_ci';

	drop temporary table if exists fixed_vals;
	create temporary table if not exists fixed_vals
	(
		db_id int unique key,
		fixed_val text
	) character set 'utf8' collate 'utf8_unicode_ci';
	-- The original mappings came from here: http://www.i18nqa.com/debug/utf8-debug.html
	insert into special_chars
		(replacement_char,special_char)
	values
		('€','â‚¬'),	('‚','â€š'),	('ƒ','Æ’'),		('„','â€ž'),
		('…','â€¦'),	('†','â€ '),	('‡','â€¡'),	('ˆ','Ë†'),
		('‰','â€°'),	('‹','â€¹'),	('Œ','Å’'),		('ÿ','Ã¿'),
		('Ž','Å½'),		('‘','â€˜'),	('’','â€™'),	('“','â€œ'),
		-- ('Š',concat(0xC5,0xA0)),
		-- Found this sequence for right-double-quote.
		-- normally, 0xC3 0xA2 0xE2 0x82 0xAC would be reduced to â€
		-- but 0xC2 0x9D can't be mapped to anything so the sequence "â€<0x9D>"
		-- will not be produced (because of that extra 0xC2). So, this mapping
		-- needed to be added explicitly.
		-- ('”',concat(0xC3,0xA2,0xE2,0x82,0xAC,0xC2,0x9D)),
		-- ('”',concat('Ã¢â‚¬Â',0x9D)),
		-- ('”',concat(0xC3,0xA2,0xE2,0x82,0xAC,0x9D)),
		('-','â€‘'), -- <-- This mapping was discovered by Lisa.
		('•','â€¢'),	('–','â€“'),	('—','â€”'),	('ª','Âª'),
		('˜','Ëœ'),		('™','â„¢'),	('š','Å¡'),		('›','â€º'),
		('œ','Å“'),		('ž','Å¾'),		('Ÿ','Å¸'),		('¡','Â¡'),
		('¢','Â¢'),		('£','Â£'),		('¤','Â¤'),		('¥','Â¥'),
		('¦','Â¦'),		('§','Â§'),		('¨','Â¨'),		('©','Â©'),
		('¬','Â¬'),		('®','Â®'),		('¯','Â¯'),		('°','Â°'),
		('±','Â±'),		('²','Â²'),		('³','Â³'),		('´','Â´'),
		('µ','Âµ'),		('¶','Â¶'),		('·','Â·'),		('¸','Â¸'),
		('¹','Â¹'),		('º','Âº'),		('»','Â»'),		('¼','Â¼'),
		('½','Â½'),		('¾','Â¾'),		('¿','Â¿'),		('À','Ã€'),
		('Â','Ã‚'),		('Ã','Ãƒ'),		('Ä','Ã„'),		('Å','Ã…'),
		('Æ','Ã†'),		('Ç','Ã‡'),		('È','Ãˆ'),		('É','Ã‰'),
		('Ê','ÃŠ'),		('Ë','Ã‹'),		('Ì','ÃŒ'),		('Î','ÃŽ'),
		('Ñ','Ã‘'),		('Ò','Ã’'),		('Ó','Ã“'),		('Ô','Ã”'),
		('Õ','Ã•'),		('Ö','Ã–'),		('×','Ã—'),		('Ø','Ã˜'),
		('Ù','Ã™'),		('Ú','Ãš'),		('Û','Ã›'),		('Ü','Ãœ'),
		('Þ','Ãž'),		('ß','ÃŸ'),		('á','Ã¡'),		('â','Ã¢'),
		('ã','Ã£'),		('ä','Ã¤'),		('å','Ã¥'),		('æ','Ã¦'),
		('ç','Ã§'),		('è','Ã¨'),		('é','Ã©'),		('ê','Ãª'),
		('ë','Ã«'),		('ì','Ã¬'),		('í','Ã­'),		('î','Ã®'),
		('ï','Ã¯'),		('ð','Ã°'),		('ñ','Ã±'),		('ò','Ã²'),
		('ó','Ã³'),		('ô','Ã´'),		('õ','Ãµ'),		('ö','Ã¶'),
		('÷','Ã·'),		('ø','Ã¸'),		('ù','Ã¹'),		('ú','Ãº'),
		('û','Ã»'),		('ü','Ã¼'),		('ý','Ã½'),		('þ','Ã¾'),
		('«','Â«'),
		-- the alpha character
		('α','Î±'),
		-- Found during Lisa's review:
		('β','Î²'),('β','ÃŽÂ²'), -- There are some really garbled sequences that reduce to Î² when they should be "ß".
		('à', concat(0xC3,0x83,0xC2,0xA0)), -- this is the correct mapping for 'à'.
		('à', concat(0xC3,0x83,0xC6,0x92,0xC3,0x82,0xC2,0xA0)), -- This is to fix a bad example in the database.
		(concat(0xC3,0x83),concat(0xC3,0x83,0x3F)), -- this was found by manually searching. Maps to Ã.
		('í', concat(0xC3,0x83,0xC2,0xAD)), /* this one was found by manually searching.
		The sequence 0xC3 0x83 0xC2 0xAD produces a Ã followed by a NBH character. In the case of 'GarcÃ­a-Trevijano', it seems likely that
		the sequence should have been replaced with 'í' (whose hex sequence is C3AD). On its own that character sequence makes no sense.*/
		(' ', concat(0xC2,0xA0)),
		(' ', concat(0xC3,0x82,0xC2,0xA0)),
		-- new mappings:
		('δ', 'ÃŽÂ´'), ('ε', 'ÃŽÂµ'),
		('δ', 'ÃƒÅ½Ã‚Â´'),
		('γ', 'ÃƒÅ½Ã‚Â³'),
		-- ('γδ', 'ÃƒÅ½Ã‚Â³ÃƒÅ½Ã‚Â´'),
		('-', '‒'), -- This is actually a valid dash (FIGURE DASH), but some tools render it as "ΓÇæ".
		('-', 'ΓÇæ'),
		('-', concat( 0xE2, 0x80, 0x92 )) -- same thing as above, but with the hex code.
		;

	-- select 'The mappings are: ' as message;
	-- select concat('''',special_char,'''') as special_char, concat('''',replacement_char,'''') as replacement_char from special_chars;

	drop temporary table if exists things_to_fix;
	set @create_things_to_fix = concat('CREATE temporary table if not exists things_to_fix
											select distinct ',tbl_name,'.DB_ID as DB_ID, ',tbl_name,'.',col_name,' AS fixed_value
											from ',tbl_name,', special_chars
											where binary ',tbl_name,'.',col_name,' like CONCAT(''%'',special_chars.special_char,''%'') ');

	prepare create_things_to_fix_stmt from @create_things_to_fix;
	execute create_things_to_fix_stmt;
	deallocate prepare create_things_to_fix_stmt;

	set @summary_report_query= CONCAT('	select special_chars.special_char, hex(special_char), replacement_char, count(distinct ',tbl_name,'.DB_ID)
					from ',tbl_name,', special_chars
					where BINARY ',tbl_name,'.',col_name,'
					like CONCAT(''%'',special_chars.special_char,''%'')
					group by special_char, hex(special_char), replacement_char
					order by count(distinct ',tbl_name,'.DB_ID) asc, special_char asc');

	-- Before running the fix, let's see what there is that has "bad" characters.
	set @detailed_report_query= CONCAT('
					select * from
					( select distinct special_char, hex(special_char), replacement_char, ',tbl_name,'.DB_ID, ',tbl_name,'.',col_name,'
					from special_chars, ',tbl_name,'
					where BINARY ',tbl_name,'.',col_name,'
					like CONCAT(''%'',special_chars.special_char,''%'') ) as records_with_special_chars
					left join
					( select ',tbl_name,'.db_id,
					ieCreator.dateTime as created_time, ieCreator.note as creation_note,
					concat(Creator.firstname, \' \', Creator.initial, \' \' , Creator.surname ) as creator,
					ieModifier.note as modified_note, ieModifier.dateTime as modified_time,
					concat(Modifier.firstname, \' \', Modifier.initial, \' \' , Modifier.surname ) as modifier
                    from ',tbl_name,'
                    left join DatabaseObject as DBO on DBO.db_id = ',tbl_name,'.db_id
					left join InstanceEdit as ieCreator on ieCreator.DB_ID = DBO.created
					left join InstanceEdit_2_author as ie2a_creator on ie2a_creator.DB_ID = ieCreator.DB_ID
					left join Person as Creator on ie2a_creator.author = Creator.DB_ID
					left join DatabaseObject_2_modified on DatabaseObject_2_modified.DB_ID = DBO.DB_ID
					left join InstanceEdit as ieModifier on DatabaseObject_2_modified.modified = ieModifier.DB_ID
					left join InstanceEdit_2_author as ie2a_modifier on ie2a_modifier.DB_ID = ieModifier.DB_ID
					left join Person as Modifier on ie2a_modifier.author = Modifier.DB_ID ) as object_info
                    on object_info.db_id = records_with_special_chars.db_id;  ');

	-- select @summary_report_query,@detailed_report_query;

	prepare summary_report_statement from @summary_report_query;
	prepare detailed_report_statement from @detailed_report_query;

	select concat('Summary report for ',tbl_name) as message;
	execute summary_report_statement;
	select concat('Detailed report for ',tbl_name) as message;
	execute detailed_report_statement;

	deallocate prepare summary_report_statement;
	deallocate prepare detailed_report_statement;

	begin
		DECLARE done INT DEFAULT FALSE;
		declare the_id varchar(10);
		declare tmp_val text;
 		declare old_char varchar(10);
		declare new_char varchar(10);


		declare things_to_fix_cursor cursor for select * from things_to_fix;
		DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

		OPEN things_to_fix_cursor;
		-- Main loop: loop through the cursor of Person records that have name fields that need to be fixed.
		things_to_fix_loop: LOOP
			FETCH things_to_fix_cursor into the_id, tmp_val;
			-- select the_id, tmp_firstname, tmp_initial, tmp_surname;

			SELECT  special_chars.special_char, special_chars.replacement_char
			INTO old_char , new_char
			FROM special_chars
			WHERE BINARY tmp_val LIKE CONCAT('%', special_chars.special_char, '%')
			order by char_length(special_chars.special_char) desc
			LIMIT 1;
			-- 1 transaction per loop iteration,
			-- there could be multiple DMLs per iteration if a Person has more than one different "special" char that needs to be replaced.
			start transaction;

			-- we need a new block here so that we can set a different handler for the queries in the inner loop.
			-- It appears that MySQL does not let specific handlers be attached to specific cursors/queries.
			begin
				DECLARE done_char_replacement int default FALSE;
				DECLARE CONTINUE HANDLER FOR NOT FOUND SET done_char_replacement = TRUE;
				-- keep going until there are no more replacements left to do...
				while not done_char_replacement do

					-- do the replacement.
					select replace(tmp_val, old_char, new_char) into tmp_val;

					-- insert to temporary table that holds results.
					insert into fixed_vals (db_id, fixed_val) values (the_id,  tmp_val)
						on duplicate key update fixed_val = tmp_val;

					-- Now, try to get the next replacement characters. If there are none, the NOT FOUND handler
					SELECT special_chars.special_char, special_chars.replacement_char
					INTO old_char , new_char
					FROM special_chars
					WHERE BINARY tmp_val LIKE CONCAT('%', special_chars.special_char, '%')
					order by char_length(special_chars.special_char) desc
					LIMIT 1;
				end while;
			end;
			commit;

			if done then
				leave things_to_fix_loop;
			end if;
		end loop;
		CLOSE things_to_fix_cursor;
	end;
	Select 'These are the fixes that will be applied: ' as message;
	SELECT * FROM fixed_vals ORDER BY db_id;

	if update_source then
		begin
			declare exit handler for sqlexception
			begin
				rollback;
				select 'Rolling back due to sqlexception!' as message;
			end;
			declare exit handler for sqlwarning
			begin
				rollback;
				select 'Rolling back due to sqlwarning!' as message;
			end;

			start transaction;
			set @update_str=concat('update ',tbl_name,', fixed_vals
									set ',tbl_name,'.',col_name,' = fixed_vals.fixed_val
									where ',tbl_name,'.db_id = fixed_vals.db_id ');
			prepare update_statement from @update_str;
			execute update_statement ;
			deallocate prepare update_statement ;
			commit;
		end;
	end if;

END //
DELIMITER ;
-- Call the procedure. Invoke this script as: 'SET @run_update = true; \.generic_fix_chars_proc.sql'
-- From the shell:
-- $ mysql -u root -p -e"SET @run_update = true; `cat $(pwd)/generic_fix_chars_proc.sql`"
SET @proc_call = concat('CALL fix_chars_in_table_col(\'Person\',\'firstname\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Person\',\'initial\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Person\',\'surname\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Publication\',\'title\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Affiliation\',\'address\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'DatabaseObject\',\'_displayName\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Summation\',\'text\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

SET @proc_call = concat('CALL fix_chars_in_table_col(\'Event_2_name\',\'name\',',@run_update,');');
PREPARE stmt from @proc_call;
EXECUTE stmt;

deallocate prepare stmt;

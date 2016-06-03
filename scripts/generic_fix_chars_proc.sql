show variables like 'character%';
show variables like 'collation%';

ALTER DATABASE gk_current default character set utf8;
ALTER DATABASE gk_current default collate utf8_general_ci;

show variables like 'character%';
show variables like 'collation%';
--
set autocommit = false;
DROP PROCEDURE IF EXISTS fix_chars_in_table_col;
DELIMITER //
CREATE PROCEDURE fix_chars_in_table_col(in tbl_name char(64), in col_name char(64), in update_source int)
BEGIN
	drop temporary table if exists special_chars;
	create temporary table if not EXISTS special_chars
	(
		special_char VARCHAR(10),
		replacement_char varchar(10),
        INDEX(special_char)
	) character set 'utf8' collate 'utf8_general_ci';

	drop temporary table if exists fixed_vals;
	create temporary table if not exists fixed_vals
	(
		db_id int unique key,
		fixed_val text
	) character set 'utf8' collate 'utf8_general_ci';

	insert into special_chars
		(replacement_char,special_char)
	values
		('€','â‚¬'),	('‚','â€š'),	('ƒ','Æ’'),		('„','â€ž'),
		('…','â€¦'),	('†','â€'),		('‡','â€¡'),	('ˆ','Ë†'),
		('‰','â€°'),	('Š',x'C5A0'),	('‹','â€¹'),	('Œ','Å’'),
		('Ž','Å½'),		('‘','â€˜'),	('’','â€™'),	('“','â€œ'),
		('”','â€'),		('•','â€¢'),	('–','â€“'),	('—','â€”'),
		('˜','Ëœ'),		('™','â„¢'),	('š','Å¡'),		('›','â€º'),
		('œ','Å“'),		('ž','Å¾'),		('Ÿ','Å¸'),		('¡','Â¡'),
		('¢','Â¢'),		('£','Â£'),		('¤','Â¤'),		('¥','Â¥'),
		('¦','Â¦'),		('§','Â§'),		('¨','Â¨'),		('©','Â©'),
		('ª','Âª'),		('«','Â«'),
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
		('ÿ','Ã¿'),
        -- ('’','†™'), -- this handles the '’' ( the "prime" character), in some Summation.text fields.
        -- Originally, the field may contain Ã¢â‚¬â„¢ which should translation to â€™ which maps to ’.
        -- But because 'â€' *on its own* maps to '†', a secondary mapping must be done.
        -- Another way to do this might be to rank the mappings to ensure that they are executed in a proper order. ordering them by byte length might work
        -- for this situation.
        -- UPDATE: I changed it to do replacements in order of character length.
        -- 'Ã¢â‚¬Â²' - This sequence is transformed to 'â€²' which ends up as '†²'. It looks weird to me, but it seems to be the only possibly correct sequence, so...
        (x'C383',x'C3833F'), -- this was found by manually searching.
		('í',x'C383C2AD'), /* this one was found by manually searching.
        The sequence C383C2AD produces a Ã followed by a NBH character. In the case of 'GarcÃ­a-Trevijano', it seems likely that
        the sequence should have been replaced with 'í' (whose hex sequence is C3AD. On its own that character sequence makes no sense.*/
		('',x'C2A0');
		-- ('','Â­'),

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
					group by special_char
					order by count(distinct ',tbl_name,'.DB_ID) asc, special_char asc');

	-- Before running the fix, let's see what there is that has "bad" characters.

	set @detailed_report_query= CONCAT('	select distinct special_char, hex(special_char), replacement_char, ',tbl_name,'.DB_ID, ',tbl_name,'.',col_name,'
					from ',tbl_name,', special_chars
					where BINARY ',tbl_name,'.',col_name,'
					like CONCAT(''%'',special_chars.special_char,''%'')');

    -- select @summary_report_query,@detailed_report_query;

    prepare summary_report_statement from @summary_report_query;
    prepare detailed_report_statement from @detailed_report_query;

    execute summary_report_statement;
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
	SELECT * FROM fixed_vals ORDER BY db_id;

    if update_source then
		begin
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
CALL fix_chars_in_table_col('Person','firstname',true);
CALL fix_chars_in_table_col('Person','initial',true );
CALL fix_chars_in_table_col('Person','surname',true);
CALL fix_chars_in_table_col('Publication','title',true);
CALL fix_chars_in_table_col('Affiliation','address',true);
CALL fix_chars_in_table_col('DatabaseObject','_displayName',true);
CALL fix_chars_in_table_col('Summation','text',true);
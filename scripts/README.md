# Reactome Perl Scripts

### all_pathway_logic_tables.pl
Wrapper to reaction_logic_table.pl

### backup_db.pl
Utility script: backs up a database and zips the dump file.

### check_disk_space.pl
Utility script: checks free disk space, sends email warning if threshold exceeded. _Could_ be run from cron.

### check_instance_classes_in_release.pl
Utility script: Prints number of instances of each instance class. Run manually.

### check4null_class.pl
Utiltiy script: Checks for null attributes. Run manually.

### complex_export.pl
Exports complexes to TSV file. Could be used for QA. Run manually.

### copy_database.pl
Utility script: Copies instances hierarchically from a source Reactome database to a target Reactome database.

### createDatabase.pl
Utility script: Simple database creation script. Run manually.

### delete_refs_with_null_refdb.pl
Was a one-time fix script. Could be useful again.

### deleteByDbId.pl
Utility script: Deletes objects by their DB_ID. Run manualy.

### efo.pl

### find_fields_with_question_mark.sql
Searches for question marks (“?”) in fields that should not have them (such as a person’s first name). This can be used to find places where “special” characters got lost and were convereted to a “?”. Must be run manually. Note: This script will create a stored procedure!
Fields checked: DatabaseObject._displayName, Person.surname, Person.firstname, Person.initial, Publication.title, Summation.text, Affiliation.address

Was a one-time fix script. Could be useful again.

### fix_characters.sh
Wrapper to generic_fix_chars_proc.sql. Run manually AND by the Slicing Tool (runProjectSlicingTool.sh)

### generic_fix_chars_proc.sql
SQL script creates a stored procedure to fix weird character sequences, and then executes the procedure against specific fields: DatabaseObject._displayName, Person.surname, Person.firstname, Person.initial, Publication.title, Summation.text, Affiliation.address

This script could be run manually. It is also called from fix_characters.sh which is called from runProjectSlicingTool.sh as a part of the Release process.

### install_slice_db.sh
Installs a slice database. Used by curators. Run manually.

### instancePrint.pl
Utility script: This seems to print an instance from the database. Run manually.

### logs_by_year.sh

### monitor_mysql.pl

### mysql.pl

### reaction_logic_table.pl
Creates a tab delimited text file describing a directed graph with nodes being reactions and their physical entities. Wrapped by all_pathway_logic_tables.pl

Run manually, when needed. Output used by MPBiopath project - an affiliate of Reactome.

### restore_datbase.pl
Utility script: restores a database from a file. Run manually.

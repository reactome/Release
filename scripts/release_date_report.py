#! /usr/bin/python3

"""This script will check for Events that have a NULL releaseDate and try to
pick the correct one, based on the first appearance of the Event. It will look
through all "test_reactome_NN" databases."""

# Database connector package.
import pymysql
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--host', help="hostname of the machine where the database is", required=True)
parser.add_argument('--user', help="username to use when connecting to the database", required=True)
parser.add_argument('--pass', help="password to use when connecting to the database", required=True)
parser.add_argument('--name', help="the name of the database to connect to", default="gk_current")
parser.add_argument('--port', help="the port to use when connecting to the database", default=3306)
parser.add_argument('--target', help="The target database to update", default="gk_current", required=False)

args = vars(parser.parse_args())

db_host = args['host']
db_user = args['user']
db_passwd = args['pass']
db_name = args['name']
db_port = int(args['port'])
db_target = args['target']

if db_host is None or db_user is None or db_passwd is None or db_name is None:
    parser.print_help()
    exit(1)

# First thing we have to do is get a list of events in the current database with null release date
db = pymysql.connect(host=db_host, user=db_user, passwd=db_passwd, db=db_name, port=db_port)

gk_current_result = []
db_names = []
fixed_events = {}

# These values come from https://docs.google.com/spreadsheets/d/1u4ZPv50BkQtn-BGnEXcjjHpussvQlZbBHdgcwOJxmfE/edit?pref=2&pli=1#gid=1404819668
release_dates = {
    1: 'unknown', 2: '2003-02-03', 3: '2003-03-06', 4: '2003-05-04',
    5: '2003-09-03', 6: '2003-11-03', 7: '2003-12-18', 8: '2004-03-06',
    9: 'unknown', 10: '2004-07-06', 11: '2004-10-27', 12: '2005-02-01',
    13: '2005-04-11', 14: '2005-06-13', 15: '2005-09-26', 16: '2006-01-23',
    17: '2006-04-24', 18: '2006-08-02', 19: '2006-11-16', 20: '2007-02-28',
    21: '2007-05-15', 22: '2007-09-04', 23: '2007-12-11', 24: '2008-03-11',
    25: '2008-06-30', 26: '2008-09-30', 27: '2008-12-17', 28: '2009-04-01',
    29: '2009-06-24', 30: '2009-09-30', 31: '2009-12-15', 32: '2010-03-16',
    33: '2010-06-15', 34: '2010-09-21', 35: '2010-12-14', 36: '2011-03-15',
    37: '2011-06-14', 38: '2011-09-20', 39: '2011-12-06', 40: '2012-03-13',
    41: '2012-06-12', 42: '2012-09-18', 43: '2012-12-04', 44: '2013-03-12',
    45: '2013-06-11', 46: '2013-09-18', 47: '2013-12-04', 48: '2014-03-12',
    49: '2014-06-10', 50: '2014-09-30', 51: '2014-12-11', 52: '2015-03-19',
    53: '2015-06-17', 54: '2015-09-22', 55: '2015-12-15', 56: '2016-03-23',
    57: '2016-06-27', 58: '2016-09-19', 59: '2016-12-12'
}

with db.cursor() as cursor:
    cursor.execute("""SELECT e.*, """ + db_name + """.DatabaseObject._displayName
        FROM """ + db_name + """.Event AS e
        LEFT JOIN """ + db_name + """.DatabaseObject ON """ + db_name + """.DatabaseObject.db_id = e.db_id
        WHERE e.releaseDate IS NULL and e.evidenceType IS NULL;""")
    #gk_current_result = cursor.fetchmany(10000)
    gk_current_result = cursor.fetchall()
    print ( "Number of events without release date in " + db_name + ": %d" % len(gk_current_result))

# Ok, now that we have this list, we need to get a list of all database names so we can go through the databases to get release dates.
# Now: Start with the OLDEST database and work towards the present. This is because it is possible that an Event might have been "unreleased".
# We want the OLDEST place where the Event appears so that means that we need to start at the oldest database.
with db.cursor() as cursor:
    cursor.execute("""SELECT * from Information_Schema.schemata WHERE schema_name LIKE 'test_reactome___' ORDER BY schema_name ASC;""")
    db_names = cursor.fetchall()

    print ( "Number of database schemas to check for release dates: %d" % len(db_names) )

# We now have a list of database names, let's go through it and for each event, check to see if exists in a database and if it has a release date.
print(release_dates)
for n in db_names:
    name = n[1]
    db_number = int((name.split(sep="_"))[2])
#    print("looking in %s" % name)
    i = 0
    for event_results in gk_current_result:
        # Check to see if this event has a null release date in database "db_name"
        with db.cursor() as cursor:
            db_id = event_results[0]
            #print ("Checking event: " + str(db_id) + " (" + event_results[9] + ")")
            #query = "select * from " + name + ".Event where db_id = " + str(db_id)

            # Only run the query if the db_id has not yet been fixed.
            if str(db_id) not in fixed_events:
                cursor.execute("SELECT db_id FROM "+name+".Event WHERE db_id = %s" , [str(db_id)] )
                result = cursor.fetchall()
                i += 1
                # If there are results, then it means that the Event was released AFTER this database,
                # so the release date should be that database's release date.
                if len(result) > 0 and str(db_id) not in fixed_events:
                    print(str(db_id) + " (" + event_results[9] + ")" + " was first found in "+name+", so it will get the release date of database version "+str(db_number)+": "+release_dates[db_number])
                    fixed_events[str(db_id)] = { 'release_date': release_dates[db_number],
                                                  'db_num': db_number,
                                                  'event_name': event_results[9] }

    print(str(i) + " DB_IDs checked in " + name)
    remaining_db_ids = len(gk_current_result) - len(fixed_events)
    print("Release dates for " + str(len(fixed_events)) + " have been found so far.")
    print(str(remaining_db_ids) + " remaing DB IDs to check.\n\n")

# We need to make a list of Events that are not yet resolved
# This will be a list of DB_IDs of things that still need to be fixed (the set of all Events with NULL release date minus the set of things that can be fixed)
still_to_be_fixed = set( [ str(item[0]) for item in gk_current_result ] ).difference( fixed_events.keys() )

print ("Items that still need to be fixed: "+str(len(still_to_be_fixed)))
#print(still_to_be_fixed)
# print(fixed_events)

if len(still_to_be_fixed) > 0:

    # this query will find the oldest release date from a set of related Pathway/Reaction events - but will be run against gk_current
    # (some of the older databases don't even have Event.releaseDate so this query wouldn't even make sense to run there).
    get_release_dates_from_subevents="""SELECT Pathway_2_hasEvent.db_id as path2eventID, Event.db_id as event_db_id,
        Event._doRelease, Event.releaseStatus, Event.releaseDate, DatabaseObject._displayName,
        Pathway_2_hasEvent.hasEvent_rank, Pathway_2_hasEvent.hasEvent_class,
        min(min_date_subq.min_release_date) as min_release_date
    from Event
    inner join Pathway_2_hasEvent on Pathway_2_hasEvent.hasEvent = Event.db_id
    inner join DatabaseObject on DatabaseObject.db_id = Event.db_id
    inner join (select Pathway_2_hasEvent.db_id as path2eventID, min(releaseDate) min_release_date
                from Event
                inner join Pathway_2_hasEvent on Pathway_2_hasEvent.hasEvent = Event.db_id
                group by Pathway_2_hasEvent.db_id) as min_date_subq
        on min_date_subq.path2eventID = Pathway_2_hasEvent.db_id
    where (Pathway_2_hasEvent.hasEvent_class like '%%Reaction%%' or Pathway_2_hasEvent.hasEvent_class like '%%Pathway%%')
        and Event.releaseDate is null and min_release_date is not null and Event.db_id in ( """ + ', '.join(still_to_be_fixed) + """ )
    group by Event.db_id
    order by Pathway_2_hasEvent.db_id, Pathway_2_hasEvent.hasEvent_rank asc ;
    """

    # print(get_release_dates_from_subevents)

    with db.cursor() as cursor:
        cursor.execute(get_release_dates_from_subevents)
        release_dates_from_subevents = cursor.fetchall()
        print("Found " + str(len(release_dates_from_subevents)) + " releaseDates in subevents (subpathways/Reactions)")

        for r in release_dates_from_subevents:
            #print (r)
            db_id = r[1]
            if str(db_id) in still_to_be_fixed and db_id not in fixed_events.keys():
                fixed_events[str(db_id)] = { 'release_date': str(r[8]),
                                                      'db_num': 'DERIVED FROM RELATED SUBPATHWAY/REACTION',
                                                      'event_name': r[5] }
                print(str(db_id) + " (" + r[5] + ")" + " is getting a releaseDate based on related event/subpathway/reaction (" + str(r[0]) + ") : " + str(r[8]))
            else:
                # Actually, I don't think this will ever print, everything in here was already selected from the database...
                print("Release date for " + str(db_id) + " could not be determined from related events (subpathways/reactions).")
    remaining_db_ids = len(gk_current_result) - len(fixed_events)
    print("Release dates for " + str(len(fixed_events)) + " have been found so far.")
    print(str(remaining_db_ids) + " remaining DB IDs to check.\n\n")

# This will be a list of DB_IDs of things that still need to be fixed (the set of all Events with NULL release date minus the set of things that can be fixed)
still_to_be_fixed = set( [ str(item[0]) for item in gk_current_result ] ).difference( fixed_events.keys() )

if len(still_to_be_fixed) > 0:
    version_10_release_date = release_dates[10]
    print ("Items that still need to be fixed: "+str(len(still_to_be_fixed)))
    print ("If we couldn't figure out a release date by this point, we'll just default to the Release 10 release date (" + version_10_release_date + " ) ")
    for db_id in still_to_be_fixed:
        fixed_events[str(db_id)] = { 'release_date': version_10_release_date,
                                              'db_num': 'Defaulting to Release 10 because nothing else could be found',
                                              'event_name': '' }
        print(str(db_id) + " is getting a releaseDate based on a Release #10 (" + version_10_release_date + ") because we can't find anything else for it")

    remaining_db_ids = len(gk_current_result) - len(fixed_events)
    print("Release dates for " + str(len(fixed_events)) + " have been found so far.")
    print(str(remaining_db_ids) + " remaining DB IDs to check.\n\n")

with open('./update_events.sql','w') as dml_file, open('simple_list.txt','w') as simple_list:
    for fixed_event in sorted(fixed_events):
        dml_file.write("-- update Event: \""+fixed_events[fixed_event]['event_name']+"\" with ID: "+str(fixed_event)+" to use releaseDate: "+ fixed_events[fixed_event]['release_date'] +" of database version: "+str(fixed_events[fixed_event]['db_num'])+" \n")
        # select AUTO_INCREMENT from INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DatabaseObject' and TABLE_SCHEMA = 'test_reactome_58'
        dml_file.write("UPDATE " + db_target + ".Event SET releaseDate='" + fixed_events[fixed_event]['release_date'] + "' WHERE db_id = '" + fixed_event + "'\n")
        simple_list.write(fixed_event + ',' + fixed_events[fixed_event]['release_date']+ '\n')
db.close()

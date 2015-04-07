#!/usr/bin/python
import subprocess as sp
import os, os.path, time, sys


# A script to check if tomcat is having authentication error 
# and restart if need be
# Sheldon McKay <sheldon.mckay@gmail.com>

file = "/usr/local/reactomes/Reactome/production/apache-tomcat/logs/catalina.out"
msg  = "WARNING: An attempt was made to authenticate"

# modification date
mtime = os.stat(file).st_mtime
now   = time.time()
age   = int(round(now - mtime))

# Skip if the log file was not touched in the last five minutes
if age > 300:
    print "Log file not touched in the last five minutes"
    sys.exit()

mtime = os.stat('/tmp/tomcat_restart').st_mtime
age   = int(round(now - mtime))

# Skip if tomcat was restarted within the last 10 minutes
if age < 600:
    alert = "tomcat was restarted recently (" + str(age) + " seconds ago)"
    print alert
    sys.exit()
    
# Open the last 100 lines of the log file
lines = sp.check_output(["tail", "-100", file])

# Loom for the connection error message
# Restart tomcat if we have 10 failed connection error  
idx = 0
for line in lines.split('\n'):
    if msg in line:
        print line
        idx += 1
        if idx > 9:
            print "RESTART"
            sp.call(["/etc/init.d/tomcat7", "restart"])
            sp.call(["touch", "/tmp/tomcat_restart"])
            break



#!/usr/bin/python
import sys
import socket
from geoip import geolite2 as gl2

print '"weight";"lat";"lon"'

locales = {}

for line in sys.stdin:
    line = line.rstrip('\n')
    (count,IP) = line.split(',')

    name = None
    try:
        name = socket.gethostbyname(IP)
    except Exception:
        sys.exc_clear()
        sys.stderr.write("IP of "+IP+" is not known\n")

    if name is not None:
        match = gl2.lookup(name)

        if match is not None:
            location = match.location
            locale = str(location[0]) + ';' + str(location[1])
            
            try:
                locales[locale] += int(count)
            except:
                locales[locale] = int(count)

                
for key in locales:
    count = locales[key]
    if int(count) > 10000:
        sys.stderr.write("Filtering out freakishly large hit number ("+str(count)+") for "+key+"\n")
        continue
    print str(count) + ';' + key

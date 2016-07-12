#!/usr/bin/python
import sys
import socket
from geoip import geolite2 as gl2

max_count = 1000000;

def validate_ip(s):
    a = s.split('.')
    if len(a) != 4:
        return False
    for x in a:
        if not x.isdigit():
            return False
        i = int(x)
        if i < 0 or i > 255:
            return False
    return True


print '"weight";"lat";"lon"'

locales = {}

stamp = None;
for line in sys.stdin:
    line = line.rstrip('\n')

    if 'Last updated' in line:
        stamp = line
        continue

    if not ',' in line:
        continue

    sys.stderr.write("LINE: "+line+"\n");

    (count,IP) = line.split(',')

    if not validate_ip(IP):
        continue

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
            if match.location and len(match.location) > 1:
                locale = str(location[0]) + ';' + str(location[1])
                try:
                    locales[locale] += int(count)
                except:
                    locales[locale] = int(count)

                
if stamp:
    print stamp

total = 0
for key in locales:
    count = locales[key]
    total += int(count);
    if int(count) > max_count:
        sys.stderr.write("Filtering out freakishly large hit number ("+str(count)+") for "+key+"\n")
        continue
    print str(count) + ';' + key

#sys.stderr.write("Total number of analyses for this period: " + str(total))
print "\n"  


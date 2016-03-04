#!/bin/bash

if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

# mkdir /nfs/reactome/reactome/tmp/newsusers.bak
# cp /etc/passwd /etc/shadow /etc/group /etc/gshadow /root/newsusers.bak
# cd /path/to/location
# cat passwd.mig >> /etc/passwd
# cat group.mig >> /etc/group
# cat shadow.mig >> /etc/shadow
# /bin/cp gshadow.mig /etc/gshadow

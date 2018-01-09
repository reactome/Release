#!/bin/bash

if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

HOST=$(hostname -i)
mkdir -p /home/smckay/accounts/$HOST/newsusers.bak
cp /etc/passwd /etc/shadow /etc/group /etc/gshadow /home/smckay/accounts/$HOST/newsusers.bak
cd /home/smckay/home/accounts/$HOST/

cat passwd.mig | grep -v oicradmin |grep -v solr >> /etc/passwd
cat group.mig >> /etc/group
cat shadow.mig >> /etc/shadow
/bin/cp gshadow.mig /etc/gshadow

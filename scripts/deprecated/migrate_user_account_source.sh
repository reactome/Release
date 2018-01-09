#!/bin/bash

if [[ $EUID -ne 0 ]]
then
    echo -e "\nSorry, you must be the root user run this script!" 1>&2
    exit 1
fi

if [[ ! -n $1 ]]
then
    echo "You must supply the new hostname as a argument"
    exit 1
fi

mkdir -p /nfs/reactome/reactome/tmp/migrate/$1
export UGIDLIMIT=500
awk -v LIMIT=$UGIDLIMIT -F: '($3>=LIMIT) && ($3!=65534)' /etc/passwd > /nfs/reactome/reactome/tmp/migrate/$1/passwd.mig
awk -v LIMIT=$UGIDLIMIT -F: '($3>=LIMIT) && ($3!=65534)' /etc/group > /nfs/reactome/reactome/tmp/migrate/$1/group.mig
awk -v LIMIT=$UGIDLIMIT -F: '($3>=LIMIT) && ($3!=65534) {print $1}' /etc/passwd | tee - | \
egrep -f - /etc/shadow > /nfs/reactome/reactome/tmp/migrate/$1/shadow.mig
cp /etc/gshadow /nfs/reactome/reactome/tmp/migrate/$1/gshadow.mig
rsync -azrP /home smckay@$1:home
scp -r /nfs/reactome/reactome/tmp/migrate/$1/* smckay@$1:home/accounts



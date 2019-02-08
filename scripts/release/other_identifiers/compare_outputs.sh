#! /bin/bash

echo "FILE, NEW, OLD"

for f in $(ls  output) ; do
  OUTPUT_SIZE_66=0
  declare -i OUTPUT_SIZE=$(ls -l output/$f | tr -s ' ' | cut -f 5 -d ' ')
  [[ -e archive/66/output/$f ]] && declare -i OUTPUT_SIZE_66=$(ls -l archive/66/output/$f  | tr -s ' ' | cut -f 5 -d ' ')
  if (($OUTPUT_SIZE != $OUTPUT_SIZE_66)) ; then
   #echo "$f has different sizes: $OUTPUT_SIZE vs Old: $OUTPUT_SIZE_66  "
   echo "$f, $OUTPUT_SIZE, $OUTPUT_SIZE_66"
  fi
done;

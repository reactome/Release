 for file in `find . |perl -pe 's/\.\///'`
 do 
     diff=$(diff ./$file /tmp/tarball/53/Release/website/html/wordpress/$file)
     if [[ -n $diff ]]
     then
	 echo $file
	 echo DIFF $diff
     fi
done


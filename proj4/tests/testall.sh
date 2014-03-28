
echo "------- RUNNING EXAMPLE TEST CASES --------"
for f in `ls ../examples/*.java`
do
	echo -n $f ": "
	../bin/mjcompile-ssa -t $f > out1 2> err1
	../project4/bin/mjcompile-ssa -t ../proj4/examples/$f > out2 2> err2 # replace this with the path to your reference solution
	diff out1 out2 > out3

	if [ -s out3 ] 
	then
		echo "Failed. (!)"
	else
		if [ -s err1 ] 
		then
			echo "Failed. (!)"
		else
			if [ -s err2 ] 
			then
				echo "Failed. (!)"
			else
				echo "Passed."
			fi
		fi
	fi
	rm out1 out2 out3 err1 err2
done
echo


echo "------- RUNNING MANUAL TEST CASES --------"
for f in `ls test*.java | sort`
do
	echo -n $f ": "
	../bin/mjcompile-ssa -t $f 2> err > out
	if [ -s "err" ] 
	then
		echo "Passed."
	fi
	if [ -s "out" ] 
	then
		echo "Failed. (!)"
	fi
	if [ "$f" = "test023.java" ]
	then
		echo "(note: test023.java should fail.)"
	fi

	rm out err
done

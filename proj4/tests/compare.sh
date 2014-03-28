bin/mjcompile-ssa -t examples/$1 > out1
~/Desktop/prj4/bin/mjcompile-ssa -t ~/junior_year/cs352/prj4/examples/$1 > out2
diff out1 out2 > out3
cat out3

if [ -s out3 ]
then
	echo "Test failed."
else
	echo "Test passed."
fi

rm out1 out2 out3


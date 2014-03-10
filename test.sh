#!/bin/bash
FILES=examples/*.java

for f in $FILES
do
  echo "Processing $f file..."
  # take action on each file. $f store current file name
  ~/Downloads/project/bin/mjparse-ast $f > out.txt
  bin/mjparse-ast $f > out1.txt

  diff out.txt out1.txt
  echo -e "\n";
done

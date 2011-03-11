#!/bin/sh
#/*******************************************************************************
# * Copyright (c) 2008 Eclipse Foundation.
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the Eclipse Public License v1.0
# * which accompanies this distribution, and is available at
# * http://www.eclipse.org/legal/epl-v10.html
# *
# * Contributors:
# *    Eclipse Foundation - Initial API and implementation
#*******************************************************************************/

# This file converts various file formats (such as SHIFT_JIS, EUC_KR, GB2312, CP1252)
# to UTF-8 for importing with import_translation_zip.php

# This script is not a complete, turnkey solution.  You need to examine the
# files, detemine which encodings are used and convert them accordingly.

echo "Doing German... "
for i in $(find de/ -type f); do 

	NONISO=$(file $i | grep -c "Non-ISO extended-ASCII");
	if [ $NONISO -eq 1 ]; then
		echo "Need to convert $i"
		iconv -f CP1252 -t UTF-8 $i -o ${i}2
		mv -f ${i}2 $i
	fi
done
echo "Doing Korean... "
for i in $(find ko/ -type f); do 

	ISO8859=$(file $i | grep -c "ISO-8859")
	if [ $ISO8859 -eq 1 ]; then
		echo "Need to convert $i"
		iconv -f EUC-KR -t UTF-8 $i -o ${i}2
		mv -f ${i}2 $i
	fi
done

echo "Doing Japanese... "
for i in $(find ja/ -type f); do 
	# Non-ISO extended-ASCII text, with CRLF, NEL line terminators
	NONISO=$(file $i | grep -c "Non-ISO extended-ASCII");
	if [ $NONISO -eq 1 ]; then
                echo "Need to convert $i"
                iconv -f SHIFT-JIS -t UTF8  $i -o ${i}2
                mv -f ${i}2 $i
        fi
done    

echo "Doing Chinese... "
for i in $(find zh/ -type f); do 

	ISO8859=$(file $i | grep -c "ISO-8859")
	if [ $ISO8859 -eq 1 ]; then
		echo "Need to convert $i"
		iconv -f GB2312 -t UTF-8 $i -o ${i}2
		mv -f ${i}2 $i
	fi
done

echo "Doing all others..."
for i in $(find . -type f); do 

	ISO8859=$(file $i | grep -c "ISO-8859")
	if [ $ISO8859 -eq 1 ]; then
		echo "Need to convert $i"
		iconv -f ISO-8859-1 -t UTF-8 $i -o ${i}2
		mv -f ${i}2 $i
	fi
done
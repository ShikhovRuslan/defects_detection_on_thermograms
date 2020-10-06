#!/bin/bash
# Извлекает параметры термограмм из папки THERMOGRAMS_DIR: Relative Altitude, Gimbal Yaw Degree,
# GPS Latitude, GPS Longitude - из данных EXIF в файл ./OUTPUT_SUBDIR/thermograms_info.txt в виде массива JSON.

THERMOGRAMS_DIR=$(grep "^THERMOGRAMS_DIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')

OUTPUT_SUBDIR=$(grep "^OUTPUT_SUBDIR" config.txt | cut -d'=' -f 2 | sed 's/^ //')


OUTPUT_FILE=$OUTPUT_SUBDIR/thermograms_info.txt
TAGS="-XMP-drone-dji:RelativeAltitude -XMP-drone-dji:GimbalYawDegree -TAG -b -GPSLatitude -GPSLongitude"

exiftool -j -n $TAGS -r $THERMOGRAMS_DIR -w+! %0f$OUTPUT_FILE

sed "s/[][]//g" $OUTPUT_FILE -i
sed "s/}/},/g" $OUTPUT_FILE -i
sed '$s/,//' $OUTPUT_FILE -i
sed "s/^/  /g" $OUTPUT_FILE -i
sed "1s/  {/[\n  {/" $OUTPUT_FILE -i
sed '$s/  }/  }\n]/' $OUTPUT_FILE -i

rm -f ./sed*.

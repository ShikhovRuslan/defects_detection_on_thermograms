#!/bin/bash
# Извлекает параметры термограмм из папки DIR_THERMOGRAMS: Relative Altitude, Gimbal Yaw Degree,
# GPS Latitude, GPS Longitude - из данных EXIF в файл ./SUBDIR_OUTPUT/thermograms_info.txt в виде массива JSON.

DIR_THERMOGRAMS=$(grep "^DIR_THERMOGRAMS" config.txt | cut -d'=' -f 2 | sed 's/^ //')

SUBDIR_OUTPUT=$(grep "^SUBDIR_OUTPUT" config.txt | cut -d'=' -f 2 | sed 's/^ //')


OUTPUT_FILE=$SUBDIR_OUTPUT/thermograms_info.txt
TAGS="-XMP-drone-dji:RelativeAltitude -XMP-drone-dji:GimbalYawDegree -TAG -b -GPSLatitude -GPSLongitude"

exiftool -j -n -fileOrder DateTimeOriginal $TAGS -r "$DIR_THERMOGRAMS" -w+! %0f"$OUTPUT_FILE"

sed "s/[][]//g" "$OUTPUT_FILE" -i
sed "s/}/},/g" "$OUTPUT_FILE" -i
sed '$s/,//' "$OUTPUT_FILE" -i
sed "s/^/  /g" "$OUTPUT_FILE" -i
sed "1s/  {/[\n  {/" "$OUTPUT_FILE" -i
sed '$s/  }/  }\n]/' "$OUTPUT_FILE" -i

rm -f ./sed*.
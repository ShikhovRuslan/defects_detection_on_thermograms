@rem Извлекает параметры термограмм из папки DIR_THERMOGRAMS: Relative Altitude, Gimbal Yaw Degree, 
@rem GPS Latitude, GPS Longitude - из данных EXIF в файл .\SUBDIR_OUTPUT\thermograms_info.txt в виде массива JSON.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^EXIFTOOL" config.txt') do set EXIFTOOL=%%i
set EXIFTOOL=%EXIFTOOL:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^SED" config.txt') do set SED=%%i
set SED="%SED:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^DIR_THERMOGRAMS" config.txt') do set DIR_THERMOGRAMS=%%i
set DIR_THERMOGRAMS="%DIR_THERMOGRAMS:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^SUBDIR_OUTPUT" config.txt') do set SUBDIR_OUTPUT=%%i
set SUBDIR_OUTPUT=%SUBDIR_OUTPUT:~1%


set OUTPUT_FILE=".\%SUBDIR_OUTPUT%\thermograms_info.txt"
set TAGS=-XMP-drone-dji:RelativeAltitude -XMP-drone-dji:GimbalYawDegree -TAG -b -GPSLatitude -GPSLongitude

%EXIFTOOL% -j -n %TAGS% -r %DIR_THERMOGRAMS% -w+! %%0f%OUTPUT_FILE%

%SED% "s/[][]//g" %OUTPUT_FILE% -i
%SED% "s/}/},/g" %OUTPUT_FILE% -i
%SED% "$s/,//" %OUTPUT_FILE% -i
%SED% "s/^/  /g" %OUTPUT_FILE% -i
%SED% "1s/  {/[\n  {/" %OUTPUT_FILE% -i
%SED% "$s/  }/  }\n]/" %OUTPUT_FILE% -i

del .\sed*.

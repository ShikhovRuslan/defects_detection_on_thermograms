@rem Извлекает параметры термограмм из папки THERMOGRAMS_DIR: Relative Altitude, Gimbal Yaw Degree, 
@rem GPS Latitude, GPS Longitude - из данных EXIF в файл .\OUTPUT_SUBDIR\thermograms_info.txt в виде массива JSON.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^EXIFTOOL" config.txt') do set EXIFTOOL=%%i
set EXIFTOOL=%EXIFTOOL:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^SED" config.txt') do set SED=%%i
set SED="%SED:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^THERMOGRAMS_DIR" config.txt') do set THERMOGRAMS_DIR=%%i
set THERMOGRAMS_DIR="%THERMOGRAMS_DIR:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^OUTPUT_SUBDIR" config.txt') do set OUTPUT_SUBDIR=%%i
set OUTPUT_SUBDIR=%OUTPUT_SUBDIR:~1%


set OUTPUT_FILE=".\%OUTPUT_SUBDIR%\thermograms_info.txt"
set TAGS=-XMP-drone-dji:RelativeAltitude -XMP-drone-dji:GimbalYawDegree -TAG -b -GPSLatitude -GPSLongitude

%EXIFTOOL% -j -n %TAGS% -r %THERMOGRAMS_DIR% -w+! %%0f%OUTPUT_FILE%

%SED% "s/[][]//g" %OUTPUT_FILE% -i
%SED% "s/}/},/g" %OUTPUT_FILE% -i
%SED% "$s/,//" %OUTPUT_FILE% -i
%SED% "s/^/  /g" %OUTPUT_FILE% -i
%SED% "1s/  {/[\n  {/" %OUTPUT_FILE% -i
%SED% "$s/  }/  }\n]/" %OUTPUT_FILE% -i

del .\sed*.
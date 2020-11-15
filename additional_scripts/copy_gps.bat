@rem Копирует EXIF-параметры термограмм из папки DIR_THERMOGRAMS: GPS Latitude, GPS Longitude - в данные
@rem EXIF картинки с дефектами .\SUBDIR_DEFECTS\<thermogram_name>POSTFIX_DEFECTS.jpg.

@echo off
SETLOCAL EnableDelayedExpansion

for /f "tokens=2 delims==" %%i in ('findstr "^EXIFTOOL" config.txt') do set EXIFTOOL=%%i
set EXIFTOOL=%EXIFTOOL:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^SED" config.txt') do set SED=%%i
set SED="%SED:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^DIR_THERMOGRAMS" config.txt') do set DIR_THERMOGRAMS=%%i
set DIR_THERMOGRAMS="%DIR_THERMOGRAMS:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^SUBDIR_DEFECTS" config.txt') do set SUBDIR_DEFECTS=%%i
set SUBDIR_DEFECTS=%SUBDIR_DEFECTS:~1%

for /f "tokens=2 delims==" %%i in ('findstr "^POSTFIX_DEFECTS" config.txt') do set POSTFIX_DEFECTS=%%i
set POSTFIX_DEFECTS=%POSTFIX_DEFECTS:~1%


set TAGS=-exif:GPSLatitude -exif:GPSLongitude

for %%i in (%DIR_THERMOGRAMS%\*) do (
  for %%a in (%%i) do set THERMOGRAM_NAME=%%~na
  %EXIFTOOL% -tagsFromFile "%%i" %TAGS% ".\%SUBDIR_DEFECTS%\!THERMOGRAM_NAME!%POSTFIX_DEFECTS%.jpg"
)

del ".\%SUBDIR_DEFECTS%\*_original"
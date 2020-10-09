@rem Извлекает из всех термограмм из папки THERMOGRAMS_DIR необработанные температурные данные в файлы в папке .\RAW_SUBDIR.

@echo off

for /f "tokens=2 delims==" %%i in ('findstr "^THERMOGRAMS_DIR" config.txt') do set THERMOGRAMS_DIR=%%i
set THERMOGRAMS_DIR="%THERMOGRAMS_DIR:~1%"

for /f "tokens=2 delims==" %%i in ('findstr "^RAW_SUBDIR" config.txt') do set RAW_SUBDIR=%%i
set RAW_SUBDIR=%RAW_SUBDIR:~1%


for %%i in (%THERMOGRAMS_DIR%\*) do raw.bat "%%i" ".\%RAW_SUBDIR%"
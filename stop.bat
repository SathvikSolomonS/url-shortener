@echo off
cd /d C:\Projects\url-shortener
echo Stopping backend...
docker compose down
echo.
echo Backend stopped. Close the frontend terminal window manually.
pause
@echo off
echo Starting backend (Docker)...
cd /d C:\Projects\url-shortener
docker compose up --build -d

echo.
echo Starting frontend...
start cmd /k "cd /d C:\Projects\url-shortener-frontend && npm run dev"

echo.
echo Waiting for things to boot up...
timeout /t 8 >nul

echo.
echo Opening the app in your browser...
start http://localhost:5173

echo.
echo Done. Backend running in Docker, frontend in the other window.
pause
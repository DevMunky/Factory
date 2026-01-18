:: echo off
set downloader_path=%1
set server_path=%2

cd %downloader_path%
.\hytale-downloader-windows-amd64.exe -download-path %server_path%\hytale_server.zip

cd %server_path%
unzip .\hytale_server.zip -d .\
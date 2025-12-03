@echo off
SET PGPASSWORD=sa123

:: Kiểm tra đường dẫn psql.exe xem có đúng ổ C không
"C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d webchat -c "CALL partman.run_maintenance_proc()"
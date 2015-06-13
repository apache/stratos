import os

stratos_prompt = "stratos> "

stratos_dir = "~/.stratos"
log_file_name = "stratos-cli.log"

stratos_dir_path = os.path.expanduser(stratos_dir)
log_file_path = stratos_dir_path+log_file_name

Stratos_url = os.getenv('STRATOS_URL', None)
Stratos_username = os.getenv('STRATOS_USERNAME', None)
Stratos_password = os.getenv('STRATOS_PASSWORD', None)
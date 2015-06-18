import os

stratos_prompt = "stratos> "

stratos_dir = "~/.stratos"
log_file_name = "stratos-cli.log"

stratos_dir_path = os.path.expanduser(stratos_dir)
log_file_path = os.path.join(stratos_dir_path, log_file_name)

stratos_url = os.getenv('STRATOS_URL', "https://localhost:9443/")
stratos_username = os.getenv('STRATOS_USERNAME', "")
stratos_password = os.getenv('STRATOS_PASSWORD', "")
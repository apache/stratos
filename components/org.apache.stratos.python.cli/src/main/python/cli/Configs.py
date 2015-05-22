import os

stratos_prompt = "stratos> "

stratos_dir = "~/.stratos"
log_file_name = "stratos-cli.log"

Stratos_url = os.getenv('STRATOS_URL', None)
Stratos_username = os.getenv('STRATOS_USERNAME', None)
Stratos_password = os.getenv('STRATOS_PASSWORD', None)
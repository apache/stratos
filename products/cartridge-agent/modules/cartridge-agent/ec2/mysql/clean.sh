echo "Removing json files created"
rm -f *.json
echo "Removing payload directory"
rm -rf payload/
echo "Removing repo info soap request"
rm -f repoinforequest.xml
echo "Removing launch.params"
rm -f launch.params 
echo "Removing cartridge agent logs"
rm -f /var/log/apache-stratos/*


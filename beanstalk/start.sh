#/bin/sh
 
# Script must be executable!
cd /tmp
wget -N http://dl.bintray.com/snowplow/snowplow-generic/schema_guru_webui_0.2.0.zip
unzip -o schema_guru_webui_0.2.0.zip
java -jar schema-guru-webui-0.2.0

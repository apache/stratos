FROM chamilad/base-image:4.1.0-beta
MAINTAINER dev@stratos.apache.org

# ----------------------
# Install prerequisites
# ----------------------
# RUN apt-get update && \ apt-get install -yq --no-install-recommends openjdk-7-jre wget ca-certificates && \apt-get clean && \rm -rf /var/lib/apt/lists/*
WORKDIR /opt
# RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u67-b01/jdk-7u67-linux-x64.tar.gz
ADD packs/jdk-7u67-linux-x64.tar.gz /mnt/jdk-7u67-linux-x64.tar.gz
# RUN tar zxvf /mnt/jdk-7u67-linux-x64.tar.gz
RUN mv /mnt/jdk-7u67-linux-x64.tar.gz/jdk1.7.0_67 /opt/jdk1.7.0_67
ENV JAVA_HOME /opt/jdk1.7.0_67


ENV CATALINA_HOME /opt/tomcat

# ----------------------
# Install Tomcat
# ----------------------
ADD packs/apache-tomcat-7.0.59.tar.gz /opt/apache-tomcat-7.0.59.tar.gz
RUN mv /opt/apache-tomcat-7.0.59.tar.gz/apache-tomcat-7.0.59 /opt/tomcat

# -----------------------
# Add shell scripts
# -----------------------
ADD files/create-admin-user.sh /opt/create-admin-user.sh
# ADD files/run.sh /opt/run.sh
RUN chmod +x /opt/create-admin-user.sh && \
    bash /opt/create-admin-user.sh

ADD files/env /tmp/env
RUN cat /tmp/env >> /etc/environment

#------------------------
# Copy Tomcat related PCA plugins
#-----------------------
ADD packs/plugins /mnt/plugins


EXPOSE 8080

# -----------------------
# Define entry point
# -----------------------
ENTRYPOINT /usr/local/bin/run | /usr/sbin/sshd -D

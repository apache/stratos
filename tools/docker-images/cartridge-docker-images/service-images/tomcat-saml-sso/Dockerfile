FROM chamilad/base-image:4.1.0-beta
MAINTAINER dev@stratos.apache.org

ENV JDK_VERSION 1.7.0_60
ENV JDK_TAR_FILENAME jdk-7u60-linux-x64.tar.gz
ENV TOMCAT_VERSION 7.0.55

# ----------------------
# Install prerequisites
# ----------------------
# RUN apt-get update && \ apt-get install -yq --no-install-recommends openjdk-7-jre wget ca-certificates && \apt-get clean && \rm -rf /var/lib/apt/lists/*
WORKDIR /opt
# RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u67-b01/jdk-7u60-linux-x64.tar.gz
ADD packs/${JDK_TAR_FILENAME} /mnt/${JDK_TAR_FILENAME}
# RUN tar zxvf /mnt/${JDK_TAR_FILENAME}
RUN mv /mnt/${JDK_TAR_FILENAME}/jdk${JDK_VERSION} /opt/jdk${JDK_VERSION}
ENV JAVA_HOME /opt/jdk${JDK_VERSION}


ENV CATALINA_HOME /opt/tomcat

# ----------------------
# Install Tomcat
# ----------------------
ADD packs/apache-tomcat-${TOMCAT_VERSION}.tar.gz /opt/apache-tomcat-${TOMCAT_VERSION}.tar.gz
RUN mv /opt/apache-tomcat-${TOMCAT_VERSION}.tar.gz/apache-tomcat-${TOMCAT_VERSION} /opt/tomcat

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

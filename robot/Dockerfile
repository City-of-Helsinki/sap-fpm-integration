FROM registry.access.redhat.com/ubi8/python-311 AS build

USER 0
ADD robot/src robot-sources
RUN chown -R 1001:0 ./
RUN ssh-keygen -N "" -t rsa -b 4096 -f /tmp/robot_id_rsa
# robot user
RUN chown 1001760000:0 /tmp/robot_id_rsa
USER 1001

RUN pip install robotframework
RUN pip install pyftpdlib
RUN pip install paramiko

EXPOSE 2121 2122 40000 40001 40002

#ENTRYPOINT robot --xunit xunitoutput.xml --log /tmp/robot-results/robot.log --outputdir /tmp/robot-results -i s4 . && sleep infinity
ENTRYPOINT sleep infinity
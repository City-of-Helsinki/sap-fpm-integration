FROM registry.access.redhat.com/ubi8/python-311 AS build

USER 0
ADD robot/src robot-sources
RUN chown -R 1001:0 ./
USER 1001

RUN pip install robotframework
RUN pip install pyftpdlib

EXPOSE 2121 40000 40001 40002

ENTRYPOINT robot . && until [ -e /tmp/robot_files_fetched.txt ]; do sleep 30; done
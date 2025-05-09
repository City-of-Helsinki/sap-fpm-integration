*** Settings ***
Library      ../resources/FTPServer.py

Suite Setup     Setup Ftp Server
Suite Teardown  Teardown Ftp Server

*** Keywords ***

Setup Ftp Server
    Init Ftp Server     robot/sap_ftp
    Add Ftp User    kasko_user  kasko_pass  kasko

Teardown Ftp Server
    Close Ftp Server

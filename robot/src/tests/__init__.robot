*** Settings ***
Library      ../resources/FTPServer.py

Suite Setup     Setup Ftp Server
Suite Teardown  Teardown Ftp Server

*** Keywords ***

Setup Ftp Server
    Init Ftp Server     %{SAP_FPM_INTEGRATION_ROBOT_SERVICE_HOST}       robot/sap_ftp
    ${perustiedotDirs}		Create List		203 	204     210
    ${toteumatDirs}      Create List
    Add Ftp User    %{KASKO_SFTP_USER_ID023}    %{KASKO_SFTP_PASSWORD_ID023}    kasko/toteumat      ${toteumatDirs}
    Add Ftp User    %{KASKO_SFTP_USER_ID137}    %{KASKO_SFTP_PASSWORD_ID137}    kasko/perustiedot   ${perustiedotDirs}
    Add Ftp User    %{SOTEPE_SFTP_USER_ID022}   %{SOTEPE_SFTP_PASSWORD_ID022}   sotepe/toteumat     ${toteumatDirs}
    Add Ftp User    %{SOTEPE_SFTP_USER_ID167}   %{SOTEPE_SFTP_PASSWORD_ID167}   sotepe/perustiedot  ${perustiedotDirs}
    Add Ftp User    %{PALKE_SFTP_USER_ID025}    %{PALKE_SFTP_PASSWORD_ID025}    palke/toteumat      ${toteumatDirs}
    Add Ftp User    %{PALKE_SFTP_USER_ID166}    %{PALKE_SFTP_PASSWORD_ID166}    palke/cototeumat    ${toteumatDirs}
    Add Ftp User    %{PALKE_SFTP_USER_ID138}    %{PALKE_SFTP_PASSWORD_ID138}    palke/perustiedot   ${perustiedotDirs}


Teardown Ftp Server
    Close Ftp Server

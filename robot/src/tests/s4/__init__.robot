*** Settings ***
Library      ../../resources/S4SFTPServer.py
Library      OperatingSystem

Suite Setup     Setup S4SFtp Server
Suite Teardown  Close Sftp Server

*** Keywords ***
Setup S4SFtp Server
    Set Log Level   DEBUG
    Remove Directory    ${CURDIR}/robot/sap_s4_sftp    recursive=True
    Init Sftp Server        robot/sap_s4_sftp
    ${perustiedotDirs}		Create List		203 	204     210     213
    ${toteumatDirs}      Create List
    Add Sftp User    %{KASKO_S4_SFTP_USER_TOTEUMAT}    %{KASKO_S4_SFTP_PASSWORD_TOTEUMAT}    kasko/toteumat      ${toteumatDirs}
    Add Sftp User    %{KASKO_S4_SFTP_USER_PERUSTIEDOT}    %{KASKO_S4_SFTP_PASSWORD_PERUSTIEDOT}    kasko/perustiedot   ${perustiedotDirs}
    Add Sftp User    %{SOTEPE_S4_SFTP_USER_TOTEUMAT}   %{SOTEPE_S4_SFTP_PASSWORD_TOTEUMAT}   sotepe/toteumat     ${toteumatDirs}
    Add Sftp User    %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}   %{SOTEPE_S4_SFTP_PASSWORD_PERUSTIEDOT}   sotepe/perustiedot  ${perustiedotDirs}
    Add Sftp User    %{PALKE_S4_SFTP_USER_TOTEUMAT}    %{PALKE_S4_SFTP_PASSWORD_TOTEUMAT}    palke/s4_toteumat      ${toteumatDirs}
    Add Sftp User    %{PALKE_S4_SFTP_USER_PERUSTIEDOT}    %{PALKE_S4_SFTP_PASSWORD_PERUSTIEDOT}    palke/perustiedot   ${perustiedotDirs}
    Start Sftp Server

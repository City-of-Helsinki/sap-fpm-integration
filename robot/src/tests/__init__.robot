*** Settings ***
Library      ../resources/FTPServer.py
Library      ../resources/S4SFTPServer.py
Library      OperatingSystem

Suite Setup     Setup Servers
Suite Teardown  Teardown Servers

*** Keywords ***
Setup Servers
    Setup Ftp Server
    #Setup S4SFtp Server


Setup Ftp Server
    Set Log Level   DEBUG
    Remove Directory    ${CURDIR}/robot/sap_ftp    recursive=True
    Init Ftp Server     %{SAP_FPM_INTEGRATION_ROBOT_SERVICE_HOST}       robot/sap_ftp
    ${perustiedotDirs}		Create List		203 	204     210
    ${toteumatDirs}      Create List
    Add Ftp User    %{KASKO_SFTP_USER_ID023}    %{KASKO_SFTP_PASSWORD_ID023}    kasko/toteumat      ${toteumatDirs}
    Add Ftp User    %{KASKO_SFTP_USER_ID137}    %{KASKO_SFTP_PASSWORD_ID137}    kasko/perustiedot   ${perustiedotDirs}
    Add Ftp User    %{SOTEPE_SFTP_USER_ID022}   %{SOTEPE_SFTP_PASSWORD_ID022}   sotepe/toteumat     ${toteumatDirs}
    Add Ftp User    %{SOTEPE_SFTP_USER_ID167}   %{SOTEPE_SFTP_PASSWORD_ID167}   sotepe/perustiedot  ${perustiedotDirs}
    Add Ftp User    %{PALKE_SFTP_USER_ID025}    %{PALKE_SFTP_PASSWORD_ID025}    palke/toteumat      ${toteumatDirs}
    # cototeumat result files get uploaded to palke/toteumat to mimic how azure upload works
    Add Ftp User    %{PALKE_SFTP_USER_ID166}    %{PALKE_SFTP_PASSWORD_ID166}    palke/cototeumat    ${toteumatDirs}
    Add Ftp User    %{PALKE_SFTP_USER_ID138}    %{PALKE_SFTP_PASSWORD_ID138}    palke/perustiedot   ${perustiedotDirs}
    Start Ftp Server


Setup S4SFtp Server
    Set Log Level   DEBUG
    Remove Directory    ${CURDIR}/robot/sap_s4_sftp    recursive=True
    Init Sftp Server        robot/sap_s4_sftp
    ${perustiedotDirs}		Create List		203 	204     210
    ${toteumatDirs}      Create List
    Add Sftp User    %{KASKO_S4_SFTP_USER_TOTEUMAT}    %{KASKO_S4_SFTP_PASSWORD_TOTEUMAT}    kasko/toteumat      ${toteumatDirs}
    Add Sftp User    %{KASKO_S4_SFTP_USER_PERUSTIEDOT}    %{KASKO_S4_SFTP_PASSWORD_PERUSTIEDOT}    kasko/perustiedot   ${perustiedotDirs}
    Add Sftp User    %{SOTEPE_S4_SFTP_USER_TOTEUMAT}   %{SOTEPE_S4_SFTP_PASSWORD_TOTEUMAT}   sotepe/toteumat     ${toteumatDirs}
    Add Sftp User    %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}   %{SOTEPE_S4_SFTP_PASSWORD_PERUSTIEDOT}   sotepe/perustiedot  ${perustiedotDirs}
    Add Sftp User    %{PALKE_S4_SFTP_USER_TOTEUMAT}    %{PALKE_S4_SFTP_PASSWORD_TOTEUMAT}    palke/toteumat      ${toteumatDirs}
    # cototeumat result files get uploaded to palke/toteumat to mimic how azure upload works
    Add Sftp User    %{PALKE_S4_SFTP_USER_TOTEUMAT}    %{PALKE_S4_SFTP_PASSWORD_TOTEUMAT}    palke/cototeumat    ${toteumatDirs}
    Add Sftp User    %{PALKE_S4_SFTP_USER_PERUSTIEDOT}    %{PALKE_S4_SFTP_PASSWORD_PERUSTIEDOT}    palke/perustiedot   ${perustiedotDirs}
    Start Sftp Server

Teardown Servers
    Close Ftp Server
    #Close Sftp Server
*** Settings ***
Documentation   Luo SFTP-palvelimen, luo perustietoja palvelimelle, tarkistaa SFTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/S4SFTPServer.py
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapkumppani     s4

*** Test Cases ***
# sotepe and kasko will move directly to S4
Lähetä S4 palke perustiedot SAPKUMPPANI
    ${RCOMP}    Generate Random String  20
    ${NAME1}    Generate Random String  20
    Set Current SFTP USER   %{PALKE_S4_SFTP_USER_PERUSTIEDOT}
    ${FtpDir}     Get SFtp Dir For     %{PALKE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä palke perustiedot SAPKUMPPANI   ${FtpDir}   ${RCOMP}    ${NAME1}    FTPDownKeyword=Set SFtp Connection As Down     FTPUpKeyword=Set SFtp Connection As Up


Lähetä S4 kasko perustiedot SAPKUMPPANI
    ${RCOMP}    Generate Random String  20
    ${NAME1}    Generate Random String  20
    Set Current SFTP USER   %{KASKO_S4_SFTP_USER_PERUSTIEDOT}
    ${FtpDir}     Get SFtp Dir For     %{KASKO_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä kasko perustiedot SAPKUMPPANI      ${FtpDir}     ${RCOMP}    ${NAME1}     FTPDownKeyword=Set SFtp Connection As Down    FTPUpKeyword=Set SFtp Connection As Up

Lähetä S4 sotepe perustiedot SAPKUMPPANI
    ${RCOMP}    Generate Random String  20
    ${NAME1}    Generate Random String  20
    Set Current SFTP USER   %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}
    ${FtpDir}     Get SFtp Dir For     %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä sotepe perustiedot SAPKUMPPANI      ${FtpDir}     ${RCOMP}    ${NAME1}     FTPDownKeyword=Set SFtp Connection As Down    FTPUpKeyword=Set SFtp Connection As Up

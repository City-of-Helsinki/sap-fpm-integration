*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../../resources/FTPServer.py
Resource    ../../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapkumppani

*** Test Cases ***
Lähetä palke perustiedot SAPKUMPPANI
    Skip    ECC Perustiedot no longer needed or supported
    ${RCOMP}    Generate Random String  20
    ${NAME1}    Generate Random String  20
    ${FtpDir}     Get Ftp Dir For     %{PALKE_SFTP_USER_ID138}
    Lähetä sotepe perustiedot SAPKUMPPANI   ${FtpDir}   ${RCOMP}    ${NAME1}    FTPDownKeyword=Set Ftp Connection As Down     FTPUpKeyword=Set Ftp Connection As Up


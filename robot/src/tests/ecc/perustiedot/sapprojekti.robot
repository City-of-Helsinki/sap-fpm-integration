*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../../resources/FTPServer.py
Resource    ../../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapprojekti

*** Test Cases ***
Lähetä palke perustiedot SAPPROJEKTI
    ${PerustiedotFtpDir}   Get FTP Dir For      %{PALKE_SFTP_USER_ID138}
    Lähetä palke perustiedot SAPPROJEKTI   ${PerustiedotFtpDir}     PBUKR=9500    PSPNR=9501    FTPDownKeyword=Set Ftp Connection As Down     FTPUpKeyword=Set Ftp Connection As Up


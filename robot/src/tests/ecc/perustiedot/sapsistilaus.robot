*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../../resources/FTPServer.py
Resource    ../../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapsistilaus   ecc

*** Test Cases ***

Lähetä palke perustiedot sapsistilaus
    ${PerustiedotFtpDir}   Get FTP Dir For     %{PALKE_SFTP_USER_ID138}
    Lähetä palke perustiedot sapsistilaus      ${PerustiedotFtpDir}     BUKRS=9500    AUART=9501     FTPDownKeyword=Set Ftp Connection As Down    FTPUpKeyword=Set Ftp Connection As Up




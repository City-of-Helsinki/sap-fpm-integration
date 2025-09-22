*** Settings ***
Documentation   Luo SFTP-palvelimen, luo perustietoja palvelimelle, tarkistaa SFTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/S4SFTPServer.py
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     saptoimintoalue   s4

*** Test Cases ***

Lähetä S4 palke perustiedot saptoimintoalue
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{PALKE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä palke perustiedot SAPTOIMINTOALUE      ${PerustiedotFtpDir}     Toiminto-alue=950001    Kuvaus=Palke    FTPDownKeyword=Set SFtp Connection As Down    FTPUpKeyword=Set SFtp Connection As Up

Lähetä S4 kasko perustiedot saptoimintoalue
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{KASKO_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä kasko perustiedot SAPTOIMINTOALUE      ${PerustiedotFtpDir}    Toiminto-alue=1400001   Kuvaus=Kasko     FTPDownKeyword=Set SFtp Connection As Down    FTPUpKeyword=Set SFtp Connection As Up

Lähetä S4 sotepe perustiedot saptoimintoalue
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä sotepe perustiedot SAPTOIMINTOALUE      ${PerustiedotFtpDir}    Toiminto-alue=3900001  Kuvaus=Sotepe   FTPDownKeyword=Set SFtp Connection As Down    FTPUpKeyword=Set SFtp Connection As Up

*** Settings ***
Documentation   Luo SFTP-palvelimen, luo perustietoja palvelimelle, tarkistaa SFTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/S4SFTPServer.py
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapprojekti     s4

*** Test Cases ***
Lähetä S4 palke perustiedot SAPPROJEKTI
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{PALKE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä palke perustiedot SAPPROJEKTI   ${PerustiedotFtpDir}     PBUKR=9500    PSPNR=9501    FTPDownKeyword=Set SFtp Connection As Down     FTPUpKeyword=Set SFtp Connection As Up

Lähetä S4 kasko perustiedot sapsistilaus
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{KASKO_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä kasko perustiedot SAPPROJEKTI      ${PerustiedotFtpDir}     BUKRS=1400    AUART=1401     FTPDownKeyword=Set SFtp Connection As Up    FTPUpKeyword=Set SFtp Connection As Down

Lähetä S4 sotepe perustiedot sapsistilaus
    ${PerustiedotFtpDir}   Get SFTP Dir For     %{SOTEPE_S4_SFTP_USER_PERUSTIEDOT}
    Lähetä sotepe perustiedot SAPPROJEKTI      ${PerustiedotFtpDir}     BUKRS=3900    AUART=3901     FTPDownKeyword=Set SFtp Connection As Up    FTPUpKeyword=Set SFtp Connection As Down

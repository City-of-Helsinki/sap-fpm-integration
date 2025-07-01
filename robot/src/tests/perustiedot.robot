*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../resources/CreateTestFiles.resource


Test Tags       perustiedot

*** Test Cases ***
Lähetä kasko perustiedot
    [Documentation]     KASKO perustiedot
    [Tags]   kasko      perustiedot
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}
    ${ORD}      Create Kasko ORD
    Create File     ${KaskoPerustiedotFtpDir}/203/ORD_OUT_1.xml   content=${ORD}
    Wait Until Keyword Succeeds      3 minutes   30 seconds      Check Kasko ${KaskoPerustiedotFtpDir}/SAPSISTILAUS.csv

*** Keywords ***

Check Kasko ${KaskoSisTilausFilePath}
    [Documentation]     Opens file sent to the FTP and checks it contains the correct lines
    ${KaskoSisTilaus}    Get File       ${KaskoSisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${KaskoSisTilaus}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Contain   ${KaskoSisTilaus}   1400;3901;3963900001;ktextiä;sttextiä
    Should Contain   ${KaskoSisTilaus}   1400;3901;3963900002;ktextiä;sttextiä

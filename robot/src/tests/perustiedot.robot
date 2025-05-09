*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem


Test Tags       perustiedot

*** Test Cases ***
Lähetä kasko perustiedot
    [Documentation]     KASKO perustiedot
    [Tags]   kasko      perustiedot
    ${FtpDir}   Get FTP Dir For     kasko_user
    Create File     ${FtpDir}/test.txt   content=test
    Wait Until Keyword Succeeds      5 minutes   1 minute      Nope

*** Keywords ***
Nope
    Should Be True    False

*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapkumppani

*** Test Cases ***
Lähetä kasko perustiedot SAPKUMPPANI
    [Documentation]     KASKO perustiedot   sapkumppani
    [Tags]   kasko      perustiedot     sapkumppani     part_out
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}
    ${RCOMP}    Set Variable    R COMP HERE
    ${RNAME1}    Set Variable   R NAME 1 HERE " OK
    ${PART}      Create PART   ${RCOMP}    ${RNAME1}
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_1.xml   content=${PART}
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_2.xml   content=${PART}
    Wait Until Keyword Succeeds      3 minutes   15 seconds      Check SAPKUMPPANI ${KaskoPerustiedotFtpDir}/SAPKUMPPANI.csv
        ...     ${RCOMP}    ${RNAME1}

*** Keywords ***

Check SAPKUMPPANI ${FilePath}
    [Documentation]     Opens SAPKUMPPANI.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${RCOMP}    ${RNAME1}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     RCOMP;RNAME1
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${Lines}    Get Lines Containing String    ${File}   ${RCOMP};${RNAME1}
    Length Should Be   ${Lines}     20


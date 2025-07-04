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
    ${NAME1}    Set Variable   R NAME 1 HERE " OK
    ${PART}      Create PART   ${RCOMP}    ${NAME1}
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_1.xml   content=${PART}
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_2.xml   content=${PART}
    ${CSV_ESCAPED_NAME1}   Set Variable     "R NAME 1 HERE "" OK"
    Wait Until Keyword Succeeds      3 minutes   15 seconds      Check SAPKUMPPANI ${KaskoPerustiedotFtpDir}/SAPKUMPPANI.csv
        ...     ${RCOMP}    ${CSV_ESCAPED_NAME1}

*** Keywords ***

Check SAPKUMPPANI ${FilePath}
    [Documentation]     Opens SAPKUMPPANI.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${RCOMP}    ${NAME1}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     RCOMP;NAME1
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${Lines}    Get Lines Containing String    ${File}   ${RCOMP};${NAME1}
    Length Should Be   ${Lines}     20


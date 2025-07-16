*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource
Resource    ../../resources/CreateTestFilesCommon.resource


Test Tags       perustiedot     sapkumppani

*** Test Cases ***
Lähetä kasko perustiedot SAPKUMPPANI
    [Documentation]     KASKO perustiedot   sapkumppani
    [Tags]   kasko      perustiedot     sapkumppani     part_out
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}
    ${RCOMP}    Set Variable    R COMP HERE
    ${NAME1}    Set Variable   R NAME 1 HERE " OK
    ${PART}      Create PART   ${RCOMP}    ${NAME1}
    ${CUR_DATE_STR}      Get Current Time Text
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_1_${CUR_DATE_STR}.xml   content=${PART}
    ${RCOMP}    Set Variable    R COMP SECOND
    ${NAME1}    Set Variable   R NAME 2 HERE " OK
    ${PART}      Create PART   ${RCOMP}    ${NAME1}
    Create File     ${KaskoPerustiedotFtpDir}/210/PART_OUT_2_${CUR_DATE_STR}.xml   content=${PART}
    ${CSV_ESCAPED_NAME1}   Set Variable     "R NAME 2 HERE "" OK"
    Wait Until Keyword Succeeds     2 minutes   15 seconds    Should Exist       ${KaskoPerustiedotFtpDir}/SAPKUMPPANI.csv
    Wait Until Keyword Succeeds     1 minute   5 seconds    Check SAPKUMPPANI ${KaskoPerustiedotFtpDir}/SAPKUMPPANI.csv      ${RCOMP}    ${CSV_ESCAPED_NAME1}

*** Keywords ***

Check SAPKUMPPANI ${FilePath}
    [Documentation]     Opens SAPKUMPPANI.csv sent to the FTP and checks it contains the correct lines. Only the contents of the latest file should be in the CSV
    [Arguments]     ${RCOMP}    ${NAME1}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     RCOMP;NAME1
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${Lines}    Get Lines Containing String    ${File}   ${RCOMP};${NAME1}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers    ${LineCount}     10
    ${AllLinesCount}    Get Line Count  ${File}
    Should Be Equal As Numbers    ${AllLinesCount}    11


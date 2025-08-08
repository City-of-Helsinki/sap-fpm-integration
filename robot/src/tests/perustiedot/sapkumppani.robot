*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource
Resource    ../../resources/CreateTestFilesCommon.resource


Test Tags       perustiedot     sapkumppani

*** Test Cases ***
Lähetä kasko perustiedot SAPKUMPPANI
    ${RCOMP}    Set Variable    R COMP SECOND
    ${NAME1}    Set Variable    Kasko name
    Lähetä kasko perustiedot SAPKUMPPANI    %{KASKO_SFTP_USER_ID137}   ${RCOMP}    ${NAME1}

Lähetä sotepe perustiedot SAPKUMPPANI
    ${RCOMP}    Set Variable    SOTEPE COMP
    ${NAME1}    Set Variable   R NAME SOTEPE HERE
    Lähetä sotepe perustiedot SAPKUMPPANI    %{SOTEPE_SFTP_USER_ID167}   ${RCOMP}    ${NAME1}

Lähetä palke perustiedot SAPKUMPPANI
    ${RCOMP}    Set Variable    PALKE " COMP
    ${NAME1}    Set Variable   R NAME palke
    Lähetä sotepe perustiedot SAPKUMPPANI   %{PALKE_SFTP_USER_ID138}   ${RCOMP}    ${NAME1}

*** Keywords ***

Lähetä ${TOIMIALA} perustiedot SAPKUMPPANI
    [Documentation]     ${TOIMIALA} perustiedot   sapkumppani
    [Tags]   ${TOIMIALA}      perustiedot     sapkumppani     part_out
    [Arguments]   ${SFTP_USER}     ${RCOMP}     ${NAME1}
    ${PerustiedotFtpDir}   Get FTP Dir For     ${SFTP_USER}
    ${RCOMP_1}    Set Variable    R COMP HERE
    ${NAME1_1}    Set Variable   R NAME 1 HERE " OK
    ${PART_1}      Create PART   ${RCOMP_1}    ${NAME1_1}
    ${CUR_DATE_STR}      Get Current Time Text
    Create File     ${PerustiedotFtpDir}/210/PART_OUT_${CUR_DATE_STR}_1.xml   content=${PART_1}
    ${PART_2}      Create PART   ${RCOMP}    ${NAME1}
    Set Ftp Connection As Down
    Create File     ${PerustiedotFtpDir}/210/PART_OUT_2_${CUR_DATE_STR}_2.xml   content=${PART_2}
    Set Ftp Connection As Up

    Wait Until Keyword Succeeds     3 minutes   15 seconds    Should Exist       ${PerustiedotFtpDir}/SAPKUMPPANI.csv
    Wait Until Keyword Succeeds     1 minute   5 seconds    Check SAPKUMPPANI ${PerustiedotFtpDir}/SAPKUMPPANI.csv      ${RCOMP}    ${NAME1}



Check SAPKUMPPANI ${FilePath}
    [Documentation]     Opens SAPKUMPPANI.csv sent to the FTP and checks it contains the correct lines. Only the contents of the latest file should be in the CSV
    [Arguments]     ${RCOMP}    ${NAME1}
    ${File}    Get File       ${FilePath}
    ${CSV_ESCAPED_RCOMP}  Escape CSV Value      ${RCOMP}
    ${CSV_ESCAPED_NAME1}   Escape CSV Value  ${NAME1}
    ${ExpHeader}     Set Variable     RCOMP;NAME1
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${Lines}    Get Lines Containing String    ${File}   ${CSV_ESCAPED_RCOMP};${CSV_ESCAPED_NAME1}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers    ${LineCount}     10
    ${AllLinesCount}    Get Line Count  ${File}
    Should Be Equal As Numbers    ${AllLinesCount}    11


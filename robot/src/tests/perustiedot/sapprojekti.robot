*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../../resources/CreatePerustiedotTestFiles.resource


Test Tags       perustiedot     sapprojekti

*** Test Cases ***
Lähetä kasko perustiedot SAPPROJEKTI
    [Documentation]     KASKO perustiedot   sapprojekti
    [Tags]   kasko      perustiedot     sapprojekti     wbs_out
    ${KaskoPerustiedotFtpDir}   Get FTP Dir For     %{KASKO_SFTP_USER_ID137}

    ${PBUKR}    Set Variable    1400
    ${PSPNR}    Set Variable   3901
    ${POSID}   Set Variable    01
    ${POST1}   Set Variable    ktxt1
    ${STUFE}   Set Variable    st text 1
    ${ERDAT}   Set Variable    autyp1
    ${AEDAT}   Set Variable    02
    ${TXT40}   Set Variable    ktext2
    ${WBS}      Create WBS   ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    ${NonKaskoWBS}      Create WBS   9    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    Create File     ${KaskoPerustiedotFtpDir}/204/WBS_OUT_non_kasko.xml   content=${NonKaskoWBS}
    Create File     ${KaskoPerustiedotFtpDir}/204/WBS_OUT_1.xml   content=${WBS}

    Wait Until Keyword Succeeds     2 minutes   15 seconds    Should Exist       ${KaskoPerustiedotFtpDir}/SAPPROJEKTI.csv
    Check SAPPROJEKTI ${KaskoPerustiedotFtpDir}/SAPPROJEKTI.csv
        ...     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}   ${STUFE}   ${ERDAT}   ${AEDAT}   ${TXT40}

*** Keywords ***

Check SAPPROJEKTI ${FilePath}
    [Documentation]     Opens SAPPROJEKTI.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     PBUKR;PSPNR;POSID;POST1;STUFE;ERDAT;AEDAT;TXT40
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${Lines}    Get Lines Containing String    ${File}   ${PBUKR};${PSPNR};${POSID};${POST1};${STUFE};${ERDAT};${AEDAT};${TXT40}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers   ${LineCount}     10
    ${AllLinesCount}    Get Line Count  ${File}
    Should Be Equal As Numbers   ${AllLinesCount}    11


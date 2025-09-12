*** Settings ***
Documentation   Luo FTP-palvelimen, luo perustietoja palvelimelle, tarkistaa FTP-palvelimelle integraation lähettämät tiedostot


Library      ../../../resources/FTPServer.py
Library         DateTime
Library     OperatingSystem
Resource    ../../../resources/CreatePerustiedotTestFiles.resource
Resource    ../../../resources/CreateTestFilesCommon.resource


Test Tags       perustiedot     sapprojekti

*** Test Cases ***
Lähetä kasko perustiedot SAPPROJEKTI
    Lähetä kasko perustiedot SAPPROJEKTI    %{KASKO_SFTP_USER_ID137}   PBUKR=1400    PSPNR=1401

Lähetä palke perustiedot SAPPROJEKTI
    Lähetä palke perustiedot SAPPROJEKTI   %{PALKE_SFTP_USER_ID138}     PBUKR=9500    PSPNR=9501

Lähetä sotepe perustiedot SAPPROJEKTI
    Lähetä sotepe perustiedot SAPPROJEKTI   %{SOTEPE_SFTP_USER_ID167}     PBUKR=3900    PSPNR=3901

*** Keywords ***

Lähetä ${TOIMIALA} perustiedot SAPPROJEKTI
    [Documentation]     ${TOIMIALA}   perustiedot   sapprojekti
    [Arguments]     ${SFTP_USER}    ${PBUKR}    ${PSPNR}
    [Tags]   ${TOIMIALA}      perustiedot     sapprojekti     wbs_out
    ${PerustiedotFtpDir}   Get FTP Dir For     ${SFTP_USER}

    ${CUR_DATE_STR}     Get Current Time Text
    ${POSID}   Set Variable    02
    ${POST1}   Generate Random String  20
    ${STUFE}   Generate Random String  20
    ${ERDAT}   Set Variable    autyp2
    ${AEDAT}   Set Variable    03
    ${TXT40}   Set Variable    ${CUR_DATE_STR}
    ${WBS}      Create WBS   ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    ${NonToimialaWBS}      Create WBS   10    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    Set Ftp Connection As Down
    Create File     ${PerustiedotFtpDir}/204/WBS_OUT_non_palke_${CUR_DATE_STR}.xml   content=${NonToimialaWBS}
    Set Ftp Connection As Up
    Create File     ${PerustiedotFtpDir}/204/WBS_OUT_1_${CUR_DATE_STR}.xml   content=${WBS}

    Wait Until Keyword Succeeds     3 minutes   15 seconds    Should Exist       ${PerustiedotFtpDir}/SAPPROJEKTI.csv
    Check SAPPROJEKTI ${PerustiedotFtpDir}/SAPPROJEKTI.csv
        ...     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}   ${STUFE}   ${ERDAT}   ${AEDAT}   ${TXT40}

Check SAPPROJEKTI ${FilePath}
    [Documentation]     Opens SAPPROJEKTI.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}
    ${File}    Get File       ${FilePath}
    ${ExpHeader}     Set Variable     PBUKR;PSPNR;POSID;POST1;STUFE;ERDAT;AEDAT;TXT40
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    ${CSV_ESCAPED_POST1}  Escape CSV Value      ${POST1}
    ${CSV_ESCAPED_STUFE}    Escape CSV Value      ${STUFE}
    ${CSV_ESCAPED_TXT40}    Escape CSV Value      ${TXT40}
    ${Lines}    Get Lines Containing String    ${File}   ${PBUKR};${PSPNR};${POSID};${CSV_ESCAPED_POST1};${CSV_ESCAPED_STUFE};${ERDAT};${AEDAT};${CSV_ESCAPED_TXT40}
    ${LineCount}    Get Line Count  ${Lines}
    Should Be Equal As Numbers   ${LineCount}     10
    ${AllLinesCount}    Get Line Count  ${File}
    # can contain lines from previous runs
    Should Be True   ${AllLinesCount} >= 11


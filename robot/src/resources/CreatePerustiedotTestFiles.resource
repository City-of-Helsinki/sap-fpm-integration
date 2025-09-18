*** Settings ***
Library			OperatingSystem
Library         String
Resource       CreateTestFilesCommon.resource

*** Variables ***


*** Keywords ***

Create ORD
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}  ${AUTYP1}  ${AUFNR2}   ${KTEXT2}  ${STTXT2}   ${AUTYP2}
    ${ORD}     Get File	    ${CURDIR}/xml/perustiedot/ORD_OUT_.xml
    ${Seg1}  Create Segment ORD_ZHKI_TARSISTILAUKSET   ${BUKRS}    ${AUART}    ${AUFNR1}    ${KTEXT1}    ${STTXT1}    ${AUTYP1}
    ${Seg2}  Create Segment ORD_ZHKI_TARSISTILAUKSET    ${BUKRS}    ${AUART}    ${AUFNR2}    ${KTEXT2}    ${STTXT2}    ${AUTYP2}
    ${AllSegments}  Catenate    ${Seg1}     ${Seg2}
    ${ORD}     Replace String 	    ${ORD}  __ZHKI_TARSISTILAUKSET__        ${AllSegments}
    RETURN  ${ORD}

Create Segment ORD_ZHKI_TARSISTILAUKSET
    [Arguments]    ${BUKRS}     ${AUART}    ${AUFNR}    ${KTEXT}    ${STTXT}   ${AUTYP}
    ${SEG}=  catenate  SEPARATOR=
    ...     <ZHKI_TARSISTILAUKSET SEGMENT="1">
    ...     <BUKRS>${BUKRS}</BUKRS><AUART>${AUART}</AUART><AUFNR>${AUFNR}</AUFNR><KTEXT>${KTEXT}</KTEXT>
    ...     <STTXT>${STTXT}</STTXT><AUTYP>${AUTYP}</AUTYP>
    ...     </ZHKI_TARSISTILAUKSET>
    RETURN    ${SEG}

Create WBS
    [Arguments]     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}
    ${WBS}     Get File	    ${CURDIR}/xml/perustiedot/WBS_OUT_.xml
    ${AllSegments}  Create Segment ZHKI_PROJEKTIRAKENTEENOSA   ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}    ${ERDAT}   ${AEDAT}   ${TXT40}
    FOR     ${index}    IN RANGE    9
        ${NewSeg}  Create Segment ZHKI_PROJEKTIRAKENTEENOSA   ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}    ${ERDAT}   ${AEDAT}   ${TXT40}
        ${AllSegments}  Catenate    ${AllSegments}     ${NewSeg}
    END
    ${WBS}     Replace String 	    ${WBS}  __ZHKI_PROJEKTIRAKENTEENOSA__        ${AllSegments}
    RETURN  ${WBS}

Create Segment ZHKI_PROJEKTIRAKENTEENOSA
    [Arguments]     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}
    ${SEG}=  catenate  SEPARATOR=
    ...     <ZHKI_PROJEKTIRAKENTEENOSA SEGMENT="1">
    ...     <PBUKR>${PBUKR}</PBUKR><PSPNR>${PSPNR}</PSPNR><POSID>${POSID}</POSID>
    ...     <POST1>${POST1}</POST1><PSPHI></PSPHI><UP></UP>
    ...     <STUFE>${STUFE}</STUFE><VERNR></VERNR><ASTNR></ASTNR>
    ...     <PRCTR></PRCTR><PSTRT></PSTRT><PENDE></PENDE>
    ...     <ESTRT></ESTRT><EENDE></EENDE><ISTRT></ISTRT>
    ...     <IENDE></IENDE><PLAKZ></PLAKZ>
    ...     <USR04></USR04><USR05></USR05><USR06></USR06><USR07></USR07><USR08></USR08><USR09></USR09>
    ...     <ERDAT>${ERDAT}</ERDAT><AEDAT>${AEDAT}</AEDAT>
    ...     <TXT40>${TXT40}</TXT40><BELKZ></BELKZ><OBJNR></OBJNR>
    ...     </ZHKI_PROJEKTIRAKENTEENOSA>
    RETURN    ${SEG}

Create PART
    [Arguments]     ${RCOMP}    ${NAME1}
    ${AllSegments}  Create Segment Kumppaniyhtiö   ${RCOMP}    ${NAME1}
    FOR     ${index}    IN RANGE    9
        ${NewSeg}  Create Segment Kumppaniyhtiö   ${RCOMP}    ${NAME1}
        ${AllSegments}  Catenate    ${AllSegments}     ${NewSeg}
    END
    RETURN  <Kumppanit>${AllSegments}</Kumppanit>

Create Segment Kumppaniyhtiö
    [Arguments]     ${RCOMP}    ${NAME1}
    RETURN        <Kumppaniyhtiö><RCOMP>${RCOMP}</RCOMP><NAME1>${NAME1}</NAME1></Kumppaniyhtiö>


# for both ECC and S4
Lähetä ${TOIMIALA} perustiedot sapsistilaus
    [Documentation]     ${TOIMIALA} perustiedot ORD_OUT_ / sapsistilaus
    [Arguments]          ${PerustiedotFtpDir}    ${BUKRS}    ${AUART}   ${FTPDownKeyword}   ${FTPUpKeyword}
    [Tags]       ${TOIMIALA}      perustiedot     sapsistilaus
    ${BUKRS_SHOULD_NOT_BE_IN_CSV}    Set Variable    -999
    ${AUART}    Set Variable   3901
    ${AUFNR1}   Set Variable    01
    ${KTEXT1}   Set Variable    ktxt1
    ${STTXT1}   Set Variable    st text 1
    ${AUTYP1}   Set Variable    autyp1
    ${AUFNR2}   Set Variable    02
    ${KTEXT2}   Set Variable    ktext2
    ${STTXT2}   Set Variable    sttext 2
    ${AUTYP2}   Set Variable    autyp2
    ${ORD}      Create ORD  ${BUKRS_SHOULD_NOT_BE_IN_CSV}   ${AUART}     ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}

    ${CUR_DATE_STR}     Get Current Time Text

    Create File     ${PerustiedotFtpDir}/203/arch/ORD_OUT_${CUR_DATE_STR}_should_not_be_handled.xml   content=${ORD}

    ${AUFNR1}   Set Variable    01
    ${KTEXT1}   Set Variable    ktxt1
    ${STTXT1}   Set Variable    st text 1
    ${AUTYP1}   Set Variable    autyp1
    ${AUFNR2}   Set Variable    02
    ${KTEXT2}   Set Variable    ktext2
    ${STTXT2}   Set Variable    sttext 2
    ${AUTYP2}   Set Variable    autyp2
    ${ORD}      Create ORD  ${BUKRS}   ${AUART}     ${AUFNR1}   ${KTEXT1}   ${STTXT1}   ${AUTYP1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   ${AUTYP2}
    Create File     ${PerustiedotFtpDir}/203/ORD_OUT_${CUR_DATE_STR}_1.xml   content=${ORD}
    Run Keyword     ${FTPDownKeyword}
    Run Keyword     ${FTPUpKeyword}
    Wait Until Keyword Succeeds      3 minutes   15 seconds      Check SAPSISTILAUS ${PerustiedotFtpDir}/SAPSISTILAUS.csv
        ...     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}   ${STTXT1}
        ...     ${AUFNR2}   ${KTEXT2}   ${STTXT2}   should_not_contain=${BUKRS_SHOULD_NOT_BE_IN_CSV}


Lähetä ${TOIMIALA} perustiedot SAPPROJEKTI
    [Documentation]     ${TOIMIALA}   perustiedot   sapprojekti
    [Arguments]    ${PerustiedotFtpDir}    ${PBUKR}    ${PSPNR}    ${FTPDownKeyword}   ${FTPUpKeyword}
    [Tags]   ${TOIMIALA}      perustiedot     sapprojekti     wbs_out

    ${CUR_DATE_STR}     Get Current Time Text
    ${POSID}   Set Variable    02
    ${POST1}   Generate Random String  20
    ${STUFE}   Generate Random String  20
    ${ERDAT}   Set Variable    autyp2
    ${AEDAT}   Set Variable    03
    ${TXT40}   Set Variable    ${CUR_DATE_STR}
    ${WBS}      Create WBS   ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    ${NonToimialaWBS}      Create WBS   10    ${PSPNR}    ${POSID}   ${POST1}  ${STUFE}  ${ERDAT}  ${AEDAT}   ${TXT40}

    Run Keyword     ${FTPDownKeyword}
    Create File     ${PerustiedotFtpDir}/204/WBS_OUT_non_palke_${CUR_DATE_STR}.xml   content=${NonToimialaWBS}
    Run Keyword     ${FTPUpKeyword}
    Create File     ${PerustiedotFtpDir}/204/WBS_OUT_1_${CUR_DATE_STR}.xml   content=${WBS}

    Wait Until Keyword Succeeds     3 minutes   15 seconds    Should Exist       ${PerustiedotFtpDir}/SAPPROJEKTI.csv
    Check SAPPROJEKTI ${PerustiedotFtpDir}/SAPPROJEKTI.csv
        ...     ${PBUKR}    ${PSPNR}    ${POSID}   ${POST1}   ${STUFE}   ${ERDAT}   ${AEDAT}   ${TXT40}



Lähetä ${TOIMIALA} perustiedot SAPKUMPPANI
    [Documentation]     ${TOIMIALA} perustiedot   sapkumppani
    [Tags]   ${TOIMIALA}      perustiedot     sapkumppani     part_out
    [Arguments]   ${PerustiedotFtpDir}      ${RCOMP}     ${NAME1}    ${FTPDownKeyword}   ${FTPUpKeyword}
    ${RCOMP_1}    Set Variable    R COMP HERE
    ${NAME1_1}    Set Variable   R NAME 1 HERE " OK
    ${PART_1}      Create PART   ${RCOMP_1}    ${NAME1_1}
    ${CUR_DATE_STR}      Get Current Time Text
    Create File     ${PerustiedotFtpDir}/210/PART_OUT_${CUR_DATE_STR}_1.xml   content=${PART_1}
    ${PART_2}      Create PART   ${RCOMP}    ${NAME1}
    Run Keyword     ${FTPDownKeyword}
    Create File     ${PerustiedotFtpDir}/210/PART_OUT_${CUR_DATE_STR}_2.xml   content=${PART_2}
    Run Keyword     ${FTPUpKeyword}

    Wait Until Keyword Succeeds     3 minutes   15 seconds    Should Exist       ${PerustiedotFtpDir}/SAPKUMPPANI.csv
    Wait Until Keyword Succeeds     1 minute   5 seconds    Check SAPKUMPPANI ${PerustiedotFtpDir}/SAPKUMPPANI.csv      ${RCOMP}    ${NAME1}


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


Check SAPSISTILAUS ${SisTilausFilePath}
    [Documentation]     Opens SAPSISTILAUS.csv sent to the FTP and checks it contains the correct lines
    [Arguments]     ${BUKRS}    ${AUART}    ${AUFNR1}   ${KTEXT1}  ${STTXT1}    ${AUFNR2}   ${KTEXT2}  ${STTXT2}    ${should_not_contain}
    ${File}    Get File       ${SisTilausFilePath}
    ${ExpHeader}     Set Variable     BUKRS;AUART;AUFNR;KTEXT;STTXT
    ${FirstLine} =	Get Line	${File}    0
    Should Be Equal     ${FirstLine}       ${ExpHeader}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR1};${KTEXT1};${STTXT1}
    Should Contain   ${File}   ${BUKRS};${AUART};${AUFNR2};${KTEXT2};${STTXT2}
    Should Not Contain     ${File}             ${should_not_contain}
    ${AllLinesCount}    Get Line Count  ${File}
    # can contain lines from previous runs
    Should Be True    ${AllLinesCount} >= 3


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

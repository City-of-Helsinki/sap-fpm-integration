*** Settings ***
Library			OperatingSystem
Library         String

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
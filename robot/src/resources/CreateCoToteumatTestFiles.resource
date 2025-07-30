*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create COTOSITEFILE
    [Documentation]     Creates a cotosite from the given params + cotositteet with BELNR_0 ... BELNR_998
    [Arguments]       ${BELNR}    ${BLDAT}    ${BUDAT}   ${CPUDT}   ${BLART}
        ...     ${REFBN}   ${VERSN}    ${AWTYP}    ${AWORG}      ${BUZEI}
        ...     ${PERIO}   ${WOGBTR}    ${OBJNR}    ${OBJ_TYPE}    ${TYPE_NR}
        ...     ${PRCTR}    ${GJAHR}     ${KSTAR}     ${BEKNZ}     ${BUKRS}    ${SGTXT}   ${FKBER}

    ${CoTositteet}   Create Segment ZCODCMT      {BELNR}    ${BLDAT}    ${BUDAT}   ${CPUDT}   ${BLART}
     ...     ${REFBN}   ${VERSN}    ${AWTYP}    ${AWORG}      ${BUZEI}
     ...     ${PERIO}   ${WOGBTR}    ${OBJNR}    ${OBJ_TYPE}    ${TYPE_NR}
     ...     ${PRCTR}    ${GJAHR}     ${KSTAR}     ${BEKNZ}     ${BUKRS}    ${SGTXT}   ${FKBER}

    FOR     ${index}    IN RANGE    499
       ${NewCoTosite}    Create Segment ZCODCMT      {BELNR}    ${BLDAT}    ${BUDAT}   ${CPUDT}   ${BLART}
       ...     ${REFBN}   ${VERSN}    ${AWTYP}    ${AWORG}      ${BUZEI}
       ...     ${PERIO}   ${WOGBTR}    ${OBJNR}    ${OBJ_TYPE}    ${TYPE_NR}
       ...     ${PRCTR}    ${GJAHR}     ${KSTAR}     ${BEKNZ}     ${BUKRS}    ${SGTXT}   ${FKBER}
        ${CoTositteet}  Catenate    ${CoTositteet}     ${NewCoTosite}
    END


    ${COTOSITE_FILE}  catenate  SEPARATOR=
    ...    <Atos_CODCMT xmlns:prx="urn:sap.com:proxy:P10:/1SAI/TASDA8F90C876F3F5B00A05:740">${CoTositteet}</Atos_CODCMT>
    RETURN  ${COTOSITE_FILE}

Create Segment ZCODCMT
    [Arguments]       ${BELNR}    ${BLDAT}    ${BUDAT}   ${CPUDT}   ${BLART}
        ...     ${REFBN}   ${VERSN}    ${AWTYP}    ${AWORG}      ${BUZEI}
        ...     ${PERIO}   ${WOGBTR}    ${OBJNR}    ${OBJ_TYPE}    ${TYPE_NR}
        ...     ${PRCTR}    ${GJAHR}     ${KSTAR}     ${BEKNZ}     ${BUKRS}    ${SGTXT}   ${FKBER}

    ${ZCODCMT}  catenate  SEPARATOR=
    ...     <ZCODCMT>
    ...     <KOKRS>2000</KOKRS>
    ...     <BELNR>${BELNR}</BELNR>
    ...     <BLDAT>${BLDAT}</BLDAT>
    ...     <BUDAT>${BUDAT}</BUDAT>
    ...     <CPUDT>${CPUDT}</CPUDT>
    ...     <BLART>${BLART}</BLART>
    ...     <REFBN>${REFBN}</REFBN>
    ...     <VERSN>${VERSN}</VERSN>
    ...     <AWTYP>${AWTYP}</AWTYP>
    ...     <AWORG> ${AWORG}</AWORG>
    ...     <BUZEI>
    ...     <BUZEI>${BUZEI}</BUZEI>
    ...     <PERIO>${PERIO}</PERIO>
    ...     <WOGBTR>${WOGBTR}</WOGBTR>
    ...     <OBJNR>${OBJNR}</OBJNR>
    ...     <OBJ_TYPE>${OBJ_TYPE}</OBJ_TYPE>
    ...     <TYPE_NR>${TYPE_NR}</TYPE_NR>
    ...     <PRCTR>${PRCTR}</PRCTR>
    ...     <GJAHR>${GJAHR}</GJAHR>
    ...     <KSTAR>${KSTAR}</KSTAR>
    ...     <TWAER>EUR</TWAER>
    ...     <BEKNZ>${BEKNZ}</BEKNZ>
    ...     <BUKRS>${BUKRS}</BUKRS>
    ...     <WTGBTR></WTGBTR>
    ...     <MEGBTR>0.000 </MEGBTR>
    ...     <MBGBTR>0.000 </MBGBTR>
    ...     <WRTTP>04</WRTTP>
    ...     <VERSN>000</VERSN>
    ...     <VRGNG>COIN</VRGNG>
    ...     <SGTXT>${SGTXT}</SGTXT>
    ...     <REFBZ>001</REFBZ>
    ...     <ZLENR>001</ZLENR>
    ...     <PAOBJNR>0000000000</PAOBJNR>
    ...     <FKBER>${FKBER}</FKBER>
    ...     </BUZEI>
    ...     </ZCODCMT>

    RETURN    ${ZCODCMT}

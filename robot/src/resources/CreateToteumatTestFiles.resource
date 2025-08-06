*** Settings ***
Library			OperatingSystem
Library         String

*** Variables ***


*** Keywords ***

Create TOSITEFILE
    [Documentation]     Creates a tosite from the given params + tositteet with BELNR_0 ... BELNR_498
    [Arguments]     ${BUKRS}     ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

    ${Tositteet}   Create Segment E1FIKPF      ${BUKRS}    ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

    FOR     ${index}    IN RANGE    499
        ${NewTosite}    Create Segment E1FIKPF      ${BUKRS}    ${BELNR}_${index}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
          ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
          ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
          ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
          ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}    ${AWTYP}

        ${Tositteet}  Catenate    ${Tositteet}     ${NewTosite}
    END

    ${TOSITE_FILE}  catenate  SEPARATOR=
    ...    <FIDCCP02 xmlns:prx="urn:sap.com:proxy:P10:/1SAI/TAS1234E5F678901A01209F:740"><IDOC>${Tositteet}</IDOC></FIDCCP02>
    RETURN  ${TOSITE_FILE}

Create Segment E1FIKPF
    [Arguments]    ${BUKRS}     ${BELNR}    ${CO_BELNR}    ${GJAHR}    ${POPER}   ${BLART}
    ...      ${BLDAT}   ${BUDAT}   ${CPUDT}   ${TCODE}   ${XBLNR}   ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}   ${Attachment}   ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}   ${AWTYP}

    # create several segs
    ${E1FISEGS}  Create Segment E1FISEG      ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
    ...      ${EBELN}      ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
    ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
    ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}


    ${E1FIKPF}  catenate  SEPARATOR=
    ...    <E1FIKPF>
    ...    <BUKRS>${BUKRS}</BUKRS>
    ...    <BELNR>${BELNR}</BELNR>
    ...    <CO_BELNR>${CO_BELNR}</CO_BELNR>
    ...    <GJAHR>${GJAHR}</GJAHR>
    ...    <BLART>${BLART}</BLART>
    ...    <BLDAT>${BLDAT}</BLDAT>
    ...    <BUDAT>${BUDAT}</BUDAT>
    ...    <MONAT>${POPER}</MONAT>
    ...    <CPUDT>${CPUDT}</CPUDT>
    ...    <WWERT>20221230</WWERT>
    ...    <USNAM>Usnam</USNAM>
    ...    <TCODE>${TCODE}</TCODE>
    ...    <XBLNR>${XBLNR}</XBLNR>
    ...    <WAERS>EUR</WAERS>
    ...    <KURSF> 1.00000</KURSF>
    ...    <GLVOR>SD00</GLVOR>
    ...    <AWTYP>${AWTYP}</AWTYP>
    ...    <FIKRS>HKI</FIKRS>
    ...    <HWAER>EUR</HWAER>
    ...    <KURS2> 0.00000</KURS2>
    ...    <KURS3> 0.00000</KURS3>
    ...    <NUMPG>000</NUMPG>
    ...    <STJAH>0000</STJAH>
    ...    <RESERVE>${Attachment}</RESERVE>
    ...    <Z_VKBUR>3941</Z_VKBUR>
    ...    ${E1FISEGS}
    ...    <E1FISET SEGMENT=""></E1FISET>
    ...    </E1FIKPF>

    RETURN    ${E1FIKPF}


Create Segment E1FISEG
   [Arguments]
       ...        ${KUNNR}    ${LIFNR}    ${LIFNR_NAME1}
       ...      ${EBELN}     ${BUZEI}    ${CO_BUZEI}    ${RACCT}    ${RCNTR}     ${PRCTR}
       ...      ${RFAREA}    ${AUFNR}     ${PS_PSPID}     ${RASSC}     ${SEGMENT}    ${SGTXT}      ${DRCRK}     ${MWSKZ}
       ...      ${VAT_PERCENT}     ${HSL}    ${PPRCTR}    ${MATNR}     ${EBELP}     ${LAST_CHANGE_DATETIME}    ${AUGBL}

    ${SEG}=  catenate  SEPARATOR=
       ...    <E1FISEG SEGMENT="">
       ...        <BUZEI>${BUZEI}</BUZEI>
       ...        <CO_BUZEI>${CO_BUZEI}</CO_BUZEI>
       ...        <AUGDT>00000000</AUGDT>
       ...        <AUGCP>00000000</AUGCP>
       ...        <AUGBL>${AUGBL}</AUGBL>
       ...        <BSCHL>50</BSCHL>
       ...        <KOART>S</KOART>
       ...        <SHKZG>${DRCRK}</SHKZG>
       ...        <GSBER>5000</GSBER>
       ...        <MWSKZ>${MWSKZ}</MWSKZ>
       ...        <DMBTR>${HSL}</DMBTR>
       ...        <DMBE2> 0.00</DMBE2>
       ...        <DMBE3> 0.00</DMBE3>
       ...        <WRBTR> 50.80</WRBTR>
       ...        <KZBTR> 0.00</KZBTR>
       ...        <PSWBT> 50.80</PSWBT>
       ...        <PSWSL>EUR</PSWSL>
       ...        <HWBAS> 0.00</HWBAS>
       ...        <FWBAS> 0.00</FWBAS>
       ...        <VALUT>20221230</VALUT>
       ...        <ZUONR>20221230</ZUONR>
       ...        <VBUND>${RASSC}</VBUND>
       ...        <VORGN>SD00</VORGN>
       ...        <SGTXT>${SGTXT}</SGTXT>
       ...        <FDTAG>00000000</FDTAG>
       ...        <KOKRS>1000</KOKRS>
       ...        <TXGRP>001</TXGRP>
       ...        <KOSTL>${RCNTR}</KOSTL>
       ...        <AUFNR>${AUFNR}</AUFNR>
       ...        <POSN2>000000</POSN2>
       ...        <BZDAT>00000000</BZDAT>
       ...        <PERNR>00000000</PERNR>
       ...        <HKONT>${RACCT}</HKONT>
       ...        <ABPER>000000</ABPER>
       ...        <MATNR>${MATNR}</MATNR>
       ...        <WERKS>3900</WERKS>
       ...        <MENGE> 1.000</MENGE>
       ...        <MEINS>ST</MEINS>
       ...        <ERFMG> 0.000</ERFMG>
       ...        <BPMNG> 0.000</BPMNG>
       ...        <EBELN>${EBELN}</EBELN>
       ...        <EBELP>${EBELP}</EBELP>
       ...        <ZEKKN>00</ZEKKN>
       ...        <BUALT> 0.00</BUALT>
       ...        <PRCTR>${PRCTR}</PRCTR>
       ...        <DABRZ>00000000</DABRZ>
       ...        <FIPOS>3200</FIPOS>
       ...        <AUFPL>0000000000</AUFPL>
       ...        <APLZL>00000000</APLZL>
       ...        <PROJK>${PS_PSPID}</PROJK>
       ...        <PAOBJNR>0000000000</PAOBJNR>
       ...        <FISTL>3900</FISTL>
       ...        <GEBER>31001</GEBER>
       ...        <ALTKT>0000003234</ALTKT>
       ...        <PPRCT>${PPRCTR}</PPRCT>
       ...        <KBLPOS>000</KBLPOS>
       ...        <OBZEI>000</OBZEI>
       ...        <TXDAT>00000000</TXDAT>
       ...        <KURSR> 0.00000</KURSR>
       ...        <GBETR> 0.00</GBETR>
       ...        <E1FISE2 SEGMENT="">
       ...          <SCTAX> 0.00</SCTAX>
       ...          <STTAX> 0.00</STTAX>
       ...          <KSTAR>0000323456</KSTAR>
       ...          <FKBER_LONG>${RFAREA}</FKBER_LONG>
       ...        </E1FISE2>
       ...        <E1FINBU SEGMENT="">
       ...       <MWSTS> 0.00</MWSTS>
       ...       <KUNNR>${KUNNR}</KUNNR>
       ...       <WMWST> 0.00</WMWST>
       ...       <ZFBDT>00000000</ZFBDT>
       ...    <ZBD1T> 0</ZBD1T>
       ...    <ZBD2T> 0</ZBD2T>
       ...    <ZBD3T> 0</ZBD3T>
       ...    <ZBD1P> 0.000</ZBD1P>
       ...    <ZBD2P> 0.000</ZBD2P>
       ...    <SKFBT> 0.00</SKFBT>
       ...    <SKNTO> 0.00</SKNTO>
       ...    <WSKTO> 0.00</WSKTO>
       ...    <REBZJ>0000</REBZJ>
       ...    <REBZZ>000</REBZZ>
       ...    <VRSDT>00000000</VRSDT>
       ...    <MADAT>00000000</MADAT>
       ...    <MANST>0</MANST>
       ...    <QSSHB> 0.00</QSSHB>
       ...    <QSFBT> 0.00</QSFBT>
       ...    <LIFNR>${LIFNR}</LIFNR>
       ...    <KIDNO>39200123456</KIDNO>
       ...    <SKNT2> 0.00</SKNT2>
       ...    <SKNT3> 0.00</SKNT3>
       ...    <PYAMT> 0.00</PYAMT>
       ...    <ABSBT> 0.00</ABSBT>
       ...    <DTWS1>00</DTWS1>
       ...    <DTWS2>00</DTWS2>
       ...    <DTWS3>00</DTWS3>
       ...    <DTWS4>00</DTWS4>
       ...    </E1FINBU>
       ...    </E1FISEG>

       RETURN   ${SEG}
*** Settings ***
Library         DateTime
Library         String

*** Keywords ***
Get Current Time Text
    [Documentation]     Returns the current time in format %Y-%m-%d_%H:%M:%S.%f
    ${DATE}     Get Current Date		result_format=%Y-%m-%d_%H:%M:%S.%f
    RETURN      ${DATE}

Escape CSV Value
    [Documentation]     If the value contains " or starts with a space, returns the value wrapped in "" and all " replaced with ""
    [Arguments]     ${CSV_VAL}
    ${CSV_ESCAPED}   Replace String    ${CSV_VAL}     "     ""
    IF    '"' in '${CSV_ESCAPED}' or '${CSV_ESCAPED}'.startswith(' ')
        ${CSV_ESCAPED}     Set Variable    "${CSV_ESCAPED}"
    END
    RETURN  ${CSV_ESCAPED}
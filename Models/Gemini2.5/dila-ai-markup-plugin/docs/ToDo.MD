# To Do
* Insert in Replace button text(i18n)
    * Use reflection to call insertTextAtOffset
    * Document insertion
* Action Options
    * pass option selected info from action in optionpage to textArea in plugin workspace


# Structure of UTF8 validation

* UTF8CheckActionListener
    * (M) i18n("action.utf8.check.transfer.selected")
    * (M) i18n("utf8.check.scanning.files")
    * checkUtf8Files(selectedFiles), executor
        * OK: displayUtf8CheckResults(nonUtf8Files)
            * (empty nonUtf8Files), setResultInformational(i18n("utf8.all.valid")), return
                * resultArea.setText(result=i18n("utf8.all.valid"))
                * hideAllButtons();
            * (!empty nonUtf8Files), setResultWithConversionButtons(info.toString())
                * resultArea.setText(result);
                * (isValidResultForConversion(result)), showTransferButtons()
                * (!isValidResultForConversion(result)), hideAllButtons();
        * NG: setResultInformational(i18n("error.checking.utf8", throwable.getMessage()))
            * resultArea.setText(result=i18n("utf8.all.valid"))
            * hideAllButtons();
* TransferButtonActionListener
    * convertFilesToUtf8(currentNonUtf8Files), executor, return summaryBuilder
        * OK: displayConversionResults(conversionResults)
        * NG: infoArea.setText(i18n("error.converting.files", throwable.getMessage()))
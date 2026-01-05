# prompt from Issues to AI Assistant

## compile tsv list between xmls before and after human inspection

* based on the difference between <ref> elements in two folders of xmls

would like to compile a tsv <ref> list between xmls which is before and after human inspection
  xmls before inspection: D:\project\OCR2Markup\Models\Gemini2.5\xml-w-link\link-b4-check
  xmls after inspection: D:\project\OCR2Markup\Models\Gemini2.5\xml-w-link\link-after-check

  the tsv list should be include the following column:
    file name: "141237.xml"
    location: relative path like "v53.1" from "v53.1\141237.xml"
    xml id: "r13", from <ref xml:id="r13" checked="0"><ptr href="https://cbetaonline.dila.edu.tw/X01n0008_p0261b12"/><ptr href="https://cbetaonline.dila.edu.tw/X01n0001_p0001a01"/><canon>続蔵</canon><v>一・一六</v>、<p>二四九</p><c>左上</c>く <p>二五〇</p><c>右上</c></ref>
    <ref> before inspection: <ref xml:id="r13" checked="0"><ptr href="https://cbetaonline.dila.edu.tw/X01n0008_p0261b12"/><ptr href="https://cbetaonline.dila.edu.tw/X01n0001_p0001a01"/><canon>続蔵</canon><v>一・一六</v>、<p>二四九</p><c>左上</c>く <p>二五〇</p><c>右上</c></ref>
    <ptr> before inspection: <ptr href="https://cbetaonline.dila.edu.tw/X01n0008_p0261b12"/>, <ptr href="https://cbetaonline.dila.edu.tw/X01n0001_p0001a01"/>
    <ref> after inspection: <ref xml:id="r13" checked="1"><ptr href="https://cbetaonline.dila.edu.tw/zh/T39n1799_p0830c24"/><canon>続蔵</canon><v>一・一六</v>、<p>二四九</p><c>左上</c>く <p>二五〇</p><c>右上</c></ref>
    <ptr> after inspection: <ptr href="https://cbetaonline.dila.edu.tw/zh/T39n1799_p0830c24"/>
    result in ptr list: yes/no

## turn above into cases for AI assistant to learn

* Grouped by how <ptr>s changed inside each <ref> (ignoring the last column)

help to group the rows into different cases inside <ref>, ignore the last column for now

## add function in AI assistant based on the above cases


    
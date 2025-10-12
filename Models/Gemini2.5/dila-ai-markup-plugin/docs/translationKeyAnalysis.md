# Translation Key Analysis

## Overview
Analysis of translation key usage in the DILA AI Markup Plugin to identify unused translation keys in `translation.xml`.

**Analysis Date:** October 5, 2025  
**Target Directory:** `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin`  
**Translation File:** `Models\Gemini2.5\dila-ai-markup-plugin\src\main\resources\i18n\translation.xml`

## Summary Statistics

| Metric | Value | Percentage |
|--------|-------|------------|
| **Total translation keys** | 69 | 100% |
| **Used keys** | 32 | 46.4% |
| **Unused keys** | 37 | 53.6% |

## Used Translation Keys (32 keys)

### DAMAWorkspaceAccessPluginExtension.java (28 keys)
- `view.title` - Main window title
- `actions.menu` - Actions menu label
- `ai.markup.action` - AI Markup menu item
- `tag.removal.action` - Tag Removal menu item
- `menu.tools` - Tools menu label
- `menu.tools.utf8.check` - UTF-8 Check/Convert menu item
- `preferences.action` - Preferences menu item
- `initial.info` - Initial information text
- `replace.button` - Replace button label
- `ui.button.transfer.utf8` - Transfer to UTF-8 button
- `button.cancel` - Cancel button label
- `ui.processing.ai.markup` - AI processing status message
- `ui.text.length` - Text length display with parameter
- `ui.error.processing.ai.markup` - AI processing error message
- `ui.processing.tag.removal` - Tag removal processing status
- `ui.error.checking.utf8` - UTF-8 checking error message
- `ui.text.replaced.successfully` - Successful text replacement message
- `ui.error.replacing.text` - Text replacement error message
- `ui.error.accessing.editor` - Editor access error message
- `ui.error.converting.files` - File conversion error message
- `ui.utf8.conversion.cancelled` - UTF-8 conversion cancelled message
- `system.prompt.ai.markup` - System prompt for AI markup
- `llm.error` - Language model error message
- `ui.utf8.all.valid` - All files valid UTF-8 message
- `ui.utf8.non.utf8.found` - Non-UTF-8 files found message with parameter
- `ui.utf8.successfully.converted` - Successful conversion count with parameter
- `ui.utf8.failed.conversions` - Failed conversion count with parameter
- `ui.utf8.conversion.completed` - Conversion completed message

### DAMAOptionPagePluginExtension.java (4 keys)
- `ft.parse.model.label` - Parsing model label
- `ft.detect.model.label` - Detection model label
- `api.key.label` - API key label
- `preferences.page.title` - Preferences page title

## Unused Translation Keys (37 keys)

### Legacy UI Messages (13 keys)
These appear to be planned but never implemented:
- `action.ai.markup.selected`
- `text.replaced`
- `text.mode.not.active.for.replace`
- `error.replacing.text`
- `text.editor.not.open`
- `no.text.to.replace`
- `action.tag.removal.selected`
- `action.settings.selected`
- `selected.text.label`
- `no.text.selected`
- `not.text.mode`
- `no.editor.open`
- `options.menu`

### Options Dialog Messages (4 keys)
Not currently used in the options dialog:
- `options.panel.title`
- `options.info`
- `options.saved.successfully`
- `options.saved.failed`
- `save.button`

### UTF-8 Conversion Dialog Messages (19 keys)
Detailed UTF-8 conversion messages that might be used in a different UI flow:
- `utf8.check.select.files`
- `utf8.check.cancelled`
- `utf8.check.dialog.title`
- `utf8.scanning.files`
- `utf8.check.found.non.utf8`
- `utf8.check.more.files`
- `utf8.check.all.valid`
- `utf8.check.select.encoding`
- `utf8.conversion.started`
- `utf8.conversion.backing.up`
- `utf8.conversion.success`
- `utf8.conversion.failed`
- `utf8.conversion.summary`
- `utf8.conversion.success.count`
- `utf8.conversion.fail.count`
- `utf8.conversion.cancelled`
- `utf8.conversion.error`
- `utf8.conversion.backup.note`
- `button.convert`

## Recommendations

### Option 1: Implement Missing Functionality
Consider implementing the features that would use these unused keys:
1. **Enhanced UTF-8 conversion dialog** with detailed progress and status messages
2. **Improved options panel** with better user feedback
3. **Status messages** for various operations

### Option 2: Clean Up Translation File
Remove unused keys to reduce maintenance overhead:
1. Remove the 37 unused keys from `translation.xml`
2. This would reduce the file size and translation maintenance burden
3. Keys can be re-added later if functionality is implemented

### Option 3: Document for Future Use
Keep the keys but document them as "reserved for future features" to maintain translation consistency when features are implemented.

## File Locations

- **Main Analysis Target:** `Models\Gemini2.5\dila-ai-markup-plugin\src\main\java\com\dila\dama\plugin\`
- **Translation File:** `Models\Gemini2.5\dila-ai-markup-plugin\src\main\resources\i18n\translation.xml`
- **Primary Usage Files:**
  - `workspace\DAMAWorkspaceAccessPluginExtension.java`
  - `preferences\DAMAOptionPagePluginExtension.java`

## Notes

- The analysis was performed by searching for `i18n()` method calls in the Java source code
- Only direct string literal usage was counted as "used"
- Some keys might be used dynamically through variable key names (not detected in this analysis)
- The UTF-8 related unused keys suggest there may have been plans for a more detailed conversion dialog
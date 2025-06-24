# Migration Guide: Android 13+ Document Access & Audio Fixes

This guide helps you migrate your existing code to work with the latest version that fixes Android 12 audio issues and properly handles Android 13+ document access.

## What's Changed

### ✅ Fixed Issues
1. **Audio files on Android 12 and below** - Now works correctly with `getMediasByType({ mediaType: 'audio' })`
2. **Document access on Android 13+** - Now properly guides you to use Storage Access Framework (SAF)

### 🔄 Breaking Changes
- **Android 13+ Document Access**: `getMediasByType({ mediaType: 'document' })` will return an error/guidance message instead of documents
- **New Methods Added**: Three new methods for SAF support

## Migration Steps

### Step 1: Update Your Audio File Code

**Before (broken on Android 12 and below):**
```typescript
// This used to not return audio files on Android 12 and below
const audioFiles = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio'
});
```

**After (now works on all versions):**
```typescript
// This now works correctly on all Android versions
const audioFiles = await CapacitorMediaStore.getMediasByType({
  mediaType: 'audio'
});
console.log(`Found ${audioFiles.totalCount} audio files`); // Will now show actual count
```

**Result:** No code changes needed! Your existing audio code will now work correctly.

### Step 2: Update Your Document Access Code

**Before (worked on all versions):**
```typescript
// This used to work on all Android versions
const documents = await CapacitorMediaStore.getMediasByType({
  mediaType: 'document'
});
```

**After (version-aware approach):**
```typescript
// Check if we need to use SAF for documents
const safStatus = await CapacitorMediaStore.shouldUseSAFForDocuments();

if (safStatus.shouldUseSAF) {
  // Android 13+ - Use Storage Access Framework
  console.log('Android 13+ detected - documents require SAF');
  
  // Option 1: Show user a message to use file picker
  alert('Please use the device file picker to select documents');
  
  // Option 2: Provide guidance for implementing SAF in your Activity
  const intentInfo = await CapacitorMediaStore.createDocumentPickerIntent();
  console.log('Implement this in your Activity:', intentInfo.guidance);
  
  // Option 3: If you have a document URI from SAF, get its metadata
  if (documentUriFromSAF) {
    const metadata = await CapacitorMediaStore.getDocumentMetadataFromSAF({
      uri: documentUriFromSAF
    });
    console.log('Document metadata:', metadata);
  }
} else {
  // Android 12 and below - Use MediaStore (works as before)
  const documents = await CapacitorMediaStore.getMediasByType({
    mediaType: 'document'
  });
  console.log(`Found ${documents.totalCount} documents`);
}
```

### Step 3: Handle Document Access Gracefully

**Recommended approach with error handling:**
```typescript
async function getDocuments() {
  try {
    // First check if SAF is required
    const safStatus = await CapacitorMediaStore.shouldUseSAFForDocuments();
    
    if (safStatus.shouldUseSAF) {
      // Handle Android 13+ SAF requirement
      return handleAndroid13DocumentAccess();
    } else {
      // Handle Android 12 and below MediaStore access
      return await CapacitorMediaStore.getMediasByType({
        mediaType: 'document'
      });
    }
  } catch (error) {
    if (error.message.includes('Storage Access Framework')) {
      // Document access requires SAF on Android 13+
      return handleAndroid13DocumentAccess();
    }
    throw error;
  }
}

function handleAndroid13DocumentAccess() {
  // Show user interface to select documents
  // Implement SAF in your native Activity
  return {
    media: [],
    totalCount: 0,
    hasMore: false,
    message: 'Please use the file picker to select documents on Android 13+'
  };
}
```

## Implementation Guide for Storage Access Framework

### For Capacitor/Ionic Apps

If you need document access on Android 13+, you'll need to implement SAF in your native Android code:

**1. Create a native Android Activity method:**

```kotlin
// In your MainActivity or custom Activity
private fun openDocumentPicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        // Optionally specify MIME types
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
            "application/pdf",
            "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        ))
    }
    startActivityForResult(intent, REQUEST_DOCUMENT_PICKER)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_DOCUMENT_PICKER && resultCode == Activity.RESULT_OK) {
        data?.data?.also { uri ->
            // Send URI back to your JavaScript/TypeScript code
            notifyListeners("documentSelected", JSObject().put("uri", uri.toString()))
        }
    }
}
```

**2. Listen for the document selection in your TypeScript:**

```typescript
import { CapacitorMediaStore } from 'capacitor-media-store';

// Listen for document selection
window.addEventListener('documentSelected', async (event: any) => {
  const uri = event.detail.uri;
  
  // Get document metadata using SAF
  const metadata = await CapacitorMediaStore.getDocumentMetadataFromSAF({
    uri: uri
  });
  
  console.log('Selected document:', metadata);
});
```

## Testing Your Migration

### Test Cases to Verify

1. **Audio Files on Android 12 and below:**
   ```typescript
   const audioFiles = await CapacitorMediaStore.getMediasByType({
     mediaType: 'audio'
   });
   // Should now return actual audio files, not empty array
   ```

2. **Audio Files on Android 13+:**
   ```typescript
   const audioFiles = await CapacitorMediaStore.getMediasByType({
     mediaType: 'audio'
   });
   // Should continue working as before
   ```

3. **Documents on Android 12 and below:**
   ```typescript
   const documents = await CapacitorMediaStore.getMediasByType({
     mediaType: 'document'
   });
   // Should continue working as before
   ```

4. **Documents on Android 13+:**
   ```typescript
   try {
     const documents = await CapacitorMediaStore.getMediasByType({
       mediaType: 'document'
     });
   } catch (error) {
     // Should receive guidance about using SAF
     console.log(error.message); // Contains SAF guidance
   }
   ```

5. **SAF Availability Check:**
   ```typescript
   const safStatus = await CapacitorMediaStore.shouldUseSAFForDocuments();
   // Should return true for Android 13+, false for older versions
   ```

## Common Migration Issues

### Issue 1: "No audio files found" on Android 12
**Solution:** ✅ Already fixed! Update to the latest version.

### Issue 2: "Documents not loading" on Android 13+
**Solution:** This is expected. Implement SAF as shown above.

### Issue 3: Permission denied errors
**Solution:** Ensure you're requesting the correct permissions:
```typescript
// Request appropriate permissions based on Android version
await CapacitorMediaStore.requestPermissions({
  types: ['images', 'audio', 'video'] // Note: documents not included for Android 13+
});
```

## Timeline Recommendations

1. **Immediate (Audio Fix):** Update your plugin version to get audio files working on Android 12 and below
2. **Short-term (Document Handling):** Add SAF availability checks and graceful fallbacks
3. **Long-term (Full SAF Implementation):** Implement native SAF support if document access is critical

## Support

If you encounter issues during migration:

1. Check the Android version using `shouldUseSAFForDocuments()`
2. Test on both Android 12 and Android 13+ devices
3. Verify permissions are correctly requested and granted
4. Check our [troubleshooting guide](README.md#troubleshooting) in the main README

## Summary

- ✅ **Audio files**: No code changes needed - now works on all versions
- ⚠️ **Documents**: Add version checking and implement SAF for Android 13+
- 📱 **Testing**: Verify on both Android 12 and 13+ devices
- 🔧 **Migration effort**: Low for audio, medium for documents (if SAF implementation needed) 